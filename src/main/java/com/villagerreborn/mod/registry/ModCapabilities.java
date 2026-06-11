package com.villagerreborn.mod.registry;

import com.villagerreborn.mod.capability.VillageRelationCapability;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "villagerreborn", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        VillageRelationCapability.register(event);
    }

    public static void register() {
        // Called during common setup; actual registration happens via @SubscribeEvent above
    }
}
