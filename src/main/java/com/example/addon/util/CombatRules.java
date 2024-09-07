package com.example.addon.util;

import net.minecraft.util.math.MathHelper;

public class CombatRules {
    public static float getDamageAfterAbsorb(float p_getDamageAfterAbsorb_0_, float p_getDamageAfterAbsorb_1_, float p_getDamageAfterAbsorb_2_) {
        float lvt_3_1_ = 2.0F + p_getDamageAfterAbsorb_2_ / 4.0F;
        float lvt_4_1_ = MathHelper.clamp(p_getDamageAfterAbsorb_1_ - p_getDamageAfterAbsorb_0_ / lvt_3_1_, p_getDamageAfterAbsorb_1_ * 0.2F, 20.0F);
        return p_getDamageAfterAbsorb_0_ * (1.0F - lvt_4_1_ / 25.0F);
    }
    public static float getDamageAfterMagicAbsorb(float p_getDamageAfterMagicAbsorb_0_, float p_getDamageAfterMagicAbsorb_1_) {
        float lvt_2_1_ = MathHelper.clamp(p_getDamageAfterMagicAbsorb_1_, 0.0F, 20.0F);
        return p_getDamageAfterMagicAbsorb_0_ * (1.0F - lvt_2_1_ / 25.0F);
    }
}
