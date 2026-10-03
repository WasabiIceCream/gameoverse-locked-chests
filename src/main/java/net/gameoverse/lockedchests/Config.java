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
    /** Lowest area level (Dynamic Difficulty, structure bonus included) for each lock tier above Common. */
    public Map<String, Integer> tierMinLevel = ordered("rare", 15, "epic", 30, "legendary", 45, "divine", 60);
    /** Key dropped by an Invader of each Apotheosis rarity (same name, Mythic gives Legendary; left out: none). */
    public Map<String, String> invaderKeys = ordered(
        "apotheosis:common", "common", "apotheosis:uncommon", "common", "apotheosis:rare", "rare",
        "apotheosis:epic", "epic", "apotheosis:mythic", "legendary");
    /** Key dropped by an Elite of each Apotheosis rarity. */
    public Map<String, String> eliteKeys = ordered(
        "apotheosis:common", "common", "apotheosis:uncommon", "common", "apotheosis:rare", "rare",
        "apotheosis:epic", "epic", "apotheosis:mythic", "legendary");
    /** Key dropped by these entity types (Dragonkind Evolved's dragons are the vanilla Ender Dragon). */
    public Map<String, String> bossKeys = ordered(
        "minecraft:wither", "divine", "minecraft:warden", "legendary", "minecraft:elder_guardian", "legendary",
        "minecraft:ender_dragon", "divine");

    public static Config INSTANCE = new Config();

    /** A map that keeps the order the defaults are written in (so the config file reads naturally). */
    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> ordered(Object... keysAndValues) {
        Map<String, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) map.put((String) keysAndValues[i], (V) keysAndValues[i + 1]);
        return map;
    }

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
        KeyTier best = KeyTier.COMMON;
        for (KeyTier tier : KeyTier.values()) {
            Integer min = tierMinLevel.get(tier.getSerializedName());
            if (min != null && level >= min) best = tier;
        }
        return best;
    }
}
