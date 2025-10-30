package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.VUtil;
import com.google.common.collect.Streams;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("渲染");
    private final Setting<List<Block>> blocks = this.sgGeneral.add(new BlockListSetting.Builder().name("方块列表").description("选择的方块.").build());
    private final Setting<ListMode> blocksFilter = this.sgGeneral
        .add(
            new Builder<ListMode>()
                .name("方块过滤")
                .description("如何使用方块列表设置")
                .defaultValue(ListMode.Blacklist)
                .build()
        );
    private final Setting<Boolean> fastTower = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("快速搭塔")
                .description("是否启用更快的向上搭塔.")
                .defaultValue(false)
                .build()
        );
    private final Setting<Double> towerSpeed = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("搭塔速度")
                .description("搭塔时的速度.")
                .defaultValue(0.5)
                .min(0.0)
                .sliderMax(1.0)
                .visible(this.fastTower::get)
                .build()
        );
    private final Setting<Boolean> whileMoving = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("移动时搭塔")
                .description("允许在移动时搭塔.")
                .defaultValue(true)
                .visible(this.fastTower::get)
                .build()
        );
    private final Setting<Boolean> airPlace = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("空气放置")
                .description("允许在空气中放置方块. 这也允许你修改脚手架半径.")
                .defaultValue(false)
                .build()
        );
    private final Setting<Double> aheadDistance = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("提前距离")
                .description("提前放置方块的距离.")
                .defaultValue(0.0)
                .min(0.0)
                .sliderMax(1.0)
                .visible(() -> !(Boolean) this.airPlace.get())
                .build()
        );
    private final Setting<Double> placeRange = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("放置范围")
                .description("在空中时脚手架可以放置方块的距离.")
                .defaultValue(5.0)
                .min(0.0)
                .sliderMax(8.0)
                .visible(() -> !(Boolean) this.airPlace.get())
                .build()
        );
    private final Setting<Double> radius = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("半径")
                .description("脚手架半径.")
                .defaultValue(0.0)
                .min(0.0)
                .max(6.0)
                .visible(this.airPlace::get)
                .build()
        );
    private final Setting<Integer> blocksPerTick = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("每刻方块数")
                .description("每个tick放置的方块数量.")
                .defaultValue(2)
                .min(1)
                .visible(this.airPlace::get)
                .build()
        );
    private final Setting<Boolean> render = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("渲染")
                .description("是否渲染已放置的方块.")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShapeMode> shapeMode = this.sgRender
        .add(
            new Builder<ShapeMode>()
                .name("形状模式")
                .description("形状的渲染方式.")
                .defaultValue(ShapeMode.Both)
                .visible(this.render::get)
                .build()
        );
    private final Setting<SettingColor> sideColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("侧面颜色")
                .description("目标方块渲染的侧面颜色.")
                .defaultValue(new SettingColor(197, 137, 232, 10))
                .visible(this.render::get)
                .build()
        );
    private final Setting<SettingColor> lineColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("线条颜色")
                .description("目标方块渲染的线条颜色.")
                .defaultValue(new SettingColor(197, 137, 232))
                .visible(this.render::get)
                .build()
        );
    private final Mutable bp = new Mutable();

    public Scaffold() {
        super("自动搭路", "scaffold. 自动在你脚下放置方块搭路.");
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @EventHandler
    private void onTick(Pre event) {
        try {
            Vec3d vec = this.mc.player.getPos().add(this.mc.player.getVelocity()).add(0.0, -0.75, 0.0);
            if (this.airPlace.get()) {
                this.bp.set(this.mc.player.getBlockPos().getX(), this.mc.player.getBlockPos().getY() - 1, this.mc.player.getBlockPos().getZ());
            } else {
                Vec3d pos = this.mc.player.getPos();
                if (this.aheadDistance.get() != 0.0
                    && !this.towering()
                    && !this.mc.world.getBlockState(this.mc.player.getBlockPos().down()).getCollisionShape(this.mc.world, this.mc.player.getBlockPos()).isEmpty()) {
                    Vec3d dir = Vec3d.fromPolar(0.0F, this.mc.player.getYaw()).multiply(this.aheadDistance.get(), 0.0, this.aheadDistance.get());
                    if (this.mc.options.forwardKey.isPressed()) {
                        pos = pos.add(dir.x, 0.0, dir.z);
                    }

                    if (this.mc.options.backKey.isPressed()) {
                        pos = pos.add(-dir.x, 0.0, -dir.z);
                    }

                    if (this.mc.options.leftKey.isPressed()) {
                        pos = pos.add(dir.z, 0.0, -dir.x);
                    }

                    if (this.mc.options.rightKey.isPressed()) {
                        pos = pos.add(-dir.z, 0.0, dir.x);
                    }
                }

                this.bp.set(pos.x, vec.y, pos.z);
            }

            if (this.mc.options.sneakKey.isPressed() && !this.mc.options.jumpKey.isPressed() && this.mc.player.getY() + vec.y > -1.0) {
                this.bp.setY(this.bp.getY() - 1);
            }

            if (this.bp.getY() >= this.mc.player.getBlockPos().getY()) {
                this.bp.setY(this.mc.player.getBlockPos().getY() - 1);
            }

            BlockPos targetBlock = this.bp.toImmutable();
            if (!(Boolean) this.airPlace.get() && !BaritoneUtil.canPlace(this.bp)) {
                BlockPos playerUnderPos = this.mc.player.getBlockPos();
                if (BaritoneUtil.canPlace(playerUnderPos)) {
                    this.bp.set(playerUnderPos);
                } else if (BaritoneUtil.canPlace(playerUnderPos.down())) {
                    this.bp.set(playerUnderPos);
                } else {
                    Vec3d pos = this.mc.player.getPos();
                    pos = pos.add(0.0, -0.98F, 0.0);
                    pos.add(this.mc.player.getVelocity());
                    List<BlockPos> blockPosArray = new ArrayList<>();

                    for (int x = (int) (this.mc.player.getX() - this.placeRange.get()); x < this.mc.player.getX() + this.placeRange.get(); x++) {
                        for (int z = (int) (this.mc.player.getZ() - this.placeRange.get()); z < this.mc.player.getZ() + this.placeRange.get(); z++) {
                            for (int y = (int) this.mc.player.getY();
                                 y > Math.max(this.mc.world.getBottomY(), this.mc.player.getY() - this.placeRange.get())
                                     && y < Math.min(DV.of(VUtil.class).getTopY(this.mc), this.mc.player.getY() + this.placeRange.get());
                                 y--
                            ) {
                                this.bp.set(x, y, z);
                                if (BaritoneUtil.canPlace(this.bp)
                                    && !(this.mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(this.bp.offset(BlockUtils.getClosestPlaceSide(this.bp)))) > 36.0)) {
                                    blockPosArray.add(new BlockPos(this.bp));
                                }
                            }
                        }
                    }

                    if (blockPosArray.isEmpty()) {
                        return;
                    }

                    blockPosArray.sort(Comparator.comparingDouble(blockPos -> blockPos.getSquaredDistance(targetBlock)));
                    this.bp.set(blockPosArray.getFirst());
                }
            }

            if (!(Boolean) this.airPlace.get()) {
                this.place(this.bp);
            } else {
                BlockPos playerUnderPos = this.mc.player.getBlockPos().down();
                if (BaritoneUtil.canPlace(playerUnderPos)) {
                    this.place(playerUnderPos);
                } else {
                    List<BlockPos> blocks = new ArrayList<>();
                    blocks.add(playerUnderPos);

                    for (int x = (int) (this.bp.getX() - this.radius.get()); x <= this.bp.getX() + this.radius.get(); x++) {
                        for (int z = (int) (this.bp.getZ() - this.radius.get()); z <= this.bp.getZ() + this.radius.get(); z++) {
                            BlockPos blockPos = BlockPos.ofFloored(x, this.bp.getY(), z);
                            if (!blockPos.equals(playerUnderPos)
                                && (
                                this.mc.player.getPos().distanceTo(Vec3d.ofCenter(blockPos)) <= this.radius.get()
                                    || x == this.bp.getX() && z == this.bp.getZ()
                            )) {
                                blocks.add(blockPos);
                            }
                        }
                    }

                    if (!blocks.isEmpty()) {
                        blocks.sort(Comparator.comparingDouble(PlayerUtils::squaredDistanceTo));
                        int counter = 0;

                        for (BlockPos block : blocks) {
                            if (this.place(block)) {
                                counter++;
                            }

                            if (counter >= this.blocksPerTick.get()) {
                                break;
                            }
                        }
                    }
                }
            }

            int slot = BagUtil.findItemInventorySlot(itemStack -> this.validItem(itemStack, this.bp));
            if (this.fastTower.get() && this.mc.options.jumpKey.isPressed() && !this.mc.options.sneakKey.isPressed() && slot != -1) {
                Vec3d velocity = this.mc.player.getVelocity();
                Box playerBox = this.mc.player.getBoundingBox();
                if (Streams.stream(this.mc.world.getBlockCollisions(this.mc.player, playerBox.offset(0.0, 1.0, 0.0))).toList().isEmpty()) {
                    if (this.whileMoving.get() || !PlayerUtils.isMoving()) {
                        velocity = new Vec3d(velocity.x, this.towerSpeed.get(), velocity.z);
                    }

                    this.mc.player.setVelocity(velocity);
                } else {
                    this.mc.player.setVelocity(velocity.x, Math.ceil(this.mc.player.getY()) - this.mc.player.getY(), velocity.z);
                    this.mc.player.setOnGround(true);
                }
            }
        } catch (Exception var10) {
            this.info("Scaffolding error:" + var10);
        }
    }

    public boolean scaffolding() {
        return this.isActive();
    }

    public boolean towering() {
        int slot = BagUtil.findItemInventorySlot(itemStack -> this.validItem(itemStack, this.bp));
        return this.scaffolding()
            && this.fastTower.get()
            && this.mc.options.jumpKey.isPressed()
            && !this.mc.options.sneakKey.isPressed()
            && (this.whileMoving.get() || !PlayerUtils.isMoving())
            && slot != -1;
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) {
            return false;
        } else {
            Block block = ((BlockItem) itemStack.getItem()).getBlock();
            if (this.blocksFilter.get() == ListMode.Blacklist && this.blocks.get().contains(block)) {
                return false;
            } else if (this.blocksFilter.get() == ListMode.Whitelist && !this.blocks.get().contains(block)) {
                return false;
            } else {
                return Block.isShapeFullCube(block.getDefaultState().getCollisionShape(this.mc.world, pos)) && (!(block instanceof FallingBlock) || !FallingBlock.canFallThrough(this.mc.world.getBlockState(pos)));
            }
        }
    }

    private boolean place(BlockPos bp) {
        int slot = BagUtil.findItemInventorySlot(itemStack -> this.validItem(itemStack, bp));
        if (slot == -1) {
            return false;
        } else {
            BagUtil.doSwap(slot);
            boolean placed = BaritoneUtil.placeBlock(bp);
            BagUtil.doSwap(slot);
            if (placed) {
                if (this.render.get()) {
                    RenderUtils.renderTickingBlock(
                        bp.toImmutable(), this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0, 8, true, false
                    );
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
