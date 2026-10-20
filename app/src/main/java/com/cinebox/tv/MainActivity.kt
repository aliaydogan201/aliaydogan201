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

// Yerel deneme kataloğu: TMDB bağlantısı veya API anahtarı gerekmez.
// Bu örnekler telifli film oynatma bağlantıları değildir.
private val demoMovies = listOf(
    Movie(1, "Büyük Tavşan", "Açık lisanslı animasyon videosuyla oynatıcı testi.", "", "", "2008", 8.0, listOf(16)),
    Movie(2, "Sintel", "Fantastik animasyon örneği. Oynatma bağlantısı örnek medya içindir.", "", "", "2010", 8.2, listOf(16, 14)),
    Movie(3, "Tears of Steel", "Bilim kurgu kısa filmi için örnek katalog kartı.", "", "", "2012", 7.8, listOf(878)),
    Movie(4, "Elephants Dream", "Animasyon kategorisinde örnek film kaydı.", "", "", "2006", 7.6, listOf(16)),
    Movie(5, "Cosmos Laundromat", "Animasyon türünde katalog gösterim kaydı.", "", "", "2015", 7.7, listOf(16)),
    Movie(6, "Caminandes", "Kısa animasyon kategorisinde örnek kayıt.", "", "", "2013", 7.4, listOf(16))
)

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
        val savedMovies = remember { mutableStateMapOf<Int, Movie>().apply { demoMovies.forEach { put(it.id, it) } } }

        fun showMovies(list: List<Movie>) {
            movies = list
            list.forEach { savedMovies[it.id] = it }
            busy = false
            message = if (list.isEmpty()) "Film bulunamadı." else ""
        }
        LaunchedEffect(Unit) { showMovies(demoMovies) }
        LaunchedEffect(selected?.id) {
            val genre = selected?.genreIds?.firstOrNull()
            recommended = demoMovies.filter { it.id != selected?.id && (genre == null || genre in it.genreIds) }
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
                                    "Ana Sayfa" -> showMovies(demoMovies)
                                    "Öneriler" -> {
                                        val preferred = savedMovies.values.filter { (ratings[it.id] ?: 0) >= 7 }.flatMap { it.genreIds }.toSet()
                                        showMovies(demoMovies.filter { preferred.isEmpty() || it.genreIds.any { genre -> genre in preferred } })
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = if (tab == label) accent else panel)) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    if (tab == "Ara") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextField(value = query, onValueChange = { query = it }, label = { Text("Film adı") }, singleLine = true, modifier = Modifier.width(450.dp))
                            Button(onClick = { if (query.isNotBlank()) showMovies(demoMovies.filter { it.title.contains(query.trim(), ignoreCase = true) }) }) { Text("🔍 Ara") }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    Text(when (tab) { "Ara" -> "Arama sonuçları"; "Favoriler" -> "Favori filmlerim"; "Öneriler" -> "Sana özel öneriler"; else -> "Örnek film kataloğu (TMDB kullanılmıyor)" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (busy) CircularProgressIndicator()
                    if (message.isNotBlank()) Text(message, color = Color.LightGray)
                    val shown = if (tab == "Favoriler") demoMovies.filter { favorites.contains(it.id) } else movies
                    MovieRow(shown) { selected = it }
                    if (tab == "Favoriler" && shown.isEmpty()) Text("Henüz favoriniz yok. Ana sayfadan bir film seçerek favorilere ekleyebilirsiniz.", color = Color.LightGray)
                    Spacer(Modifier.height(20.dp))
                    Text("Deneme kataloğu: Gerçek film ve yayın kaynakları henüz bağlı değildir.", color = Color.Gray, fontSize = 11.sp)
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
