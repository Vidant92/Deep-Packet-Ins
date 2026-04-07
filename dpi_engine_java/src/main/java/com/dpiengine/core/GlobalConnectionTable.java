package com.dpiengine.core;

import com.dpiengine.models.AppType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalConnectionTable {

    public static class GlobalStats {
        public long totalActiveConnections = 0;
        public long totalConnectionsSeen = 0;
        public Map<AppType, Long> appDistribution = new HashMap<>();
        public List<Map.Entry<String, Long>> topDomains = new ArrayList<>();
    }

    private final ConnectionTracker[] trackers;

    public GlobalConnectionTable(int numFps) {
        trackers = new ConnectionTracker[numFps];
    }

    public synchronized void registerTracker(int fpId, ConnectionTracker tracker) {
        if (fpId >= 0 && fpId < trackers.length) {
            trackers[fpId] = tracker;
        }
    }

    public synchronized GlobalStats getGlobalStats() {
        GlobalStats stats = new GlobalStats();
        Map<String, Long> domainCounts = new HashMap<>();

        for (ConnectionTracker tracker : trackers) {
            if (tracker == null) continue;
            
            ConnectionTracker.TrackerStats tStats = tracker.getStats();
            stats.totalActiveConnections += tStats.activeConnections;
            stats.totalConnectionsSeen += tStats.totalConnectionsSeen;
            
            tracker.forEach(conn -> {
                stats.appDistribution.put(conn.appType, stats.appDistribution.getOrDefault(conn.appType, 0L) + 1);
                if (conn.sni != null && !conn.sni.isEmpty()) {
                    domainCounts.put(conn.sni, domainCounts.getOrDefault(conn.sni, 0L) + 1);
                }
            });
        }
        
        List<Map.Entry<String, Long>> domainVec = new ArrayList<>(domainCounts.entrySet());
        domainVec.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        stats.topDomains = domainVec.subList(0, Math.min(20, domainVec.size()));
        
        return stats;
    }

    public String generateReport() {
        GlobalStats stats = getGlobalStats();
        
        StringBuilder b = new StringBuilder();
        b.append("\n==============================================================\n");
        b.append("               CONNECTION STATISTICS REPORT                    \n");
        b.append("==============================================================\n");
        
        b.append(String.format(" Active Connections:     %10d                          \n", stats.totalActiveConnections));
        b.append(String.format(" Total Connections Seen: %10d                          \n", stats.totalConnectionsSeen));
        
        b.append("==============================================================\n");
        b.append("                    APPLICATION BREAKDOWN                      \n");
        b.append("==============================================================\n");
        
        long totalApp = stats.appDistribution.values().stream().mapToLong(Long::longValue).sum();
        
        List<Map.Entry<AppType, Long>> sortedApps = new ArrayList<>(stats.appDistribution.entrySet());
        sortedApps.sort((a1, a2) -> Long.compare(a2.getValue(), a1.getValue()));
        
        for (Map.Entry<AppType, Long> pair : sortedApps) {
            double pct = totalApp > 0 ? (100.0 * pair.getValue() / totalApp) : 0;
            b.append(String.format(" %-20s %10d (%.1f%%)           \n", pair.getKey().toString(), pair.getValue(), pct));
        }
        
        if (!stats.topDomains.isEmpty()) {
            b.append("==============================================================\n");
            b.append("                      TOP DOMAINS                             \n");
            b.append("==============================================================\n");
            for (Map.Entry<String, Long> pair : stats.topDomains) {
                String domain = pair.getKey();
                if (domain.length() > 35) {
                    domain = domain.substring(0, 32) + "...";
                }
                b.append(String.format(" %-40s %10d           \n", domain, pair.getValue()));
            }
        }
        b.append("==============================================================\n");
        
        return b.toString();
    }
}
