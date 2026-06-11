package com.villagerreborn.mod.capability;

import com.villagerreborn.mod.data.VillageRelationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;

public class VillageRelationCapability implements INBTSerializable<CompoundTag> {

    public static final Capability<VillageRelationCapability> VILLAGE_RELATION =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final VillageRelationData data = new VillageRelationData();

    public VillageRelationData getData() {
        return data;
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(VillageRelationCapability.class);
    }
}
