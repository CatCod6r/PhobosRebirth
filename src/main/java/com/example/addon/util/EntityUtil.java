package com.example.addon.util;

import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.*;

public class EntityUtil implements Util {
    public static final Vec3d[] antiDropOffsetList;
    public static final Vec3d[] platformOffsetList;
    public static final Vec3d[] legOffsetList;
    public static final Vec3d[] doubleLegOffsetList;
    public static final Vec3d[] OffsetList;
    public static final Vec3d[] headpiece;
    public static final Vec3d[] offsetsNoHead;
    public static final Vec3d[] antiStepOffsetList;
    public static final Vec3d[] antiScaffoldOffsetList;

    static {
        antiDropOffsetList = new Vec3d[]{new Vec3d(0.0, -2.0, 0.0)};
        platformOffsetList = new Vec3d[]{new Vec3d(0.0, -1.0, 0.0), new Vec3d(0.0, -1.0, -1.0), new Vec3d(0.0, -1.0, 1.0), new Vec3d(-1.0, -1.0, 0.0), new Vec3d(1.0, -1.0, 0.0)};
        legOffsetList = new Vec3d[]{new Vec3d(-1.0, 0.0, 0.0), new Vec3d(1.0, 0.0, 0.0), new Vec3d(0.0, 0.0, -1.0), new Vec3d(0.0, 0.0, 1.0)};
        doubleLegOffsetList = new Vec3d[]{new Vec3d(-1.0, 0.0, 0.0), new Vec3d(1.0, 0.0, 0.0), new Vec3d(0.0, 0.0, -1.0), new Vec3d(0.0, 0.0, 1.0), new Vec3d(-2.0, 0.0, 0.0), new Vec3d(2.0, 0.0, 0.0), new Vec3d(0.0, 0.0, -2.0), new Vec3d(0.0, 0.0, 2.0)};
        OffsetList = new Vec3d[]{new Vec3d(1.0, 1.0, 0.0), new Vec3d(-1.0, 1.0, 0.0), new Vec3d(0.0, 1.0, 1.0), new Vec3d(0.0, 1.0, -1.0), new Vec3d(0.0, 2.0, 0.0)};
        headpiece = new Vec3d[]{new Vec3d(0.0, 2.0, 0.0)};
        offsetsNoHead = new Vec3d[]{new Vec3d(1.0, 1.0, 0.0), new Vec3d(-1.0, 1.0, 0.0), new Vec3d(0.0, 1.0, 1.0), new Vec3d(0.0, 1.0, -1.0)};
        antiStepOffsetList = new Vec3d[]{new Vec3d(-1.0, 2.0, 0.0), new Vec3d(1.0, 2.0, 0.0), new Vec3d(0.0, 2.0, 1.0), new Vec3d(0.0, 2.0, -1.0)};
        antiScaffoldOffsetList = new Vec3d[]{new Vec3d(0.0, 3.0, 0.0)};
    }

    public static void attackEntity(Entity entity, boolean packet, boolean swingArm) {
        if (packet) {
            PlayerInteractEntityC2SPacket interactPacket = PlayerInteractEntityC2SPacket.attack(entity, true);
            mc.getNetworkHandler().sendPacket(interactPacket);
        } else {
            mc.player.attack(entity);
        }
        if (swingArm) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static boolean isSafe(Entity entity, int height, boolean floor,boolean face) {
        return getUnsafeBlocks(entity, height, floor, face).isEmpty();
    }
    public static boolean isSafe(Entity entity) {
        return isSafe(entity, 0, false, true);
    }

    public static BlockPos getPlayerPos(ClientPlayerEntity player) {
        return new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
    }

    public static List<Vec3d> getUnsafeBlocks(Entity entity, int height, boolean floor, boolean face) {
        return getUnsafeBlocksFromVec3d(entity.getPos(), height, floor, face);
    }

    public static List<Vec3d> getUnsafeBlocksFromVec3d(Vec3d pos, int height, boolean floor, boolean face) {
       List<Vec3d> vec3ds = new ArrayList<>();
        for (final Vec3d vector : getOffsets(height, floor, face)) {
            BlockPos targetPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z).add((int) vector.x, (int) vector.y, (int) vector.z);
           Block block = BlockUtil.getBlock(targetPos);
            if (block instanceof AirBlock || block instanceof FluidBlock || block instanceof TallFlowerBlock || block instanceof FireBlock || block instanceof DeadBushBlock || block instanceof SnowBlock) {
                vec3ds.add(vector);
            }
        }
        return vec3ds;
    }

    public static boolean isCrystalAtFeet(EndCrystalEntity crystal, double range) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (mc.player.squaredDistanceTo(player) > range * range) {
                continue;
            }
            if (Friends.get().isFriend(player)) {
                continue;
            }
            for (Vec3d vec : EntityUtil.doubleLegOffsetList) {
                BlockPos playerPos = new BlockPos(player.getBlockPos()).add((int) vec.x, (int) vec.y, (int) vec.z);
                if (playerPos.equals(crystal.getBlockPos())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Vec3d> getOffsetList(int y, boolean floor, boolean face) {
       List<Vec3d> offsets = new ArrayList<>();
        if (face) {
            offsets.add(new Vec3d(-1.0, y, 0.0));
            offsets.add(new Vec3d(1.0, y, 0.0));
            offsets.add(new Vec3d(0.0, y, -1.0));
            offsets.add(new Vec3d(0.0, y, 1.0));
        } else {
            offsets.add(new Vec3d(-1.0, y, 0.0));
        }
        if (floor) {
            offsets.add(new Vec3d(0.0, y - 1, 0.0));
        }
        return offsets;
    }

    public static Vec3d[] getOffsets(int y, boolean floor, boolean face) {
       List<Vec3d> offsets = getOffsetList(y, floor, face);
       Vec3d[] array = new Vec3d[offsets.size()];
        return offsets.toArray(array);
    }

    public static boolean isLiving(Entity entity) {
        return entity instanceof LivingEntity;
    }

    public static float getHealth(Entity entity) {
        if (isLiving(entity)) {
           LivingEntity livingBase = (LivingEntity) entity;
            return livingBase.getHealth() + livingBase.getAbsorptionAmount();
        }
        return 0.0f;
    }

    public static float getHealth(Entity entity, boolean absorption) {
        if (isLiving(entity)) {
           LivingEntity livingBase = (LivingEntity) entity;
            return livingBase.getHealth() + (absorption ? livingBase.getAbsorptionAmount() : 0.0f);
        }
        return 0.0f;
    }

    public static boolean isntValid(Entity entity, double range) {
        return entity == null || !entity.isAlive() || entity.equals(mc.player) || (entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) || mc.player.getBlockPos().getSquaredDistance(entity.getPos()) > MathHelper.square(range);
    }

    public static boolean isValid(Entity entity, double range) {
        return !isntValid(entity, range);
    }

    public static PlayerEntity getClosestEnemy(double distance) {
        PlayerEntity closest = null;
        for (final PlayerEntity player : mc.world.getPlayers()) {
            if (isntValid(player, distance)) {
                continue;
            }
            if (closest != null) {
                if (mc.player.getBlockPos().getSquaredDistance(player.getPos()) >= mc.player.getBlockPos().getSquaredDistance(closest.getPos())) {
                    continue;
                }
            }
            closest = player;
        }
        return closest;
    }
    //TODO : rewrite
    public static void swingArmNoPacket(Hand hand, ClientPlayerEntity entity) {
        final ItemStack stack = entity.getActiveItem();
        if (!stack.isEmpty()) {
            return;
        }
        if (!entity.handSwinging || entity.handSwingProgress >= entity.lastHandSwingProgress  / 2 || entity.handSwingProgress < 0) {
            entity.handSwingProgress = -1;
            entity.handSwinging = true;
            entity.swingHand(hand);
        }
    }
}
