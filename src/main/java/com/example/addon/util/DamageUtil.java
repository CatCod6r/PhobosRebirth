package com.example.addon.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
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

    public static float getDamageInPercent(ItemStack stack) {
        return (float) DamageUtil.getItemDamage(stack) / (float) stack.getMaxDamage() * 100.0f;
    }

    public static int getRoundedDamage(ItemStack stack) {
        return (int) DamageUtil.getDamageInPercent(stack);
    }

    public static boolean hasDurability(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ArmorItem || item instanceof SwordItem || item instanceof ToolItem || item instanceof ShieldItem;
    }

    public static boolean canBreakWeakness(PlayerEntity player) {
        int strengthAmp = 0;
        StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        if (effect != null) {
            strengthAmp = effect.getAmplifier();
        }

        ItemStack mainHandStack = mc.player.getMainHandStack();
        Item mainHandItem = mainHandStack.getItem();

        return !mc.player.hasStatusEffect(StatusEffects.WEAKNESS) ||
            strengthAmp >= 1 ||
            mainHandItem instanceof SwordItem ||
            mainHandItem instanceof PickaxeItem ||
            mainHandItem instanceof AxeItem ||
            mainHandItem instanceof ShovelItem;
    }


        public static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0f;
        double distancedsize = entity.getPos().distanceTo(new Vec3d(posX, posY, posZ)) / doubleExplosionSize;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = 0.0;
//        try {
//            blockDensity = mc.world.canCollide (entity.getPos(), entity.getBoundingBox());
//        } catch (Exception exception) {
//            // empty catch block
//        }
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * (double) doubleExplosionSize + 1.0);
        double finald = 1.0;
        if (entity instanceof LivingEntity) {
            finald = DamageUtil.getBlastReduction((LivingEntity) entity, DamageUtil.getDamageMultiplied(damage), new Explosion(DamageUtil.mc.world, null, posX, posY, posZ, 6.0f, false, Explosion.DestructionType.DESTROY));
        }
        return (float) finald;
    }
//    public static RayTraceResult rayTraceBlocks(Vec3d start,
//                                                Vec3d end,
//                                                BlockView world,
//                                                boolean stopOnLiquid,
//                                                boolean ignoreNoBox,
//                                                boolean lastUncollidableBlock,
//                                                boolean ignoreWebs,
//                                                boolean ignoreBeds,
//                                                boolean terrainCalc,
//                                                boolean anvils) {
//
//        ClientPlayerEntity minecraftWorld = MinecraftClient.getInstance().player;
//
//        return new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, minecraftWorld)
//            .getBlockHitResult();
//    }
//    public static float getBlockDensity(Vec3d vec,
//                                        Box bb)
//    {
//        double x = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
//        double y = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
//        double z = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
//        double xFloor = (1.0 - Math.floor(1.0 / x) * x) / 2.0;
//        double zFloor = (1.0 - Math.floor(1.0 / z) * z) / 2.0;
//
//        if (x >= 0.0D && y >= 0.0D && z >= 0.0D)
//        {
//            int air = 0;
//            int traced = 0;
//
//            for (float a = 0.0F; a <= 1.0F; a = (float) (a + x))
//            {
//                for (float b = 0.0F; b <= 1.0F; b = (float) (b + y))
//                {
//                    for (float c = 0.0F; c <= 1.0F; c = (float) (c + z))
//                    {
//                        double xOff = bb.minX + (bb.maxX - bb.minX) * a;
//                        double yOff = bb.minY + (bb.maxY - bb.minY) * b;
//                        double zOff = bb.minZ + (bb.maxZ - bb.minZ) * c;
//
//                        RayTraceResult result = rayTraceBlocks(
//                            new Vec3d(xOff + xFloor, yOff, zOff + zFloor),
//                            vec,
//                            false,
//                            false,
//                            false);
//
//                        if (result == null)
//                        {
//                            air++;
//                        }
//
//                        traced++;
//                    }
//                }
//            }
//
//            return (float) air / (float) traced;
//        }
//        else
//        {
//            return 0.0F;
//        }
//    }
    public static float getBlastReduction(LivingEntity entity, float damage, Explosion explosion) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            Explosion explosion1 = explosion;
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
            return damage;
        } else {
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) entity.getArmor(), (float) entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
            return damage;
        }
    }

    public static float getDamageMultiplied(float damage) {
        int diff = DamageUtil.mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }

    public static float calculateDamage(Entity crystal, Entity entity) {
        return DamageUtil.calculateDamage(crystal.getX(), crystal.getY(), crystal.getZ(), entity);
    }

    public static float calculateDamage(BlockPos pos, Entity entity) {
        return DamageUtil.calculateDamage((double) pos.getX() + 0.5, pos.getY() + 1, (double) pos.getZ() + 0.5, entity);
    }

    public static boolean canTakeDamage(boolean suicide) {
        return !DamageUtil.mc.player.isCreative() && !suicide;
    }

    public static int getCooldownByWeapon(PlayerEntity player) {
        Item item = player.getMainHandStack().getItem();
        if (item instanceof SwordItem) {
            return 600;
        }
        if (item instanceof PickaxeItem) {
            return 850;
        }
        if (item == Items.IRON_AXE) {
            return 1100;
        }
        if (item == Items.STONE_HOE) {
            return 500;
        }
        if (item == Items.IRON_HOE) {
            return 350;
        }
        if (item == Items.WOODEN_AXE || item == Items.STONE_AXE) {
            return 1250;
        }
        if (item instanceof ShovelItem || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE || item == Items.WOODEN_HOE || item == Items.GOLDEN_HOE) {
            return 1000;
        }
        return 250;
    }
}
