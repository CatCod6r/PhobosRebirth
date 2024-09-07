package com.example.addon.util;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.addon.manager.Managers.inventoryManager;

public class InventoryUtil
    implements Util {

//    public static void switchToHotbarSlot(int slot, boolean silent) {
//        if (mc.player != null) {
//            Inventory playerInventory = mc.player.getInventory();
//            if (mc.player.getInventory().selectedSlot != slot && slot >= 0) {
//                if (silent) {
//                    // Send a CreativeInventoryAction packet without changing the selected slot
//                    mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot));
//                } else {
//                    // Send a CreativeInventoryAction packet and change the selected slot
//                    mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot));
//                    mc.player.getInventory().selectedSlot = slot;
//                }
//            }
//        }
//    }

//    public static void switchToHotbarSlot(Class clazz, boolean silent) {
//        int slot = InventoryUtil.findHotbarBlock(clazz);
//        if (slot > -1) {
//            InventoryUtil.switchToHotbarSlot(slot, silent);
//        }
//    }

    public static boolean isNull(ItemStack stack) {
        return stack == null || stack.getItem() instanceof AirBlockItem;
    }

    public static int findHotbarBlock(Class clazz) {
        for (int i = 0; i < 9; ++i) {
            Block block;
            ItemStack stack = InventoryUtil.mc.player.getInventory().getStack(i);
            if (stack == ItemStack.EMPTY) continue;
            if (clazz.isInstance(stack.getItem())) {
                return i;
            }
            if (!(stack.getItem() instanceof BlockItem) || !clazz.isInstance(block = ((BlockItem) stack.getItem()).getBlock()))
                continue;
            return i;
        }
        return -1;
    }

    public static int findHotbarBlock(Block blockIn) {
        for (int i = 0; i < 9; ++i) {
            Block block;
            ItemStack stack = InventoryUtil.mc.player.getInventory().getStack(i);
            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof BlockItem) || (block = ((BlockItem) stack.getItem()).getBlock()) != blockIn)
                continue;
            return i;
        }
        return -1;
    }

    public static int getItemHotbar(Item input) {
        for (int i = 0; i < 9; ++i) {
            Item item = InventoryUtil.mc.player.getInventory().getStack(i).getItem();
            if (Item.getRawId(item) != Item.getRawId(input)) continue;
            return i;
        }
        return -1;
    }

    public static int findStackInventory(Item input) {
        return InventoryUtil.findStackInventory(input, false);
    }

    public static int findStackInventory(Item input, boolean withHotbar) {
        int i;
        int n = i = withHotbar ? 0 : 9;
        while (i < 36) {
            Item item = InventoryUtil.mc.player.getInventory().getStack(i).getItem();
            if (Item.getRawId(input) == Item.getRawId(item)) {
                return i + (i < 9 ? 36 : 0);
            }
            ++i;
        }
        return -1;
    }

    public static int findItemInventorySlot(Item item, boolean offHand) {
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (entry.getValue().getItem() != item || entry.getKey() == 45 && !offHand) continue;
            slot.set(entry.getKey());
            return slot.get();
        }
        return slot.get();
    }

    public static List<Integer> findEmptySlots(boolean withXCarry) {
        ArrayList<Integer> outPut = new ArrayList<Integer>();
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (!entry.getValue().isEmpty() && entry.getValue().getItem() != Items.AIR) continue;
            outPut.add(entry.getKey());
        }
        if (withXCarry) {
            for (int i = 1; i < 5; ++i) {
                Slot craftingSlot = InventoryUtil.mc.player.playerScreenHandler.getSlot(i);
                ItemStack craftingStack = craftingSlot.getStack();
                if (!craftingStack.isEmpty() && craftingStack.getItem() != Items.AIR) continue;
                outPut.add(Integer.valueOf(i));
            }
        }
        return outPut;
    }

    public static int findInventoryBlock(Class clazz, boolean offHand) {
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (!InventoryUtil.isBlock(entry.getValue().getItem(), clazz) || entry.getKey() == 45 && !offHand) continue;
            slot.set(entry.getKey());
            return slot.get();
        }
        return slot.get();
    }
    //Ai
//    public static int findInventoryWool(boolean offHand) {
//        AtomicInteger slot = new AtomicInteger();
//        slot.set(-1);
//
//        for (Map.Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
//            ItemStack stack = entry.getValue();
//
//            if (stack.getItem() instanceof BlockItem) {
//                BlockItem blockItem = (BlockItem) stack.getItem();
//
//                if (blockItem.getBlock() == CLOTH &&
//                    (entry.getKey() != 45 || offHand)) {
//                    slot.set(entry.getKey());
//                    return slot.get();
//                }
//            }
//        }
//
//        return slot.get();
//    }
    public static int findEmptySlot() {
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (!entry.getValue().isEmpty()) continue;
            slot.set(entry.getKey());
            return slot.get();
        }
        return slot.get();
    }

    public static boolean isBlock(Item item, Class clazz) {
        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();
            return clazz.isInstance(block);
        }
        return false;
    }

//    public static void confirmSlot(int slot) {
//        Util.mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, ItemStack.EMPTY));
//        Util.mc.player.playerScreenHandler.setStackInHand(slot, ItemStack.EMPTY);
//        Util.mc.player.updateCursorStack();
//    }

    public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
        if (InventoryUtil.mc.currentScreen instanceof CraftingScreen) {
            return InventoryUtil.fuckYou3arthqu4kev2(10, 45);
        }
        return InventoryUtil.getInventorySlots(9, 44);
    }

    private static Map<Integer, ItemStack> getInventorySlots(int currentI, int last) {
        HashMap<Integer, ItemStack> fullInventorySlots = new HashMap<Integer, ItemStack>();
        for (int current = currentI; current <= last; ++current) {
            fullInventorySlots.put(current, InventoryUtil.mc.player.playerScreenHandler.getSlot(current).getStack());
        }
        return fullInventorySlots;
    }

    private static Map<Integer, ItemStack> fuckYou3arthqu4kev2(int currentI, int last) {
        HashMap<Integer, ItemStack> fullInventorySlots = new HashMap<Integer, ItemStack>();
        for (int current = currentI; current <= last; ++current) {
            fullInventorySlots.put(current, InventoryUtil.mc.player.playerScreenHandler.getSlot(current).getStack());
        }
        return fullInventorySlots;
    }

//    public static boolean[] switchItem(boolean back, int lastHotbarSlot, boolean switchedItem, Switch mode, Class clazz) {
//        boolean[] switchedItemSwitched = new boolean[]{switchedItem, false};
//        switch (mode) {
//            case NORMAL: {
//                if (!back && !switchedItem) {
//                    InventoryUtil.switchToHotbarSlot(InventoryUtil.findHotbarBlock(clazz), false);
//                    switchedItemSwitched[0] = true;
//                } else if (back && switchedItem) {
//                    InventoryUtil.switchToHotbarSlot(lastHotbarSlot, false);
//                    switchedItemSwitched[0] = false;
//                }
//                switchedItemSwitched[1] = true;
//                break;
//            }
//            case SILENT: {
//                if (!back && !switchedItem) {
//                    InventoryUtil.switchToHotbarSlot(InventoryUtil.findHotbarBlock(clazz), true);
//                    switchedItemSwitched[0] = true;
//                } else if (back && switchedItem) {
//                    switchedItemSwitched[0] = false;
//                    inventoryManager.recoverSilent(lastHotbarSlot);
//                }
//                switchedItemSwitched[1] = true;
//                break;
//            }
//            case NONE: {
//                switchedItemSwitched[1] = back || InventoryUtil.mc.player.getInventory().selectedSlot == InventoryUtil.findHotbarBlock(clazz);
//            }
//        }
//        return switchedItemSwitched;
//    }

//    public static boolean[] switchItemToItem(boolean back, int lastHotbarSlot, boolean switchedItem, Switch mode, Item item) {
//        boolean[] switchedItemSwitched = new boolean[]{switchedItem, false};
//        switch (mode) {
//            case NORMAL: {
//                if (!back && !switchedItem) {
//                    InventoryUtil.switchToHotbarSlot(InventoryUtil.getItemHotbar(item), false);
//                    switchedItemSwitched[0] = true;
//                } else if (back && switchedItem) {
//                    InventoryUtil.switchToHotbarSlot(lastHotbarSlot, false);
//                    switchedItemSwitched[0] = false;
//                }
//                switchedItemSwitched[1] = true;
//                break;
//            }
//            case SILENT: {
//                if (!back && !switchedItem) {
//                    InventoryUtil.switchToHotbarSlot(InventoryUtil.getItemHotbar(item), true);
//                    switchedItemSwitched[0] = true;
//                } else if (back && switchedItem) {
//                    switchedItemSwitched[0] = false;
//                    inventoryManager.recoverSilent(lastHotbarSlot);
//                }
//                switchedItemSwitched[1] = true;
//                break;
//            }
//            case NONE: {
//                switchedItemSwitched[1] = back || InventoryUtil.mc.player.getInventory().selectedSlot == InventoryUtil.getItemHotbar(item);
//            }
//        }
//        return switchedItemSwitched;
//    }

    public static boolean holdingItem(Class clazz) {
        boolean result = false;
        ItemStack stack = InventoryUtil.mc.player.getMainHandStack();
        result = InventoryUtil.isInstanceOf(stack, clazz);
        if (!result) {
            ItemStack offhand = InventoryUtil.mc.player.getOffHandStack();
            result = InventoryUtil.isInstanceOf(stack, clazz);
        }
        return result;
    }

    public static boolean isInstanceOf(ItemStack stack, Class clazz) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        if (clazz.isInstance(item)) {
            return true;
        }
        if (item instanceof BlockItem) {
            Block block = Block.getBlockFromItem(item);
            return clazz.isInstance(block);
        }
        return false;
    }

    public static int getEmptyXCarry() {
        for (int i = 1; i < 5; ++i) {
            Slot craftingSlot = InventoryUtil.mc.player.playerScreenHandler.getSlot(i);
            ItemStack craftingStack = craftingSlot.getStack();
            if (!craftingStack.isEmpty() && craftingStack.getItem() != Items.AIR) continue;
            return i;
        }
        return -1;
    }

    public static boolean isSlotEmpty(int i) {
        Slot slot = InventoryUtil.mc.player.playerScreenHandler.getSlot(i);
        ItemStack stack = slot.getStack();
        return stack.isEmpty();
    }

    public static int convertHotbarToInv(int input) {
        return 36 + input;
    }

//    public static boolean areStacksCompatible(ItemStack stack1, ItemStack stack2) {
//        if (!stack1.getItem().equals(stack2.getItem())) {
//            return false;
//        }
//        if (stack1.getItem() instanceof BlockItem && stack2.getItem() instanceof BlockItem) {
//            Block block1 = ((BlockItem) stack1.getItem()).getBlock();
//            Block block2 = ((BlockItem) stack2.getItem()).getBlock();
//            if (!block1.material.equals(block2.material)) {
//                return false;
//            }
//        }
//        if (!stack1.getDisplayName().equals(stack2.getDisplayName())) {
//            return false;
//        }
//        return stack1.getDamage() == stack2.getDamage();
//    }

//    public static EntityEquipmentSlot getEquipmentFromSlot(int slot) {
//        if (slot == 5) {
//            return EntityEquipmentSlot.HEAD;
//        }
//        if (slot == 6) {
//            return EntityEquipmentSlot.CHEST;
//        }
//        if (slot == 7) {
//            return EntityEquipmentSlot.LEGS;
//        }
//        return EntityEquipmentSlot.FEET;
//    }

//    public static int findArmorSlot(EquipmentSlot type, boolean binding) {
//        int slot = -1;
//        float damage = 0.0f;
//        for (int i = 9; i < 45; ++i) {
//            boolean cursed;
//            ItemStack s = MinecraftClient.getInstance().player.playerScreenHandler.getSlot(i).getStack();
//            if (s.getItem() == Items.AIR || !(s.getItem() instanceof ArmorItem)) continue;
//            ArmorItem armor = (ArmorItem) s.getItem();
//            if (armor.getType() != type) continue;
//            float currentDamage = armor.damageReduceAmount + EnchantmentHelper.getLevel(Enchantments.PROTECTION, s);
//            boolean bl = cursed = binding && EnchantmentHelper.hasBindingCurse(s);
//            if (!(currentDamage > damage) || cursed) continue;
//            damage = currentDamage;
//            slot = i;
//        }
//        return slot;
//    }

//    public static int findArmorSlot(EquipmentSlot type, boolean binding, boolean withXCarry) {
//        int slot = InventoryUtil.findArmorSlot(type, binding);
//        if (slot == -1 && withXCarry) {
//            float damage = 0.0f;
//            for (int i = 1; i < 5; ++i) {
//                boolean cursed;
//                Slot craftingSlot = InventoryUtil.mc.player.playerScreenHandler.getSlot(i);
//                ItemStack craftingStack = craftingSlot.getStack();
//                if (craftingStack.getItem() == Items.AIR || !(craftingStack.getItem() instanceof ArmorItem)) continue;
//                ArmorItem armor = (ArmorItem) craftingStack.getItem();
//                if (armor.getType() != type) continue;
//                float currentDamage = armor.damageReduceAmount + EnchantmentHelper.getLevel(Enchantments.PROTECTION, craftingStack);
//                boolean bl = cursed = binding && EnchantmentHelper.hasBindingCurse(craftingStack);
//                if (!(currentDamage > damage) || cursed) continue;
//                damage = currentDamage;
//                slot = i;
//            }
//        }
//        return slot;
//    }

    public static int findItemInventorySlot(Item item, boolean offHand, boolean withXCarry) {
        int slot = InventoryUtil.findItemInventorySlot(item, offHand);
        if (slot == -1 && withXCarry) {
            for (int i = 1; i < 5; ++i) {
                Item craftingStackItem;
                Slot craftingSlot = InventoryUtil.mc.player.playerScreenHandler.getSlot(i);
                ItemStack craftingStack = craftingSlot.getStack();
                if (craftingStack.getItem() == Items.AIR || (craftingStackItem = craftingStack.getItem()) != item)
                    continue;
                slot = i;
            }
        }
        return slot;
    }

//    public static int findBlockSlotInventory(Class clazz, boolean offHand, boolean withXCarry) {
//        int slot = InventoryUtil.findInventoryBlock(clazz, offHand);
//        if (slot == -1 && withXCarry) {
//            for (int i = 1; i < 5; ++i) {
//                Block block;
//                Slot craftingSlot = InventoryUtil.mc.player.getInventory().getSlot(i);
//                ItemStack craftingStack = craftingSlot.getStack();
//                if (craftingStack.getItem() == Items.AIR) continue;
//                Item craftingStackItem = craftingStack.getItem();
//                if (clazz.isInstance(craftingStackItem)) {
//                    slot = i;
//                    continue;
//                }
//                if (!(craftingStackItem instanceof BlockItem) || !clazz.isInstance(block = ((BlockItem) craftingStackItem).getBlock()))
//                    continue;
//                slot = i;
//            }
//        }
//        return slot;
//    }

    public enum Switch {
        NORMAL,
        SILENT,
        NONE

    }

    public static class Task {
        private final int slot;
        private final boolean update;
        private final boolean quickClick;

        public Task() {
            this.update = true;
            this.slot = -1;
            this.quickClick = false;
        }

        public Task(int slot) {
            this.slot = slot;
            this.quickClick = false;
            this.update = false;
        }

        public Task(int slot, boolean quickClick) {
            this.slot = slot;
            this.quickClick = quickClick;
            this.update = false;
        }

//        public void run() {
//            if (this.update) {
//                Util.mc.playerScreenHandler.updateController();
//            }
//            if (this.slot != -1) {
//                Util.mc.playerScreenHandler.windowClick(Util.mc.player. playerScreenHandler.windowId, this.slot, 0, this.quickClick ? ClickType.QUICK_MOVE : ClickType.PICKUP, Util.mc.player);
//            }
//        }

        public boolean isSwitching() {
            return !this.update;
        }
    }
}
