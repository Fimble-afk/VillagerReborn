package com.villagerreborn.mod.event;

import com.villagerreborn.mod.capability.VillageRelationCapability;
import com.villagerreborn.mod.capability.VillageRelationProvider;
import com.villagerreborn.mod.data.VillageRelationData;
import com.villagerreborn.mod.village.VillageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Handles player capability attachment, daily rewards, and villager AI expansions.
 */
public class PlayerEventHandler {

    private static final ResourceLocation CAPABILITY_KEY =
        new ResourceLocation("villagerreborn", "village_relation");

    // Tick counter for periodic checks (avoid running every tick)
    private int tickCounter = 0;
    private static final int DAILY_CHECK_INTERVAL = 100; // every 5 seconds
    private static final int VILLAGER_AI_INTERVAL  = 200; // every 10 seconds

    // Track last AI update per villager
    private final Map<UUID, Long> lastVillagerAiTick = new HashMap<>();

    // ---- Capability attachment ----

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        VillageRelationProvider provider = new VillageRelationProvider();
        event.addCapability(CAPABILITY_KEY, provider);
        event.addListener(provider::invalidate);
    }

    // ---- Clone capability on respawn ----

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        original.getCapability(VillageRelationCapability.VILLAGE_RELATION).ifPresent(origCap -> {
            clone.getCapability(VillageRelationCapability.VILLAGE_RELATION).ifPresent(cloneCap -> {
                cloneCap.deserializeNBT(origCap.serializeNBT());
            });
        });
    }

    // ---- Server tick: daily rewards + villager AI ----

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;

        if (tickCounter % DAILY_CHECK_INTERVAL == 0) {
            // Run daily reward checks for all online players
            for (ServerLevel level : event.getServer().getAllLevels()) {
                long day = level.getDayTime() / 24000L;
                for (ServerPlayer player : level.players()) {
                    checkDailyRewards(player, level, day);
                }
            }
        }

        if (tickCounter % VILLAGER_AI_INTERVAL == 0) {
            // Run villager activity/expansion logic
            for (ServerLevel level : event.getServer().getAllLevels()) {
                runVillagerExpansionAI(level);
            }
        }

        if (tickCounter > 100000) tickCounter = 0;
    }

    private void checkDailyRewards(ServerPlayer player, ServerLevel level, long currentDay) {
        player.getCapability(VillageRelationCapability.VILLAGE_RELATION).ifPresent(cap -> {
            VillageRelationData data = cap.getData();
            String villageKey = VillageHelper.getNearestVillageKey(player, level);
            if (villageKey == null) return;

            if (data.canClaimDailyReward(villageKey, currentDay)) {
                int emeralds = data.getDailyEmeralds(villageKey);
                if (emeralds > 0) {
                    data.claimDailyReward(villageKey, currentDay);
                    player.getInventory().add(new ItemStack(Items.EMERALD, emeralds));
                    player.sendSystemMessage(Component.literal(
                        "§2[Village Reward] You received your daily " + emeralds
                        + " emerald(s) from the village!"));
                }
            }

            // If player is Champion and has personal golem, try to spawn it nearby
            if (data.hasPersonalGolem(villageKey) && data.getRank(villageKey) >= 4) {
                ensurePersonalGolem(player, level, villageKey);
            }
        });
    }

    private void ensurePersonalGolem(ServerPlayer player, ServerLevel level, String villageKey) {
        // Check if a tagged golem already exists near the player
        AABB nearPlayer = new AABB(player.blockPosition().offset(-20, -5, -20),
            player.blockPosition().offset(20, 5, 20));
        List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, nearPlayer);

        boolean hasTagged = golems.stream().anyMatch(g -> {
            CompoundTag t = g.getPersistentData();
            return t.contains("PersonalGolem") && t.getString("PersonalGolem")
                .equals(player.getStringId());
        });

        if (!hasTagged) {
            // Spawn a personal golem
            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            if (golem == null) return;
            golem.setPos(player.getX() + 2, player.getY(), player.getZ());
            golem.getPersistentData().putString("PersonalGolem", player.getStringId());
            golem.setCustomName(Component.literal("§5" + player.getName().getString() + "'s Guardian"));
            golem.setCustomNameVisible(true);
            level.addFreshEntity(golem);
            player.sendSystemMessage(Component.literal(
                "§5Your personal Guardian Golem has arrived!"));
        }
    }

    /**
     * Periodically makes villagers do expansion-related activities:
     * - Farmers tend to their crops
     * - Masons look for build sites
     * - All villagers wander and gather near workstations
     */
    private void runVillagerExpansionAI(ServerLevel level) {
        Random rand = new Random();

        // Get all villagers
        AABB worldBox = new AABB(-5000, 0, -5000, 5000, 256, 5000);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, worldBox);

        for (Villager villager : villagers) {
            long lastAi = lastVillagerAiTick.getOrDefault(villager.getUUID(), 0L);
            long currentTick = level.getGameTime();
            if (currentTick - lastAi < 400) continue; // 20 second cooldown per villager

            lastVillagerAiTick.put(villager.getUUID(), currentTick);
            VillagerProfession prof = villager.getVillagerData().getProfession();

            // Farmers: plant crops if nearby farmland is empty
            if (prof == VillagerProfession.FARMER) {
                doFarmerActivity(villager, level, rand);
            }
            // Masons: place cobblestone paths occasionally
            else if (prof == VillagerProfession.MASON) {
                doMasonActivity(villager, level, rand);
            }
            // Any villager: random chitchat floating text above head (just a name tag pulse)
            // (Real floating text requires client-side rendering; skipped here)
        }
    }

    private void doFarmerActivity(Villager farmer, ServerLevel level, Random rand) {
        // Look for dirt/farmland in a 5-block radius and interact
        net.minecraft.core.BlockPos pos = farmer.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                net.minecraft.core.BlockPos check = pos.offset(dx, 0, dz);
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(check);
                // If dirt, convert to farmland
                if (state.is(net.minecraft.world.level.block.Blocks.DIRT) && rand.nextFloat() < 0.1f) {
                    level.setBlock(check, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 3);
                    return;
                }
                // If farmland with no crop, plant wheat
                if (state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                    net.minecraft.core.BlockPos above = check.above();
                    if (level.getBlockState(above).isAir() && rand.nextFloat() < 0.15f) {
                        level.setBlock(above, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(), 3);
                        return;
                    }
                }
                // Harvest fully grown wheat
                if (state.is(net.minecraft.world.level.block.Blocks.WHEAT)) {
                    int age = state.getValue(net.minecraft.world.level.block.CropBlock.AGE);
                    if (age == 7 && rand.nextFloat() < 0.2f) {
                        level.destroyBlock(check, true, farmer);
                        return;
                    }
                }
            }
        }
    }

    private void doMasonActivity(Villager mason, ServerLevel level, Random rand) {
        // Occasionally place a stone brick on rough terrain
        net.minecraft.core.BlockPos pos = mason.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                net.minecraft.core.BlockPos check = pos.offset(dx, -1, dz);
                if (level.getBlockState(check).is(net.minecraft.world.level.block.Blocks.DIRT)
                    && rand.nextFloat() < 0.05f) {
                    level.setBlock(check, net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    return;
                }
            }
        }
    }
}
