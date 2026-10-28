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
    private var pageUrl = ""
    private var playerOnly = true
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
                    if (!stageReady) {
                        stageReady = true
                        web.alpha = 1f
                        status.text = if (result == "\"episodes\"") "Bölüm seçin · Seçtiğiniz bölümün oynatıcısı açılacak." else "Oynatıcı hazır · Oynat düğmesine basın. Kaynak/bölüm seçmek için Siteyi göster."
                        web.requestFocus()
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
    private fun togglePlayerOnly() {
        playerOnly = !playerOnly
        stageEpoch++
        stageHandler.removeCallbacks(stageTask)
        stageButton.text = if (playerOnly) "Siteyi göster" else "Sade görünüm"
        if (playerOnly) startPlayerSearch()
        else {
            web.evaluateJavascript("window.__alicoStage && window.__alicoStage.restore();", null)
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
        playerOnly = savedInstanceState?.getBoolean("alico_player_only", true) ?: true
        val webPrefs = getSharedPreferences("cinebox_web", MODE_PRIVATE)
        adFilterEnabled = webPrefs.getBoolean("ad_filter", true)
        pageUrl = intent.getStringExtra("url").orEmpty()
        if (Uri.parse(pageUrl).scheme != "https" || Uri.parse(pageUrl).host.isNullOrBlank()) {
            finish(); return
        }
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
        button("Hata bilgisi") { showDiagnostics() }

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
        button("Oynatıcıya geç") { web.requestFocus() }
        button("Tarayıcıda / YouTube'da aç") {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl))) }
            catch (_: Exception) { status.text = "Bu bağlantıyı açacak harici uygulama bulunamadı." }
        }
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
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
                startPlayerSearch()
                if (!playerOnly) status.text = "${Uri.parse(url).host.orEmpty()} · Yükleniyor…"
            }
            override fun onPageFinished(view: WebView, url: String) {
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
                    addView(Button(this@WebPlayerActivity).apply {
                        text = "Tam ekrandan çık"; styleButton(this); setOnClickListener { hideFull() }
                    }, FrameLayout.LayoutParams(-2, -2, android.view.Gravity.TOP or android.view.Gravity.END))
                }
                root.addView(full, FrameLayout.LayoutParams(-1, -1))
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
        stageButton.requestFocus()
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) web.loadUrl(pageUrl)
        if (savedInstanceState != null) startPlayerSearch()
    }
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU && ::web.isInitialized) {
            if (full != null) hideFull()
            fullscreenButton.requestFocus()
            return true
        }
        return super.onKeyDown(keyCode, event)
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
        outState.putBoolean("alico_player_only", playerOnly)
        if (::web.isInitialized) web.saveState(outState)
        super.onSaveInstanceState(outState)
    }
    override fun onPause() { if (::web.isInitialized) web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if (::web.isInitialized) web.onResume() }
    override fun onDestroy() {
        stageEpoch++
        stageHandler.removeCallbacksAndMessages(null)
        if (::web.isInitialized) {
            hideFull(); web.stopLoading(); (web.parent as? ViewGroup)?.removeView(web)
            web.removeAllViews(); web.destroy()
        }
        super.onDestroy()
    }
}
