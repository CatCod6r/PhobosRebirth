package com.example.addon.util;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

public class DamageUtil
    implements Util {
    public static boolean isArmorLow(PlayerEntity player, int durability) {
        for (ItemStack piece : player.getInventory().armor) {
            if (piece == null) {
                return true;
            }
            if (DamageUtil.getItemDamage(piece) >= durability) continue;
            return true;
        }
        return false;
    }

    public static boolean isNaked(PlayerEntity player) {
        for (ItemStack piece : player.getInventory().armor) {
            if (piece == null || piece.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public static int getItemDamage(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamage();
    }

    public static boolean canBreakWeakness(PlayerEntity player) {
        int strengthAmp = 0;
        StatusEffectInstance effect = player.getStatusEffect(StatusEffects.STRENGTH);
        if (effect != null) {
            strengthAmp = effect.getAmplifier();
        }

        ItemStack mainHandStack = player.getMainHandStack();
        Item mainHandItem = mainHandStack.getItem();

        return !player.hasStatusEffect(StatusEffects.WEAKNESS) ||
            strengthAmp >= 1 ||
            mainHandItem instanceof SwordItem ||
            mainHandItem instanceof PickaxeItem ||
            mainHandItem instanceof AxeItem ||
            mainHandItem instanceof ShovelItem;
    }
    //maybe
    public static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0f;
        double distancedsize = entity.getPos().distanceTo(new Vec3d(posX, posY, posZ)) / (double) doubleExplosionSize;
        double blockDensity = 0.0;
        try {
            blockDensity = BlockUtil.getBlock(new BlockPos((int) posX, (int) posY, (int) posZ)).getBlastResistance();
        } catch (Exception exception) {
            // empty catch block
        }
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * (double) doubleExplosionSize + 1.0);
        double finald = 1.0;
        if (entity instanceof LivingEntity) {
            finald = getBlastReduction((LivingEntity) entity, getDamageMultiplied(damage), new Explosion(mc.world, null, posX, posY, posZ, 6.0f, false, Explosion.DestructionType.DESTROY));
        }
        return (float) finald;
    }

    public static float getBlastReduction(LivingEntity entity, float damage, Explosion explosion) {
        if (entity instanceof PlayerEntity player) {
            DamageSource damageSource = (explosion).getDamageSource();
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) player.getArmor(), (float) player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
            int enchantmentModifier;
            try {
                enchantmentModifier = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), damageSource);
            } catch (Exception exception) {
                enchantmentModifier = 0;
            }
            float protection = MathHelper.clamp((float) enchantmentModifier, 0.0f, 20.0f);
            damage *= 1.0f - protection / 25.0f;

            if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                damage -= damage / 4.0f;
            }

            damage = Math.max(damage, 0.0f);
        } else {
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) entity.getArmor(), (float) entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
        }
        return damage;
    }

    public static float getDamageMultiplied(float damage) {
        int diff = mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }

    public static float calculateDamage(Entity crystal, Entity entity) {
        return DamageUtil.calculateDamage(crystal.getX(), crystal.getY(), crystal.getZ(), entity);
    }

    public static float calculateDamage(BlockPos pos, Entity entity) {
        return DamageUtil.calculateDamage((double) pos.getX() + 0.5, pos.getY() + 1, (double) pos.getZ() + 0.5, entity);
    }

    public static boolean canTakeDamage(boolean suicide) {
        return !mc.player.isCreative() && !suicide;
    }

}
