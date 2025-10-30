package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = this.settings.createGroup("Whitelist");
    private final SettingGroup sgRender = this.settings.createGroup("Render");
    private final Setting<Shape> shape = this.sgGeneral
        .add(new Builder<Shape>().name("shape").description("挖掘的形状").defaultValue(Shape.Sphere).build());
    private final Setting<Mode> mode = this.sgGeneral
        .add(new Builder<Mode>().name("mode").description("挖掘的方式").defaultValue(Mode.Flatten).build());
    private final Setting<Double> range = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("range")
                .description("破坏范围")
                .defaultValue(4.0)
                .min(0.0)
                .visible(() -> this.shape.get() != Shape.Cube)
                .build()
        );
    private final Setting<Integer> range_up = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("up")
                .description("向上破坏范围")
                .defaultValue(1)
                .min(0)
                .visible(() -> this.shape.get() == Shape.Cube)
                .build()
        );
    private final Setting<Integer> range_down = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("down")
                .description("向下破坏范围")
                .defaultValue(1)
                .min(0)
                .visible(() -> this.shape.get() == Shape.Cube)
                .build()
        );
    private final Setting<Integer> range_left = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("left")
                .description("向左破坏范围")
                .defaultValue(1)
                .min(0)
                .visible(() -> this.shape.get() == Shape.Cube)
                .build()
        );
    private final Setting<Integer> range_right = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("right")
                .description("向右破坏范围")
                .defaultValue(1)
                .min(0)
                .visible(() -> this.shape.get() == Shape.Cube)
                .build()
        );
    private final Setting<Integer> range_forward = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("forward")
                .description("向前破坏范围")
                .defaultValue(1)
                .min(0)
                .visible(() -> this.shape.get() == Shape.Cube)
                .build()
        );
    private final Setting<Integer> range_back = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("back")
                .description("向后破坏范围")
                .defaultValue(1)
                .min(0)
                .visible(() -> this.shape.get() == Shape.Cube)
                .build()
        );
    private final Setting<Integer> delay = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("delay")
                .description("破坏间隔")
                .defaultValue(0)
                .build()
        );
    private final Setting<Integer> maxBlocksPerTick = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("max-blocks-per-tick")
                .description("一次破坏方块的数量")
                .defaultValue(1)
                .min(1)
                .sliderRange(1, 6)
                .build()
        );
    private final Setting<SortMode> sortMode = this.sgGeneral
        .add(new Builder<SortMode>().name("sort-mode").description("方块筛选").defaultValue(SortMode.Closest).build());
    private final Setting<Boolean> swingHand = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("swing-hand")
                .description("摆手")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> packetMine = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("packet-mine")
                .description("使用包挖")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> rotate = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("rotate")
                .description("服务器端转头")
                .defaultValue(true)
                .build()
        );
    private final Setting<ListMode> listMode = this.sgWhitelist
        .add(new Builder<ListMode>().name("list-mode").description("筛选模式").defaultValue(ListMode.Blacklist).build());
    private final Setting<List<Block>> blacklist = this.sgWhitelist
        .add(
            new BlockListSetting.Builder()
                .name("blacklist")
                .description("不破坏的方块")
                .visible(() -> this.listMode.get() == ListMode.Blacklist)
                .build()
        );
    private final Setting<List<Block>> whitelist = this.sgWhitelist
        .add(
            new BlockListSetting.Builder()
                .name("whitelist")
                .description("破坏的方块")
                .visible(() -> this.listMode.get() == ListMode.Whitelist)
                .build()
        );
    private final Setting<Boolean> enableRenderBounding = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("bounding-box")
                .description("渲染破坏范围")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShapeMode> shapeModeBox = this.sgRender
        .add(new Builder<ShapeMode>().name("nuke-box-mode").description("渲染模式").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColorBox = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("side-color")
                .description("边颜色")
                .defaultValue(new SettingColor(16, 106, 144, 100))
                .build()
        );
    private final Setting<SettingColor> lineColorBox = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("line-color")
                .description("轮廓颜色")
                .defaultValue(new SettingColor(16, 106, 144, 255))
                .build()
        );
    private final Setting<Boolean> enableRenderBreaking = this.sgRender
        .add(
            new BoolSetting.Builder()
                .name("broken-blocks")
                .description("被破坏方块渲染")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShapeMode> shapeModeBreak = this.sgRender
        .add(
            new Builder<ShapeMode>().name("nuke-block-mode").description("渲染被破坏方块").defaultValue(ShapeMode.Both)
                .visible(this.enableRenderBreaking::get)
                .build()
        );
    private final Setting<SettingColor> sideColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("side-color")
                .description("边颜色")
                .defaultValue(new SettingColor(255, 0, 0, 80))
                .visible(this.enableRenderBreaking::get)
                .build()
        );
    private final Setting<SettingColor> lineColor = this.sgRender
        .add(
            new ColorSetting.Builder()
                .name("line-color")
                .description("轮廓颜色")
                .defaultValue(new SettingColor(255, 0, 0, 255))
                .visible(this.enableRenderBreaking::get)
                .build()
        );
    private final List<BlockPos> blocks = new ArrayList<>();
    private boolean firstBlock;
    private final Mutable lastBlockPos = new Mutable();
    private int timer;
    private int noBlockTimer;
    private final Mutable pos1 = new Mutable();
    private final Mutable pos2 = new Mutable();
    int maxh = 0;
    int maxv = 0;

    public Nuker() {
        super(Categories.Player, "nuker", "自动挖周围");
    }

    @Override
    public void onActivate() {
        this.firstBlock = true;
        this.timer = 0;
        this.noBlockTimer = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.enableRenderBounding.get() && this.shape.get() != Shape.Sphere && this.mode.get() != Mode.Smash) {
            int minX = Math.min(this.pos1.getX(), this.pos2.getX());
            int minY = Math.min(this.pos1.getY(), this.pos2.getY());
            int minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
            int maxX = Math.max(this.pos1.getX(), this.pos2.getX());
            int maxY = Math.max(this.pos1.getY(), this.pos2.getY());
            int maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
            event.renderer
                .box(minX, minY, minZ, maxX, maxY, maxZ, this.sideColorBox.get(), this.lineColorBox.get(), this.shapeModeBox.get(), 0);
        }
    }

    @EventHandler
    private void onTickPre(Pre event) {
        if (this.timer > 0) {
            this.timer--;
        } else {
            double pX = this.mc.player.getX();
            double pY = this.mc.player.getY();
            double pZ = this.mc.player.getZ();
            double rangeSq = Math.pow(this.range.get(), 2.0);
            if (this.shape.get() == Shape.UniformCube) {
                this.range.set((double) Math.round(this.range.get()));
            }

            int r = (int) Math.round(this.range.get());
            if (this.shape.get() == Shape.UniformCube) {
                double pX_ = pX + 1.0;
                this.pos1.set(pX_ - r, pY - r + 1.0, pZ - r + 1.0);
                this.pos2.set(pX_ + r - 1.0, pY + r, pZ + r);
            } else {
                int direction = Math.round(this.mc.player.getRotationClient().y % 360.0F / 90.0F);
                direction = Math.floorMod(direction, 4);
                this.pos1
                    .set(
                        pX - this.range_forward.get().intValue(),
                        Math.ceil(pY) - this.range_down.get().intValue(),
                        pZ - this.range_right.get().intValue()
                    );
                this.pos2
                    .set(
                        pX + this.range_back.get().intValue() + 1.0,
                        Math.ceil(pY + this.range_up.get().intValue() + 1.0),
                        pZ + this.range_left.get().intValue() + 1.0
                    );
                switch (direction) {
                    case 0:
                        double var19 = pZ + 1.0;
                        double var18 = pX + 1.0;
                        this.pos1
                            .set(
                                var18 - (this.range_right.get() + 1),
                                Math.ceil(pY) - this.range_down.get().intValue(),
                                var19 - (this.range_back.get() + 1)
                            );
                        this.pos2
                            .set(
                                var18 + this.range_left.get().intValue(),
                                Math.ceil(pY + this.range_up.get().intValue() + 1.0),
                                var19 + this.range_forward.get().intValue()
                            );
                    case 1:
                    default:
                        break;
                    case 2:
                        double var17 = pX + 1.0;
                        double pZ_ = pZ + 1.0;
                        this.pos1
                            .set(
                                var17 - (this.range_left.get() + 1),
                                Math.ceil(pY) - this.range_down.get().intValue(),
                                pZ_ - (this.range_forward.get() + 1)
                            );
                        this.pos2
                            .set(
                                var17 + this.range_right.get().intValue(),
                                Math.ceil(pY + this.range_up.get().intValue() + 1.0),
                                pZ_ + this.range_back.get().intValue()
                            );
                        break;
                    case 3:
                        double var16 = pX + 1.0;
                        this.pos1
                            .set(
                                var16 - (this.range_back.get() + 1),
                                Math.ceil(pY) - this.range_down.get().intValue(),
                                pZ - this.range_left.get().intValue()
                            );
                        this.pos2
                            .set(
                                var16 + this.range_forward.get().intValue(),
                                Math.ceil(pY + this.range_up.get().intValue() + 1.0),
                                pZ + this.range_right.get().intValue() + 1.0
                            );
                }

                this.maxh = 1
                    + Math.max(
                    Math.max(Math.max(this.range_back.get(), this.range_right.get()), this.range_forward.get()),
                    this.range_left.get()
                );
                this.maxv = 1 + Math.max(this.range_up.get(), this.range_down.get());
            }

            if (this.mode.get() == Mode.Flatten) {
                this.pos1.setY((int) Math.floor(pY));
            }

            Box box = new Box(this.pos1.toCenterPos(), this.pos2.toCenterPos());
            BlockIterator.register(
                Math.max((int) Math.ceil(this.range.get() + 1.0), this.maxh),
                Math.max((int) Math.ceil(this.range.get()), this.maxv),
                (blockPos, blockState) -> {
                    switch (this.shape.get()) {
                        case Cube:
                            if (!box.contains(Vec3d.ofCenter(blockPos))) {
                                return;
                            }
                            break;
                        case UniformCube:
                            if (chebyshevDist(
                                this.mc.player.getBlockPos().getX(),
                                this.mc.player.getBlockPos().getY(),
                                this.mc.player.getBlockPos().getZ(),
                                blockPos.getX(),
                                blockPos.getY(),
                                blockPos.getZ()
                            )
                                >= this.range.get()) {
                                return;
                            }
                            break;
                        case Sphere:
                            if (Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq) {
                                return;
                            }
                    }

                    if (BlockUtils.canBreak(blockPos, blockState)) {
                        if (this.mode.get() != Mode.Flatten || !(blockPos.getY() < Math.floor(this.mc.player.getY()))) {
                            if (this.mode.get() != Mode.Smash || blockState.getHardness(this.mc.world, blockPos) == 0.0F) {
                                if (this.listMode.get() != ListMode.Whitelist || this.whitelist.get().contains(blockState.getBlock())) {
                                    if (this.listMode.get() != ListMode.Blacklist || !this.blacklist.get().contains(blockState.getBlock())) {
                                        this.blocks.add(blockPos.toImmutable());
                                    }
                                }
                            }
                        }
                    }
                }
            );
            BlockIterator.after(
                () -> {
                    if (this.sortMode.get() == SortMode.TopDown) {
                        this.blocks.sort(Comparator.comparingDouble(value -> -value.getY()));
                    } else if (this.sortMode.get() != SortMode.None) {
                        this.blocks
                            .sort(
                                Comparator.comparingDouble(
                                    value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5)
                                        * (this.sortMode.get() == SortMode.Closest ? 1 : -1)
                                )
                            );
                    }

                    if (this.blocks.isEmpty()) {
                        if (this.noBlockTimer++ >= this.delay.get()) {
                            this.firstBlock = true;
                        }
                    } else {
                        this.noBlockTimer = 0;
                        if (!this.firstBlock && !this.lastBlockPos.equals(this.blocks.getFirst())) {
                            this.timer = this.delay.get();
                            this.firstBlock = false;
                            this.lastBlockPos.set(this.blocks.getFirst());
                            if (this.timer > 0) {
                                return;
                            }
                        }

                        int count = 0;

                        for (BlockPos block : this.blocks) {
                            if (count >= this.maxBlocksPerTick.get()) {
                                break;
                            }

                            boolean canInstaMine = BlockUtils.canInstaBreak(block);
                            if (this.rotate.get()) {
                                Rotations.rotate(Rotations.getYaw(block), Rotations.getPitch(block), () -> this.breakBlock(block));
                            } else {
                                this.breakBlock(block);
                            }

                            if (this.enableRenderBreaking.get()) {
                                RenderUtils.renderTickingBlock(
                                    block, this.sideColor.get(), this.lineColor.get(), this.shapeModeBreak.get(), 0, 8, true, false
                                );
                            }

                            this.lastBlockPos.set(block);
                            count++;
                            if (!canInstaMine && !(Boolean) this.packetMine.get()) {
                                break;
                            }
                        }

                        this.firstBlock = false;
                        this.blocks.clear();
                    }
                }
            );
        }
    }

    private void breakBlock(BlockPos blockPos) {
        if (this.packetMine.get()) {
            this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
            this.mc.player.swingHand(Hand.MAIN_HAND);
            this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
        } else {
            BlockUtils.breakBlock(blockPos, this.swingHand.get());
        }
    }

    @EventHandler(
        priority = 200
    )
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        event.cooldown = 0;
    }

    public static int chebyshevDist(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dX = Math.abs(x2 - x1);
        int dY = Math.abs(y2 - y1);
        int dZ = Math.abs(z2 - z1);
        return Math.max(Math.max(dX, dY), dZ);
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum Shape {
        Cube,
        UniformCube,
        Sphere
    }

    public enum SortMode {
        None,
        Closest,
        Furthest,
        TopDown
    }
}
