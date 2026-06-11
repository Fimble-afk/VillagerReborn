package com.villagerreborn.mod.registry;

import com.villagerreborn.mod.VillagerReborn;
import com.villagerreborn.mod.item.QuestJournalItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, VillagerReborn.MOD_ID);

    public static final RegistryObject<Item> QUEST_JOURNAL = ITEMS.register("quest_journal",
        () -> new QuestJournalItem(new Item.Properties()
            .stacksTo(1)));
}
