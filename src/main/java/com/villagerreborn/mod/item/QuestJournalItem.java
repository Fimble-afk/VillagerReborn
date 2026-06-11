package com.villagerreborn.mod.item;

import com.villagerreborn.mod.capability.VillageRelationCapability;
import com.villagerreborn.mod.data.VillageRelationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A craftable Quest Journal item. Right-click to view all village standings
 * and active quests.
 */
public class QuestJournalItem extends Item {

    public QuestJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        player.getCapability(VillageRelationCapability.VILLAGE_RELATION).ifPresent(cap -> {
            VillageRelationData data = cap.getData();
            CompoundTag nbt = data.serializeNBT();
            CompoundTag pointsTag = nbt.getCompound("RelationPoints");
            CompoundTag questTag  = nbt.getCompound("ActiveQuests");

            player.sendSystemMessage(Component.literal("§6=== Quest Journal ==="));

            if (pointsTag.getAllKeys().isEmpty()) {
                player.sendSystemMessage(Component.literal("§7No villages visited yet."));
            } else {
                for (String key : pointsTag.getAllKeys()) {
                    int pts = data.getPoints(key);
                    int rank = data.getRank(key);
                    String rankName = data.getRankName(key);
                    int toNext = data.getPointsToNextRank(key);
                    int emeralds = data.getDailyEmeralds(key);
                    player.sendSystemMessage(Component.literal(
                        "§eVillage [" + key + "]: §f" + rankName
                        + " (" + pts + " pts"
                        + (toNext > 0 ? ", " + toNext + " to next" : "")
                        + ")"
                        + (emeralds > 0 ? " | Daily: " + emeralds + " ✦" : "")
                    ));
                }
            }

            player.sendSystemMessage(Component.literal("§6--- Active Quests ---"));
            if (questTag.getAllKeys().isEmpty()) {
                player.sendSystemMessage(Component.literal("§7No active quests."));
            } else {
                for (String villagerUUID : questTag.getAllKeys()) {
                    String questId = data.getActiveQuestId(villagerUUID);
                    com.villagerreborn.mod.quest.QuestManager.QuestDefinition quest =
                        com.villagerreborn.mod.quest.QuestManager.ALL_QUESTS.stream()
                            .filter(q -> q.id.equals(questId)).findFirst().orElse(null);
                    if (quest == null) continue;

                    CompoundTag progress = data.getQuestProgress(villagerUUID);
                    int prog = progress.getInt("progress");
                    player.sendSystemMessage(Component.literal(
                        "§b• " + quest.title + " §7— " + quest.description));
                    if (quest.type == com.villagerreborn.mod.quest.QuestManager.QuestType.KILL_MOBS) {
                        player.sendSystemMessage(Component.literal(
                            "  §7Kill " + quest.targetMob.replace("minecraft:", "")
                            + " (" + prog + "/" + quest.targetCount + ")"));
                    } else {
                        int have = com.villagerreborn.mod.quest.QuestManager
                            .countItemsInInventory(player, quest.targetItem);
                        player.sendSystemMessage(Component.literal(
                            "  §7Bring " + quest.targetCount + "x "
                            + quest.targetItem.replace("minecraft:", "")
                            + " (have: " + have + ")"));
                    }
                    player.sendSystemMessage(Component.literal(
                        "  §aReward: " + quest.emeraldReward + " emeralds + "
                        + quest.relationReward + " relation"));
                }
            }
        });

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§6Quest Journal");
    }
}
