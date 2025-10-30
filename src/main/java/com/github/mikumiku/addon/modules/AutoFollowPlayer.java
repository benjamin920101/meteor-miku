package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.util.BagUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

public class AutoFollowPlayer extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("渲染");
    private final List<Entity> targets = new ArrayList<>();
    private final Setting<SortPriority> priority = this.sgGeneral
        .add(new Builder<SortPriority>().name("priority").description("范围内多个敌人的选择优先级").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Double> range = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("range")
                .description("检测范围")
                .defaultValue(50.0)
                .range(0.0, 192.0)
                .build()
        );
    private final Setting<Boolean> onlyAir = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("only-air")
                .description("仅限空中")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> preventGround = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("prevent-ground")
                .description("防落地")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> render = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("render")
                .description("是否渲染")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShapeMode> shapeMode = this.sgRender
        .add(new Builder<ShapeMode>().name("shape-mode")
            .description("渲染模式")
            .defaultValue(ShapeMode.Both)
            .visible(this.render::get)
            .build()
        );
    private final Setting<SettingColor> sideColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("side-color")
                .description("边颜色")
                .defaultValue(new SettingColor(160, 0, 225, 35))
                .visible(() -> this.shapeMode.get().sides())
                .build()
        );
    private final Setting<SettingColor> lineColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("line-color")
                .description("轮廓颜色")
                .defaultValue(new SettingColor(255, 255, 255, 50))
                .visible(() -> this.render.get() && this.shapeMode.get().lines())
                .build()
        );

    public AutoFollowPlayer() {
        super(MikuMikuAddon.CATEGORY_MIKU_COMBAT, "鞘翅追人", "鞘翅追人");
    }

    @Override
    public void onActivate() {
        this.targets.clear();
        TargetUtils.getList(
            this.targets,
            entity -> {
                if (entity instanceof PlayerEntity player) {
                    if (Friends.get().isFriend(player)) {
                        return false;
                    } else if (entity == this.mc.player) {
                        return false;
                    } else {
                        Box hitbox = entity.getBoundingBox();
                        return PlayerUtils.isWithin(
                            MathHelper.clamp(this.mc.player.getX(), hitbox.minX, hitbox.maxX),
                            MathHelper.clamp(this.mc.player.getY(), hitbox.minY, hitbox.maxY),
                            MathHelper.clamp(this.mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
                            this.range.get()
                        );
                    }
                } else {
                    return false;
                }
            },
            this.priority.get(),
            1
        );
        BagUtil.quickUse(Items.FIREWORK_ROCKET);
    }

    public void onDeactivate() {
        this.targets.clear();
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (this.mc.player.isAlive() && PlayerUtils.getGameMode() != GameMode.SPECTATOR) {
            if (!this.targets.isEmpty() && (!(Boolean) this.onlyAir.get() || !this.mc.player.isOnGround())) {
                Entity primary = this.targets.getFirst();
                if (!(Boolean) this.preventGround.get() || !primary.isOnGround()) {
                    MeteorClient.mc.player.setYaw((float) Rotations.getYaw(primary));
                }

                MeteorClient.mc.player.setPitch(primary.isOnGround() && this.preventGround.get() ? -90.0F : (float) Rotations.getPitch(primary, Target.Body));
            }

            try {
                Entity lastAttackedEntity = this.targets.getFirst();
                if (this.targets.getFirst() != null) {
                    double x = MathHelper.lerp(event.tickDelta, lastAttackedEntity.lastRenderX, lastAttackedEntity.getX()) - lastAttackedEntity.getX();
                    double y = MathHelper.lerp(event.tickDelta, lastAttackedEntity.lastRenderY, lastAttackedEntity.getY()) - lastAttackedEntity.getY();
                    double z = MathHelper.lerp(event.tickDelta, lastAttackedEntity.lastRenderZ, lastAttackedEntity.getZ()) - lastAttackedEntity.getZ();
                    Box box = lastAttackedEntity.getBoundingBox();
                    event.renderer
                        .box(
                            x + box.minX,
                            y + box.minY,
                            z + box.minZ,
                            x + box.maxX,
                            y + box.maxY,
                            z + box.maxZ,
                            this.sideColor.get(),
                            this.lineColor.get(),
                            this.shapeMode.get(),
                            0
                        );
                }
            } catch (Exception ignored) {
            }
        }
    }

    public String getInfoString() {
        return !this.targets.isEmpty() ? EntityUtils.getName(this.targets.getFirst()) : null;
    }
}
