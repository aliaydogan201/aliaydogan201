package com.cinebox.tv

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.C
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

private val bg = Color(0xFFAD4300)
private val panel = Color(0xFF512700)
private val accent = Color(0xFFFFE082)
private data class PlayRequest(val movieId: Int, val title: String, val url: String, val subtitleUrl: String = "")
private fun validStreamUrl(url: String): Boolean = try {
    val uri = Uri.parse(url.trim())
    uri.scheme == "https" && !uri.host.isNullOrBlank() &&
        (uri.path.orEmpty().lowercase().endsWith(".mp4") || uri.path.orEmpty().lowercase().endsWith(".m3u8") || uri.path.orEmpty().lowercase().endsWith(".mpd") || uri.path.orEmpty().lowercase().endsWith(".webm"))
} catch (_: Exception) { false }


// Playback links are explicitly provided by the publisher or content owner, never scraped.
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val poster: String,
    val backdrop: String,
    val year: String,
    val score: Double,
    val genreIds: List<Int>,
    val playableUrl: String = "",
    val license: String = "",
    val provider: String = "",
    val webUrl: String = "",
    val category: String = "Film"
)

// Full-length sources checked with ranged HTTP requests; no shared demo fallback.
internal val catalogMovies = listOf(
    Movie(id = 7502, title = "Ayrılsak da Beraberiz (1999)", overview = "Janset ve Hakan Yılmaz’lı eski seri. Bağlı arşiv sayfasında 40 bölüm listeleniyor; tüm seri veya yalnız 1999 yılında yayımlanan bölümler olarak doğrulanmadı. 2015 uyarlaması değildir.", poster = "", backdrop = "", year = "1999", score = 0.0, genreIds = listOf(35), provider = "Diziizle — üçüncü taraf arşiv", webUrl = "https://www.diziizle.you/ayrilsak-da-beraberiz-eski-bolumler/", category = "Yerli Diziler"),
    Movie(id = 7501, title = "Yaprak Dökümü", overview = "Kanal D’nin resmî bölüm arşivi. Tüm bölümler sayfasını veya aşağıdaki sezonlardan birini açıp izlemek istediğiniz bölümü seçin.", poster = "", backdrop = "", year = "2006–2010", score = 0.0, genreIds = listOf(18), provider = "Kanal D", webUrl = "https://www.kanald.com.tr/yaprakdokumu/bolumler", category = "Yerli Diziler"),
    Movie(id = 7401, title = "Guguk Kuşu", overview = "Verdiğiniz sitedeki oynatıcıyı uygulama içinde açar. Doğrudan video kaynağı doğrulanmadı; site erişimi TV üzerinde denenmelidir.", poster = "", backdrop = "", year = "1975", score = 0.0, genreIds = emptyList(), provider = "Kullanıcının seçtiği site", webUrl = "https://www.fullhdfilmizlesene.now/film/guguk-kusu/", category = "Film"),
    Movie(id = 7404, title = "Tarkan — Şımarık", overview = "Tarkan’ın resmî sitesinin yönlendirdiği müzik klibi. YouTube oynatıcısında açılır.", poster = "", backdrop = "", year = "1997", score = 0.0, genreIds = emptyList(), provider = "Tarkan", webUrl = "https://www.youtube.com/watch?v=cpp69ghR1IM", category = "Müzik"),
    Movie(id = 7405, title = "Tarkan — Kuzu Kuzu", overview = "Tarkan’ın resmî sitesinin yönlendirdiği müzik klibi. YouTube oynatıcısında açılır.", poster = "", backdrop = "", year = "2001", score = 0.0, genreIds = emptyList(), provider = "Tarkan", webUrl = "https://www.youtube.com/watch?v=NAHRpEqgcL4", category = "Müzik"),
    Movie(7301, "Umudunu Kaybetme", "The Pursuit of Happyness. Will Smith’in başrolünde, oğluyla birlikte zor koşullarda hayata tutunmaya çalışan bir babanın hikâyesi. Yaklaşık 117 dakika. Türkçe dublaj (kaynak etiketi). Görüntü: Çözünürlük doğrulanamadı.", "file:///android_asset/umudunu-kaybetme-the-pursuit-of-happyness-turkce-dublaj-1080p-full-hd-izle_202405.jpg", "", "2006", 0.0, listOf(18), "https://archive.org/download/umudunu-kaybetme-the-pursuit-of-happyness-turkce-dublaj-1080p-full-hd-izle_202405/Umudunu%20Kaybetme%20%28The%20Pursuit%20of%20Happyness%29%20T%C3%BCrk%C3%A7e%20dublaj%201080p%20Full%20HD%20izle.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7302, "Vikingler", "The Vikings. Kirk Douglas ve Tony Curtis’in rol aldığı tarihî macera. Yaklaşık 116 dakika. Türkçe dublaj (kaynak etiketi). Görüntü: 852 × 480.", "file:///android_asset/20210524_20210524_0931.jpg", "", "1958", 0.0, listOf(12,28), "https://archive.org/download/20210524_20210524_0931/Vikingler.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7201, "Arizona Haydutları", "Robbers’ Roost. Bir çiftlikte karşı karşıya gelen haydut grupları üzerine bir western. Yaklaşık 80 dakika. Kaynak kaydı: Türkçe dublaj. Ses dili kaynaktaki etikete dayanır.", "file:///android_asset/arizona-haydutlari-1955.jpg", "", "1955", 0.0, listOf(37), "https://archive.org/download/arizona-haydutlari-1955/Arizona%20Haydutlar%C4%B1%20%281955%29.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7202, "30 Winchester per El Diablo", "Meksika’ya taşınacak altının peşindeki haydutlar arasında geçen western. Yaklaşık 85 dakika. Kaynak kaydı: Türkçe dublaj. Ses dili kaynaktaki etikete dayanır.", "file:///android_asset/30-winchester-per-el-diablo.jpg", "", "1965", 0.0, listOf(37), "https://archive.org/download/30-winchester-per-el-diablo/30%20Winchester%20per%20El%20Diablo.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7203, "Yalnız Kovboy", "Ride Lonesome. Bir ödül avcısının tutsağıyla çıktığı tehlikeli yolculuk. Yaklaşık 73 dakika. Kaynak kaydı: Türkçe dublaj. Ses dili kaynaktaki etikete dayanır.", "file:///android_asset/yalniz-kovboy-ride-lonesome-1959-turkce-dublaj-tek-parca-full-film-izle-otukenim.tv.jpg", "", "1959", 0.0, listOf(37), "https://archive.org/download/yalniz-kovboy-ride-lonesome-1959-turkce-dublaj-tek-parca-full-film-izle-otukenim.tv/Yaln%C4%B1z%20Kovboy%20%E2%80%93%20Ride%20Lonesome%201959%20%E2%80%93%20T%C3%BCrk%C3%A7e%20Dublaj%20Tek%20Par%C3%A7a%20Full%20Film%20izle%20-%20Otukenim.tv.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive")
) + importedCatalog + ultraCatalog + fullFilmCatalog + galatasaray2025Catalog + fullHdCatalog + yerliDiziCatalog + extraFullHdCatalog + addedSeriesCatalog + belgeselCatalog + izle7Catalog + searchImportCatalog + youtubeImportCatalog + filmPagesCatalog + youngSheldonCatalog + hdSeriesCatalog + hdBatchCatalog + rFilmCatalog + ddiziCatalog + htmlBatch49Catalog + natureEmpireCatalog + diziBoxCatalog

// Wikimedia Commons API: openly licensed media metadata and direct downloadable video URLs.
// This code does not extract third-party player URLs or circumvent access controls.
private suspend fun searchCommons(term: String): List<Movie> = withContext(Dispatchers.IO) {
    if (term.isBlank()) return@withContext emptyList()
    val encoded = URLEncoder.encode("filetype:video " + term.trim(), "UTF-8")
    val endpoint = "https://commons.wikimedia.org/w/api.php?action=query&generator=search" +
        "&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=30&prop=imageinfo" +
        "&iiprop=url%7Cmime%7Cextmetadata&iiurlwidth=320&format=json&formatversion=2"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 12000
        connection.readTimeout = 16000
        connection.setRequestProperty("User-Agent", "ALICOTV/0.23 (Android TV open-license video discovery)")
        if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
        val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val pages = JSONObject(raw).optJSONObject("query")?.optJSONArray("pages")
            ?: return@withContext emptyList()
        val result = mutableListOf<Movie>()
        for (i in 0 until pages.length()) {
            val page = pages.optJSONObject(i) ?: continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val mime = info.optString("mime")
            if (mime != "video/mp4" && mime != "video/webm") continue
            val url = info.optString("url")
            if (!validStreamUrl(url) || Uri.parse(url).host?.endsWith("wikimedia.org") != true) continue
            val meta = info.optJSONObject("extmetadata")
            val license = meta?.optJSONObject("LicenseShortName")?.optString("value").orEmpty()
            if (license.isBlank() || license.equals("unknown", true)) continue
            val title = page.optString("title").removePrefix("File:")
                .replace(Regex("\\.(webm|mp4)$", RegexOption.IGNORE_CASE), "")
                .replace('_', ' ')
            val id = (page.optInt("pageid") + 1000000)
            result.add(Movie(id, title, "Wikimedia Commons üzerinden bulunan açık lisanslı video. Lisans: $license. Kaynak: Wikimedia Commons.",
                "", "", "", 0.0, emptyList(), url, license, "Wikimedia Commons"))
        }
        result
    } finally {
        connection.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    private var historyRevision by mutableIntStateOf(0)
    private var backupBusy by mutableStateOf(false)
    private var backupMessage by mutableStateOf("")
    private fun runBackup(uri: Uri, restore: Boolean) {
        if (backupBusy) return
        backupBusy = true
        lifecycleScope.launch {
            try {
                ArchiveSync.storageOperation {
                    if (restore) AppBackup.restore(this@MainActivity,uri)
                    else AppBackup.export(this@MainActivity,uri)
                }
                backupMessage = if (restore) "Yedek geri yüklendi." else "Yedek kaydedildi. Dosyayı uygulama dışında saklayın."
                android.widget.Toast.makeText(this@MainActivity,backupMessage,android.widget.Toast.LENGTH_LONG).show()
                if (restore) recreate()
            } catch (e: Exception) {
                backupMessage = "İşlem tamamlanamadı: " + (e.message ?: "Dosyaya erişilemedi.")
            } finally { backupBusy = false }
        }
    }
    private val backupExport = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runBackup(uri,false) else backupBusy=false
    }
    private val backupImport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) android.app.AlertDialog.Builder(this)
            .setTitle("Yedeği geri yükle")
            .setMessage("Mevcut ek arşiv, favoriler, izleme geçmişi ve takip kayıtları seçtiğiniz yedekle değiştirilecek.")
            .setPositiveButton("Geri yükle") { _, _ -> runBackup(uri,true) }
            .setNegativeButton("Vazgeç",null).show()
    }
    private fun chooseBackup(restore: Boolean) {
        try {
            if (restore) backupImport.launch(arrayOf("application/json","text/plain","application/octet-stream"))
            else backupExport.launch("ALICOTV-yedek-" + java.text.SimpleDateFormat("yyyyMMdd-HHmmss",java.util.Locale.ROOT).format(java.util.Date()) + ".json")
        } catch (_: android.content.ActivityNotFoundException) {
            backupMessage = "Bu cihazda sistem dosya seçicisi bulunamadı. Yedekleme için Android dosya seçicisi gerekir."
        }
    }
    override fun onResume() { super.onResume(); historyRevision++ }

    private val prefs by lazy { getSharedPreferences("cinebox", Context.MODE_PRIVATE) }
    private fun openPublisherPage(url: String) {
        try {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(this, "Tarayıcı bulunamadı. Uygulamada aç seçeneğini kullanın.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArchiveSync.announceBundle(this)
        ArchiveSync.announceGuldur(this)
        ArchiveSync.announceDiziBox(this)
        ArchiveSync.announceFilmizleHell(this)
        setContent { App() }
    }

    @Composable
    private fun App() {
        var liveCatalog by remember { mutableStateOf(ArchiveSync.load(this@MainActivity,catalogMovies)) }
        val followed = remember { mutableStateListOf<Int>().apply { addAll(SeriesTracking.following(this@MainActivity)) } }
        val syncScope = rememberCoroutineScope()
        var syncing by remember { mutableStateOf(false) }
        var syncMessage by remember { mutableStateOf("") }
        var episodeRevision by remember { mutableIntStateOf(0) }
        var showNotices by remember { mutableStateOf(ArchiveSync.hasUnread(this@MainActivity)) }
        var showSources by remember { mutableStateOf(false) }
        suspend fun syncArchive(force: Boolean) {
            if(syncing || backupBusy)return
            syncing=true
            try {
                val before=ArchiveSync.notices(this@MainActivity)
                syncMessage=ArchiveSync.update(this@MainActivity,catalogMovies,force)
                for (item in liveCatalog.filter { it.id in followed }) {
                    ArchiveSync.refreshEpisodes(this@MainActivity,item)
                }
                if(ArchiveSync.notices(this@MainActivity)!=before)showNotices=true
                liveCatalog=ArchiveSync.load(this@MainActivity,catalogMovies)
                episodeRevision++
            } catch(e:Exception) { syncMessage="Güncelleme tamamlanamadı; kayıtlı arşiv kullanılabilir." }
            finally { syncing=false }
        }
        LaunchedEffect(Unit) { syncArchive(false) }
        if(showNotices) AlertDialog(onDismissRequest={showNotices=false; ArchiveSync.markRead(this@MainActivity)},title={Text("Arşiv bildirimleri")},text={Text(ArchiveSync.notices(this@MainActivity),modifier=Modifier.verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={showNotices=false; ArchiveSync.markRead(this@MainActivity)}){Text("Kapat")}})

        if(showSources) AlertDialog(onDismissRequest={showSources=false},title={Text("Güncellenen kaynaklar")},text={Text(ArchiveSync.sourcesDescription(),modifier=Modifier.verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={showSources=false}){Text("Kapat")}})
        val recentIds = remember(liveCatalog, episodeRevision) { ArchiveSync.recentIds(this@MainActivity) }
        val recentMovies = remember(liveCatalog, recentIds) { val index=liveCatalog.associateBy { it.id }; recentIds.mapNotNull { index[it] } }
        var category by remember { mutableStateOf("Tümü") }
        var tab by remember { mutableStateOf("Ana Sayfa") }
        var query by remember { mutableStateOf("") }
        var page by remember { mutableIntStateOf(0) }
        var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var recommended by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var selected by remember { mutableStateOf<Movie?>(null) }
        var playing by remember { mutableStateOf<PlayRequest?>(null) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("") }
        val favorites = remember { mutableStateListOf<Int>().apply { addAll(prefs.getString("favorites", "")!!.split(",").mapNotNull { it.toIntOrNull() }) } }
        val ratings = remember { mutableStateMapOf<Int, Int>().apply {
            prefs.all.forEach { (key, value) -> if (key.startsWith("rating_") && value is Int) key.removePrefix("rating_").toIntOrNull()?.let { put(it, value) } }
        } }
        val savedMovies = remember { mutableStateMapOf<Int, Movie>().apply { liveCatalog.forEach { put(it.id, it) } } }

        fun showMovies(list: List<Movie>) {
            movies = list
            list.forEach { savedMovies[it.id] = it }
            busy = false
            message = if (list.isEmpty()) "Film bulunamadı." else ""
        }
        LaunchedEffect(liveCatalog) { showMovies(liveCatalog) }
        LaunchedEffect(selected?.id) {
            selected?.let { item ->
                try {
                    val result=ArchiveSync.refreshEpisodes(this@MainActivity,item)
                    if(result.isNotBlank()) { syncMessage=result; android.widget.Toast.makeText(this@MainActivity,result,android.widget.Toast.LENGTH_LONG).show() }
                    episodeRevision++
                } catch(e:Exception) { syncMessage="Bölümler güncellenemedi; kayıtlı liste korunuyor." }
            }
        }
        LaunchedEffect(selected?.id) {
            val genre = selected?.genreIds?.firstOrNull()
            recommended = liveCatalog.filter { it.id != selected?.id && genre != null && genre in it.genreIds }
        }
        BackHandler(enabled = playing != null || selected != null) {
            if (playing != null) playing = null else selected = null
        }
        MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = accent)) {
            when {
                playing != null -> PlayerScreen(playing!!, prefs) { playing = null }
                tab == "İzlemeye Devam" && selected == null -> {
                    val history = remember(historyRevision) { WatchHistory.list(this@MainActivity) }
                    Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TvAction(onClick = { tab = "Ana Sayfa" }) { Text("← Ana sayfa") }
                        Text("İzlemeye Devam", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Son izlediklerin · Kaldığın yerden devam et", color = accent)
                        if (history.isEmpty()) Text("İzlediğin içerikler burada görünecek.", color = Color.White)
                        history.forEach { entry ->
                            Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(14.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(entry.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                if (entry.episode.isNotBlank() && entry.episode != entry.title) Text(entry.episode, color = Color.LightGray)
                                Text(if (entry.position >= 0) "${WatchHistory.time(entry.position)} konumunda kaldınız" else "Son açılan içerik · Süre bilgisi alınamadı", color = accent)
                                if (entry.duration > 0 && entry.position >= 0) {
                                    LinearProgressIndicator(progress = { (entry.position.toFloat()/entry.duration).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                                    Text("Toplam ${WatchHistory.time(entry.duration)}", color = Color.LightGray)
                                }
                                Text(java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("tr", "TR")).format(java.util.Date(entry.updated)), color = Color.LightGray)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TvAction(onClick = {
                                        if (entry.web) startActivity(android.content.Intent(this@MainActivity, WebPlayerActivity::class.java)
                                            .putExtra("url",entry.url).putExtra("title",entry.title).putExtra("movie_id",entry.id)
                                            .putExtra("resume_ms",entry.position.coerceAtLeast(0)))
                                        else playing = PlayRequest(entry.id,entry.title,entry.url,prefs.getString("subtitle_${entry.id}","").orEmpty())
                                    }) { Text(if (entry.position >= 0) "▶ Devam et" else "▶ Yeniden aç") }
                                    TvAction(onClick = { WatchHistory.remove(this@MainActivity,entry.url); historyRevision++ }) { Text("Listeden kaldır") }
                                }
                            }
                        }
                    }
                }
                selected != null -> {
                    val m = selected!!
                    val offlineEpisodes = remember(m.id,episodeRevision,historyRevision) { SeriesTracking.episodes(this@MainActivity,m) }
                    val availableSeasons = offlineEpisodes.map { it.season }.distinct().sorted()
                    val continuousEpisodes = m.id in ddiziContinuousIds || (m.webUrl.contains("ddizi.tel") && availableSeasons==listOf(1))
                    var pickedSeason by remember(m.id) { mutableIntStateOf(availableSeasons.firstOrNull() ?: 1) }
                    LaunchedEffect(availableSeasons) { if(availableSeasons.isNotEmpty() && pickedSeason !in availableSeasons) pickedSeason=availableSeasons.first() }
                    val detail = remember(m.webUrl) { ContentDetails.get(this@MainActivity,m.webUrl) }
                    var advanced by remember(m.id) { mutableStateOf(false) }
                    val wideDetails = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 650

                    val playFocus = remember(m.id) { FocusRequester() }
                    LaunchedEffect(m.id) { if (m.webUrl.isNotBlank() || m.playableUrl.isNotBlank()) playFocus.requestFocus() }
                    var customUrl by remember(m.id) { mutableStateOf(prefs.getString("source_${m.id}", "").orEmpty()) }
                    var subtitleUrl by remember(m.id) { mutableStateOf(prefs.getString("subtitle_${m.id}", "").orEmpty()) }
                    var sourceError by remember(m.id) { mutableStateOf("") }
                    Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF25140C), Color(0xFF101014)))).verticalScroll(rememberScrollState()).padding(24.dp)) {
                        TvAction(onClick = { selected = null }) { Text("← Geri") }
                        Row(horizontalArrangement = Arrangement.spacedBy(25.dp)) {
                            if (wideDetails) Box(Modifier.width(190.dp).height(285.dp).background(panel, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("▶",color=accent,fontSize=48.sp)
                                    Text(m.title,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                                    Text(m.category,color=accent,fontSize=13.sp)
                                }
                                if (m.poster.isNotBlank()) AsyncImage(model=m.poster,contentDescription=m.title,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(m.title, color = Color.White, fontSize = if (wideDetails) 36.sp else 27.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(m.category, m.year, if(m.score>0) "★ ${"%.1f".format(m.score)}" else "", detail.optString("duration").let { if(it.isNotBlank()&&it!="0") "$it dk" else "" }).filter { it.isNotBlank() }.forEach { badge ->
                                        Text(badge,color=accent,modifier=Modifier.background(Color(0xFF3B291E),RoundedCornerShape(6.dp)).padding(8.dp))
                                    }
                                }
                                val genericOverview = m.overview.contains("HTML") || m.overview.startsWith("Film sayfasını") || m.overview.startsWith("Bölümleri dizi") || m.overview.startsWith("Uygulama içi web")
                                Text("Hakkında",color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)
                                Text(if(genericOverview || m.overview.isBlank()) "Bu içerik için konu özeti henüz eklenmemiş. Mevcut yapım bilgileri aşağıda yer alıyor." else m.overview,color=Color(0xFFE0DAD5),fontSize=16.sp)
                                Column(Modifier.fillMaxWidth().background(Color(0xFF231F20),RoundedCornerShape(12.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    val fields = listOf("Tür" to detail.optString("type"),"Süre" to detail.optString("duration").let { if(it.isNotBlank()&&it!="0") "$it dakika" else "" },"Oyuncular" to detail.optString("actor"),"Yönetmen" to detail.optString("director"),"Ülke" to detail.optString("country"),"Yaş sınıflandırması (kaynak)" to detail.optString("derecelendirme"),"Kaynak" to m.provider)
                                    fields.filter { it.second.isNotBlank() }.forEach { (label,value) ->
                                        Text("$label: ${value.replace(",", ", ")}",color=Color.LightGray,fontSize=14.sp)
                                    }
                                    if (detail.length()==0) Text("Ek yapım bilgisi kaynak dosyada bulunmuyor.",color=Color.LightGray,fontSize=13.sp)
                                }
                                if (m.webUrl.isNotBlank() && m.id !in 970000..970015 && offlineEpisodes.isEmpty()) TvAction(modifier = Modifier.focusRequester(playFocus), onClick = {
                                    startActivity(android.content.Intent(this@MainActivity, WebPlayerActivity::class.java)
                                        .putExtra("movie_id", m.id).putExtra("series", m.category in listOf("Dizi", "Yerli Diziler")).putExtra("url", m.webUrl).putExtra("title", m.title))
                                }) { Text(if (m.category == "Maç Özeti") "▶ Maç özetini aç" else if (m.id in 960001..960006) "▶ Dizi bölümlerini aç" else if(m.category in listOf("Dizi","Yerli Diziler")) "▶ Bölümleri aç" else "▶ Şimdi izle") }
                                if (m.webUrl.isNotBlank()) TvAction(modifier = if (m.id in 970000..970015) Modifier.focusRequester(playFocus) else Modifier, onClick = {
                                    openPublisherPage(m.webUrl)
                                }) { Text(if (m.id in 970000..970015) "Resmî program bilgisini aç" else if (m.id == 7501) "Tüm bölümleri tarayıcıda aç" else "Tarayıcıda aç") }
                                if (m.category in listOf("Dizi", "Yerli Diziler")) {
                                    TvAction(onClick = {
                                        val enable = m.id !in followed
                                        SeriesTracking.follow(this@MainActivity,m.id,enable)
                                        if (enable) followed.add(m.id) else followed.remove(m.id)
                                    }) { Text(if (m.id in followed) "✓ Takip ediliyor · Takibi bırak" else "+ Diziyi takip et") }
                                }
                                if (offlineEpisodes.isNotEmpty()) {
                                    Text(if(continuousEpisodes) "Bölümler" else "Sezonlar ve Bölümler", color=Color.White, fontSize=22.sp, fontWeight=FontWeight.Bold)
                                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        availableSeasons.forEach { season ->
                                            TvAction(modifier=if(season==availableSeasons.first()) Modifier.focusRequester(playFocus) else Modifier,
                                                onClick={pickedSeason=season}) { Text(if(continuousEpisodes) "Tüm bölümler" else if(pickedSeason==season) "✓ $season. Sezon" else "$season. Sezon") }
                                        }
                                    }
                                    offlineEpisodes.filter { it.season==pickedSeason }.forEach { episode ->
                                        TvAction(onClick={
                                            startActivity(android.content.Intent(this@MainActivity,WebPlayerActivity::class.java)
                                                .putExtra("movie_id",m.id).putExtra("url",episode.url)
                                                .putExtra("title",if(continuousEpisodes) "${m.title} · ${episode.episode}. Bölüm" else "${m.title} · ${episode.season}. Sezon ${episode.episode}. Bölüm"))
                                        }) { Text(SeriesTracking.label(this@MainActivity,m.id,episode.url) + if(continuousEpisodes) "▶ ${episode.episode}. Bölüm" else "▶ ${episode.season}. Sezon · ${episode.episode}. Bölüm") }
                                    }
                                }
                                (seriesExtraLinks[m.id].orEmpty() + rFilmSeriesLinks[m.id].orEmpty()).distinctBy { it.url }.forEach { link ->
                                    TvAction(onClick = {
                                        startActivity(android.content.Intent(this@MainActivity, WebPlayerActivity::class.java)
                                            .putExtra("movie_id", m.id).putExtra("series", true).putExtra("url", link.url).putExtra("title", m.title + " · " + link.label))
                                    }) { Text(link.label) }
                                }
                                if (m.id == 7502) {
                                    TvAction(onClick = { openPublisherPage("https://www.youtube.com/watch?v=GgAkIcXFinA") }) {
                                        Text("1999 · 1. bölüm — YouTube’da aç")
                                    }
                                    Text("Arşiv yüklemesi: Dizi Arşivi. Bölümler tek tek oynatılarak doğrulanmadı.", color = Color.LightGray, fontSize = 12.sp)
                                }
                                if (m.id == 7501) {
                                    Text("Sezon seç", color = Color.White, fontWeight = FontWeight.Bold)
                                    (1..5).forEach { season ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val seasonUrl = "https://www.kanald.com.tr/yaprakdokumu/bolumler?activeSeasonNumber=$season&seasonCount=5"
                                            TvAction(onClick = { openPublisherPage(seasonUrl) }) { Text("$season. Sezon · Tarayıcıda aç") }
                                            TvAction(onClick = {
                                                startActivity(android.content.Intent(this@MainActivity, WebPlayerActivity::class.java)
                                                    .putExtra("movie_id", m.id).putExtra("series", true).putExtra("url", seasonUrl).putExtra("title", "Yaprak Dökümü · $season. Sezon"))
                                            }) { Text("Uygulamada aç") }
                                        }
                                    }
                                }
                                if (m.playableUrl.isNotBlank()) TvAction(modifier = if (m.webUrl.isBlank()) Modifier.focusRequester(playFocus) else Modifier, onClick = { playing = PlayRequest(m.id, m.title, m.playableUrl) }) { Text("▶ Filmi oynat") }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TvAction(onClick = {
                                        if (favorites.contains(m.id)) favorites.remove(m.id) else favorites.add(m.id)
                                        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
                                    }) { Text(if (favorites.contains(m.id)) "♥ Favorilerden çıkar" else "♡ Favorilere ekle") }
                                }
                                Spacer(Modifier.height(8.dp))
                                TvAction(onClick = { advanced = !advanced }) { Text(if(advanced) "− Gelişmiş seçenekleri kapat" else "+ Bağlantı ve puanlama seçenekleri") }
                                if (advanced) {
                                Text("ALICOTV Player · Yayın kaynakları", color = Color.White, fontWeight = FontWeight.Bold)
                                if (m.playableUrl.isBlank() && m.webUrl.isBlank() && customUrl.isBlank()) {
                                    Text("Bu film için oynatma bağlantısı henüz eklenmedi. Bu kart yalnızca katalog kaydıdır.", color = Color(0xFFFFD180))
                                }
                                Text("Kullanma hakkınız olan HTTPS MP4 / WebM / M3U8 / MPD adresini ekleyin.", color = Color.LightGray, fontSize = 12.sp)
                                TextField(value = customUrl, onValueChange = { customUrl = it }, label = { Text("Video bağlantısı (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                TextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Altyazı bağlantısı (isteğe bağlı, HTTPS .vtt)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                TvAction(onClick = {
                                    val sub = subtitleUrl.trim()
                                    if (!validStreamUrl(customUrl)) sourceError = "HTTPS ile başlayan MP4, WebM, M3U8 veya MPD adresi girin."
                                    else if (sub.isNotEmpty() && !(sub.startsWith("https://") && Uri.parse(sub).path.orEmpty().lowercase().endsWith(".vtt"))) sourceError = "Altyazı HTTPS .vtt formatında olmalı."
                                    else {
                                        sourceError = ""
                                        prefs.edit().putString("source_${m.id}", customUrl.trim()).putString("subtitle_${m.id}", sub).apply()
                                        playing = PlayRequest(m.id, m.title, customUrl.trim(), sub)
                                    }
                                }) { Text("▶ Bağlantıyı kaydet ve oynat") }
                                if (sourceError.isNotBlank()) Text(sourceError, color = Color(0xFFFF9292))
                                Text("Kişisel puanım: ${ratings[m.id] ?: "Henüz yok"}", color = Color.White)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    (1..10).forEach { n ->
                                        TvAction(onClick = { ratings[m.id] = n; prefs.edit().putInt("rating_${m.id}", n).apply() }, contentPadding = PaddingValues(6.dp), modifier = Modifier.width(44.dp)) { Text("$n") }
                                    }
                                }
                                Text("Her film kendi kaynak bağlantısıyla oynatılır. Türkçe altyazı isterseniz VTT bağlantısı ekleyebilirsiniz.", color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        TvAction(enabled=!syncing,onClick={syncScope.launch { syncArchive(true) }}) { Text(if(syncing) "Güncelleniyor…" else "Arşivi güncelle") }
                        TvAction(onClick={showNotices=true}) { Text("Bildirimler") }
                    }
                    if(syncMessage.isNotBlank()) Text(syncMessage,color=Color.White)
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TvAction(onClick={selected=null; tab="Yeni eklenenler"; category="Tümü"; query=""; page=0}) { Text("Yeni eklenenler (${recentIds.size})") }
                        TvAction(onClick={showSources=true}) { Text("Güncellenen kaynaklar") }
                    }
                    Text("Uygulama açıldığında 12 saatte bir kontrol edilir. Arşivi güncelle ile hemen kontrol edebilirsiniz.",color=Color.LightGray,fontSize=12.sp)
                    recentMovies.take(3).forEach { item ->
                        TvAction(onClick={selected=item}) { Text("Yeni · ${item.title}",maxLines=1) }
                    }
                    Spacer(Modifier.height(25.dp))
                        Text("Bunları da beğenebilirsin", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        MovieRow(recommended.filter { it.id != m.id }) { selected = it }
                    }
                }
                else -> Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvAction(enabled=!syncing && !backupBusy,onClick={chooseBackup(false)}) { Text("Arşivi yedekle") }
                        TvAction(enabled=!syncing && !backupBusy,onClick={chooseBackup(true)}) { Text("Yedeği geri yükle") }
                    }
                    if (backupBusy) Text("Yedek işlemi sürüyor…",color=Color.White)
                    if (backupMessage.isNotBlank()) Text(backupMessage,color=Color.White)
                    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(R.drawable.alico_wordmark), contentDescription = "ALICOTV", modifier = Modifier.width(160.dp).height(52.dp))
                        listOf("Ana Sayfa", "Ara", "Favoriler", "Öneriler", "İzlemeye Devam", "Takip Ettiklerim", "Yeni eklenenler").forEach { label ->
                            TvAction(onClick = {
                                tab = label
                                if (label in listOf("Takip Ettiklerim", "Yeni eklenenler")) { category = "Tümü"; query = "" }
                                page = 0
                                when (label) {
                                    "Ana Sayfa", "Ara" -> showMovies(liveCatalog)
                                    "Öneriler" -> {
                                        val preferred = savedMovies.values.filter { (ratings[it.id] ?: 0) >= 7 }.flatMap { it.genreIds }.toSet()
                                        showMovies(liveCatalog.filter { preferred.isEmpty() || it.genreIds.any { genre -> genre in preferred } })
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = if (tab == label) accent else panel)) { Text(label) }
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        TvAction(enabled=!syncing,onClick={syncScope.launch { syncArchive(true) }}) { Text(if(syncing) "Güncelleniyor…" else "Arşivi güncelle") }
                        TvAction(onClick={showNotices=true}) { Text("Bildirimler") }
                    }
                    if(syncMessage.isNotBlank()) Text(syncMessage,color=Color.White)
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TvAction(onClick={selected=null; tab="Yeni eklenenler"; category="Tümü"; query=""; page=0}) { Text("Yeni eklenenler (${recentIds.size})") }
                        TvAction(onClick={showSources=true}) { Text("Güncellenen kaynaklar") }
                    }
                    Text("Uygulama açıldığında 12 saatte bir kontrol edilir. Arşivi güncelle ile hemen kontrol edebilirsiniz.",color=Color.LightGray,fontSize=12.sp)
                    recentMovies.take(3).forEach { item ->
                        TvAction(onClick={selected=item}) { Text("Yeni · ${item.title}",maxLines=1) }
                    }
                    Spacer(Modifier.height(25.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(value = query, onValueChange = { query = it; page = 0 },
                            label = { Text("İçerik adı ara · $category") }, singleLine = true,
                            modifier = Modifier.weight(1f))
                        TvAction(onClick = { query = ""; page = 0 }) { Text("Temizle") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(when (tab) { "Yeni eklenenler" -> "Yeni eklenen içerikler"; "Ara" -> "Arama sonuçları"; "Favoriler" -> "Favori filmlerim"; "Öneriler" -> "Sana özel öneriler"; else -> "Film, dizi, belgesel, spor, müzik ve eğlence" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (busy) CircularProgressIndicator()
                    if (message.isNotBlank()) Text(message, color = Color.LightGray)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tümü", "Filmler", "Diziler", "Yerli Diziler", "Belgesel", "Maç Özeti", "Müzik", "Eğlence").forEach { label ->
                            TvAction(onClick = { category = label; page = 0 }) { Text(if (category == label) "✓ $label" else label) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val baseMovies = if (tab == "Yeni eklenenler") recentMovies else if (tab == "Takip Ettiklerim") liveCatalog.filter { it.id in followed } else if (tab == "Favoriler") savedMovies.values.filter { favorites.contains(it.id) } else movies
                    val shown = remember(baseMovies, category, query) {
                        val searchText = query.trim().lowercase(java.util.Locale("tr", "TR"))
                        baseMovies.filter {
                        (category == "Tümü" || (category == "Filmler" && it.category == "Film") ||
                         (category == "Diziler" && it.category in listOf("Dizi", "Yerli Diziler")) || it.category == category) &&
                        it.title.lowercase(java.util.Locale("tr", "TR")).contains(searchText)
                    }.sortedWith(compareBy(java.text.Collator.getInstance(java.util.Locale("tr", "TR"))) { it.title })
                    }
                    val pageSize = 20
                    val pageCount = maxOf(1, (shown.size + pageSize - 1) / pageSize)
                    val currentPage = page.coerceIn(0, pageCount - 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvAction(enabled = currentPage > 0, onClick = { page = currentPage - 1 }) { Text("◀ Önceki") }
                        Text("${shown.size} içerik · Sayfa ${currentPage + 1} / $pageCount", color = Color.White)
                        TvAction(enabled = currentPage + 1 < pageCount, onClick = { page = currentPage + 1 }) { Text("Sonraki ▶") }
                    }
                    Spacer(Modifier.height(12.dp))
                    key(category, query, tab, currentPage) {
                        MovieRow(shown.drop(currentPage * pageSize).take(pageSize)) { selected = it }
                    }
                    if (shown.isEmpty()) Text("Bu kategoride aramanızla eşleşen içerik bulunamadı.", color = Color.White)
                    Spacer(Modifier.height(20.dp))
                    Text("MP4 içerikler ALICOTV Player’da; web içerikleri yayıncının oynatıcısında açılır. İsim araması seçili kategori içinde yapılır.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MovieRow(movies: List<Movie>, onClick: (Movie) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 22.dp)) {
        items(movies.sortedWith(compareBy(java.text.Collator.getInstance(java.util.Locale("tr", "TR"))) { it.title }), key = { it.id }) { movie ->
            Column(Modifier.width(180.dp)) {
                var cardFocused by remember { mutableStateOf(false) }
                Surface(modifier = Modifier.onFocusChanged { cardFocused = it.isFocused }.border(if (cardFocused) 4.dp else 1.dp, if (cardFocused) Color.Yellow else Color.Transparent, RoundedCornerShape(11.dp)), onClick = { onClick(movie) }, shape = RoundedCornerShape(11.dp), color = panel) {
                    Column {
                        Text(movie.title, color = Color.White, fontSize = 15.sp, minLines = 2, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                        if (movie.poster.isNotEmpty()) AsyncImage(model = movie.poster, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp))
                        else Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text(movie.title, color = Color.LightGray, modifier = Modifier.padding(12.dp)) }
                        Text(if (movie.id in 970000..970015) "Belgesel · Program bilgisi" else if (movie.webUrl.isNotBlank()) "${movie.category} · Web oynatıcı" else if ((movie.id in 7201..7203 || movie.id in 7301..7302)) "${movie.year} · Türkçe dublaj" else if (movie.provider.isNotEmpty()) "${movie.provider} · ${movie.license}" else "${movie.year}  ★ ${"%.1f".format(movie.score)}", color = Color.LightGray, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(request: PlayRequest, prefs: android.content.SharedPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    var immersive by remember { mutableStateOf(false) }
    val playerView = remember { mutableStateOf<PlayerView?>(null) }
    DisposableEffect(immersive) {
        val window = (context as android.app.Activity).window
        window.decorView.systemUiVisibility = if (immersive) 5894 else 0
        if (immersive) { playerView.value?.hideController(); playerView.value?.requestFocus() }
        onDispose { window.decorView.systemUiVisibility = 0 }
    }
    BackHandler(immersive) { immersive = false }
    var status by remember(request.url) { mutableStateOf("") }
    var retryNumber by remember(request.url) { mutableStateOf(0) }
    val activeUrl = request.url
    // Sources may contain different edits of a film: never reuse another URL's position.
    val progressKey = remember(request.movieId, activeUrl) {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(activeUrl.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        "progress_v05_${request.movieId}_$digest"
    }
    val player = remember(activeUrl, request.subtitleUrl, retryNumber) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("ALICOTV/0.23 (Android TV)")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory)))
            .build().apply {
            val item = MediaItem.Builder().setUri(activeUrl)
            if (request.subtitleUrl.isNotBlank()) {
                item.setSubtitleConfigurations(listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(request.subtitleUrl))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage("tr")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                ))
            }
            setMediaItem(item.build())
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val httpError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                    val detail = if (httpError != null) {
                        val explanation = when (httpError.responseCode) {
                            401, 403 -> "Sunucu erişimi reddetti."
                            404, 410 -> "Video bağlantısı bulunamadı veya kaldırıldı."
                            416 -> "Sunucu istenen video konumunu kabul etmedi."
                            429 -> "Sunucu çok fazla istek nedeniyle beklemenizi istiyor."
                            in 500..599 -> "Video sunucusunda geçici hata var."
                            else -> "Sunucu video isteğini kabul etmedi."
                        }
                        "HTTP ${httpError.responseCode}: $explanation"
                    } else error.errorCodeName
                    val host = Uri.parse(activeUrl).host.orEmpty()
                        status = "Video açılamadı. $detail\nKaynak: $host\nKod: ${error.errorCodeName}"
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) status = ""
                }
            })
            prepare()
            val savedPosition = prefs.getLong(progressKey, 0L)
            if (savedPosition > 0) seekTo(savedPosition)
            playWhenReady = true
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(2500)
            if (player.duration > 0 && player.currentPosition > 0) {
                WatchHistory.save(context,WatchEntry(request.movieId,request.title,activeUrl,false,player.currentPosition,player.duration))
                if (player.playbackState == Player.STATE_ENDED) {
                    prefs.edit().remove(progressKey).apply()
                } else {
                    prefs.edit().putLong(progressKey, player.currentPosition).apply()
                }
            }
        }
    }
    DisposableEffect(player) {
        onDispose {
            if (player.playbackState == Player.STATE_ENDED) prefs.edit().remove(progressKey).apply()
            else if (player.currentPosition > 0) prefs.edit().putLong(progressKey, player.currentPosition).apply()
            if (player.currentPosition > 0) WatchHistory.save(context,WatchEntry(request.movieId,request.title,activeUrl,false,player.currentPosition,player.duration.coerceAtLeast(0)))
            player.release()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply {
                this.player = player
                playerView.value = this
                useController = true
                controllerShowTimeoutMs = 6000
                controllerAutoShow = true
                keepScreenOn = true
            } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            if (!immersive) TvAction(onClick = onBack) { Text("← Film detayına dön") }
            TvAction(onClick = { immersive = !immersive }) { Text(if (immersive) "Tam ekrandan çık" else "⛶ Tam ekran") }
            if (status.isNotEmpty()) {
                Text(status, color = Color(0xFFFFAAAA), modifier = Modifier.background(Color.Black).padding(8.dp))
                TvAction(onClick = {
                    status = "Yeniden bağlanılıyor…"
                    retryNumber += 1
                }) { Text("Yeniden dene") }
                TvAction(onClick = {
                    prefs.edit().remove(progressKey).apply()
                    player.seekTo(0)
                    status = "Baştan deneniyor…"
                    retryNumber += 1
                }) { Text("Baştan dene") }
            }
        }
        if (!immersive) Text(request.title, color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
    }
}

@Composable
private fun TvAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = panel, contentColor = Color.White),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Button(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp).onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 4.dp else 1.dp, if (focused) Color.White else Color(0xFFFFCC80)),
        colors = if (focused) ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) else colors,
        contentPadding = contentPadding, content = content)
}
