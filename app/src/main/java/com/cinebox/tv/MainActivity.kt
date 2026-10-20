package com.cinebox.tv

import android.os.Bundle
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

private val bg = Color(0xFF090D18)
private val panel = Color(0xFF192034)
private val accent = Color(0xFF8B68FF)
private const val DEMO_URL = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

// Playback links are explicitly provided by the publisher or content owner, never scraped.
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val poster: String,
    val backdrop: String,
    val year: String,
    val score: Double,
    val genreIds: List<Int>
)

private fun parseMovies(json: JSONObject): List<Movie> {
    val results = json.optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).mapNotNull { i ->
        val o = results.optJSONObject(i) ?: return@mapNotNull null
        val title = o.optString("title", o.optString("name"))
        if (title.isBlank()) return@mapNotNull null
        val poster = o.optString("poster_path").takeIf { it.startsWith("/") }?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
        val backdrop = o.optString("backdrop_path").takeIf { it.startsWith("/") }?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: ""
        val genres = o.optJSONArray("genre_ids")
        Movie(o.optInt("id"), title, o.optString("overview"), poster, backdrop,
            o.optString("release_date").take(4), o.optDouble("vote_average"),
            if (genres == null) emptyList() else (0 until genres.length()).map { genres.optInt(it) })
    }
}

private fun tmdb(path: String): List<Movie> {
    if (BuildConfig.TMDB_BEARER_TOKEN.isBlank()) return emptyList()
    val connection = (URL("https://api.themoviedb.org/3/$path").openConnection() as HttpURLConnection)
    connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.TMDB_BEARER_TOKEN}")
    connection.setRequestProperty("Accept", "application/json")
    connection.connectTimeout = 10000
    connection.readTimeout = 10000
    try {
        if (connection.responseCode !in 200..299) return emptyList()
        return parseMovies(JSONObject(connection.inputStream.bufferedReader().use { it.readText() }))
    } finally { connection.disconnect() }
}

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("cinebox", Context.MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    @Composable
    private fun App() {
        var tab by remember { mutableStateOf("Ana Sayfa") }
        var query by remember { mutableStateOf("") }
        var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var recommended by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var selected by remember { mutableStateOf<Movie?>(null) }
        var playing by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("") }
        val favorites = remember { mutableStateListOf<Int>().apply { addAll(prefs.getString("favorites", "")!!.split(",").mapNotNull { it.toIntOrNull() }) } }
        val ratings = remember { mutableStateMapOf<Int, Int>().apply {
            prefs.all.forEach { (key, value) -> if (key.startsWith("rating_") && value is Int) key.removePrefix("rating_").toIntOrNull()?.let { put(it, value) } }
        } }
        val savedMovies = remember { mutableStateMapOf<Int, Movie>() }

        fun load(path: String, recommendation: Boolean = false) {
            busy = true
            message = ""
            thread {
                try {
                    val found = tmdb(path)
                    runOnUiThread {
                        if (recommendation) recommended = found else { movies = found; found.forEach { savedMovies[it.id] = it } }
                        busy = false
                        if (found.isEmpty()) message = if (BuildConfig.TMDB_BEARER_TOKEN.isBlank()) "Gerçek film kataloğu için TMDB anahtarını gradle.properties dosyasına girin." else "İçerik bulunamadı veya TMDB bağlantısı kurulamadı."
                    }
                } catch (e: Exception) { runOnUiThread { busy = false; message = "Bağlantı hatası: ${e.localizedMessage}" } }
            }
        }
        LaunchedEffect(Unit) { load("trending/movie/week?language=tr-TR") }
        LaunchedEffect(selected?.id) {
            val genre = selected?.genreIds?.firstOrNull()
            if (genre != null) load("discover/movie?language=tr-TR&sort_by=popularity.desc&with_genres=$genre", true)
        }
        BackHandler(enabled = playing != null || selected != null) {
            if (playing != null) playing = null else selected = null
        }
        MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = accent)) {
            when {
                playing != null -> PlayerScreen(playing!!) { playing = null }
                selected != null -> {
                    val m = selected!!
                    Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(30.dp)) {
                        TextButton(onClick = { selected = null }) { Text("← Geri") }
                        Row(horizontalArrangement = Arrangement.spacedBy(25.dp)) {
                            AsyncImage(model = m.poster, contentDescription = m.title, modifier = Modifier.width(190.dp).height(270.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(m.title, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                                Text("${m.year}   ★ ${"%.1f".format(m.score)}", color = accent)
                                Text(m.overview.ifBlank { "Türkçe açıklama bulunamadı." }, color = Color.LightGray, maxLines = 6)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = {
                                        if (favorites.contains(m.id)) favorites.remove(m.id) else favorites.add(m.id)
                                        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
                                    }) { Text(if (favorites.contains(m.id)) "♥ Favorilerden çıkar" else "♡ Favorilere ekle") }
                                    Button(onClick = { playing = DEMO_URL }) { Text("▶ Oynatıcıyı test et") }
                                }
                                Text("Kişisel puanım: ${ratings[m.id] ?: "Henüz yok"}", color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    (1..10).forEach { n ->
                                        OutlinedButton(onClick = { ratings[m.id] = n; prefs.edit().putInt("rating_${m.id}", n).apply() }, contentPadding = PaddingValues(6.dp), modifier = Modifier.width(44.dp)) { Text("$n") }
                                    }
                                }
                                Text("Not: Oynatıcı testi gerçek filmin kendisini açmaz. Film bazlı yayın bağlantıları yalnızca lisanslı kaynaklar eklendiğinde aktif olacaktır.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(25.dp))
                        Text("Bunları da beğenebilirsin", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        MovieRow(recommended.filter { it.id != m.id }) { selected = it }
                    }
                }
                else -> Column(Modifier.fillMaxSize().background(bg).padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("CINEBOX", color = accent, fontWeight = FontWeight.Black, fontSize = 29.sp)
                        listOf("Ana Sayfa", "Ara", "Favoriler", "Öneriler").forEach { label ->
                            Button(onClick = {
                                tab = label
                                when (label) {
                                    "Ana Sayfa" -> load("trending/movie/week?language=tr-TR")
                                    "Öneriler" -> {
                                        val genre = savedMovies.values.filter { ratings.getOrDefault(it.id, 0) >= 7 }.flatMap { it.genreIds }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                                        load(if (genre == null) "movie/top_rated?language=tr-TR" else "discover/movie?language=tr-TR&with_genres=$genre&sort_by=vote_average.desc&vote_count.gte=300")
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = if (tab == label) accent else panel)) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    if (tab == "Ara") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextField(value = query, onValueChange = { query = it }, label = { Text("Film adı") }, singleLine = true, modifier = Modifier.width(450.dp))
                            Button(onClick = { if (query.isNotBlank()) load("search/movie?language=tr-TR&query=${URLEncoder.encode(query, "UTF-8")}") }) { Text("🔍 Ara") }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    Text(when (tab) { "Ara" -> "Arama sonuçları"; "Favoriler" -> "Favori filmlerim"; "Öneriler" -> "Sana özel öneriler"; else -> "Bu haftanın popüler filmleri" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (busy) CircularProgressIndicator()
                    if (message.isNotBlank()) Text(message, color = Color.LightGray)
                    val shown = if (tab == "Favoriler") savedMovies.values.filter { favorites.contains(it.id) } else movies
                    MovieRow(shown) { selected = it }
                    if (tab == "Favoriler" && shown.isEmpty()) Text("Favorileriniz bu oturumda henüz yüklenmedi. Ana sayfadan film açarak favorilerinizi görüntüleyebilirsiniz.", color = Color.LightGray)
                    Spacer(Modifier.height(20.dp))
                    Text("Bu ürün TMDB API kullanır, ancak TMDB tarafından onaylanmış veya sertifikalandırılmış değildir. TMDB © themoviedb.org", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MovieRow(movies: List<Movie>, onClick: (Movie) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 22.dp)) {
        items(movies, key = { it.id }) { movie ->
            Column(Modifier.width(155.dp)) {
                Surface(onClick = { onClick(movie) }, shape = RoundedCornerShape(11.dp), color = panel) {
                    Column {
                        if (movie.poster.isNotEmpty()) AsyncImage(model = movie.poster, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(205.dp))
                        else Box(Modifier.fillMaxWidth().height(205.dp), contentAlignment = Alignment.Center) { Text("Afiş yok", color = Color.LightGray) }
                        Text(movie.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                        Text("${movie.year}  ★ ${"%.1f".format(movie.score)}", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxSize())
        Button(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(15.dp)) { Text("← Filmlere dön") }
    }
}
