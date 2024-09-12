package com.example.addon.features.modules;

import com.example.addon.Addon;
import com.example.addon.event.events.UpdateWalkingPlayerEvent;
import com.example.addon.mixin.PlayerMoveC2SPacketAccessor;
import com.example.addon.util.*;
import com.example.addon.util.Timer;
import com.mojang.authlib.GameProfile;
import io.netty.util.internal.ConcurrentSet;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import static com.example.addon.manager.Managers.*;
import static com.example.addon.util.EntityUtil.getHealth;

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
    public static Set<BlockPos> placedPos = new HashSet<>();
    public static Set<BlockPos> brokenPos = new HashSet<>();
    private static AutoCrystalP instance;
    private final Timer switchTimer = new Timer();
    public final Timer threadTimer = new Timer();
    private final Timer manualTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer syncTimer = new Timer();
    private final Timer predictTimer = new Timer();
    private final Timer renderTimer = new Timer();
    private final AtomicBoolean shouldInterrupt = new AtomicBoolean(false);
    private final Timer syncroTimer = new Timer();
    private final Map<PlayerEntity, Timer> totemPops = new ConcurrentHashMap<>();
    private final Queue<PlayerInteractEntityC2SPacket> packetUseEntities = new LinkedList<>();
    private final AtomicBoolean threadOngoing = new AtomicBoolean(false);

    ///DEV///
//    private final Setting<Boolean> attackOppositeHand = sgDev.add(new BoolSetting.Builder()
//        .name("opposite-hand")
//        .description("")
//        .defaultValue(false)
//        .build()
//    );
    private final Setting<Boolean> removeAfterAttack = sgDev.add(new BoolSetting.Builder()
        .name("attack-remove")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> antiBlock = sgDev.add(new BoolSetting.Builder()
        .name("anti-feet-place")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> eventMode = sgDev.add(new IntSetting.Builder()
        .name("updates")
        .description("Updates")
        .defaultValue(3)
        .min(1)
        .max(3)
        .sliderRange(1, 3)
        .build()
    );
    private final Setting<Double> minMinDmg = sgDev.add(new DoubleSetting.Builder()
        .name("min-min-dmg")
        .description("")
        .min(0.0).max(3.0)
        .sliderRange(0.0, 3.0)
        .defaultValue(0.0)
        .visible(() -> this.place.get())
        .build()
    );
    private final Setting<Boolean> breakSwing = sgDev.add(new BoolSetting.Builder()
        .name("break-swing")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> placeSwing = sgDev.add(new BoolSetting.Builder()
        .name("place-swing")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> exactHand = sgDev.add(new BoolSetting.Builder()
        .name("exact-hand")
        .description("")
        .defaultValue(false)
        .visible(this.placeSwing::get)
        .build()
    );
    private final Setting<Boolean> justRender = sgDev.add(new BoolSetting.Builder()
        .name("just-render")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> fakeSwing = sgDev.add(new BoolSetting.Builder()
        .name("fake-swing")
        .description("")
        .defaultValue(false)
        .visible(this.justRender::get)
        .build()
    );
    private final Setting<Logic> logic = sgDev.add(new EnumSetting.Builder<Logic>()
        .name("logic")
        .description("Logic")
        .defaultValue(Logic.BREAKPLACE)
        .build()
    );
    private final Setting<DamageSync> damageSync = sgDev.add(new EnumSetting.Builder<DamageSync>()
        .name("damage-sync")
        .description("")
        .defaultValue(DamageSync.NONE)
        .build()
    );
    private final Setting<Integer> damageSyncTime = sgDev.add(new IntSetting.Builder()
        .name("sync-delay")
        .description("")
        .defaultValue(500)
        .min(0)
        .max(500)
        .sliderRange(0, 500)
        .visible(() -> this.damageSync.get() != DamageSync.NONE)
        .build()
    );
    private final Setting<Double> dropOff = sgDev.add(new DoubleSetting.Builder()
        .name("drop-fff")
        .description("")
        .min(0.0)
        .sliderMax(10.0)
        .defaultValue(5.0)
        .visible(() -> this.damageSync.get() == DamageSync.BREAK)
        .build()
    );
    private final Setting<Integer> confirm = sgDev.add(new IntSetting.Builder()
        .name("confirm")
        .description("")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .visible(() -> this.damageSync.get() != DamageSync.NONE)
        .build()
    );
    //TODO Make a list for syncs
    private final Setting<Boolean> syncedFeetPlace = sgDev.add(new BoolSetting.Builder()
        .name("feet-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE)
        .build()
    );
    private final Setting<Boolean> fullSync = sgDev.add(new BoolSetting.Builder()
        .name("full-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> syncCount = sgDev.add(new BoolSetting.Builder()
        .name("sync-count")
        .description("")
        .defaultValue(true)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> hyperSync = sgDev.add(new BoolSetting.Builder()
        .name("hyper-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> gigaSync = sgDev.add(new BoolSetting.Builder()
        .name("giga-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> syncySync = sgDev.add(new BoolSetting.Builder()
        .name("syncy-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> enormousSync = sgDev.add(new BoolSetting.Builder()
        .name("enormous-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> holySync = sgDev.add(new BoolSetting.Builder()
        .name("unbelievable-sync")
        .description("")
        .defaultValue(false)
        .visible(() -> this.damageSync.get() != DamageSync.NONE && this.syncedFeetPlace.get())
        .build()
    );
    private final Setting<Boolean> rotateFirst = sgDev.add(new BoolSetting.Builder()
        .name("first-rotation")
        .description("")
        .defaultValue(false)
        .visible(() -> this.rotate.get() != Rotate.OFF && this.eventMode.get() == 2)
        .build()
    );
    public final Setting<ThreadMode> threadMode = sgDev.add(new EnumSetting.Builder<ThreadMode>()
        .name("thread")
        .description("")
        .defaultValue(ThreadMode.NONE)
        .build()
    );
    private final Setting<Integer> threadDelay = sgDev.add(new IntSetting.Builder()
        .name("thread-delay")
        .description("")
        .defaultValue(50)
        .min(1)
        .sliderMax(1000)
        .visible(() -> this.threadMode.get() != ThreadMode.NONE)
        .build()
    );
    private final Setting<Boolean> syncThreadBool = sgDev.add(new BoolSetting.Builder()
        .name("thread-sync")
        .description("")
        .defaultValue(true)
        .visible(() -> this.threadMode.get() != ThreadMode.NONE)
        .build()
    );
    private final Setting<Integer> syncThreads = sgDev.add(new IntSetting.Builder()
        .name("sync-threads")
        .description("")
        .defaultValue(1000)
        .min(1)
        .sliderMax(10000)
        .visible(() -> this.threadMode.get() != ThreadMode.NONE && this.syncThreadBool.get())
        .build()
    );
    private final Setting<Boolean> predictPos = sgDev.add(new BoolSetting.Builder()
        .name("predict-pos")
        .description("")
        .defaultValue(false)
        .build()
    );
    //TODO
    private final Setting<Boolean> renderExtrapolation = sgDev.add(new BoolSetting.Builder()
        .name("render-extrapolation")
        .description("send help")
        .defaultValue(false)
        .visible(this.predictPos::get)
        .build()
    );
    private final Setting<Integer> predictTicks = sgDev.add(new IntSetting.Builder()
        .name("extrapolation-ticks")
        .description("")
        .defaultValue(2)
        .min(1)
        .sliderMax(20)
        .visible(this.predictPos::get)
        .build()
    );
    private final Setting<Integer> rotations = sgDev.add(new IntSetting.Builder()
        .name("spoofs")
        .description("")
        .defaultValue(1)
        .min(1)
        .sliderMax(20)
        .build()
    );
    private final Setting<Boolean> predictRotate = sgDev.add(new BoolSetting.Builder()
        .name("predict-rotate")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> predictOffset = sgDev.add(new DoubleSetting.Builder()
        .name("predict-offset")
        .description("")
        .min(0.0)
        .sliderMax(4.0)
        .defaultValue(0.0)
        .build()
    );
    ///MISC///
    private final Setting<Integer> switchCooldown = sgMisc.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Cooldown")
        .defaultValue(500)
        .min(0)
        .max(1000)
        .sliderRange(0, 1000)
        .build()
    );
    private final Setting<Raytrace> raytrace = sgMisc.add(new EnumSetting.Builder<Raytrace>()
        .name("raytrace")
        .description("Raytrace")
        .defaultValue(Raytrace.NONE)
        .build()
    );
    private final Setting<Boolean> holdFacePlace = sgMisc.add(new BoolSetting.Builder()
        .name("hold-face-place")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> holdFaceBreak = sgMisc.add(new BoolSetting.Builder()
        .name("hold-slow-break")
        .description("")
        .defaultValue(false)
        .visible(this.holdFacePlace::get)
        .build()
    );
    private final Setting<Boolean> slowFaceBreak = sgMisc.add(new BoolSetting.Builder()
        .name("slow-face-break")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> actualSlowBreak = sgMisc.add(new BoolSetting.Builder()
        .name("actually-slow")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> facePlaceSpeed = sgMisc.add(new IntSetting.Builder()
        .name("face-speed")
        .description("")
        .defaultValue(500)
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Boolean> antiNaked = sgMisc.add(new BoolSetting.Builder()
        .name("anti-naked")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> range = sgMisc.add(new DoubleSetting.Builder()
        .name("range")
        .description("")
        .min(0.1)
        .sliderMax(20.0)
        .defaultValue(12.0)
        .build()
    );
    private final Setting<Target> targetMode = sgMisc.add(new EnumSetting.Builder<Target>()
        .name("target")
        .description("")
        .defaultValue(Target.CLOSEST)
        .build()
    );
    private final Setting<Integer> minArmor = sgMisc.add(new IntSetting.Builder()
        .name("min-armor")
        .description("")
        .defaultValue(5)
        .min(0)
        .sliderMax(125)
        .build()
    );
    private final Setting<AutoSwitch> autoSwitch = sgMisc.add(new EnumSetting.Builder<AutoSwitch>()
        .name("switch")
        .description("")
        .defaultValue(AutoSwitch.TOGGLE)
        .build()
    );
    //TODO : find use + fix
    private final Setting<Keybind> switchBind = sgMisc.add(new KeybindSetting.Builder()
        .name("switch-bind")
        .description("")
        .defaultValue(Keybind.none())
        .visible(() -> this.autoSwitch.get() == AutoSwitch.TOGGLE)
        .build()
    );
    private final Setting<Boolean> switchBack = sgMisc.add(new BoolSetting.Builder()
        .name("switchback")
        .description("")
        .defaultValue(true)
        .visible(() -> this.autoSwitch.get() != AutoSwitch.NONE && this.offhandSwitch.get())
        .build()
    );
    private final Setting<Boolean> lethalSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("lethal-switch")
        .description("")
        .defaultValue(false)
        .visible(() -> this.autoSwitch.get() != AutoSwitch.NONE)
        .build()
    );
    private final Setting<Boolean> mineSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("mine-switch")
        .description("")
        .defaultValue(true)
        .visible(() -> this.autoSwitch.get() != AutoSwitch.NONE)
        .build()
    );
    private final Setting<Rotate> rotate = sgMisc.add(new EnumSetting.Builder<Rotate>()
        .name("rotate")
        .description("")
        .defaultValue(Rotate.OFF)
        .build()
    );
    private final Setting<Boolean> suicide = sgMisc.add(new BoolSetting.Builder()
        .name("suicide")
        .description("")
        .defaultValue(false)
        .visible(() -> this.targetMode.get() != Target.DAMAGE)
        .build()
    );
    private final Setting<Boolean> webAttack = sgMisc.add(new BoolSetting.Builder()
        .name("web-attack")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> fullCalc = sgMisc.add(new BoolSetting.Builder()
        .name("extra-calc")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> sound = sgMisc.add(new BoolSetting.Builder()
        .name("sound")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> soundRange = sgMisc.add(new DoubleSetting.Builder()
        .name("sound-range")
        .description("")
        .min(0.0)
        .sliderMax(12.0)
        .defaultValue(12.0)
        .build()
    );
    private final Setting<Double> soundPlayer = sgMisc.add(new DoubleSetting.Builder()
        .name("sound-player")
        .description("")
        .min(0.0)
        .sliderMax(12.0)
        .defaultValue(6.0)
        .build()
    );
    private final Setting<Boolean> soundConfirm = sgMisc.add(new BoolSetting.Builder()
        .name("sound-confirm")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> extraSelfCalc = sgMisc.add(new BoolSetting.Builder()
        .name("min-self-dmg")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<AntiFriendPop> antiFriendPop = sgMisc.add(new EnumSetting.Builder<AntiFriendPop>()
        .name("friend-pop")
        .description("")
        .defaultValue(AntiFriendPop.NONE)
        .build()
    );
    private final Setting<Boolean> noCount = sgMisc.add(new BoolSetting.Builder()
        .name("anti-count")
        .description("")
        .defaultValue(false)
        .visible(() -> this.antiFriendPop.get() == AntiFriendPop.ALL || this.antiFriendPop.get() == AntiFriendPop.BREAK)
        .build()
    );
    private final Setting<Boolean> calcEvenIfNoDamage = sgMisc.add(new BoolSetting.Builder()
        .name("big-friend-calc")
        .description("")
        .defaultValue(false)
        .visible(() -> (this.antiFriendPop.get() == AntiFriendPop.ALL || this.antiFriendPop.get() == AntiFriendPop.BREAK) && this.targetMode.get() != Target.DAMAGE)
        .build()
    );
    private final Setting<Boolean> predictFriendDmg = sgMisc.add(new BoolSetting.Builder()
        .name("predict-friend")
        .description("")
        .defaultValue(false)
        .visible(() -> (this.antiFriendPop.get() == AntiFriendPop.ALL || this.antiFriendPop.get() == AntiFriendPop.BREAK) && this.instant.get())
        .build()
    );
    private final Setting<Boolean> brownZombie = sgMisc.add(new BoolSetting.Builder()
        .name("brown-zombie-mode")
        .description("")
        .defaultValue(false)
        .build()
    );
    //TODO
    private final Setting<Boolean> offhandSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("Offhand")
        .description("")
        .defaultValue(true)
        .build()
    );
    ///Place
    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
        .name("place")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("")
        .defaultValue(25)
        .min(0)
        .max(500)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("")
        .min(0.0).max(10.0)
        .sliderRange(0.0, 10.0)
        .defaultValue(6.0)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("")
        .min(0.1).max(20.0)
        .sliderRange(0.1, 20.0)
        .defaultValue(7.0)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Double> maxSelfPlace = sgPlace.add(new DoubleSetting.Builder()
        .name("max-self-place")
        .description("")
        .min(0.1).max(36.0)
        .sliderRange(0.1, 36.0)
        .defaultValue(10.0)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Integer> wasteAmount = sgPlace.add(new IntSetting.Builder()
        .name("waste-amount")
        .description("")
        .defaultValue(2)
        .min(1)
        .max(5)
        .sliderRange(1, 5)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Boolean> wasteMinDmgCount = sgPlace.add(new BoolSetting.Builder()
        .name("count-min-dmg")
        .description("")
        .defaultValue(true)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Double> facePlace = sgPlace.add(new DoubleSetting.Builder()
        .name("face-place")
        .description("")
        .min(0.1)
        .sliderMax(20.0)
        .defaultValue(8.0)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Double> placetrace = sgPlace.add(new DoubleSetting.Builder()
        .name("place-trace")
        .description("")
        .min(0.0)
        .sliderMax(10.0)
        .defaultValue(4.5)
        .visible(() -> this.place.get() && this.raytrace.get() != Raytrace.NONE && this.raytrace.get() != Raytrace.BREAK)
        .build()
    );
    private final Setting<Boolean> antiSurround = sgPlace.add(new BoolSetting.Builder()
        .name("anti-surround")
        .description("")
        .defaultValue(true)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Boolean> limitFacePlace = sgPlace.add(new BoolSetting.Builder()
        .name("LimitFacePlace")
        .description("")
        .defaultValue(true)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Boolean> oneDot15 = sgPlace.add(new BoolSetting.Builder()
        .name("1.15")
        .description("")
        .defaultValue(false)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Boolean> doublePop = sgPlace.add(new BoolSetting.Builder()
        .name("anti-totem")
        .description("")
        .defaultValue(false)
        .visible(this.place::get)
        .build()
    );
    private final Setting<Double> popHealth = sgPlace.add(new DoubleSetting.Builder()
        .name("pop-health")
        .description("")
        .defaultValue(1.0)
        .min(0)
        .sliderMax(3.0)
        .visible(() -> this.place.get() && this.doublePop.get())
        .build()
    );
    private final Setting<Double> popDamage = sgPlace.add(new DoubleSetting.Builder()
        .name("pop-damage")
        .description("")
        .min(0.0)
        .sliderMax(6.0)
        .defaultValue(4.0)
        .visible(() -> this.place.get() && this.doublePop.get())
        .build()
    );
    private final Setting<Integer> popTime = sgPlace.add(new IntSetting.Builder()
        .name("pop-time")
        .description("")
        .defaultValue(500)
        .min(0)
        .sliderMax(1000)
        .visible(() -> this.place.get() && this.doublePop.get())
        .build()
    );
    ///Break///
    private final Setting<Boolean> explode = sgBreak.add(new BoolSetting.Builder()
        .name("break")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Switch> switchMode = sgBreak.add(new EnumSetting.Builder<Switch>()
        .name("attack")
        .description("")
        .defaultValue(Switch.BREAKSLOT)
        .visible(this.explode::get)
        .build()
    );
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("")
        .defaultValue(50)
        .min(0)
        .sliderMax(500)
        .visible(this.explode::get)
        .build()
    );
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("")
        .min(0.0)
        .sliderMax(10.0)
        .defaultValue(6.0)
        .visible(this.explode::get)
        .build()
    );
    private final Setting<Integer> packets = sgBreak.add(new IntSetting.Builder()
        .name("packets")
        .description("")
        .defaultValue(1)
        .min(1)
        .sliderMax(6)
        .visible(this.explode::get)
        .build()
    );
    private final Setting<Double> maxSelfBreak = sgBreak.add(new DoubleSetting.Builder()
        .name("max-self-break")
        .description("")
        .min(0.1)
        .sliderMax(36.0)
        .defaultValue(10.0)
        .visible(this.explode::get)
        .build()
    );
    private final Setting<Double> breaktrace = sgBreak.add(new DoubleSetting.Builder()
        .name("break-trace")
        .description("")
        .min(0.0)
        .sliderMax(10.0)
        .defaultValue(4.5)
        .visible(() -> this.explode.get() && this.raytrace.get() != Raytrace.NONE && this.raytrace.get() != Raytrace.PLACE)
        .build()
    );
    private final Setting<Boolean> manual = sgBreak.add(new BoolSetting.Builder()
        .name("manual")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> manualMinDmg = sgBreak.add(new BoolSetting.Builder()
        .name("manual-min-dmg")
        .description("")
        .defaultValue(true)
        .visible(this.manual::get)
        .build()
    );
    private final Setting<Integer> manualBreak = sgBreak.add(new IntSetting.Builder()
        .name("manual-delay")
        .description("")
        .defaultValue(500)
        .min(0)
        .sliderMax(500)
        .visible(this.manual::get)
        .build()
    );
    private final Setting<Boolean> sync = sgBreak.add(new BoolSetting.Builder()
        .name("sync")
        .description("")
        .defaultValue(true)
        .visible(() -> this.explode.get() || this.manual.get())
        .build()
    );
    private final Setting<Boolean> instant = sgBreak.add(new BoolSetting.Builder()
        .name("predict")
        .description("")
        .defaultValue(true)
        .visible(() -> this.explode.get() && this.place.get())
        .build()
    );
    private final Setting<PredictTimer> instantTimer = sgBreak.add(new EnumSetting.Builder<PredictTimer>()
        .name("predict-timer")
        .description("")
        .defaultValue(PredictTimer.NONE)
        .visible(() -> this.explode.get() && this.place.get() && this.instant.get())
        .build()
    );
    private final Setting<Boolean> resetBreakTimer = sgBreak.add(new BoolSetting.Builder()
        .name("reset-break-timer")
        .description("")
        .defaultValue(true)
        .visible(() -> this.explode.get() && this.place.get() && this.instant.get())
        .build()
    );
    private final Setting<Integer> predictDelay = sgBreak.add(new IntSetting.Builder()
        .name("predict-delay")
        .description("")
        .defaultValue(12)
        .min(0)
        .sliderMax(500)
        .visible(() -> this.explode.get() && this.place.get() && this.instant.get() && this.instantTimer.get() == PredictTimer.PREDICT)
        .build()
    );
    private final Setting<Boolean> predictCalc = sgBreak.add(new BoolSetting.Builder()
        .name("predict-calc")
        .description("")
        .defaultValue(true)
        .visible(() -> this.explode.get() && this.place.get() && this.instant.get())
        .build()
    );
    private final Setting<Boolean> superSafe = sgBreak.add(new BoolSetting.Builder()
        .name("super-safe")
        .description("")
        .defaultValue(true)
        .visible(() -> this.explode.get() && this.place.get() && this.instant.get())
        .build()
    );
    private final Setting<Boolean> antiCommit = sgBreak.add(new BoolSetting.Builder()
        .name("anti-over-commit")
        .description("")
        .defaultValue(true)
        .visible(() -> this.explode.get() && this.place.get() && this.instant.get())
        .build()
    );
    private final Setting<Boolean> doublePopOnDamage = sgPlace.add(new BoolSetting.Builder()
        .name("damage-pop")
        .description("")
        .defaultValue(false)
        .visible(() -> this.place.get() && this.doublePop.get() && this.targetMode.get() == Target.DAMAGE)
        .build()
    );
    ///RENDER///
    // TODO : implement rest of settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Highlight valid locations and more.")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(this.render::get)
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the block overlay.")
        .defaultValue(new SettingColor(255, 255, 255, 45))
        .visible(this.render::get)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the block outline.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Sides)
        .build()
    );
    private final Setting<Boolean> colorSync = sgRender.add(new BoolSetting.Builder()
        .name("color-sync")
        .description("Syncs the outline colors with the client's current theme.")
        .defaultValue(false)
        .visible(this.render::get)
        .build()
    );
    private final Setting<Boolean> text = sgRender.add(new BoolSetting.Builder()
        .name("text")
        .description("text text text")
        .defaultValue(false)
        .visible(this.render::get)
        .build()
    );
//    private final Setting<Double> lineWidth = sgRender.add(new DoubleSetting.Builder()
//        .name("line-width")
//        .description("Adjusts the width of the line")
//        .defaultValue(1.5)
//        .min(0.1).max(5.0)
//        .sliderRange(0.1, 5.0)
//        .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Sides)
//        .build()
//    );
//    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
//        .name("swing")
//        .description("Renders hand swinging client-side.")
//        .defaultValue(true)
//        .build()
//    );
//    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
//        .name("break")
//        .description("Renders a block overlay over the block the crystals are broken on.")
//        .defaultValue(false)
//        .build()
//    );
//    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder()
//        .name("damage")
//        .description("Renders crystal damage text in the block overlay.")
//        .defaultValue(true)
//        .build()
//    );
//    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder()
//        .name("damage-color")
//        .description("The color of the damage text.")
//        .defaultValue(new SettingColor(255, 255, 255))
//        .visible(this.renderDamageText::get)
//        .build()
//    );
//    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
//        .name("damage-scale")
//        .description("How big the damage text should be.")
//        .defaultValue(1.25)
//        .min(1)
//        .sliderMax(4)
//        .visible(renderDamageText::get)
//        .build()
//    );
//    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
//        .name("render-time")
//        .description("How long to render for.")
//        .defaultValue(10)
//        .min(0)
//        .sliderMax(20)
//        .build()
//    );
//    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder()
//        .name("break-time")
//        .description("How long to render breaking for.")
//        .defaultValue(13)
//        .min(0)
//        .sliderMax(20)
//        .visible(renderBreak::get)
//        .build()
//    );

    public boolean rotating = false;
    private Queue<Entity> attackList = new ConcurrentLinkedQueue<>();
    private Map<Entity, Float> crystalMap = new HashMap<>();
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
    //TODO : check and rewrite again
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

    @Override
    public String getInfoString() {
        if (this.switching) {
            return "\u00a7aSwitch";
        }
        if (target != null) {
            return target.getName().getString();
        }
        return null;
    }

    //TODO : check and rewrite again
    @EventHandler
    public void onSend(PacketEvent.Send event) {
        if (this.rotate.get() != Rotate.OFF && this.rotating && this.eventMode.get() != 2 && event.packet instanceof PlayerMoveC2SPacket) {
            ((PlayerMoveC2SPacketAccessor) event.packet).setYaw(this.yaw);
            ((PlayerMoveC2SPacketAccessor) event.packet).setPitch(this.pitch);
            ++this.rotationPacketsSpoofed;
            if (this.rotationPacketsSpoofed >= this.rotations.get()) {
                this.rotating = false;
                this.rotationPacketsSpoofed = 0;
            }
        }
        BlockPos pos;
        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK && packet.getEntity() instanceof EndCrystalEntity) {
            EndCrystalEntity crystal = (EndCrystalEntity) packet.getEntity();
            pos = packet.getEntity().getBlockPos();
            if (this.removeAfterAttack.get()) {
                Objects.requireNonNull(packet.getEntity()).kill();
                mc.world.removeEntity(packet.getEntity().getId(), Entity.RemovalReason.KILLED);
            }
            if (this.antiBlock.get() && EntityUtil.isCrystalAtFeet(crystal, this.range.get()) && pos != null) {
                this.rotateToPos(pos);
                BlockUtil.placeCrystalOnBlock(this.placePos, this.offHand ? Hand.OFF_HAND : Hand.MAIN_HAND, this.placeSwing.get(), this.exactHand.get());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketEvent.Receive event) {
        PlaySoundS2CPacket packet;
        if (mc.player == null || mc.world == null) {return;}
        if (!this.justRender.get() && this.switchTimer.passedMs(this.switchCooldown.get()) && this.explode.get() && this.instant.get() && event.packet instanceof EntitySpawnS2CPacket
            && (this.syncedCrystalPos == null || !this.syncedFeetPlace.get() || this.damageSync.get() == DamageSync.NONE)) {
            BlockPos pos;
            EntitySpawnS2CPacket packet2 = (EntitySpawnS2CPacket) event.packet;
            //TODO : check ID
            if (packet2.getId() == 51 && mc.player.getBlockPos().getSquaredDistance(pos = new BlockPos((int) packet2.getX(), (int) packet2.getY(), (int) packet2.getZ())) + this.predictOffset.get() <= MathHelper.square(this.breakRange.get()) && (this.instantTimer.get() == PredictTimer.NONE || this.instantTimer.get() == PredictTimer.BREAK && this.breakTimer.passedMs(this.breakDelay.get()) || this.instantTimer.get() == PredictTimer.PREDICT && this.predictTimer.passedMs(this.predictDelay.get()))) {
                if (this.predictSlowBreak(pos.down())) {
                    return;
                }
                if (this.predictFriendDmg.get() && (this.antiFriendPop.get() == AntiFriendPop.BREAK || this.antiFriendPop.get() == AntiFriendPop.ALL) && this.isRightThread()) {
                    for (PlayerEntity friend : mc.world.getPlayers()) {
                        if (friend == null || mc.player.equals(friend) || friend.getBlockPos().getSquaredDistance(pos) > MathHelper.square(this.range.get() + this.placeRange.get()) || !Friends.get().isFriend(friend) || !((double) DamageUtil.calculateDamage(pos, friend) > (double) getHealth(friend) + 0.5))
                            continue;
                        return;
                    }
                }
                if (placedPos.contains(pos.down())) {
                    float selfDamage;
                    if (this.isRightThread() && this.superSafe.get() ? DamageUtil.canTakeDamage(this.suicide.get()) && ((double) (selfDamage = DamageUtil.calculateDamage(pos, mc.player)) - 0.5 > (double) getHealth(mc.player) || selfDamage > this.maxSelfBreak.get()) : this.superSafe.get() != false) {
                        return;
                    }
                    this.attackCrystalPredict(packet2.getId(), pos);
                } else if (this.predictCalc.get() && this.isRightThread()) {
                    float selfDamage = -1.0f;
                    if (DamageUtil.canTakeDamage(this.suicide.get())) {
                        selfDamage = DamageUtil.calculateDamage(pos, mc.player);
                    }
                    if ((double) selfDamage + 0.5 < (double) getHealth(mc.player) && selfDamage <= this.maxSelfBreak.get()) {
                        for (PlayerEntity player : mc.world.getPlayers()) {
                            float damage;
                            if (!(player.getBlockPos().getSquaredDistance(pos) <= MathHelper.square(this.range.get())) || !EntityUtil.isValid(player, this.range.get() + this.breakRange.get()) || this.antiNaked.get() && DamageUtil.isNaked(player) || !((damage = DamageUtil.calculateDamage(pos, player)) > selfDamage || damage > this.minDamage.get() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(damage > getHealth(player)))
                                continue;
                            if (this.predictRotate.get() && this.eventMode.get() != 2 && (this.rotate.get() == Rotate.BREAK || this.rotate.get() == Rotate.ALL)) {
                                this.rotateToPos(pos);
                            }
                            this.attackCrystalPredict(packet2.getId(), pos);
                            break;
                        }
                    }
                }
            }
        } else if (!this.soundConfirm.get() && event.packet instanceof ExplosionS2CPacket) {
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
            //TODO : check ID
            if (packet5.hashCode() == 35 && packet5.getEntity(mc.world) instanceof PlayerEntity) {
                this.totemPops.put((PlayerEntity) packet5.getEntity(mc.world), new Timer().reset());
            }
        } else if (event.packet instanceof PlaySoundS2CPacket && (packet = (PlaySoundS2CPacket) event.packet).getCategory() == SoundCategory.BLOCKS && packet.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
            BlockPos pos = new BlockPos((int) packet.getX(), (int) packet.getY(), (int) packet.getZ());
            if (this.sound.get() || this.threadMode.get() == ThreadMode.SOUND) {
                NoSoundLag.removeEntities(packet, this.soundRange.get());
            }
            if (this.soundConfirm.get()) {
                this.removePos(pos);
            }
            if (this.threadMode.get() == ThreadMode.SOUND && this.isRightThread() && mc.player != null && mc.player.getBlockPos().getSquaredDistance(pos) < MathHelper.square(this.soundPlayer.get())) {
                this.handlePool(true);
            }
        }
    }

    private boolean predictSlowBreak(BlockPos pos) {
        if (this.antiCommit.get() && lowDmgPos.remove(pos)) {
            return this.shouldSlowBreak(false);
        }
        return false;
    }

    private boolean isRightThread() {
        return mc.isOnThread() || !eventManager.ticksOngoing() && !this.threadOngoing.get();
    }

    private void attackCrystalPredict(int entityID, BlockPos pos) {
        if (!(!this.predictRotate.get() || this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE || this.rotate.get() != Rotate.BREAK && this.rotate.get() != Rotate.ALL)) {
            this.rotateToPos(pos);
        }
        PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(mc.world.getEntityById(entityID), mc.player.isSneaking());
        mc.getNetworkHandler().sendPacket(attackPacket);
        if (this.breakSwing.get()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        if (this.resetBreakTimer.get()) {
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
        List<Color> syncColors = getThemeColors();
        if ((this.offHand || this.mainHand || this.switchMode.get() == Switch.CALC) && this.renderPos != null && this.render.get()) {
            event.renderer.box(this.renderPos, this.colorSync.get() ? syncColors.get(0) : this.sideColor.get(), this.colorSync.get() ? syncColors.get(1) : this.lineColor.get(), this.shapeMode.get(), 0);
            if (this.text.get()) {
                //Render2d event ?
                //RenderUtil.drawText(this.renderPos, (Math.floor(this.renderDamage) == this.renderDamage ? Integer.valueOf((int) this.renderDamage) : String.format("%.1f", this.renderDamage)) + "");
            }
        }
    }
// TODO : some other time

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
//TODO : check

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

    private void postProcessing() {
        if (this.threadMode.get() != ThreadMode.NONE || this.eventMode.get() != 2 || this.rotate.get() == Rotate.OFF || !this.rotateFirst.get()) {
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

    private void postProcessBreak() {
        while (!this.packetUseEntities.isEmpty()) {
            PlayerInteractEntityC2SPacket packet = this.packetUseEntities.poll();
            mc.player.networkHandler.sendPacket(packet);
            if (this.breakSwing.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            this.breakTimer.reset();
        }
    }

    private void postProcessPlace() {
        if (this.placeInfo != null) {
            this.placeInfo.runPlace();
            this.placeTimer.reset();
            this.placeInfo = null;
        }
    }

    private void processMultiThreading() {
        if (!this.isActive()) {
            return;
        }
        if (this.threadMode.get() == ThreadMode.WHILE) {
            this.handleWhile();
        } else if (this.threadMode.get() != ThreadMode.NONE) {
            this.handlePool(false);
        }
    }

    private void handlePool(boolean justDoIt) {
        if (justDoIt || this.executor == null || this.executor.isTerminated() || this.executor.isShutdown() || this.syncroTimer.passedMs(this.syncThreads.get()) && this.syncThreadBool.get()) {
            if (this.executor != null) {
                this.executor.shutdown();
            }
            this.executor = this.getExecutor();
            this.syncroTimer.reset();
        }
    }

    private void handleWhile() {
        if (this.thread == null || this.thread.isInterrupted() || !this.thread.isAlive() || this.syncroTimer.passedMs(this.syncThreads.get()) && this.syncThreadBool.get()) {
            if (this.thread == null) {
                this.thread = new Thread(RAutoCrystal.getInstance(this));
            } else if (this.syncroTimer.passedMs(this.syncThreads.get()) && !this.shouldInterrupt.get() && this.syncThreadBool.get()) {
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

    private ScheduledExecutorService getExecutor() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(RAutoCrystal.getInstance(this), 0L, this.threadDelay.get(), TimeUnit.MILLISECONDS);
        return service;
    }

    public void doAutoCrystal() {
        if (this.brownZombie.get()) {
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

    private boolean check() {
        //TODO : just extract the logic to here
        if (mc.player == null || mc.world == null) {return false;}
        if (this.syncTimer.passedMs(this.damageSyncTime.get())) {
            this.currentSyncTarget = null;
            this.syncedCrystalPos = null;
            this.syncedPlayerPos = null;
        } else if (this.syncySync.get() && this.syncedCrystalPos != null) {
            this.posConfirmed = true;
        }
        this.foundDoublePop = false;
        if (this.renderTimer.passedMs(500L)) {
            this.renderPos = null;
            this.renderTimer.reset();
        }
        this.mainHand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
        this.offHand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        this.currentDamage = 0.0;
        this.placePos = null;
        //TODO : reimplement surround check
        if (this.lastSlot != mc.player.getInventory().selectedSlot) {
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
        if (!((this.offHand || this.mainHand || this.switchMode.get() != Switch.BREAKSLOT || this.switching) && DamageUtil.canBreakWeakness(mc.player) && this.switchTimer.passedMs(this.switchCooldown.get()))) {
            this.renderPos = null;
            target = null;
            this.rotating = false;
            return false;
        }
        // TODO : check if works
        if (this.mineSwitch.get() && mc.mouse.wasLeftButtonClicked() && (this.switching || this.autoSwitch.get() == AutoSwitch.ALWAYS) && mc.mouse.wasRightButtonClicked() && mc.player.getMainHandStack().getItem() instanceof PickaxeItem) {
            this.switchItem();
        }
        this.mapCrystals();
        if (!this.posConfirmed && this.damageSync.get() != DamageSync.NONE && this.syncTimer.passedMs(this.confirm.get())) {
            this.syncTimer.setMs(this.damageSyncTime.get() + 1);
        }
        return true;
    }

    private void mapCrystals() {
        this.efficientTarget = null;
        if (this.packets.get() != 1) {
            this.attackList = new ConcurrentLinkedQueue<>();
            this.crystalMap = new HashMap<>();
        }
        this.crystalCount = 0;
        this.minDmgCount = 0;
        Entity maxCrystal = null;
        float maxDamage = 0.5f;
        for (Entity entity : mc.world.getEntities()) {
            if (!entity.isAlive() || !(entity instanceof EndCrystalEntity) || !this.isValid(entity)) continue;
            if (this.syncedFeetPlace.get() && entity.getBlockPos().down().equals(this.syncedCrystalPos) && this.damageSync.get() != DamageSync.NONE) {
                ++this.minDmgCount;
                ++this.crystalCount;
                if (this.syncCount.get()) {
                    this.minDmgCount = this.wasteAmount.get() + 1;
                    this.crystalCount = this.wasteAmount.get() + 1;
                }
                if (!this.hyperSync.get()) continue;
                maxCrystal = null;
                break;
            }
            boolean count = false;
            boolean countMin = false;
            float selfDamage = -1.0f;
            if (DamageUtil.canTakeDamage(this.suicide.get())) {
                selfDamage = DamageUtil.calculateDamage(entity, mc.player);
            }
            if ((double) selfDamage + 0.5 < (double) getHealth(mc.player) && selfDamage <= this.maxSelfBreak.get()) {
                Entity beforeCrystal = maxCrystal;
                float beforeDamage = maxDamage;
                for (PlayerEntity player : mc.world.getPlayers()) {
                    float damage;
                    if (!(player.getBlockPos().getSquaredDistance(entity.getPos()) <= MathHelper.square(this.range.get())))
                        continue;
                    if (EntityUtil.isValid(player, this.range.get() + this.breakRange.get())) {
                        if (this.antiNaked.get() && DamageUtil.isNaked(player) || !((damage = DamageUtil.calculateDamage(entity, player)) > selfDamage || damage > this.minDamage.get() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(damage > getHealth(player)))
                            continue;
                        if (damage > maxDamage) {
                            maxDamage = damage;
                            maxCrystal = entity;
                        }
                        if (this.packets.get() == 1) {
                            if (damage >= this.minDamage.get() || !this.wasteMinDmgCount.get()) {
                                count = true;
                            }
                            countMin = true;
                            continue;
                        }
                        if (this.crystalMap.get(entity) != null && !(this.crystalMap.get(entity) < damage))
                            continue;
                        this.crystalMap.put(entity, damage);
                        continue;
                    }
                    if (this.antiFriendPop.get() != AntiFriendPop.BREAK && this.antiFriendPop.get() != AntiFriendPop.ALL || !Friends.get().isFriend(player) || !((double) (damage = DamageUtil.calculateDamage(entity, player)) > (double) getHealth(player) + 0.5))
                        continue;
                    maxCrystal = beforeCrystal;
                    maxDamage = beforeDamage;
                    this.crystalMap.remove(entity);
                    if (!this.noCount.get()) break;
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
        if (this.damageSync.get() == DamageSync.BREAK && ((double) maxDamage > this.lastDamage || this.syncTimer.passedMs(this.damageSyncTime.get()) || this.damageSync.get() == DamageSync.NONE)) {
            this.lastDamage = maxDamage;
        }
        if (this.enormousSync.get() && this.syncedFeetPlace.get() && this.damageSync.get() != DamageSync.NONE && this.syncedCrystalPos != null) {
            if (this.syncCount.get()) {
                this.minDmgCount = this.wasteAmount.get() + 1;
                this.crystalCount = this.wasteAmount.get() + 1;
            }
            return;
        }
        if (this.webAttack.get() && this.webPos != null) {
            if (mc.player.getBlockPos().getSquaredDistance(this.webPos.up()) > MathHelper.square(this.breakRange.get())) {
                this.webPos = null;
            } else {
                for (Entity entity : mc.world.getOtherEntities(null, new Box(this.webPos.up()))) {
                    if (!(entity instanceof EndCrystalEntity)) continue;
                    this.attackList.add(entity);
                    this.efficientTarget = entity;
                    this.webPos = null;
                    this.lastDamage = 0.5;
                    return;
                }
            }
        }
        if (this.shouldSlowBreak(true) && maxDamage < this.minDamage.get() && (target == null || !(getHealth(target) <= this.facePlace.get()) || !this.breakTimer.passedMs(this.facePlaceSpeed.get()) && this.slowFaceBreak.get() && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) && this.holdFacePlace.get() && this.holdFaceBreak.get())) {
            this.efficientTarget = null;
            return;
        }
        if (this.packets.get() == 1) {
            this.efficientTarget = maxCrystal;
        } else {
            this.crystalMap = MathUtil.sortByValue(this.crystalMap, true);
            for (Map.Entry entry : this.crystalMap.entrySet()) {
                Entity crystal = (Entity) entry.getKey();
                float damage = ((Float) entry.getValue());
                if (damage >= this.minDamage.get() || !this.wasteMinDmgCount.get()) {
                    ++this.crystalCount;
                }
                this.attackList.add(crystal);
                ++this.minDmgCount;
            }
        }
    }

    //TODO : check
    private boolean shouldSlowBreak(boolean withManual) {
        return withManual && this.manual.get() && this.manualMinDmg.get() && mc.mouse.wasRightButtonClicked() && (!mc.mouse.wasLeftButtonClicked() || !this.holdFacePlace.get()) || this.holdFacePlace.get() && this.holdFaceBreak.get() && mc.mouse.wasLeftButtonClicked() && !this.breakTimer.passedMs(this.facePlaceSpeed.get()) || this.slowFaceBreak.get() && !this.breakTimer.passedMs(this.facePlaceSpeed.get());
    }

    private void placeCrystal() {
        int crystalLimit = this.wasteAmount.get();
        if (this.placeTimer.passedMs(this.placeDelay.get()) && this.place.get() && (this.offHand || this.mainHand || this.switchMode.get() == Switch.CALC || this.switchMode.get() == Switch.BREAKSLOT && this.switching)) {
            if (!(!this.offHand && !this.mainHand && (this.switchMode.get() == Switch.ALWAYS || this.switching) || this.crystalCount < crystalLimit || this.antiSurround.get() && this.lastPos != null && this.lastPos.equals(this.placePos))) {
                return;
            }
            this.calculateDamage(this.getTarget(this.targetMode.get() == Target.UNSAFE));
            if (target != null && this.placePos != null) {
                if (!this.offHand && !this.mainHand && this.autoSwitch.get() != AutoSwitch.NONE && (this.currentDamage > this.minDamage.get() || this.lethalSwitch.get() && getHealth(target) <= this.facePlace.get()) && !this.switchItem()) {
                    return;
                }
                if (this.currentDamage < this.minDamage.get() && this.limitFacePlace.get()) {
                    crystalLimit = 1;
                }
                if (this.currentDamage >= this.minMinDmg.get() && (this.offHand || this.mainHand || this.autoSwitch.get() != AutoSwitch.NONE) && (this.crystalCount < crystalLimit || this.antiSurround.get() && this.lastPos != null && this.lastPos.equals(this.placePos)) && (this.currentDamage > this.minDamage.get() || this.minDmgCount < crystalLimit) && this.currentDamage >= 1.0 && (DamageUtil.isArmorLow(target, this.minArmor.get()) || getHealth(target) <= this.facePlace.get() || this.currentDamage > this.minDamage.get() || this.shouldHoldFacePlace())) {
                    double damageOffset = this.damageSync.get() == DamageSync.BREAK ? this.dropOff.get() - 5.0f : 0.0f;
                    boolean syncflag = false;
                    if (this.syncedFeetPlace.get() && this.placePos.equals(this.lastPos) && this.isEligibleForFeetSync(target, this.placePos) && !this.syncTimer.passedMs(this.damageSyncTime.get()) && target.equals(this.currentSyncTarget) && target.getPos().equals(this.syncedPlayerPos) && this.damageSync.get() != DamageSync.NONE) {
                        this.syncedCrystalPos = this.placePos;
                        this.lastDamage = this.currentDamage;
                        if (this.fullSync.get()) {
                            this.lastDamage = 100.0;
                        }
                        syncflag = true;
                    }
                    if (syncflag || this.currentDamage - damageOffset > this.lastDamage || this.syncTimer.passedMs(this.damageSyncTime.get()) || this.damageSync.get() == DamageSync.NONE) {
                        if (!syncflag && this.damageSync.get() != DamageSync.BREAK) {
                            this.lastDamage = this.currentDamage;
                        }
                        this.renderPos = this.placePos;
                        this.renderDamage = this.currentDamage;
                        if (this.switchItem()) {
                            this.currentSyncTarget = target;
                            this.syncedPlayerPos = target.getBlockPos();
                            if (this.foundDoublePop) {
                                this.totemPops.put(target, new Timer().reset());
                            }
                            this.rotateToPos(this.placePos);
                            if (this.addTolowDmg || this.actualSlowBreak.get() && this.currentDamage < this.minDamage.get()) {
                                lowDmgPos.add(this.placePos);
                            }
                            placedPos.add(this.placePos);
                            if (!this.justRender.get()) {
                                if (this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE && this.rotateFirst.get() && this.rotate.get() != Rotate.OFF) {
                                    this.placeInfo = new PlaceInfo(this.placePos, this.offHand, this.placeSwing.get(), this.exactHand.get());
                                } else {
                                    BlockUtil.placeCrystalOnBlock(this.placePos, this.offHand ? Hand.OFF_HAND : Hand.MAIN_HAND, this.placeSwing.get(), this.exactHand.get());
                                }
                            }
                            this.lastPos = this.placePos;
                            this.placeTimer.reset();
                            this.posConfirmed = false;
                            if (this.syncTimer.passedMs(this.damageSyncTime.get())) {
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
    //TODO : check
    private boolean shouldHoldFacePlace() {
        this.addTolowDmg = false;
        if (this.holdFacePlace.get() && mc.mouse.wasLeftButtonClicked()) {
            this.addTolowDmg = true;
            return true;
        }
        return false;
    }

    private boolean switchItem() {
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
            //offhand
//            case ALWAYS: {
//                if (!this.doSwitch()) break;
//                    return true;
//            }
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

    private void calculateDamage(PlayerEntity targetedPlayer) {
        BlockPos playerPos;
        Block web;
        if (targetedPlayer == null && this.targetMode.get() != Target.DAMAGE && !this.fullCalc.get()) {
            return;
        }
        float maxDamage = 0.5f;
        PlayerEntity currentTarget = null;
        BlockPos currentPos = null;
        float maxSelfDamage = 0.0f;
        this.foundDoublePop = false;
        BlockPos setToAir = null;
        BlockState state = null;
        if (this.webAttack.get() && targetedPlayer != null && (web = BlockUtil.getState(playerPos = new BlockPos(targetedPlayer.getBlockPos())).getBlock()) == Blocks.COBWEB) {
            setToAir = playerPos;
            state = BlockUtil.getState(playerPos);
            mc.world.removeBlock(playerPos, false);
        }

        block0:
        //this.antiSurround - VERY IMPORTANT - special entity check ?? TODO - 1.15
        for (BlockPos pos : BlockUtil.possiblePlacePositions(this.placeRange.get(), this.antiSurround.get(), this.oneDot15.get())) {
            if (!BlockUtil.rayTracePlaceCheck(pos, (this.raytrace.get() == Raytrace.PLACE || this.raytrace.get() == Raytrace.FULL) && mc.player.getBlockPos().getSquaredDistance(pos) > MathHelper.square(this.placetrace.get()), 1.0f)) {continue;}
            float selfDamage = -1.0f;
            if (DamageUtil.canTakeDamage(this.suicide.get())) {
                selfDamage = DamageUtil.calculateDamage(pos, mc.player);
            }
            if (!((double) selfDamage + 0.5 < (double) getHealth(mc.player)) || !(selfDamage <= this.maxSelfPlace.get()))
                continue;
            if (targetedPlayer != null) {
                float playerDamage = DamageUtil.calculateDamage(pos, targetedPlayer);
                if (this.calcEvenIfNoDamage.get() && (this.antiFriendPop.get() == AntiFriendPop.ALL || this.antiFriendPop.get() == AntiFriendPop.PLACE)) {
                    boolean friendPop = false;
                    for (PlayerEntity friend : mc.world.getPlayers()) {
                        float friendDamage;
                        if (friend == null || mc.player.equals(friend) || friend.getBlockPos().getSquaredDistance(pos) > MathHelper.square(this.range.get() + this.placeRange.get()) || !Friends.get().isFriend(friend) || !((double) (friendDamage = DamageUtil.calculateDamage(pos, friend)) > (double) getHealth(friend) + 0.5)) {
                            continue;
                        }
                        friendPop = true;
                        break;
                    }
                    if (friendPop) continue;
                }
                if (this.isDoublePoppable(targetedPlayer, playerDamage) && (currentPos == null || targetedPlayer.getBlockPos().getSquaredDistance(pos) < targetedPlayer.getBlockPos().getSquaredDistance(currentPos))) {
                    currentTarget = targetedPlayer;
                    maxDamage = playerDamage;
                    currentPos = pos;
                    this.foundDoublePop = true;
                    continue;
                }
                if (this.foundDoublePop || !(playerDamage > maxDamage) && (!this.extraSelfCalc.get() || !(playerDamage >= maxDamage) || !(selfDamage < maxSelfDamage)) || !(playerDamage > selfDamage || playerDamage > this.minDamage.get() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(playerDamage > getHealth(targetedPlayer))) {
                    continue;
                }
                maxDamage = playerDamage;
                currentTarget = targetedPlayer;
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
                if (EntityUtil.isValid(player, this.placeRange.get() + this.range.get())) {
                    if (this.antiNaked.get() && DamageUtil.isNaked(player)) continue;
                    float playerDamage = DamageUtil.calculateDamage(pos, player);
                    if (this.doublePopOnDamage.get() && this.isDoublePoppable(player, playerDamage) && (currentPos == null || player.getBlockPos().getSquaredDistance(pos) < player.getBlockPos().getSquaredDistance(currentPos))) {
                        currentTarget = player;
                        maxDamage = playerDamage;
                        currentPos = pos;
                        maxSelfDamage = selfDamage;
                        this.foundDoublePop = true;
                        if (this.antiFriendPop.get() != AntiFriendPop.BREAK && this.antiFriendPop.get() != AntiFriendPop.PLACE)
                            continue;
                        continue block0;
                    }
                    if (this.foundDoublePop || !(playerDamage > maxDamage) && (!this.extraSelfCalc.get() || !(playerDamage >= maxDamage) || !(selfDamage < maxSelfDamage)) || !(playerDamage > selfDamage || playerDamage > this.minDamage.get() && !DamageUtil.canTakeDamage(this.suicide.get())) && !(playerDamage > getHealth(player)))
                        continue;
                    maxDamage = playerDamage;
                    currentTarget = player;
                    currentPos = pos;
                    maxSelfDamage = selfDamage;
                    continue;
                }
                if (this.antiFriendPop.get() != AntiFriendPop.ALL && this.antiFriendPop.get() != AntiFriendPop.PLACE || player == null || !(player.getBlockPos().getSquaredDistance(pos) <= MathHelper.square(this.range.get() + this.placeRange.get())) || !Friends.get().isFriend(player) || !((double) (friendDamage = DamageUtil.calculateDamage(pos, player)) > (double) getHealth(player) + 0.5))
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

    private PlayerEntity getTarget(boolean unsafe) {
        if (this.targetMode.get() == Target.DAMAGE) {
            return null;
        }
        PlayerEntity currentTarget = null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (EntityUtil.isntValid(player, this.placeRange.get() + this.range.get()) || this.antiNaked.get() && DamageUtil.isNaked(player) || unsafe && EntityUtil.isSafe(player))
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
        //TODO - check inventory part
        if (this.predictPos.get() && currentTarget != null) {
            GameProfile profile = new GameProfile(currentTarget.getUuid() == null ? UUID.fromString("8af022c8-b926-41a0-8b79-2b544ff00fcf") : currentTarget.getUuid(), currentTarget.getDisplayName().toString());
            OtherClientPlayerEntity newTarget = new OtherClientPlayerEntity(mc.world, profile);
            Vec3d extrapolatePosition = MathUtil.extrapolatePlayerPosition(currentTarget, this.predictTicks.get());
            newTarget.copyPositionAndRotation(currentTarget);
            newTarget.setPos(extrapolatePosition.x, extrapolatePosition.y, extrapolatePosition.z);
            newTarget.setHealth(EntityUtil.getHealth(currentTarget));
            inventoryManager.copyInventory(currentTarget.getInventory());
            currentTarget = newTarget;
        }
        return currentTarget;
    }

    private void breakCrystal() {
        if (this.explode.get() && this.breakTimer.passedMs(this.breakDelay.get()) && (this.switchMode.get() == Switch.ALWAYS || this.mainHand || this.offHand)) {
            if (this.packets.get() == 1 && this.efficientTarget != null) {
                if (this.justRender.get()) {
                    this.doFakeSwing();
                    return;
                }
                if (this.syncedFeetPlace.get() && this.gigaSync.get() && this.syncedCrystalPos != null && this.damageSync.get() != DamageSync.NONE) {
                    return;
                }
                this.rotateTo(this.efficientTarget);
                this.attackEntity(this.efficientTarget);
                this.breakTimer.reset();
            } else if (!this.attackList.isEmpty()) {
                if (this.justRender.get()) {
                    this.doFakeSwing();
                    return;
                }
                if (this.syncedFeetPlace.get() && this.gigaSync.get() && this.syncedCrystalPos != null && this.damageSync.get() != DamageSync.NONE) {
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

    private void attackEntity(Entity entity) {
        if (entity != null) {
            if (this.eventMode.get() == 2 && this.threadMode.get() == ThreadMode.NONE && this.rotateFirst.get() && this.rotate.get() != Rotate.OFF) {
                //TODO: Fix this
                this.packetUseEntities.add(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
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
    private void doFakeSwing() {
        if (this.fakeSwing.get()) {
            EntityUtil.swingArmNoPacket(Hand.MAIN_HAND, mc.player);
        }
    }
    //TODO: REWRITE
    private void manualBreaker() {
        HitResult result;
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
        if ((this.offHand || this.mainHand) && this.manual.get() && this.manualTimer.passedMs(this.manualBreak.get()) && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.field_31997) && mc.player.getOffHandStack().getItem() != Items.GOLDEN_APPLE && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.GOLDEN_APPLE && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.BOW && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.EXPERIENCE_BOTTLE && (result = mc.crosshairTarget) != null) {
            switch (result.getType()) {
                case ENTITY: {
                    Entity entity = mc.targetedEntity;
                    if (!(entity instanceof EndCrystalEntity)) break;
                    EntityUtil.attackEntity(entity, this.sync.get(), this.breakSwing.get());
                    this.manualTimer.reset();
                    break;
                }
                case BLOCK: {
                    //maybe
                    BlockPos mousePos = new BlockPos((int) result.getPos().x, (int) result.getPos().y, (int) result.getPos().z).up();
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

    private void rotateTo(Entity entity) {
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

    private void rotateToPos(BlockPos pos) {
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

    private boolean isDoublePoppable(PlayerEntity player, float damage) {
        float health;
        if (this.doublePop.get() && (double) (health = getHealth(player)) <= this.popHealth.get() && (double) damage > (double) health + 0.5 && damage <= this.popDamage.get()) {
            Timer timer = this.totemPops.get(player);
            return timer == null || timer.passedMs(this.popTime.get());
        }
        return false;
    }

    private boolean isValid(Entity entity) {
        return entity != null && mc.player.getBlockPos().getSquaredDistance(entity.getPos()) <= MathHelper.square(this.breakRange.get()) && (this.raytrace.get() == Raytrace.NONE || this.raytrace.get() == Raytrace.PLACE || mc.player.canSee(entity) || !mc.player.canSee(entity) && mc.player.getBlockPos().getSquaredDistance(entity.getPos()) <= MathHelper.square(this.breaktrace.get()));
    }

    private boolean isEligibleForFeetSync(PlayerEntity player, BlockPos pos) {
        if (this.holySync.get()) {
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
                        Thread.sleep(this.autoCrystal.threadDelay.get());
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
    //TODO : move into util
    private List<Color> getThemeColors() {
        MeteorGuiTheme theme = (MeteorGuiTheme) GuiThemes.get();
        return Arrays.asList(theme.accentColor.get(), theme.outlineColor.get());
    }
}
