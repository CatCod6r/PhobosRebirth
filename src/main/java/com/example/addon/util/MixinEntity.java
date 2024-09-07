package com.example.addon.util;

import java.util.function.Supplier;

public class MixinEntity {
    private Supplier<EntityType> type;
    public EntityType getType()
    {
        return type.get();
    }
}
