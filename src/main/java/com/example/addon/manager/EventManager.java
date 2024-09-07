package com.example.addon.manager;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import java.util.concurrent.atomic.AtomicBoolean;

public class EventManager {
    private final AtomicBoolean tickOngoing = new AtomicBoolean(false);
    @EventHandler(priority = EventPriority.HIGHEST)
    public void TickEvent(TickEvent event) {
            this.tickOngoing.set(true);
    }
    public boolean ticksOngoing() {
        return this.tickOngoing.get();
    }
}
