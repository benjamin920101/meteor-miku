package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import lombok.Getter;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;

import java.util.ArrayList;
import java.util.List;

public class AutoSlab extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("渲染");
    private final Setting<BlockType> blockType = this.sgGeneral
        .add(new Builder<BlockType>().name("方块类型").description("要放置的方块类型").defaultValue(BlockType.SLAB).build());
    private final Setting<Integer> placementDelay = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("延迟")
                .description("放置之间的延迟(刻)")
                .defaultValue(1)
                .build()
        );
    private final Setting<Integer> blockPerTick = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("每刻放置数量")
                .description("每刻放置多少方块")
                .defaultValue(2)
                .sliderRange(1, 10)
                .build()
        );
    private final Setting<Double> detectionRange = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("范围")
                .description("方块放置范围")
                .defaultValue(4.5)
                .range(1.0, 6.0)
                .build()
        );
    private final Setting<Boolean> renderEnabled = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("渲染")
                .description("渲染方块位置")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShapeMode> renderMode = this.sgRender
        .add(new Builder<ShapeMode>().name("形状模式").description("如何渲染方块").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("侧面颜色")
                .description("渲染方块侧面的颜色")
                .defaultValue(new SettingColor(100, 200, 255, 45))
                .build()
        );
    private final Setting<SettingColor> lineColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("线条颜色")
                .description("渲染方块线条的颜色")
                .defaultValue(new SettingColor(120, 220, 255, 180))
                .build()
        );
    private final List<BlockPos> placePositions = new ArrayList<>();
    private int currentDelay;

    public AutoSlab() {
        super("铺半砖+", "自动放置半砖/活板门/铁轨/按钮/地毯, 只会铺一层， 用来防刷怪");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        this.placePositions.clear();
        this.currentDelay = 0;
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.currentDelay < this.placementDelay.get()) {
            this.currentDelay++;
        } else {
            this.currentDelay = 0;
            this.scanPlacementPositions();
            if (!this.placePositions.isEmpty()) {
                int slot = this.getBlockSlot();
                if (slot != -1) {
                    int placementCount = Math.min(this.blockPerTick.get(), this.placePositions.size());

                    for (int i = 0; i < placementCount; i++) {
                        BlockPos pos = this.placePositions.get(i);
                        BagUtil.doSwap(slot);
                        if (!this.isExistingPlacementBlock(this.mc.world.getBlockState(pos).getBlock())) {
                            BaritoneUtil.placeBlock(pos, true, true, true);
                        }

                        BagUtil.doSwap(slot);
                        BagUtil.sync();
                    }
                }
            }
        }
    }

    private void scanPlacementPositions() {
        this.placePositions.clear();
        float range = this.detectionRange.get().floatValue();

        for (BlockPos pos : WorldUtils.getSphere(range)) {
            if (this.canPlaceAt(pos)) {
                this.placePositions.add(pos);
            }
        }
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockState downState = this.mc.world.getBlockState(pos.down());
        BlockState currentState = this.mc.world.getBlockState(pos);
        BlockState upState = this.mc.world.getBlockState(pos.up());
        Block downBlock = downState.getBlock();
        Block currentBlock = currentState.getBlock();
        boolean isOnFullCube = downState.isFullCube(this.mc.world, pos.down())
            && !BaritoneUtil.SNEAK_BLOCKS.contains(downBlock)
            && upState.isAir()
            && currentBlock != Blocks.WATER
            && currentBlock != Blocks.LAVA
            && !this.isExistingPlacementBlock(currentBlock)
            && BaritoneUtil.canPlace(pos, true)
            && BlockUtils.canPlace(pos);
        double maxHeight = downState.getCollisionShape(this.mc.world, pos.down()).getMax(Axis.Y);
        boolean isOnHalfBlock = maxHeight > 0.8 && maxHeight < 1.0 && BlockUtils.canPlace(pos) && !BaritoneUtil.SNEAK_BLOCKS.contains(downBlock);
        return isOnFullCube || isOnHalfBlock;
    }

    private int getBlockSlot() {
        return BagUtil.findClassInventorySlotGrim((this.blockType.get()).getBlockClass());
    }

    private boolean isExistingPlacementBlock(Block block) {
        return block instanceof SlabBlock || block instanceof TrapdoorBlock || block instanceof AbstractRailBlock || block instanceof ButtonBlock;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.renderEnabled.get() && !this.placePositions.isEmpty() && this.getBlockSlot() != -1) {
            for (BlockPos pos : this.placePositions) {
                double x1 = pos.getX();
                double y1 = pos.getY();
                double z1 = pos.getZ();
                double x2 = x1 + 1.0;
                double y2 = y1 + 1.0;
                double z2 = z1 + 1.0;
                event.renderer.box(x1, y1, z1, x2, y2, z2, this.sideColor.get(), this.lineColor.get(), this.renderMode.get(), 0);
            }
        }
    }

    @Getter
    public enum BlockType {
        SLAB(SlabBlock.class, "半砖"),
        TRAPDOOR(TrapdoorBlock.class, "活板门"),
        BUTTON(ButtonBlock.class, "按钮"),
        RAIL(AbstractRailBlock.class, "铁轨"),
        CARPET(CarpetBlock.class, "地毯"),
        Leave(LeavesBlock.class, "树叶"),
        FENCE(FenceBlock.class, "栅栏"),
        FENCE_GATE(FenceGateBlock.class, "栅栏门"),
        DOOR(DoorBlock.class, "门"),
        PRESSURE_PLATE(PressurePlateBlock.class, "压力板"),
        TORCH(TorchBlock.class, "火把"),
        LANTERN(LanternBlock.class, "灯笼"),
        GLASS(StainedGlassBlock.class, "染色玻璃"),
        REDSTONE_WIRE(RedstoneWireBlock.class, "红石线"),
        REDSTONE_TORCH(RedstoneTorchBlock.class, "红石火把"),
        REPEATER(RepeaterBlock.class, "红石中继器"),
        COMPARATOR(ComparatorBlock.class, "红石比较器"),
        SNOW_LAYER(SnowBlock.class, "雪层"),
        FLOWER(FlowerBlock.class, "花"),
        SAPLING(SaplingBlock.class, "树苗"),
        COBWEB(CobwebBlock.class, "蜘蛛网"),
        NOTE_BLOCK(NoteBlock.class, "音符盒"),
        DAYLIGHT_SENSOR(DaylightDetectorBlock.class, "光感器"),
        SOUL_SAND(SoulSandBlock.class, "灵魂沙"),
        CAMPFIRE(CampfireBlock.class, "营火"),
        FLOWER_POT(FlowerPotBlock.class, "花盆"),
        CHAIN(ChainBlock.class, "锁链"),
        VINE(VineBlock.class, "藤蔓"),
        BAMBOO(BambooBlock.class, "竹子"),
        ICE(IceBlock.class, "冰"),
        STAIRS(StairsBlock.class, "楼梯");

        private final Class<?> blockClass;
        private final String displayName;

        BlockType(Class<?> blockClass, String displayName) {
            this.blockClass = blockClass;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
