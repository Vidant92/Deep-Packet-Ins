package com.dpiengine.models;

public class PacketJob {
    public int packetId;
    public FiveTuple tuple;
    
    // The raw packet data
    public byte[] data;
    
    // Parsed offsets
    public int ethOffset = 0;
    public int ipOffset = 0;
    public int transportOffset = 0;
    public int payloadOffset = 0;
    public int payloadLength = 0;
    
    public byte tcpFlags = 0;
    
    // Timestamps
    public long tsSec;
    public long tsUsec;

    // Optional packet length info from Header
    public int inclLen;
    public int origLen;
}
