package com.dpiengine.core;

import com.dpiengine.models.AppType;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class RuleManager {

    public static class BlockReason {
        public enum Type { IP, APP, DOMAIN, PORT }
        public Type type;
        public String detail;
        
        public BlockReason(Type type, String detail) {
            this.type = type;
            this.detail = detail;
        }
    }

    private final Set<Integer> blockedIps = new HashSet<>();
    private final Set<AppType> blockedApps = new HashSet<>();
    private final Set<String> blockedDomains = new HashSet<>();
    private final List<String> domainPatterns = new ArrayList<>();
    private final Set<Integer> blockedPorts = new HashSet<>();

    public synchronized void blockIp(int ip) {
        blockedIps.add(ip);
    }
    
    public synchronized void blockIp(String ipStr) {
        blockIp(parseIp(ipStr));
    }

    public synchronized void blockApp(AppType app) {
        blockedApps.add(app);
    }

    public synchronized void blockDomain(String domain) {
        if (domain.contains("*")) {
            domainPatterns.add(domain);
        } else {
            blockedDomains.add(domain);
        }
    }

    public synchronized void blockPort(int port) {
        blockedPorts.add(port);
    }

    public synchronized boolean isIpBlocked(int ip) {
        return blockedIps.contains(ip);
    }

    public synchronized boolean isAppBlocked(AppType app) {
        return blockedApps.contains(app);
    }

    public synchronized boolean isPortBlocked(int port) {
        return blockedPorts.contains(port);
    }

    public synchronized boolean isDomainBlocked(String domain) {
        if (blockedDomains.contains(domain)) return true;
        
        String lowerDomain = domain.toLowerCase();
        for (String pattern : domainPatterns) {
            String lowerPattern = pattern.toLowerCase();
            if (domainMatchesPattern(lowerDomain, lowerPattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean domainMatchesPattern(String domain, String pattern) {
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1);
            if (domain.endsWith(suffix) || domain.equals(pattern.substring(2))) {
                return true;
            }
        }
        return false;
    }

    public BlockReason shouldBlock(int srcIp, int dstPort, AppType app, String domain) {
        if (isIpBlocked(srcIp)) return new BlockReason(BlockReason.Type.IP, formatIp(srcIp));
        if (isPortBlocked(dstPort)) return new BlockReason(BlockReason.Type.PORT, String.valueOf(dstPort));
        if (isAppBlocked(app)) return new BlockReason(BlockReason.Type.APP, app.toString());
        if (domain != null && !domain.isEmpty() && isDomainBlocked(domain)) {
            return new BlockReason(BlockReason.Type.DOMAIN, domain);
        }
        return null; // Don't block
    }

    private int parseIp(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return 0;
        return ((Integer.parseInt(parts[0]) & 0xFF) << 24) |
               ((Integer.parseInt(parts[1]) & 0xFF) << 16) |
               ((Integer.parseInt(parts[2]) & 0xFF) << 8) |
               (Integer.parseInt(parts[3]) & 0xFF);
    }

    private String formatIp(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }
}
