package com.villagerreborn.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * Stores all village relationship data for a single player.
 * Keyed by the UUID string of the nearest village bell position.
 */
public class VillageRelationData {

    // Relation thresholds for each rank
    public static final int RANK_STRANGER    = 0;
    public static final int RANK_ACQUAINTANCE = 100;
    public static final int RANK_FRIEND       = 300;
    public static final int RANK_ALLY         = 700;
    public static final int RANK_CHAMPION     = 1500;

    public static final String[] RANK_NAMES = {
        "Stranger", "Acquaintance", "Friend", "Ally", "Champion"
    };

    public static final int[] RANK_THRESHOLDS = {
        RANK_STRANGER, RANK_ACQUAINTANCE, RANK_FRIEND, RANK_ALLY, RANK_CHAMPION
    };

    // Maps village key -> relation points
    private final Map<String, Integer> relationPoints = new HashMap<>();
    // Maps village key -> last time daily reward was claimed (game day)
    private final Map<String, Long> lastRewardDay = new HashMap<>();
    // Maps villager UUID -> active quest id
    private final Map<String, String> activeQuests = new HashMap<>();
    // Maps villager UUID -> quest progress data (serialized CompoundTag)
    private final Map<String, CompoundTag> questProgress = new HashMap<>();
    // Tracks whether player has a personal golem per village
    private final Set<String> personalGolems = new HashSet<>();

    // ---- Relation Points ----

    public int getPoints(String villageKey) {
        return relationPoints.getOrDefault(villageKey, 0);
    }

    public void addPoints(String villageKey, int amount) {
        int current = getPoints(villageKey);
        relationPoints.put(villageKey, Math.max(0, current + amount));
    }

    public int getRank(String villageKey) {
        int pts = getPoints(villageKey);
        int rank = 0;
        for (int i = RANK_THRESHOLDS.length - 1; i >= 0; i--) {
            if (pts >= RANK_THRESHOLDS[i]) {
                rank = i;
                break;
            }
        }
        return rank;
    }

    public String getRankName(String villageKey) {
        return RANK_NAMES[getRank(villageKey)];
    }

    public int getPointsToNextRank(String villageKey) {
        int rank = getRank(villageKey);
        if (rank >= RANK_NAMES.length - 1) return 0;
        return RANK_THRESHOLDS[rank + 1] - getPoints(villageKey);
    }

    // ---- Daily Rewards ----

    public boolean canClaimDailyReward(String villageKey, long currentDay) {
        long last = lastRewardDay.getOrDefault(villageKey, -1L);
        return currentDay > last;
    }

    public void claimDailyReward(String villageKey, long currentDay) {
        lastRewardDay.put(villageKey, currentDay);
    }

    public int getDailyEmeralds(String villageKey) {
        int rank = getRank(villageKey);
        return switch (rank) {
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default -> 0;
        };
    }

    // ---- Quests ----

    public void assignQuest(String villagerUUID, String questId, CompoundTag data) {
        activeQuests.put(villagerUUID, questId);
        questProgress.put(villagerUUID, data);
    }

    public String getActiveQuestId(String villagerUUID) {
        return activeQuests.get(villagerUUID);
    }

    public CompoundTag getQuestProgress(String villagerUUID) {
        return questProgress.getOrDefault(villagerUUID, new CompoundTag());
    }

    public void updateQuestProgress(String villagerUUID, CompoundTag data) {
        if (activeQuests.containsKey(villagerUUID)) {
            questProgress.put(villagerUUID, data);
        }
    }

    public void completeQuest(String villagerUUID) {
        activeQuests.remove(villagerUUID);
        questProgress.remove(villagerUUID);
    }

    public boolean hasActiveQuest(String villagerUUID) {
        return activeQuests.containsKey(villagerUUID);
    }

    // ---- Personal Golem ----

    public boolean hasPersonalGolem(String villageKey) {
        return personalGolems.contains(villageKey);
    }

    public void setPersonalGolem(String villageKey, boolean value) {
        if (value) personalGolems.add(villageKey);
        else personalGolems.remove(villageKey);
    }

    // ---- NBT Serialization ----

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        CompoundTag pointsTag = new CompoundTag();
        relationPoints.forEach(pointsTag::putInt);
        tag.put("RelationPoints", pointsTag);

        CompoundTag rewardTag = new CompoundTag();
        lastRewardDay.forEach(rewardTag::putLong);
        tag.put("LastRewardDay", rewardTag);

        CompoundTag questTag = new CompoundTag();
        activeQuests.forEach(questTag::putString);
        tag.put("ActiveQuests", questTag);

        CompoundTag progressTag = new CompoundTag();
        questProgress.forEach(progressTag::put);
        tag.put("QuestProgress", progressTag);

        CompoundTag golemTag = new CompoundTag();
        personalGolems.forEach(k -> golemTag.putBoolean(k, true));
        tag.put("PersonalGolems", golemTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        relationPoints.clear();
        lastRewardDay.clear();
        activeQuests.clear();
        questProgress.clear();
        personalGolems.clear();

        CompoundTag pointsTag = tag.getCompound("RelationPoints");
        for (String k : pointsTag.getAllKeys()) {
            relationPoints.put(k, pointsTag.getInt(k));
        }

        CompoundTag rewardTag = tag.getCompound("LastRewardDay");
        for (String k : rewardTag.getAllKeys()) {
            lastRewardDay.put(k, rewardTag.getLong(k));
        }

        CompoundTag questTag = tag.getCompound("ActiveQuests");
        for (String k : questTag.getAllKeys()) {
            activeQuests.put(k, questTag.getString(k));
        }

        CompoundTag progressTag = tag.getCompound("QuestProgress");
        for (String k : progressTag.getAllKeys()) {
            questProgress.put(k, progressTag.getCompound(k));
        }

        CompoundTag golemTag = tag.getCompound("PersonalGolems");
        for (String k : golemTag.getAllKeys()) {
            if (golemTag.getBoolean(k)) personalGolems.add(k);
        }
    }
}
