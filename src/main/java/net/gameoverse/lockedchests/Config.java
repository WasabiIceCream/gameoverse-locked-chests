package net.gameoverse.lockedchests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

/**
 * {@code config/gameoverse_locked_chests.json}, written with the defaults on first start. Missing fields keep their
 * defaults.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Chance for each single structure loot chest to generate locked. */
    public double lockedChestChance = 0.06;
    /** Log every Locked Chest world generation places (for testing). */
    public boolean logPlacements = false;
    /** Lowest area level (Dynamic Difficulty, structure bonus included) for each lock tier above Bronze. */
    public Map<String, Integer> tierMinLevel = new LinkedHashMap<>(Map.of("silver", 15, "gold", 30, "diamond", 45));
    /** Key dropped by an Invader of each Apotheosis rarity (rarities left out drop nothing). */
    public Map<String, String> invaderKeys = new LinkedHashMap<>(Map.of(
        "apotheosis:rare", "bronze", "apotheosis:epic", "silver", "apotheosis:mythic", "gold"));
    /** Key dropped by an Elite of each Apotheosis rarity. */
    public Map<String, String> eliteKeys = new LinkedHashMap<>(Map.of(
        "apotheosis:common", "bronze", "apotheosis:uncommon", "bronze", "apotheosis:rare", "bronze",
        "apotheosis:epic", "silver", "apotheosis:mythic", "silver"));
    /** Key dropped by these entity types (Dragonkind Evolved's dragons are the vanilla Ender Dragon). */
    public Map<String, String> bossKeys = new LinkedHashMap<>(Map.of(
        "minecraft:wither", "diamond", "minecraft:warden", "diamond", "minecraft:elder_guardian", "diamond",
        "minecraft:ender_dragon", "diamond"));

    public static Config INSTANCE = new Config();

    static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("gameoverse_locked_chests.json");
        try {
            if (Files.exists(path)) {
                Config read = GSON.fromJson(Files.readString(path), Config.class);
                if (read != null) INSTANCE = read;
            }
            Files.writeString(path, GSON.toJson(INSTANCE));
        } catch (IOException | RuntimeException e) {
            INSTANCE = new Config();
        }
    }

    KeyTier tierForLevel(int level) {
        KeyTier best = KeyTier.BRONZE;
        for (KeyTier tier : KeyTier.values()) {
            Integer min = tierMinLevel.get(tier.getSerializedName());
            if (min != null && level >= min) best = tier;
        }
        return best;
    }
}
