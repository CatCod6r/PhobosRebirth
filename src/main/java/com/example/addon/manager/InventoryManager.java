package com.example.addon.manager;

import net.minecraft.entity.player.PlayerInventory;

import static com.example.addon.util.Util.mc;

public class InventoryManager {
    public void copyInventory(PlayerInventory p_copyInventory_1_) {
        int i =0;
        while(i < mc.player.getInventory().size()) {
            ++i;
            mc.player.getInventory().insertStack(i, p_copyInventory_1_.getStack(i));
        }

        mc.player.getInventory().selectedSlot = p_copyInventory_1_.selectedSlot;
    }
}
