package com.dpiengine.core;

import com.dpiengine.models.PacketJob;
import com.dpiengine.models.FiveTuple;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LoadBalancer implements Runnable {
    private final int lbId;
    private final int fpStartId;
    private final int numFps;
    private final BlockingQueue<PacketJob> inputQueue;
    private final List<BlockingQueue<PacketJob>> fpQueues;
    
    private volatile boolean running = false;
    private Thread thread;
    
    private final AtomicLong packetsReceived = new AtomicLong(0);
    private final AtomicLong packetsDispatched = new AtomicLong(0);

    public LoadBalancer(int lbId, List<BlockingQueue<PacketJob>> fpQueues, int fpStartId) {
        this.lbId = lbId;
        this.fpStartId = fpStartId;
        this.numFps = fpQueues.size();
        this.fpQueues = fpQueues;
        this.inputQueue = new LinkedBlockingQueue<>(10000);
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "LB-" + lbId);
        thread.start();
        System.out.println("[LB" + lbId + "] Started (serving FP" + fpStartId + "-FP" + (fpStartId + numFps - 1) + ")");
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
        System.out.println("[LB" + lbId + "] Stopped");
    }

    @Override
    public void run() {
        while (running) {
            try {
                PacketJob job = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (job == null) continue;
                
                packetsReceived.incrementAndGet();
                
                int fpIndex = selectFP(job.tuple);
                
                // Block if internal queue full
                fpQueues.get(fpIndex).put(job);
                
                packetsDispatched.incrementAndGet();
            } catch (InterruptedException e) {
                if (!running) break;
            }
        }
    }

    public BlockingQueue<PacketJob> getInputQueue() {
        return inputQueue;
    }

    private int selectFP(FiveTuple tuple) {
        if (numFps == 0) return 0;
        int hash = tuple.hashCode();
        return Math.abs(hash % numFps);
    }
}
