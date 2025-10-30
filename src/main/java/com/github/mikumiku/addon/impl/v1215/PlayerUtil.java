package com.github.mikumiku.addon.impl.v1215;

import meteordevelopment.meteorclient.mixin.LivingEntityAccessor;
import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlayerUtil implements com.github.mikumiku.addon.util.PlayerUtil {

    @Override
    public int getSelectedSlot(PlayerInventory playerInventory) {
        return playerInventory.getSelectedSlot();
    }

    @Override
    public void setSelectedSlot(PlayerInventory playerInventory, int selectedSlot) {
        playerInventory.setSelectedSlot(selectedSlot);
    }

    @Override
    public Iterable<ItemStack> getArmor(PlayerInventory playerInventory) {
        return getArmor(playerInventory.player);
    }

    @Override
    public Iterable<ItemStack> getArmor(LivingEntity entity) {
        return EquipmentSlot.VALUES.stream()
            .filter(e -> EquipmentSlot.Type.HUMANOID_ARMOR.equals(e.getType()))
            .map(entity::getEquippedStack).toList();
    }

    @Override
    public float movementForward(Input input) {
        return input.getMovementInput().y;
    }

    @Override
    public float movementSideways(Input input) {
        return input.getMovementInput().x;
    }

    @Override
    public Vec3d getEntityPos(Entity entity) {
        return entity.getPos();
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.getWorld();
    }

    @Override
    public String getGameProfileName(PlayerEntity entity) {
        return entity.getGameProfile().getName();
    }

    @Override
    public boolean blockedByShield(LivingEntity livingEntity, DamageSource source) {
        Entity entity = source.getSource();
        boolean bl = false;
        if (entity instanceof PersistentProjectileEntity persistentProjectileEntity) {
            if (persistentProjectileEntity.getPierceLevel() > 0) {
                bl = true;
            }
        }

        ItemStack itemStack = livingEntity.getBlockingItem();
        if (!source.isIn(DamageTypeTags.BYPASSES_SHIELD) && itemStack != null && itemStack.getItem() instanceof ShieldItem && !bl) {
            Vec3d vec3d = source.getPosition();
            if (vec3d != null) {
                Vec3d vec3d2 = livingEntity.getRotationVector(0.0F, livingEntity.getHeadYaw());
                Vec3d vec3d3 = vec3d.relativize(getEntityPos(livingEntity));
                vec3d3 = (new Vec3d(vec3d3.x, 0.0F, vec3d3.z)).normalize();
                return vec3d3.dotProduct(vec3d2) < (double)0.0F;
            }
        }

        return false;
    }

    @Override
    public void setJumpCooldown(LivingEntity entity, int cooldown) {
        LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
        accessor.setJumpCooldown(0);
    }
}
