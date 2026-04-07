package com.dpiengine.core;

import com.dpiengine.models.PacketAction;
import com.dpiengine.models.PacketJob;

public interface PacketOutputCallback {
    void onPacketProcessed(PacketJob job, PacketAction action);
}
