package com.github.mikumiku.addon.util.timer;

import lombok.Generated;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class TickTimerManager {
    public static final int TICK_PRIORITY = 2147483646;
    public static final TickTimerManager INSTANCE = new TickTimerManager();
    private volatile long tickTime = 0L;

    private TickTimerManager() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.onClientTick());
    }

    private void onClientTick() {
        this.tickTime++;
    }

    @Generated
    public long getTickTime() {
        return this.tickTime;
    }
}
