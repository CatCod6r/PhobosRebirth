package com.example.addon.event.events;

import com.example.addon.event.EventStage;
import meteordevelopment.meteorclient.events.Cancellable;
public class UpdateWalkingPlayerEvent extends EventStage {
    public UpdateWalkingPlayerEvent(int stage) {
        super(stage);
    }
}
