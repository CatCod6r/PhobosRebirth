package com.example.addon.features;

import com.example.addon.util.Util;
public class Feature implements Util {
    public static boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
}
