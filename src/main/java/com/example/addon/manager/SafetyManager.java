package com.example.addon.manager;

import com.example.addon.features.Feature;
import com.example.addon.features.modules.AutoCrystalP;
import com.example.addon.util.BlockUtil;
import com.example.addon.util.DamageUtil;
import com.example.addon.util.EntityUtil;
import com.example.addon.util.Timer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SafetyManager
    extends Feature
    implements Runnable {
    private final Timer syncTimer = new Timer();
    private final AtomicBoolean SAFE = new AtomicBoolean(false);
    private ScheduledExecutorService service;

    private boolean safety = false;
    private int safetyCheck = 50;

    private boolean oneDot15 = false;//test with true

    //class managers

    @Override
    public void run() {
        Modules modules = Modules.get();
        if (modules.get(AutoCrystalP.class).isActive() || AutoCrystalP.getInstance().threadMode.get() == AutoCrystalP.ThreadMode.NONE) {
            this.doSafetyCheck();
        }
    }

    public void doSafetyCheck() {
        if (!SafetyManager.fullNullCheck()) {
            PlayerEntity closest;
            boolean safe = true;
            PlayerEntity entityPlayer = closest = safety != false ? EntityUtil.getClosestEnemy(18.0) : null;
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

    public void onUpdate() {
        this.run();
    }

    public String getSafetyString() {
        if (this.SAFE.get()) {
            return "\u00a7aSecure";
        }
        return "\u00a7cUnsafe";
    }

    public boolean isSafe() {
        return this.SAFE.get();
    }

    public ScheduledExecutorService getService() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this, 0L, safetyCheck, TimeUnit.MILLISECONDS);
        return service;
    }
}
