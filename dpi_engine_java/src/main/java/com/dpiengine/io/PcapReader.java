package com.dpiengine.io;

import com.dpiengine.models.PacketJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PcapReader {
    private static final int PCAP_MAGIC_NATIVE = 0xa1b2c3d4;
    private static final int PCAP_MAGIC_SWAPPED = 0xd4c3b2a1;

    private FileInputStream file;
    private boolean needsByteSwap;
    private int snaplen;

    public boolean open(String filename) {
        close();
        try {
            file = new FileInputStream(new File(filename));
            byte[] globalHeader = new byte[24];
            int read = file.read(globalHeader);
            if (read != 24) {
                System.err.println("Error: Could not read PCAP global header");
                return false;
            }

            int magicNumber = readIntNative(globalHeader, 0);
            if (magicNumber == PCAP_MAGIC_NATIVE) {
                needsByteSwap = false;
            } else if (magicNumber == PCAP_MAGIC_SWAPPED) {
                needsByteSwap = true;
            } else {
                System.err.printf("Error: Invalid PCAP magic number: 0x%x\n", magicNumber);
                close();
                return false;
            }

            snaplen = readInt(globalHeader, 16);
            int network = readInt(globalHeader, 20);

            System.out.println("Opened PCAP file: " + filename);
            System.out.println("  Snaplen: " + snaplen + " bytes");
            System.out.println("  Link type: " + network + (network == 1 ? " (Ethernet)" : ""));
            return true;
        } catch (IOException e) {
            System.err.println("Error: Could not open file: " + filename);
            return false;
        }
    }

    public void close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                // Ignore
            }
            file = null;
        }
        needsByteSwap = false;
    }

    public boolean readNextPacket(PacketJob packet) {
        if (file == null) return false;
        try {
            byte[] packetHeader = new byte[16];
            int read = file.read(packetHeader);
            if (read != 16) {
                return false; // EOF or error
            }

            packet.tsSec = readInt(packetHeader, 0) & 0xFFFFFFFFL;
            packet.tsUsec = readInt(packetHeader, 4) & 0xFFFFFFFFL;
            packet.inclLen = readInt(packetHeader, 8);
            packet.origLen = readInt(packetHeader, 12);

            if (packet.inclLen < 0 || packet.inclLen > snaplen || packet.inclLen > 65535) {
                System.err.println("Error: Invalid packet length: " + packet.inclLen);
                return false;
            }

            packet.data = new byte[packet.inclLen];
            read = file.read(packet.data);
            if (read != packet.inclLen) {
                System.err.println("Error: Could not read packet data");
                return false;
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error: Exception reading packet: " + e.getMessage());
            return false;
        }
    }

    private int readIntNative(byte[] buf, int offset) { // error removed in  this  shift operations 
        return ((buf[offset] & 0xFF) << 24) |
               ((buf[offset + 1] & 0xFF) << 16) |
               ((buf[offset + 2] & 0xFF) << 8) |
               (buf[offset + 3] & 0xFF);
    }

    private int readInt(byte[] buf, int offset) {
        int val = readIntNative(buf, offset);
        if (needsByteSwap) {
            return Integer.reverseBytes(val);
        }
        return val;
    }
}
