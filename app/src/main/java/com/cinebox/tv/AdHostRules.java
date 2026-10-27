package com.cinebox.tv;
import java.util.Locale;
/** Small, local host list. Does not block video CDNs or inspect URL query text. */
public final class AdHostRules {
    private AdHostRules() {}
    private static final String[] HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adnxs.com", "pubmatic.com", "openx.net", "rubiconproject.com",
        "popads.net", "popcash.net", "adsterra.com", "propellerads.com"
    };
    public static boolean isBlockedHost(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);
        for (String domain : HOSTS) {
            if (normalized.equals(domain) || normalized.endsWith("." + domain)) return true;
        }
        return false;
    }
}
