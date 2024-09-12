package com.example.addon.manager;

import net.minecraft.entity.player.PlayerInventory;

import static com.example.addon.util.Util.mc;

public class InventoryManager {
    public void copyInventory(PlayerInventory playerInventory) {
        int i = 0;
        while(i < mc.player.getInventory().size()) {
            ++i;
            mc.player.getInventory().insertStack(i, playerInventory.getStack(i));
        }
        mc.player.getInventory().selectedSlot = playerInventory.selectedSlot;
    }
}
