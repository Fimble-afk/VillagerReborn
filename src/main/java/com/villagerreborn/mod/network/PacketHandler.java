package com.villagerreborn.mod.network;

import com.villagerreborn.mod.VillagerReborn;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Handles client-server networking.
 * Expanded in future versions to sync village data to client HUD.
 */
public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new net.minecraft.resources.ResourceLocation(VillagerReborn.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // Future packets registered here:
        // CHANNEL.registerMessage(packetId++, SyncVillageDataPacket.class,
        //     SyncVillageDataPacket::encode,
        //     SyncVillageDataPacket::decode,
        //     SyncVillageDataPacket::handle);
        VillagerReborn.LOGGER.info("VillagerReborn network channel registered.");
    }
}
