package com.example.addon.util;

import net.minecraft.client.MinecraftClient;

public interface Util {
    MinecraftClient mc = MinecraftClient.getInstance();

    static boolean fullCheck() {
        return (mc.player == null || mc.world == null);
    }
}
