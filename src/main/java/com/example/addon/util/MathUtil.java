package com.example.addon.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class MathUtil extends Util {

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
