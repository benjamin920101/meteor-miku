package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.math.Vec3d;

public class Hover extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Double> hoverSpeed = this.sgGeneral
        .add(new Builder().name("悬停速度").description("悬停时的移动速度").defaultValue(0.1).min(0.01).sliderRange(0.01, 1.0).build());
    private final Setting<Boolean> disableOnGround = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("地面禁用")
                .description("在玩家接触地面时禁用悬停")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> autoElytra = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("自动鞘翅")
                .description("自动激活鞘翅飞行以保持悬停状态")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> pauseOnMovement = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("移动时暂停")
                .description("当玩家移动时暂停悬停")
                .defaultValue(true)
                .build()
        );

    public Hover() {
        super("悬停", "允许玩家在空中保持位置不变");
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (this.mc.player != null && this.mc.world != null) {
            if (!(Boolean) this.disableOnGround.get() || !this.mc.player.isOnGround()) {
                if (this.autoElytra.get() && !DV.of(VUtil.class).isFallFlying(this.mc) && !this.mc.player.isOnGround()) {
                    this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
                }

                if (DV.of(VUtil.class).isFallFlying(this.mc)) {
                    float forward = DV.of(PlayerUtil.class).movementForward(this.mc.player.input);
                    float sideways = DV.of(PlayerUtil.class).movementSideways(this.mc.player.input);
                    float yaw = this.mc.player.getYaw();
                    double cos = Math.cos(Math.toRadians(yaw + 90.0F));
                    double sin = Math.sin(Math.toRadians(yaw + 90.0F));
                    double moveX = (forward * cos + sideways * sin) * this.hoverSpeed.get();
                    double moveZ = (forward * sin - sideways * cos) * this.hoverSpeed.get();
                    double moveY = 0.0;
                    if (this.mc.options.jumpKey.isPressed()) {
                        moveY = this.hoverSpeed.get();
                    } else if (this.mc.options.sneakKey.isPressed()) {
                        moveY = -(Double) this.hoverSpeed.get();
                    }

                    DV.of(VUtil.class).setMovement((IVec3d) event.movement, moveX, moveY, moveZ);
                    Vec3d velocity = this.mc.player.getVelocity();
                    this.mc.player.setVelocity(velocity.x, velocity.y * 0.9, velocity.z);
                }

                boolean shouldPause = false;
                if (this.pauseOnMovement.get()) {
                    shouldPause = this.isPlayerMoving();
                }

                if (!shouldPause) {
                    this.performSpin();
                }
            }
        }
    }

    public boolean isPlayerMoving() {
        return Input.isPressed(this.mc.options.forwardKey)
            || Input.isPressed(this.mc.options.backKey)
            || Input.isPressed(this.mc.options.leftKey)
            || Input.isPressed(this.mc.options.rightKey)
            || Input.isPressed(this.mc.options.jumpKey)
            || Input.isPressed(this.mc.options.sneakKey);
    }

    private void performSpin() {
        float currentYaw = this.mc.player.getYaw();
        this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).get(currentYaw, this.mc.player.getPitch(), this.mc.player.isOnGround()));
    }
}
