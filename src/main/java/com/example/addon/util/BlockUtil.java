package com.example.addon.util;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.stream.Collectors;

public class BlockUtil
    implements Util {
    public static final List<Block> blackList = Arrays.asList(Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.CRAFTING_TABLE, Blocks.ANVIL, Blocks.BREWING_STAND, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER, Blocks.ACACIA_TRAPDOOR
        //needs to be added all trapdoor
        , Blocks.ENCHANTING_TABLE);
    public static final List<Block> shulkerList = Arrays.asList(Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX);
    public static List<Block> unSolidBlocks = Arrays.asList(Blocks.LAVA_CAULDRON , Blocks.FLOWER_POT, Blocks.SNOW, Blocks.YELLOW_CARPET
        //needs to get all of carpets
        , Blocks.END_ROD, Blocks.SKELETON_SKULL,
        //needs to be all skulls
        Blocks.FLOWER_POT, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK, Blocks.OAK_BUTTON,
        //needs to be all buttons
        Blocks.LEVER, Blocks.STONE_BUTTON, Blocks.LADDER, Blocks.COMPARATOR,Blocks.REPEATER, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WIRE, Blocks.AIR, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.WATER, Blocks.WATER_CAULDRON, Blocks.LAVA, Blocks.LAVA_CAULDRON, Blocks.ACACIA_SAPLING
        //needs to be all saplings
        , Blocks.SUNFLOWER, Blocks.FLOWERING_AZALEA, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.PUMPKIN_STEM, Blocks.MELON_STEM, Blocks.LILY_PAD, Blocks.NETHER_WART, Blocks.COCOA, Blocks.CHORUS_FLOWER, Blocks.CHORUS_PLANT, Blocks.GRASS_BLOCK, Blocks.DEAD_BUSH, Blocks.VINE, Blocks.FIRE, Blocks.RAIL, Blocks.ACTIVATOR_RAIL, Blocks.DETECTOR_RAIL, Blocks.POWERED_RAIL, Blocks.TORCH);
        //TODO : All new updates block also

    public static List<BlockPos> getSphere(BlockPos pos, double placeRange, int placeRange2, boolean hollow, boolean sphere, int plus_y) {
        ArrayList<BlockPos> circleBlocks = new ArrayList<>();
        int currX = pos.getX();
        int currY = pos.getY();
        int currZ = pos.getZ();
        int x = currX - (int) placeRange;

        while ((float) x <= (float) currX + placeRange) {
            int z = currZ - (int) placeRange;
            while ((float) z <= (float) currZ + placeRange) {
                int y = sphere ? currY - (int) placeRange : currY;
                while (true) {
                    float f = y;
                    double f2 = sphere ? currY + placeRange : (currY + placeRange2);
                    if (!(f < f2)) break;
                    double dist = (currX - x) * (currX - x) + (currZ - z) * (currZ - z) + (sphere ? (currY - y) * (currY - y) : 0);
                    if (!(!(dist < (placeRange * placeRange)) || hollow && dist < ((placeRange - 1.0f) * (placeRange - 1.0f)))) {
                        BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleBlocks.add(l);
                    }
                    ++y;
                }
                ++z;
            }
            ++x;
        }
        return circleBlocks;
    }

    public static List<BlockPos> possiblePlacePositions(double placeRange, boolean specialEntityCheck, boolean oneDot15) {
        NonNullList positions = NonNullList.create();
        positions.addAll(getSphere(EntityUtil.getPlayerPos(mc.player), placeRange, (int) placeRange, false, true, 0).stream().filter(pos -> canPlaceCrystal(pos, specialEntityCheck, oneDot15)).collect(Collectors.toList()));
        return positions;
    }

    public static boolean canPlaceCrystal(BlockPos blockPos, boolean specialEntityCheck, boolean oneDot15) {
        BlockPos boost = blockPos.add(0, 1, 0);
        BlockPos boost2 = blockPos.add(0, 2, 0);
        try {
            //Check for bedrock or obby
            if (getBlock(blockPos) != Blocks.BEDROCK && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) {
                return false;
            }
            //Check for air above and not 1.15
            if (!oneDot15 && getBlock(boost2) != Blocks.AIR || getBlock(boost) != Blocks.AIR) {
                return false;
            }
            for (Entity entity : mc.world.getOtherEntities(null, new Box(boost))) {
                //Check for dead or special and end crystal
                if (!entity.isAlive() || specialEntityCheck && entity instanceof EndCrystalEntity) continue;
                return false;
            }
            if (!oneDot15) {
                for (Entity entity : mc.world.getOtherEntities(null, new Box(boost))) {
                    if (!entity.isAlive() || specialEntityCheck && entity instanceof EndCrystalEntity) continue;
                    return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).getBlock();
    }

    public static BlockState getState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }

    public static void placeCrystalOnBlock(BlockPos pos, Hand hand, boolean swing, boolean exactHand) {
        BlockHitResult result = mc.world.raycast(
            new RaycastContext(
                new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()),
                new Vec3d(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            )
        );
        Direction facing = result.getSide() == null || result.getSide() == null ? Direction.UP : result.getSide();
        if (mc.player != null) {
            BlockHitResult blockHitResult = new BlockHitResult(mc.player.getPos(), facing, pos, false);
            ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);
            if (actionResult == ActionResult.SUCCESS) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHitResult, 0));
            }
        }
        if (swing) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public static boolean isBlockUnSolid(BlockPos pos) {
        return isBlockUnSolid(getBlock(pos));
    }
    //TODO : update lists
    public static boolean isBlockUnSolid(Block block) {
        return unSolidBlocks.contains(block);
    }

    public static boolean rayTracePlaceCheck(BlockPos pos, boolean shouldCheck, float height) {
        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Vec3d end = new Vec3d(pos.getX(), (float) pos.getY() + height, pos.getZ());
        return !shouldCheck || mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)) == null;
    }
}
