package net.gameoverse.lockedchests;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * there, set once the chunk has loaded ({@link #register}). The roll is seeded by world seed and position (and differs from the Mimic roll in
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
            level.setBlock(pos, LockedChests.LOCKED_CHEST.defaultBlockState()
                .setValue(LockedChestBlock.FACING, state.getValue(ChestBlock.FACING)), 2);
            // The block entity is still NBT at this stage (asking the level for it builds a copy that isn't kept),
            // so write the data where the finished chunk will load it from.
            chunk.removeBlockEntity(pos);
            CompoundTag tag = new CompoundTag();
            tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(LockedChests.LOCKED_CHEST_ENTITY).toString());
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.putString("LootTable", loot.identifier().toString());
            tag.putLong("LootTableSeed", random.nextLong());
            tag.putBoolean("TierPending", true);
            chunk.setBlockEntityNbt(tag);
            if (Config.INSTANCE.logPlacements) {
                LOG.info("Locked chest placed at {}, loot {}", pos.toShortString(), loot.identifier());
            }
        }
    }

    private static final Queue<Pending> PENDING = new ArrayDeque<>();

    private record Pending(ServerLevel level, BlockPos pos) {
    }

    /**
     * The tier needs the Dynamic Difficulty area level, and asking for it during world generation waits on the very
     * chunk being generated (the server hung on it). So world generation places a Bronze chest marked tier-pending,
     * and the tier is set here on the server thread once its chunk has fully loaded.
     */
    static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newChunk) -> {
            // Block entities from world generation may still be pending NBT here, so go by block state;
            // settleTiers creates the block entity and checks the pending flag.
            for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                if (chunk.getBlockState(pos).is(LockedChests.LOCKED_CHEST)) {
                    PENDING.add(new Pending(level, pos.immutable()));
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> settleTiers());
    }

    private static void settleTiers() {
        for (int i = 0; i < 16 && !PENDING.isEmpty(); i++) {
            Pending p = PENDING.poll();
            if (!p.level().isLoaded(p.pos())) continue;
            if (!(p.level().getBlockEntity(p.pos()) instanceof LockedChestBlockEntity chest) || !chest.isTierPending()) continue;
            int areaLevel = areaLevel(p.level(), p.pos());
            KeyTier tier = Config.INSTANCE.tierForLevel(areaLevel);
            BlockState state = p.level().getBlockState(p.pos());
            p.level().setBlock(p.pos(), state.setValue(LockedChestBlock.TIER, tier), 2);
            chest.setTierPending(false);
            if (Config.INSTANCE.logPlacements) {
                LOG.info("Locked chest ({}, area level {}) at {} in {}", tier.getSerializedName(), areaLevel,
                    p.pos().toShortString(), p.level().dimension().identifier());
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
