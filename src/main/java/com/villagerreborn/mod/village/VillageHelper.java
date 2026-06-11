package com.villagerreborn.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Utility for identifying villages and managing expansion behavior.
 */
public class VillageHelper {

    public static final int VILLAGE_RADIUS = 64;
    public static final int EXPANSION_RELATION_COST = 200;

    /**
     * Returns a stable string key for the village a villager belongs to.
     * Uses the nearest bell block position as the village identifier.
     */
    public static String getVillageKey(Villager villager) {
        BlockPos home = villager.getRestingPlace();
        if (home == null) home = villager.blockPosition();
        // Snap to 32-block grid to group nearby villagers into same "village"
        int gx = Math.floorDiv(home.getX(), 32) * 32;
        int gz = Math.floorDiv(home.getZ(), 32) * 32;
        return gx + "," + gz;
    }

    /**
     * Gets the village key for the village nearest to a player.
     * Returns null if no villagers are within range.
     */
    public static String getNearestVillageKey(Player player, ServerLevel level) {
        AABB searchBox = new AABB(
            player.blockPosition().offset(-VILLAGE_RADIUS, -20, -VILLAGE_RADIUS),
            player.blockPosition().offset(VILLAGE_RADIUS,  20,  VILLAGE_RADIUS)
        );
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, searchBox);
        if (nearby.isEmpty()) return null;

        // Find the most common village key (majority village)
        Map<String, Integer> keyCounts = new HashMap<>();
        for (Villager v : nearby) {
            String key = getVillageKey(v);
            keyCounts.merge(key, 1, Integer::sum);
        }
        return keyCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Attempts to trigger village expansion by placing a new structure block near the village.
     * In a full implementation this would summon a mason villager to build.
     * Here we simply find a flat spot and place a simple house footprint.
     */
    public static boolean tryExpandVillage(ServerLevel level, String villageKey, int rank) {
        String[] parts = villageKey.split(",");
        int vx = Integer.parseInt(parts[0]);
        int vz = Integer.parseInt(parts[1]);

        // Look for a flat spot at the village edge
        Random rand = new Random();
        for (int attempt = 0; attempt < 10; attempt++) {
            int ox = rand.nextInt(40) - 20 + vx;
            int oz = rand.nextInt(40) - 20 + vz;
            int y = findSurface(level, ox, oz);
            if (y < 0) continue;

            if (rank >= 2) {
                placeSimpleHouse(level, new BlockPos(ox, y, oz));
                return true;
            } else {
                placeWoodPile(level, new BlockPos(ox, y, oz));
                return true;
            }
        }
        return false;
    }

    private static int findSurface(ServerLevel level, int x, int z) {
        for (int y = 80; y > 40; y--) {
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            BlockState at = level.getBlockState(new BlockPos(x, y, z));
            if (below.isSolidRender(level, new BlockPos(x, y - 1, z)) && at.isAir()) {
                return y;
            }
        }
        return -1;
    }

    private static void placeSimpleHouse(ServerLevel level, BlockPos origin) {
        // 5x4 oak plank box with a dirt floor and oak log pillars
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                // Walls
                if (dx == 0 || dx == 4 || dz == 0 || dz == 4) {
                    for (int dy = 0; dy <= 3; dy++) {
                        level.setBlock(origin.offset(dx, dy, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Roof
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                level.setBlock(origin.offset(dx, 4, dz), Blocks.OAK_SLAB.defaultBlockState(), 3);
            }
        }
        // Door gap
        level.setBlock(origin.offset(2, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 0, 0), Blocks.OAK_DOOR.defaultBlockState(), 3);
        // Window
        level.setBlock(origin.offset(1, 1, 4), Blocks.GLASS_PANE.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, 1, 4), Blocks.GLASS_PANE.defaultBlockState(), 3);
        // Bed
        level.setBlock(origin.offset(1, 0, 2), Blocks.RED_BED.defaultBlockState(), 3);
        // Crafting table
        level.setBlock(origin.offset(3, 0, 2), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        // Lantern outside
        level.setBlock(origin.offset(2, 0, -1), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private static void placeWoodPile(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 3; i++) {
            level.setBlock(origin.offset(i, 0, 0), Blocks.OAK_LOG.defaultBlockState(), 3);
        }
    }

    /**
     * Counts villagers in a village.
     */
    public static int countVillagers(ServerLevel level, String villageKey) {
        String[] parts = villageKey.split(",");
        int vx = Integer.parseInt(parts[0]);
        int vz = Integer.parseInt(parts[1]);
        AABB box = new AABB(vx - 64, 0, vz - 64, vx + 96, 256, vz + 96);
        return level.getEntitiesOfClass(Villager.class, box).size();
    }
}
