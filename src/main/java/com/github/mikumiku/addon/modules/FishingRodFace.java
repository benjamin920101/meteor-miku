package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.ChatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class FishingRodFace extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgTarget = this.settings.createGroup("目标设置");
    private final Setting<Integer> detectionRange = this.sgGeneral
        .add(
            new Builder().name("检测范围").description("检测敌人的范围（方块）").defaultValue(6)
                .min(1)
                .max(256)
                .sliderMin(1)
                .sliderMax(128)
                .build()
        );
    private final Setting<Integer> delay = this.sgGeneral
        .add(
            new Builder().name("丢钓延迟").description("丢钓鱼竿的延迟（tick）").defaultValue(1)
                .min(0)
                .max(20)
                .sliderMin(0)
                .sliderMax(20)
                .build()
        );
    private final Setting<Boolean> autoRecast = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("自动重新丢钓")
                .description("钓到人后自动重新丢钓")
                .defaultValue(true)
                .build()
        );
    private final Setting<Integer> recastDelay = this.sgGeneral
        .add(
            new Builder().name("重新丢钓延迟").description("钓到人后重新丢钓的延迟（tick）").defaultValue(10)
                .min(0)
                .max(40)
                .sliderMin(0)
                .sliderMax(40)
                .build()
        );
    private final Setting<Boolean> targetPlayers = this.sgTarget
        .add(
            new BoolSetting.Builder()
                .name("糊弄玩家")
                .description("是否针对玩家")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> targetTeam = this.sgTarget
        .add(
            new BoolSetting.Builder()
                .name("糊弄队友")
                .description("是否针对队友")
                .defaultValue(false)
                .build()
        );
    private final Setting<Double> headOffsetY = this.sgTarget
        .add(
            new DoubleSetting.Builder()
                .name("头部高度偏移")
                .description("针对头部的高度偏移")
                .defaultValue(1.6)
                .min(0.0)
                .max(3.0)
                .sliderMin(0.0)
                .sliderMax(3.0)
                .build()
        );
    private int tickTimer = 0;
    private int recastTimer = 0;
    private boolean isFishingRodThrown = false;
    private PlayerEntity lastTargetedPlayer = null;

    public FishingRodFace() {
        super(MikuMikuAddon.CATEGORY_MIKU_COMBAT, "鱼竿糊脸", "使用鱼竿丢敌人脸上");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        this.tickTimer = 0;
        this.recastTimer = 0;
        this.isFishingRodThrown = false;
        this.lastTargetedPlayer = null;
        ChatUtils.sendMsg("鱼竿糊脸模块已启动");
        ChatUtils.sendMsg("正在搜索范围内的目标...");
    }

    public void onDeactivate() {
        this.tickTimer = 0;
        this.recastTimer = 0;
        this.isFishingRodThrown = false;
        this.lastTargetedPlayer = null;
        ChatUtils.sendMsg("鱼竿糊脸模块已停止");
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player != null && this.mc.world != null) {
            if (!this.hasFishingRodInInventory()) {
                this.warning("背包中没有鱼竿！");
            } else if (this.recastTimer > 0) {
                this.recastTimer--;
            } else if (this.tickTimer > 0) {
                this.tickTimer--;
            } else {
                PlayerEntity target = this.findNearestTarget();
                if (target != null) {
                    if (this.lastTargetedPlayer == null || !this.lastTargetedPlayer.equals(target)) {
                        this.lastTargetedPlayer = target;
                        this.info("选中目标: " + target.getName().getString());
                    }

                    Vec3d targetHeadPos = this.getPlayerHeadPosition(target);
                    this.castFishingRod(targetHeadPos);
                    this.tickTimer = this.delay.get();
                    if (this.autoRecast.get()) {
                        this.recastTimer = this.recastDelay.get();
                    }
                } else if (this.lastTargetedPlayer != null) {
                    this.lastTargetedPlayer = null;
                }
            }
        }
    }

    private PlayerEntity findNearestTarget() {
        PlayerEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (!player.equals(this.mc.player) && this.isValidTarget(player)) {
                double distance = this.mc.player.distanceTo(player);
                if (distance <= this.detectionRange.get().intValue() && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = player;
                }
            }
        }

        return nearestTarget;
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player.isCreative()) {
            return false;
        } else if (player.isDead()) {
            return false;
        } else {
            return this.isFriendly(player) ? this.targetTeam.get() : this.targetPlayers.get();
        }
    }

    private boolean isFriendly(PlayerEntity player) {
        return Friends.get().isFriend(player);
    }

    private Vec3d getPlayerHeadPosition(PlayerEntity player) {
        Vec3d playerPos = player.getPos();
        return playerPos.add(0.0, this.headOffsetY.get(), 0.0);
    }

    private void castFishingRod(Vec3d targetPos) {
        this.equipFishingRod();
        Vec3d playerEyePos = this.mc.player.getEyePos();
        Vec3d direction = targetPos.subtract(playerEyePos).normalize();
        float[] rotation = this.calculateRotation(playerEyePos, targetPos);
        this.mc.player.setYaw(rotation[0]);
        this.mc.player.setPitch(rotation[1]);
        this.mc.interactionManager.attackBlock(this.mc.player.getBlockPos(), Direction.UP);
        this.mc.player.swingHand(Hand.MAIN_HAND);
        this.info("丢钓鱼竿向目标: " + String.format("(%.1f, %.1f, %.1f)", targetPos.x, targetPos.y, targetPos.z));
    }

    private float[] calculateRotation(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, distance)));
        return new float[]{yaw, pitch};
    }

    private void equipFishingRod() {
        int slot = BagUtil.findItemInventorySlot(itemStack -> itemStack.getItem() instanceof FishingRodItem);
        BagUtil.doSwap(slot);
    }

    private boolean hasFishingRodInInventory() {
        for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FishingRodItem) {
                return true;
            }
        }

        return false;
    }

    public String getStatus() {
        if (this.lastTargetedPlayer != null) {
            double distance = this.mc.player.distanceTo(this.lastTargetedPlayer);
            return "目标: " + this.lastTargetedPlayer.getName().getString() + " | 距离: " + String.format("%.1f", distance);
        } else {
            return "搜索中...";
        }
    }
}
