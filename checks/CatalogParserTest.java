import com.cinebox.tv.CatalogParser;
import java.nio.file.*;
public class CatalogParserTest {
 public static void main(String[] args)throws Exception {
  String html=Files.readString(Path.of(args[0]));
  CatalogParser.Page p=CatalogParser.parse(html,CatalogParser.pageUrl(1,false),1);
  long series=p.entries.stream().filter(e->e.series).count();
  if(p.entries.size()!=38 || series!=6 || p.lastPage!=1135)throw new AssertionError("snapshot mismatch "+p.entries.size()+" "+series+" "+p.lastPage);
  if(CatalogParser.allowed("https://filmmakinesi.to.evil.test/"))throw new AssertionError("host validation");
  if(!CatalogParser.pageUrl(2,false).equals("https://filmmakinesi.to/filmler-1/sayfa/2/"))throw new AssertionError("page path");
  if(!CatalogParser.parse("<html>Access denied</html>",CatalogParser.HOME,1).entries.isEmpty())throw new AssertionError("block detection");
  System.out.println("PASS: 32 films, 6 series, 1135 pages, host validation, empty response and page URL");
 }
}
