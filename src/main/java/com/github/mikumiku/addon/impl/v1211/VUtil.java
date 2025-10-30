package com.github.mikumiku.addon.impl.v1211;

import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

public class VUtil implements com.github.mikumiku.addon.util.VUtil {

    @Override
    public ItemStack getEnchantedBookWith(Optional<RegistryEntry.Reference<Enchantment>> en) {
        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
    }

    @Override
    public Registry<Enchantment> getEnchantmentRegistry() {
        DynamicRegistryManager registryManager = MinecraftClient.getInstance().world.getRegistryManager();
        return registryManager.get(RegistryKeys.ENCHANTMENT);
    }

    @Override
    public PlayerMoveC2SPacket.LookAndOnGround get(float currentYaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.LookAndOnGround(currentYaw, pitch, onGround);
    }

    @Override
    public PlayerMoveC2SPacket.Full getFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.Full(
            x,
            y,
            z,
            yaw,
            pitch,
            onGround
        );
    }

    @Override
    public boolean isFallFlying(MinecraftClient mc) {
        return mc.player.isFallFlying();
    }

    @Override
    public boolean isJumping(MinecraftClient mc) {
        return mc.player.input.jumping;
    }

    @Override
    public boolean isSneaking(MinecraftClient mc) {
        return mc.player.input.sneaking;

    }

    @Override
    public Direction getOppositeDirectionTo(BlockPos blockPos) {
        return Direction.fromRotation(Rotations.getYaw(blockPos)).getOpposite();
    }

    @Override
    public double getToughness(LivingEntity entity) {
        return entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
    }

    @Override
    public void setRaycast(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player) {
        raycastContext.set(source, vec3d, shapeType, fluidHandling, player);
    }

    @Override
    public void setMovement(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player) {
        raycastContext.set(source, vec3d, shapeType, fluidHandling, player);
    }

    @Override
    public void setMovement(IVec3d movement, double x, double y, double z) {
        movement.set(x, y, z);
    }

    @Override
    public PlayerMoveC2SPacket.PositionAndOnGround getPositionAndOnGround(double x, double y, double z, boolean onGround) {
        return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround);
    }

    @Override
    public PlayerMoveC2SPacket.OnGroundOnly getOnGroundOnly(boolean onGround) {
        return new PlayerMoveC2SPacket.OnGroundOnly(onGround);
    }

    @Override
    public int getTopY(MinecraftClient mc) {
        return mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, (int) mc.player.getX(), (int) mc.player.getZ());
    }

}
