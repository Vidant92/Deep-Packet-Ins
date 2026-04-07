package com.dpiengine.models;

import java.util.Objects;

public class FiveTuple {
    private final int srcIp;
    private final int dstIp;
    private final int srcPort;
    private final int dstPort;
    private final int protocol; // TCP=6, UDP=17

    public FiveTuple(int srcIp, int dstIp, int srcPort, int dstPort, int protocol) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort & 0xFFFF; // Treat as unsigned short
        this.dstPort = dstPort & 0xFFFF;
        this.protocol = protocol & 0xFF; // Treat as unsigned byte
    }

    public int getSrcIp() {
        return srcIp;
    }

    public int getDstIp() {
        return dstIp;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public int getProtocol() {
        return protocol;
    }

    public FiveTuple reverse() {
        return new FiveTuple(dstIp, srcIp, dstPort, srcPort, protocol);
    }

    private String formatIp(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip & 0xFF),
                ((ip >> 8) & 0xFF),
                ((ip >> 16) & 0xFF),
                ((ip >> 24) & 0xFF));
    }

    @Override
    public String toString() {
        String protoStr = (protocol == 6) ? "TCP" : (protocol == 17) ? "UDP" : "?";
        return String.format("%s:%d -> %s:%d (%s)",
                formatIp(srcIp), srcPort, formatIp(dstIp), dstPort, protoStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FiveTuple fiveTuple = (FiveTuple) o;
        return srcIp == fiveTuple.srcIp &&
                dstIp == fiveTuple.dstIp &&
                srcPort == fiveTuple.srcPort &&
                dstPort == fiveTuple.dstPort &&
                protocol == fiveTuple.protocol;
    }

    @Override
    public int hashCode() {
        int h = 0;
        h ^= Integer.hashCode(srcIp) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= Integer.hashCode(dstIp) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= Integer.hashCode(srcPort) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= Integer.hashCode(dstPort) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= Integer.hashCode(protocol) + 0x9e3779b9 + (h << 6) + (h >> 2);
        return h;
    }
}
