package com.example.addon.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

import static com.example.addon.util.Util.mc;

public class NoSoundLag {
    public static void removeEntities(PlaySoundS2CPacket packet, double range) {
        BlockPos pos = new BlockPos((int) packet.getX(), (int) packet.getY(), (int) packet.getZ());
        ArrayList<Entity> toRemove = new ArrayList<Entity>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity) || !(entity.getBlockPos().getSquaredDistance(pos) <= MathHelper.square(range)))
                continue;
            toRemove.add(entity);
        }
        for (Entity entity : toRemove) {
            entity.kill();
        }
    }
}
