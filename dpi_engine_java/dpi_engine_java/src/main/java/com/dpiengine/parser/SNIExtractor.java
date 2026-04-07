package com.dpiengine.parser;

public class SNIExtractor {

    public static String extract(byte[] payload, int offset, int length) {
        if (length < 9) return null;
        
        // Check TLS record header
        if (payload[offset] != 0x16) return null; // Content Type = Handshake
        
        int version = readUint16BE(payload, offset + 1);
        if (version < 0x0300 || version > 0x0304) return null;
        
        int recordLength = readUint16BE(payload, offset + 3);
        if (recordLength > length - 5) return null;
        
        if (payload[offset + 5] != 0x01) return null; // Handshake Type = Client Hello
        
        int currentOffset = offset + 5;
        
        int handshakeLength = readUint24BE(payload, currentOffset + 1);
        currentOffset += 4;
        
        // Client version
        currentOffset += 2;
        
        // Random
        currentOffset += 32;
        
        // Session ID
        if (currentOffset >= offset + length) return null;
        int sessionIdLength = payload[currentOffset] & 0xFF;
        currentOffset += 1 + sessionIdLength;
        
        // Cipher suites
        if (currentOffset + 2 > offset + length) return null;
        int cipherSuitesLength = readUint16BE(payload, currentOffset);
        currentOffset += 2 + cipherSuitesLength;
        
        // Compression methods
        if (currentOffset >= offset + length) return null;
        int compressionMethodsLength = payload[currentOffset] & 0xFF;
        currentOffset += 1 + compressionMethodsLength;
        
        // Extensions
        if (currentOffset + 2 > offset + length) return null;
        int extensionsLength = readUint16BE(payload, currentOffset);
        currentOffset += 2;
        
        int extensionsEnd = currentOffset + extensionsLength;
        if (extensionsEnd > offset + length) {
            extensionsEnd = offset + length;
        }
        
        while (currentOffset + 4 <= extensionsEnd) {
            int extType = readUint16BE(payload, currentOffset);
            int extLen = readUint16BE(payload, currentOffset + 2);
            currentOffset += 4;
            
            if (currentOffset + extLen > extensionsEnd) break;
            
            if (extType == 0x0000) { // SNI
                if (extLen < 5) break;
                
                int sniListLen = readUint16BE(payload, currentOffset);
                if (sniListLen < 3) break;
                
                int sniType = payload[currentOffset + 2] & 0xFF;
                int sniLen = readUint16BE(payload, currentOffset + 3);
                
                if (sniType != 0x00) break;
                if (sniLen > extLen - 5) break;
                
                return new String(payload, currentOffset + 5, sniLen);
            }
            
            currentOffset += extLen;
        }
        
        return null; // Not found
    }

    public static String extractHTTPHost(byte[] payload, int offset, int length) {
        if (length < 4) return null;
        
        boolean isHttp = false;
        if (payload[offset] == 'G' && payload[offset+1] == 'E' && payload[offset+2] == 'T') isHttp = true;
        else if (payload[offset] == 'P' && payload[offset+1] == 'O' && payload[offset+2] == 'S' && payload[offset+3] == 'T') isHttp = true;
        else if (payload[offset] == 'H' && payload[offset+1] == 'E' && payload[offset+2] == 'A' && payload[offset+3] == 'D') isHttp = true;
        // Basic check above to allow simple extraction. Real HTTP method check omitted for brevity.
        
        if (!isHttp) return null;
        
        String str = new String(payload, offset, length);
        int hostIdx = str.toLowerCase().indexOf("host: ");
        if (hostIdx != -1) {
            int start = hostIdx + 6;
            while (start < str.length() && (str.charAt(start) == ' ' || str.charAt(start) == '\t')) {
                start++;
            }
            int end = start;
            while (end < str.length() && str.charAt(end) != '\r' && str.charAt(end) != '\n') {
                end++;
            }
            if (end > start) {
                String host = str.substring(start, end);
                int colon = host.indexOf(':');
                if (colon != -1) host = host.substring(0, colon);
                return host.trim();
            }
        }
        return null;
    }

    private static int readUint16BE(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }
    
    private static int readUint24BE(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF);
    }
}
