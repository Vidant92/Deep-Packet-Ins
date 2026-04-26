package com.dpiengine.core;

import com.dpiengine.models.AppType;
import com.dpiengine.models.Connection;
import com.dpiengine.models.ConnectionState;
import com.dpiengine.models.FiveTuple;
import com.dpiengine.models.PacketAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConnectionTracker {
    public static class TrackerStats {
        public long activeConnections;
        public long totalConnectionsSeen;
        public long classifiedConnections;
        public long blockedConnections;
    }

    private final int fpId;
    private final int maxConnections;
    private final Map<FiveTuple, Connection> connections = new HashMap<>();
    
    private long totalSeen = 0;
    private long classifiedCount = 0;
    private long blockedCount = 0;

    public ConnectionTracker(int fpId, int maxConnections) {
        this.fpId = fpId;
        this.maxConnections = maxConnections;
    }

    public Connection getOrCreateConnection(FiveTuple tuple) {
        Connection conn = connections.get(tuple);
        if (conn != null) return conn;

        if (connections.size() >= maxConnections) {
            evictOldest();
        }

        conn = new Connection(tuple);
        connections.put(tuple, conn);
        totalSeen++;
        return conn;
    }

    public Connection getConnection(FiveTuple tuple) {
        Connection conn = connections.get(tuple);
        if (conn != null) return conn;
        return connections.get(tuple.reverse());
    }

    public void updateConnection(Connection conn, int packetSize, boolean isOutbound) {
        if (conn == null) return;
        conn.lastSeenMs = System.currentTimeMillis();
        
        if (isOutbound) {
            conn.packetsOut++;
            conn.bytesOut += packetSize;
        } else {
            conn.packetsIn++;
            conn.bytesIn += packetSize;
        }
    }

    public void classifyConnection(Connection conn, AppType app, String sni) {
        if (conn == null) return;
        if (conn.state != ConnectionState.CLASSIFIED) {
            conn.appType = app;
            conn.sni = sni;
            conn.state = ConnectionState.CLASSIFIED;
            classifiedCount++;
        }
    }

    public void blockConnection(Connection conn) {
        if (conn == null) return;
        conn.state = ConnectionState.BLOCKED;
        conn.action = PacketAction.DROP;
        blockedCount++;
    }

    public void closeConnection(FiveTuple tuple) {
        Connection conn = connections.get(tuple);
        if (conn != null) {
            conn.state = ConnectionState.CLOSED;
        }
    }

    public int cleanupStale(long timeoutMs) {
        long now = System.currentTimeMillis();
        int removed = 0;
        
        List<FiveTuple> toRemove = new ArrayList<>();
        for (Map.Entry<FiveTuple, Connection> entry : connections.entrySet()) {
            Connection conn = entry.getValue();
            if ((now - conn.lastSeenMs) > timeoutMs || conn.state == ConnectionState.CLOSED) {
                toRemove.add(entry.getKey());
                removed++;
            }
        }
        
        for (FiveTuple k : toRemove) { // ye wala tha error m
            connections.remove(k);
        }
        
        return removed;
    }

    public TrackerStats getStats() {
        TrackerStats stats = new TrackerStats();
        stats.activeConnections = connections.size();
        stats.totalConnectionsSeen = totalSeen;
        stats.classifiedConnections = classifiedCount;
        stats.blockedConnections = blockedCount;
        return stats;
    }

    public void forEach(Consumer<Connection> consumer) {
        for (Connection c : connections.values()) {
            consumer.accept(c);
        }
    }

    private void evictOldest() {
        if (connections.isEmpty()) return;
        
        FiveTuple oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<FiveTuple, Connection> entry : connections.entrySet()) {
            if (entry.getValue().lastSeenMs < oldestTime) {
                oldestTime = entry.getValue().lastSeenMs;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) connections.remove(oldestKey);
    }
}
