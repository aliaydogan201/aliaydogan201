package com.cinebox.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.graphics.Color;
import android.text.*;
import android.view.View;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class CatalogActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<CatalogParser.Entry> visible=new ArrayList<>();
    private List<CatalogParser.Entry> entries=Collections.emptyList();
    private ArrayAdapter<CatalogParser.Entry> adapter;
    private TextView status;
    private EditText pageInput, filter;
    private Button prev,next,go,reload;
    private int page=1,lastPage=1;
    private boolean busy=false;
    private String folder="";
    private LinearLayout root, folderView, nav;
    private ListView list;
    private TextView title;
    private volatile HttpURLConnection activeConnection;
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    private Button button(LinearLayout row,String label,View.OnClickListener click) {
        Button b=new Button(this);b.setText(label);b.setOnClickListener(click);row.addView(b);return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String initial=getIntent().getStringExtra("folder");
        if("Filmler".equals(initial)||"Diziler".equals(initial))folder=initial;
        if(state!=null) { page=Math.max(1,state.getInt("page",1)); folder=state.getString("folder", ""); }
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(12),dp(20),dp(12));root.setBackgroundColor(Color.rgb(14,18,28));
        LinearLayout top=new LinearLayout(this);
        button(top,"Geri",v->back());
        title=new TextView(this);title.setText("Film ve dizi kataloğu");title.setTextColor(Color.WHITE);title.setTextSize(22);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        button(top,"Tarayıcıda aç",v->open(CatalogParser.pageUrl(page)));root.addView(top);
        nav=new LinearLayout(this);
        prev=button(nav,"Önceki",v->load(page-1));next=button(nav,"Sonraki",v->load(page+1));
        pageInput=new EditText(this);pageInput.setTextColor(Color.WHITE);pageInput.setInputType(2);pageInput.setSingleLine(true);pageInput.setHint("Sayfa");pageInput.setHintTextColor(Color.LTGRAY);nav.addView(pageInput,new LinearLayout.LayoutParams(dp(95),-2));
        go=button(nav,"Git",v->{try { int n=Integer.parseInt(pageInput.getText().toString());if(n<1 || n>lastPage) throw new NumberFormatException();load(n); }catch(NumberFormatException e){Toast.makeText(this,"1–"+lastPage+" arası sayfa girin",Toast.LENGTH_SHORT).show();}});
        reload=button(nav,"Yenile",v->load(page));root.addView(nav);
        filter=new EditText(this);filter.setSingleLine(true);filter.setTextColor(Color.WHITE);filter.setHintTextColor(Color.LTGRAY);filter.setHint("Bu sayfadaki isimlerde ara");root.addView(filter);
        status=new TextView(this);status.setTextColor(Color.LTGRAY);root.addView(status);
        list=new ListView(this);adapter=new ArrayAdapter<CatalogParser.Entry>(this,android.R.layout.simple_list_item_1,visible) {
            @Override public View getView(int position,View convert,android.view.ViewGroup parent) {
                TextView v=(TextView)super.getView(position,convert,parent);v.setTextColor(Color.WHITE);v.setTextSize(20);v.setMinHeight(dp(54));return v;
            }
        };
        list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->open(visible.get(pos).url));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));folderView=new LinearLayout(this);folderView.setOrientation(LinearLayout.VERTICAL);
        Button films=button(folderView,"📁 Filmler",v->choose("Filmler"));
        Button series=button(folderView,"📁 Diziler",v->choose("Diziler"));
        films.setMinHeight(dp(90));series.setMinHeight(dp(90));
        root.addView(folderView,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){applyFilter();}public void afterTextChanged(Editable e){}});
        showFolder();
        if(!folder.isEmpty()) load(page);
    }
    private void choose(String value) { folder=value;filter.setText("");showFolder();if(entries.isEmpty()&&!busy)load(page);else applyFilter(); }
    private void showFolder() {
        boolean inside=!folder.isEmpty();
        title.setText(inside?"📁 "+folder:"Klasörler");
        folderView.setVisibility(inside?View.GONE:View.VISIBLE);
        for(View v:new View[]{nav,filter,status,list})v.setVisibility(inside?View.VISIBLE:View.GONE);
    }
    private void back() { if(getIntent().hasExtra("folder") || folder.isEmpty())finish();else {folder="";showFolder();} }
    @Override public void onBackPressed() {back();}
    private void controls() { prev.setEnabled(!busy && page>1);next.setEnabled(!busy && page<lastPage);go.setEnabled(!busy);reload.setEnabled(!busy);pageInput.setEnabled(!busy); }
    private void applyFilter() {
        String q=filter.getText().toString().toLowerCase(new Locale("tr","TR")).trim();visible.clear();
        for(CatalogParser.Entry e:entries) if(e.series == folder.equals("Diziler") && e.title.toLowerCase(new Locale("tr","TR")).contains(q)) visible.add(e);
        adapter.notifyDataSetChanged();
        if(!busy) status.setText("Kaynak sayfası "+page+" / "+lastPage+" • Bu sayfada "+visible.size()+" eşleşme. "+(visible.isEmpty()?"Diğer sayfalara geçebilirsiniz.":"Seçilen içerik tarayıcıda açılır."));
    }
    private void load(int requested) {
        if(busy || requested<1) return;
        busy=true;controls();status.setText("Sayfa "+requested+" yükleniyor…");
        worker.execute(()->{
            try {
                String url=CatalogParser.pageUrl(requested);
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();activeConnection=c;
                String html;
                try {
                    c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","CineBoxTV/0.13 (catalog reader)");
                    int code=c.getResponseCode();if(code!=200) throw new IOException("HTTP "+code);
                    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                        byte[] b=new byte[8192];int n,total=0;
                        while((n=in.read(b))!=-1) {total+=n;if(total>4*1024*1024) throw new IOException("Sayfa boyutu sınırı aşıldı");out.write(b,0,n);}
                        html=out.toString("UTF-8");
                    }
                } finally { c.disconnect();activeConnection=null; }
                CatalogParser.Page result=CatalogParser.parse(html,url,requested);
                if(result.entries.isEmpty()) throw new IOException("Katalog okunamadı; site yapısı değişmiş veya erişim engellenmiş olabilir");
                runOnUiThread(()->{if(isDestroyed())return;page=requested;lastPage=result.lastPage;entries=result.entries;pageInput.setText(String.valueOf(page));filter.setText("");applyFilter();busy=false;controls();applyFilter();});
            } catch(Exception e) { runOnUiThread(()->{if(isDestroyed())return;busy=false;controls();status.setText("Yüklenemedi: "+e.getMessage()+". Yenileyin veya tarayıcıda açın.");}); }
        });
    }
    private void open(String url) { try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"Bir web tarayıcısı yükleyin",Toast.LENGTH_LONG).show();} }
    @Override protected void onSaveInstanceState(Bundle out) {out.putInt("page",page);out.putString("folder",folder);super.onSaveInstanceState(out);}
    @Override protected void onDestroy() {worker.shutdownNow();HttpURLConnection c=activeConnection;if(c!=null)c.disconnect();super.onDestroy();}
}
