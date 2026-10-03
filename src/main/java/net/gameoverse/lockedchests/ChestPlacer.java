package net.gameoverse.lockedchests;

import java.util.ArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * After a chunk's structures and features are placed, each single loot chest has {@link Config#lockedChestChance}
 * to become a Locked Chest holding the same loot table. The lock's tier comes from the Dynamic Difficulty area level
 * there. The roll is seeded by world seed and position (and differs from the Mimic roll in
 * {@code gameoverse-content-fixes}, which uses the same hook), so it doesn't depend on chunk generation order.
 */
public final class ChestPlacer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LockedChests.MOD_ID);
    private static final boolean DYNAMIC_DIFFICULTY = FabricLoader.getInstance().isModLoaded("dynamic_difficulty");

    private ChestPlacer() {
    }

    public static void replaceChests(WorldGenLevel level, ChunkAccess chunk) {
        double chance = Config.INSTANCE.lockedChestChance;
        if (chance <= 0) return;
        for (BlockPos pos : new ArrayList<>(chunk.getBlockEntitiesPos())) {
            BlockState state = chunk.getBlockState(pos);
            if (!state.is(Blocks.CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                || state.getValue(ChestBlock.WATERLOGGED)) {
                continue;
            }
            ResourceKey<LootTable> loot = lootTable(chunk, pos);
            if (loot == null) continue;
            RandomSource random = RandomSource.create(level.getSeed() ^ pos.asLong() * 0xC2B2AE3D27D4EB4FL);
            if (random.nextDouble() >= chance) continue;
            KeyTier tier = Config.INSTANCE.tierForLevel(areaLevel(level.getLevel(), pos));
            level.setBlock(pos, LockedChests.LOCKED_CHEST.defaultBlockState()
                .setValue(LockedChestBlock.FACING, state.getValue(ChestBlock.FACING))
                .setValue(LockedChestBlock.TIER, tier), 2);
            if (level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest) {
                chest.setLoot(loot, random.nextLong());
            }
            if (Config.INSTANCE.logPlacements) {
                LOG.info("Locked chest ({}) at {} in {}, loot {}", tier.getSerializedName(), pos.toShortString(),
                    level.getLevel().dimension().identifier(), loot.identifier());
            }
        }
    }

    private static int areaLevel(ServerLevel level, BlockPos pos) {
        if (!DYNAMIC_DIFFICULTY) return 1;
        try {
            return DynamicDifficulty.levelAt(level, pos);
        } catch (RuntimeException | LinkageError e) {
            return 1;
        }
    }

    private static ResourceKey<LootTable> lootTable(ChunkAccess chunk, BlockPos pos) {
        BlockEntity be = chunk.getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity container) return container.getLootTable();
        CompoundTag pending = chunk.getBlockEntityNbt(pos);
        if (pending == null) return null;
        return pending.getString("LootTable")
            .map(id -> ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)))
            .orElse(null);
    }

    /** Separate class so Dynamic Difficulty's classes are only loaded when it's installed. */
    private static final class DynamicDifficulty {
        static int levelAt(ServerLevel level, BlockPos pos) {
            return dev.muon.dynamic_difficulty.api.LevelingAPI.getLevelAt(level, pos);
        }
    }
}
