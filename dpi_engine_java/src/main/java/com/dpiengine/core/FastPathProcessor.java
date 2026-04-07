package com.dpiengine.core;

import com.dpiengine.models.AppType;
import com.dpiengine.models.Connection;
import com.dpiengine.models.ConnectionState;
import com.dpiengine.models.PacketAction;
import com.dpiengine.models.PacketJob;
import com.dpiengine.parser.SNIExtractor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FastPathProcessor implements Runnable {
    private final int fpId;
    private final BlockingQueue<PacketJob> inputQueue;
    private final ConnectionTracker connTracker;
    private final RuleManager ruleManager;
    private final PacketOutputCallback outputCallback;
    
    private volatile boolean running = false;
    private Thread thread;
    
    private final AtomicLong packetsProcessed = new AtomicLong(0);
    private final AtomicLong packetsForwarded = new AtomicLong(0);
    private final AtomicLong packetsDropped = new AtomicLong(0);

    public FastPathProcessor(int fpId, RuleManager ruleManager, PacketOutputCallback outputCallback) {
        this.fpId = fpId;
        this.ruleManager = ruleManager;
        this.outputCallback = outputCallback;
        this.inputQueue = new LinkedBlockingQueue<>(10000);
        this.connTracker = new ConnectionTracker(fpId, 50000);
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "FP-" + fpId);
        thread.start();
        System.out.println("[FP" + fpId + "] Started");
    }

    public void stop() {
        if (!running) return;
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[FP" + fpId + "] Stopped (processed " + packetsProcessed.get() + " packets)");
    }

    public BlockingQueue<PacketJob> getInputQueue() {
        return inputQueue;
    }

    public ConnectionTracker getConnTracker() {
        return connTracker;
    }

    @Override
    public void run() {
        long lastCleanup = System.currentTimeMillis();
        
        while (running) {
            try {
                PacketJob job = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (job == null) {
                    if (System.currentTimeMillis() - lastCleanup > 300000) { // 5 minutes
                        connTracker.cleanupStale(300000);
                        lastCleanup = System.currentTimeMillis();
                    }
                    continue;
                }
                
                packetsProcessed.incrementAndGet();
                
                PacketAction action = processPacket(job);
                
                if (outputCallback != null) {
                    outputCallback.onPacketProcessed(job, action);
                }
                
                if (action == PacketAction.DROP) {
                    packetsDropped.incrementAndGet();
                } else {
                    packetsForwarded.incrementAndGet();
                }
                
            } catch (InterruptedException e) {
                if (!running) break;
            }
        }
    }

    private PacketAction processPacket(PacketJob job) {
        Connection conn = connTracker.getOrCreateConnection(job.tuple);
        if (conn == null) return PacketAction.FORWARD;
        
        connTracker.updateConnection(conn, job.data.length, true);
        
        if (job.tuple.getProtocol() == 6) { // TCP
            updateTCPState(conn, job.tcpFlags);
        }
        
        if (conn.state == ConnectionState.BLOCKED) {
            return PacketAction.DROP;
        }
        
        if (conn.state != ConnectionState.CLASSIFIED && job.payloadLength > 0) {
            inspectPayload(job, conn);
        }
        
        return checkRules(job, conn);
    }

    private void inspectPayload(PacketJob job, Connection conn) {
        if (job.payloadLength == 0 || job.payloadOffset >= job.data.length) return;
        
        if (job.tuple.getDstPort() == 443 || job.payloadLength >= 50) {
            String sni = SNIExtractor.extract(job.data, job.payloadOffset, job.payloadLength);
            if (sni != null) {
                AppType app = AppType.fromSni(sni);
                connTracker.classifyConnection(conn, app, sni);
                return;
            }
        }
        
        if (job.tuple.getDstPort() == 80) {
            String host = SNIExtractor.extractHTTPHost(job.data, job.payloadOffset, job.payloadLength);
            if (host != null) {
                AppType app = AppType.fromSni(host);
                connTracker.classifyConnection(conn, app, host);
                return;
            }
        }
        
        if (job.tuple.getDstPort() == 80) {
            connTracker.classifyConnection(conn, AppType.HTTP, "");
        } else if (job.tuple.getDstPort() == 443) {
            connTracker.classifyConnection(conn, AppType.HTTPS, "");
        }
    }

    private PacketAction checkRules(PacketJob job, Connection conn) {
        if (ruleManager == null) return PacketAction.FORWARD;
        
        RuleManager.BlockReason reason = ruleManager.shouldBlock(job.tuple.getSrcIp(), job.tuple.getDstPort(), conn.appType, conn.sni);
        
        if (reason != null) {
            System.out.println("[FP" + fpId + "] BLOCKED packet: " + reason.type + " " + reason.detail);
            connTracker.blockConnection(conn);
            return PacketAction.DROP;
        }
        
        return PacketAction.FORWARD;
    }

    private void updateTCPState(Connection conn, byte flags) {
        boolean syn = (flags & 0x02) != 0;
        boolean ack = (flags & 0x10) != 0;
        boolean fin = (flags & 0x01) != 0;
        boolean rst = (flags & 0x04) != 0;
        
        if (syn) {
            if (ack) conn.synAckSeen = true;
            else conn.synSeen = true;
        }
        
        if (conn.synSeen && conn.synAckSeen && ack) {
            if (conn.state == ConnectionState.NEW) {
                conn.state = ConnectionState.ESTABLISHED;
            }
        }
        
        if (fin) conn.finSeen = true;
        if (rst) conn.state = ConnectionState.CLOSED;
        
        if (conn.finSeen && ack) {
            conn.state = ConnectionState.CLOSED;
        }
    }
}
