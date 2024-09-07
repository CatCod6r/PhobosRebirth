package com.example.addon.event.events;

import com.example.addon.event.EventStage;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;


public class ProcessRightClickBlockEvent
    extends EventStage {
    public BlockPos pos;
    public Hand hand;
    public ItemStack stack;

    public ProcessRightClickBlockEvent(BlockPos pos, Hand hand, ItemStack stack) {
        this.pos = pos;
        this.hand = hand;
        this.stack = stack;
    }
}
