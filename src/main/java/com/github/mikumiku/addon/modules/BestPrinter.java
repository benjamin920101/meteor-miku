package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.SchematicContext;
import com.github.mikumiku.addon.util.WorldUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class BestPrinter extends BaseModule {
    private final SettingGroup generalSettings = this.settings.getDefaultGroup();
    private final SettingGroup slabSettings = this.settings.createGroup("特殊方块设置");
    private final SettingGroup substituteSettings = this.settings.createGroup("替补方块");
    private final SettingGroup renderSettings = this.settings.createGroup("渲染");
    private final Setting<PlaceMode> placeMode = this.generalSettings
        .add(new Builder<PlaceMode>().name("放置模式").description("决定打印方块时的放置逻辑：严格或合法模式。").defaultValue(PlaceMode.快速).build());
    private final Setting<Integer> blocksPerTick = this.generalSettings
        .add(
            new IntSetting.Builder()
                .name("每次放置数量")
                .description("每tick最多放置的方块数量。")
                .defaultValue(2)
                .sliderRange(1, 6)
                .build()
        );
    private final Setting<Integer> placeDelayTicks = this.generalSettings
        .add(
            new IntSetting.Builder()
                .name("放置延迟")
                .description("两次方块放置之间的延迟（tick）。")
                .defaultValue(0)
                .sliderRange(0, 10)
                .build()
        );
    private final Setting<Double> searchRadius = this.generalSettings
        .add(
            new DoubleSetting.Builder()
                .name("放置范围")
                .description("搜索可放置方块的半径范围。")
                .defaultValue(4.1)
                .sliderRange(1.0, 7.0)
                .build()
        );
    private final Setting<Double> autoFix = this.generalSettings
        .add(
            new DoubleSetting.Builder()
                .name("自动纠错")
                .description("记录打印位置，错误自动挖掉纠正。")
                .defaultValue(4.1)
                .sliderRange(1.0, 7.0)
                .build()
        );
    private final Setting<Boolean> redHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("红石元件处理")
                .description("是否启用特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> stairsHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("楼梯处理")
                .description("是否启用楼梯的特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> enableSlabHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("半砖处理")
                .description("是否启用半砖的特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> trapdoorHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("活板门处理")
                .description("是否启用活板门的特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> enableTorchHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("火把处理")
                .description("是否启用火把的特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> hopperHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("漏斗处理")
                .description("是否启用特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> chestHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("箱子处理")
                .description("是否启用特殊放置逻辑。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> dirtHandling = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("泥土处理")
                .description("草方块不足时，泥土也认为是草方块。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> debug = this.slabSettings
        .add(
            new BoolSetting.Builder()
                .name("debug")
                .description("debug log")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> enableSubstitute = this.substituteSettings
        .add(
            new BoolSetting.Builder()
                .name("启用替补")
                .description("当目标方块缺失时，自动使用替补方块。")
                .defaultValue(true)
                .build()
        );
    private final Setting<Block> substituteOriginal1 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("原方块-1")
                .description("需要被替换的方块类型。")
                .defaultValue(Blocks.GRASS_BLOCK)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteReplacement1 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("替补方块-1")
                .description("用于替换的方块类型。")
                .defaultValue(Blocks.DIRT)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteOriginal2 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("原方块-2")
                .description("需要被替换的方块类型。")
                .defaultValue(Blocks.MYCELIUM)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteReplacement2 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("替补方块-2")
                .description("用于替换的方块类型。")
                .defaultValue(Blocks.DIRT)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteOriginal3 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("原方块-3")
                .description("需要被替换的方块类型。")
                .defaultValue(Blocks.PODZOL)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteReplacement3 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("替补方块-3")
                .description("用于替换的方块类型。")
                .defaultValue(Blocks.DIRT)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteOriginal4 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("原方块-4")
                .description("需要被替换的方块类型。")
                .defaultValue(Blocks.STONE)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteReplacement4 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("替补方块-4")
                .description("用于替换的方块类型。")
                .defaultValue(Blocks.COBBLESTONE)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteOriginal5 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("原方块-5")
                .description("需要被替换的方块类型。")
                .defaultValue(Blocks.AIR)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Block> substituteReplacement5 = this.substituteSettings
        .add(
            new BlockSetting.Builder()
                .name("替补方块-5")
                .description("用于替换的方块类型。")
                .defaultValue(Blocks.AIR)
                .visible(this.enableSubstitute::get)
                .build()
        );
    private final Setting<Boolean> enableRender = this.renderSettings
        .add(
            new BoolSetting.Builder()
                .name("渲染预览")
                .description("是否渲染打印方块的预览框。")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShapeMode> shapeMode = this.renderSettings
        .add(new Builder<ShapeMode>().name("渲染模式").description("选择渲染方式：线框、填充或两者。").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> blockSideColor = this.renderSettings
        .add(
            new ColorSetting.Builder()
                .name("侧面颜色")
                .description("打印方块预览的侧面颜色。")
                .defaultValue(new SettingColor(0, 120, 255, 50))
                .build()
        );
    private final Setting<SettingColor> blockLineColor = this.renderSettings
        .add(
            new ColorSetting.Builder()
                .name("边框色")
                .description("打印方块预览的线条颜色。")
                .defaultValue(new SettingColor(20, 146, 230, 255))
                .build()
        );
    private final ArrayList<BlockPos> pendingBlocks = new ArrayList<>();
    private final Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
    private int currentDelay = 0;
    private final List<Block> noOppositeBlocks = Collections.singletonList(Blocks.OBSERVER);
    private final Map<BlockPos, Long> positionCache = new HashMap<>();
    private static final long CACHE_EXPIRE_TIME_MS = 1000L;

    public BestPrinter() {
        super("Miku投影打印机", "最强打印机。根据投影蓝图自动放置方块。\n自动重建投影文件（Schematic）中的建筑。\n使用前最好把Via 调1.20.6或以下");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        this.currentDelay = 0;
    }

    public void onDeactivate() {
        this.currentDelay = 0;
        this.pendingBlocks.clear();
        this.blockStateMap.clear();
        this.positionCache.clear();
    }

    @EventHandler
    public void onTick(Post event) {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (this.mc.player != null && schematicWorld != null) {
            this.cleanExpiredCache();
            if (this.currentDelay < this.placeDelayTicks.get()) {
                this.currentDelay++;
            } else {
                this.currentDelay = 0;
                this.updatePendingBlocks();
                if (!this.pendingBlocks.isEmpty()) {
                    int blocksToPlace = Math.min(this.blocksPerTick.get(), this.pendingBlocks.size());

                    for (int i = 0; i < blocksToPlace; i++) {
                        this.placeBlockAtIndex(i);
                    }
                }
            }
        }
    }

    private void placeBlockAtIndex(int index) {
        BlockPos targetPos = this.pendingBlocks.get(index);
        BlockState targetState = this.blockStateMap.get(targetPos);
        int inventorySlot = this.findBlockInInventory(targetState);
        if (inventorySlot != -1) {
            BagUtil.doSwap(inventorySlot);
            if (this.enableSlabHandling.get() && targetState.getBlock() instanceof SlabBlock) {
                this.placeSlabBlock(targetPos, targetState);
            } else if (targetState.getBlock() instanceof StairsBlock) {
                this.placeStairsBlock(targetPos, targetState);
            } else if (this.isTorchBlock(targetState.getBlock())) {
                this.placeTorchBlock(targetPos, targetState);
            } else if (this.trapdoorHandling.get() && targetState.getBlock() instanceof TrapdoorBlock) {
                this.placeTrapdoorBlock(targetPos, targetState);
            } else if (targetState.getProperties().contains(Properties.FACING)) {
                this.placeFacingBlock(targetPos, targetState, inventorySlot, Properties.FACING);
            } else if (targetState.getProperties().contains(Properties.HOPPER_FACING)) {
                this.placeHopperBlock(targetPos, targetState, inventorySlot);
            } else if (targetState.getProperties().contains(Properties.HORIZONTAL_FACING)) {
                this.placeFacingBlock(targetPos, targetState, inventorySlot, Properties.HORIZONTAL_FACING);
            } else {
                this.placeSimpleBlock(targetPos, inventorySlot);
            }

            this.addToCache(targetPos);
            BagUtil.doSwap(inventorySlot);
            BagUtil.sync();
        }
    }

    private void placeFacingBlock(BlockPos pos, BlockState state, int slot, Property<Direction> property) {
        Direction facing = state.get(property);
        if (facing != Direction.UP && facing != Direction.DOWN) {
            Direction placementDirection = this.shouldUseOriginalDirection(state.getBlock()) ? facing : facing.getOpposite();
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, placementDirection);
        }
    }

    private void placeHopperBlock(BlockPos pos, BlockState state, int slot) {
        Direction facing = state.get(Properties.HOPPER_FACING);
        if (facing != Direction.UP && facing != Direction.DOWN) {
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, facing.getOpposite());
        }
    }

    private void placeSimpleBlock(BlockPos pos, int slot) {
        BaritoneUtil.placeBlock(pos);
    }

    private void placeSlabBlock(BlockPos pos, BlockState targetState) {
        if (!targetState.getProperties().contains(Properties.SLAB_TYPE)) {
            BaritoneUtil.placeBlock(pos, true, true, true);
        } else {
            SlabType targetSlabType = targetState.get(Properties.SLAB_TYPE);
            if (targetSlabType == SlabType.TOP) {
                BaritoneUtil.placeUpBlock(pos, true, true, true);
            } else if (targetSlabType == SlabType.BOTTOM) {
                BaritoneUtil.placeDownBlock(pos, true, true, true);
            } else if (targetSlabType == SlabType.DOUBLE) {
                BaritoneUtil.placeDownBlock(pos, true, true, true);
            } else {
                BaritoneUtil.placeBlock(pos, true, true, true);
            }
        }
    }

    private void placeStairsBlock(BlockPos pos, BlockState targetState) {
        if (!targetState.getProperties().contains(Properties.HORIZONTAL_FACING)) {
            BaritoneUtil.placeBlock(pos, true, true, true);
        } else {
            Direction facing = targetState.get(Properties.HORIZONTAL_FACING);
            if (!targetState.getProperties().contains(Properties.BLOCK_HALF)) {
                BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, facing);
            } else {
                BlockHalf targetHalf = targetState.get(Properties.BLOCK_HALF);
                if (targetHalf == BlockHalf.TOP) {
                    BaritoneUtil.placeUpBlock(pos, true, true, true);
                } else {
                    BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, facing);
                }
            }
        }
    }

    private boolean isTorchBlock(Block block) {
        return block == Blocks.TORCH
            || block == Blocks.WALL_TORCH
            || block == Blocks.REDSTONE_TORCH
            || block == Blocks.REDSTONE_WALL_TORCH
            || block == Blocks.SOUL_TORCH
            || block == Blocks.SOUL_WALL_TORCH;
    }

    private void placeTorchBlock(BlockPos pos, BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        boolean isWallTorch = targetBlock == Blocks.WALL_TORCH || targetBlock == Blocks.REDSTONE_WALL_TORCH || targetBlock == Blocks.SOUL_WALL_TORCH;
        if (isWallTorch) {
            Direction facing = targetState.get(Properties.HORIZONTAL_FACING);
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, facing);
        } else {
            BlockPos below = pos.down();
            BlockState belowState = this.mc.world.getBlockState(below);
            boolean canSupport = !belowState.isAir() && Block.sideCoversSmallSquare(this.mc.world, below, Direction.UP);
            if (canSupport) {
                BaritoneUtil.placeBlock(pos, true, true, true);
            }
        }
    }

    private void placeTrapdoorBlock(BlockPos pos, BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        Direction facing = targetState.get(TrapdoorBlock.FACING);
        BlockHalf half = targetState.get(TrapdoorBlock.HALF);
        if (facing != null && half != null) {
            Direction placementDirection = facing.getOpposite();
            if (half == BlockHalf.TOP) {
                BaritoneUtil.placeUpBlockByFaceDirection(pos, true, true, true, placementDirection);
            } else {
                BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, placementDirection);
            }
        } else {
            BaritoneUtil.placeBlock(pos, true, true, true);
        }
    }

    private boolean shouldUseOriginalDirection(Block block) {
        return this.noOppositeBlocks.contains(block);
    }

    private void updatePendingBlocks() {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        List<BlockPos> spherePositions = WorldUtils.getSphere(this.searchRadius.get());
        this.pendingBlocks.clear();
        this.blockStateMap.clear();

        for (BlockPos pos : spherePositions) {
            if (this.shouldPlaceBlockAt(pos, schematicWorld)) {
                SchematicContext context = new SchematicContext(this.mc.player.getWorld(), schematicWorld, pos);
                this.pendingBlocks.add(pos);
                this.blockStateMap.put(pos, context.targetState);
            }
        }
    }

    private boolean shouldPlaceBlockAt(BlockPos pos, WorldSchematic schematicWorld) {
        SchematicContext context = new SchematicContext(this.mc.player.getWorld(), schematicWorld, pos);
        return (!this.enableSlabHandling.get() || !(context.targetState.getBlock() instanceof SlabBlock) || this.isValidSlabPlacement(pos, context)) && this.canPlaceByMode(pos)
            && this.isValidTargetBlock(context.targetState)
            && this.isBlockDifferent(context)
            && this.isWithinLayerLimit(pos)
            && this.hasBlockInInventory(context.targetState)
            && !this.pendingBlocks.contains(pos)
            && !this.isInCache(pos);
    }

    private boolean isValidSlabPlacement(BlockPos pos, SchematicContext context) {
        BlockState targetState = context.targetState;
        if (!targetState.getProperties().contains(Properties.SLAB_TYPE)) {
            return true;
        } else {
            SlabType targetSlabType = targetState.get(Properties.SLAB_TYPE);
            Block targetBlock = targetState.getBlock();
            List<Direction> directionsToCheck = new ArrayList<>();
            directionsToCheck.add(Direction.NORTH);
            directionsToCheck.add(Direction.SOUTH);
            directionsToCheck.add(Direction.EAST);
            directionsToCheck.add(Direction.WEST);
            if (targetSlabType == SlabType.TOP) {
                directionsToCheck.add(Direction.UP);
            }

            if (targetSlabType == SlabType.BOTTOM) {
                directionsToCheck.add(Direction.DOWN);
            }

            if (targetSlabType == SlabType.DOUBLE) {
                directionsToCheck.add(Direction.UP);
                directionsToCheck.add(Direction.DOWN);
            }

            for (Direction direction : directionsToCheck) {
                BlockPos neighborPos = pos.offset(direction);
                BlockState neighborState = this.mc.world.getBlockState(neighborPos);
                Block neighborBlock = neighborState.getBlock();
                if (neighborBlock instanceof SlabBlock && neighborBlock == targetBlock && neighborState.getProperties().contains(Properties.SLAB_TYPE)) {
                    SlabType neighborSlabType = neighborState.get(Properties.SLAB_TYPE);
                    if (targetSlabType == SlabType.TOP && (neighborSlabType == SlabType.TOP || neighborSlabType == SlabType.DOUBLE)) {
                        return true;
                    }

                    if (targetSlabType == SlabType.BOTTOM && (neighborSlabType == SlabType.BOTTOM || neighborSlabType == SlabType.DOUBLE)) {
                        return true;
                    }

                    if (targetSlabType == SlabType.DOUBLE) {
                        return true;
                    }
                }

                if (!(neighborBlock instanceof SlabBlock) && neighborBlock == targetBlock) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean isValidTargetBlock(BlockState targetState) {
        return targetState.getBlock() != Blocks.AIR && targetState.getBlock().asItem() != Items.AIR;
    }

    private boolean isBlockDifferent(SchematicContext context) {
        return context.targetState.getBlock() != context.currentState.getBlock();
    }

    private boolean isWithinLayerLimit(BlockPos pos) {
        return pos.getY() <= DataManager.getRenderLayerRange().getLayerMax();
    }

    private boolean hasBlockInInventory(BlockState targetState) {
        return this.findBlockInInventory(targetState) != -1;
    }

    private int findBlockInInventory(BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        Item targetItem = targetBlock.asItem();
        int slot = BagUtil.findItemInventorySlotGrim(targetItem);
        if (slot != -1) {
            return slot;
        } else {
            if (this.dirtHandling.get() && targetBlock == Blocks.GRASS_BLOCK) {
                slot = BagUtil.findItemInventorySlotGrim(Items.DIRT);
                if (slot != -1) {
                    return slot;
                }
            }

            if (this.enableSubstitute.get()) {
                Block substitute = this.findSubstituteBlock(targetBlock);
                if (substitute != null && substitute != Blocks.AIR) {
                    slot = BagUtil.findItemInventorySlotGrim(substitute.asItem());
                    return slot;
                }
            }

            return -1;
        }
    }

    private Block findSubstituteBlock(Block targetBlock) {
        if (this.substituteOriginal1.get() == targetBlock) {
            return this.substituteReplacement1.get();
        } else if (this.substituteOriginal2.get() == targetBlock) {
            return this.substituteReplacement2.get();
        } else if (this.substituteOriginal3.get() == targetBlock) {
            return this.substituteReplacement3.get();
        } else if (this.substituteOriginal4.get() == targetBlock) {
            return this.substituteReplacement4.get();
        } else {
            return this.substituteOriginal5.get() == targetBlock ? this.substituteReplacement5.get() : null;
        }
    }

    private boolean canPlaceByMode(BlockPos pos) {
        return this.placeMode.get() == PlaceMode.合法
            ? BaritoneUtil.canPlaceWithDis(pos, this.searchRadius.get(), true)
            : BaritoneUtil.getInteractDirection(pos, true) != null;
    }

    private void addToCache(BlockPos pos) {
        long currentTime = System.currentTimeMillis();
        this.positionCache.put(pos, currentTime);
    }

    private boolean isInCache(BlockPos pos) {
        if (!this.positionCache.containsKey(pos)) {
            return false;
        } else {
            long cachedTime = this.positionCache.get(pos);
            long currentTime = System.currentTimeMillis();
            if (currentTime - cachedTime >= 1000L) {
                this.positionCache.remove(pos);
                return false;
            } else {
                return true;
            }
        }
    }

    private void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        this.positionCache.entrySet().removeIf(entry -> currentTime - entry.getValue() >= 1000L);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.enableRender.get() && !this.pendingBlocks.isEmpty()) {
            for (BlockPos pos : this.pendingBlocks) {
                this.renderBlockPreview(event, pos);
            }
        }
    }

    private void renderBlockPreview(Render3DEvent event, BlockPos pos) {
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = pos.getX() + 1;
        double y2 = pos.getY() + 1;
        double z2 = pos.getZ() + 1;
        event.renderer.box(x1, y1, z1, x2, y2, z2, this.blockSideColor.get(), this.blockLineColor.get(), this.shapeMode.get(), 0);
    }

    public enum PlaceMode {
        快速,
        合法
    }
}
