package com.example.addon.util;

import com.example.addon.mixin.mixins.MixinBlockLiquid;
import com.google.common.util.concurrent.AtomicDouble;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.example.addon.manager.Managers.rotationManager;

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
//All new updates block also
    public static List<BlockPos> getBlockSphere(float breakRange, Class clazz) {
        NonNullList positions = NonNullList.create();
        positions.addAll(BlockUtil.getSphere(EntityUtil.getPlayerPos(BlockUtil.mc.player), breakRange, (int) breakRange, false, true, 0).stream().filter(pos -> clazz.isInstance(BlockUtil.mc.world.getBlockState(pos).getBlock())).collect(Collectors.toList()));
        return positions;
    }
    public static RayTraceResult objectMouseOver;
//    public static Direction getFacing(BlockPos pos) {
//        for (Direction facing : Direction.values()) {
//            RayTraceResult rayTraceResult = BlockUtil.mc.world.getBlockState().rayTraceBlocks(new Vec3d(BlockUtil.mc.player.getX(), BlockUtil.mc.player.getY() + (double) BlockUtil.mc.player.getEyeHeight(), BlockUtil.mc.player.getZ()), new Vec3d((double) pos.getX() + 0.5 + (double) facing.getVector().getX() * 1.0 / 2.0, (double) pos.getY() + 0.5 + (double) facing.getVector().getY() * 1.0 / 2.0, (double) pos.getZ() + 0.5 + (double) facing.getVector().getZ() * 1.0 / 2.0), false, true, false);
//            if (rayTraceResult != null && (rayTraceResult.typeOfHit != RayTraceResult.Type.BLOCK || !rayTraceResult.getBlockPos().equals(pos)))
//                continue;
//            return facing;
//        }
//        if ((double) pos.getY() > BlockUtil.mc.player.getY() + (double) BlockUtil.mc.player.getEyeHeight()) {
//            return Direction.DOWN;
//        }
//        return Direction.UP;
//    }

    public static List<Direction> getPossibleSides(BlockPos pos) {
        ArrayList<Direction> facings = new ArrayList<Direction>();
        if (BlockUtil.mc.world == null || pos == null) {
            return facings;
        }
        for (Direction side : Direction.values()) {
            BlockPos neighbour = pos.offset(side);
            BlockState blockState = BlockUtil.mc.world.getBlockState(neighbour);
//            if (blockState == null || !MixinBlockLiquid.canCollide(blockState, false) || blockState.isReplaceable())
//                continue;
            facings.add(side);
        }
        return facings;
    }

//    public static Direction getFirstFacing(BlockPos pos) {
//        Iterator<Direction> iterator = BlockUtil.getPossibleSides(pos).iterator();
//        if (iterator.hasNext()) {
//            Direction facing = iterator.next();
//            return facing;
//        }
//        return null;
//    }

//    public static Direction getRayTraceFacing(BlockPos pos) {
//        RayTraceResult result = BlockUtil.mc.world.raycast(new Vec3d(BlockUtil.mc.player.getX(), BlockUtil.mc.player.getY() + (double) BlockUtil.mc.player.getEyeHeight(), BlockUtil.mc.player.getZ()), new Vec3d((double) pos.getX() + 0.5, (double) pos.getX() - 0.5, (double) pos.getX() + 0.5));
//        if (result == null || result.sideHit == null) {
//            return Direction.UP;
//        }
//        return result.sideHit;
//    }

    public static int isPositionPlaceable(BlockPos pos, boolean rayTrace) {
        try {
            return BlockUtil.isPositionPlaceable(pos, rayTrace, true);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static int isPositionPlaceable(BlockPos pos, boolean rayTrace, boolean entityCheck) throws InstantiationException, IllegalAccessException {
        Block block = BlockUtil.mc.world.getBlockState(pos).getBlock();
        if (!(block instanceof AirBlock || block instanceof FluidBlock || block instanceof GrassBlock || block instanceof FireBlock || block instanceof DeadBushBlock || block instanceof SnowBlock)) {
            return 0;
        }
        if (!BlockUtil.rayTracePlaceCheck(pos, rayTrace, 0.0f)) {
            return -1;
        }
        if (entityCheck) {
            for (Entity entity : BlockUtil.mc.world.getOtherEntities(Entity.class.newInstance() , new Box(pos))) {
                if (entity instanceof ItemEntity || entity instanceof ExperienceBottleEntity) continue;
                return 1;
            }
        }
        for (Direction side : BlockUtil.getPossibleSides(pos)) {
            if (!BlockUtil.canBeClicked(pos.offset(side))) continue;
            return 3;
        }
        return 2;
    }
//
//    public static void rightClickBlock(BlockPos pos, Vec3d vec, Hand hand, Direction direction, boolean packet) {
//        if (packet) {
//            float f = (float) (vec.x - (double) pos.getX());
//            float f1 = (float) (vec.y - (double) pos.getY());
//            float f2 = (float) (vec.z - (double) pos.getZ());
//            BlockUtil.mc.player.networkHandler.sendPacket(new CPacketPlayerTryUseItemOnBlock(pos, direction, hand, f, f1, f2));
//        } else {
//            BlockUtil.mc.playerController.processRightClickBlock(BlockUtil.mc.player, BlockUtil.mc.world, pos, direction, vec, hand);
//        }
//        BlockUtil.mc.player.swingHand(Hand.MAIN_HAND);
//        BlockUtil.mc.rightClickDelayTimer = 4;
//    }

//    public static void rightClickBed(BlockPos pos, float range, boolean rotate, Hand hand, AtomicDouble yaw, AtomicDouble pitch, AtomicBoolean rotating, boolean packet) {
//        Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
//        RayTraceResult result = BlockUtil.mc.world.rayTraceBlocks(new Vec3d(BlockUtil.mc.player.posX, BlockUtil.mc.player.posY + (double) BlockUtil.mc.player.getEyeHeight(), BlockUtil.mc.player.posZ), posVec);
//        Direction face = result == null || result.sideHit == null ? Direction.UP : result.sideHit;
//        Vec3d eyesPos = RotationUtil.getEyesPos();
//        if (rotate) {
//            float[] rotations = RotationUtil.getLegitRotations(posVec);
//            yaw.set(rotations[0]);
//            pitch.set(rotations[1]);
//            rotating.set(true);
//        }
//        BlockUtil.rightClickBlock(pos, posVec, hand, face, packet);
//        BlockUtil.mc.player.swingHand(hand);
//        BlockUtil.mc.rightClickDelayTimer = 4;
//    }

//    public static void rightClickBlockLegit(BlockPos pos, float range, boolean rotate, Direction hand, AtomicDouble Yaw2, AtomicDouble Pitch, AtomicBoolean rotating, boolean packet) {
//        Vec3d eyesPos = RotationUtil.getEyesPos();
//        Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
//        double distanceSqPosVec = eyesPos.squareDistanceTo(posVec);
//        for (Direction side : Direction.values()) {
//            Vec3d hitVec = posVec.add(new Vec3d(side.getDirectionVec()).scale(0.5));
//            double distanceSqHitVec = eyesPos.squareDistanceTo(hitVec);
//            if (distanceSqHitVec > MathUtil.square(range) || distanceSqHitVec >= distanceSqPosVec || BlockUtil.mc.world.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)
//                continue;
//            if (rotate) {
//                float[] rotations = RotationUtil.getLegitRotations(hitVec);
//                Yaw2.set(rotations[0]);
//                Pitch.set(rotations[1]);
//                rotating.set(true);
//            }
//            BlockUtil.rightClickBlock(pos, hitVec, hand, side, packet);
//            BlockUtil.mc.player.swingHand(hand);
//            BlockUtil.mc.rightClickDelayTimer = 4;
//            break;
//        }
//    }

//    public static boolean placeBlock(BlockPos pos, Direction hand, boolean rotate, boolean packet, boolean isSneaking) {
//        boolean sneaking = false;
//        Direction side = BlockUtil.getFirstFacing(pos);
//        if (side == null) {
//            return isSneaking;
//        }
//        BlockPos neighbour = pos.offset(side);
//        Direction opposite = side.getOpposite();
//        Vec3d hitVec = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(new Vec3d(opposite.getDirection()).scale(0.5));
//        Block neighbourBlock = BlockUtil.mc.world.getBlockState(neighbour).getBlock();
//        if (!BlockUtil.mc.player.isSneaking() && (blackList.contains(neighbourBlock) || shulkerList.contains(neighbourBlock))) {
//            BlockUtil.mc.player.connection.sendPacket(new CPacketEntityAction(BlockUtil.mc.player, CPacketEntityAction.Action.START_SNEAKING));
//            BlockUtil.mc.player.setSneaking(true);
//            sneaking = true;
//        }
//        if (rotate) {
//            RotationUtil.faceVector(hitVec, true);
//        }
//        BlockUtil.rightClickBlock(neighbour, hitVec, hand, opposite, packet);
//        BlockUtil.mc.player.swingHand(Hand.MAIN_HAND);
//        BlockUtil.mc.rightClickDelayTimer = 4;
//        return sneaking || isSneaking;
//    }

//    public static boolean placeBlockSmartRotate(BlockPos pos, Direction hand, boolean rotate, boolean packet, boolean isSneaking) {
//        boolean sneaking = false;
//        Direction side = BlockUtil.getFirstFacing(pos);
//        if (side == null) {
//            return isSneaking;
//        }
//        BlockPos neighbour = pos.offset(side);
//        Direction opposite = side.getOpposite();
//        Vec3d hitVec = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(new Vec3d(opposite.getVector()).scale(0.5));
//        Block neighbourBlock = BlockUtil.mc.world.getBlockState(neighbour).getBlock();
//        if (!BlockUtil.mc.player.isSneaking() && (blackList.contains(neighbourBlock) || shulkerList.contains(neighbourBlock))) {
//            BlockUtil.mc.player.networkHandler.sendPacket(new CPacketEntityAction(BlockUtil.mc.player, CPacketEntityAction.Action.START_SNEAKING));
//            sneaking = true;
//        }
//        if (rotate) {
//            rotationManager.lookAtVec3d(hitVec);
//        }
//        BlockUtil.rightClickBlock(neighbour, hitVec, hand, opposite, packet);
//        BlockUtil.mc.player.swingHand(Hand.MAIN_HAND);
//        BlockUtil.mc.mouse.ri = 4;
//        return sneaking || isSneaking;
//    }

//    public static void placeBlockStopSneaking(BlockPos pos, Direction hand, boolean rotate, boolean packet, boolean isSneaking) {
//        boolean sneaking = BlockUtil.placeBlockSmartRotate(pos, hand, rotate, packet, isSneaking);
//        if (!isSneaking && sneaking) {
//            BlockUtil.mc.player.networkHandler.sendPacket(new CPacketEntityAction(BlockUtil.mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
//        }
//    }
//
//    public static Vec3d[] getHelpingBlocks(Vec3d vec3d) {
//        return new Vec3d[]{new Vec3d(vec3d.x, vec3d.y - 1.0, vec3d.z), new Vec3d(vec3d.x != 0.0 ? vec3d.x * 2.0 : vec3d.x, vec3d.y, vec3d.x != 0.0 ? vec3d.z : vec3d.z * 2.0), new Vec3d(vec3d.x == 0.0 ? vec3d.x + 1.0 : vec3d.x, vec3d.y, vec3d.x == 0.0 ? vec3d.z : vec3d.z + 1.0), new Vec3d(vec3d.x == 0.0 ? vec3d.x - 1.0 : vec3d.x, vec3d.y, vec3d.x == 0.0 ? vec3d.z : vec3d.z - 1.0), new Vec3d(vec3d.x, vec3d.y + 1.0, vec3d.z)};
//    }
//
//    public static List<BlockPos> possiblePlacePositions(float placeRange) {
//        NonNullList positions = NonNullList.create();
//        positions.addAll(BlockUtil.getSphere(EntityUtil.getPlayerPos(BlockUtil.mc.player), placeRange, (int) placeRange, false, true, 0).stream().filter(BlockUtil::canPlaceCrystal).collect(Collectors.toList()));
//        return positions;
//    }

    public static List<BlockPos> getSphere(BlockPos pos, float r, int h, boolean hollow, boolean sphere, int plus_y) {
        ArrayList<BlockPos> circleblocks = new ArrayList<BlockPos>();
        int cx = pos.getX();
        int cy = pos.getY();
        int cz = pos.getZ();
        int x = cx - (int) r;
        while ((float) x <= (float) cx + r) {
            int z = cz - (int) r;
            while ((float) z <= (float) cz + r) {
                int y = sphere ? cy - (int) r : cy;
                while (true) {
                    float f = y;
                    float f2 = sphere ? (float) cy + r : (float) (cy + h);
                    if (!(f < f2)) break;
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (!(!(dist < (double) (r * r)) || hollow && dist < (double) ((r - 1.0f) * (r - 1.0f)))) {
                        BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                    ++y;
                }
                ++z;
            }
            ++x;
        }
        return circleblocks;
    }

//    public static List<BlockPos> getDisc(BlockPos pos, float r) {
//        ArrayList<BlockPos> circleblocks = new ArrayList<BlockPos>();
//        int cx = pos.getX();
//        int cy = pos.getY();
//        int cz = pos.getZ();
//        int x = cx - (int) r;
//        while ((float) x <= (float) cx + r) {
//            int z = cz - (int) r;
//            while ((float) z <= (float) cz + r) {
//                double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z);
//                if (dist < (double) (r * r)) {
//                    BlockPos position = new BlockPos(x, cy, z);
//                    circleblocks.add(position);
//                }
//                ++z;
//            }
//            ++x;
//        }
//        return circleblocks;
//    }
//
//    public static boolean canPlaceCrystal(BlockPos blockPos) {
//        BlockPos boost = blockPos.add(0, 1, 0);
//        BlockPos boost2 = blockPos.add(0, 2, 0);
//        try {
//            return (BlockUtil.mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || BlockUtil.mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && BlockUtil.mc.world.getBlockState(boost).getBlock() == Blocks.AIR && BlockUtil.mc.world.getBlockState(boost2).getBlock() == Blocks.AIR && BlockUtil.mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty() && BlockUtil.mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public static List<BlockPos> possiblePlacePositions(float placeRange, boolean specialEntityCheck, boolean oneDot15) {
        NonNullList positions = NonNullList.create();
        positions.addAll(BlockUtil.getSphere(EntityUtil.getPlayerPos(BlockUtil.mc.player), placeRange, (int) placeRange, false, true, 0).stream().filter(pos -> BlockUtil.canPlaceCrystal(pos, specialEntityCheck, oneDot15)).collect(Collectors.toList()));
        return positions;
    }

    public static boolean canPlaceCrystal(BlockPos blockPos, boolean specialEntityCheck, boolean oneDot15) {
        BlockPos boost = blockPos.add(0, 1, 0);
        BlockPos boost2 = blockPos.add(0, 2, 0);
        try {
            if (BlockUtil.mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && BlockUtil.mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) {
                return false;
            }
            if (!oneDot15 && BlockUtil.mc.world.getBlockState(boost2).getBlock() != Blocks.AIR || BlockUtil.mc.world.getBlockState(boost).getBlock() != Blocks.AIR) {
                return false;
            }
            for (Entity entity : BlockUtil.mc.world.getOtherEntities(Entity.class.newInstance(), new Box(boost))) {
                if (!entity.isAlive() || specialEntityCheck && entity instanceof EndCrystalEntity) continue;
                return false;
            }
            if (!oneDot15) {
                for (Entity entity : BlockUtil.mc.world.getOtherEntities(Entity.class.newInstance(), new Box(boost))) {
                    if (!entity.isAlive() || specialEntityCheck && entity instanceof EndCrystalEntity) continue;
                    return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    public static boolean canBeClicked(BlockPos pos) {

//        return MixinBlockLiquid.canCollide(BlockUtil.getState(pos), false);
    return true;
    }
    private static Block getBlock(BlockPos pos) {
        return BlockUtil.getState(pos).getBlock();
    }

    private static BlockState getState(BlockPos pos) {
        return BlockUtil.mc.world.getBlockState(pos);
    }

//    public static boolean isBlockAboveEntitySolid(Entity entity) {
//        if (entity != null) {
//            BlockPos pos = new BlockPos(entity.getBlockX(), (int) (entity.getBlockY() + 2.0), entity.getBlockZ());
//            return BlockUtil.isBlockSolid(pos);
//        }
//        return false;
//    }
//
//    public static void debugPos(String message, BlockPos pos) {
//        Command.sendMessage(message + pos.getX() + "x, " + pos.getY() + "y, " + pos.getZ() + "z");
//    }

    public static Vec3d getEyesPos(@NotNull Entity entity) {
        return entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
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
//        RayTraceResult result = BlockUtil.mc.world.raycast(
//            new Vec3d(BlockUtil.mc.player.getX(),
//                BlockUtil.mc.player.getY() + (double) BlockUtil.mc.player.getEyeHeight(mc.player.getPose()),
//                BlockUtil.mc.player.getZ()),
//
//            new Vec3d((double) pos.getX() + 0.5,
//                (double) pos.getY() - 0.5,
//                (double) pos.getZ() + 0.5)
//        );
        Direction facing = result.getSide();
            //== null || result.sideHit == null ? Direction.UP : result.sideHit;
        if (mc.player != null) {
            BlockHitResult blockHitResult = new BlockHitResult(mc.player.getPos(), facing, pos, false);
            ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);
            if (actionResult == ActionResult.SUCCESS) {
                PlayerActionC2SPacket packet = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, facing);
                mc.getNetworkHandler().sendPacket(packet);
            }
        }        if (swing) {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, mc.player.getBlockPos(), Direction.UP));        }
    }

//    public static BlockPos[] toBlockPos(Vec3d[] vec3ds) {
//        BlockPos[] list = new BlockPos[vec3ds.length];
//        for (int i = 0; i < vec3ds.length; ++i) {
//            list[i] = new BlockPos(vec3ds[i]);
//        }
//        return list;
//    }
//
//    public static Vec3d posToVec3d(BlockPos pos) {
//        return new Vec3d(pos);
//    }
//
//    public static BlockPos vec3dToPos(Vec3d vec3d) {
//        return new BlockPos(vec3d);
//    }
//
//    public static Boolean isPosInFov(BlockPos pos) {
//        int dirnumber = RotationUtil.getDirection4D();
//        if (dirnumber == 0 && (double) pos.getZ() - BlockUtil.mc.player.getPos().z < 0.0) {
//            return false;
//        }
//        if (dirnumber == 1 && (double) pos.getX() - BlockUtil.mc.player.getPos().x > 0.0) {
//            return false;
//        }
//        if (dirnumber == 2 && (double) pos.getZ() - BlockUtil.mc.player.getPos().z > 0.0) {
//            return false;
//        }
//        return dirnumber != 3 || !((double) pos.getX() - BlockUtil.mc.player.getPos().x < 0.0);
//    }
//
//    public static boolean isBlockBelowEntitySolid(Entity entity) {
//        if (entity != null) {
//            BlockPos pos = new BlockPos(entity.getBlockX(), (int) (entity.getBlockY() - 1.0), entity.getBlockZ());
//            return BlockUtil.isBlockSolid(pos);
//        }
//        return false;
//    }

    public static boolean isBlockSolid(BlockPos pos) {
        return !BlockUtil.isBlockUnSolid(pos);
    }

    public static boolean isBlockUnSolid(BlockPos pos) {
        return BlockUtil.isBlockUnSolid(BlockUtil.mc.world.getBlockState(pos).getBlock());
    }

    public static boolean isBlockUnSolid(Block block) {
        return unSolidBlocks.contains(block);
    }

    public static Vec3d[] convertVec3ds(Vec3d vec3d, Vec3d[] input) {
        Vec3d[] output = new Vec3d[input.length];
        for (int i = 0; i < input.length; ++i) {
            output[i] = vec3d.add(input[i]);
        }
        return output;
    }

    public static Vec3d[] convertVec3ds(PlayerEntity entity, Vec3d[] input) {
        return BlockUtil.convertVec3ds(entity.getPos(), input);
    }

//    public static boolean canBreak(BlockPos pos) {
//        BlockState blockState = BlockUtil.mc.world.getBlockState(pos);
//        Block block = blockState.getBlock();
//        return block.getBlockHardness(blockState, BlockUtil.mc.world, pos) != -1.0f;
//    }
//
//    public static boolean isValidBlock(BlockPos pos) {
//        Block block = BlockUtil.mc.world.getBlockState(pos).getBlock();
//        return !(block instanceof BlockLiquid) && block.getMaterial(null) != Material.AIR;
//    }
//
//    public static boolean isScaffoldPos(BlockPos pos) {
//        return BlockUtil.mc.world.isAir(pos) || BlockUtil.mc.world.getBlockState(pos).getBlock() == Blocks.SNOW_LAYER || BlockUtil.mc.world.getBlockState(pos).getBlock() == Blocks.TALLGRASS || BlockUtil.mc.world.getBlockState(pos).getBlock() instanceof BlockLiquid;
//    }

    public static boolean rayTracePlaceCheck(BlockPos pos, boolean shouldCheck, float height) {
//        if (!shouldCheck) {
//            return true; // No need to check, return true
//        }
//        // Perform ray trace
//        boolean isClear = mc.world.raycastBlock(
//            new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()),
//            new Vec3d(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5),
//            mc.player.getBlockPos() , RaycastContext.ShapeType.COLLIDER, mc.world.getBlockState()
//        ) == null;
//
//        return isClear;
        return shouldCheck;
    }
    public static boolean rayTracePlaceCheck(BlockPos pos, boolean shouldCheck) {
        return BlockUtil.rayTracePlaceCheck(pos, shouldCheck, 1.0f);
    }

    public static boolean rayTracePlaceCheck(BlockPos pos) {
        return BlockUtil.rayTracePlaceCheck(pos, true);
    }

//    public static boolean isInHole() {
//        BlockPos blockPos = new BlockPos(BlockUtil.mc.player.getBlockX(), BlockUtil.mc.player.getY(), BlockUtil.mc.player.getZ());
//        BlockState blockState = BlockUtil.mc.world.getBlockState(blockPos);
//        return BlockUtil.isBlockValid(blockState, blockPos);
//    }

//    public static double getNearestBlockBelow() {
//        for (double y = BlockUtil.mc.player.getY(); y > 0.0; y -= 0.001) {
//            if (BlockUtil.mc.world.getBlockState(new BlockPos(BlockUtil.mc.player.getBlockX(), (int) y, BlockUtil.mc.player.getBlockZ())).getBlock() instanceof SlabBlock || BlockUtil.mc.world.getBlockState(new BlockPos(BlockUtil.mc.player.getBlockX(), (int) y, BlockUtil.mc.player.getBlockZ())).getBlock().getDefaultState().getCollisionBoundingBox(BlockUtil.mc.world, new BlockPos(0, 0, 0)) == null)
//                continue;
//            return y;
//        }
//        return -1.0;
//    }

//    public static boolean isBlockValid(BlockState blockState, BlockPos blockPos) {
//        if (blockState.getBlock() != Blocks.AIR) {
//            return false;
//        }
//        if (BlockUtil.mc.player.getBlockPos().getSquaredDistance(blockPos) < 1.0) {
//            return false;
//        }
//        if (BlockUtil.mc.world.getBlockState(blockPos.up()).getBlock() != Blocks.AIR) {
//            return false;
//        }
//        if (BlockUtil.mc.world.getBlockState(blockPos.up(2)).getBlock() != Blocks.AIR) {
//            return false;
//        }
//        return BlockUtil.isBedrockHole(blockPos) || BlockUtil.isObbyHole(blockPos) || BlockUtil.isBothHole(blockPos) || BlockUtil.isElseHole(blockPos);
//    }

    public static boolean isObbyHole(BlockPos blockPos) {
        for (BlockPos pos : BlockUtil.getTouchingBlocks(blockPos)) {
            BlockState touchingState = BlockUtil.mc.world.getBlockState(pos);
            if (touchingState.getBlock() != Blocks.AIR && touchingState.getBlock() == Blocks.OBSIDIAN) continue;
            return false;
        }
        return true;
    }

    public static boolean isBedrockHole(BlockPos blockPos) {
        for (BlockPos pos : BlockUtil.getTouchingBlocks(blockPos)) {
            BlockState touchingState = BlockUtil.mc.world.getBlockState(pos);
            if (touchingState.getBlock() != Blocks.AIR && touchingState.getBlock() == Blocks.BEDROCK) continue;
            return false;
        }
        return true;
    }

    public static boolean isBothHole(BlockPos blockPos) {
        for (BlockPos pos : BlockUtil.getTouchingBlocks(blockPos)) {
            BlockState touchingState = BlockUtil.mc.world.getBlockState(pos);
            if (touchingState.getBlock() != Blocks.AIR && (touchingState.getBlock() == Blocks.BEDROCK || touchingState.getBlock() == Blocks.OBSIDIAN))
                continue;
            return false;
        }
        return true;
    }

//    public static boolean isElseHole(BlockPos blockPos) {
//        for (BlockPos pos : BlockUtil.getTouchingBlocks(blockPos)) {
//            BlockState touchingState = BlockUtil.mc.world.getBlockState(pos);
//            if (touchingState.getBlock() != Blocks.AIR && touchingState.isFullCube() continue;
//            return false;
//        }
//        return true;
//    }

    public static BlockPos[] getTouchingBlocks(BlockPos blockPos) {
        return new BlockPos[]{blockPos.north(), blockPos.south(), blockPos.east(), blockPos.west(), blockPos.down()};
    }
}

