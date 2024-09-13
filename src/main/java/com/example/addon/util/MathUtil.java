package com.example.addon.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class MathUtil extends Util {

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.FLOOR);
        return bd.doubleValue();
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean descending) {
        LinkedList<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        if (descending) {
            list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        } else {
            list.sort(Map.Entry.comparingByValue());
        }
        LinkedHashMap result = new LinkedHashMap();
        for (Map.Entry entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static List<Vec3d> getBlockBlocks(Entity entity) {
        ArrayList<Vec3d> vec3ds = new ArrayList<Vec3d>();
        Box bb = entity.getBoundingBox();
        double y = entity.getY();
        double minX = MathUtil.round(bb.minX, 0);
        double minZ = MathUtil.round(bb.minZ, 0);
        double maxX = MathUtil.round(bb.maxX, 0);
        double maxZ = MathUtil.round(bb.maxZ, 0);
        if (minX != maxX) {
            Vec3d vec3d1 = new Vec3d(minX, y, minZ);
            Vec3d vec3d2 = new Vec3d(maxX, y, minZ);
            BlockPos pos1 = new BlockPos((int) vec3d1.getX(), (int) vec3d1.getY(), (int) vec3d1.getZ());
            BlockPos pos2 = new BlockPos((int) vec3d2.getX(), (int) vec3d2.getY(), (int) vec3d2.getZ());
            if (BlockUtil.isBlockUnSolid(pos1) && BlockUtil.isBlockUnSolid(pos2)) {
                vec3ds.add(vec3d1);
                vec3ds.add(vec3d2);
            }
            if (minZ != maxZ) {
                Vec3d vec3d3 = new Vec3d(minX, y, maxZ);
                Vec3d vec3d4 = new Vec3d(maxX, y, maxZ);
                BlockPos pos3 = new BlockPos((int) vec3d1.getX(), (int) vec3d1.getY(), (int) vec3d1.getZ());
                BlockPos pos4 = new BlockPos((int) vec3d2.getX(), (int) vec3d2.getY(), (int) vec3d2.getZ());
                if (BlockUtil.isBlockUnSolid(pos3) && BlockUtil.isBlockUnSolid(pos4)) {
                    vec3ds.add(vec3d3);
                    vec3ds.add(vec3d4);
                    return vec3ds;
                }
            }
            if (vec3ds.isEmpty()) {
                vec3ds.add(entity.getPos());
            }
            return vec3ds;
        }
        if (minZ != maxZ) {
            Vec3d vec3d1 = new Vec3d(minX, y, minZ);
            Vec3d vec3d2 = new Vec3d(minX, y, maxZ);
            BlockPos pos1 = new BlockPos((int) vec3d1.getX(), (int) vec3d1.getY(), (int) vec3d1.getZ());
            BlockPos pos2 = new BlockPos((int) vec3d2.getX(), (int) vec3d2.getY(), (int) vec3d2.getZ());
            if (BlockUtil.isBlockUnSolid(pos1) && BlockUtil.isBlockUnSolid(pos2)) {
                vec3ds.add(vec3d1);
                vec3ds.add(vec3d2);
            }
            if (vec3ds.isEmpty()) {
                vec3ds.add(entity.getPos());
            }
            return vec3ds;
        }
        vec3ds.add(entity.getPos());
        return vec3ds;
    }

    public static float[] calcAngle(Vec3d from, Vec3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{(float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))};
    }

    public static Vec3d calculateLine(Vec3d x1, Vec3d x2, double distance) {
        double length = Math.sqrt(MathHelper.square(x2.x - x1.x) + MathHelper.square(x2.y - x1.y) + MathHelper.square(x2.z - x1.z));
        double unitSlopeX = (x2.x - x1.x) / length;
        double unitSlopeY = (x2.y - x1.y) / length;
        double unitSlopeZ = (x2.z - x1.z) / length;
        double x = x1.x + unitSlopeX * distance;
        double y = x1.y + unitSlopeY * distance;
        double z = x1.z + unitSlopeZ * distance;
        return new Vec3d(x, y, z);
    }

    public static Vec3d extrapolatePlayerPosition(PlayerEntity player, int ticks) {
        Vec3d lastPos = new Vec3d(player.lastRenderX, player.lastRenderY, player.lastRenderZ);
        Vec3d currentPos = player.getPos();
        Vec3d velocity = player.getVelocity();
        double distance = MathHelper.square(velocity.x) + MathHelper.square(velocity.y) + MathHelper.square(velocity.z);
        Vec3d tempVec = MathUtil.calculateLine(lastPos, currentPos, distance * ticks);
        return new Vec3d(tempVec.x, player.getY(), tempVec.z);
    }
}
