package com.dpiengine.parser;

import com.dpiengine.models.FiveTuple;
import com.dpiengine.models.PacketJob;

public class PacketParser {
    public static final int PROTOCOL_TCP = 6;
    public static final int PROTOCOL_UDP = 17;

    public static boolean parse(PacketJob job) {
        byte[] data = job.data;
        int len = data.length;
        int offset = 0;

        // 1. Ethernet Header (14 bytes)
        if (len < 14) return false;
        int etherType = ((data[12] & 0xFF) << 8) | (data[13] & 0xFF);
        offset = 14;
        job.ethOffset = 0;

        // 2. IPv4 Header
        if (etherType == 0x0800) {
            job.ipOffset = offset;
            if (len < offset + 20) return false;

            int versionIhl = data[offset] & 0xFF;
            int version = versionIhl >> 4;
            if (version != 4) return false;

            int ihl = versionIhl & 0x0F;
            int ipHeaderLen = ihl * 4;
            if (ipHeaderLen < 20 || len < offset + ipHeaderLen) return false;

            int protocol = data[offset + 9] & 0xFF;

            // IPs are in Network byte order (big endian)
            int srcIp = ((data[offset + 12] & 0xFF) << 24) | ((data[offset + 13] & 0xFF) << 16) | ((data[offset + 14] & 0xFF) << 8) | (data[offset + 15] & 0xFF);
            int dstIp = ((data[offset + 16] & 0xFF) << 24) | ((data[offset + 17] & 0xFF) << 16) | ((data[offset + 18] & 0xFF) << 8) | (data[offset + 19] & 0xFF);

            offset += ipHeaderLen;
            job.transportOffset = offset;

            int srcPort = 0;
            int dstPort = 0;

            // 3. Transport Header
            if (protocol == PROTOCOL_TCP) {
                if (len < offset + 20) return false;
                srcPort = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                dstPort = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
                
                int dataOffset = (data[offset + 12] >> 4) & 0x0F;
                int tcpHeaderLen = dataOffset * 4;
                
                job.tcpFlags = data[offset + 13];
                
                if (tcpHeaderLen < 20 || len < offset + tcpHeaderLen) return false;
                offset += tcpHeaderLen;
            } else if (protocol == PROTOCOL_UDP) {
                if (len < offset + 8) return false;
                srcPort = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                dstPort = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
                offset += 8;
            } else {
                return false; // Not TCP or UDP in this case 
            }

            job.payloadOffset = offset;
            job.payloadLength = len - offset;
            job.tuple = new FiveTuple(srcIp, dstIp, srcPort, dstPort, protocol);
            
            return true;
        }

        return false;
    }
}
