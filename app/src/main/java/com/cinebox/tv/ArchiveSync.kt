package com.cinebox.tv

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.net.URI
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object ArchiveSync {
    private val lock = Mutex()
    suspend fun <T> storageOperation(block: () -> T): T = withContext(Dispatchers.IO) {
        lock.withLock { block() }
    }
    private val sources = listOf("https://filmmakinesi.to/", "https://www.fullhdfilmizlesene.now/", "https://www.hdfilmcehennemi.land/film/", "https://www.hdfilmcehennemi.land/dizi/", "https://www.hdfilmcehennemi.land/", "https://rfilmizle.com/", "https://www.ddizi.tel/", "https://ufilmizle.com/", "https://fullfilmizle.fit/")
    private fun prefs(c: Context) = c.getSharedPreferences("archive_sync", Context.MODE_PRIVATE)
    private fun norm(t: String) = Normalizer.normalize(t.lowercase(Locale.forLanguageTag("tr")).replace('ı','i'),Normalizer.Form.NFD).replace(Regex("[^a-z0-9]"),"")
    private fun title(t: String) = t.replace(Regex("(?i)\\s+izle.*$"),"").replace(Regex("\\s*\\(?((19|20)\\d{2})\\)?$"),"").trim()
    private fun valid(u: String): Boolean = try { val x=URI(u); x.scheme=="https" && sources.any { URI(it).host==x.host } } catch (_:Exception) { false }
    private fun read(c:Context,name:String): JSONObject = try { JSONObject(AtomicFile(File(c.filesDir,name)).openRead().bufferedReader().use { it.readText() }) } catch (_:Exception) { JSONObject() }
    private fun write(c:Context,name:String,data:JSONObject) {
        val f=AtomicFile(File(c.filesDir,name)); val out=f.startWrite()
        try { out.write(data.toString().toByteArray(Charsets.UTF_8)); f.finishWrite(out) } catch(e:Exception) { f.failWrite(out); throw e }
    }
    private fun movie(o:JSONObject) = Movie(id=o.getInt("id"),title=o.getString("title"),overview=o.optString("overview"),poster=o.optString("poster"),backdrop="",year=o.optString("year"),score=0.0,genreIds=emptyList(),webUrl=o.getString("url"),provider=o.optString("provider"),category=o.optString("category","Film"))
    fun load(c:Context,base:List<Movie>):List<Movie> {
        val a=read(c,"archive_dynamic.json").optJSONArray("movies")?:JSONArray()
        return (base+NetdCatalog.load(c)+(0 until a.length()).mapNotNull { try { movie(a.getJSONObject(it)) } catch (_:Exception) { null } }).distinctBy { it.id }
    }
    fun notices(c:Context):String = prefs(c).getString("notices","Henüz yeni içerik bildirimi yok.")!!
    fun episodes(c:Context,u:String):List<SeriesEpisode> {
        val a=read(c,"archive_episodes.json").optJSONArray(u)?:return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it).let { e-> SeriesEpisode(e.getInt("season"),e.getInt("episode"),e.getString("url")) } }
    }
    private fun document(u:String) = Jsoup.connect(u).userAgent("Mozilla/5.0 ALICOTV/0.46").timeout(10000).maxBodySize(3_000_000).get()
    suspend fun update(c:Context,base:List<Movie>,force:Boolean=false):String = withContext(Dispatchers.IO) {
      lock.withLock {
        val now=System.currentTimeMillis(); val p=prefs(c)
        if(!force && now-p.getLong("attempt",0)<12*60*60*1000L) return@withLock ""
        p.edit().putLong("attempt",now).apply()
        val current=load(c,base).toMutableList();val added=mutableListOf<String>();val failures=mutableListOf<String>()
        val state=read(c,"archive_dynamic.json");val a=state.optJSONArray("movies")?:JSONArray()
        val ids=current.map { it.id }.toMutableSet()
        val episodeState=read(c,"archive_episodes.json"); var episodeChanges=false
        for(source in sources) {
          try {
            val doc=document(source)
            for(link in doc.select("a[href]")) {
              val u=link.absUrl("href"); val label=link.text()
              if(!valid(u) || (!URI(u).path.contains("/bolum/") && !URI(u).path.contains("/izle/")))continue
              if(Regex("(?i)fragman|özet|tanıtım|sahne").containsMatchIn(label))continue
              val number=Regex("(?i)(\\d+)\\s*\\.?\\s*Bölüm").find(label)?.groupValues?.get(1)?.toIntOrNull()?:continue
              val season=Regex("(?i)(\\d+)\\s*\\.?\\s*Sezon").find(label)?.groupValues?.get(1)?.toIntOrNull()?:1
              val match=current.filter { it.category in listOf("Dizi","Yerli Diziler") && norm(label).startsWith(norm(it.title.substringBefore(':'))) }.maxByOrNull { it.title.length }?:continue
              if(number<1)continue
              val known=OfflineEpisodes.get(c,match.webUrl)
              val saved=episodeState.optJSONArray(match.webUrl)?:JSONArray()
              if(known.any { it.season==season && it.episode==number } || (0 until saved.length()).any { saved.getJSONObject(it).optInt("season")==season && saved.getJSONObject(it).optInt("episode")==number })continue
              saved.put(JSONObject().put("season",season).put("episode",number).put("url",u));episodeState.put(match.webUrl,saved);episodeChanges=true
              added.add("Yeni bölüm: ${match.title} · $season. Sezon $number. Bölüm")
            }
            val cards=when {
              source.contains("filmmakinesi") -> doc.select("a.item[data-title]")
              source.contains("ddizi") -> doc.select("a[href*=/diziler/]")
              source.contains("rfilmizle") -> doc.select(".listmovie")
              source.contains("hdfilmcehennemi") -> doc.select("article")
              else -> doc.select("article, .movie, .film, .poster, .film-item, .movie-item, .film-poster, .poster-container, a:has(img)")
            }
            var parsed=0
            for(card in cards) {
              val link=if(card.tagName()=="a") card else card.selectFirst("a[href]")?:continue
              val u=link.absUrl("href").substringBefore('#');if(!valid(u))continue
              val path=URI(u).path
              if(source.contains("ddizi")) { if(!path.startsWith("/diziler/"))continue }
              else if(!source.contains("rfilmizle") && !path.contains("/film/") && !path.contains("/dizi/") && !path.contains("-izle"))continue
              if(path.contains("/bolum/") || path.contains("/kategori/") || path.contains("/tur/"))continue
              val img=card.selectFirst("img")
              val raw=card.attr("data-title").takeIf { it.isNotBlank() } ?:card.selectFirst("h2, h3, .film-ismi, .title")?.text()?.takeIf { it.isNotBlank() }
                ?:img?.attr("alt")?.takeIf { it.isNotBlank() }?:link.text()
              val name=title(raw).removeSuffix(" Poster").trim();if(name.length<2)continue
              if(Regex("(?i)fragman|masterchef|müge anlı").containsMatchIn(name))continue
              parsed++
              val season=Regex("(?i)(\\d+)\\s*\\.?\\s*Sezon").containsMatchIn(name)
              val series=season || path.contains("/dizi/") || path.contains("/diziler/") || card.hasClass("tvshows")
              val cleanName=name.replace(Regex("(?i)\\s*\\d+\\s*\\.?\\s*Sezon.*$"),"").trim()
              val year=Regex("(?:19|20)\\d{2}").find(card.selectFirst(".anayil, .film-yil, .year")?.text().orEmpty())?.value.orEmpty()
              if(current.any { it.webUrl.trimEnd('/')==u.trimEnd('/') || ((it.category in listOf("Dizi","Yerli Diziler"))==series && norm(title(it.title))==norm(cleanName) && (it.year.isBlank() || year.isBlank() || it.year==year)) })continue
              var id=1_500_000_000+(u.hashCode() and 0x0fffffff);while(id in ids)id++
              val poster=img?.let { if(it.hasAttr("data-src"))it.absUrl("data-src") else it.absUrl("src") }.orEmpty().takeIf { it.startsWith("https://") }.orEmpty()
              val o=JSONObject().put("id",id).put("title",cleanName).put("url",u).put("poster",poster).put("year",year).put("provider",URI(source).host)
                .put("category",if(series) "Dizi" else "Film")
                .put("overview","Kaynak sayfa arşive eklendi. İzlemek için içeriği açın.")
              a.put(o);current.add(movie(o));ids.add(id);added.add((if(series)"Dizi: " else "Film: ")+cleanName)
            }
            if(parsed==0)failures.add(URI(source).host+": liste okunamadı")
          } catch(e:Exception) { failures.add(URI(source).host+": erişilemedi") }
        }
        state.put("movies",a);write(c,"archive_dynamic.json",state)
        if(episodeChanges)write(c,"archive_episodes.json",episodeState)
        if(added.isNotEmpty())p.edit().putString("notices",java.text.DateFormat.getDateTimeInstance().format(java.util.Date())+"\n"+added.joinToString("\n")+"\n\n"+notices(c).take(12000)).apply()
        "${added.size} yeni içerik eklendi."+(if(failures.isNotEmpty()) "\n"+failures.distinct().joinToString("\n") else "")
      }
    }
    suspend fun refreshEpisodes(c:Context,m:Movie):String = withContext(Dispatchers.IO) {
      lock.withLock {
        if(!valid(m.webUrl) || m.category !in listOf("Dizi","Yerli Diziler"))return@withLock ""
        val p=prefs(c);val stamp="episodes_"+m.id;val now=System.currentTimeMillis()
        if(now-p.getLong(stamp,0)<60*60*1000L)return@withLock ""
        val existing=OfflineEpisodes.get(c,m.webUrl).associateBy { it.season to it.episode }.toMutableMap()
        val original=existing.keys.toSet();val todo=java.util.ArrayDeque<String>();todo.add(m.webUrl);val seen=mutableSetOf<String>();val added=mutableListOf<String>();var failed=false
        while(todo.isNotEmpty() && seen.size<100) {
          val u=todo.removeFirst();if(!seen.add(u))continue
          try {
            val doc=document(u)
            for(link in doc.select("a[href]")) {
              val target=link.absUrl("href");if(!valid(target))continue
              if(target.startsWith(m.webUrl.trimEnd('/')+"/sayfa-") && target !in seen)todo.add(target)
              val label=link.text();if(Regex("(?i)fragman|özet|sahne|tanıtım").containsMatchIn(label))continue
              val ep=Regex("(?i)(\\d+)\\s*\\.?\\s*Bölüm").find(label)?.groupValues?.get(1)?.toIntOrNull()?:continue
              val season=Regex("(?i)(\\d+)\\s*\\.?\\s*Sezon").find(label)?.groupValues?.get(1)?.toIntOrNull()?:1
              if(ep<1)continue
              val relevant=when {
                m.webUrl.contains("ddizi") -> target.contains("/izle/") && norm(label).startsWith(norm(m.title.substringBefore(':')))
                m.webUrl.contains("hdfilmcehennemi") -> target.contains("/bolum/") && norm(URI(target).path).contains(norm(m.title))
                else -> target.startsWith(m.webUrl.trimEnd('/')+"/") || target.contains("/bolum/") && norm(label).startsWith(norm(m.title))
              }
              if(!relevant)continue
              val key=season to ep
              if(key !in existing) {existing[key]=SeriesEpisode(season,ep,target);added.add("${m.title}: $season. Sezon $ep. Bölüm")}
            }
          } catch(e:Exception) {failed=true}
        }
        if(todo.isNotEmpty())failed=true
        if(existing.keys!=original) {
          val state=read(c,"archive_episodes.json");val a=JSONArray()
          existing.values.sortedWith(compareBy({it.season},{it.episode})).forEach { a.put(JSONObject().put("season",it.season).put("episode",it.episode).put("url",it.url)) }
          state.put(m.webUrl,a);write(c,"archive_episodes.json",state)
          p.edit().putString("notices",added.joinToString("\n")+"\n\n"+notices(c).take(12000)).apply()
        }
        if(!failed)p.edit().putLong(stamp,now).apply()
        if(added.isNotEmpty())"${m.title}: ${added.size} yeni bölüm eklendi." else if(failed)"Bazı bölüm sayfalarına ulaşılamadı; kayıtlı bölümler gösteriliyor." else ""
      }
    }
}
