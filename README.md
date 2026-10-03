# Gameoverse Locked Chests

Fabric mod for Minecraft 26.1.2, both sides, MIT. Bosses drop keys; some structure loot chests generate locked and
hold better loot. The idea comes from Minecraft Infinite (an Infdev mod with no license); all code and art here are
original (`tools/draw.py` draws the textures).

## What it does

- **Keys**: Common, Rare, Epic, Legendary, Divine (names coloured like the chest art's tiers). A key opens its own tier
  and every tier below, and is used up. Dropped when a player kills:
  - an Apotheosis Invader or Elite: the key named like its rarity (Common and Uncommon: Common, Rare, Epic,
    Mythic: Legendary; user's call 2026-10-03, which also gave Common and Uncommon Invaders a key);
  - the Warden or an Elder Guardian: Legendary; the Wither or Ender Dragon (Dragonkind Evolved's dragons included):
    Divine.
- **Locked Chests**: each single, dry structure loot chest has a 6% chance to generate locked instead, keeping its
  loot table. Its tier comes from the Dynamic Difficulty area level there (structure bonus included): Common below
  15, Rare 15+, Epic 30+, Legendary 45+, Divine 60+ (only the End goes that high).
- **Per player**: unlocking rolls the chest's own loot table for that player (their own seed) plus the tier's bonus
  table (`data/gameoverse_locked_chests/loot_table/chests/<tier>.json`: Apotheosis affixed gear and a gem, better
  rarities and purities per tier, plus a Heart Crystal from `gameoverse:hearts/heart_crystal_item` at 25% Common, 45%
  Rare, 70% Epic, always in Legendary, 1-2 in Divine: the hearts mod's own 10% chest roll only hooks vanilla loot
  containers, so locking a chest would otherwise have removed its crystal chance). After that the player opens their
  copy without a key; everyone else still needs one.
- **Jade** shows the tier ("Epic Locked Chest", tier colour, matching icon): the picked item is named and tagged
  (`custom_model_data` string) with the tier, and `JadeTierName` tells Jade to use the picked item for this block.
- Unbreakable outside creative, blast-proof, doesn't hide the block below it (`noOcclusion`), model-shaped hitbox.
  Hoppers can't reach it (not a vanilla container).
- **Animated** with `gameoverse-locked-chests-art` (the purchased chest art, never published): a client block entity
  renderer draws the art pack's per-bone part models and plays its keyframes, `idle_mouve` looping (offset per chest)
  while nobody has it open, `opening` (`opening_rare` for Legendary and Divine) while someone does, `closing` when the
  last viewer leaves. Opens/closes reach clients as block event 1 (viewer count), like vanilla chests. Rotations use
  Blockbench's convention (X and Y negated, applied Z, Y, X); checked in game. Without the art pack the renderer draws
  nothing and the mod's own fallback cube shows (`tools/draw.py`).

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

## Tested (2026-10-03, all in game unless noted)

World generation at 100% in fresh far chunks (placement, pending NBT, tier settling to Diamond at area level 50), and
in game: textures, too-weak/no key message, unlocking with the right and a stronger key, reopening without a key,
survival breaking refused, a Rare Invader dropping a key, a guaranteed Heart Crystal from the top tier, the art pack's
models and keys, the idle/opening/rare/closing animations and their directions.

## Later

Exclusive items in locked chests (with assets from licensed resource packs), per the user's plan.
