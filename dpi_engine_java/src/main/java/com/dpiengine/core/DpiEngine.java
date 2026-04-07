package com.dpiengine.core;

import com.dpiengine.io.PcapReader;
import com.dpiengine.models.AppType;
import com.dpiengine.models.DPIStats;
import com.dpiengine.models.PacketAction;
import com.dpiengine.models.PacketJob;
import com.dpiengine.parser.PacketParser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DpiEngine {

    public static class Config {
        public int numLbs = 2;
        public int fpsPerLb = 2;
    }

    private final Config config;
    private final RuleManager ruleManager;
    private final DPIStats stats;
    private final BlockingQueue<PacketJob> outputQueue;
    private final List<FastPathProcessor> fps = new ArrayList<>();
    private final List<LoadBalancer> lbs = new ArrayList<>();
    private final GlobalConnectionTable globalConnTable;

    public DpiEngine(Config config) {
        this.config = config;
        this.ruleManager = new RuleManager();
        this.stats = new DPIStats();
        this.outputQueue = new LinkedBlockingQueue<>(10000);
        
        int totalFps = config.numLbs * config.fpsPerLb;

        System.out.println("\n");
        System.out.println("==============================================================");
        System.out.println("              DPI ENGINE v2.0 (Multi-threaded)                ");
        System.out.println("==============================================================");
        System.out.printf(" Load Balancers: %2d    FPs per LB: %2d    Total FPs: %2d   \n", config.numLbs, config.fpsPerLb, totalFps);
        System.out.println("==============================================================\n");

        PacketOutputCallback outputCallback = (job, action) -> {
            if (action == PacketAction.DROP) {
                stats.droppedPackets.incrementAndGet();
            } else {
                stats.forwardedPackets.incrementAndGet();
                try {
                    outputQueue.put(job);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        for (int i = 0; i < totalFps; i++) {
            fps.add(new FastPathProcessor(i, ruleManager, outputCallback));
        }

        for (int lb = 0; lb < config.numLbs; lb++) {
            List<BlockingQueue<PacketJob>> lbFpQueues = new ArrayList<>();
            int start = lb * config.fpsPerLb;
            for (int i = 0; i < config.fpsPerLb; i++) {
                lbFpQueues.add(fps.get(start + i).getInputQueue());
            }
            lbs.add(new LoadBalancer(lb, lbFpQueues, start));
        }
        
        globalConnTable = new GlobalConnectionTable(totalFps);
        for (int i = 0; i < totalFps; i++) {
            globalConnTable.registerTracker(i, fps.get(i).getConnTracker());
        }
    }

    public void blockIp(String ip) { ruleManager.blockIp(ip); }
    public void blockApp(String app) {
        for (AppType type : AppType.values()) {
            if (type.toString().equalsIgnoreCase(app) || type.name().equalsIgnoreCase(app)) {
                ruleManager.blockApp(type);
                return;
            }
        }
        System.err.println("[DPIEngine] Unknown app: " + app);
    }
    public void blockDomain(String domain) { ruleManager.blockDomain(domain); }

    public boolean process(String inputFile, String outputFile) {
        PcapReader reader = new PcapReader();
        if (!reader.open(inputFile)) return false;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            byte[] pcapHeader = new byte[]{
                (byte) 0xd4, (byte) 0xc3, (byte) 0xb2, (byte) 0xa1,
                0x02, 0x00, 0x04, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                (byte) 0xff, (byte) 0xff, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00
            }; // basic swapped header
            fos.write(pcapHeader);
        } catch (IOException e) {
            System.err.println("Cannot open output file: " + e.getMessage());
            return false;
        }

        for (FastPathProcessor fp : fps) fp.start();
        for (LoadBalancer lb : lbs) lb.start();

        final FileOutputStream finalFos = fos;
        Thread outputThread = new Thread(() -> {
            ByteBuffer headerBuf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            PacketJob job;
            while (true) {
                try {
                    job = outputQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (job != null) {
                        try {
                            headerBuf.clear();
                            headerBuf.putInt((int)job.tsSec);
                            headerBuf.putInt((int)job.tsUsec);
                            headerBuf.putInt(job.data.length);
                            headerBuf.putInt(job.data.length);
                            finalFos.write(headerBuf.array());
                            finalFos.write(job.data);
                        } catch (IOException e) {
                            System.err.println("Error writing to output pcap");
                        }
                    }
                } catch (InterruptedException e) {
                    while ((job = outputQueue.poll()) != null) {
                        try {
                            headerBuf.clear();
                            headerBuf.putInt((int)job.tsSec);
                            headerBuf.putInt((int)job.tsUsec);
                            headerBuf.putInt(job.data.length);
                            headerBuf.putInt(job.data.length);
                            finalFos.write(headerBuf.array());
                            finalFos.write(job.data);
                        } catch (IOException ignored) {}
                    }
                    break;
                }
            }
        });
        outputThread.start();

        System.out.println("[Reader] Processing packets...");
        int pktId = 0;
        PacketJob raw;
        while (true) {
            raw = new PacketJob();
            if (!reader.readNextPacket(raw)) break;
            
            raw.packetId = pktId++;
            if (!PacketParser.parse(raw)) continue;
            
            stats.totalPackets.incrementAndGet();
            stats.totalBytes.addAndGet(raw.data.length);
            
            if (raw.tuple.getProtocol() == PacketParser.PROTOCOL_TCP) stats.tcpPackets.incrementAndGet();
            else if (raw.tuple.getProtocol() == PacketParser.PROTOCOL_UDP) stats.udpPackets.incrementAndGet();

            int lbIdx = Math.abs(raw.tuple.hashCode()) % config.numLbs;
            try {
                lbs.get(lbIdx).getInputQueue().put(raw);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("[Reader] Done reading " + pktId + " packets");
        reader.close();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        for (LoadBalancer lb : lbs) lb.stop();
        for (FastPathProcessor fp : fps) fp.stop();

        outputThread.interrupt();
        try {
            outputThread.join();
            if (fos != null) fos.close();
        } catch (InterruptedException | IOException ignored) {}

        System.out.println(printReport());
        System.out.println(globalConnTable.generateReport());
        return true;
    }

    private String printReport() {
        StringBuilder b = new StringBuilder();
        b.append("\n==============================================================\n");
        b.append("                     PROCESSING REPORT                        \n");
        b.append("==============================================================\n");
        b.append(String.format(" Total Packets:      %12d                           \n", stats.totalPackets.get()));
        b.append(String.format(" Total Bytes:        %12d                           \n", stats.totalBytes.get()));
        b.append(String.format(" TCP Packets:        %12d                           \n", stats.tcpPackets.get()));
        b.append(String.format(" UDP Packets:        %12d                           \n", stats.udpPackets.get()));
        b.append("==============================================================\n");
        b.append(String.format(" Forwarded:          %12d                           \n", stats.forwardedPackets.get()));
        b.append(String.format(" Dropped:            %12d                           \n", stats.droppedPackets.get()));
        
        b.append("==============================================================\n");
        b.append(" THREAD STATISTICS                                             \n");
        b.append("==============================================================\n");
        for (int i = 0; i < lbs.size(); i++) {
        }
        for (int i = 0; i < fps.size(); i++) {
        }
        
        return b.toString();
    }
}
