package com.example.addon.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class RayTraceResult {
    public int subHit;
    public Object hitInfo;
    private BlockPos blockPos;
    public Type typeOfHit;
    public Direction sideHit;
    public Vec3d hitVec;
    public Entity entityHit;



    public RayTraceResult(Vec3d p_i47094_1_, Direction p_i47094_2_, BlockPos p_i47094_3_) {
        this(RayTraceResult.Type.BLOCK, p_i47094_1_, p_i47094_2_, p_i47094_3_);
    }

    public RayTraceResult(Vec3d p_i47095_1_, Direction p_i47095_2_) {
        this(RayTraceResult.Type.BLOCK, p_i47095_1_, p_i47095_2_, BlockPos.ORIGIN);
    }

    public RayTraceResult(Entity p_i2304_1_) {
        this(p_i2304_1_, new Vec3d(p_i2304_1_.getX(), p_i2304_1_.getY(), p_i2304_1_.getZ()));
    }
    public RayTraceResult(Type p_i47096_1_, Vec3d p_i47096_2_, Direction p_i47096_3_, BlockPos p_i47096_4_) {
        this.subHit = -1;
        this.hitInfo = null;
        this.typeOfHit = p_i47096_1_;
        this.blockPos = p_i47096_4_;
        this.sideHit = p_i47096_3_;
        this.hitVec = new Vec3d(p_i47096_2_.x, p_i47096_2_.y, p_i47096_2_.z);
    }

    public RayTraceResult(Entity p_i47097_1_, Vec3d p_i47097_2_) {
        this.subHit = -1;
        this.hitInfo = null;
        this.typeOfHit = RayTraceResult.Type.ENTITY;
        this.entityHit = p_i47097_1_;
        this.hitVec = p_i47097_2_;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public String toString() {
        return "HitResult{type=" + this.typeOfHit + ", blockpos=" + this.blockPos + ", f=" + this.sideHit + ", pos=" + this.hitVec + ", entity=" + this.entityHit + '}';
    }

    public static enum Type {
        MISS,
        BLOCK,
        ENTITY;

        private Type() {
        }
    }
}
