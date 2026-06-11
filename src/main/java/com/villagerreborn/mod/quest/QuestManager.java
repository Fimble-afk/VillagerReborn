package com.villagerreborn.mod.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.EntityType;

import java.util.*;

/**
 * Defines all quest templates and handles quest logic.
 */
public class QuestManager {

    public enum QuestType {
        DELIVER_ITEM,
        KILL_MOBS,
        GATHER_RESOURCES,
        BUILD_STRUCTURE,
        ESCORT_VILLAGER
    }

    public static class QuestDefinition {
        public final String id;
        public final String title;
        public final String description;
        public final QuestType type;
        public final int relationReward;
        public final int emeraldReward;
        // For DELIVER_ITEM / GATHER_RESOURCES: item registry name and count
        public final String targetItem;
        public final int targetCount;
        // For KILL_MOBS: mob type name
        public final String targetMob;

        public QuestDefinition(String id, String title, String description, QuestType type,
                               int relationReward, int emeraldReward,
                               String targetItem, int targetCount, String targetMob) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.relationReward = relationReward;
            this.emeraldReward = emeraldReward;
            this.targetItem = targetItem;
            this.targetCount = targetCount;
            this.targetMob = targetMob;
        }
    }

    // All available quests
    public static final List<QuestDefinition> ALL_QUESTS = new ArrayList<>();

    static {
        // Item delivery quests
        ALL_QUESTS.add(new QuestDefinition(
            "bread_delivery", "Daily Bread",
            "Our baker needs flour. Could you bring us 8 pieces of bread?",
            QuestType.DELIVER_ITEM, 40, 3,
            "minecraft:bread", 8, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "wood_delivery", "Lumber Supply",
            "We're running low on wood for construction. Bring 32 oak logs.",
            QuestType.DELIVER_ITEM, 50, 4,
            "minecraft:oak_log", 32, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "stone_delivery", "Stonework",
            "Our mason needs 32 cobblestone to repair the road.",
            QuestType.DELIVER_ITEM, 40, 3,
            "minecraft:cobblestone", 32, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "wool_delivery", "Wool for Warmth",
            "Winter is coming. Please bring us 16 wool.",
            QuestType.DELIVER_ITEM, 45, 3,
            "minecraft:white_wool", 16, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "coal_delivery", "Fuel the Forge",
            "The smithy needs coal to keep the forge burning. Bring 16 coal.",
            QuestType.DELIVER_ITEM, 45, 4,
            "minecraft:coal", 16, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "iron_delivery", "Iron for Tools",
            "Our blacksmith needs iron ingots to craft new tools. Bring 8.",
            QuestType.DELIVER_ITEM, 60, 5,
            "minecraft:iron_ingot", 8, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "pumpkin_delivery", "Harvest Festival",
            "We need 6 pumpkins for the village harvest festival!",
            QuestType.DELIVER_ITEM, 35, 2,
            "minecraft:pumpkin", 6, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "apple_delivery", "Fruit for the Children",
            "The children are hungry. Bring 10 apples.",
            QuestType.DELIVER_ITEM, 30, 2,
            "minecraft:apple", 10, null
        ));

        // Kill mob quests
        ALL_QUESTS.add(new QuestDefinition(
            "kill_zombies", "Undead Threat",
            "Zombies have been spotted near the village at night. Slay 5 of them!",
            QuestType.KILL_MOBS, 70, 5,
            null, 5, "minecraft:zombie"
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "kill_skeletons", "Arrow Rain",
            "Skeletons have been harassing our guards. Kill 5 skeletons.",
            QuestType.KILL_MOBS, 70, 5,
            null, 5, "minecraft:skeleton"
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "kill_creepers", "Explosive Problem",
            "Creepers destroyed part of our wall! Eliminate 3 creepers.",
            QuestType.KILL_MOBS, 80, 6,
            null, 3, "minecraft:creeper"
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "kill_spiders", "Web Clearing",
            "Spiders have infested the nearby cave. Clear out 5 of them.",
            QuestType.KILL_MOBS, 65, 4,
            null, 5, "minecraft:spider"
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "kill_pillagers", "Pillager Patrol",
            "A pillager patrol was spotted on the road. Dispatch 4 of them!",
            QuestType.KILL_MOBS, 100, 8,
            null, 4, "minecraft:pillager"
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "kill_witches", "Witchcraft",
            "A witch has been cursing our crops. Deal with her and 2 more of her kind.",
            QuestType.KILL_MOBS, 90, 7,
            null, 3, "minecraft:witch"
        ));

        // Gather resource quests
        ALL_QUESTS.add(new QuestDefinition(
            "gather_gravel", "Road Gravel",
            "Help us pave the road! Gather 32 gravel.",
            QuestType.GATHER_RESOURCES, 35, 2,
            "minecraft:gravel", 32, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "gather_sand", "Glass for Windows",
            "We want to make glass for our windows. Bring 16 sand.",
            QuestType.GATHER_RESOURCES, 35, 2,
            "minecraft:sand", 16, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "gather_leather", "Leatherwork",
            "Our leather worker needs 8 leather hides.",
            QuestType.GATHER_RESOURCES, 45, 3,
            "minecraft:leather", 8, null
        ));
        ALL_QUESTS.add(new QuestDefinition(
            "gather_string", "Bowyer's Request",
            "The fletcher needs 16 string for new bows.",
            QuestType.GATHER_RESOURCES, 40, 3,
            "minecraft:string", 16, null
        ));
    }

    private static final Random RANDOM = new Random();

    /**
     * Returns a random quest appropriate for the player's rank with this village.
     */
    public static QuestDefinition getRandomQuest(int rank) {
        List<QuestDefinition> pool;
        if (rank >= 3) {
            pool = ALL_QUESTS; // Champions get all quests
        } else if (rank >= 2) {
            pool = ALL_QUESTS.subList(0, ALL_QUESTS.size() - 4); // No hardest quests
        } else {
            // Low rank: only simple item deliveries and easy kills
            pool = ALL_QUESTS.stream()
                .filter(q -> q.relationReward <= 60)
                .toList();
        }
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    /**
     * Initialize quest progress NBT for a quest.
     */
    public static CompoundTag initProgress(QuestDefinition quest) {
        CompoundTag tag = new CompoundTag();
        tag.putString("questId", quest.id);
        tag.putInt("progress", 0);
        tag.putInt("target", quest.targetCount);
        return tag;
    }

    /**
     * Check if player inventory satisfies a DELIVER_ITEM or GATHER_RESOURCES quest.
     */
    public static int countItemsInInventory(Player player, String itemId) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem().getRegistryName() != null
                    && stack.getItem().getRegistryName().toString().equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Remove items from player inventory for quest completion.
     */
    public static boolean consumeItems(Player player, String itemId, int count) {
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem().getRegistryName() != null
                    && stack.getItem().getRegistryName().toString().equals(itemId)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) return true;
            }
        }
        return remaining <= 0;
    }
}
