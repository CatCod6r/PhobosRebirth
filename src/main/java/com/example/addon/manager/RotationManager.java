package com.example.addon.manager;

import com.example.addon.features.Feature;

public class RotationManager extends Feature {
    private float yaw;
    private float pitch;
    private static float preYaw, prePitch;
//    public void updateRotations() {
//        this.yaw = RotationManager.mc.player.getYaw();
//        this.pitch = RotationManager.mc.player.getPitch();
//    }
//    public void restoreRotations() {
//            RotationManager.mc.player.getYaw() = this.yaw;
//            RotationManager.mc.player.getHeadYaw() = this.yaw;
//            RotationManager.mc.player.getPitch() = this.pitch;
//
//    }

    public void setPlayerRotations(float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setHeadYaw(yaw);
        mc.player.setPitch(pitch);
    }

//    private static void setClientRotation(Rotation rotation) {
//        preYaw = mc.player.getYaw();
//        prePitch = mc.player.getPitch();
//
//        mc.player.setYaw((float) rotation.yaw);
//        mc.player.setPitch((float) rotation.pitch);
//    }
//    private static class Rotation {
//        public double yaw, pitch;
//        public int priority;
//        public boolean clientSide;
//        public Runnable callback;
//
//        public void set(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
//            this.yaw = yaw;
//            this.pitch = pitch;
//            this.priority = priority;
//            this.clientSide = clientSide;
//            this.callback = callback;
//        }
//    }
//    public void setPlayerYaw(float yaw) {
//        RotationManager.mc.player.getYaw() = yaw;
//        RotationManager.mc.player.getHeadYaw() = yaw;
//    }
//    public void setPlayerPitch(float pitch) {
//        RotationManager.mc.player.getPitch() = pitch;
//    }
//
//    public float getYaw() {
//        return this.yaw;
//    }
//
//    public void setYaw(float yaw) {
//        this.yaw = yaw;
//    }
//
//    public float getPitch() {
//        return this.pitch;
//    }
//
//    public void setPitch(float pitch) {
//        this.pitch = pitch;
//    }

//    public int getDirection4D() {
//        return RotationUtil.getDirection4D();
//    }
//
//    public String getDirection4D(boolean northRed) {
//        return RotationUtil.getDirection4D(northRed);
//    }

}
