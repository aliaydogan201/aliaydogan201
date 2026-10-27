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
    private lateinit var toolbar: HorizontalScrollView
    private lateinit var fullscreenButton: Button
    private lateinit var exitFullscreen: Button
    private var pageFullscreen = false

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
    private fun setPageFullscreen(enabled: Boolean) {
        pageFullscreen = enabled
        toolbar.visibility = if (enabled) View.GONE else View.VISIBLE
        status.visibility = if (enabled) View.GONE else View.VISIBLE
        exitFullscreen.visibility = if (enabled) View.VISIBLE else View.GONE
        systemFullscreen(enabled)
        if (enabled) web.requestFocus() else fullscreenButton.requestFocus()
    }
    @Volatile private var adFilterEnabled = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        exitFullscreen = Button(this).apply {
            text = "Tam ekrandan çık"
            styleButton(this)
            visibility = View.GONE
            setOnClickListener { setPageFullscreen(false) }
        }
        root.addView(exitFullscreen, FrameLayout.LayoutParams(-2, -2, android.view.Gravity.TOP or android.view.Gravity.END))
        button("← Kapat") { finish() }
        fullscreenButton = Button(this).apply {
            text = "⛶ Tam ekran"
            styleButton(this)
            setOnClickListener {
                setPageFullscreen(true)
                // Use the page's supported fullscreen API, never access cross-origin frame contents.
                web.evaluateJavascript("""
                    (function(){
                        var v=document.querySelector('video');
                        if(!v) return;
                        var f=v.requestFullscreen||v.webkitRequestFullscreen;
                        if(f) { try { var p=f.call(v); if(p&&p.catch)p.catch(function(){}); } catch(e){} }
                    })();
                """.trimIndent(), null)
            }
        }
        tools.addView(fullscreenButton)
        button("Geri") { if (web.canGoBack()) web.goBack() }
        button("Yenile") { web.reload() }
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
                status.text = "${Uri.parse(url).host.orEmpty()} · Yükleniyor…"
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (status.text.endsWith("Yükleniyor…")) status.text = "${Uri.parse(url).host.orEmpty()} · Sayfadaki oynat düğmesini seçin."
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame) status.text = "HTTP ${errorResponse.statusCode} · Site erişim vermedi. Harici uygulamada deneyebilirsiniz."
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) status.text = "Sayfa açılamadı: ${error.description}"
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                status.text = "Açılır pencere engellendi. Gerekirse Tarayıcıda aç seçeneğini kullanın."
                return false
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                if (full != null) { callback.onCustomViewHidden(); return }
                fullCallback = callback
                normal.visibility = View.GONE
                exitFullscreen.visibility = View.GONE
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
                when { full != null -> hideFull(); pageFullscreen -> setPageFullscreen(false); web.canGoBack() -> web.goBack(); else -> finish() }
            }
        })
        fullscreenButton.requestFocus()
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) web.loadUrl(pageUrl)
    }
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU && ::web.isInitialized) {
            if (full != null) hideFull()
            else if (pageFullscreen) setPageFullscreen(false)
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
        if (::fullscreenButton.isInitialized) setPageFullscreen(false)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        if (::web.isInitialized) web.saveState(outState)
        super.onSaveInstanceState(outState)
    }
    override fun onPause() { if (::web.isInitialized) web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if (::web.isInitialized) web.onResume() }
    override fun onDestroy() {
        if (::web.isInitialized) {
            hideFull(); web.stopLoading(); (web.parent as? ViewGroup)?.removeView(web)
            web.removeAllViews(); web.destroy()
        }
        super.onDestroy()
    }
}
