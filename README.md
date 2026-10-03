# Gameoverse Locked Chests

Fabric mod for Minecraft 26.1.2, both sides, MIT. Bosses drop keys; some structure loot chests generate locked and
hold better loot. The idea comes from Minecraft Infinite (an Infdev mod with no license); all code and art here are
original (`tools/draw.py` draws the textures).

## What it does

- **Keys**: Bronze, Silver, Golden, Diamond. A key opens its own tier and every tier below, and is used up.
  Dropped when a player kills:
  - an Apotheosis Invader: Rare Bronze, Epic Silver, Mythic Gold (Common and Uncommon drop none);
  - an Apotheosis Elite: Common to Rare Bronze, Epic and Mythic Silver;
  - the Wither, Warden, Elder Guardian or Ender Dragon (Dragonkind Evolved's dragons included): Diamond.
- **Locked Chests**: each single, dry structure loot chest has a 6% chance to generate locked instead, keeping its
  loot table. Its tier comes from the Dynamic Difficulty area level there (structure bonus included): Bronze below
  15, Silver 15+, Gold 30+, Diamond 45+.
- **Per player**: unlocking rolls the chest's own loot table for that player (their own seed) plus the tier's bonus
  table (`data/gameoverse_locked_chests/loot_table/chests/<tier>.json`: Apotheosis affixed gear and a gem, better
  rarities and purities per tier). After that the player opens their copy without a key; everyone else still needs one.
- Unbreakable outside creative and blast-proof. Hoppers can't reach it (not a vanilla container).

Everything above is in `config/gameoverse_locked_chests.json` (written with the defaults on first start):
`lockedChestChance`, `tierMinLevel`, `invaderKeys`, `eliteKeys`, `bossKeys`, and `logPlacements` (logs every chest
world generation places and the tier it settles on; for testing).

## How it works

- `ChestPlacer` runs after each chunk's structures and features (same `ChunkGenerator.applyBiomeDecoration` hook as
  `gameoverse-content-fixes`' Mimics; separate seeded roll). At that point the chunk's block entities are still NBT,
  so the chest's data is written as pending block-entity NBT (asking the level for the block entity builds a copy that
  isn't kept).
- The tier is **not** worked out during world generation: `LevelingAPI.getLevelAt` waits on the chunk being
  generated, which hung the server until the watchdog killed it (found in testing). The chest is placed Bronze with
  `TierPending`, and on `ServerChunkEvents.CHUNK_LOAD` it's queued and settled on the server thread (16 per tick).
- Key drops read Apotheosis's persistent entity data (`apoth.boss.rarity`, `apoth.miniboss.rarity`) by reflection,
  so it doesn't compile against Cardinal Components.

## Build

    JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh ./gradlew build

Compiles against the server's Dynamic Difficulty jar by path (`build.gradle.kts`).

## Tested (2026-10-03)

World generation at 100% in fresh far chunks (placement, pending NBT, tier settling to Diamond at area level 50), and
in game: tier textures, too-weak/no key message, unlocking with the right and a stronger key, reopening without a
key, survival breaking refused, a Rare Invader dropping a Bronze Key.

## Later

Exclusive items in locked chests (with assets from licensed resource packs), per the user's plan.
