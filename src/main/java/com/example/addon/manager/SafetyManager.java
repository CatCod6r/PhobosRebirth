package com.example.addon.manager;

import com.example.addon.features.modules.AutoCrystalP;
import com.example.addon.util.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class SafetyManager implements Runnable, Util {
    private final AtomicBoolean SAFE = new AtomicBoolean(false);
    private ScheduledExecutorService service;
    private boolean safety = false;
    private boolean oneDot15 = false;

    @Override
    public void run() {
        Modules modules = Modules.get();
        if (modules.get(AutoCrystalP.class).isActive() || AutoCrystalP.getInstance().threadMode.get() == AutoCrystalP.ThreadMode.NONE) {
            this.doSafetyCheck();
        }
    }

    public void doSafetyCheck() {
        if (!(mc.player == null || mc.world == null)) {
            PlayerEntity closest;
            boolean safe = true;
            PlayerEntity entityPlayer = closest = safety ? EntityUtil.getClosestEnemy(18.0) : null;
            if (safety && closest == null) {
                this.SAFE.set(true);
                return;
            }
            ArrayList<Entity> crystals = new ArrayList<>((Collection) mc.world.getEntities());
            for (Entity crystal : crystals) {
                if (!(crystal instanceof EndCrystalEntity) || !((double) DamageUtil.calculateDamage(crystal, mc.player) > 4.0) || closest != null && !(closest.getBlockPos().getSquaredDistance(crystal.getPos()) < 40.0))
                    continue;
                safe = false;
                break;
            }
            if (safe) {
                for (BlockPos pos : BlockUtil.possiblePlacePositions(4.0f, false, oneDot15)) {
                    if (!((double) DamageUtil.calculateDamage(pos, mc.player) > 4.0) || closest != null && !(closest.getBlockPos().getSquaredDistance(pos) < 40.0))
                        continue;
                    safe = false;
                    break;
                }
            }
            this.SAFE.set(safe);
        }
    }
}
