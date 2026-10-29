package com.cinebox.tv

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/** Uses the publisher's own page and player; does not extract media URLs. */
class WebPlayerActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private lateinit var normal: LinearLayout
    private lateinit var status: TextView
    private var full: FrameLayout? = null
    private var fullCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var resumeButton: Button
    private var resumeFullscreen = false
    private var okCaptured = false
    private var transportPending = false
    private val transportScript by lazy { assets.open("playback_control.js").bufferedReader().use { it.readText() } }
    private fun controlPlayback(action: String, done: (Boolean) -> Unit) {
        val surface = full?.getChildAt(0) ?: web
        web.evaluateJavascript("(function(){ var action = '" + action + "'; " + transportScript + " })();") { result ->
            if (isFinishing || isDestroyed) return@evaluateJavascript
            val confirmed = result == "\"ok\""
            if (!confirmed) {
                // Explicit media keys, never a blind click or a play/pause toggle.
                val key = if (action == "pause") android.view.KeyEvent.KEYCODE_MEDIA_PAUSE else android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                surface.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, key))
                surface.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, key))
            }
            done(confirmed)
        }
    }
    private fun pauseAndShowControls() {
        if (transportPending) return
        transportPending = true
        resumeFullscreen = full != null
        controlPlayback("pause") { confirmed ->
            transportPending = false
            if (full != null) hideFull()
            resumeButton.visibility = View.VISIBLE
            resumeButton.requestFocus()
            toolbar.smoothScrollTo(0, 0)
            setPointer(true)
            status.text = if (confirmed) "Duraklatıldı · Devam et ile izlemeye dönün."
                else "Kontroller açık · Kaynak duraklatmayı doğrulamadı; gerekirse TV imlecini kullanın."
        }
    }
    private fun resumePlayback() {
        if (transportPending) return
        setPointer(false)
        transportPending = true
        controlPlayback("play") { confirmed ->
            transportPending = false
            resumeButton.visibility = View.GONE
            web.requestFocus()
            if (resumeFullscreen) web.evaluateJavascript(assets.open("video_fullscreen.js").bufferedReader().use { it.readText() }, null)
            resumeFullscreen = false
            status.text = if (confirmed) "Oynatma isteği gönderildi · OK: duraklat ve kontrolleri aç."
                else "Oynatma isteği gönderildi · Başlamazsa oynatıcıdaki oynat düğmesini kullanın."
        }
    }
    private lateinit var nextEpisodeButton: Button
    private var episodeList: List<SeriesEpisode> = emptyList()
    private var seriesTitle = ""
    private var episodePageUrl = ""
    private var episodeLoading = false
    private fun updateNextEpisode() {
        if (!::nextEpisodeButton.isInitialized) return
        val next = EpisodeNavigation.next(episodeList, episodePageUrl)
        nextEpisodeButton.isEnabled = next != null && !episodeLoading
        nextEpisodeButton.text = if (episodeList.none { it.url.trimEnd('/') == episodePageUrl.trimEnd('/') }) "Bölüm sırası bulunamadı"
            else if (next == null) "Sonraki bölüm yok"
            else "Sonraki bölüm ▶ " + next.season + ".S " + next.episode + ".B"
    }
    private fun openNextEpisode() {
        if (episodeLoading) return
        val next = EpisodeNavigation.next(episodeList, episodePageUrl) ?: return
        openEpisode(next)
    }
    private fun openEpisode(next: SeriesEpisode) {
        if (episodeLoading) return
        episodeLoading = true
        updateNextEpisode()
        setPointer(false)
        if (full != null) hideFull()
        resumeFullscreen = false
        resumeButton.visibility = View.GONE
        intent.removeExtra("resume_ms")
        intent.putExtra("series", false)
        val title = seriesTitle + " · " + next.season + ". Sezon " + next.episode + ". Bölüm"
        intent.putExtra("title", title)
        intent.putExtra("url", next.url)
        pageUrl = next.url
        episodePageUrl = next.url
        playerOnly = true
        stageButton.text = "Siteyi göster"
        status.text = title + " yükleniyor…"
        WatchHistory.save(this, WatchEntry(intent.getIntExtra("movie_id", 0), title, next.url, true))
        web.loadUrl(next.url)
    }
    private fun showEpisodePanel() {
        setPointer(false)
        if (full != null) hideFull()
        if (episodeList.isEmpty()) {
            android.app.AlertDialog.Builder(this).setTitle("Bölümler")
                .setMessage("Bu dizi için kayıtlı bölüm listesi bulunmuyor.")
                .setPositiveButton("Kapat", null).show()
            return
        }
        val seasons = episodeList.map { it.season }.distinct().sorted()
        android.app.AlertDialog.Builder(this).setTitle(seriesTitle + " · Sezon seç")
            .setItems(seasons.map { "$it. Sezon" }.toTypedArray()) { _, index ->
                showSeasonEpisodes(seasons[index])
            }.setNegativeButton("Kapat", null).show()
    }
    private fun showSeasonEpisodes(season: Int) {
        val rows = episodeList.filter { it.season == season }.sortedBy { it.episode }
        val labels = rows.map {
            SeriesTracking.label(this, intent.getIntExtra("movie_id",0), it.url) + it.episode + ". Bölüm"
        }.toTypedArray()
        val current = rows.indexOfFirst { it.url == episodePageUrl }
        val dialog = android.app.AlertDialog.Builder(this).setTitle("$season. Sezon · Bölümler")
            .setSingleChoiceItems(labels, current) { d, index ->
                d.dismiss()
                val episode = rows[index]
                android.app.AlertDialog.Builder(this).setTitle("$season. Sezon · ${episode.episode}. Bölüm")
                    .setItems(arrayOf("▶ Bölümü aç", if (SeriesTracking.watched(this,episode.url)) "İzlendi işaretini kaldır" else "✓ İzlendi olarak işaretle")) { _, action ->
                        if (action == 0) openEpisode(episode)
                        else {
                            SeriesTracking.mark(this,episode.url,!SeriesTracking.watched(this,episode.url))
                            showSeasonEpisodes(season)
                        }
                    }.setNegativeButton("Bölümlere dön") { _, _ -> showSeasonEpisodes(season) }.show()
            }.setNegativeButton("Sezonlar") { _, _ -> showEpisodePanel() }
            .setPositiveButton("Kapat", null).create()
        dialog.show()
        if (current >= 0) dialog.listView.setSelection(current)
    }
    private var pointerEnabled = false
    private var pointerX = 0f
    private var pointerY = 0f
    private lateinit var pointerOverlay: View
    private fun pointerSurface(): View = full ?: root
    private fun pointerBounds(): android.graphics.RectF {
        val surface = pointerSurface()
        val surfaceLocation = IntArray(2); surface.getLocationInWindow(surfaceLocation)
        val rootLocation = IntArray(2); root.getLocationInWindow(rootLocation)
        val left = (surfaceLocation[0] - rootLocation[0]).toFloat()
        val top = (surfaceLocation[1] - rootLocation[1]).toFloat()
        return android.graphics.RectF(left, top, left + surface.width, top + surface.height)
    }
    private fun setPointer(enabled: Boolean) {
        pointerEnabled = enabled
        pointerOverlay.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            val bounds = pointerBounds()
            pointerX = bounds.centerX(); pointerY = bounds.centerY()
            pointerOverlay.bringToFront(); pointerOverlay.invalidate()
            web.requestFocus()
        }
    }
    private fun pointerEvent(action: Int) {
        val bounds = pointerBounds()
        if (bounds.width() <= 2 || bounds.height() <= 2) return
        pointerX = pointerX.coerceIn(bounds.left + 1, bounds.right - 1)
        pointerY = pointerY.coerceIn(bounds.top + 1, bounds.bottom - 1)
        val now = android.os.SystemClock.uptimeMillis()
        val event = android.view.MotionEvent.obtain(now, now, action,
            pointerX - bounds.left, pointerY - bounds.top, 0)
        try {
            if (action == android.view.MotionEvent.ACTION_HOVER_MOVE) {
                event.source = android.view.InputDevice.SOURCE_MOUSE
                pointerSurface().dispatchGenericMotionEvent(event)
            } else pointerSurface().dispatchTouchEvent(event)
        } finally { event.recycle() }
    }
    private var historyActive = false
    private val historyHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val positionScript by lazy { assets.open("watch_position.js").bufferedReader().use { it.readText() } }
    private val historyTask = object : Runnable {
        override fun run() {
            if (!historyActive || !::web.isInitialized || isFinishing || isDestroyed) return
            web.evaluateJavascript(positionScript) { raw ->
                if (!historyActive || isFinishing || isDestroyed) return@evaluateJavascript
                try {
                    val decoded=org.json.JSONTokener(raw).nextValue() as? String
                    if (decoded != null) {
                        val o=org.json.JSONObject(decoded)
                        val position = o.optLong("position",-1)
                        val duration = o.optLong("duration",0)
                        if (duration >= 300000 && position >= duration * 0.95 &&
                            episodeList.any { it.url.trimEnd('/') == o.optString("url").trimEnd('/') })
                            SeriesTracking.mark(this@WebPlayerActivity,o.getString("url"),true)
                        WatchHistory.save(this@WebPlayerActivity,WatchEntry(intent.getIntExtra("movie_id",0),
                            intent.getStringExtra("title").orEmpty(),o.getString("url"),true,
                            o.getLong("position"),o.getLong("duration"),episode=o.optString("title")))
                    }
                } catch (_: Exception) { }
                historyHandler.postDelayed(this,5000)
            }
        }
    }
    private var pageUrl = ""
    private var playerOnly = true
    private var focusPlayerPending = false
    private var nativeTapPending = false
    private var showingEpisodes = false
    private lateinit var errorInfoButton: Button
    private var stageReady = false
    private var stageAttempts = 0
    private var stageEpoch = 0
    private lateinit var stageButton: Button
    private val stageScript by lazy { assets.open("player_only.js").bufferedReader().use { it.readText() } }
    private val stageHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stageTask = object : Runnable {
        override fun run() {
            if (!playerOnly || !::web.isInitialized || isFinishing || isDestroyed) return
            val epoch = stageEpoch
            web.evaluateJavascript("window.__alicoSeriesMode = " + intent.getBooleanExtra("series", false) + ";\n" + stageScript) { result ->
                if (epoch != stageEpoch || !playerOnly || isFinishing || isDestroyed) return@evaluateJavascript
                if (result == "\"ready\"" || result == "\"episodes\"") {
                    showingEpisodes = result == "\"episodes\""
                    if (!stageReady) {
                        stageReady = true
                        web.alpha = 1f
                        status.text = if (result == "\"episodes\"") "Bölüm seçin · Seçtiğiniz bölümün oynatıcısı açılacak." else "Oynatıcı hazır · Oynat düğmesine basın. Kaynak/bölüm seçmek için Siteyi göster."
                        if (!showingEpisodes) attemptPlay()
                        web.requestFocus()
                        if (focusPlayerPending) {
                            focusPlayerPending = false
                            web.evaluateJavascript("window.__alicoStage && window.__alicoStage.focusPlayer();", null)
                        }
                    }
                    stageHandler.postDelayed(this, 1500)
                } else {
                    stageReady = false
                    web.alpha = 0f
                    stageAttempts++
                    if (stageAttempts < 40) stageHandler.postDelayed(this, 500)
                    else {
                        status.text = "Oynatıcı veya bölüm listesi bulunamadı. Siteyi göster ile bölüm veya kaynak seçebilirsiniz."
                        stageButton.requestFocus()
                    }
                }
            }
        }
    }
    private fun startPlayerSearch() {
        showingEpisodes = false
        stageEpoch++
        stageHandler.removeCallbacks(stageTask)
        stageReady = false
        stageAttempts = 0
        web.alpha = if (playerOnly) 0f else 1f
        if (playerOnly) {
            status.text = "Oynatıcı aranıyor… Gerekirse Siteyi göster düğmesini kullanın."
            stageHandler.postDelayed(stageTask, 300)
        }
    }
    private fun attemptPlay() {
        val epoch = stageEpoch
        web.evaluateJavascript("window.__alicoStage && window.__alicoStage.tryPlay();") { result ->
            if (epoch != stageEpoch || isFinishing || isDestroyed) return@evaluateJavascript
            if (nativeTapPending && (result == "\"frame-unavailable\"" || result == "\"manual-play-needed\"")) {
                nativeTapPending = false
                web.evaluateJavascript("JSON.stringify(window.__alicoStage && window.__alicoStage.tapPoint());") pointCallback@ { raw ->
                    if (epoch != stageEpoch || !playerOnly || showingEpisodes || full != null || !web.hasWindowFocus()) return@pointCallback
                    try {
                        val decoded = org.json.JSONTokener(raw).nextValue() as? String ?: return@pointCallback
                        val point = org.json.JSONObject(decoded)
                        val nx = point.getDouble("x")
                        val ny = point.getDouble("y")
                        if (nx !in 0.0..1.0 || ny !in 0.0..1.0) return@pointCallback
                        val x = (nx * web.width).toFloat()
                        val y = (ny * web.height).toFloat()
                        val now = android.os.SystemClock.uptimeMillis()
                        web.requestFocus()
                        val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
                        val up = android.view.MotionEvent.obtain(now, now + 80, android.view.MotionEvent.ACTION_UP, x, y, 0)
                        try { web.dispatchTouchEvent(down); web.dispatchTouchEvent(up) }
                        finally { down.recycle(); up.recycle() }
                        status.text = "Oynatıcı ortasına bir dokunuş gönderildi. Başlamazsa oynat düğmesini seçin."
                    } catch (_: Exception) { status.text = "Oynatıcıya dokunulamadı. Oynat düğmesini seçin." }
                }
            } else nativeTapPending = false
        }
    }
    private fun goToPlayer() {
        showingEpisodes = false
        playerOnly = true
        nativeTapPending = true
        focusPlayerPending = true
        stageButton.text = "Siteyi göster"
        stageEpoch++
        stageHandler.removeCallbacks(stageTask)
        stageReady = false
        stageAttempts = 0
        status.text = "Oynatıcı aranıyor…"
        val epoch = stageEpoch
        web.evaluateJavascript("window.__alicoForcePlayer = true; " + stageScript +
            "; if (window.__alicoStage) window.__alicoStage.openPlayer();") {
            if (epoch == stageEpoch && !isFinishing && !isDestroyed) stageHandler.post(stageTask)
        }
    }
    private fun togglePlayerOnly() {
        showingEpisodes = false
        playerOnly = !playerOnly
        stageEpoch++
        stageHandler.removeCallbacks(stageTask)
        stageButton.text = if (playerOnly) "Siteyi göster" else "Sade görünüm"
        if (playerOnly) startPlayerSearch()
        else {
            focusPlayerPending = false
            web.evaluateJavascript("window.__alicoForcePlayer = false; window.__alicoStage && window.__alicoStage.restore();", null)
            web.alpha = 1f
            stageReady = false
            status.text = "Site görünümü · Bölüm/kaynak seçtikten sonra Sadece oynatıcı düğmesine basın."
        }
    }

    private val diagnostics = java.util.ArrayDeque<String>()
    private fun recordIssue(message: String) {
        if (diagnostics.size >= 20) diagnostics.removeFirst()
        diagnostics.addLast(message)
    }
    private fun showDiagnostics() {
        val engine = if (android.os.Build.VERSION.SDK_INT >= 26) {
            WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" } ?: "Bilinmiyor"
        } else "Bu Android sürümünde sürüm bilgisi alınamıyor"
        val details = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid ${android.os.Build.VERSION.RELEASE}\nWebView: $engine\n\n" +
            "Kimlik: ${web.settings.userAgentString}\n\n" +
            if (diagnostics.isEmpty()) "Henüz ağ/oynatıcı hatası yakalanmadı. Videoyu başlatıp tekrar açın." else diagnostics.joinToString("\n")
        android.app.AlertDialog.Builder(this).setTitle("Oynatıcı tanılama")
            .setMessage(details).setPositiveButton("Kapat", null).show()
    }
    @Volatile private var adFilterEnabled: Boolean = true
    private lateinit var toolbar: HorizontalScrollView
    private lateinit var fullscreenButton: Button

    private fun styleButton(button: Button) {
        button.isFocusable = true
        button.isFocusableInTouchMode = true
        button.minHeight = (52 * resources.displayMetrics.density).toInt()
        fun paint(focused: Boolean) {
            button.setTextColor(if (focused) Color.BLACK else Color.WHITE)
            button.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (focused) Color.YELLOW else Color.rgb(81, 39, 0))
                cornerRadius = 8 * resources.displayMetrics.density
                setStroke((if (focused) 4 else 1) * resources.displayMetrics.density.toInt().coerceAtLeast(1), Color.WHITE)
            }
            button.setPadding(18, 12, 18, 12)
        }
        paint(false)
        button.setOnFocusChangeListener { _, focused -> paint(focused) }
    }
    @Suppress("DEPRECATION")
    private fun systemFullscreen(enabled: Boolean) {
        window.decorView.systemUiVisibility = if (enabled) 5894 else 0
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nativeTapPending = true
        playerOnly = savedInstanceState?.getBoolean("alico_player_only", true) ?: true
        val webPrefs = getSharedPreferences("cinebox_web", MODE_PRIVATE)
        adFilterEnabled = webPrefs.getBoolean("ad_filter", true)
        pageUrl = savedInstanceState?.getString("alico_episode_url") ?: intent.getStringExtra("url").orEmpty()
        if (Uri.parse(pageUrl).scheme != "https" || Uri.parse(pageUrl).host.isNullOrBlank()) {
            finish(); return
        }
        val oldEntry = WatchHistory.list(this).firstOrNull { it.url == pageUrl }
        if (oldEntry == null) WatchHistory.save(this,WatchEntry(intent.getIntExtra("movie_id",0),intent.getStringExtra("title").orEmpty(),pageUrl,true))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        normal = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(normal, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toolbar = HorizontalScrollView(this).apply { addView(tools); setBackgroundColor(Color.rgb(173, 67, 0)) }
        normal.addView(toolbar)
        status = TextView(this).apply {
            setTextColor(Color.WHITE); setPadding(12, 4, 12, 4)
            text = "${intent.getStringExtra("title").orEmpty()} · Sayfa yükleniyor…"
        }
        normal.addView(status)
        try { web = WebView(this) } catch (_: Exception) {
            status.text = "Web oynatıcı başlatılamadı. Android System WebView bileşenini kontrol edin."
            Button(this).apply { text = "Kapat"; setOnClickListener { finish() }; normal.addView(this) }
            return
        }
        normal.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        fun button(label: String, action: () -> Unit) {
            tools.addView(Button(this).apply { text = label; styleButton(this); setOnClickListener { action() } })
        }
        resumeButton = Button(this).apply {
            text = "▶ Devam et"; styleButton(this); visibility = View.GONE
            setOnClickListener { resumePlayback() }
        }
        tools.addView(resumeButton)
        val seriesMovie = ArchiveSync.load(this, catalogMovies).firstOrNull {
            it.id == intent.getIntExtra("movie_id", 0) && it.category in listOf("Dizi", "Yerli Diziler")
        }
        seriesTitle = seriesMovie?.title.orEmpty()
        episodePageUrl = pageUrl
        episodeList = if (seriesMovie != null) SeriesTracking.episodes(this,seriesMovie) else emptyList()
        nextEpisodeButton = Button(this).apply {
            styleButton(this)
            visibility = if (seriesMovie == null) View.GONE else View.VISIBLE
            setOnClickListener { openNextEpisode() }
        }
        tools.addView(nextEpisodeButton)
        if (seriesMovie != null) {
            button("☷ Bölümler") { showEpisodePanel() }
            button("◀ Önceki bölüm") {
                val ordered = episodeList.sortedWith(compareBy({it.season},{it.episode}))
                val index = ordered.indexOfFirst { it.url == episodePageUrl }
                if (index > 0) openEpisode(ordered[index-1])
                else android.widget.Toast.makeText(this, "Önceki kayıtlı bölüm bulunamadı.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        updateNextEpisode()
        button("← Kapat") { finish() }
        stageButton = Button(this).apply {
            styleButton(this)
            text = if (playerOnly) "Siteyi göster" else "Sade görünüm"
            setOnClickListener { togglePlayerOnly() }
        }
        tools.addView(stageButton)
        web.alpha = if (playerOnly) 0f else 1f

        fullscreenButton = Button(this).apply {
            text = "⛶ Tam ekran"
            styleButton(this)
            setOnClickListener {
                status.text = "Video tam ekranı isteniyor…"
                web.evaluateJavascript(assets.open("video_fullscreen.js").bufferedReader().use { it.readText() }, null)
                web.postDelayed({
                    if (full == null && !isFinishing && !isDestroyed) {
                        status.text = "Oynatıcı tam ekranı açmadı. Videoyu başlatıp video içindeki ⛶ düğmesine basın."
                        web.requestFocus()
                    }
                }, 1800)

            }
        }
        tools.addView(fullscreenButton)
        button("Oynatıcıya geç") { goToPlayer() }
        button("TV imleci") { setPointer(!pointerEnabled) }
        button("Geri") { if (web.canGoBack()) web.goBack() }
        button("Yenile") { web.reload() }
        val defaultAgent = WebSettings.getDefaultUserAgent(this)
        val browserAgent = defaultAgent.replace("; wv", "").replace("Version/4.0 ", "")
        val compatibilityKey = "browser_identity_" + Uri.parse(pageUrl).host.orEmpty()
        var browserIdentity = webPrefs.getBoolean(compatibilityKey, false)
        web.settings.userAgentString = if (browserIdentity) browserAgent else defaultAgent
        tools.addView(Button(this).apply {
            styleButton(this)
            text = if (browserIdentity) "Uyumluluk: Açık" else "Uyumluluk: Kapalı"
            setOnClickListener {
                browserIdentity = !browserIdentity
                webPrefs.edit().putBoolean(compatibilityKey, browserIdentity).apply()
                text = if (browserIdentity) "Uyumluluk: Açık" else "Uyumluluk: Kapalı"
                diagnostics.clear()
                web.stopLoading()
                web.settings.userAgentString = if (browserIdentity) browserAgent else defaultAgent
                web.loadUrl(pageUrl)
            }
        })
        errorInfoButton = Button(this).apply {
            text = "Hata bilgisi"
            styleButton(this)
            setOnClickListener { showDiagnostics() }
        }
        tools.addView(errorInfoButton)

        tools.addView(Button(this).apply {
            styleButton(this)
            text = if (adFilterEnabled) "Reklam filtresi: Açık" else "Reklam filtresi: Kapalı"
            setOnClickListener {
                adFilterEnabled = !adFilterEnabled
                webPrefs.edit().putBoolean("ad_filter", adFilterEnabled).apply()
                text = if (adFilterEnabled) "Reklam filtresi: Açık" else "Reklam filtresi: Kapalı"
                web.reload()
            }
        })
        button("Tarayıcıda / YouTube'da aç") {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl))) }
            catch (_: Exception) { status.text = "Bu bağlantıyı açacak harici uygulama bulunamadı." }
        }
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.isFocusableInTouchMode = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return request.url.scheme !in listOf("https", "http") ||
                    (adFilterEnabled && AdHostRules.isBlockedHost(request.url.host))
            }
            @Deprecated("Compatibility for Android 6")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return Uri.parse(url).scheme !in listOf("https", "http") ||
                    (adFilterEnabled && AdHostRules.isBlockedHost(Uri.parse(url).host))
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (adFilterEnabled && AdHostRules.isBlockedHost(request.url.host)) {
                    return WebResourceResponse("text/plain", "UTF-8", 204, "No Content",
                        emptyMap<String, String>(), java.io.ByteArrayInputStream(ByteArray(0)))
                }
                return null
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                episodePageUrl = url
                if (episodeList.any { it.url.trimEnd('/') == url.trimEnd('/') })
                    SeriesTracking.opened(this@WebPlayerActivity,intent.getIntExtra("movie_id",0),url)
                episodeLoading = true
                updateNextEpisode()
                resumeFullscreen = false
                resumeButton.visibility = View.GONE
                nativeTapPending = true
                startPlayerSearch()
                if (!playerOnly) status.text = "${Uri.parse(url).host.orEmpty()} · Yükleniyor…"
            }
            override fun onPageFinished(view: WebView, url: String) {
                episodePageUrl = url
                episodeLoading = false
                updateNextEpisode()
                if (url == pageUrl && intent.getLongExtra("resume_ms",0) > 0) {
                    web.evaluateJavascript("window.__alicoResumeMs = " + intent.getLongExtra("resume_ms",0) + ";",null)
                    intent.removeExtra("resume_ms")
                }
                if (playerOnly && !stageReady) startPlayerSearch()
                if (!playerOnly && status.text.endsWith("Yükleniyor…")) status.text = "${Uri.parse(url).host.orEmpty()} · Sayfadaki oynat düğmesini seçin."
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                recordIssue("HTTP ${errorResponse.statusCode} · ${request.url.host.orEmpty()}")
                if (request.isForMainFrame) status.text = "HTTP ${errorResponse.statusCode} · Site erişim vermedi. Harici uygulamada deneyebilirsiniz."
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                recordIssue("Ağ hatası ${error.errorCode} · ${request.url.host.orEmpty()}")
                if (request.isForMainFrame) status.text = "Sayfa açılamadı: ${error.description}"
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                val text = message.message().orEmpty()
                // Keep diagnostic categories only, never signed URLs, tokens or cookies.
                when {
                    text.contains("232011") -> recordIssue("Oynatıcı: 232011 · Alanlar arası yayın listesi erişimi")
                    text.contains("cors", true) || text.contains("cross-origin", true) -> recordIssue("Tarayıcı: CORS / alanlar arası erişim hatası")
                    text.contains("mixed content", true) -> recordIssue("Tarayıcı: HTTPS/HTTP karma içerik engeli")
                    text.contains("ERR_", true) -> recordIssue("Tarayıcı: Kaynak yükleme hatası")
                }
                return true
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                status.text = "Açılır pencere engellendi. Gerekirse Tarayıcıda aç seçeneğini kullanın."
                return false
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                if (full != null) { callback.onCustomViewHidden(); return }
                fullCallback = callback
                normal.visibility = View.GONE
                systemFullscreen(true)
                full = FrameLayout(this@WebPlayerActivity).apply {
                    setBackgroundColor(Color.BLACK)
                    addView(view, FrameLayout.LayoutParams(-1, -1))
                }
                root.addView(full, FrameLayout.LayoutParams(-1, -1))
                if (pointerEnabled) pointerOverlay.post { setPointer(true) }
                view.requestFocus()
            }
            override fun onHideCustomView() { hideFull() }
        }
        web.setDownloadListener { _, _, _, _, _ -> status.text = "Bu ekran indirme yerine web oynatma içindir." }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when { full != null -> hideFull(); web.canGoBack() -> web.goBack(); else -> finish() }
            }
        })
        pointerOverlay = object : View(this) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: android.graphics.Canvas) {
                if (!pointerEnabled) return
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = Color.BLACK
                canvas.drawCircle(pointerX, pointerY, 13f, paint)
                paint.color = Color.YELLOW
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawCircle(pointerX, pointerY, 10f, paint)
                canvas.drawLine(pointerX-17, pointerY, pointerX+17, pointerY, paint)
                canvas.drawLine(pointerX, pointerY-17, pointerX, pointerY+17, paint)
                paint.style = android.graphics.Paint.Style.FILL
                paint.textSize = 18 * resources.displayMetrics.scaledDensity
            }
        }.apply { visibility = View.GONE; isFocusable = false; isClickable = false }
        root.addView(pointerOverlay, FrameLayout.LayoutParams(-1, -1))
        stageButton.requestFocus()
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) web.loadUrl(pageUrl)
        if (savedInstanceState != null) startPlayerSearch()
    }
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (pointerEnabled) {
            val code = event.keyCode
            if (code == android.view.KeyEvent.KEYCODE_MENU) setPointer(false)
            else if (code == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.action == android.view.KeyEvent.ACTION_UP) setPointer(false)
                return true
            } else if (code in listOf(android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER)) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    val step = (if (event.repeatCount > 8) 20 else 8) * resources.displayMetrics.density
                    when (code) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> pointerY -= step
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> pointerY += step
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> pointerX -= step
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> pointerX += step
                        else -> if (event.repeatCount == 0) {
                            pointerEvent(android.view.MotionEvent.ACTION_DOWN)
                            pointerEvent(android.view.MotionEvent.ACTION_UP)
                        }
                    }
                    pointerEvent(android.view.MotionEvent.ACTION_HOVER_MOVE)
                    pointerOverlay.invalidate()
                }
                return true
            }
        }

        val ok = event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == android.view.KeyEvent.KEYCODE_ENTER
        if (ok && okCaptured) {
            if (event.action == android.view.KeyEvent.ACTION_UP) okCaptured = false
            return true
        }
        if (ok && event.action == android.view.KeyEvent.ACTION_DOWN &&
            full != null) {
            okCaptured = true
            pauseAndShowControls()
            return true
        }

        // Keep episode-list navigation, but let its top edge escape to native controls.
        if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP &&
            ::web.isInitialized && web.hasFocus() && showingEpisodes && full == null) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val epoch = stageEpoch
                web.evaluateJavascript("window.__alicoStage ? window.__alicoStage.navigateEpisodeUp() : 'toolbar';") { result ->
                    if (epoch == stageEpoch && !isFinishing && !isDestroyed &&
                        web.hasFocus() && full == null && result != "\"moved\"") focusToolbar()
                }
            }
            return true
        }

        val menu = event.keyCode == android.view.KeyEvent.KEYCODE_MENU
        val upFromPlayer = event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP &&
            ::web.isInitialized && web.hasFocus() && !showingEpisodes && full == null
        if (menu || upFromPlayer) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (full != null) hideFull()
                focusToolbar()
            }
            return true
        }
        if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN &&
            ::toolbar.isInitialized && toolbar.hasFocus()) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                web.requestFocus()
                web.evaluateJavascript("window.__alicoStage && window.__alicoStage.focusPlayer();", null)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    private fun focusToolbar() {
        if (!::errorInfoButton.isInitialized) return
        errorInfoButton.requestFocus()
        errorInfoButton.post { errorInfoButton.requestRectangleOnScreen(
            android.graphics.Rect(0, 0, errorInfoButton.width, errorInfoButton.height), true) }
        status.text = "Üst menü · Sağ/sol ile düğme seçin, aşağı ile oynatıcıya dönün."
    }
    private fun hideFull() {
        full?.let { it.removeAllViews(); root.removeView(it) }
        full = null
        normal.visibility = View.VISIBLE
        val callback = fullCallback
        fullCallback = null
        callback?.onCustomViewHidden()
        systemFullscreen(false)
        if (::fullscreenButton.isInitialized) fullscreenButton.requestFocus()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("alico_episode_url", episodePageUrl.ifBlank { pageUrl })
        outState.putBoolean("alico_player_only", playerOnly)
        if (::web.isInitialized) web.saveState(outState)
        super.onSaveInstanceState(outState)
    }
    override fun onPause() { historyActive=false; historyHandler.removeCallbacksAndMessages(null); if (::web.isInitialized) web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if (::web.isInitialized) { web.onResume(); historyActive=true; historyHandler.removeCallbacks(historyTask); historyHandler.post(historyTask) } }
    override fun onDestroy() {
        historyActive=false
        historyHandler.removeCallbacksAndMessages(null)
        stageEpoch++
        stageHandler.removeCallbacksAndMessages(null)
        if (::web.isInitialized) {
            hideFull(); web.stopLoading(); (web.parent as? ViewGroup)?.removeView(web)
            web.removeAllViews(); web.destroy()
        }
        super.onDestroy()
    }
}
