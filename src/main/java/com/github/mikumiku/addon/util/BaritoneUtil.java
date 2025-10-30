package com.github.mikumiku.addon.util;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.*;

public class BaritoneUtil {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static final List<Block> FUCk_BLOCKS = Arrays.asList(
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.ENDER_CHEST,
        Blocks.BARREL,
        Blocks.HOPPER,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX,
        Blocks.CRAFTING_TABLE,
        Blocks.ENCHANTING_TABLE,
        Blocks.ANVIL,
        Blocks.CHIPPED_ANVIL,
        Blocks.DAMAGED_ANVIL,
        Blocks.BREWING_STAND,
        Blocks.FURNACE,
        Blocks.BLAST_FURNACE,
        Blocks.SMOKER,
        Blocks.STONECUTTER,
        Blocks.GRINDSTONE,
        Blocks.LOOM,
        Blocks.SMITHING_TABLE,
        Blocks.COMPOSTER,
        Blocks.BEACON,
        Blocks.LECTERN,
        Blocks.BELL,
        Blocks.LEVER,
        Blocks.STONE_BUTTON,
        Blocks.OAK_BUTTON,
        Blocks.SPRUCE_BUTTON,
        Blocks.BIRCH_BUTTON,
        Blocks.JUNGLE_BUTTON,
        Blocks.ACACIA_BUTTON,
        Blocks.DARK_OAK_BUTTON,
        Blocks.MANGROVE_BUTTON,
        Blocks.CHERRY_BUTTON,
        Blocks.BAMBOO_BUTTON,
        Blocks.WARPED_BUTTON,
        Blocks.CRIMSON_BUTTON,
        Blocks.NOTE_BLOCK,
        Blocks.OAK_DOOR,
        Blocks.SPRUCE_DOOR,
        Blocks.BIRCH_DOOR,
        Blocks.JUNGLE_DOOR,
        Blocks.ACACIA_DOOR,
        Blocks.DARK_OAK_DOOR,
        Blocks.MANGROVE_DOOR,
        Blocks.CHERRY_DOOR,
        Blocks.BAMBOO_DOOR,
        Blocks.WARPED_DOOR,
        Blocks.CRIMSON_DOOR,
        Blocks.OAK_TRAPDOOR,
        Blocks.SPRUCE_TRAPDOOR,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.MANGROVE_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.WARPED_TRAPDOOR,
        Blocks.CRIMSON_TRAPDOOR,
        Blocks.SCAFFOLDING,
        Blocks.OAK_SIGN,
        Blocks.OAK_WALL_SIGN,
        Blocks.OAK_HANGING_SIGN,
        Blocks.OAK_WALL_HANGING_SIGN
    );
    public static final List<Block> SNEAK_BLOCKS = Arrays.asList(
        Blocks.ENDER_CHEST,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.CRAFTING_TABLE,
        Blocks.CRAFTER,
        Blocks.JUKEBOX,
        Blocks.DECORATED_POT,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.OAK_TRAPDOOR,
        Blocks.SPRUCE_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR,
        Blocks.WARPED_TRAPDOOR,
        Blocks.CRIMSON_TRAPDOOR,
        Blocks.MANGROVE_TRAPDOOR,
        Blocks.ANVIL,
        Blocks.REPEATER,
        Blocks.COMPARATOR,
        Blocks.CHIPPED_ANVIL,
        Blocks.DAMAGED_ANVIL,
        Blocks.BREWING_STAND,
        Blocks.HOPPER,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.ENCHANTING_TABLE,
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX,
        Blocks.SCAFFOLDING,
        Blocks.LECTERN,
        Blocks.NOTE_BLOCK,
        Blocks.SMITHING_TABLE,
        Blocks.CARTOGRAPHY_TABLE,
        Blocks.BARREL,
        Blocks.BELL,
        Blocks.SWEET_BERRY_BUSH,
        Blocks.POWDER_SNOW_CAULDRON,
        Blocks.CAULDRON,
        Blocks.FURNACE,
        Blocks.BLAST_FURNACE,
        Blocks.SMOKER,
        Blocks.LEVER,
        Blocks.LOOM,
        Blocks.STONECUTTER,
        Blocks.BEACON,
        Blocks.GRINDSTONE
    );
    public static final List<Class> SneakBlockClass = Arrays.asList(
        SignBlock.class, DoorBlock.class, ButtonBlock.class, TrapdoorBlock.class, HangingSignBlock.class, ShulkerBoxBlock.class, WallSignBlock.class
    );

    public static boolean canPlace(BlockPos pos) {
        return getInteractDirection(pos, true) != null;
    }

    public static boolean canPlace(BlockPos pos, boolean strictDirection) {
        return getInteractDirection(pos, strictDirection) != null;
    }

    public static boolean canClick(BlockPos pos) {
        return mc.world.getBlockState(pos).isSolid() && (!SNEAK_BLOCKS.contains(getBlock(pos)) && !(getBlock(pos) instanceof BedBlock) || mc.player.isSneaking());
    }

    public static Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    public static boolean canReplace(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable();
    }

    public static ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean bothIfInRange) {
        ArrayList<Direction> valid = new ArrayList<>();
        if (diff < -0.5) {
            valid.add(negativeSide);
        }

        if (diff > 0.5) {
            valid.add(positiveSide);
        }

        if (bothIfInRange) {
            if (!valid.contains(negativeSide)) {
                valid.add(negativeSide);
            }

            if (!valid.contains(positiveSide)) {
                valid.add(positiveSide);
            }
        }

        return valid;
    }

    public static boolean isStrictDirection(BlockPos pos, Direction side) {
        if (mc.player.getBlockY() - pos.getY() >= 0 && side == Direction.DOWN) {
            return false;
        } else if (side == Direction.UP && pos.getY() + 1 > MeteorClient.mc.player.getEyePos().getY()) {
            return false;
        } else if (getBlock(pos.offset(side)) != Blocks.OBSIDIAN
            && getBlock(pos.offset(side)) != Blocks.BEDROCK
            && getBlock(pos.offset(side)) != Blocks.RESPAWN_ANCHOR) {
            Vec3d eyePos = getEyesPos();
            Vec3d blockCenter = pos.toCenterPos();
            ArrayList<Direction> validAxis = new ArrayList<>();
            validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, false));
            validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
            validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, false));
            return validAxis.contains(side);
        } else {
            return false;
        }
    }

    public static boolean canPlaceWithDis(BlockPos pos, double dis, boolean ignoreCrystal) {
        if (getPlaceSide(pos, dis) == null) {
            return false;
        } else {
            return canReplace(pos) && !hasEntityHere(pos, ignoreCrystal);
        }
    }

    public static boolean hasEntityHere(BlockPos pos, boolean ignoreCrystal) {
        for (Entity entity : getEntities(new Box(pos))) {
            if (entity.isAlive()
                && !(entity instanceof ItemEntity)
                && !(entity instanceof ExperienceOrbEntity)
                && !(entity instanceof ExperienceBottleEntity)
                && !(entity instanceof ArrowEntity)
                && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))) {
                if (entity instanceof ArmorStandEntity) {
                }

                return true;
            }
        }

        return false;
    }

    public static List<Entity> getEntities(Box box) {
        List<Entity> list = new ArrayList<>();

        for (Entity entity : MeteorClient.mc.world.getEntities()) {
            if (entity != null && entity.getBoundingBox().intersects(box)) {
                list.add(entity);
            }
        }

        return list;
    }

    public static Direction getPlaceSide(BlockPos pos, double distance) {
        double dis = 2.147483647E9;
        Direction side = null;

        for (Direction i : Direction.values()) {
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i)) && canSeeBlockFace(pos.offset(i), i.getOpposite())) {
                double vecDis = MeteorClient.mc
                    .player
                    .getEyePos()
                    .squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
                if (!(MathHelper.sqrt((float) vecDis) > distance) && (side == null || vecDis < dis)) {
                    side = i;
                    dis = vecDis;
                }
            }
        }

        return side;
    }

    public static Direction getPlaceSide(BlockPos pos) {
        if (pos == null) {
            return null;
        } else {
            double dis = 114514.0;
            Direction side = null;

            for (Direction i : Direction.values()) {
                if (canClick(pos.offset(i)) && !canReplace(pos.offset(i)) && isStrictDirection(pos.offset(i), i.getOpposite())) {
                    double vecDis = MeteorClient.mc
                        .player
                        .getEyePos()
                        .squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
                    if (side == null || vecDis < dis) {
                        side = i;
                        dis = vecDis;
                    }
                }
            }

            return side;
        }
    }

    public static boolean isSneakBlockClass(Block block) {
        if (block == null) {
            return false;
        } else {
            for (Class clazz : SneakBlockClass) {
                if (clazz.isInstance(block)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean canPlaceIf(BlockPos pos, boolean strictDirection, Direction direction) {
        return getInteractDirectionIf(pos, strictDirection, direction) != null;
    }

    public static boolean placeBlock(BlockPos pos) {
        return placeBlock(pos, true, true, true);
    }

    public static boolean placeBlock(BlockPos pos, Item item) {
        int slot = BagUtil.findItemInventorySlot(item);
        if (slot == -1) {
            return false;
        } else if (mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) > 36.0) {
            return false;
        } else {
            BagUtil.doSwap(slot);
            boolean placed = placeBlock(pos, true, true, true);
            BagUtil.doSwap(slot);
            return placed;
        }
    }

    public static boolean placeBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
        Direction direction = getInteractDirection(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeBlock(neighbor, direction, clientSwing, rotate);
        }
    }

    public static boolean placeUpBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeUpBlock(neighbor, direction, clientSwing, rotate);
        }
    }

    public static boolean placeDownBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
        if (direction == null) {
            return false;
        } else if (!canSeeBlockFace(pos, direction)) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeDownBlock(neighbor, direction, clientSwing, rotate);
        }
    }

    public static boolean canSeeBlockFace(BlockPos pos, Direction side) {
        if (side == null) {
            return false;
        } else {
            Vec3d testVec = pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
            HitResult result = mc.world.raycast(new RaycastContext(getEyesPos(), testVec, ShapeType.COLLIDER, FluidHandling.NONE, MeteorClient.mc.player));
            return result == null || result.getType() == Type.MISS;
        }
    }

    public static Vec3d getEyesPos() {
        return mc.player.getEyePos();
    }

    public static boolean placeBlockByFaceDirection(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate, Direction faceDirection) {
        Direction direction = getInteractDirection(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeBlockByFaceDirection(pos, neighbor, direction, clientSwing, rotate, faceDirection);
        }
    }

    public static boolean placeUpBlockByFaceDirection(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate, Direction placementDirection) {
        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeUpBlockByFaceDirection(pos, neighbor, direction, clientSwing, rotate, placementDirection);
        }
    }

    public static boolean placeBlockByFaceDirection(
        BlockPos initPos, BlockPos pos, Direction direction, boolean clientSwing, boolean rotate, Direction faceDirection
    ) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5));
        if (rotate) {
            Rotation rotation = new Rotation(hitVec).setPriority(10);
            RotationManager.getInstance().register(rotation);
            rotation.setYaw(getDirectionYaw(faceDirection));
            rotation.setPitch(5.0F);
            boolean rot = RotationManager.getInstance().register(rotation);
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    public static boolean placeUpBlockByFaceDirection(
        BlockPos initPos, BlockPos pos, Direction direction, boolean clientSwing, boolean rotate, Direction faceDirection
    ) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5)).add(0.0, 0.3, 0.0);
        if (rotate) {
            Rotation rotation = new Rotation(hitVec).setPriority(10);
            RotationManager.getInstance().register(rotation);
            rotation.setYaw(getDirectionYaw(faceDirection));
            rotation.setPitch(5.0F);
            boolean rotated = RotationManager.getInstance().register(rotation);
            if (!rotated) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    public static float getDirectionYaw(Direction direction) {
        if (direction == null) {
            return 0.0F;
        } else {
            switch (direction) {
                case NORTH:
                    return 180.0F;
                case SOUTH:
                    return 0.0F;
                case WEST:
                    return 90.0F;
                case EAST:
                    return -90.0F;
                default:
                    return 0.0F;
            }
        }
    }

    public static boolean placeBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5));
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(hitVec).setPriority(10));
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    public static boolean placeUpBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
        Vec3d hitVec = pos.toCenterPos().add(0.0, 0.3, 0.0);
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(hitVec).setPriority(10));
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    public static boolean placeDownBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
        Vec3d hitVec = pos.toCenterPos().add(0.0, -0.2, 0.0);
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(hitVec).setPriority(10));
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    public static boolean placeBlock(BlockHitResult hitResult, boolean clientSwing) {
        return placeBlockImmediately(hitResult, clientSwing);
    }

    public static boolean placeBlockImmediately(BlockHitResult result, boolean clientSwing) {
        BlockState state = mc.world.getBlockState(result.getBlockPos());
        boolean shouldSneak = (SNEAK_BLOCKS.contains(state.getBlock()) || isSneakBlockClass(mc.world.getBlockState(result.getBlockPos()).getBlock()))
            && !mc.player.isSneaking();
        if (shouldSneak) {
            DV.of(PacketUtil.class).sendPressShift();
        }

        ActionResult actionResult = placeBlockInternally(result);
        if (actionResult.isAccepted()) {
            if (clientSwing) {
                mc.player.swingHand(Hand.MAIN_HAND);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        if (shouldSneak) {
            DV.of(PacketUtil.class).sendReleaseShift();
        }

        return actionResult.isAccepted();
    }

    private static ActionResult placeBlockInternally(BlockHitResult hitResult) {
        return mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    public static Direction getInteractDirection(BlockPos blockPos, boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if (!state.isAir() && state.getFluidState().isEmpty() && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
                interactDirection = direction;
                break;
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    public static Direction getInteractDirectionExitUpDown(BlockPos blockPos, boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if (!state.isAir()
                && state.getFluidState().isEmpty()
                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))
                && direction != Direction.UP
                && direction != Direction.DOWN) {
                interactDirection = direction;
                break;
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    public static Direction getInteractDirectionIf(BlockPos blockPos, boolean strictDirection, Direction direction_) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if ((!state.isAir() && state.getFluidState().isEmpty() || direction == direction_)
                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
                interactDirection = direction;
                break;
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    public static Direction getInteractDirectionSlabBlock(BlockPos blockPos, boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && direction != Direction.DOWN) {
                BlockState state = mc.world.getBlockState(blockPos.offset(direction));
                if (!state.isAir() && state.getFluidState().isEmpty() && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
                    interactDirection = direction;
                    break;
                }
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    public static Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
        return getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
    }

    public static Set<Direction> getPlaceDirectionsNCP(double x, double y, double z, double dx, double dy, double dz) {
        double xdiff = x - dx;
        double ydiff = y - dy;
        double zdiff = z - dz;
        Set<Direction> dirs = new HashSet<>(6);
        if (ydiff > 0.5) {
            dirs.add(Direction.UP);
        } else if (ydiff < -0.5) {
            dirs.add(Direction.DOWN);
        } else {
            dirs.add(Direction.UP);
            dirs.add(Direction.DOWN);
        }

        if (xdiff > 0.5) {
            dirs.add(Direction.EAST);
        } else if (xdiff < -0.5) {
            dirs.add(Direction.WEST);
        } else {
            dirs.add(Direction.EAST);
            dirs.add(Direction.WEST);
        }

        if (zdiff > 0.5) {
            dirs.add(Direction.SOUTH);
        } else if (zdiff < -0.5) {
            dirs.add(Direction.NORTH);
        } else {
            dirs.add(Direction.SOUTH);
            dirs.add(Direction.NORTH);
        }

        return dirs;
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, SwingSide swingSide) {
        Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
        );
        swingHand(hand, swingSide);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(directionVec).setPriority(10));
            if (!rot) {
                return;
            }
        }

        BaseModule.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
        RotationManager.getInstance().sync();
    }

    public static void swingHand(Hand hand, SwingSide side) {
        switch (side) {
            case All:
                MeteorClient.mc.player.swingHand(hand);
                break;
            case Client:
                MeteorClient.mc.player.swingHand(hand, false);
                break;
            case Server:
                MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public static boolean intersectsEntity(BlockPos pos) {
        if (pos == null) {
            return true;
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)
                    && (
                    entity.getBoundingBox().intersects(new Box(pos)) && entity.isOnGround()
                        || entity instanceof ItemEntity && entity.getBoundingBox().intersects(new Box(pos.up()))
                )) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean intersectsAnyEntity(BlockPos pos) {
        if (pos == null) {
            return true;
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (entity.getBoundingBox().intersects(new Box(pos))) {
                    return true;
                }
            }

            return false;
        }
    }

    public static Direction getPlaceDirection(BlockPos pos, boolean ignoreContainers) {
        if (pos == null) {
            return null;
        } else {
            Direction best = null;
            if (mc.world != null && mc.player != null) {
                double cDist = -1.0;

                for (Direction dir : Direction.values()) {
                    if (pos.offset(dir).getY() < 319 && (!ignoreContainers || !mc.world.getBlockState(pos.offset(dir)).hasBlockEntity())) {
                        Block b = mc.world.getBlockState(pos.offset(dir)).getBlock();
                        if (!(b instanceof AbstractFireBlock) && !(b instanceof FluidBlock) && !(b instanceof AirBlock)) {
                            double dist = PlayerUtils.distanceTo(pos.offset(dir));
                            if (dist >= 0.0 && (cDist < 0.0 || dist < cDist)) {
                                best = dir;
                                cDist = dist;
                            }
                        }
                    }
                }
            }

            return best;
        }
    }

    public static Direction getPlaceOnDirection(BlockPos pos) {
        if (pos == null) {
            return null;
        } else {
            Direction best = null;
            if (mc.world != null && mc.player != null) {
                double cDist = -1.0;

                for (Direction dir : Direction.values()) {
                    if (pos.offset(dir).getY() < 319) {
                        Block b = mc.world.getBlockState(pos.offset(dir)).getBlock();
                        if (b instanceof AbstractFireBlock || b instanceof FluidBlock || b instanceof AirBlock) {
                            double dist = mc.player.getEyePos().distanceTo(pos.offset(dir).toCenterPos());
                            if (dist >= 0.0 && (cDist < 0.0 || dist < cDist)) {
                                best = dir;
                                cDist = dist;
                            }
                        }
                    }
                }
            }

            return best;
        }
    }

    public enum SwingSide {
        All,
        Client,
        Server,
        None
    }
}
