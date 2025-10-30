package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.*;
import lombok.Getter;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class KillAuraMiku extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgTargeting = this.settings.createGroup("目标选择");
    private final SettingGroup sgTiming = this.settings.createGroup("时机控制");
    private final Setting<Weapon> weapon = this.sgGeneral
        .add(new Builder<Weapon>().name("武器类型").description("仅在手持指定武器时攻击实体").defaultValue(Weapon.All).build());
    private final Setting<RotationMode> rotation = this.sgGeneral
        .add(new Builder<RotationMode>().name("视角旋转").description("决定何时将视角转向目标").defaultValue(RotationMode.OnHit).build());
    private final Setting<Boolean> autoSwitch = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("自动切换")
                .description("攻击目标时自动切换到选定的武器")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> packetAttack = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("发包攻击")
                .description("更好的模式")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> stopSprint = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("停止疾跑")
                .description("攻击前停止疾跑以保持原版行为")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> stopShield = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("停止格挡")
                .description("攻击前自动处理盾牌格挡")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> onlyOnClick = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("仅在点击时")
                .description("仅在按住鼠标左键时攻击")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> onlyOnLook = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("仅在注视时")
                .description("仅在注视实体时攻击")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> pauseOnCombat = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("暂停Baritone")
                .description("在攻击实体时暂时冻结 Baritone 自动寻路")
                .defaultValue(true)
                .build()
        );
    private final Setting<ShieldMode> shieldMode = this.sgGeneral
        .add(
            new Builder<ShieldMode>().name("破盾模式").description("尝试使用斧头破坏目标的盾牌").defaultValue(ShieldMode.Break)
                .visible(() -> this.autoSwitch.get() && this.weapon.get() != Weapon.Axe)
                .build()
        );
    private final Setting<Set<EntityType<?>>> entities = this.sgTargeting
        .add(
            new EntityTypeListSetting.Builder()
                .name("实体类型")
                .description("要攻击的实体类型")
                .onlyAttackable()
                .defaultValue(
                    new EntityType[]{
                        EntityType.PLAYER,
                        EntityType.BLAZE,
                        EntityType.HUSK,
                        EntityType.WIND_CHARGE,
                        EntityType.CAVE_SPIDER,
                        EntityType.CREEPER,
                        EntityType.DROWNED,
                        EntityType.ELDER_GUARDIAN,
                        EntityType.ENDER_DRAGON,
                        EntityType.ENDERMAN,
                        EntityType.ENDERMITE,
                        EntityType.EVOKER,
                        EntityType.GHAST,
                        EntityType.GIANT,
                        EntityType.GUARDIAN,
                        EntityType.HOGLIN,
                        EntityType.HUSK,
                        EntityType.ILLUSIONER,
                        EntityType.MAGMA_CUBE,
                        EntityType.PHANTOM,
                        EntityType.PIGLIN,
                        EntityType.PIGLIN_BRUTE,
                        EntityType.PILLAGER,
                        EntityType.RAVAGER,
                        EntityType.SHULKER,
                        EntityType.SILVERFISH,
                        EntityType.SKELETON,
                        EntityType.SLIME,
                        EntityType.SPIDER,
                        EntityType.STRAY,
                        EntityType.VEX,
                        EntityType.VINDICATOR,
                        EntityType.WARDEN,
                        EntityType.WITCH,
                        EntityType.WITHER,
                        EntityType.WITHER_SKELETON,
                        EntityType.ZOMBIE,
                        EntityType.ZOMBIFIED_PIGLIN,
                        EntityType.ZOGLIN,
                        EntityType.FIREBALL,
                        EntityType.SHULKER_BULLET
                    }
                )
                .build()
        );
    private final Setting<SortPriority> priority = this.sgTargeting
        .add(new Builder<SortPriority>().name("优先级").description("范围内目标的筛选方式").defaultValue(SortPriority.ClosestAngle).build());
    private final Setting<Integer> maxTargets = this.sgTargeting
        .add(
            new IntSetting.Builder()
                .name("最大目标数")
                .description("同时锁定的实体数量")
                .defaultValue(1)
                .min(1)
                .sliderRange(1, 5)
                .visible(() -> !(Boolean) this.onlyOnLook.get())
                .build()
        );
    private final Setting<Double> range = this.sgTargeting
        .add(
            new DoubleSetting.Builder()
                .name("攻击范围")
                .description("可攻击实体的最大距离")
                .defaultValue(3.1)
                .min(3.0)
                .sliderMax(7.0)
                .build()
        );
    private final Setting<Double> wallsRange = this.sgTargeting
        .add(
            new DoubleSetting.Builder()
                .name("穿墙范围")
                .description("可穿墙攻击实体的最大距离")
                .defaultValue(4.5)
                .min(2.0)
                .sliderMax(7.0)
                .build()
        );
    private final Setting<EntityAge> mobAgeFilter = this.sgTargeting
        .add(new Builder<EntityAge>().name("生物年龄过滤").description("决定要攻击的生物年龄（幼年、成年或全部）").defaultValue(EntityAge.Adult).build());
    private final Setting<Boolean> ignoreNamed = this.sgTargeting
        .add(
            new BoolSetting.Builder()
                .name("忽略命名生物")
                .description("是否攻击拥有自定义名称的生物")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> ignorePassive = this.sgTargeting
        .add(
            new BoolSetting.Builder()
                .name("忽略被动生物")
                .description("仅在被动型生物主动攻击你时才进行反击.如猪人、小黑、狼")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> ignoreTamed = this.sgTargeting
        .add(
            new BoolSetting.Builder()
                .name("忽略驯服生物")
                .description("避免攻击你驯服的生物")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> pauseOnLag = this.sgTiming
        .add(
            new BoolSetting.Builder()
                .name("卡顿时暂停")
                .description("服务器卡顿时暂停攻击")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> pauseOnUse = this.sgTiming
        .add(
            new BoolSetting.Builder()
                .name("使用物品时暂停")
                .description("使用物品时不进行攻击")
                .defaultValue(false)
                .build()
        );
    private final Setting<Boolean> pauseOnCA = this.sgTiming
        .add(
            new BoolSetting.Builder()
                .name("水晶光环时暂停")
                .description("水晶光环放置时不进行攻击")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> tpsSync = this.sgTiming
        .add(
            new BoolSetting.Builder()
                .name("TPS同步")
                .description("尝试将攻击延迟与服务器 TPS 同步")
                .defaultValue(true)
                .build()
        );
    private final Setting<Boolean> customDelay = this.sgTiming
        .add(
            new BoolSetting.Builder()
                .name("自定义延迟")
                .description("使用自定义延迟而非原版冷却时间")
                .defaultValue(false)
                .build()
        );
    private final Setting<Integer> hitDelay = this.sgTiming
        .add(
            new IntSetting.Builder()
                .name("攻击延迟")
                .description("攻击实体的速度（以刻为单位）")
                .defaultValue(13)
                .min(0)
                .sliderMax(60)
                .visible(this.customDelay::get)
                .build()
        );
    private final Setting<Integer> switchDelay = this.sgTiming
        .add(
            new IntSetting.Builder()
                .name("切换延迟")
                .description("切换快捷栏后等待多少刻才能攻击实体")
                .defaultValue(0)
                .min(0)
                .sliderMax(10)
                .build()
        );
    private final List<Entity> targets = new ArrayList<>();
    private int switchTimer;
    private int hitTimer;
    private boolean wasPathing = false;
    public boolean attacking;

    public KillAuraMiku() {
        super(MikuMikuAddon.CATEGORY_MIKU_COMBAT, "Miku杀戮光环", "超级强力的杀敌光环,自动攻击你周围指定的实体,不卡脚");
    }

    public void onDeactivate() {
        this.targets.clear();
        this.attacking = false;
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player.isAlive() && PlayerUtils.getGameMode() != GameMode.SPECTATOR) {
            if (!(Boolean) this.pauseOnUse.get() || !this.mc.interactionManager.isBreakingBlock() && !this.mc.player.isUsingItem()) {
                if (!(Boolean) this.onlyOnClick.get() || this.mc.options.attackKey.isPressed()) {
                    if (!(TickRate.INSTANCE.getTimeSinceLastTick() >= 1.0F) || !(Boolean) this.pauseOnLag.get()) {
                        if (!(Boolean) this.pauseOnCA.get()
                            || !Modules.get().get(CrystalAura.class).isActive()
                            || Modules.get().get(CrystalAura.class).kaTimer <= 0) {
                            if (this.onlyOnLook.get()) {
                                Entity targeted = this.mc.targetedEntity;
                                if (targeted == null) {
                                    return;
                                }

                                if (!this.entityCheck(targeted)) {
                                    return;
                                }

                                this.targets.clear();
                                this.targets.add(this.mc.targetedEntity);
                            } else {
                                this.targets.clear();
                                TargetUtils.getList(this.targets, this::entityCheck, this.priority.get(), this.maxTargets.get());
                            }

                            if (this.targets.isEmpty()) {
                                this.attacking = false;
                                if (this.wasPathing) {
                                    PathManagers.get().resume();
                                    this.wasPathing = false;
                                }
                            } else {
                                Entity primary = this.targets.getFirst();
                                if (this.autoSwitch.get()) {
                                    Predicate<ItemStack> predicate = switch (this.weapon.get()) {
                                        case Sword -> stack -> DV.of(ItemUtil.class).isSword(stack.getItem());
                                        case Axe -> stack -> stack.getItem() instanceof AxeItem;
                                        case Mace -> stack -> stack.getItem() instanceof MaceItem;
                                        case Trident -> stack -> stack.getItem() instanceof TridentItem;
                                        case All -> stack -> stack.getItem() instanceof AxeItem
                                            || DV.of(ItemUtil.class).isSword(stack.getItem())
                                            || stack.getItem() instanceof MaceItem
                                            || stack.getItem() instanceof TridentItem;
                                        default -> o -> true;
                                    };
                                    FindItemResult weaponResult = InvUtils.findInHotbar(predicate);
                                    if (this.shouldShieldBreak()) {
                                        FindItemResult axeResult = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof AxeItem);
                                        if (axeResult.found()) {
                                            weaponResult = axeResult;
                                        }
                                    }

                                    InvUtils.swap(weaponResult.slot(), false);
                                }

                                if (this.itemInHand()) {
                                    this.attacking = true;
                                    if (this.rotation.get() == RotationMode.Always) {
                                        Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
                                    }

                                    if (this.pauseOnCombat.get() && PathManagers.get().isPathing() && !this.wasPathing) {
                                        PathManagers.get().pause();
                                        this.wasPathing = true;
                                    }

                                    if (this.delayCheck()) {
                                        this.targets.forEach(this::attack);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onSendPacket(Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            this.switchTimer = this.switchDelay.get();
        }
    }

    private boolean shouldShieldBreak() {
        for (Entity target : this.targets) {
            if (target instanceof PlayerEntity player
                && DV.of(PlayerUtil.class).blockedByShield(player, this.mc.world.getDamageSources().playerAttack(this.mc.player))
                && this.shieldMode.get() == ShieldMode.Break) {
                return true;
            }
        }

        return false;
    }

    private boolean entityCheck(Entity entity) {
        if (!entity.equals(this.mc.player) && !entity.equals(this.mc.getCameraEntity())) {
            if (!(entity instanceof LivingEntity livingEntity && livingEntity.isDead()) && entity.isAlive()) {
                Box hitbox = entity.getBoundingBox();
                if (!PlayerUtils.isWithin(
                    MathHelper.clamp(this.mc.player.getX(), hitbox.minX, hitbox.maxX),
                    MathHelper.clamp(this.mc.player.getY(), hitbox.minY, hitbox.maxY),
                    MathHelper.clamp(this.mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
                    this.range.get()
                )) {
                    return false;
                } else if (!this.entities.get().contains(entity.getType())) {
                    return false;
                } else if (this.ignoreNamed.get() && entity.hasCustomName()) {
                    return false;
                } else if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, this.wallsRange.get())) {
                    return false;
                } else if (this.ignoreTamed.get()
                    && entity instanceof Tameable tameable
                    && tameable.getOwner() != null
                    && this.mc.player.getUuid().equals(tameable.getOwner().getUuid())) {
                    return false;
                } else {
                    if (this.ignorePassive.get()) {
                        if (entity instanceof EndermanEntity enderman && !enderman.isAngry()) {
                            return false;
                        }

                        if (entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) {
                            return false;
                        }

                        if (entity instanceof WolfEntity wolf && !wolf.isAttacking()) {
                            return false;
                        }
                    }

                    if (entity instanceof PlayerEntity player) {
                        if (player.isCreative()) {
                            return false;
                        }

                        if (!Friends.get().shouldAttack(player)) {
                            return false;
                        }

                        if (this.shieldMode.get() == ShieldMode.Ignore
                            && DV.of(PlayerUtil.class).blockedByShield(player, this.mc.world.getDamageSources().playerAttack(this.mc.player))) {
                            return false;
                        }
                    }

                    if (entity instanceof AnimalEntity animal) {
                        return switch (this.mobAgeFilter.get()) {
                            case Baby -> animal.isBaby();
                            case Adult -> !animal.isBaby();
                            case Both -> true;
                        };
                    } else {
                        return true;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean delayCheck() {
        if (this.switchTimer > 0) {
            this.switchTimer--;
            return false;
        } else {
            float delay = this.customDelay.get() ? this.hitDelay.get().intValue() : 0.5F;
            if (this.tpsSync.get()) {
                delay /= TickRate.INSTANCE.getTickRate() / 20.0F;
            }

            if (this.customDelay.get()) {
                if (this.hitTimer < delay) {
                    this.hitTimer++;
                    return false;
                } else {
                    return true;
                }
            } else {
                return this.mc.player.getAttackCooldownProgress(delay) >= 1.0F;
            }
        }
    }

    private void attack(Entity target) {
        if (this.stopSprint.get()) {
            if (this.mc.player.isSneaking()) {
                DV.of(PacketUtil.class).sendReleaseShift();
            }

            if (this.mc.player.isSprinting()) {
                this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.STOP_SPRINTING));
            }
        }

        Vec3d feetPos = DV.of(PlayerUtil.class).getEntityPos(target);
        Vec3d torsoPos = feetPos.add(0.0, target.getHeight() / 2.0F, 0.0);
        Vec3d eyesPos = target.getEyePos();
        Vec3d hitVec = Stream.of(feetPos, torsoPos, eyesPos).min(Comparator.comparing(pos -> this.mc.player.getEyePos().squaredDistanceTo(pos))).orElse(eyesPos);
        Rotation rotation = new Rotation(hitVec).setPriority(10);
        new Rotation((float) Rotations.getYaw(target), (float) Rotations.getPitch(target, Target.Body));
        RotationManager.getInstance().register(rotation);
        this.mc.interactionManager.attackEntity(this.mc.player, target);
        this.mc.player.swingHand(Hand.MAIN_HAND);
        if (this.packetAttack.get()) {
            this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
            this.mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            this.mc.interactionManager.attackEntity(this.mc.player, target);
            this.mc.player.swingHand(Hand.MAIN_HAND);
        }

        this.hitTimer = 0;
        if (this.rotation.get() == RotationMode.OnHit) {
            RotationManager.getInstance().sync();
        }
    }

    private boolean itemInHand() {
        if (this.shouldShieldBreak()) {
            return this.mc.player.getMainHandStack().getItem() instanceof AxeItem;
        } else {
            return switch (this.weapon.get()) {
                case Sword -> DV.of(ItemUtil.class).isSword(this.mc.player.getMainHandStack().getItem());
                case Axe -> this.mc.player.getMainHandStack().getItem() instanceof AxeItem;
                case Mace -> this.mc.player.getMainHandStack().getItem() instanceof MaceItem;
                case Trident -> this.mc.player.getMainHandStack().getItem() instanceof TridentItem;
                case All -> this.mc.player.getMainHandStack().getItem() instanceof AxeItem
                    || DV.of(ItemUtil.class).isSword(this.mc.player.getMainHandStack().getItem())
                    || this.mc.player.getMainHandStack().getItem() instanceof MaceItem
                    || this.mc.player.getMainHandStack().getItem() instanceof TridentItem;
                default -> true;
            };
        }
    }

    public Entity getTarget() {
        return !this.targets.isEmpty() ? this.targets.getFirst() : null;
    }

    public String getInfoString() {
        return !this.targets.isEmpty() ? EntityUtils.getName(this.getTarget()) : null;
    }

    @Getter
    public enum EntityAge {
        Baby("幼年"),
        Adult("成年"),
        Both("全部");

        private final String displayName;

        EntityAge(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    @Getter
    public enum RotationMode {
        Always("始终旋转"),
        OnHit("攻击时旋转"),
        None("不旋转");

        private final String displayName;

        RotationMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    @Getter
    public enum ShieldMode {
        Ignore("忽略盾牌"),
        Break("破坏盾牌"),
        None("无操作");

        private final String displayName;

        ShieldMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    @Getter
    public enum Weapon {
        Sword("剑"),
        Axe("斧"),
        Mace("锤"),
        Trident("三叉戟"),
        All("全部武器"),
        Any("任意物品");

        private final String displayName;

        Weapon(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
