package com.example.addon.manager;

import com.example.addon.util.Util;

public class RotationManager implements Util {
    public void setPlayerRotations(float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setHeadYaw(yaw);
        mc.player.setPitch(pitch);
    }
}
