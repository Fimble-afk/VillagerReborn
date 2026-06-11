package com.villagerreborn.mod.event;

import com.villagerreborn.mod.capability.VillageRelationCapability;
import com.villagerreborn.mod.data.VillageRelationData;
import com.villagerreborn.mod.quest.QuestManager;
import com.villagerreborn.mod.village.VillageHelper;
import com.villagerreborn.mod.villager.VillagerDialogue;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.pillager.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

/**
 * Handles all villager-related events: talking, questing, mob kills.
 */
public class VillagerEventHandler {

    private static final Random RANDOM = new Random();
    // Cooldown map: player UUID -> last talk time (ms)
    private final java.util.Map<java.util.UUID, Long> talkCooldowns = new java.util.HashMap<>();
    private static final long TALK_COOLDOWN_MS = 3000;

    // ---- Player right-clicks a villager ----

    @SubscribeEvent
    public void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        if (player.level.isClientSide) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        Long last = talkCooldowns.get(player.getUUID());
        if (last != null && now - last < TALK_COOLDOWN_MS) return;
        talkCooldowns.put(player.getUUID(), now);

        player.getCapability(VillageRelationCapability.VILLAGE_RELATION).ifPresent(cap -> {
            VillageRelationData data = cap.getData();
            String villageKey = VillageHelper.getVillageKey(villager);
            String villagerUUID = villager.getStringUUID();
            int rank = data.getRank(villageKey);
            int oldRank = rank;
            boolean isNight = !player.level.isDay();
            VillagerProfession prof = villager.getVillagerData().getProfession();

            // --- Small talk gives +1 relation (max once per villager per session via cooldown) ---
            data.addPoints(villageKey, 1);

            // Send greeting
            sendVillagerMessage(player, villager,
                VillagerDialogue.getGreeting(rank, prof, isNight));

            // Send profession chitchat if rank >= 1
            if (rank >= 1 && RANDOM.nextFloat() < 0.6f) {
                sendVillagerMessage(player, villager,
                    VillagerDialogue.getProfessionChitchat(prof));
            }

            // Village expansion chitchat if rank >= 2
            if (rank >= 2 && RANDOM.nextFloat() < 0.25f) {
                sendVillagerMessage(player, villager,
                    VillagerDialogue.getExpansionLine());
            }

            // ---- Quest handling ----
            if (data.hasActiveQuest(villagerUUID)) {
                // Player has an active quest with this villager
                String questId = data.getActiveQuestId(villagerUUID);
                QuestManager.QuestDefinition quest = findQuestById(questId);

                if (quest != null) {
                    if (quest.type == QuestManager.QuestType.KILL_MOBS) {
                        // Kill quests track progress via LivingDeathEvent
                        CompoundTag progress = data.getQuestProgress(villagerUUID);
                        int killed = progress.getInt("progress");
                        sendVillagerMessage(player, villager,
                            VillagerDialogue.getQuestReminder());
                        player.sendSystemMessage(Component.literal(
                            VillagerDialogue.formatQuestMessage(quest, killed)));

                        if (killed >= quest.targetCount) {
                            completeQuest(player, data, villager, villageKey, villagerUUID, quest);
                        }
                    } else {
                        // Item delivery quest — check inventory
                        int count = QuestManager.countItemsInInventory(player, quest.targetItem);
                        player.sendSystemMessage(Component.literal(
                            VillagerDialogue.formatQuestMessage(quest, count)));

                        if (count >= quest.targetCount) {
                            // Complete the quest
                            if (QuestManager.consumeItems(player, quest.targetItem, quest.targetCount)) {
                                sendVillagerMessage(player, villager,
                                    VillagerDialogue.getQuestComplete());
                                completeQuest(player, data, villager, villageKey, villagerUUID, quest);
                            }
                        } else {
                            sendVillagerMessage(player, villager,
                                VillagerDialogue.getQuestReminder());
                        }
                    }
                }
            } else if (rank >= 0 && RANDOM.nextFloat() < 0.55f) {
                // Offer a new quest
                QuestManager.QuestDefinition quest = QuestManager.getRandomQuest(rank);
                data.assignQuest(villagerUUID, quest.id, QuestManager.initProgress(quest));

                sendVillagerMessage(player, villager, VillagerDialogue.getQuestOffer());
                player.sendSystemMessage(Component.literal(
                    VillagerDialogue.formatQuestMessage(quest, 0)));
            }

            // ---- Check for rank-up ----
            int newRank = data.getRank(villageKey);
            if (newRank > oldRank) {
                String rankMsg = VillagerDialogue.RANK_UP_MESSAGES.get(newRank);
                if (rankMsg != null) {
                    player.sendSystemMessage(Component.literal(rankMsg));
                }
                handleRankUpRewards(player, data, villageKey, newRank,
                    (ServerLevel) player.level);
            }

            // ---- Show current standing ----
            sendStatusBar(player, data, villageKey);
        });
    }

    // ---- Mob kill tracking for quests ----

    @SubscribeEvent
    public void onMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level.isClientSide) return;

        String mobType = event.getEntity().getType().getRegistryName() != null
            ? event.getEntity().getType().getRegistryName().toString() : "";

        player.getCapability(VillageRelationCapability.VILLAGE_RELATION).ifPresent(cap -> {
            VillageRelationData data = cap.getData();

            // Check all active quests for mob kill progress
            // We iterate over all possible villager UUIDs that might have active quests
            // (stored in data's quest map)
            for (String villagerUUID : new java.util.ArrayList<>(getActiveQuestVillagers(data))) {
                String questId = data.getActiveQuestId(villagerUUID);
                if (questId == null) continue;

                QuestManager.QuestDefinition quest = findQuestById(questId);
                if (quest == null || quest.type != QuestManager.QuestType.KILL_MOBS) continue;
                if (!quest.targetMob.equals(mobType)) continue;

                CompoundTag progress = data.getQuestProgress(villagerUUID);
                int current = progress.getInt("progress");
                current++;
                progress.putInt("progress", current);
                data.updateQuestProgress(villagerUUID, progress);

                player.sendSystemMessage(Component.literal(
                    "§7[" + quest.title + "] Kill progress: §f" + current + "/" + quest.targetCount));

                if (current >= quest.targetCount) {
                    player.sendSystemMessage(Component.literal(
                        "§a[Quest ready!] Return to the villager to claim your reward!"));
                }
            }

            // Bonus relation for protecting villages: killing mobs near a village
            if (player.level instanceof ServerLevel sLevel) {
                String villageKey = VillageHelper.getNearestVillageKey(player, sLevel);
                if (villageKey != null) {
                    boolean isThreat = event.getEntity() instanceof Zombie
                        || event.getEntity() instanceof Skeleton
                        || event.getEntity() instanceof Creeper
                        || event.getEntity() instanceof Spider
                        || event.getEntity() instanceof Witch
                        || event.getEntity() instanceof Pillager;

                    if (isThreat) {
                        data.addPoints(villageKey, 3);
                    }
                }
            }
        });
    }

    // ---- Helpers ----

    private void completeQuest(Player player, VillageRelationData data, Villager villager,
                               String villageKey, String villagerUUID,
                               QuestManager.QuestDefinition quest) {
        data.addPoints(villageKey, quest.relationReward);
        data.completeQuest(villagerUUID);

        // Give emerald reward
        ItemStack reward = new ItemStack(Items.EMERALD, quest.emeraldReward);
        player.getInventory().add(reward);

        player.sendSystemMessage(Component.literal(
            "§2Quest complete! +" + quest.relationReward + " relation, +"
            + quest.emeraldReward + " emerald(s)!"));
    }

    private void handleRankUpRewards(Player player, VillageRelationData data,
                                     String villageKey, int newRank, ServerLevel level) {
        // Immediate rank-up bonus emeralds
        int bonus = switch (newRank) {
            case 1 -> 5;
            case 2 -> 15;
            case 3 -> 30;
            case 4 -> 64;
            default -> 0;
        };
        if (bonus > 0) {
            player.getInventory().add(new ItemStack(Items.EMERALD, bonus));
            player.sendSystemMessage(Component.literal(
                "§aRank-up bonus: " + bonus + " emeralds!"));
        }

        // Champion rank: grant personal golem flag
        if (newRank >= 4 && !data.hasPersonalGolem(villageKey)) {
            data.setPersonalGolem(villageKey, true);
            player.sendSystemMessage(Component.literal(
                "§5A personal Iron Golem has been assigned to protect you in this village!"));
            // In a full implementation we'd spawn a tagged golem here
        }

        // Trigger village expansion build
        if (newRank >= 2) {
            boolean built = VillageHelper.tryExpandVillage(level, villageKey, newRank);
            if (built) {
                player.sendSystemMessage(Component.literal(
                    "§7The villagers celebrate your standing by expanding the village!"));
            }
        }
    }

    private void sendVillagerMessage(Player player, Villager villager, String text) {
        String profName = villager.getVillagerData().getProfession().toString()
            .replace("minecraft:", "");
        profName = Character.toUpperCase(profName.charAt(0)) + profName.substring(1);
        player.sendSystemMessage(Component.literal(
            "§e[" + profName + "] §f" + text));
    }

    private void sendStatusBar(Player player, VillageRelationData data, String villageKey) {
        int pts = data.getPoints(villageKey);
        int rank = data.getRank(villageKey);
        int toNext = data.getPointsToNextRank(villageKey);
        String rankName = data.getRankName(villageKey);
        String msg = "§7Village Standing: §e" + rankName + " §7(" + pts + " pts"
            + (toNext > 0 ? ", " + toNext + " to next rank" : " — MAX") + ")";
        player.sendSystemMessage(Component.literal(msg));
    }

    private QuestManager.QuestDefinition findQuestById(String id) {
        return QuestManager.ALL_QUESTS.stream()
            .filter(q -> q.id.equals(id))
            .findFirst().orElse(null);
    }

    /**
     * Retrieve all villager UUIDs that have active quests from the data.
     * Since we don't expose the internal map directly, we reflect over known villager UUIDs.
     * This works by checking the serialized NBT.
     */
    private java.util.Set<String> getActiveQuestVillagers(VillageRelationData data) {
        // Serialize and read back to get quest keys — pragmatic approach
        CompoundTag serialized = data.serializeNBT();
        CompoundTag questTag = serialized.getCompound("ActiveQuests");
        return questTag.getAllKeys();
    }
}
