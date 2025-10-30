package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;

import java.util.Random;

public class Criticals extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Mode> mode = this.sgGeneral
        .add(new Builder<Mode>().name("模式").description("暴击攻击的触发方式").defaultValue(Mode.GrimV3).build());
    private final Setting<Boolean> multitask = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("多任务")
                .description("其他模块启用时也能触发暴击")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> phasedOnly = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("仅卡墙时")
                .description("仅在相位状态下尝试暴击")
                .defaultValue(false)
                .visible(() -> this.mode.get() == Mode.Grim || this.mode.get() == Mode.GrimV3)
                .build()
        );
    private final Setting<Boolean> wallsOnly = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("仅墙内")
                .description("只在完全卡在墙里时触发暴击")
                .defaultValue(false)
                .visible(() -> (this.mode.get() == Mode.Grim || this.mode.get() == Mode.GrimV3) && this.phasedOnly.get())
                .build()
        );
    private final Setting<Boolean> moveFix = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("移动时暂停")
                .description("移动时不触发暴击")
                .defaultValue(false)
                .visible(() -> this.mode.get() == Mode.Grim || this.mode.get() == Mode.GrimV3)
                .build()
        );
    private final Setting<Boolean> pauseOnCA = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("水晶时暂停")
                .description("使用水晶光环时不触发暴击")
                .defaultValue(true)
                .build()
        );
    private final Random random = new Random();
    private long lastAttackTime = 0L;
    private boolean postUpdateGround = false;
    private boolean postUpdateSprint = false;

    public Criticals() {
        super(MikuMikuAddon.CATEGORY_MIKU_COMBAT, "刀暴击+", "让每次攻击都打出暴击伤害");
    }

    public String getInfoString() {
        return this.mode.get().name();
    }

    @EventHandler
    private void onAttackEntity(AttackEntityEvent event) {
        if (this.pauseOnCA.get()) {
            CrystalAura ca = Modules.get().get(CrystalAura.class);
            if (ca != null && ca.isActive()) {
                return;
            }
        }

        if (this.multitask.get() || !Modules.get().isActive(Surround.class) && !Modules.get().isActive(SelfTrapPlusPlus.class)) {
            Entity target = event.entity;
            if (target != null && target.isAlive() && target instanceof LivingEntity) {
                if (!this.mc.player.isRiding()
                    && !DV.of(VUtil.class).isFallFlying(this.mc)
                    && !this.mc.player.isTouchingWater()
                    && !this.mc.player.isInLava()
                    && !this.mc.player.isClimbing()
                    && !this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
                    this.postUpdateSprint = this.mc.player.isSprinting();
                    if (this.postUpdateSprint) {
                        this.mc
                            .player
                            .networkHandler
                            .sendPacket(new ClientCommandC2SPacket(this.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    }

                    this.doCritical(target);
                }
            }
        }
    }

    @EventHandler
    private void onPacketSent(Sent event) {
        if (this.mc.player != null) {
            if (event.packet instanceof PlayerInteractEntityC2SPacket) {
                if (this.postUpdateGround) {
                    this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).getPositionAndOnGround(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), false));
                    this.postUpdateGround = false;
                }

                if (this.postUpdateSprint) {
                    this.mc
                        .player
                        .networkHandler
                        .sendPacket(new ClientCommandC2SPacket(this.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    this.postUpdateSprint = false;
                }
            }
        }
    }

    private void doCritical(Entity target) {
        if (this.mc.player.isOnGround() && !DV.of(VUtil.class).isJumping(this.mc)) {
            double x = this.mc.player.getX();
            double y = this.mc.player.getY();
            double z = this.mc.player.getZ();
            float yaw = this.mc.player.getYaw();
            float pitch = this.mc.player.getPitch();
            switch (this.mode.get()) {
                case Vanilla:
                    double d = 1.0E-7 + 1.0E-7 * (1.0 + this.random.nextInt(this.random.nextBoolean() ? 34 : 43));
                    this.sendPosition(x, y + 0.1016 + d * 3.0, z, false);
                    this.sendPosition(x, y + 0.0202 + d * 2.0, z, false);
                    this.sendPosition(x, y + 3.239E-4 + d, z, false);
                    this.mc.player.addCritParticles(target);
                    break;
                case Packet:
                    this.sendPosition(x, y + 0.0625, z, false);
                    this.sendPosition(x, y, z, false);
                    this.mc.player.addCritParticles(target);
                    break;
                case PacketStrict:
                    if (System.currentTimeMillis() - this.lastAttackTime >= 500L) {
                        this.sendPosition(x, y + 1.1E-7, z, false);
                        this.sendPosition(x, y + 1.0E-8, z, false);
                        this.postUpdateGround = true;
                        this.lastAttackTime = System.currentTimeMillis();
                    }
                    break;
                case Grim:
                    if (this.phasedOnly.get() && (this.wallsOnly.get() ? !this.isDoublePhased() : !this.isPhased())) {
                        return;
                    }

                    if (this.moveFix.get() && PlayerUtils.isMoving()) {
                        return;
                    }

                    if (System.currentTimeMillis() - this.lastAttackTime >= 250L && !this.mc.player.isCrawling()) {
                        this.sendPositionFull(x, y + 0.0625, z, yaw, pitch, false);
                        this.sendPositionFull(x, y + 0.0625013579, z, yaw, pitch, false);
                        this.sendPositionFull(x, y + 1.3579E-6, z, yaw, pitch, false);
                        this.lastAttackTime = System.currentTimeMillis();
                    }
                    break;
                case GrimV3:
                    if (this.phasedOnly.get() && (this.wallsOnly.get() ? !this.isDoublePhased() : !this.isPhased())) {
                        return;
                    }

                    if (this.moveFix.get() && PlayerUtils.isMoving()) {
                        return;
                    }

                    if (!this.mc.player.isCrawling()) {
                        this.sendPositionFull(x, y, z, yaw, pitch, true);
                        this.sendPositionFull(x, y + 0.0625, z, yaw, pitch, false);
                        this.sendPositionFull(x, y + 0.04535, z, yaw, pitch, false);
                    }
                    break;
                case LowHop:
                    this.mc.player.setVelocity(this.mc.player.getVelocity().x, 0.3425, this.mc.player.getVelocity().z);
            }
        }
    }

    private void sendPosition(double x, double y, double z, boolean onGround) {
        this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).getPositionAndOnGround(x, y, z, onGround));
    }

    private void sendPositionFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).getFull(x, y, z, yaw, pitch, onGround));
    }

    private boolean isPhased() {
        Box box = this.mc.player.getBoundingBox();
        Mutable blockPos = new Mutable();

        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    blockPos.set(x, y, z);
                    if (!this.mc.world.getBlockState(blockPos).getCollisionShape(this.mc.world, blockPos).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isDoublePhased() {
        Box box = this.mc.player.getBoundingBox();
        Mutable blockPos = new Mutable();

        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    blockPos.set(x, y, z);
                    blockPos.set(x, y + 1, z);
                    if (!this.mc.world.getBlockState(blockPos).getCollisionShape(this.mc.world, blockPos).isEmpty()
                        && !this.mc.world.getBlockState(blockPos.up()).getCollisionShape(this.mc.world, blockPos.up()).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public enum Mode {
        Vanilla,
        Packet,
        PacketStrict,
        Grim,
        GrimV3,
        LowHop
    }
}
