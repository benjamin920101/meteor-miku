package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.ChatUtils;
import com.github.mikumiku.addon.util.ItemUtil;
import com.github.mikumiku.addon.util.timer.SyncedTickTimer;
import com.github.mikumiku.addon.util.timer.Timers;
import lombok.Getter;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AutoMiner extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgShulkerBoxes = this.settings.createGroup("潜影盒设置");
    private final SettingGroup sgTools = this.settings.createGroup("工具管理");
    private final Setting<List<Block>> targetBlocks = this.sgGeneral
        .add(new BlockListSetting.Builder().name("挖掘目标").description("要挖掘的方块类型 多选").defaultValue(Collections.singletonList(Blocks.SAND)).build());
    private final Setting<Integer> miningRange = this.sgGeneral
        .add(new IntSetting.Builder()
            .name("挖掘范围")
            .description("搜索目标方块的范围")
            .defaultValue(32)
            .min(4)
            .max(200)
            .sliderMin(8)
            .sliderMax(200)
            .build()
        );
    private final Setting<Boolean> packetMine = this.sgGeneral
        .add(new BoolSetting.Builder()
            .name("使用极速包挖")
            .description("使用包挖乱挖周围目标方块，可能捡不到")
            .defaultValue(false)
            .build()
        );
    private final Setting<Integer> delay = this.sgGeneral
        .add(new IntSetting.Builder()
            .name("延迟")
            .description("操作之间的延迟（tick）")
            .defaultValue(5)
            .min(0)
            .max(20)
            .sliderMin(0)
            .sliderMax(20)
            .build()
        );
    private final Setting<Boolean> autoReturn = this.sgGeneral
        .add(new BoolSetting.Builder()
            .name("自动返回")
            .description("完成存储或取工具后自动返回挖掘")
            .defaultValue(true)
            .build()
        );
    private final Setting<Integer> shulkerSearchRadius = this.sgShulkerBoxes
        .add(new IntSetting.Builder()
            .name("潜影盒搜索半径")
            .description("搜索潜影盒的半径范围")
            .defaultValue(32)
            .min(4)
            .max(200)
            .sliderMin(8)
            .sliderMax(200)
            .build()
        );
    private final Setting<ToolType> toolType = this.sgTools
        .add(new Builder<ToolType>()
            .name("工具类型")
            .description("选择使用的工具类型")
            .defaultValue(ToolType.SHOVEL)
            .build()
        );
    private final Setting<Integer> minDurability = this.sgTools
        .add(new IntSetting.Builder()
            .name("最低耐久度")
            .description("工具耐久度低于此值时更换")
            .defaultValue(10)
            .min(1)
            .max(100)
            .build()
        );
    private MinerState currentState = MinerState.WAITING_TOOL_SELECTION;
    private int tickTimer = 0;
    private final SyncedTickTimer cacheClearTimer = Timers.tickTimer();
    private BlockPos lastMiningPos = null;
    private BlockPos currentTarget = null;
    private BlockPos toolShulkerPos = null;
    private final Set<BlockPos> protectedShulkerBoxes = new HashSet<>();
    private int shulkerInteractionTimer = 0;
    private boolean waitingForShulkerOpen = false;
    private final Map<BlockPos, Long> positionCache = new HashMap<>();
    private static final long CACHE_EXPIRE_TIME_MS = 5000L;

    public AutoMiner() {
        super("挖沙挖一切", "自动挖掘指定方块，支持背包管理和工具更换");
    }

    public void resetToolShulkerSelection() {
        this.toolShulkerPos = null;
        this.currentState = MinerState.WAITING_TOOL_SELECTION;
        ChatUtils.sendMsg("工具潜影盒选择已重置，请重新右键选择工具潜影盒！");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (!BaritoneUtils.IS_AVAILABLE) {
            this.error("Baritone 不可用！");
            this.toggle();
        } else {
            this.currentState = MinerState.WAITING_TOOL_SELECTION;
            this.tickTimer = 0;
            this.lastMiningPos = null;
            this.currentTarget = null;
            this.toolShulkerPos = null;
            this.protectedShulkerBoxes.clear();
            this.shulkerInteractionTimer = 0;
            this.waitingForShulkerOpen = false;
            this.scanAndProtectShulkerBoxes();
            ChatUtils.sendMsg("自动挖掘模块已启动");
            ChatUtils.sendMsg("请右键点击工具存储潜影盒来选择它！");
        }
    }

    public void onDeactivate() {
        if (BaritoneUtils.IS_AVAILABLE) {
            this.cancelBaritone();
            this.clearProtectedBlocks();
        }

        this.positionCache.clear();
        this.protectedShulkerBoxes.clear();
        ChatUtils.sendMsg("自动挖掘模块已停止");
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (this.currentState == MinerState.WAITING_TOOL_SELECTION) {
            BlockPos pos = event.result.getBlockPos();
            Block block = this.mc.world.getBlockState(pos).getBlock();
            if (block instanceof ShulkerBoxBlock) {
                this.toolShulkerPos = pos;
                this.currentState = MinerState.MINING;
                this.updateProtectedBlocks();
                event.cancel();
                ChatUtils.sendMsg("工具潜影盒已选择: " + pos.toShortString());
                ChatUtils.sendMsg("已保护所有潜影盒免被挖掘");
                ChatUtils.sendMsg("开始自动挖掘！");
            } else {
                this.warning("请右键点击潜影盒来选择工具存储位置！");
            }
        }
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player != null && this.mc.world != null) {
            if (this.currentState != MinerState.WAITING_TOOL_SELECTION) {
                if (this.cacheClearTimer.tick(500L, true)) {
                    this.cleanExpiredCache();
                }

                if (this.tickTimer > 0) {
                    this.tickTimer--;
                } else {
                    switch (this.currentState) {
                        case MINING:
                            this.handleMining();
                        case INVENTORY_FULL:
                            this.handleInventoryFull();
                            break;
                        case TOOL_BROKEN:
                            this.handleToolBroken();
                            break;
                        case GOING_TO_STORAGE:
                            this.handleGoingToStorage();
                            break;
                        case GOING_TO_TOOLS:
                            this.handleGoingToTools();
                            break;
                        case STORING_ITEMS:
                            this.handleStoringItems();
                            break;
                        case GETTING_TOOLS:
                            this.handleGettingTools();
                            break;
                        case RETURNING:
                            this.handleReturning();
                            break;
                        case WAITING_FOR_TARGET_BOX:
                        default:
                            break;
                    }

                    this.tickTimer = this.delay.get();
                }
            }
        }
    }

    private void handleMining() {
        if (this.isInventoryFull()) {
            this.currentState = MinerState.INVENTORY_FULL;
        } else if (this.needNewTool()) {
            this.currentState = MinerState.TOOL_BROKEN;
        } else {
            BlockPos targetPos = this.findNearestTargetBlock();
            if (targetPos != null) {
                if (this.currentTarget == null || !this.currentTarget.equals(targetPos)) {
                    if (this.packetMine.get() && !this.isInCache(targetPos)) {
                        BlockPos playerPos = this.mc.player.getBlockPos();
                        double distance = Math.sqrt(
                            Math.pow(targetPos.getX() - playerPos.getX(), 2.0)
                                + Math.pow(targetPos.getY() - playerPos.getY(), 2.0)
                                + Math.pow(targetPos.getZ() - playerPos.getZ(), 2.0)
                        );
                        if (targetPos.getY() >= playerPos.getY() && distance < 4.2) {
                            this.addToCache(targetPos);
                            BlockUtils.breakBlock(targetPos, true);
                        }
                    }

                    this.currentTarget = targetPos;
                    this.lastMiningPos = this.mc.player.getBlockPos();
                    Block targetBlock = this.mc.world.getBlockState(targetPos).getBlock();
                    BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(this.targetBlocks.get().toArray(new Block[0]));
                }
            } else {
                ChatUtils.sendMsg("附近没有找到目标方块");
            }
        }
    }

    private void handleInventoryFull() {
        ChatUtils.sendMsg("背包已满，搜索存储潜影盒");
        BlockPos storageShulker = this.findSandStorageShulkerBox();
        if (storageShulker != null) {
            this.currentState = MinerState.GOING_TO_STORAGE;
            this.cancelBaritone();
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(storageShulker));
            ChatUtils.sendMsg("找到存储潜影盒: " + storageShulker.toShortString());
        } else {
            this.error("未找到存储潜影盒！");
            this.currentState = MinerState.MINING;
        }
    }

    private void handleToolBroken() {
        ToolType selectedToolType = this.toolType.get();
        ChatUtils.sendMsg("工具耐久度过低，前往工具潜影盒获取" + selectedToolType.getDisplayName());
        if (this.toolShulkerPos != null) {
            this.currentState = MinerState.GOING_TO_TOOLS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.toolShulkerPos));
            ChatUtils.sendMsg("前往工具存储潜影盒: " + this.toolShulkerPos.toShortString());
        } else {
            this.error("工具潜影盒位置未设置！请重新启动模块并选择工具潜影盒。");
            this.currentState = MinerState.WAITING_TOOL_SELECTION;
            ChatUtils.sendMsg("请右键点击工具存储潜影盒来选择它！");
        }
    }

    private void handleGoingToStorage() {
        BlockPos nearbyShulker = this.findNearbyShulkerBox();
        if (nearbyShulker != null && this.mc.player.getBlockPos().isWithinDistance(nearbyShulker, 4.0)) {
            this.currentState = MinerState.STORING_ITEMS;
            this.cancelBaritone();
        }
    }

    private void handleGoingToTools() {
        if (this.toolShulkerPos != null && this.mc.player.getBlockPos().isWithinDistance(this.toolShulkerPos, 4.0)) {
            this.currentState = MinerState.GETTING_TOOLS;
            this.cancelBaritone();
        }
    }

    private void handleStoringItems() {
        if (this.isShulkerBoxOpen()) {
            this.storeSandToShulkerBox();
        } else {
            if (this.waitingForShulkerOpen) {
                this.shulkerInteractionTimer++;
                if (this.shulkerInteractionTimer <= 40) {
                    return;
                }

                this.waitingForShulkerOpen = false;
                this.shulkerInteractionTimer = 0;
                this.warning("潜影盒打开超时，重试中...");
            }

            if (this.isShulkerBoxOpen()) {
                this.storeSandToShulkerBox();
            } else {
                BlockPos nearbyShulker = this.findNearbyShulkerBox();
                if (nearbyShulker != null) {
                    this.openShulkerBox(nearbyShulker);
                    this.waitingForShulkerOpen = true;
                    this.shulkerInteractionTimer = 0;
                } else {
                    this.error("附近没有找到潜影盒！");
                    this.currentState = MinerState.MINING;
                }
            }
        }
    }

    private void handleGettingTools() {
        if (this.isShulkerBoxOpen()) {
            this.getToolFromShulkerBox();
        } else {
            if (this.waitingForShulkerOpen) {
                this.shulkerInteractionTimer++;
                if (this.shulkerInteractionTimer <= 40) {
                    return;
                }

                this.waitingForShulkerOpen = false;
                this.shulkerInteractionTimer = 0;
                this.warning("工具潜影盒打开超时，重试中...");
            }

            if (this.isShulkerBoxOpen()) {
                this.getToolFromShulkerBox();
            } else if (this.toolShulkerPos != null) {
                this.openShulkerBox(this.toolShulkerPos);
                this.waitingForShulkerOpen = true;
                this.shulkerInteractionTimer = 0;
            } else {
                this.error("工具潜影盒位置未设置！");
                this.currentState = MinerState.WAITING_TOOL_SELECTION;
            }
        }
    }

    private void handleReturning() {
        if (this.lastMiningPos != null && this.mc.player.getBlockPos().isWithinDistance(this.lastMiningPos, 3.0)) {
            this.currentState = MinerState.MINING;
            this.cancelBaritone();
            this.info("已返回挖掘位置");
        }
    }

    private boolean isInventoryFull() {
        int emptySlots = 0;

        for (int i = 9; i < 36; i++) {
            if (this.mc.player.getInventory().getStack(i).isEmpty()) {
                emptySlots++;
            }
        }

        return emptySlots <= 2;
    }

    private boolean needNewTool() {
        FindItemResult tool = InvUtils.find(itemStackx -> DV.of(ItemUtil.class).isTool(itemStackx.getItem()));
        if (!tool.found()) {
            return true;
        } else {
            ItemStack itemStack = this.mc.player.getInventory().getStack(tool.slot());
            if (!itemStack.isDamageable()) {
                return false;
            } else {
                int maxDamage = itemStack.getMaxDamage();
                int currentDamage = itemStack.getDamage();
                int remainingDurability = maxDamage - currentDamage;
                return remainingDurability <= this.minDurability.get();
            }
        }
    }

    private BlockPos findNearestTargetBlock() {
        BlockPos playerPos = this.mc.player.getBlockPos();
        BlockPos nearestBlock = null;
        double nearestDistance = Double.MAX_VALUE;
        List<Block> targets = this.targetBlocks.get();
        int range = this.miningRange.get();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (targets.contains(block)) {
                        double distance = playerPos.getSquaredDistance(pos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestBlock = pos;
                        }
                    }
                }
            }
        }

        return nearestBlock;
    }

    private BlockPos findSandStorageShulkerBox() {
        BlockPos playerPos = this.mc.player.getBlockPos();
        int radius = this.shulkerSearchRadius.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ShulkerBoxBlock && !pos.equals(this.toolShulkerPos)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private BlockPos findNearbyShulkerBox() {
        BlockPos playerPos = this.mc.player.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private void scanAndProtectShulkerBoxes() {
        this.protectedShulkerBoxes.clear();
        BlockPos playerPos = this.mc.player.getBlockPos();
        int radius = this.shulkerSearchRadius.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        this.protectedShulkerBoxes.add(pos);
                    }
                }
            }
        }

        ChatUtils.sendMsg("已扫描到 " + this.protectedShulkerBoxes.size() + " 个潜影盒，将被保护");
    }

    private void updateProtectedBlocks() {
        if (BaritoneUtils.IS_AVAILABLE) {
            try {
                Settings settings = BaritoneAPI.getSettings();
                List<Block> blocksToAvoid = new ArrayList<>();
                blocksToAvoid.add(Blocks.SHULKER_BOX);
                blocksToAvoid.add(Blocks.WHITE_SHULKER_BOX);
                blocksToAvoid.add(Blocks.ORANGE_SHULKER_BOX);
                blocksToAvoid.add(Blocks.MAGENTA_SHULKER_BOX);
                blocksToAvoid.add(Blocks.LIGHT_BLUE_SHULKER_BOX);
                blocksToAvoid.add(Blocks.YELLOW_SHULKER_BOX);
                blocksToAvoid.add(Blocks.LIME_SHULKER_BOX);
                blocksToAvoid.add(Blocks.PINK_SHULKER_BOX);
                blocksToAvoid.add(Blocks.GRAY_SHULKER_BOX);
                blocksToAvoid.add(Blocks.LIGHT_GRAY_SHULKER_BOX);
                blocksToAvoid.add(Blocks.CYAN_SHULKER_BOX);
                blocksToAvoid.add(Blocks.PURPLE_SHULKER_BOX);
                blocksToAvoid.add(Blocks.BLUE_SHULKER_BOX);
                blocksToAvoid.add(Blocks.BROWN_SHULKER_BOX);
                blocksToAvoid.add(Blocks.GREEN_SHULKER_BOX);
                blocksToAvoid.add(Blocks.RED_SHULKER_BOX);
                blocksToAvoid.add(Blocks.BLACK_SHULKER_BOX);
                settings.blocksToDisallowBreaking.value = blocksToAvoid;
                this.info("已设置 Baritone 保护所有潜影盒");
            } catch (Exception var3) {
                this.warning("设置 Baritone 保护失败: " + var3.getMessage());
            }
        }
    }

    private void clearProtectedBlocks() {
        if (BaritoneUtils.IS_AVAILABLE) {
            try {
                Settings settings = BaritoneAPI.getSettings();
                settings.blocksToDisallowBreaking.value = new ArrayList<>();
            } catch (Exception var2) {
                this.warning("清理 Baritone 保护设置失败: " + var2.getMessage());
            }
        }
    }

    private void openShulkerBox(BlockPos pos) {
        if (this.mc.interactionManager != null && this.mc.player != null) {
            try {
                if (!this.mc.player.getBlockPos().isWithinDistance(pos, 5.0)) {
                    this.warning("距离潜影盒太远，无法打开");
                    return;
                }

                Block block = this.mc.world.getBlockState(pos).getBlock();
                if (!(block instanceof ShulkerBoxBlock)) {
                    this.warning("目标位置不是潜影盒: " + pos.toShortString());
                    return;
                }

                Vec3d hitVec = Vec3d.ofCenter(pos);
                Direction side = Direction.UP;
                BlockHitResult hitResult = new BlockHitResult(hitVec, side, pos, false);
                this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hitResult);
                this.info("正在打开潜影盒: " + pos.toShortString());
            } catch (Exception var6) {
                this.error("打开潜影盒时发生错误: " + var6.getMessage());
            }
        }
    }

    private void storeSandToShulkerBox() {
        if (this.mc.interactionManager != null) {
            int itemsMoved = 0;
            int maxMovesPerTick = 3;
            List<Block> targets = this.targetBlocks.get();
            boolean hasMovedItems = false;

            for (int i = 9; i < 36; i++) {
                ItemStack stack = this.mc.player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    boolean isTargetBlock = false;

                    for (Block targetBlock : targets) {
                        if (stack.getItem() == targetBlock.asItem()) {
                            isTargetBlock = true;
                            break;
                        }
                    }

                    if (isTargetBlock) {
                        int targetSlot = this.findShulkerSlotForItem(stack);
                        if (targetSlot == -1) {
                            BlockPos nextShulker = this.findNextEmptyShulkerBox();
                            if (nextShulker != null) {
                                this.closeShulkerBox();
                                this.openShulkerBox(nextShulker);
                                this.waitingForShulkerOpen = true;
                                this.shulkerInteractionTimer = 0;
                                return;
                            }

                            this.closeShulkerBox();
                            if (this.autoReturn.get() && this.lastMiningPos != null) {
                                this.currentState = MinerState.RETURNING;
                                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.lastMiningPos));
                            } else {
                                this.currentState = MinerState.MINING;
                            }

                            this.info("物品存储完成");
                            return;
                        }

                        this.moveItemToShulker(i, targetSlot);
                        itemsMoved++;
                        hasMovedItems = true;
                        this.info("移动" + stack.getItem().getName().getString() + "到潜影盒，数量: " + stack.getCount());
                        if (itemsMoved >= maxMovesPerTick) {
                            break;
                        }
                    }
                }
            }

            if (!this.hasMoreTargetBlocksToStore()) {
                this.closeShulkerBox();
                if (this.autoReturn.get() && this.lastMiningPos != null) {
                    this.currentState = MinerState.RETURNING;
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.lastMiningPos));
                } else {
                    this.currentState = MinerState.MINING;
                }

                this.info("物品存储完成");
            }
        }
    }

    private void getToolFromShulkerBox() {
        if (this.mc.interactionManager != null) {
            int toolSlot = this.findToolInShulker();
            if (toolSlot != -1) {
                int targetSlot = this.findPlayerSlotForTool();
                if (targetSlot != -1) {
                    this.moveItemFromShulker(toolSlot, targetSlot);
                    this.info("获取新工具成功");
                    this.closeShulkerBox();
                    if (this.autoReturn.get() && this.lastMiningPos != null) {
                        this.currentState = MinerState.RETURNING;
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.lastMiningPos));
                    } else {
                        this.currentState = MinerState.MINING;
                    }
                } else {
                    this.warning("背包已满，无法获取新工具！");
                    this.closeShulkerBox();
                    this.currentState = MinerState.MINING;
                }
            } else {
                ToolType selectedToolType = this.toolType.get();
                this.warning("潜影盒中没有找到可用的" + selectedToolType.getDisplayName() + "！");
                this.closeShulkerBox();
                this.currentState = MinerState.MINING;
            }
        }
    }

    private BlockPos findNextEmptyShulkerBox() {
        BlockPos playerPos = this.mc.player.getBlockPos();
        int radius = this.shulkerSearchRadius.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ShulkerBoxBlock && !pos.equals(this.toolShulkerPos)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private int findShulkerSlotForItem(ItemStack stack) {
        if (!(this.mc.currentScreen instanceof ShulkerBoxScreen)) {
            return -1;
        } else {
            ScreenHandler handler = this.mc.player.currentScreenHandler;

            for (int i = 0; i < 27 && i < handler.slots.size(); i++) {
                ItemStack slotStack = handler.getSlot(i).getStack();
                if (slotStack.isEmpty()) {
                    return i;
                }

                if (slotStack.getItem() == stack.getItem() && slotStack.getCount() < slotStack.getMaxCount()) {
                    return i;
                }
            }

            return -1;
        }
    }

    private int findToolInShulker() {
        if (!(this.mc.currentScreen instanceof ShulkerBoxScreen)) {
            return -1;
        } else {
            ScreenHandler handler = this.mc.player.currentScreenHandler;
            ToolType selectedToolType = this.toolType.get();

            for (Item preferredTool : this.getToolPriorityList(selectedToolType)) {
                for (int i = 0; i < 27 && i < handler.slots.size(); i++) {
                    ItemStack slotStack = handler.getSlot(i).getStack();
                    if (!slotStack.isEmpty()
                        && slotStack.getItem() == preferredTool
                        && (!slotStack.isDamageable() || slotStack.getMaxDamage() - slotStack.getDamage() > this.minDurability.get())) {
                        return i;
                    }
                }
            }

            return -1;
        }
    }

    private int findPlayerSlotForTool() {
        ToolType selectedToolType = this.toolType.get();

        for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()
                && DV.of(ItemUtil.class).isTool(stack.getItem())
                && stack.isDamageable()
                && stack.getMaxDamage() - stack.getDamage() <= this.minDurability.get()) {
                return i + 27;
            }
        }

        for (int ix = 0; ix < this.mc.player.getInventory().size(); ix++) {
            if (this.mc.player.getInventory().getStack(ix).isEmpty()) {
                return ix + 27;
            }
        }

        return -1;
    }

    private void moveItemToShulker(int playerSlot, int shulkerSlot) {
        if (this.mc.interactionManager != null) {
            int screenSlot = playerSlot + 27;
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
        }
    }

    private void moveItemFromShulker(int shulkerSlot, int playerSlot) {
        if (this.mc.interactionManager != null) {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, shulkerSlot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
        }
    }

    private boolean hasMoreTargetBlocksToStore() {
        List<Block> targets = this.targetBlocks.get();

        for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                for (Block targetBlock : targets) {
                    if (stack.getItem() == targetBlock.asItem()) {
                        return true;
                    }
                }
            }
        }

        return false;
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
            if (currentTime - cachedTime >= 5000L) {
                this.positionCache.remove(pos);
                return false;
            } else {
                return true;
            }
        }
    }

    private void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        this.positionCache.entrySet().removeIf(entry -> currentTime - entry.getValue() >= 5000L);
    }

    private void cancelBaritone() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    private void closeShulkerBox() {
        if (this.mc.currentScreen instanceof ShulkerBoxScreen) {
            this.mc.player.closeHandledScreen();
            ChatUtils.sendMsg("关闭潜影盒");
        }
    }

    private boolean isShulkerBoxOpen() {
        return this.mc.currentScreen instanceof ShulkerBoxScreen || this.mc.currentScreen instanceof HandledScreen;
    }

    private List<Item> getToolPriorityList(ToolType toolType) {
        return switch (toolType) {
            case SHOVEL ->
                Arrays.asList(Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL);
            case PICKAXE ->
                Arrays.asList(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE);
            case AXE ->
                Arrays.asList(Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.WOODEN_AXE);
            case HOE ->
                Arrays.asList(Items.NETHERITE_HOE, Items.DIAMOND_HOE, Items.IRON_HOE, Items.STONE_HOE, Items.WOODEN_HOE);
        };
    }

    private enum MinerState {
        WAITING_TOOL_SELECTION,
        MINING,
        WAITING_FOR_TARGET_BOX,
        INVENTORY_FULL,
        TOOL_BROKEN,
        GOING_TO_STORAGE,
        GOING_TO_TOOLS,
        STORING_ITEMS,
        GETTING_TOOLS,
        RETURNING
    }

    @Getter
    public enum ToolType {
        SHOVEL("铲子"),
        PICKAXE("镐子"),
        AXE("斧子"),
        HOE("锄头");

        private final String displayName;

        ToolType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
