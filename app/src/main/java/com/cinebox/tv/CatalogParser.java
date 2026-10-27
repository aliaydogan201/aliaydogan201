package com.cinebox.tv;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Reads public catalogue metadata only; never executes page scripts. */
public final class CatalogParser {
    public static final String HOME = "https://filmmakinesi.to/";
    public static final class Entry {
        public final String title, url, year;
        public final boolean series;
        Entry(String title, String url, String year) { this.title=title; this.url=url; this.year=year; this.series=URI.create(url).getPath().startsWith("/dizi/"); }
        @Override public String toString() { return title + (year.isEmpty() ? "" : "  •  " + year); }
    }
    public static final class Page {
        public final List<Entry> entries;
        public final int lastPage;
        Page(List<Entry> entries, int lastPage) { this.entries=Collections.unmodifiableList(entries); this.lastPage=lastPage; }
    }
    public static boolean allowed(String url) {
        try {
            URI u=URI.create(url);
            return "https".equalsIgnoreCase(u.getScheme()) && "filmmakinesi.to".equalsIgnoreCase(u.getHost()) && u.getUserInfo()==null && (u.getPort()==-1 || u.getPort()==443);
        } catch (Exception e) { return false; }
    }
    public static String pageUrl(int page, boolean series) {
        if(page<1) throw new IllegalArgumentException("Geçersiz sayfa");
        String base=HOME+(series?"yabanci-dizi-izle-2/":"filmler-1/");
        return page==1 ? base : base+"sayfa/"+page+"/";
    }
    public static Page parse(String html, String baseUrl, int currentPage) {
        Document doc=Jsoup.parse(html,baseUrl);
        LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
        for(Element a:doc.select("a.item[href]")) {
            String url=a.absUrl("href");
            if(!allowed(url) || !URI.create(url).getPath().matches("/(film|dizi)/[^/]+/")) continue;
            Element heading=a.selectFirst(".title, .item-title");
            String title=a.attr("data-title").trim();
            if(title.isEmpty() && heading!=null) title=heading.text().trim();
            Element year=a.selectFirst(".item-footer .info span");
            if(!title.isEmpty() && allowed(url)) entries.putIfAbsent(url,new Entry(title,url,year==null?"":year.text()));
        }
        int last=currentPage;
        Pattern pages=Pattern.compile("/(?:filmler-1|yabanci-dizi-izle-2)/sayfa/(\\d+)/?");
        for(Element a:doc.select("a.page-link[href]")) {
            String url=a.absUrl("href");
            if(!allowed(url)) continue;
            if(baseUrl.contains("yabanci-dizi-izle-2") && !url.contains("/yabanci-dizi-izle-2/"))continue;
            if(baseUrl.contains("filmler-1") && !url.contains("/filmler-1/"))continue;
            Matcher m=pages.matcher(URI.create(url).getPath());
            if(m.matches()) try { last=Math.max(last,Integer.parseInt(m.group(1))); } catch(NumberFormatException ignored) {}
        }
        return new Page(new ArrayList<>(entries.values()),last);
    }
}
