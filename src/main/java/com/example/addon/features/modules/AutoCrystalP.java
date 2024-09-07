package com.example.addon.features.modules;

import com.example.addon.Addon;
import com.example.addon.event.events.UpdateWalkingPlayerEvent;
import com.example.addon.features.Feature;
import com.example.addon.settings.FloatSetting;
import com.example.addon.util.*;
import com.example.addon.util.Timer;
import com.mojang.authlib.GameProfile;
import io.netty.util.internal.ConcurrentSet;
import meteordevelopment.meteorclient.commands.commands.BindsCommand;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.network.*;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.server.*;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.common.gameevent.InputEvent;

import javax.annotation.Nonnull;

import static com.example.addon.Addon.HUD_GROUP;
import static com.example.addon.features.modules.AutoCrystalP.AutoSwitch.ALWAYS;
import static com.example.addon.manager.Managers.*;
import static com.example.addon.util.EntityUtil.getHealth;
import static com.example.addon.util.NoSoundLag.removeEntities;
import static meteordevelopment.meteorclient.utils.network.PacketUtils.getPacket;
import static net.minecraft.block.entity.ChestBlockEntity.copyInventory;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType.ATTACK;
import static net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.attack;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoCrystalP extends Module {
    private final SettingGroup sgDev = settings.createGroup("Dev");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Attack");
    private final SettingGroup sgRender = settings.createGroup("Render");
    public static PlayerEntity target = null;
    public static Set<BlockPos> lowDmgPos = new ConcurrentSet();
    public static Set<BlockPos> placedPos = new HashSet<BlockPos>();
    public static Set<BlockPos> brokenPos = new HashSet<BlockPos>();
    private static AutoCrystalP instance;
    public final Timer threadTimer = new Timer();
    private final Timer manualTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer syncTimer = new Timer();
    private final Timer predictTimer = new Timer();

    private final Timer switchTimer = new Timer();
    private final Timer renderTimer = new Timer();
    private final AtomicBoolean shouldInterrupt = new AtomicBoolean(false);
    private final Timer syncroTimer = new Timer();
    private final Map<PlayerEntity, Timer> totemPops = new ConcurrentHashMap<PlayerEntity, Timer>();
    private final Queue<PlayerInteractEntityC2SPacket> packetUseEntities = new LinkedList<PlayerInteractEntityC2SPacket>();
    private final AtomicBoolean threadOngoing = new AtomicBoolean(false);

    ///DEV///
    private final Setting<Settings> setting = sgDev.add(new EnumSetting.Builder<Settings>()
        .name("Settings")
        .description("Settings")
        .defaultValue(Settings.PLACE)
        .build()
    );
    private final Setting<Boolean> attackOppositeHand = sgDev.add(new BoolSetting.Builder()
        .name("OppositeHand")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> removeAfterAttack = sgDev.add(new BoolSetting.Builder()
        .name("AttackRemove")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> antiBlock = sgDev.add(new BoolSetting.Builder()
        .name("AntiFeetPlace")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Integer> switchCooldown = sgDev.add(new IntSetting.Builder()
        .name("Cooldown")
        .description("Cooldown")
        .defaultValue(Integer.valueOf(500))
        .min(0)
        .max(1000)
        .sliderRange(0, 1000)
        .build()
    );
    private final Setting<Integer> eventMode = sgDev.add(new IntSetting.Builder()
        .name("Updates")
        .description("Updates")
        .defaultValue(Integer.valueOf(3))
        .min(1)
        .max(3)
        .sliderRange(1, 3)
        .build()
    );
    private final Setting<Float> minMinDmg = sgDev.add(new FloatSetting.Builder()
        .name("MinMinDmg")
        .description("")
        .min(0.0f).max(3.0f)
        .sliderRange(0.0f, 3.0f)
        .defaultValue(0.0f)
        .build()
    );
    private final Setting<Boolean> breakSwing = sgDev.add(new BoolSetting.Builder()
        .name("BreakSwing")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> placeSwing = sgDev.add(new BoolSetting.Builder()
        .name("PlaceSwing")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> exactHand = sgDev.add(new BoolSetting.Builder()
        .name("ExactHand")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> justRender = sgDev.add(new BoolSetting.Builder()
        .name("JustRender")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> fakeSwing = sgDev.add(new BoolSetting.Builder()
        .name("FakeSwing")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Logic> logic = sgDev.add(new EnumSetting.Builder<Logic>()
        .name("Logic")
        .description("Logic")
        .defaultValue(Logic.BREAKPLACE)
        .build()
    );
    private final Setting<DamageSync> damageSync = sgDev.add(new EnumSetting.Builder<DamageSync>()
        .name("Logic")
        .description("")
        .defaultValue(DamageSync.NONE)
        .build()
    );
    private final Setting<Integer> damageSyncTime = sgDev.add(new IntSetting.Builder()
        .name("SyncDelay")
        .description("")
        .defaultValue(Integer.valueOf(500))
        .min(0)
        .max(500)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Boolean> syncedFeetPlace = sgDev.add(new BoolSetting.Builder()
        .name("FeetSync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> fullSync = sgDev.add(new BoolSetting.Builder()
        .name("FullSync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> syncCount = sgDev.add(new BoolSetting.Builder()
        .name("SyncCount")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> hyperSync = sgDev.add(new BoolSetting.Builder()
        .name("HyperSync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> gigaSync = sgDev.add(new BoolSetting.Builder()
        .name("GigaSync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> syncySync = sgDev.add(new BoolSetting.Builder()
        .name("SyncySync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> enormousSync = sgDev.add(new BoolSetting.Builder()
        .name("EnormousSync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> holySync = sgDev.add(new BoolSetting.Builder()
        .name("UnbelievableSync")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> rotateFirst = sgDev.add(new BoolSetting.Builder()
        .name("FirstRotation")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Float> dropOff = sgDev.add(new FloatSetting.Builder()
        .name("DropOff")
        .description("")
        .min(0.0f)
        .sliderMax(10.0f)
        .defaultValue(5.0f)
        .build()
    );
    private final Setting<Integer> confirm = sgDev.add(new IntSetting.Builder()
        .name("Confirm")
        .description("")
        .defaultValue(Integer.valueOf(250))
        .min(0)
        .sliderMax(1000)
        .build()
    );
    public final Setting<ThreadMode> threadMode = sgDev.add(new EnumSetting.Builder<ThreadMode>()
        .name("Thread")
        .description("")
        .defaultValue(ThreadMode.NONE)
        .build()
    );
    private final Setting<Integer> threadDelay = sgDev.add(new IntSetting.Builder()
        .name("ThreadDelay")
        .description("")
        .defaultValue(Integer.valueOf(50))
        .min(1)
        .sliderMax(1000)
        .build()
    );
    private final Setting<Boolean> syncThreadBool = sgDev.add(new BoolSetting.Builder()
        .name("ThreadSync")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Integer> syncThreads = sgDev.add(new IntSetting.Builder()
        .name("SyncThreads")
        .description("")
        .defaultValue(Integer.valueOf(1000))
        .min(1)
        .sliderMax(10000)
        .build()
    );
    private final Setting<Boolean> predictPos = sgDev.add(new BoolSetting.Builder()
        .name("PredictPos")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Integer> predictTicks = sgDev.add(new IntSetting.Builder()
        .name("ExtrapolationTicks")
        .description("")
        .defaultValue(Integer.valueOf(2))
        .min(1)
        .sliderMax(20)
        .build()
    );
    private final Setting<Integer> rotations = sgDev.add(new IntSetting.Builder()
        .name("Spoofs")
        .description("")
        .defaultValue(Integer.valueOf(1))
        .min(1)
        .sliderMax(20)
        .build()
    );
    private final Setting<Boolean> predictRotate = sgDev.add(new BoolSetting.Builder()
        .name("PredictRotate")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Float> predictOffset = sgDev.add(new FloatSetting.Builder()
        .name("PredictOffset")
        .description("")
        .min(0.0f)
        .sliderMax(4.0f)
        .defaultValue(0.0f)
        .build()
    );
    ///MISC///

    private final Setting<Raytrace> raytrace = sgMisc.add(new EnumSetting.Builder<Raytrace>()
        .name("Raytrace")
        .description("Raytrace")
        .defaultValue(Raytrace.NONE)
        .build()
    );
    private final Setting<Boolean> brownZombie = sgMisc.add(new BoolSetting.Builder()
        .name("BrownZombieMode")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> holdFacePlace = sgMisc.add(new BoolSetting.Builder()
        .name("HoldFacePlace")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> holdFaceBreak = sgMisc.add(new BoolSetting.Builder()
        .name("HoldSlowBreak")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> slowFaceBreak = sgMisc.add(new BoolSetting.Builder()
        .name("SlowFaceBreak")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> actualSlowBreak = sgMisc.add(new BoolSetting.Builder()
        .name("ActuallySlow")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Integer> facePlaceSpeed = sgMisc.add(new IntSetting.Builder()
        .name("FaceSpeed")
        .description("")
        .defaultValue(Integer.valueOf(500))
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Boolean> antiNaked = sgMisc.add(new BoolSetting.Builder()
        .name("AntiNaked")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Float> range = sgMisc.add(new FloatSetting.Builder()
        .name("Range")
        .description("")
        .min(0.1f)
        .sliderMax(20.0f)
        .defaultValue(12.0f)
        .build()
    );
    private final Setting<Target> targetMode = sgMisc.add(new EnumSetting.Builder<Target>()
        .name("Target")
        .description("")
        .defaultValue(Target.CLOSEST)
        .build()
    );
    private final Setting<Integer> minArmor = sgMisc.add(new IntSetting.Builder()
        .name("MinArmor")
        .description("")
        .defaultValue(Integer.valueOf(5))
        .min(0)
        .sliderMax(125)
        .build()
    );
    private final Setting<AutoSwitch> autoSwitch = sgMisc.add(new EnumSetting.Builder<AutoSwitch>()
        .name("Switch")
        .description("")
        .defaultValue(AutoSwitch.TOGGLE)
        .build()
    );
    private final Setting<Keybind> switchBind = sgMisc.add(new KeybindSetting.Builder()
        .name("SwitchBind")
        .description("")
        .defaultValue(Keybind.none())
        .build()
    );
    private final Setting<Boolean> offhandSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("Offhand")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> switchBack = sgMisc.add(new BoolSetting.Builder()
        .name("Switchback")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> lethalSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("LethalSwitch")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> mineSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("MineSwitch")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Rotate> rotate = sgMisc.add(new EnumSetting.Builder<Rotate>()
        .name("Rotate")
        .description("")
        .defaultValue(Rotate.OFF)
        .build()
    );
    private final Setting<Boolean> suicide = sgMisc.add(new BoolSetting.Builder()
        .name("Suicide")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> webAttack = sgMisc.add(new BoolSetting.Builder()
        .name("WebAttack")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> fullCalc = sgMisc.add(new BoolSetting.Builder()
        .name("ExtraCalc")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> sound = sgMisc.add(new BoolSetting.Builder()
        .name("Sound")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Float> soundRange = sgMisc.add(new FloatSetting.Builder()
        .name("SoundRange")
        .description("")
        .min(0.0f)
        .sliderMax(12.0f)
        .defaultValue(12.0f)
        .build()
    );
    private final Setting<Float> soundPlayer = sgMisc.add(new FloatSetting.Builder()
        .name("SoundPlayer")
        .description("")
        .min(0.0f)
        .sliderMax(12.0f)
        .defaultValue(6.0f)
        .build()
    );
    private final Setting<Boolean> soundConfirm = sgMisc.add(new BoolSetting.Builder()
        .name("SoundConfirm")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> extraSelfCalc = sgMisc.add(new BoolSetting.Builder()
        .name("MinSelfDmg")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<AntiFriendPop> antiFriendPop = sgMisc.add(new EnumSetting.Builder<AntiFriendPop>()
        .name("FriendPop")
        .description("")
        .defaultValue(AntiFriendPop.NONE)
        .build()
    );
    private final Setting<Boolean> noCount = sgMisc.add(new BoolSetting.Builder()
        .name("AntiCount")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> calcEvenIfNoDamage = sgMisc.add(new BoolSetting.Builder()
        .name("BigFriendCalc")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> predictFriendDmg = sgMisc.add(new BoolSetting.Builder()
        .name("PredictFriend")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );

    ///Place

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
        .name("Place")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> doublePopOnDamage = sgPlace.add(new BoolSetting.Builder()
        .name("DamagePop")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("PlaceDelay")
        .description("")
        .defaultValue(Integer.valueOf(25))
        .min(0)
        .max(500)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Float> placeRange = sgPlace.add(new FloatSetting.Builder()
        .name("PlaceRange")
        .description("")
        .min(0.0f).max(10.0f)
        .sliderRange(0.0f, 10.0f)
        .defaultValue(6.0f)
        .build()
    );
    private final Setting<Float> minDamage = sgPlace.add(new FloatSetting.Builder()
        .name("MinDamage")
        .description("")
        .min(0.1f).max(20.0f)
        .sliderRange(0.1f, 20.0f)
        .defaultValue(7.0f)
        .build()
    );
    private final Setting<Float> maxSelfPlace = sgPlace.add(new FloatSetting.Builder()
        .name("MaxSelfPlace")
        .description("")
        .min(0.1f).max(36.0f)
        .sliderRange(0.1f, 36.0f)
        .defaultValue(10.0f)
        .build()
    );
    private final Setting<Integer> wasteAmount = sgPlace.add(new IntSetting.Builder()
        .name("WasteAmount")
        .description("")
        .defaultValue(Integer.valueOf(2))
        .min(1)
        .max(5)
        .sliderRange(1, 5)
        .build()
    );
    private final Setting<Boolean> wasteMinDmgCount = sgPlace.add(new BoolSetting.Builder()
        .name("CountMinDmg")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> antiSurround = sgPlace.add(new BoolSetting.Builder()
        .name("AntiSurround")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> limitFacePlace = sgPlace.add(new BoolSetting.Builder()
        .name("LimitFacePlace")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> oneDot15 = sgPlace.add(new BoolSetting.Builder()
        .name("1.15")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Boolean> doublePop = sgPlace.add(new BoolSetting.Builder()
        .name("AntiTotem")
        .description("")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );
    private final Setting<Double> popHealth = sgPlace.add(new DoubleSetting.Builder()
        .name("PopHealth")
        .description("")
        .defaultValue(1.0)
        .min(0)
        .sliderMax(3.0)
        .build()
    );
    private final Setting<Integer> popTime = sgPlace.add(new IntSetting.Builder()
        .name("PopTime")
        .description("")
        .defaultValue(Integer.valueOf(500))
        .min(0)
        .sliderMax(1000)
        .build()
    );
    private final Setting<Float> facePlace = sgPlace.add(new FloatSetting.Builder()
        .name("FacePlace")
        .description("")
        .min(0.1f)
        .sliderMax(20.0f)
        .defaultValue(8.0f)
        .build()
    );
    private final Setting<Float> placetrace = sgPlace.add(new FloatSetting.Builder()
        .name("Placetrace")
        .description("")
        .min(0.0f)
        .sliderMax(10.0f)
        .defaultValue(4.5f)
        .build()
    );
    private final Setting<Float> popDamage = sgPlace.add(new FloatSetting.Builder()
        .name("FacePlace")
        .description("")
        .min(0.0f)
        .sliderMax(6.0f)
        .defaultValue(4.0f)
        .build()
    );

    ///Break///
    private final Setting<Boolean> resetBreakTimer = sgBreak.add(new BoolSetting.Builder()
        .name("ResetBreakTimer")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> predictCalc = sgBreak.add(new BoolSetting.Builder()
        .name("PredictCalc")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> superSafe = sgBreak.add(new BoolSetting.Builder()
        .name("SuperSafe")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> antiCommit = sgBreak.add(new BoolSetting.Builder()
        .name("AntiOverCommit")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> sync = sgBreak.add(new BoolSetting.Builder()
        .name("Sync")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> instant = sgBreak.add(new BoolSetting.Builder()
        .name("Predict")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> manual = sgBreak.add(new BoolSetting.Builder()
        .name("Manual")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> manualMinDmg = sgBreak.add(new BoolSetting.Builder()
        .name("ManMinDmg")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Boolean> explode = sgBreak.add(new BoolSetting.Builder()
        .name("Break")
        .description("")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<Switch> switchMode = sgDev.add(new EnumSetting.Builder<Switch>()
        .name("Attack")
        .description("")
        .defaultValue(Switch.BREAKSLOT)
        .build()
    );
    private final Setting<PredictTimer> instantTimer = sgDev.add(new EnumSetting.Builder<PredictTimer>()
        .name("Attack")
        .description("")
        .defaultValue(PredictTimer.NONE)
        .build()
    );
    private final Setting<Integer> packets = sgPlace.add(new IntSetting.Builder()
        .name("Packets")
        .description("")
        .defaultValue(Integer.valueOf(1))
        .min(1)
        .sliderMax(6)
        .build()
    );
    private final Setting<Integer> breakDelay = sgPlace.add(new IntSetting.Builder()
        .name("BreakDelay")
        .description("")
        .defaultValue(Integer.valueOf(50))
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Integer> manualBreak = sgPlace.add(new IntSetting.Builder()
        .name("ManualDelay")
        .description("")
        .defaultValue(Integer.valueOf(500))
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Integer> predictDelay = sgPlace.add(new IntSetting.Builder()
        .name("PredictDelay")
        .description("")
        .defaultValue(Integer.valueOf(12))
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Float> breakRange = sgPlace.add(new FloatSetting.Builder()
        .name("BreakRange")
        .description("")
        .min(0.0f)
        .sliderMax(10.0f)
        .defaultValue(6.0f)
        .build()
    );
    private final Setting<Float> maxSelfBreak = sgPlace.add(new FloatSetting.Builder()
        .name("MaxSelfBreak")
        .description("")
        .min(0.1f)
        .sliderMax(36.0f)
        .defaultValue(10.0f)
        .build()
    );
    private final Setting<Float> breaktrace = sgPlace.add(new FloatSetting.Builder()
        .name("Breaktrace")
        .description("")
        .min(0.0f)
        .sliderMax(10.0f)
        .defaultValue(4.5f)
        .build()
    );

    ///RENDER///

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("The mode to render in.")
        .defaultValue(RenderMode.Normal)
        .build()
    );
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders hand swinging client-side.")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay over the block the crystals are being placed on.")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
        .name("break")
        .description("Renders a block overlay over the block the crystals are broken on.")
        .defaultValue(Boolean.valueOf(false))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the block overlay.")
        .defaultValue(new SettingColor(255, 255, 255, 45))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the block overlay.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder()
        .name("damage")
        .description("Renders crystal damage text in the block overlay.")
        .defaultValue(Boolean.valueOf(true))
        .build()
    );
    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder()
        .name("damage-color")
        .description("The color of the damage text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> renderMode.get() != RenderMode.None && renderDamageText.get())
        .build()
    );

    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
        .name("damage-scale")
        .description("How big the damage text should be.")
        .defaultValue(1.25)
        .min(1)
        .sliderMax(4)
        .visible(renderDamageText::get)
        .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
        .name("render-time")
        .description("How long to render for.")
        .defaultValue(Integer.valueOf(10))
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder()
        .name("break-time")
        .description("How long to render breaking for.")
        .defaultValue(Integer.valueOf(13))
        .min(0)
        .sliderMax(20)
        .visible(renderBreak::get)
        .build()
    );
//    private final Setting<Integer> red = this.register(new Setting<Object>("Red", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    private final Setting<Integer> green = this.register(new Setting<Object>("Green", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    private final Setting<Integer> blue = this.register(new Setting<Object>("Blue", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    private final Setting<Integer> alpha = this.register(new Setting<Object>("Alpha", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    public Setting<Boolean> colorSync = this.register(new Setting<Object>("CSync", Boolean.valueOf(false), v -> this.setting.getValue() == Settings.RENDER));
//    public Setting<Boolean> box = this.register(new Setting<Object>("Box", Boolean.valueOf(true), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    private final Setting<Integer> boxAlpha = this.register(new Setting<Object>("BoxAlpha", Integer.valueOf(125), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.box.getValue() != false));
//    public Setting<Boolean> outline = this.register(new Setting<Object>("Outline", Boolean.valueOf(true), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    private final Setting<Float> lineWidth = this.register(new Setting<Object>("LineWidth", Float.valueOf(1.5f), Float.valueOf(0.1f), Float.valueOf(5.0f), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.outline.getValue() != false));
//    public Setting<Boolean> text = this.register(new Setting<Object>("Text", Boolean.valueOf(false), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false));
//    public Setting<Boolean> customOutline = this.register(new Setting<Object>("CustomLine", Boolean.valueOf(false), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.outline.getValue() != false));
//    private final Setting<Integer> cRed = this.register(new Setting<Object>("OL-Red", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.customOutline.getValue() != false && this.outline.getValue() != false));
//    private final Setting<Integer> cGreen = this.register(new Setting<Object>("OL-Green", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.customOutline.getValue() != false && this.outline.getValue() != false));
//    private final Setting<Integer> cBlue = this.register(new Setting<Object>("OL-Blue", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.customOutline.getValue() != false && this.outline.getValue() != false));
//    private final Setting<Integer> cAlpha = this.register(new Setting<Object>("OL-Alpha", Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255), v -> this.setting.getValue() == Settings.RENDER && this.render.getValue() != false && this.customOutline.getValue() != false && this.outline.getValue() != false));
//public Setting<Boolean> renderExtrapolation = this.register(new Setting<Object>("RenderExtrapolation", Boolean.valueOf(false), v -> this.setting.getValue() == Settings.DEV && this.predictPos.getValue() != false));


    public boolean rotating = false;
    private Queue<Entity> attackList = new ConcurrentLinkedQueue<Entity>();
    private Map<Entity, Float> crystalMap = new HashMap<Entity, Float>();
    private Entity efficientTarget = null;
    private double currentDamage = 0.0;
    private double renderDamage = 0.0;
    private double lastDamage = 0.0;
    private boolean didRotation = false;
    private boolean switching = false;
    private BlockPos placePos = null;
    private BlockPos renderPos = null;
    private boolean mainHand = false;
    private boolean offHand = false;
    private int crystalCount = 0;
    private int minDmgCount = 0;
    private int lastSlot = -1;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private BlockPos webPos = null;
    private BlockPos lastPos = null;
    private boolean posConfirmed = false;
    private boolean foundDoublePop = false;
    private int rotationPacketsSpoofed = 0;
    private ScheduledExecutorService executor;
    private Thread thread;
    private PlayerEntity currentSyncTarget;
    private BlockPos syncedPlayerPos;
    private BlockPos syncedCrystalPos;
    private PlaceInfo placeInfo;
    private boolean addTolowDmg;
    static Modules modules = Modules.get();
    private final Vector3d vec3 = new Vector3d();
    private Box renderBoxOne, renderBoxTwo;
    private final BlockPos.Mutable placeRenderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();

    public AutoCrystalP() {
        super(Addon.CATEGORY, "AutoCrystalP", "Best CA on the market");
        instance = this;
    }

    public static AutoCrystalP getInstance() {
        if (instance == null) {
            instance = new AutoCrystalP();
        }
        return instance;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTickPost(TickEvent.Post event) {
        if (this.threadMode.get() == ThreadMode.NONE && this.eventMode.get() == 3) {
            this.doAutoCrystal();
        }
    }

    @EventHandler
    public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
        if (event.getStage() == 1) {
            this.postProcessing();
        }
        if (event.getStage() != 0) {
            return;
        }
        if (this.eventMode.get() == 2) {
            this.doAutoCrystal();
        }
    }

    public void postTick() {
        if (this.threadMode.get() != ThreadMode.NONE) {
            this.processMultiThreading();
        }
    }
    @Override
    public void onActivate() {
        brokenPos.clear();
        placedPos.clear();
        this.totemPops.clear();
        this.rotating = false;
        if (this.threadMode.get() != ThreadMode.NONE) {
            this.processMultiThreading();
        }
    }

    @Override
    public void onDeactivate() {
        if (this.thread != null) {
            this.shouldInterrupt.set(true);
        }
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }
//    @Override
//    public String getDisplayInfo() {
//        if (this.switching) {
//            return "\u00a7aSwitch";
//        }
//        if (target != null) {
//            return String.valueOf(target.getName());
//        }
//        return null;
//    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSend(PacketEvent.Send event) {
        PlayerInteractEntityC2SPacket packet;
        if (event.hashCode() == 0 && this.rotate.get() != Rotate.OFF && this.rotating && this.eventMode.get() != 2 && event.packet instanceof EntityS2CPacket) {
            EntityS2CPacket packet2 = (EntityS2CPacket) event.packet;
            byte getYawByte = packet2.getYaw();
            byte getPitchByte = packet2.getPitch();
            this.yaw = (int) getYawByte;
            this.pitch = (int) getPitchByte;
            ++this.rotationPacketsSpoofed;
            if (this.rotationPacketsSpoofed >= this.rotations.get()) {
                this.rotating = false;
                this.rotationPacketsSpoofed = 0;
            }
        }
        BlockPos pos = null;
        if (event.hashCode() == 0 && event.packet instanceof PlayerInteractEntityC2SPacket && event.packet.equals(PlayerInteractEntityC2SPacket.InteractType.ATTACK) && ((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world.getServer().getOverworld()) instanceof EndCrystalEntity) {
            pos = ((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world.getServer().getOverworld()).getBlockPos();
            if (this.removeAfterAttack.get().booleanValue()) {
                Objects.requireNonNull(((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world.getServer().getOverworld()));

//                mc.world.removeEntity(packet.getEntity(mc.world.getServer().getOverworld()).getId()));
            }
        }
        if (event.hashCode() == 0 && event.packet instanceof PlayerInteractEntityC2SPacket && event.packet.equals(PlayerInteractEntityC2SPacket.InteractType.ATTACK) && ((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world.getServer().getOverworld()) instanceof EndCrystalEntity) {
            EndCrystalEntity crystal = (EndCrystalEntity) ((PlayerInteractEntityC2SPacket) event.packet).getEntity((ServerWorld) mc.world.getEntities());
            if (this.antiBlock.get().booleanValue() && EntityUtil.isCrystalAtFeet(crystal, this.range.get().floatValue()) && pos != null) {
                this.rotateToPos(pos);
                BlockUtil.placeCrystalOnBlock(this.placePos, this.offHand ? Hand.OFF_HAND : Hand.MAIN_HAND, this.placeSwing.get(), this.exactHand.get());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketEvent.Receive event) {
        PlaySoundS2CPacket packet;
        if (Feature.fullNullCheck()) {
            return;
        }
        if (!this.justRender.get().booleanValue() && this.switchTimer.passedMs(this.switchCooldown.get().intValue()) && this.explode.get().booleanValue() && this.instant.get().booleanValue() && event.packet instanceof
            //Entity spawn object
            EntitySpawnS2CPacket && (this.syncedCrystalPos == null || !this.syncedFeetPlace.get().booleanValue() || this.damageSync.get() == DamageSync.NONE)) {
            BlockPos pos;
            EntitySpawnS2CPacket packet2 = (EntitySpawnS2CPacket) event.packet;
            if (packet2.getId() == 51 && mc.player.getBlockPos().getSquaredDistance(pos = new BlockPos((int) packet2.getX(), (int) packet2.getY(), (int) packet2.getZ())) + (double) this.predictOffset.get().floatValue() <= MathUtil.square(this.breakRange.get().floatValue()) && (this.instantTimer.get() == PredictTimer.NONE || this.instantTimer.get() == PredictTimer.BREAK && this.breakTimer.passedMs(this.breakDelay.get().intValue()) || this.instantTimer.get() == PredictTimer.PREDICT && this.predictTimer.passedMs(this.predictDelay.get().intValue()))) {
                if (this.predictSlowBreak(pos.down())) {
                    return;
                }
                if (this.predictFriendDmg.get().booleanValue() && (this.antiFriendPop.get() == AntiFriendPop.BREAK || this.antiFriendPop.get() == AntiFriendPop.ALL) && this.isRightThread()) {
                    for (PlayerEntity friend : mc.world.getPlayers()) {
                        if (friend == null || mc.player.equals(friend) || friend.getBlockPos().getSquaredDistance(pos) > MathUtil.square(this.range.get().floatValue() + this.placeRange.get().floatValue()) || !Friends.get().isFriend(friend) || !((double) DamageUtil.calculateDamage(pos, friend) > (double) getHealth(friend) + 0.5))
                            continue;
                        return;
                    }
                }
                if (placedPos.contains(pos.down())) {
                    float selfDamage;
                    if (this.isRightThread() && this.superSafe.get() != false ? DamageUtil.canTakeDamage(this.suicide.get()) && ((double) (selfDamage = DamageUtil.calculateDamage(pos, mc.player)) - 0.5 > (double) getHealth(mc.player) || selfDamage > this.maxSelfBreak.get().floatValue()) : this.superSafe.get() != false) {
                        return;
                    }
                    this.attackCrystalPredict(packet2.getId(), pos);
                } else if (this.predictCalc.get().booleanValue() && this.isRightThread()) {
                    float selfDamage = -1.0f;
                    if (DamageUtil.canTakeDamage(this.suicide.get())) {
                        selfDamage = DamageUtil.calculateDamage(pos, mc.player);
                    }
                    if ((double) selfDamage + 0.5 < (double) getHealth(mc.player) && selfDamage <= this.maxSelfBreak.get().floatValue()) {
                        for (PlayerEntity player : mc.world.getPlayers()) {
                            float damage;
                            if (!(player.getBlockPos().getSquaredDistance(pos) <= MathUtil.square(this.range.get().floatValue())) || !EntityUtil.isValid(player, this.range.get().floatValue() + this.breakRange.get().floatValue()) || this.antiNaked.get().booleanValue() && DamageUtil.isNaked(player) || !((damage = DamageUtil.calculateDamage(pos, player)) > selfDamage || damage > this.minDamage.get().floatValue() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(damage > getHealth(player)))
                                continue;
                            if (this.predictRotate.get().booleanValue() && this.eventMode.get() != 2 && (this.rotate.get() == Rotate.BREAK || this.rotate.get() == Rotate.ALL)) {
                                this.rotateToPos(pos);
                            }
                            this.attackCrystalPredict(packet2.getId(), pos);
                            break;
                        }
                    }
                }
            }
        } else if (!this.soundConfirm.get().booleanValue() && event.packet instanceof ExplosionS2CPacket) {
           ExplosionS2CPacket packet3 = (ExplosionS2CPacket) event.packet;
            BlockPos pos = new BlockPos((int) packet3.getX(), (int) packet3.getY(), (int) packet3.getZ()).down();
            this.removePos(pos);
        } else if (event.packet instanceof EntitiesDestroyS2CPacket) {
            EntitiesDestroyS2CPacket packet4 = (EntitiesDestroyS2CPacket) event.packet;
            for (int id : packet4.getEntityIds()) {
                Entity entity = mc.world.getEntityById(id);
                if (!(entity instanceof EndCrystalEntity)) continue;
                brokenPos.remove(new BlockPos(entity.getBlockPos()).down());
                placedPos.remove(new BlockPos(entity.getBlockPos()).down());
            }
        } else if (event.packet instanceof EntityStatusS2CPacket) {
           EntityStatusS2CPacket packet5 = (EntityStatusS2CPacket) event.packet;
            if (packet5.hashCode() == 35 && packet5.getEntity(mc.world) instanceof PlayerEntity) {
                this.totemPops.put((PlayerEntity) packet5.getEntity(mc.world), new Timer().reset());
            }
        } else if (event.packet instanceof PlaySoundS2CPacket && (packet = (PlaySoundS2CPacket) event.packet).getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
            BlockPos pos = new BlockPos((int) packet.getX(), (int) packet.getY(), (int) packet.getZ());
            if (this.sound.get().booleanValue() || this.threadMode.get() == ThreadMode.SOUND) {
                 NoSoundLag.removeEntities(packet, this.soundRange.get().floatValue());
            }
            if (this.soundConfirm.get().booleanValue()) {
                this.removePos(pos);
            }
            if (this.threadMode.get() == ThreadMode.SOUND && this.isRightThread() && mc.player != null && mc.player.getBlockPos().getSquaredDistance(pos) < MathUtil.square(this.soundPlayer.get().floatValue())) {
                this.handlePool(true);
            }
        }
    }

    private boolean predictSlowBreak(BlockPos pos) {
        if (this.antiCommit.get().booleanValue() && lowDmgPos.remove(pos)) {
            return this.shouldSlowBreak(false);
        }
        return false;
    }

    private boolean isRightThread() {
        return mc.isOnThread()  || !eventManager.ticksOngoing() && !this.threadOngoing.get();
    }

    private void attackCrystalPredict(int entityID, BlockPos pos) {
        if (!(!this.predictRotate.get().booleanValue() || this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE || this.rotate.get() != Rotate.BREAK && this.rotate.get() != Rotate.ALL)) {
            this.rotateToPos(pos);
        }
        PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(mc.world.getEntityById(entityID), mc.player.isSneaking());
        mc.getNetworkHandler().sendPacket(attackPacket);
        if (this.breakSwing.get().booleanValue()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
        }
        if (this.resetBreakTimer.get().booleanValue()) {
            this.breakTimer.reset();
        }
        this.predictTimer.reset();
    }

    private void removePos(BlockPos pos) {
        if (this.damageSync.get() == DamageSync.PLACE) {
            if (placedPos.remove(pos)) {
                this.posConfirmed = true;
            }
        } else if (this.damageSync.get() == DamageSync.BREAK && brokenPos.remove(pos)) {
            this.posConfirmed = true;
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
if (Feature.fullNullCheck() || placePos != null) {
    vec3.set(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);
    String text = String.valueOf(renderDamage);
        double w = TextRenderer.get().getWidth(text) / 2;
        TextRenderer.get().render(text, -w, 0, damageColor.get(), true);
    if (renderMode.get() == AutoCrystalP.RenderMode.None || !renderDamageText.get()) return;
    if (renderMode.get() == AutoCrystalP.RenderMode.Smooth) {
        if (renderBoxOne == null) return;
        vec3.set(renderBoxOne.minX + 0.5, renderBoxOne.minY + 0.5, renderBoxOne.minZ + 0.5);
    } else vec3.set(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);

    if (NametagUtils.to2D(vec3, damageTextScale.get())) {
        NametagUtils.begin(vec3);
        TextRenderer.get().begin(1, false, true);

//        String text = String.valueOf(renderDamage);
//        double w = TextRenderer.get().getWidth(text) / 2;
        TextRenderer.get().render(text, -w, 0, damageColor.get(), true);

        TextRenderer.get().end();
        NametagUtils.end();
    }
}
    }
//    @SubscribeEvent
//    public void onKeyInput(InputEvent.KeyInputEvent event) {
//        if (Keyboard.getEventKeyState() && !(AutoCrystal.mc.currentScreen instanceof PhobosGui) && this.switchBind.getValue().getKey() == Keyboard.getEventKey()) {
//            if (this.switchBack.getValue().booleanValue() && this.offhandSwitch.getValue().booleanValue() && this.offHand) {
//                Offhand module = Phobos.moduleManager.getModuleByClass(Offhand.class);
//                if (module.isOff()) {
//                    Command.sendMessage("<" + this.getDisplayName() + "> " + "\u00a7c" + "Switch failed. Enable the Offhand module.");
//                } else if (module.type.getValue() == Offhand.Type.NEW) {
//                    module.setSwapToTotem(true);
//                    module.doOffhand();
//                } else {
//                    module.setMode(Offhand.Mode2.TOTEMS);
//                    module.doSwitch();
//                }
//                return;
//            }
//            this.switching = !this.switching;
//        }
//    }

//    @SubscribeEvent
//    public void onSettingChange(ClientEvent event) {
//        if (event.getStage() == 2 && event.getSetting() != null && event.getSetting().getFeature() != null && event.getSetting().getFeature().equals(this) && this.isEnabled() && (event.getSetting().equals(this.threadDelay) || event.getSetting().equals(this.threadMode))) {
//            if (this.executor != null) {
//                this.executor.shutdown();
//            }
//            if (this.thread != null) {
//                this.shouldInterrupt.set(true);
//            }
//        }
//    }

            private void postProcessing () {
                if (this.threadMode.get() != ThreadMode.NONE || this.eventMode.get() != 2 || this.rotate.get() == Rotate.OFF || !this.rotateFirst.get().booleanValue()) {
                    return;
                }
                switch (this.logic.get()) {
                    case BREAKPLACE: {
                        this.postProcessBreak();
                        this.postProcessPlace();
                        break;
                    }
                    case PLACEBREAK: {
                        this.postProcessPlace();
                        this.postProcessBreak();
                    }
                }
            }

            private void postProcessBreak () {
                while (!this.packetUseEntities.isEmpty()) {
                    PlayerInteractEntityC2SPacket packet = this.packetUseEntities.poll();
                    mc.player.networkHandler.sendPacket(packet);
                    if (this.breakSwing.get().booleanValue()) {
                        mc.player.swingHand(Hand.OFF_HAND);
                    }
                    this.breakTimer.reset();
                }
            }

            private void postProcessPlace () {
                if (this.placeInfo != null) {
                    this.placeInfo.runPlace();
                    this.placeTimer.reset();
                    this.placeInfo = null;
                }
            }

            private void processMultiThreading () {
                if (modules.get(AutoCrystalP.class).isActive()) {
                    return;
                }
                if (this.threadMode.get() == ThreadMode.WHILE) {
                    this.handleWhile();
                } else if (this.threadMode.get() != ThreadMode.NONE) {
                    this.handlePool(false);
                }
            }

            private void handlePool ( boolean justDoIt){
                if (justDoIt || this.executor == null || this.executor.isTerminated() || this.executor.isShutdown() || this.syncroTimer.passedMs(this.syncThreads.get().intValue()) && this.syncThreadBool.get().booleanValue()) {
                    if (this.executor != null) {
                        this.executor.shutdown();
                    }
                    this.executor = this.getExecutor();
                    this.syncroTimer.reset();
                }
            }

            private void handleWhile () {
                if (this.thread == null || this.thread.isInterrupted() || !this.thread.isAlive() || this.syncroTimer.passedMs(this.syncThreads.get().intValue()) && this.syncThreadBool.get().booleanValue()) {
                    if (this.thread == null) {
                        this.thread = new Thread(RAutoCrystal.getInstance(this));
                    } else if (this.syncroTimer.passedMs(this.syncThreads.get().intValue()) && !this.shouldInterrupt.get() && this.syncThreadBool.get().booleanValue()) {
                        this.shouldInterrupt.set(true);
                        this.syncroTimer.reset();
                        return;
                    }
                    if (this.thread != null && (this.thread.isInterrupted() || !this.thread.isAlive())) {
                        this.thread = new Thread(RAutoCrystal.getInstance(this));
                    }
                    if (this.thread != null && this.thread.getState() == Thread.State.NEW) {
                        try {
                            this.thread.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        this.syncroTimer.reset();
                    }
                }
            }

            private ScheduledExecutorService getExecutor () {
                ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                service.scheduleAtFixedRate(RAutoCrystal.getInstance(this), 0L, this.threadDelay.get().intValue(), TimeUnit.MILLISECONDS);
                return service;
            }

            public void doAutoCrystal () {
                if (this.brownZombie.get().booleanValue()) {
                    return;
                }
                if (this.check()) {
                    switch (this.logic.get()) {
                        case PLACEBREAK: {
                            this.placeCrystal();
                            this.breakCrystal();
                            break;
                        }
                        case BREAKPLACE: {
                            this.breakCrystal();
                            this.placeCrystal();
                            break;
                        }
                    }
                    this.manualBreaker();
                }
            }

            private boolean check () {
                if (Feature.fullNullCheck()) {
                    return false;
                }
                if (this.syncTimer.passedMs(this.damageSyncTime.get().intValue())) {
                    this.currentSyncTarget = null;
                    this.syncedCrystalPos = null;
                    this.syncedPlayerPos = null;
                } else if (this.syncySync.get().booleanValue() && this.syncedCrystalPos != null) {
                    this.posConfirmed = true;
                }
                this.foundDoublePop = false;
                if (this.renderTimer.passedMs(500L)) {
                    this.renderPos = null;
                    this.renderTimer.reset();
                }
                this.mainHand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
                this.offHand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
                this.currentDamage = 0.0;
                this.placePos = null;
                if (this.lastSlot != mc.player.getInventory().selectedSlot

                    //|| AutoTrap.isPlacing ||
                    //Surround and autotrap
                    //    Surround.isPlacing
                ) {
                    this.lastSlot = mc.player.getInventory().selectedSlot;
                    this.switchTimer.reset();
                }
                if (!this.offHand && !this.mainHand) {
                    this.placeInfo = null;
                    this.packetUseEntities.clear();
                }
                if (this.offHand || this.mainHand) {
                    this.switching = false;
                }
                if (!((this.offHand || this.mainHand || this.switchMode.get() != Switch.BREAKSLOT || this.switching) && DamageUtil.canBreakWeakness(mc.player) && this.switchTimer.passedMs(this.switchCooldown.get().intValue()))) {
                    this.renderPos = null;
                    target = null;
                    this.rotating = false;
                    return false;
                }
//                if (this.mineSwitch.get().booleanValue() && mc.mouse. && (this.switching || this.autoSwitch.get() == ALWAYS) && Mouse.isButtonDown(1) && mc.player.getMainHandStack().getItem() instanceof PickaxeItem) {
//                    this.switchItem();
//                }
                //idk switch
                this.mapCrystals();
                if (!this.posConfirmed && this.damageSync.get() != DamageSync.NONE && this.syncTimer.passedMs(this.confirm.get().intValue())) {
                    this.syncTimer.setMs(this.damageSyncTime.get() + 1);
                }
                return true;
            }

            private void mapCrystals () {
                this.efficientTarget = null;
                if (this.packets.get() != 1) {
                    this.attackList = new ConcurrentLinkedQueue<Entity>();
                    this.crystalMap = new HashMap<Entity, Float>();
                }
                this.crystalCount = 0;
                this.minDmgCount = 0;
                Entity maxCrystal = null;
                float maxDamage = 0.5f;
                for (Entity entity : mc.world.getEntities()) {
                    if (!entity.isAlive() || !(entity instanceof EndCrystalEntity) || !this.isValid(entity)) continue;
                    if (this.syncedFeetPlace.get().booleanValue() && entity.getBlockPos().down().equals(this.syncedCrystalPos) && this.damageSync.get() != DamageSync.NONE) {
                        ++this.minDmgCount;
                        ++this.crystalCount;
                        if (this.syncCount.get().booleanValue()) {
                            this.minDmgCount = this.wasteAmount.get() + 1;
                            this.crystalCount = this.wasteAmount.get() + 1;
                        }
                        if (!this.hyperSync.get().booleanValue()) continue;
                        maxCrystal = null;
                        break;
                    }
                    boolean count = false;
                    boolean countMin = false;
                    float selfDamage = -1.0f;
                    if (DamageUtil.canTakeDamage(this.suicide.get())) {
                        selfDamage = DamageUtil.calculateDamage(entity, mc.player);
                    }
                    if ((double) selfDamage + 0.5 < (double) getHealth(mc.player) && selfDamage <= this.maxSelfBreak.get().floatValue()) {
                        Entity beforeCrystal = maxCrystal;
                        float beforeDamage = maxDamage;
                        for (PlayerEntity player : mc.world.getPlayers()) {
                            float damage;
                            if (!(player.getBlockPos().getSquaredDistance(entity.getPos()) <= MathUtil.square(this.range.get().floatValue())))
                                continue;
                            if (EntityUtil.isValid(player, this.range.get().floatValue() + this.breakRange.get().floatValue())) {
                                if (this.antiNaked.get().booleanValue() && DamageUtil.isNaked(player) || !((damage = DamageUtil.calculateDamage(entity, player)) > selfDamage || damage > this.minDamage.get().floatValue() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(damage > getHealth(player)))
                                    continue;
                                if (damage > maxDamage) {
                                    maxDamage = damage;
                                    maxCrystal = entity;
                                }
                                if (this.packets.get() == 1) {
                                    if (damage >= this.minDamage.get().floatValue() || !this.wasteMinDmgCount.get().booleanValue()) {
                                        count = true;
                                    }
                                    countMin = true;
                                    continue;
                                }
                                if (this.crystalMap.get(entity) != null && !(this.crystalMap.get(entity).floatValue() < damage))
                                    continue;
                                this.crystalMap.put(entity, Float.valueOf(damage));
                                continue;
                            }
                            if (this.antiFriendPop.get() != AntiFriendPop.BREAK && this.antiFriendPop.get() != AntiFriendPop.ALL || !Friends.get().isFriend(player) || !((double) (damage = DamageUtil.calculateDamage(entity, player)) > (double) getHealth(player) + 0.5))
                                continue;
                            maxCrystal = beforeCrystal;
                            maxDamage = beforeDamage;
                            this.crystalMap.remove(entity);
                            if (!this.noCount.get().booleanValue()) break;
                            count = false;
                            countMin = false;
                            break;
                        }
                    }
                    if (!countMin) continue;
                    ++this.minDmgCount;
                    if (!count) continue;
                    ++this.crystalCount;
                }
                if (this.damageSync.get() == DamageSync.BREAK && ((double) maxDamage > this.lastDamage || this.syncTimer.passedMs(this.damageSyncTime.get().intValue()) || this.damageSync.get() == DamageSync.NONE)) {
                    this.lastDamage = maxDamage;
                }
                if (this.enormousSync.get().booleanValue() && this.syncedFeetPlace.get().booleanValue() && this.damageSync.get() != DamageSync.NONE && this.syncedCrystalPos != null) {
                    if (this.syncCount.get().booleanValue()) {
                        this.minDmgCount = this.wasteAmount.get() + 1;
                        this.crystalCount = this.wasteAmount.get() + 1;
                    }
                    return;
                }
                if (this.webAttack.get().booleanValue() && this.webPos != null) {
                    if (mc.player.getBlockPos().getSquaredDistance(this.webPos.up()) > MathUtil.square(this.breakRange.get().floatValue())) {
                        this.webPos = null;
                    } else {
                        try {
                            for (Entity entity : mc.world.getOtherEntities(Entity.class.newInstance(), new Box(this.webPos.up()))) {
                                if (!(entity instanceof EndCrystalEntity)) continue;
                                this.attackList.add(entity);
                                this.efficientTarget = entity;
                                this.webPos = null;
                                this.lastDamage = 0.5;
                                return;
                            }
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (this.shouldSlowBreak(true) && maxDamage < this.minDamage.get().floatValue() && (target == null || !(getHealth(target) <= this.facePlace.get().floatValue()) || !this.breakTimer.passedMs(this.facePlaceSpeed.get().intValue()) && this.slowFaceBreak.get().booleanValue() && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) && this.holdFacePlace.get().booleanValue() && this.holdFaceBreak.get().booleanValue())) {
                    this.efficientTarget = null;
                    return;
                }
                if (this.packets.get() == 1) {
                    this.efficientTarget = maxCrystal;
                } else {
                    this.crystalMap = MathUtil.sortByValue(this.crystalMap, true);
                    for (Map.Entry entry : this.crystalMap.entrySet()) {
                        Entity crystal = (Entity) entry.getKey();
                        float damage = ((Float) entry.getValue()).floatValue();
                        if (damage >= this.minDamage.get().floatValue() || !this.wasteMinDmgCount.get().booleanValue()) {
                            ++this.crystalCount;
                        }
                        this.attackList.add(crystal);
                        ++this.minDmgCount;
                    }
                }
            }

            private boolean shouldSlowBreak ( boolean withManual){
                return withManual && this.manual.get() != false && this.manualMinDmg.get() != false
//                    //LeftButton 1
                && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) && (!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) || this.holdFacePlace.get() == false) || this.holdFacePlace.get() != false && this.holdFaceBreak.get() != false && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) && !this.breakTimer.passedMs(this.facePlaceSpeed.get().intValue()) || this.slowFaceBreak.get() != false && !this.breakTimer.passedMs(this.facePlaceSpeed.get().intValue());
            }


            private void placeCrystal () {

         int crystalLimit = this.wasteAmount.get();
                if (this.placeTimer.passedMs(this.placeDelay.get().intValue()) && this.place.get().booleanValue() && (this.offHand || this.mainHand || this.switchMode.get() == Switch.CALC || this.switchMode.get() == Switch.BREAKSLOT && this.switching)) {
                    if (!(!this.offHand && !this.mainHand && (this.switchMode.get() == Switch.ALWAYS || this.switching) || this.crystalCount < crystalLimit || this.antiSurround.get().booleanValue() && this.lastPos != null && this.lastPos.equals(this.placePos))) {
                        return;
                    }
                    this.calculateDamage(this.getTarget(this.targetMode.get() == Target.UNSAFE));
                    if (target != null && this.placePos != null) {
                        if (!this.offHand && !this.mainHand && this.autoSwitch.get() != AutoSwitch.NONE && (this.currentDamage > (double) this.minDamage.get().floatValue() || this.lethalSwitch.get().booleanValue() && getHealth(target) <= this.facePlace.get().floatValue()) && !this.switchItem()) {
                            return;
                        }
                        if (this.currentDamage < (double) this.minDamage.get().floatValue() && this.limitFacePlace.get().booleanValue()) {
                            crystalLimit = 1;
                        }
                        if (this.currentDamage >= (double) this.minMinDmg.get().floatValue() && (this.offHand || this.mainHand || this.autoSwitch.get() != AutoSwitch.NONE) && (this.crystalCount < crystalLimit || this.antiSurround.get().booleanValue() && this.lastPos != null && this.lastPos.equals(this.placePos)) && (this.currentDamage > (double) this.minDamage.get().floatValue() || this.minDmgCount < crystalLimit) && this.currentDamage >= 1.0 && (DamageUtil.isArmorLow(target, this.minArmor.get()) || getHealth(target) <= this.facePlace.get().floatValue() || this.currentDamage > (double) this.minDamage.get().floatValue() || this.shouldHoldFacePlace())) {
                            float damageOffset = this.damageSync.get() == DamageSync.BREAK ? this.dropOff.get().floatValue() - 5.0f : 0.0f;
                            boolean syncflag = false;
                            if (this.syncedFeetPlace.get().booleanValue() && this.placePos.equals(this.lastPos) && this.isEligableForFeetSync(target, this.placePos) && !this.syncTimer.passedMs(this.damageSyncTime.get().intValue()) && target.equals(this.currentSyncTarget) && target.getPos().equals(this.syncedPlayerPos) && this.damageSync.get() != DamageSync.NONE) {
                                this.syncedCrystalPos = this.placePos;
                                this.lastDamage = this.currentDamage;
                                if (this.fullSync.get().booleanValue()) {
                                    this.lastDamage = 100.0;
                                }
                                syncflag = true;
                            }
                            if (syncflag || this.currentDamage - (double) damageOffset > this.lastDamage || this.syncTimer.passedMs(this.damageSyncTime.get().intValue()) || this.damageSync.get() == DamageSync.NONE) {
                                if (!syncflag && this.damageSync.get() != DamageSync.BREAK) {
                                    this.lastDamage = this.currentDamage;
                                }
                                this.renderPos = this.placePos;
                                this.renderDamage = this.currentDamage;
                                if (this.switchItem()) {
                                    this.currentSyncTarget = target;
                                    this.syncedPlayerPos = BlockPos.ofFloored(target.getPos());
                                    if (this.foundDoublePop) {
                                        this.totemPops.put(target, new Timer().reset());
                                    }
                                    this.rotateToPos(this.placePos);
                                    if (this.addTolowDmg || this.actualSlowBreak.get().booleanValue() && this.currentDamage < (double) this.minDamage.get().floatValue()) {
                                        lowDmgPos.add(this.placePos);
                                    }
                                    placedPos.add(this.placePos);
                                    if (!this.justRender.get().booleanValue()) {
                                        if (this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE && this.rotateFirst.get().booleanValue() && this.rotate.get() != Rotate.OFF) {
                                            this.placeInfo = new PlaceInfo(this.placePos, this.offHand, this.placeSwing.get(), this.exactHand.get());
                                        } else {
                                            BlockUtil.placeCrystalOnBlock(this.placePos, this.offHand ? Hand.OFF_HAND : Hand.MAIN_HAND, this.placeSwing.get(), this.exactHand.get());
                                        }
                                    }
                                    this.lastPos = this.placePos;
                                    this.placeTimer.reset();
                                    this.posConfirmed = false;
                                    if (this.syncTimer.passedMs(this.damageSyncTime.get().intValue())) {
                                        this.syncedCrystalPos = null;
                                        this.syncTimer.reset();
                                    }
                                }
                            }
                        }
                    } else {
                        this.renderPos = null;
                    }
                }
            }

            private boolean shouldHoldFacePlace () {
                this.addTolowDmg = false;
                if (this.holdFacePlace.get().booleanValue() && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                    this.addTolowDmg = true;
                    return true;
                }
                return false;
            }

            private boolean switchItem () {
                if (this.offHand || this.mainHand) {
                    return true;
                }
                switch (this.autoSwitch.get()) {
                    case NONE: {
                        return false;
                    }
                    case TOGGLE: {
                        if (!this.switching) {
                            return false;
                        }
                    }
//                    case ALWAYS: {
////                        if (!this.doSwitch()) break;
//                        return true;
//                    }
                }
                return false;
            }

//            private boolean doSwitch () {
//                if (this.offhandSwitch.get().booleanValue()) {
//                    Offhand module = Phobos.moduleManager.getModuleByClass(Offhand.class);
//                    if (module.isOff()) {
//                        Command.sendMessage("<" + this.getDisplayName() + "> " + "\u00a7c" + "Switch failed. Enable the Offhand module.");
//                        this.switching = false;
//                        return false;
//                    }
//                    if (module.type.get() == Offhand.Type.NEW) {
//                        module.setSwapToTotem(false);
//                        module.setMode(Offhand.Mode.CRYSTALS);
//                        module.doOffhand();
//                    } else {
//                        module.setMode(Offhand.Mode2.CRYSTALS);
//                        module.doSwitch();
//                    }
//                    this.switching = false;
//                    return true;
//                }
//                if (mc.player.getInventory().offHand.get() == Items.END_CRYSTAL) {
//                    this.mainHand = false;
//                } else {
//                    InventoryUtil.switchToHotbarSlot(EndCrystalItem.class, false);
//                    this.mainHand = true;
//                }
//                this.switching = false;
//                return true;
//            }

            private void calculateDamage (PlayerEntity targettedPlayer){
                BlockPos playerPos;
                Block web;
                if (targettedPlayer == null && this.targetMode.get() != Target.DAMAGE && !this.fullCalc.get().booleanValue()) {
                    return;
                }
                float maxDamage = 0.5f;
                PlayerEntity currentTarget = null;
                BlockPos currentPos = null;
                float maxSelfDamage = 0.0f;
                this.foundDoublePop = false;
                BlockPos setToAir = null;
                BlockState state = null;
                if (this.webAttack.get().booleanValue() && targettedPlayer != null && (web = mc.world.getBlockState(playerPos = new BlockPos(targettedPlayer.getBlockPos())).getBlock()) == Blocks.COBWEB) {
                    setToAir = playerPos;
                    state = mc.world.getBlockState(playerPos);
                    mc.world.removeBlock(playerPos, false);
                }
                block0:
                for (BlockPos pos : BlockUtil.possiblePlacePositions(this.placeRange.get().floatValue(), this.antiSurround.get(), this.oneDot15.get())) {
                    if (!BlockUtil.rayTracePlaceCheck(pos, (this.raytrace.get() == Raytrace.PLACE || this.raytrace.get() == Raytrace.FULL) && mc.player.getBlockPos().getSquaredDistance(pos) > MathUtil.square(this.placetrace.get().floatValue()), 1.0f))
                        continue;
                    float selfDamage = -1.0f;
                    if (DamageUtil.canTakeDamage(this.suicide.get())) {
                        selfDamage = DamageUtil.calculateDamage(pos, mc.player);
                    }
                    if (!((double) selfDamage + 0.5 < (double)  getHealth(mc.player)) || !(selfDamage <= this.maxSelfPlace.get().floatValue()))
                        continue;
                    if (targettedPlayer != null) {
                        float playerDamage = DamageUtil.calculateDamage(pos, targettedPlayer);
                        if (this.calcEvenIfNoDamage.get().booleanValue() && (this.antiFriendPop.get() == AntiFriendPop.ALL || this.antiFriendPop.get() == AntiFriendPop.PLACE)) {
                            boolean friendPop = false;
                            for (PlayerEntity friend : mc.world.getPlayers()) {
                                float friendDamage;
                                if (friend == null || mc.player.equals(friend) || friend.getBlockPos().getSquaredDistance(pos) > MathUtil.square(this.range.get().floatValue() + this.placeRange.get().floatValue()) || !Friends.get().isFriend((PlayerEntity) friend) || !((double) (friendDamage = DamageUtil.calculateDamage(pos, friend)) > (double) getHealth(friend) + 0.5))
                                    continue;
                                friendPop = true;
                                break;
                            }
                            if (friendPop) continue;
                        }
                        if (this.isDoublePoppable(targettedPlayer, playerDamage) && (currentPos == null || targettedPlayer.getBlockPos().getSquaredDistance(pos) < targettedPlayer.getBlockPos().getSquaredDistance(currentPos))) {
                            currentTarget = targettedPlayer;
                            maxDamage = playerDamage;
                            currentPos = pos;
                            this.foundDoublePop = true;
                            continue;
                        }
                        if (this.foundDoublePop || !(playerDamage > maxDamage) && (!this.extraSelfCalc.get().booleanValue() || !(playerDamage >= maxDamage) || !(selfDamage < maxSelfDamage)) || !(playerDamage > selfDamage || playerDamage > this.minDamage.get().floatValue() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(playerDamage > getHealth(targettedPlayer)))
                            continue;
                        maxDamage = playerDamage;
                        currentTarget = targettedPlayer;
                        currentPos = pos;
                        maxSelfDamage = selfDamage;
                        continue;
                    }
                    float maxDamageBefore = maxDamage;
                    PlayerEntity currentTargetBefore = currentTarget;
                    BlockPos currentPosBefore = currentPos;
                    float maxSelfDamageBefore = maxSelfDamage;
                    for (PlayerEntity player : mc.world.getPlayers()) {
                        float friendDamage;
                        if (EntityUtil.isValid(player, this.placeRange.get().floatValue() + this.range.get().floatValue())) {
                            if (this.antiNaked.get().booleanValue() && DamageUtil.isNaked(player)) continue;
                            float playerDamage = DamageUtil.calculateDamage(pos, player);
                            if (this.doublePopOnDamage.get().booleanValue() && this.isDoublePoppable(player, playerDamage) && (currentPos == null || player.getBlockPos().getSquaredDistance(pos) < player.getBlockPos().getSquaredDistance(currentPos))) {
                                currentTarget = player;
                                maxDamage = playerDamage;
                                currentPos = pos;
                                maxSelfDamage = selfDamage;
                                this.foundDoublePop = true;
                                if (this.antiFriendPop.get() != AntiFriendPop.BREAK && this.antiFriendPop.get() != AntiFriendPop.PLACE)
                                    continue;
                                continue block0;
                            }
                            if (this.foundDoublePop || !(playerDamage > maxDamage) && (!this.extraSelfCalc.get().booleanValue() || !(playerDamage >= maxDamage) || !(selfDamage < maxSelfDamage)) || !(playerDamage > selfDamage || playerDamage > this.minDamage.get().floatValue() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(playerDamage > getHealth(player)))
                                continue;
                            maxDamage = playerDamage;
                            currentTarget = player;
                            currentPos = pos;
                            maxSelfDamage = selfDamage;
                            continue;
                        }
                        if (this.antiFriendPop.get() != AntiFriendPop.ALL && this.antiFriendPop.get() != AntiFriendPop.PLACE || player == null || !(player.getBlockPos().getSquaredDistance(pos) <= MathUtil.square(this.range.get().floatValue() + this.placeRange.get().floatValue())) || !Friends.get().isFriend(player) || !((double) (friendDamage = DamageUtil.calculateDamage(pos, player)) > (double) getHealth(player) + 0.5))
                            continue;
                        maxDamage = maxDamageBefore;
                        currentTarget = currentTargetBefore;
                        currentPos = currentPosBefore;
                        maxSelfDamage = maxSelfDamageBefore;
                        continue block0;
                    }
                }
                if (setToAir != null) {
                    mc.world.setBlockState(setToAir, state);
                    this.webPos = currentPos;
                }
                target = currentTarget;
                this.currentDamage = maxDamage;
                this.placePos = currentPos;
            }
            private PlayerEntity getTarget (boolean unsafe){
                if (this.targetMode.get() == Target.DAMAGE) {
                    return null;
                }
                PlayerEntity currentTarget = null;
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (EntityUtil.isntValid(player, this.placeRange.get().floatValue() + this.range.get().floatValue()) || this.antiNaked.get().booleanValue() && DamageUtil.isNaked(player) || unsafe && EntityUtil.isSafe(player))
                        continue;
                    if (this.minArmor.get() > 0 && DamageUtil.isArmorLow(player, this.minArmor.get())) {
                        currentTarget = player;
                        break;
                    }
                    if (currentTarget == null) {
                        currentTarget = player;
                        continue;
                    }
                    if (!(mc.player.getBlockPos().getSquaredDistance(player.getPos()) < mc.player.getBlockPos().getSquaredDistance(currentTarget.getPos())))
                        continue;
                    currentTarget = player;
                }
                if (unsafe && currentTarget == null) {
                    return this.getTarget(false);
                }
                if (this.predictPos.get().booleanValue() && currentTarget != null) {
                    GameProfile profile = new GameProfile(currentTarget.getUuid() == null ? UUID.fromString("8af022c8-b926-41a0-8b79-2b544ff00fcf") : currentTarget.getUuid(), currentTarget.getName().toString());
                    OtherClientPlayerEntity newTarget = new OtherClientPlayerEntity(mc.world, profile);
                    Vec3d extrapolatePosition = MathUtil.extrapolatePlayerPosition(currentTarget, this.predictTicks.get());
                    newTarget.copyPositionAndRotation(currentTarget);
                    extrapolatePosition = newTarget.getPos();
//                   newTarget.getX() = extrapolatePosition.x;
//                    newTarget.getY() = extrapolatePosition.y;
//                    newTarget.getZ() = extrapolatePosition.z;
                    newTarget.setHealth(getHealth(currentTarget));
                    inventoryManager.copyInventory(currentTarget.getInventory());
                    currentTarget = newTarget;
                }
                return currentTarget;
            }

            private void breakCrystal () {
                if (this.explode.get().booleanValue() && this.breakTimer.passedMs(this.breakDelay.get().intValue()) && (this.switchMode.get() == Switch.ALWAYS || this.mainHand || this.offHand)) {
                    if (this.packets.get() == 1 && this.efficientTarget != null) {
                        if (this.justRender.get().booleanValue()) {
                            this.doFakeSwing();
                            return;
                        }
                        if (this.syncedFeetPlace.get().booleanValue() && this.gigaSync.get().booleanValue() && this.syncedCrystalPos != null && this.damageSync.get() != DamageSync.NONE) {
                            return;
                        }
                        this.rotateTo(this.efficientTarget);
                        this.attackEntity(this.efficientTarget);
                        this.breakTimer.reset();
                    } else if (!this.attackList.isEmpty()) {
                        if (this.justRender.get().booleanValue()) {
                            this.doFakeSwing();
                            return;
                        }
                        if (this.syncedFeetPlace.get().booleanValue() && this.gigaSync.get().booleanValue() && this.syncedCrystalPos != null && this.damageSync.get() != DamageSync.NONE) {
                            return;
                        }
                        for (int i = 0; i < this.packets.get(); ++i) {
                            Entity entity = this.attackList.poll();
                            if (entity == null) continue;
                            this.rotateTo(entity);
                            this.attackEntity(entity);
                        }
                        this.breakTimer.reset();
                    }
                }
            }

            private void attackEntity (Entity entity){
                if (entity != null) {
                    if (this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE && this.rotateFirst.get().booleanValue() && this.rotate.get() != Rotate.OFF) {
//                        this.packetUseEntities.add(new PlayerInteractEntityC2SPacket(entity));
                    } else {
                        EntityUtil.attackEntity(entity, this.sync.get(), this.breakSwing.get());
                        brokenPos.add(new BlockPos(entity.getBlockPos()).down());
                    }
                }
            }
            //Meteor ones:
//    private void attackCrystal(Entity entity) {
//        // Attack
//        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
//
//        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
//        if (hand == null) hand = Hand.MAIN_HAND;
////
//        if (fakeSwing.get()) mc.player.swingHand(hand);
//        if (renderSwing.get()) mc.player.swingHand(hand);
//        if (placeSwing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
//        if (breakSwing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
//
////        attacks++;
//    }
            private void doFakeSwing () {
                if (this.fakeSwing.get().booleanValue()) {
                    EntityUtil.swingArmNoPacket(Hand.MAIN_HAND, mc.player);
                }
            }

            private void manualBreaker () {
                RayTraceResult result;
                if (this.rotate.get() != Rotate.OFF && this.eventMode.get() != 2 && this.rotating) {
                    if (this.didRotation) {
                        float pitchDidRotation = mc.player.getPitch();
                        pitchDidRotation = (float) ((double) mc.player.getPitch() + 4.0E-4);
                        this.didRotation = false;
                    } else {
                        float pitchManualBreaker = mc.player.getPitch();
                        pitchManualBreaker = (float) ((double) mc.player.getPitch() - 4.0E-4);
                        this.didRotation = true;
                    }
                }
                if ((this.offHand || this.mainHand) && this.manual.get().booleanValue() && this.manualTimer.passedMs(this.manualBreak.get().intValue()) && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.field_31997) && mc.player.getOffHandStack().getItem() != Items.GOLDEN_APPLE && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.GOLDEN_APPLE && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.BOW && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.EXPERIENCE_BOTTLE && (result = BlockUtil.objectMouseOver) != null) {
                    switch (result.typeOfHit) {
                        case ENTITY: {
                            Entity entity = result.entityHit;
                            if (!(entity instanceof EndCrystalEntity)) break;
                            EntityUtil.attackEntity(entity, this.sync.get(), this.breakSwing.get());
                            this.manualTimer.reset();
                            break;
                        }
                        case BLOCK: {
                            BlockPos mousePos = result.getBlockPos().up();
                            for (Entity target : mc.world.getOtherEntities(null, new Box(mousePos))) {
                                if (!(target instanceof EndCrystalEntity)) continue;
                                EntityUtil.attackEntity(target, this.sync.get(), this.breakSwing.get());
                                this.manualTimer.reset();
                            }
                            break;
                        }
                    }
                }
            }

            private void rotateTo (Entity entity){
                switch (this.rotate.get()) {
                    case OFF: {
                        this.rotating = false;
                    }
                    case PLACE: {
                        break;
                    }
                    case BREAK:
                    case ALL: {
                        float[] angle = MathUtil.calcAngle(mc.player.getEyePos(), entity.getPos());
                    if (this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE) {
                            rotationManager.setPlayerRotations(angle[0], angle[1]);
                            break;
                        }
                        this.yaw = angle[0];
                        this.pitch = angle[1];
                        this.rotating = true;
                    }
                }
            }

            private void rotateToPos (BlockPos pos){
                switch (this.rotate.get()) {
                    case OFF: {
                        this.rotating = false;
                    }
                    case BREAK: {
                        break;
                    }
                    case PLACE:
                    case ALL: {
                        float[] angle = MathUtil.calcAngle(mc.player.getEyePos(), new Vec3d((float) pos.getX() + 0.5f, (float) pos.getY() - 0.5f, (float) pos.getZ() + 0.5f));
                        if (this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE) {
                            rotationManager.setPlayerRotations(angle[0], angle[1]);
                            break;
                        }
                        this.yaw = angle[0];
                        this.pitch = angle[1];
                        this.rotating = true;
                    }
                }
            }

            private boolean isDoublePoppable (PlayerEntity player,float damage){
                float health;
                if (this.doublePop.get().booleanValue() && (double) (health = getHealth(player)) <= this.popHealth.get() && (double) damage > (double) health + 0.5 && damage <= this.popDamage.get().floatValue()) {
                    Timer timer = this.totemPops.get(player);
                    return timer == null || timer.passedMs(this.popTime.get().intValue());
                }
                return false;
            }

            private boolean isValid (Entity entity){
                return entity != null && mc.player.getBlockPos().getSquaredDistance(entity.getPos()) <= MathUtil.square(this.breakRange.get().floatValue()) && (this.raytrace.get() == Raytrace.NONE || this.raytrace.get() == Raytrace.PLACE || mc.player.canSee(entity) || !mc.player.canSee(entity) && mc.player.getBlockPos().getSquaredDistance(entity.getPos()) <= MathUtil.square(this.breaktrace.get().floatValue()));
            }

            private boolean isEligableForFeetSync (PlayerEntity player, BlockPos pos){
                if (this.holySync.get().booleanValue()) {
                    BlockPos playerPos = new BlockPos(player.getBlockPos());
                    for (Direction facing : Direction.values()) {
                        BlockPos holyPos;
                        if (facing == Direction.DOWN || facing == Direction.UP || !pos.equals(holyPos = playerPos.down().offset(facing)))
                            continue;
                        return true;
                    }
                    return false;
                }
                return true;
            }

            public enum PredictTimer {
                NONE,
                BREAK,
                PREDICT

            }

            public enum AntiFriendPop {
                NONE,
                PLACE,
                BREAK,
                ALL

            }

            public enum ThreadMode {
                NONE,
                POOL,
                SOUND,
                WHILE

            }

            public enum AutoSwitch {
                NONE,
                TOGGLE,
                ALWAYS

            }

            public enum Raytrace {
                NONE,
                PLACE,
                BREAK,
                FULL

            }

            public enum Switch {
                ALWAYS,
                BREAKSLOT,
                CALC

            }

            public enum Logic {
                BREAKPLACE,
                PLACEBREAK

            }

            public enum Target {
                CLOSEST,
                UNSAFE,
                DAMAGE

            }

            public enum Rotate {
                OFF,
                PLACE,
                BREAK,
                ALL

            }

            public enum DamageSync {
                NONE,
                PLACE,
                BREAK

            }

            public enum Settings {
                PLACE,
                BREAK,
                RENDER,
                MISC,
                DEV

            }
            public enum RenderMode {
                Normal,
                Smooth,
                Fading,
                Gradient,
                None
            }
            public static class PlaceInfo {
                private final BlockPos pos;
                private final boolean offhand;
                private final boolean placeSwing;
                private final boolean exactHand;

                public PlaceInfo(BlockPos pos, boolean offhand, boolean placeSwing, boolean exactHand) {
                    this.pos = pos;
                    this.offhand = offhand;
                    this.placeSwing = placeSwing;
                    this.exactHand = exactHand;
                }

                public void runPlace() {
                    BlockUtil.placeCrystalOnBlock(this.pos, this.offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, this.placeSwing, this.exactHand);
                }
            }

            private static class RAutoCrystal
                implements Runnable {
                private static RAutoCrystal instance;
                private AutoCrystalP autoCrystal;

                private RAutoCrystal() {
                }

                public static RAutoCrystal getInstance(AutoCrystalP autoCrystal) {
                    if (instance == null) {
                        instance = new RAutoCrystal();
                        RAutoCrystal.instance.autoCrystal = autoCrystal;
                    }
                    return instance;
                }

                @Override
                public void run() {
                    if (this.autoCrystal.threadMode.get() == ThreadMode.WHILE) {
                        Modules modules = Modules.get();
                        while (modules.get(AutoCrystalP.class).isActive() && this.autoCrystal.threadMode.get() == ThreadMode.WHILE) {
                            while (eventManager.ticksOngoing()) {
                            }
                            if (this.autoCrystal.shouldInterrupt.get()) {
                                this.autoCrystal.shouldInterrupt.set(false);
                                this.autoCrystal.syncroTimer.reset();
                                this.autoCrystal.thread.interrupt();
                                break;
                            }
                            this.autoCrystal.threadOngoing.set(true);
                            safetyManager.doSafetyCheck();
                            this.autoCrystal.doAutoCrystal();
                            this.autoCrystal.threadOngoing.set(false);
                            try {
                                Thread.sleep(this.autoCrystal.threadDelay.get().intValue());
                            } catch (InterruptedException e) {
                                this.autoCrystal.thread.interrupt();
                                e.printStackTrace();
                            }
                        }
                    } else if (this.autoCrystal.threadMode.get() != ThreadMode.NONE && modules.get(AutoCrystalP.class).isActive()) {
                        while (eventManager.ticksOngoing()) {
                        }
                    this.autoCrystal.threadOngoing.set(true);
                        safetyManager.doSafetyCheck();
                        this.autoCrystal.doAutoCrystal();
                        this.autoCrystal.threadOngoing.set(false);
                    }
                }
            }
        }
