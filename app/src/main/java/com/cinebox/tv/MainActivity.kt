package com.cinebox.tv

import android.os.Bundle
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.C
import kotlinx.coroutines.delay
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

private val bg = Color(0xFF090D18)
private val panel = Color(0xFF192034)
private val accent = Color(0xFF8B68FF)
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
private val catalogMovies = listOf(
    Movie(id = 7502, title = "Ayrılsak da Beraberiz (1999)", overview = "Janset ve Hakan Yılmaz’lı eski seri. Bağlı arşiv sayfasında 40 bölüm listeleniyor; tüm seri veya yalnız 1999 yılında yayımlanan bölümler olarak doğrulanmadı. 2015 uyarlaması değildir.", poster = "", backdrop = "", year = "1999", score = 0.0, genreIds = listOf(35), provider = "Diziizle — üçüncü taraf arşiv", webUrl = "https://www.diziizle.you/ayrilsak-da-beraberiz-eski-bolumler/", category = "Türk Dizileri"),
    Movie(id = 7501, title = "Yaprak Dökümü", overview = "Kanal D’nin resmî bölüm arşivi. Tüm bölümler sayfasını veya aşağıdaki sezonlardan birini açıp izlemek istediğiniz bölümü seçin.", poster = "", backdrop = "", year = "2006–2010", score = 0.0, genreIds = listOf(18), provider = "Kanal D", webUrl = "https://www.kanald.com.tr/yaprakdokumu/bolumler", category = "Türk Dizileri"),
    Movie(id = 7401, title = "Guguk Kuşu", overview = "Verdiğiniz sitedeki oynatıcıyı uygulama içinde açar. Doğrudan video kaynağı doğrulanmadı; site erişimi TV üzerinde denenmelidir.", poster = "", backdrop = "", year = "1975", score = 0.0, genreIds = emptyList(), provider = "Kullanıcının seçtiği site", webUrl = "https://www.fullhdfilmizlesene.now/film/guguk-kusu/", category = "Film"),
    Movie(id = 7402, title = "Su Savaşları — Bölümler", overview = "TRT Belgesel’in suya erişim mücadelesini konu alan serisi. Açılan program sayfasından bir bölüm seçin.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "TRT Belgesel", webUrl = "https://www.trtbelgesel.com.tr/macera/su-savaslari", category = "Belgesel"),
    Movie(id = 7403, title = "Türkiye – Gürcistan | EURO 2024", overview = "18 Haziran 2024 karşılaşmasının TRT Spor tarafından yayımlanan özeti. Yayıncının kendi oynatıcısında açılır.", poster = "", backdrop = "", year = "2024", score = 0.0, genreIds = emptyList(), provider = "TRT Spor", webUrl = "https://www.youtube.com/watch?v=Sx_2vItxhXo", category = "Maç Özeti"),
    Movie(id = 7404, title = "Tarkan — Şımarık", overview = "Tarkan’ın resmî sitesinin yönlendirdiği müzik klibi. YouTube oynatıcısında açılır.", poster = "", backdrop = "", year = "1997", score = 0.0, genreIds = emptyList(), provider = "Tarkan", webUrl = "https://www.youtube.com/watch?v=cpp69ghR1IM", category = "Müzik"),
    Movie(id = 7405, title = "Tarkan — Kuzu Kuzu", overview = "Tarkan’ın resmî sitesinin yönlendirdiği müzik klibi. YouTube oynatıcısında açılır.", poster = "", backdrop = "", year = "2001", score = 0.0, genreIds = emptyList(), provider = "Tarkan", webUrl = "https://www.youtube.com/watch?v=NAHRpEqgcL4", category = "Müzik"),
    Movie(7301, "Umudunu Kaybetme", "The Pursuit of Happyness. Will Smith’in başrolünde, oğluyla birlikte zor koşullarda hayata tutunmaya çalışan bir babanın hikâyesi. Yaklaşık 117 dakika. Türkçe dublaj (kaynak etiketi). Görüntü: Çözünürlük doğrulanamadı.", "file:///android_asset/umudunu-kaybetme-the-pursuit-of-happyness-turkce-dublaj-1080p-full-hd-izle_202405.jpg", "", "2006", 0.0, listOf(18), "https://archive.org/download/umudunu-kaybetme-the-pursuit-of-happyness-turkce-dublaj-1080p-full-hd-izle_202405/Umudunu%20Kaybetme%20%28The%20Pursuit%20of%20Happyness%29%20T%C3%BCrk%C3%A7e%20dublaj%201080p%20Full%20HD%20izle.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7302, "Vikingler", "The Vikings. Kirk Douglas ve Tony Curtis’in rol aldığı tarihî macera. Yaklaşık 116 dakika. Türkçe dublaj (kaynak etiketi). Görüntü: 852 × 480.", "file:///android_asset/20210524_20210524_0931.jpg", "", "1958", 0.0, listOf(12,28), "https://archive.org/download/20210524_20210524_0931/Vikingler.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7201, "Arizona Haydutları", "Robbers’ Roost. Bir çiftlikte karşı karşıya gelen haydut grupları üzerine bir western. Yaklaşık 80 dakika. Kaynak kaydı: Türkçe dublaj. Ses dili kaynaktaki etikete dayanır.", "file:///android_asset/arizona-haydutlari-1955.jpg", "", "1955", 0.0, listOf(37), "https://archive.org/download/arizona-haydutlari-1955/Arizona%20Haydutlar%C4%B1%20%281955%29.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7202, "30 Winchester per El Diablo", "Meksika’ya taşınacak altının peşindeki haydutlar arasında geçen western. Yaklaşık 85 dakika. Kaynak kaydı: Türkçe dublaj. Ses dili kaynaktaki etikete dayanır.", "file:///android_asset/30-winchester-per-el-diablo.jpg", "", "1965", 0.0, listOf(37), "https://archive.org/download/30-winchester-per-el-diablo/30%20Winchester%20per%20El%20Diablo.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive"),
    Movie(7203, "Yalnız Kovboy", "Ride Lonesome. Bir ödül avcısının tutsağıyla çıktığı tehlikeli yolculuk. Yaklaşık 73 dakika. Kaynak kaydı: Türkçe dublaj. Ses dili kaynaktaki etikete dayanır.", "file:///android_asset/yalniz-kovboy-ride-lonesome-1959-turkce-dublaj-tek-parca-full-film-izle-otukenim.tv.jpg", "", "1959", 0.0, listOf(37), "https://archive.org/download/yalniz-kovboy-ride-lonesome-1959-turkce-dublaj-tek-parca-full-film-izle-otukenim.tv/Yaln%C4%B1z%20Kovboy%20%E2%80%93%20Ride%20Lonesome%201959%20%E2%80%93%20T%C3%BCrk%C3%A7e%20Dublaj%20Tek%20Par%C3%A7a%20Full%20Film%20izle%20-%20Otukenim.tv.mp4", "Lisans bilgisi belirtilmemiş", "Internet Archive")
) + importedCatalog + season1999Catalog

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
        connection.setRequestProperty("User-Agent", "CineBoxTV/0.11 (Android TV open-license video discovery)")
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
        setContent { App() }
    }

    @Composable
    private fun App() {
        var category by remember { mutableStateOf("Tümü") }
        var tab by remember { mutableStateOf("Ana Sayfa") }
        var query by remember { mutableStateOf("") }
        var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var recommended by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var selected by remember { mutableStateOf<Movie?>(null) }
        var playing by remember { mutableStateOf<PlayRequest?>(null) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("") }
        var searchNonce by remember { mutableIntStateOf(0) }
        val favorites = remember { mutableStateListOf<Int>().apply { addAll(prefs.getString("favorites", "")!!.split(",").mapNotNull { it.toIntOrNull() }) } }
        val ratings = remember { mutableStateMapOf<Int, Int>().apply {
            prefs.all.forEach { (key, value) -> if (key.startsWith("rating_") && value is Int) key.removePrefix("rating_").toIntOrNull()?.let { put(it, value) } }
        } }
        val savedMovies = remember { mutableStateMapOf<Int, Movie>().apply { catalogMovies.forEach { put(it.id, it) } } }

        fun showMovies(list: List<Movie>) {
            movies = list
            list.forEach { savedMovies[it.id] = it }
            busy = false
            message = if (list.isEmpty()) "Film bulunamadı." else ""
        }
        LaunchedEffect(Unit) { showMovies(catalogMovies) }
        LaunchedEffect(searchNonce) {
            if (searchNonce == 0) return@LaunchedEffect
            busy = true
            message = "Açık lisanslı videolar aranıyor…"
            try {
                val result = searchCommons(query)
                showMovies(result)
                message = if (result.isEmpty()) "Açık lisanslı video bulunamadı. Başka bir isim deneyin." else "${result.size} açık lisanslı video bulundu."
            } catch (e: Exception) {
                busy = false
                movies = emptyList()
                message = "Arama başarısız: ${e.localizedMessage ?: "Bağlantıyı kontrol edin."}"
            }
        }
        LaunchedEffect(selected?.id) {
            val genre = selected?.genreIds?.firstOrNull()
            recommended = catalogMovies.filter { it.id != selected?.id && genre != null && genre in it.genreIds }
        }
        BackHandler(enabled = playing != null || selected != null) {
            if (playing != null) playing = null else selected = null
        }
        MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = accent)) {
            when {
                playing != null -> PlayerScreen(playing!!, prefs) { playing = null }
                selected != null -> {
                    val m = selected!!
                    var customUrl by remember(m.id) { mutableStateOf(prefs.getString("source_${m.id}", "").orEmpty()) }
                    var subtitleUrl by remember(m.id) { mutableStateOf(prefs.getString("subtitle_${m.id}", "").orEmpty()) }
                    var sourceError by remember(m.id) { mutableStateOf("") }
                    Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(30.dp)) {
                        TextButton(onClick = { selected = null }) { Text("← Geri") }
                        Row(horizontalArrangement = Arrangement.spacedBy(25.dp)) {
                            AsyncImage(model = m.poster, contentDescription = m.title, modifier = Modifier.width(190.dp).height(270.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(m.title, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                                Text(if (m.score > 0) "${m.year}   ★ ${"%.1f".format(m.score)}" else m.year, color = accent)
                                Text(m.overview.ifBlank { "Türkçe açıklama bulunamadı." }, color = Color.LightGray, maxLines = 6)
                                if (m.provider.isNotBlank()) Text("Kaynak: ${m.provider}" + if (m.license.isNotBlank()) " · ${m.license}" else "", color = Color.LightGray, fontSize = 13.sp)
                                if (m.webUrl.isNotBlank()) Button(onClick = {
                                    startActivity(android.content.Intent(this@MainActivity, WebPlayerActivity::class.java)
                                        .putExtra("url", m.webUrl).putExtra("title", m.title))
                                }) { Text(if (m.provider == "Özet ara") "🔎 Özet ara" else "▶ Web oynatıcıda aç") }
                                if (m.webUrl.isNotBlank()) OutlinedButton(onClick = {
                                    openPublisherPage(m.webUrl)
                                }) { Text(if (m.id == 7501) "Tüm bölümleri tarayıcıda aç" else "Tarayıcıda aç") }
                                if (m.id == 7502) {
                                    OutlinedButton(onClick = { openPublisherPage("https://www.youtube.com/watch?v=GgAkIcXFinA") }) {
                                        Text("1999 · 1. bölüm — YouTube’da aç")
                                    }
                                    Text("Arşiv yüklemesi: Dizi Arşivi. Bölümler tek tek oynatılarak doğrulanmadı.", color = Color.LightGray, fontSize = 12.sp)
                                }
                                if (m.id == 7501) {
                                    Text("Sezon seç", color = Color.White, fontWeight = FontWeight.Bold)
                                    (1..5).forEach { season ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val seasonUrl = "https://www.kanald.com.tr/yaprakdokumu/bolumler?activeSeasonNumber=$season&seasonCount=5"
                                            Button(onClick = { openPublisherPage(seasonUrl) }) { Text("$season. Sezon · Tarayıcıda aç") }
                                            OutlinedButton(onClick = {
                                                startActivity(android.content.Intent(this@MainActivity, WebPlayerActivity::class.java)
                                                    .putExtra("url", seasonUrl).putExtra("title", "Yaprak Dökümü · $season. Sezon"))
                                            }) { Text("Uygulamada aç") }
                                        }
                                    }
                                }
                                if (m.playableUrl.isNotBlank()) Button(onClick = { playing = PlayRequest(m.id, m.title, m.playableUrl) }) { Text("▶ Filmi oynat") }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = {
                                        if (favorites.contains(m.id)) favorites.remove(m.id) else favorites.add(m.id)
                                        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
                                    }) { Text(if (favorites.contains(m.id)) "♥ Favorilerden çıkar" else "♡ Favorilere ekle") }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("CineBox Player · Yayın kaynakları", color = Color.White, fontWeight = FontWeight.Bold)
                                if (m.playableUrl.isBlank() && m.webUrl.isBlank() && customUrl.isBlank()) {
                                    Text("Bu film için oynatma bağlantısı henüz eklenmedi. Bu kart yalnızca katalog kaydıdır.", color = Color(0xFFFFD180))
                                }
                                Text("Kullanma hakkınız olan HTTPS MP4 / WebM / M3U8 / MPD adresini ekleyin.", color = Color.LightGray, fontSize = 12.sp)
                                TextField(value = customUrl, onValueChange = { customUrl = it }, label = { Text("Video bağlantısı (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                TextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Altyazı bağlantısı (isteğe bağlı, HTTPS .vtt)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
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
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    (1..10).forEach { n ->
                                        OutlinedButton(onClick = { ratings[m.id] = n; prefs.edit().putInt("rating_${m.id}", n).apply() }, contentPadding = PaddingValues(6.dp), modifier = Modifier.width(44.dp)) { Text("$n") }
                                    }
                                }
                                Text("Her film kendi kaynak bağlantısıyla oynatılır. Türkçe altyazı isterseniz VTT bağlantısı ekleyebilirsiniz.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(25.dp))
                        Text("Bunları da beğenebilirsin", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        MovieRow(recommended.filter { it.id != m.id }) { selected = it }
                    }
                }
                else -> Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("CINEBOX", color = accent, fontWeight = FontWeight.Black, fontSize = 29.sp)
                        listOf("Ana Sayfa", "Ara", "Favoriler", "Öneriler").forEach { label ->
                            Button(onClick = {
                                tab = label
                                when (label) {
                                    "Ana Sayfa" -> showMovies(catalogMovies)
                                    "Öneriler" -> {
                                        val preferred = savedMovies.values.filter { (ratings[it.id] ?: 0) >= 7 }.flatMap { it.genreIds }.toSet()
                                        showMovies(catalogMovies.filter { preferred.isEmpty() || it.genreIds.any { genre -> genre in preferred } })
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = if (tab == label) accent else panel)) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    if (tab == "Ara") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextField(value = query, onValueChange = { query = it }, label = { Text("Film adı") }, singleLine = true, modifier = Modifier.width(450.dp))
                            Button(onClick = { if (query.isNotBlank()) searchNonce++ }) { Text("🔍 İnternette ara") }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    Text(when (tab) { "Ara" -> "Arama sonuçları"; "Favoriler" -> "Favori filmlerim"; "Öneriler" -> "Sana özel öneriler"; else -> "Film, dizi, belgesel, spor ve müzik" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (busy) CircularProgressIndicator()
                    if (message.isNotBlank()) Text(message, color = Color.LightGray)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tümü", "Filmler", "Diziler", "Türk Dizileri", "Belgesel", "Maç Özeti", "Müzik").forEach { label ->
                            OutlinedButton(onClick = { category = label }) { Text(if (category == label) "✓ $label" else label) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val baseMovies = if (tab == "Favoriler") savedMovies.values.filter { favorites.contains(it.id) } else movies
                    val shown = baseMovies.filter { category == "Tümü" || (category == "Filmler" && it.category == "Film") || (category == "Diziler" && it.category in listOf("Dizi", "Türk Dizileri")) || it.category == category }
                    MovieRow(shown) { selected = it }
                    if (tab == "Favoriler" && shown.isEmpty()) Text("Henüz favoriniz yok. Ana sayfadan bir film seçerek favorilere ekleyebilirsiniz.", color = Color.LightGray)
                    Spacer(Modifier.height(20.dp))
                    Text("MP4 içerikler CineBox Player’da; web içerikleri yayıncının oynatıcısında açılır. İnternet araması: Wikimedia Commons.", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MovieRow(movies: List<Movie>, onClick: (Movie) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 22.dp)) {
        items(movies, key = { it.id }) { movie ->
            Column(Modifier.width(180.dp)) {
                Surface(onClick = { onClick(movie) }, shape = RoundedCornerShape(11.dp), color = panel) {
                    Column {
                        Text(movie.title, color = Color.White, fontSize = 15.sp, minLines = 2, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                        if (movie.poster.isNotEmpty()) AsyncImage(model = movie.poster, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp))
                        else Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text(movie.title, color = Color.LightGray, modifier = Modifier.padding(12.dp)) }
                        Text(if (movie.provider == "Özet ara") "Maç Özeti · Video aranacak" else if (movie.webUrl.isNotBlank()) "${movie.category} · Web oynatıcı" else if ((movie.id in 7201..7203 || movie.id in 7301..7302)) "${movie.year} · Türkçe dublaj" else if (movie.provider.isNotEmpty()) "${movie.provider} · ${movie.license}" else "${movie.year}  ★ ${"%.1f".format(movie.score)}", color = Color.LightGray, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(request: PlayRequest, prefs: android.content.SharedPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
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
            .setUserAgent("CineBoxTV/0.11 (Android TV)")
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
            if (player.duration > 0 && player.currentPosition >= 0) {
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
            player.release()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply {
                this.player = player
                useController = true
                controllerShowTimeoutMs = 6000
                controllerAutoShow = true
                keepScreenOn = true
            } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Button(onClick = onBack) { Text("← Film detayına dön") }
            if (status.isNotEmpty()) {
                Text(status, color = Color(0xFFFFAAAA), modifier = Modifier.background(Color.Black).padding(8.dp))
                Button(onClick = {
                    status = "Yeniden bağlanılıyor…"
                    retryNumber += 1
                }) { Text("Yeniden dene") }
                Button(onClick = {
                    prefs.edit().remove(progressKey).apply()
                    player.seekTo(0)
                    status = "Baştan deneniyor…"
                    retryNumber += 1
                }) { Text("Baştan dene") }
            }
        }
        Text(request.title, color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
    }
}
