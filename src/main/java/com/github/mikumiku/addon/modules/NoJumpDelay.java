package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.orbit.EventHandler;

public class NoJumpDelay extends BaseModule {
    public NoJumpDelay() {
        super("可顶头跳", "可2格高顶头跳，无延迟，非常湿滑。");
    }

    @EventHandler
    private void onUpdate(Post e) {
        DV.of(PlayerUtil.class).setJumpCooldown(mc.player, 0);
    }
}
