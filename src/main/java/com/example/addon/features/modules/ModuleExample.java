package com.example.addon.features.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ModuleExample extends Module {
    public ModuleExample() {
        super(Addon.CATEGORY, "example", "An example module in a custom category.");
    }
}
