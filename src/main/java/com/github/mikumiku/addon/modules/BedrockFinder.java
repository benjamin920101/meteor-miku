package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.HashSet;
import java.util.Set;

public class BedrockFinder extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("渲染");
    private final Setting<Integer> searchRange = this.sgGeneral
        .add(
            new IntSetting.Builder().name("搜索范围").description("搜索基岩的范围（方块）").defaultValue(128).min(16).max(256).sliderRange(16, 128).build()
        );
    private final Setting<Integer> searchSpeed = this.sgGeneral
        .add(
            new IntSetting.Builder().name("搜索速度").description("每个tick检查的方块数量").defaultValue(3600)
                .min(10)
                .max(20000)
                .sliderRange(10, 10000)
                .build()
        );
    private final Setting<Boolean> notifyOnFind = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("找到时通知")
                .description("找到3x3基岩时发送聊天消息")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> autoPause = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("找到后暂停")
                .description("找到3x3基岩后自动暂停搜索")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> renderFound = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("渲染找到的区域")
                .description("渲染找到的3x3基岩区域")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> xRayRender = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("透视渲染")
                .description("是否透视渲染（可以隔墙看到）")
                .defaultValue(true)
                .visible(this.renderFound::get)
                .build()
        );
    private final Setting<ShapeMode> shapeMode = this.sgRender
        .add(
            new EnumSetting.Builder<ShapeMode>()
                .name("形状模式")
                .description("渲染形状模式")
                .defaultValue(ShapeMode.Both)
                .visible(this.renderFound::get)
                .build()
        );
    private final Setting<SettingColor> sideColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("侧面颜色")
                .description("3x3基岩区域的侧面颜色")
                .defaultValue(new SettingColor(0, 120, 255, 50))
                .visible(this.renderFound::get)
                .build()
        );
    private final Setting<SettingColor> lineColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("线条颜色")
                .description("3x3基岩区域的线条颜色")
                .defaultValue(new SettingColor(20, 146, 230, 255))
                .visible(this.renderFound::get)
                .build()
        );
    private BlockPos centerPos;
    private int searchRadius = 0;
    private int currentAngle = 0;
    private int blocksChecked = 0;
    private int totalBlocksChecked = 0;
    private boolean searching = true;
    private final Set<BlockPos> foundBedrockAreas = new HashSet<>();
    private final Set<BlockPos> checkedPositions = new HashSet<>();

    public BedrockFinder() {
        super("杀雕机基岩寻找", "寻找适用于杀雕机的3x3基岩区域：Y=126-123之间3x3基岩+中心点上方基岩+中心点下方2格非基岩");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        this.resetSearch();
        this.info("开始搜索3x3基岩区域...");
    }

    public void onDeactivate() {
        super.onDeactivate();
        this.info("基岩搜索已停止。总共检查了 " + this.totalBlocksChecked + " 个方块，找到了 " + this.foundBedrockAreas.size() + " 个3x3基岩区域。");
    }

    private void resetSearch() {
        if (this.mc.player != null && this.mc.world != null) {
            this.centerPos = this.mc.player.getBlockPos();
            this.searchRadius = 0;
            this.currentAngle = 0;
            this.blocksChecked = 0;
            this.totalBlocksChecked = 0;
            this.searching = true;
            this.foundBedrockAreas.clear();
            this.checkedPositions.clear();
        }
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player == null || this.mc.world == null || !this.isInNether()) {
            this.error("必须在下界才能使用基岩寻找器！");
            this.toggle();
        } else if (this.searching) {
            int blocksToCheck = this.searchSpeed.get();

            for (int checkedThisTick = 0; checkedThisTick < blocksToCheck && this.searching; this.totalBlocksChecked++) {
                if (!this.checkNextPosition()) {
                    this.searching = false;
                    break;
                }

                checkedThisTick++;
                this.blocksChecked++;
            }

            if (this.blocksChecked >= this.searchRadius * this.searchRadius * 4) {
                this.searchRadius++;
                this.blocksChecked = 0;
                if (this.searchRadius > this.searchRange.get()) {
                    this.searching = false;
                    this.info("搜索完成！搜索范围已达到最大值 " + this.searchRange.get() + " 格。");
                    this.info("总共检查了 " + this.totalBlocksChecked + " 个方块，找到了 " + this.foundBedrockAreas.size() + " 个3x3基岩区域。");
                    if (this.autoPause.get() && !this.foundBedrockAreas.isEmpty()) {
                        this.searching = false;
                    }
                }
            }
        }
    }

    private boolean checkNextPosition() {
        int x = this.centerPos.getX() + (int) (this.searchRadius * Math.cos(this.currentAngle * Math.PI / 180.0));
        int z = this.centerPos.getZ() + (int) (this.searchRadius * Math.sin(this.currentAngle * Math.PI / 180.0));

        for (int y = 126; y >= 123; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            if (!this.checkedPositions.contains(checkPos)) {
                this.checkedPositions.add(checkPos);
                if (this.mc.world.getBlockState(checkPos).getBlock() == Blocks.BEDROCK && this.checkFor3x3Bedrock(checkPos)) {
                    return true;
                }
            }
        }

        this.currentAngle = this.currentAngle + 360 / Math.max(1, this.searchRadius * 8);
        if (this.currentAngle >= 360) {
            this.currentAngle = 0;
        }

        return true;
    }

    private boolean checkFor3x3Bedrock(BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = center.add(x, 0, z);
                if (this.mc.world.getBlockState(checkPos).getBlock() != Blocks.BEDROCK) {
                    return false;
                }
            }
        }

        BlockPos aboveCenter = center.up();
        if (this.mc.world.getBlockState(aboveCenter).getBlock() != Blocks.BEDROCK) {
            return false;
        } else {
            BlockPos belowTwoCenter = center.down(2);
            if (this.mc.world.getBlockState(belowTwoCenter).getBlock() == Blocks.BEDROCK) {
                return false;
            } else if (!this.checkForNonBedrockArea(belowTwoCenter)) {
                return false;
            } else if (this.foundBedrockAreas.contains(center)) {
                return false;
            } else {
                this.foundBedrockAreas.add(center);
                if (this.notifyOnFind.get()) {
                    this.info("\ud83c\udf89 找到3x3基岩区域！位置: " + center.getX() + ", " + center.getY() + ", " + center.getZ());
                    this.info("距离: " + this.mc.player.getBlockPos().getManhattanDistance(center) + " 格");
                }

                if (this.autoPause.get()) {
                    this.searching = false;
                    this.info("搜索已暂停，找到了目标位置。");
                }

                return true;
            }
        }
    }

    private boolean checkForNonBedrockArea(BlockPos belowTwoCenter) {
        int[][] directions = new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        for (int[] dir : directions) {
            BlockPos directionCenter = belowTwoCenter.add(dir[0], 0, dir[1]);
            boolean hasComplete3x3NonBedrock = true;

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = directionCenter.add(x, 0, z);
                    if (this.mc.world.getBlockState(checkPos).getBlock() == Blocks.BEDROCK) {
                        hasComplete3x3NonBedrock = false;
                        break;
                    }
                }

                if (!hasComplete3x3NonBedrock) {
                    break;
                }
            }

            if (hasComplete3x3NonBedrock) {
                return true;
            }
        }

        return false;
    }

    private boolean isInNether() {
        return this.mc.world != null && PlayerUtils.getDimension().equals(Dimension.Nether);
    }

    private boolean canSeePosition(BlockPos pos) {
        if (this.mc.player != null && this.mc.world != null) {
            Vec3d eyePos = this.mc.player.getEyePos();
            Vec3d targetPos = Vec3d.ofCenter(pos);
            RaycastContext context = new RaycastContext(eyePos, targetPos, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player);
            return this.mc.world.raycast(context).getType() == Type.MISS;
        } else {
            return false;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.renderFound.get() && !this.foundBedrockAreas.isEmpty()) {
            for (BlockPos center : this.foundBedrockAreas) {
                if (this.xRayRender.get() || this.canSeePosition(center)) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos pos = center.add(x, 0, z);
                            if (this.xRayRender.get() || this.canSeePosition(pos)) {
                                double x1 = pos.getX();
                                double y1 = pos.getY();
                                double z1 = pos.getZ();
                                double x2 = pos.getX() + 1;
                                double y2 = pos.getY() + 1;
                                double z2 = pos.getZ() + 1;
                                event.renderer
                                    .box(x1, y1, z1, x2, y2, z2, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
                            }
                        }
                    }

                    BlockPos aboveCenter = center.up();
                    boolean canSeeAbove = this.xRayRender.get() || this.canSeePosition(aboveCenter);
                    if (canSeeAbove) {
                        double x1 = aboveCenter.getX();
                        double y1 = aboveCenter.getY();
                        double z1 = aboveCenter.getZ();
                        double x2 = aboveCenter.getX() + 1;
                        double y2 = aboveCenter.getY() + 1;
                        double z2 = aboveCenter.getZ() + 1;
                        event.renderer
                            .box(x1, y1, z1, x2, y2, z2, new SettingColor(255, 255, 0, 120), new SettingColor(255, 255, 0, 255), this.shapeMode.get(), 0);
                    }

                    BlockPos belowTwoCenter = center.down(2);
                    boolean canSeeBelow = this.xRayRender.get() || this.canSeePosition(belowTwoCenter);
                    if (canSeeBelow) {
                        double x1 = belowTwoCenter.getX();
                        double y1 = belowTwoCenter.getY();
                        double z1 = belowTwoCenter.getZ();
                        double x2 = belowTwoCenter.getX() + 1;
                        double y2 = belowTwoCenter.getY() + 1;
                        double z2 = belowTwoCenter.getZ() + 1;
                        event.renderer
                            .box(x1, y1, z1, x2, y2, z2, new SettingColor(0, 255, 0, 100), new SettingColor(0, 255, 0, 200), this.shapeMode.get(), 0);
                    }
                }
            }
        }
    }

    public String getSearchStatus() {
        if (!this.isActive()) {
            return "基岩寻找器: 未启用";
        } else {
            String status = this.searching ? "搜索中" : "已暂停";
            return String.format(
                "基岩寻找器: %s | 半径: %d/%d | 已检查: %d | 找到: %d",
                status,
                this.searchRadius,
                this.searchRange.get(),
                this.totalBlocksChecked,
                this.foundBedrockAreas.size()
            );
        }
    }

    public void clearFoundAreas() {
        this.foundBedrockAreas.clear();
        this.info("已清除所有找到的基岩区域缓存。");
    }

    public void restartSearch() {
        this.resetSearch();
        this.info("重新开始搜索3x3基岩区域...");
    }
}
