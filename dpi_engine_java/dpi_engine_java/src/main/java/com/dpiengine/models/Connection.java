package com.dpiengine.models;

public class Connection {
    public FiveTuple tuple;
    public ConnectionState state = ConnectionState.NEW;
    public AppType appType = AppType.UNKNOWN;
    public String sni = null;
    
    public long packetsIn = 0;
    public long packetsOut = 0;
    public long bytesIn = 0;
    public long bytesOut = 0;
    
    public long firstSeenMs;
    public long lastSeenMs;
    
    public PacketAction action = PacketAction.FORWARD;
    
    public boolean synSeen = false;
    public boolean synAckSeen = false;
    public boolean finSeen = false;

    public Connection(FiveTuple tuple) {
        this.tuple = tuple;
        long now = System.currentTimeMillis();
        this.firstSeenMs = now;
        this.lastSeenMs = now;
    }
}
