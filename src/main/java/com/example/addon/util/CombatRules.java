package com.example.addon.util;

import net.minecraft.util.math.MathHelper;

public class CombatRules {
    public static float getDamageAfterAbsorb(float damage, float armorValue, float armorToughness) {
        float lvt_3_1_ = 2.0F + armorToughness / 4.0F;
        float lvt_4_1_ = MathHelper.clamp(armorValue - damage / lvt_3_1_, armorValue * 0.2F, 20.0F);
        return damage * (1.0F - lvt_4_1_ / 25.0F);
    }
}
