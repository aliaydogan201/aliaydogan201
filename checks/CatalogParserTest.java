import com.cinebox.tv.CatalogParser;
import java.nio.file.*;
public class CatalogParserTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  String base=CatalogParser.HOME;
  String card="<a class='poster' href='/film/'><h2 class='title'>A &amp; B</h2><div class='poster-year'>1999</div></a>";
  CatalogParser.Page p=CatalogParser.parse("<div id='moviesListResult'>"+card+card+"<a class='poster' href='https://evil.test/'><h2 class='title'>Bad</h2></a></div><a class='page-link' href='/page/1002/'>Son</a>",base,1);
  check(p.entries.size()==1,"dedupe and external rejection");check(p.entries.get(0).title.equals("A & B"),"entities");check(p.entries.get(0).year.equals("1999"),"year");check(p.lastPage==1002,"pagination");
  check(!p.entries.get(0).series,"film classification");
  CatalogParser.Page series=CatalogParser.parse("<div id='moviesListResult'><a class='poster' href='/dizi/test/'><h2 class='title'>Dizi</h2></a></div>",base,1);
  check(series.entries.size()==1 && series.entries.get(0).series,"series classification");
  check(!CatalogParser.allowed("https://www.hdfilmizle.to.evil.test/"),"suffix attack");check(!CatalogParser.allowed("javascript:alert(1)"),"scheme");check(!CatalogParser.allowed("https://user@www.hdfilmizle.to/"),"userinfo");check(CatalogParser.parse("blocked",base,1).entries.isEmpty(),"blocked");
  if(args.length>0){p=CatalogParser.parse(Files.readString(Path.of(args[0])),CatalogParser.pageUrl(2),2);check(p.entries.size()==20,"real page count: "+p.entries.size());check(p.lastPage>=1002,"real pagination");System.out.println("Live snapshot: "+p.entries.size()+" entries, last page "+p.lastPage);}
  System.out.println("Catalog parser tests passed");
 }
}
