package com.github.mikumiku.addon;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MoveEvent {
    private boolean cancelled;
    public double x, y, z;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void cancel() {
        this.setCancelled(true);
    }
}
