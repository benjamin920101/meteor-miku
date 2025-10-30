package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.ItemUtil;
import com.github.mikumiku.addon.util.PlayerUtil;
import com.github.mikumiku.addon.util.VUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MaceCombo extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgPower = this.settings.createGroup("威力增强");
    private final Setting<WeaponType> weaponType = this.sgGeneral
        .add(new Builder<WeaponType>().name("武器类型").description("触发切换的武器类型").defaultValue(WeaponType.SWORD).build());
    private final Setting<Boolean> autoSwitch = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("自动切换")
                .description("使用指定武器攻击时自动切换到锤子")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> breachOnly = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("仅破甲锤")
                .description("只切换到有破甲附魔的锤子")
                .defaultValue(true)
                .visible(this.autoSwitch::get)
                .build()
        );
    private final Setting<Integer> switchDelay = this.sgGeneral
        .add(
            new IntSetting.Builder()
                .name("切回延迟")
                .description("攻击后多少tick切回剑")
                .defaultValue(1)
                .min(0)
                .max(5)
                .sliderMax(5)
                .visible(this.autoSwitch::get)
                .build()
        );
    private final Setting<Boolean> macePower = this.sgPower
        .add(
            new BoolSetting.Builder()
                .name("威力增强")
                .description("使用锤子时增强伤害")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> elytraOnly = this.sgPower
        .add(
            new BoolSetting.Builder()
                .name("仅鞘翅时")
                .description("只在鞘翅飞行时增强威力")
                .defaultValue(true)
                .visible(this.macePower::get)
                .build()
        );
    private final Setting<Boolean> maxPower = this.sgPower
        .add(
            new BoolSetting.Builder()
                .name("最大威力")
                .description("自动寻找最大可用高度")
                .defaultValue(false)
                .visible(this.macePower::get)
                .build()
        );
    private final Setting<Integer> fallHeight = this.sgPower
        .add(
            new IntSetting.Builder()
                .name("下落高度")
                .description("模拟的下落高度")
                .defaultValue(22)
                .min(1)
                .max(50)
                .sliderMax(50)
                .visible(() -> this.macePower.get() && !(Boolean) this.maxPower.get())
                .build()
        );
    private final Setting<Boolean> checkTarget = this.sgPower
        .add(
            new BoolSetting.Builder()
                .name("检查目标")
                .description("不对创造模式、无敌、格挡的玩家使用")
                .defaultValue(true)
                .visible(this.macePower::get)
                .build()
        );
    private boolean wasGliding = false;
    private int originalSlot = -1;
    private int switchBackTicks = 0;

    public MaceCombo() {
        super(MikuMikuAddon.CATEGORY_MIKU_COMBAT, "切锤增伤", "武器锤连击. 使用指定武器攻击时自动切锤增伤");
    }

    public void onDeactivate() {
        if (this.originalSlot != -1 && this.mc.player != null) {
            BagUtil.swap(this.originalSlot, false);
            this.originalSlot = -1;
        }

        this.switchBackTicks = 0;
        this.wasGliding = false;
    }

    @EventHandler(
        priority = 100
    )
    private void onAttack(AttackEntityEvent event) {
        if (this.mc.player != null && this.mc.world != null) {
            Entity target = event.entity;
            if (target != null && target.isAlive() && target instanceof LivingEntity) {
                this.wasGliding = DV.of(VUtil.class).isFallFlying(this.mc);
                if (this.autoSwitch.get() && this.isWeaponTypeMatched()) {
                    FindItemResult mace = this.findMace();
                    if (mace.found()) {
                        this.originalSlot = DV.of(PlayerUtil.class).getSelectedSlot(this.mc.player.getInventory());
                        BagUtil.swap(mace.slot(), false);
                        this.switchBackTicks = this.switchDelay.get();
                    }
                }
            }
        }
    }

    @EventHandler(
        priority = -100
    )
    private void onAttackAfter(AttackEntityEvent event) {
        if (this.mc.player != null && this.mc.world != null) {
            if (event.entity instanceof LivingEntity living) {
                if (this.macePower.get()
                    && this.mc.player.getMainHandStack().getItem() instanceof MaceItem
                    && (!(Boolean) this.elytraOnly.get() || DV.of(VUtil.class).isFallFlying(this.mc))
                    && !this.shouldSkipTarget(living)) {
                    this.applyMacePower();
                }
                if (this.wasGliding && !DV.of(VUtil.class).isFallFlying(this.mc)) {
                    this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
                }
            }
        }
    }

    @EventHandler
    private void onTick(Post event) {
        if (this.mc.player != null) {
            if (this.switchBackTicks > 0) {
                this.switchBackTicks--;
                if (this.switchBackTicks == 0 && this.originalSlot != -1) {
                    BagUtil.swap(this.originalSlot, false);
                    this.originalSlot = -1;
                }
            }

            if (this.wasGliding && !DV.of(VUtil.class).isFallFlying(this.mc)) {
                this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
                this.wasGliding = false;
            }
        }
    }

    private boolean isWeaponTypeMatched() {
        switch (this.weaponType.get()) {
            case SWORD:
                return DV.of(ItemUtil.class).isSword(this.mc.player.getMainHandStack().getItem());
            case AXE:
                return this.mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case HAND:
                return this.mc.player.getMainHandStack().isEmpty();
            case ANY:
                return true;
            default:
                return false;
        }
    }

    private FindItemResult findMace() {
        return this.breachOnly.get()
            ? BagUtil.findInHotbar(stack -> this.isMaceWithBreach(stack))
            : BagUtil.findInHotbar(stack -> stack.getItem() instanceof MaceItem);
    }

    private boolean isMaceWithBreach(ItemStack stack) {
        if (!(stack.getItem() instanceof MaceItem)) {
            return false;
        } else {
            ItemEnchantmentsComponent enchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

            for (Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentEntries()) {
                RegistryEntry<?> enchant = entry.getKey();
                if (enchant.getKey().isPresent()) {
                    Identifier id = enchant.getKey().get().getValue();
                    if (id.getPath().equals("breach")) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private boolean shouldSkipTarget(LivingEntity target) {
        if (!(Boolean) this.checkTarget.get()) {
            return false;
        } else {
            return !(target instanceof PlayerEntity player) ? target.isInvulnerable() : player.isCreative() || player.isBlocking();
        }
    }

    private void applyMacePower() {
        try {
            Vec3d originalPos = this.mc.player.getPos();
            int height = this.getOptimalHeight();
            if (height <= 0) {
                return;
            }

            BlockPos checkPos1 = this.mc.player.getBlockPos().add(0, height, 0);
            BlockPos checkPos2 = checkPos1.up();
            if (!this.isSafeBlock(checkPos1) || !this.isSafeBlock(checkPos2)) {
                return;
            }

            this.applyPower(originalPos, height);
        } catch (Exception var5) {
        }
    }

    private void applyPower(Vec3d originalPos, int height) {
        int packets = Math.min((int) Math.ceil(height / 10.0), 20);

        for (int i = 0; i < Math.max(4, packets - 1); i++) {
            this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).getOnGroundOnly(false));
        }

        double targetY = this.mc.player.getY() + Math.min(height, this.fallHeight.get());
        this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).getPositionAndOnGround(this.mc.player.getX(), targetY, this.mc.player.getZ(), false));
        this.mc.player.networkHandler.sendPacket(DV.of(VUtil.class).getPositionAndOnGround(originalPos.x, originalPos.y, originalPos.z, false));
    }

    private int getOptimalHeight() {
        if (!(Boolean) this.maxPower.get()) {
            return this.fallHeight.get();
        } else {
            BlockPos playerPos = this.mc.player.getBlockPos();
            int maxSearch = playerPos.getY() + 170;

            for (int y = maxSearch; y > playerPos.getY(); y--) {
                BlockPos check1 = new BlockPos(playerPos.getX(), y, playerPos.getZ());
                BlockPos check2 = check1.up();
                if (this.isSafeBlock(check1) && this.isSafeBlock(check2)) {
                    return y - playerPos.getY();
                }
            }

            return 0;
        }
    }

    private boolean isSafeBlock(BlockPos pos) {
        return this.mc.world.getBlockState(pos).isReplaceable()
            && this.mc.world.getFluidState(pos).isEmpty()
            && !this.mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
    }

    public enum WeaponType {
        SWORD("剑"),
        AXE("斧头"),
        HAND("空手"),
        ANY("任意武器");

        private final String name;

        WeaponType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
