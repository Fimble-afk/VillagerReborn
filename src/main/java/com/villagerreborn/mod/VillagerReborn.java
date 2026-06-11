package com.villagerreborn.mod;

import com.villagerreborn.mod.capability.VillageRelationProvider;
import com.villagerreborn.mod.event.VillagerEventHandler;
import com.villagerreborn.mod.event.PlayerEventHandler;
import com.villagerreborn.mod.network.PacketHandler;
import com.villagerreborn.mod.registry.ModCapabilities;
import com.villagerreborn.mod.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VillagerReborn.MOD_ID)
public class VillagerReborn {
    public static final String MOD_ID = "villagerreborn";
    public static final Logger LOGGER = LogManager.getLogger();

    public VillagerReborn() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new VillagerEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
        ModCapabilities.register();
    }
}
