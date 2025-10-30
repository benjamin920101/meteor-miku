package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.ChatUtils;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class CometTunnel extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Boolean> autoStop = this.sgGeneral
        .add(new BoolSetting.Builder().name("到底自动停止").description("到达基岩层时自动停止").defaultValue(true).build());
    private final Setting<Integer> delay = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("延迟")
                .description("操作之间的延迟（tick）")
                .defaultValue(2)
                .min(0)
                .max(20)
                .sliderMin(0)
                .sliderMax(20)
                .build()
        );
    private final Setting<Block> fillBlock = this.sgGeneral
        .add(
            new BlockSetting.Builder()
                .name("填充方块")
                .description("用于填充的方块类型")
                .defaultValue(Blocks.COBBLESTONE)
                .build()
        );
    private final Setting<Boolean> autoFill = this.sgGeneral
        .add(new BoolSetting.Builder().name("自动填充").description("自动在身后填充方块").defaultValue(true).build());
    private TunnelState currentState = TunnelState.IDLE;
    private int tickTimer = 0;
    private BlockPos startPos = null;
    private BlockPos currentDigPos = null;
    private int blocksBrokenInPattern = 0;
    private BlockPos fillPos = null;

    public CometTunnel() {
        super("挖3填1", "挖3填1 - 把自己埋进一个坑里");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        this.currentState = TunnelState.SELECTING_BLOCK;
        this.tickTimer = 0;
        this.startPos = null;
        this.currentDigPos = null;
        this.blocksBrokenInPattern = 0;
        this.fillPos = null;
        ChatUtils.sendMsg("彗星隧道模块已启动");
        ChatUtils.sendMsg("请在脚下右键点击方块来选择起始位置！");
    }

    public void onDeactivate() {
        this.currentState = TunnelState.IDLE;
        this.startPos = null;
        this.currentDigPos = null;
        this.blocksBrokenInPattern = 0;
        this.fillPos = null;
        ChatUtils.sendMsg("彗星隧道模块已停止");
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (this.currentState == TunnelState.SELECTING_BLOCK) {
            BlockPos pos = event.result.getBlockPos();
            this.startPos = pos;
            this.currentDigPos = pos;
            this.currentState = TunnelState.DIGGING;
            this.blocksBrokenInPattern = 0;
            this.fillPos = null;
            event.cancel();
            ChatUtils.sendMsg("起始位置已选择: " + this.startPos.toShortString());
            ChatUtils.sendMsg("开始挖掘隧道 (挖3填1)");
        }
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player != null && this.mc.world != null) {
            if (this.currentState != TunnelState.SELECTING_BLOCK && this.currentState != TunnelState.IDLE) {
                if (this.tickTimer > 0) {
                    this.tickTimer--;
                } else {
                    switch (this.currentState) {
                        case DIGGING:
                            this.handleDigging();
                            break;
                        case FILLING:
                            this.handleFilling();
                    }

                    this.tickTimer = this.delay.get();
                }
            }
        }
    }

    private void handleDigging() {
        if (this.currentDigPos != null) {
            Block blockAtPos = this.mc.world.getBlockState(this.currentDigPos).getBlock();
            if (this.autoStop.get() && blockAtPos == Blocks.BEDROCK) {
                ChatUtils.sendMsg("到达基岩层，停止挖掘");
                this.currentState = TunnelState.IDLE;
                this.toggle();
            } else if (blockAtPos == Blocks.BEDROCK) {
                this.moveToNextPosition();
            } else {
                if (blockAtPos != Blocks.AIR) {
                    BlockUtils.breakBlock(this.currentDigPos, true);
                    this.blocksBrokenInPattern++;
                    if (this.blocksBrokenInPattern >= 3) {
                        this.currentState = TunnelState.FILLING;
                        this.calculateFillPosition();
                        this.blocksBrokenInPattern = 0;
                    }
                } else {
                    this.blocksBrokenInPattern++;
                    if (this.blocksBrokenInPattern >= 3) {
                        this.currentState = TunnelState.FILLING;
                        this.calculateFillPosition();
                        this.blocksBrokenInPattern = 0;
                    }
                }
            }
        }
    }

    private void handleFilling() {
        if (this.fillPos == null) {
            this.currentState = TunnelState.DIGGING;
            this.moveToNextPosition();
        } else {
            Block blockAtFillPos = this.mc.world.getBlockState(this.fillPos).getBlock();
            if (blockAtFillPos != Blocks.AIR && blockAtFillPos != Blocks.CAVE_AIR) {
                this.fillPos = null;
                this.currentState = TunnelState.DIGGING;
            } else {
                int slot = BagUtil.findItemInventorySlotGrim(this.fillBlock.get().asItem());
                if (slot == -1) {
                    this.warning("背包中没有 " + this.fillBlock.get().getName().getString());
                    this.currentState = TunnelState.DIGGING;
                    this.fillPos = null;
                    return;
                }

                BagUtil.doSwap(slot);
                BaritoneUtil.placeBlock(this.fillPos);
                BagUtil.doSwap(slot);
                BagUtil.sync();
                this.info("在 " + this.fillPos.toShortString() + " 放置 " + this.fillBlock.get().getName().getString());
                this.fillPos = null;
                this.currentState = TunnelState.DIGGING;
            }
        }
    }

    private void calculateFillPosition() {
        if (this.startPos != null) {
            this.fillPos = this.currentDigPos.down(3);
        }
    }

    private void moveToNextPosition() {
        if (this.currentDigPos != null) {
            this.currentDigPos = this.currentDigPos.down(1);
            BlockPos playerPos = this.mc.player.getBlockPos();
            if (!this.isPlayerInTunnel(playerPos)) {
                this.warning("玩家已离开隧道");
                this.currentState = TunnelState.IDLE;
            }
        }
    }

    private boolean isPlayerInTunnel(BlockPos playerPos) {
        return this.startPos != null && this.currentDigPos != null && playerPos.getX() == this.startPos.getX()
            && playerPos.getZ() == this.startPos.getZ()
            && playerPos.getY() <= this.startPos.getY()
            && playerPos.getY() >= this.currentDigPos.getY();
    }

    private boolean hasBlockInInventory(Block block) {
        Item blockItem = block.asItem();

        for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == blockItem) {
                return true;
            }
        }

        return false;
    }

    private String getStateString() {
        return switch (this.currentState) {
            case IDLE -> "空闲";
            case SELECTING_BLOCK -> "等待选择";
            case DIGGING -> "挖掘 (" + this.blocksBrokenInPattern + "/3)";
            case FILLING -> "填充";
        };
    }

    private enum TunnelState {
        IDLE,
        SELECTING_BLOCK,
        DIGGING,
        FILLING
    }
}
