import com.cinebox.tv.AdHostRules;
public class AdHostRulesTest {
    private static void check(String host, boolean expected) {
        if (AdHostRules.isBlockedHost(host) != expected) throw new AssertionError("Host: " + host);
    }
    public static void main(String[] args) {
        check("doubleclick.net", true);
        check("ads.doubleclick.net", true);
        check("ADS.DOUBLECLICK.NET.", true);
        check("popads.net", true);
        check("notdoubleclick.net", false);
        check("doubleclick.net.example.org", false);
        check("www.youtube.com", false);
        check("rr1.googlevideo.com", false);
        check("www.kanald.com.tr", false);
        check("www.diziizle.you", false);
        check("archive.org", false);
        check("", false);
        check(null, false);
        System.out.println("13 ad-host matching tests passed");
    }
}
