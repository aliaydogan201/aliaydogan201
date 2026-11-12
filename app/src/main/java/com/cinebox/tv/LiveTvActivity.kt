package com.cinebox.tv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import org.jsoup.Jsoup
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Shared player: resolves only public HTTPS manifests exposed by the publisher. */
class LiveTvActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var task: Future<*>? = null
    private var generation = 0
    private lateinit var player: ExoPlayer
    private lateinit var surface: PlayerView
    private lateinit var list: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var controls: LinearLayout
    private var channels = emptyList<Movie>()
    private var selected = 0
    private var active = false
    private val cache = java.util.concurrent.ConcurrentHashMap<Int, Pair<Long, String>>()
    private val orange = Color.rgb(244, 132, 35)
    private fun label(s: String, size: Float) = TextView(this).apply { text=s; textSize=size; setTextColor(Color.WHITE); setPadding(16,12,16,12) }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text=s; isAllCaps=false; setTextColor(Color.WHITE); textSize=16f; minHeight=58
        fun paint(focus: Boolean) { background=GradientDrawable().apply { cornerRadius=12f; setColor(if(focus) orange else Color.rgb(49,38,31)); setStroke(if(focus) 4 else 1, if(focus) Color.WHITE else orange) }; setTextColor(if(focus) Color.BLACK else Color.WHITE) }
        paint(false); setOnFocusChangeListener { _, focused -> paint(focused) }; setOnClickListener { action() }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        channels=LiveTvCatalog.load(this).filter { LiveTvCatalog.entry(it.id)?.status == "live_page" && it.webUrl.startsWith("https://") }.sortedBy { it.title.lowercase(java.util.Locale.forLanguageTag("tr")) }
        if(channels.isEmpty()) { finish(); return }
        selected=channels.indexOfFirst { it.id==intent.getIntExtra("movie_id",-1) }.coerceAtLeast(0)
        player=ExoPlayer.Builder(this).build()
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(20,17,15)) }
        title=label("ALICOTV • CANLI",22f).apply { setTypeface(null,Typeface.BOLD) }; root.addView(title)
        val body=LinearLayout(this); root.addView(body,LinearLayout.LayoutParams(-1,0,1f))
        sidebar=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        body.addView(sidebar,LinearLayout.LayoutParams((280*resources.displayMetrics.density).toInt(),-1))
        val search=EditText(this).apply { hint="Kanal ara"; setSingleLine(); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY) }; sidebar.addView(search)
        val scroll=ScrollView(this); sidebar.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; scroll.addView(list)
        fun populate(q: String) { list.removeAllViews(); channels.forEachIndexed { index,m -> if(m.title.lowercase(java.util.Locale.forLanguageTag("tr")).contains(q.lowercase(java.util.Locale.forLanguageTag("tr")))) list.addView(button("${index+1}   ${m.title}") { tune(index) },LinearLayout.LayoutParams(-1,-2).apply { setMargins(8,4,8,4) }) } }
        search.addTextChangedListener(object: android.text.TextWatcher { override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}; override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){populate(s.toString())}; override fun afterTextChanged(s:android.text.Editable?){} }); populate("")
        val right=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; body.addView(right,LinearLayout.LayoutParams(0,-1,1f))
        surface=PlayerView(this).apply { player=this@LiveTvActivity.player; controllerShowTimeoutMs=3000 }; right.addView(surface,LinearLayout.LayoutParams(-1,0,1f))
        status=label("",15f); right.addView(status)
        controls=LinearLayout(this); right.addView(controls)
        controls.addView(button("◀ Önceki") { tune((selected-1+channels.size)%channels.size) })
        controls.addView(button("Sonraki ▶") { tune((selected+1)%channels.size) })
        controls.addView(button("Tam ekran / Liste") { togglePanel() })
        controls.addView(button("Tekrar dene") { cache.remove(channels[selected].id); tune(selected) })
        player.addListener(object: Player.Listener {
            override fun onPlaybackStateChanged(state:Int) { status.text=when(state) { Player.STATE_READY -> "● CANLI   •   CH+/CH−: kanal değiştir   •   Geri: kanal listesi"; Player.STATE_BUFFERING -> "Yayın yükleniyor…"; else -> status.text } }
            override fun onPlayerError(error:PlaybackException) { cache.remove(channels[selected].id); status.text="Yayın açılamadı (${error.errorCodeName}). Tekrar deneyin veya başka kanal seçin." }
        })
        setContentView(root); active=true; tune(selected); list.getChildAt(selected)?.requestFocus()
    }
    private fun togglePanel() { sidebar.visibility=if(sidebar.visibility==View.VISIBLE) View.GONE else View.VISIBLE; title.visibility=sidebar.visibility; status.visibility=sidebar.visibility; controls.visibility=sidebar.visibility; if(sidebar.visibility==View.VISIBLE) list.getChildAt(selected)?.requestFocus() else surface.requestFocus() }
    private fun tune(index:Int) {
        selected=index; generation++; val token=generation; task?.cancel(true); player.stop(); player.clearMediaItems()
        val channel=channels[index]; title.text="ALICOTV   /   ${channel.title}   •   CANLI"; status.text="Yayın bağlantısı aranıyor…"
        task=worker.submit {
            val cached=cache[channel.id]?.takeIf { System.currentTimeMillis()-it.first<120000 }?.second
            val url=cached ?: runCatching { resolve(channel.webUrl,0,mutableSetOf(),System.currentTimeMillis()+15000) }.getOrNull()
            handler.post {
                if(token!=generation || !active || isFinishing) return@post
                if(url==null) { status.text="Bu kanalın ortak oynatıcı bağlantısı bulunamadı. Kanalın yayıncı/YouTube seçeneğini kullanabilirsiniz."; return@post }
                cache[channel.id]=System.currentTimeMillis() to url
                val http=DefaultHttpDataSource.Factory().setUserAgent("ALICOTV/63").setConnectTimeoutMs(8000).setReadTimeoutMs(12000).setDefaultRequestProperties(mapOf("Referer" to channel.webUrl))
                player.setMediaSource(DefaultMediaSourceFactory(http).createMediaSource(MediaItem.fromUri(url))); player.prepare(); player.playWhenReady=true
            }
        }
    }
    private fun resolve(url:String,depth:Int,seen:MutableSet<String>, deadline:Long):String? {
        if(System.currentTimeMillis()>deadline || Thread.currentThread().isInterrupted || depth>2 || !url.startsWith("https://") || !seen.add(url)) return null
        if(URI(url).path.orEmpty().endsWith(".m3u8",true)) return url
        val doc=Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(6000).maxBodySize(1500000).get()
        val html=doc.html().replace("\\/","/").replace("&amp;","&")
        val stream=Regex("https://[^\\s\"'<>\\\\]+\\.m3u8(?:\\?[^\\s\"'<>\\\\]*)?",RegexOption.IGNORE_CASE).find(html)?.value
        if(stream!=null) return stream
        for(frame in doc.select("iframe[src]").take(3)) {
            val next=frame.absUrl("src")
            if(next.contains("youtube.com") || next.contains("doubleclick")) continue
            val found=runCatching { resolve(next,depth+1,seen,deadline) }.getOrNull(); if(found!=null)return found
        }
        return null
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(event.keyCode==KeyEvent.KEYCODE_CHANNEL_UP || event.keyCode==KeyEvent.KEYCODE_CHANNEL_DOWN) { if(event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0) tune((selected+(if(event.keyCode==KeyEvent.KEYCODE_CHANNEL_UP) 1 else channels.size-1))%channels.size); return true }; return super.dispatchKeyEvent(event)
    }
    @Deprecated("Legacy Activity back handler") override fun onBackPressed() { if(sidebar.visibility!=View.VISIBLE) togglePanel() else super.onBackPressed() }
    override fun onStop() { active=false; generation++; task?.cancel(true); if(::player.isInitialized) player.pause(); super.onStop() }
    override fun onStart() { super.onStart(); if(::player.isInitialized && !active) { active=true; tune(selected) } }
    override fun onDestroy() { generation++; handler.removeCallbacksAndMessages(null); task?.cancel(true); worker.shutdownNow(); if(::surface.isInitialized) surface.player=null; if(::player.isInitialized) player.release(); super.onDestroy() }
}
