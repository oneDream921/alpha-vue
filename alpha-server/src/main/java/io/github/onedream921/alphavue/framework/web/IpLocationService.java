package io.github.onedream921.alphavue.framework.web;

import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;

/** Optional offline ip2region lookup with a safe fallback when the XDB is absent. */
@Component
public class IpLocationService {
    private final Searcher searcher;

    public IpLocationService(@Value("${alpha.web.ip-location-xdb:}") String xdbPath) {
        this.searcher = open(xdbPath);
    }

    public String resolve(String address) {
        if (isPrivate(address)) return "内网 IP";
        if (searcher == null || address == null || address.isBlank()) return "未知";
        try {
            String region = searcher.search(address);
            return region == null || region.isBlank() ? "未知" : region;
        } catch (Exception ignored) {
            return "未知";
        }
    }

    @PreDestroy
    void close() {
        if (searcher != null) {
            try { searcher.close(); } catch (Exception ignored) { }
        }
    }

    private static Searcher open(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            File file = new File(path);
            return file.isFile() ? Searcher.newWithFileOnly(Version.IPv4, file) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isPrivate(String address) {
        if (address == null || address.isBlank()) return true;
        try {
            InetAddress ip = InetAddress.getByName(address);
            return ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress()
                    || ip.isSiteLocalAddress();
        } catch (Exception ignored) {
            return false;
        }
    }
}
