package net.gameoverse.lockedchests.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.gameoverse.lockedchests.KeyTier;
import net.gameoverse.lockedchests.LockedChests;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Bone pivots and keyframes for one chest tier, read from {@code assets/gameoverse_locked_chests/chest_animations/
 * <tier>.json} (written by the art pack). No file: no art pack, the renderer draws nothing and the block model shows.
 */
final class ChestAnimation {
    record Bone(String name, String parent, float[] pivot) {
    }

    record Key(float time, float[] value, boolean smooth) {
    }

    record Clip(float length, boolean loop, Map<String, Map<String, List<Key>>> channels) {
    }

    final Map<String, Bone> bones = new LinkedHashMap<>();
    final Map<String, Clip> clips = new HashMap<>();

    private static final Map<KeyTier, ChestAnimation> CACHE = new HashMap<>();
    private static ResourceManager cachedFor;

    static ChestAnimation get(KeyTier tier) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        if (rm != cachedFor) {
            CACHE.clear();
            cachedFor = rm;
        }
        return CACHE.computeIfAbsent(tier, t -> load(rm, t));
    }

    private static ChestAnimation load(ResourceManager rm, KeyTier tier) {
        Identifier id = Identifier.fromNamespaceAndPath(LockedChests.MOD_ID, "chest_animations/" + tier.getSerializedName() + ".json");
        var resource = rm.getResource(id);
        if (resource.isEmpty()) return null;
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            ChestAnimation anim = new ChestAnimation();
            for (var e : root.getAsJsonObject("bones").entrySet()) {
                JsonObject b = e.getValue().getAsJsonObject();
                JsonElement parent = b.get("parent");
                anim.bones.put(e.getKey(), new Bone(e.getKey(), parent == null || parent.isJsonNull() ? null : parent.getAsString(),
                    floats(b.getAsJsonArray("pivot"))));
            }
            for (var e : root.getAsJsonObject("animations").entrySet()) {
                JsonObject a = e.getValue().getAsJsonObject();
                Map<String, Map<String, List<Key>>> channels = new HashMap<>();
                for (var bone : a.getAsJsonObject("bones").entrySet()) {
                    Map<String, List<Key>> byChannel = new HashMap<>();
                    for (var ch : bone.getValue().getAsJsonObject().entrySet()) {
                        List<Key> keys = new ArrayList<>();
                        for (JsonElement k : ch.getValue().getAsJsonArray()) {
                            JsonObject ko = k.getAsJsonObject();
                            keys.add(new Key(ko.get("t").getAsFloat(), floats(ko.getAsJsonArray("v")),
                                "catmullrom".equals(ko.get("interp").getAsString())));
                        }
                        byChannel.put(ch.getKey(), keys);
                    }
                    channels.put(bone.getKey(), byChannel);
                }
                anim.clips.put(e.getKey(), new Clip(a.get("length").getAsFloat(), "loop".equals(a.get("loop").getAsString()), channels));
            }
            return anim;
        } catch (Exception ex) {
            LockedChestsClient.LOG.warn("Can't read chest animation {}: {}", id, ex.toString());
            return null;
        }
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsFloat();
        return out;
    }

    /**
     * A channel's value at time {@code t}: linear or Catmull-Rom between keyframes (Blockbench's two smooth modes),
     * the first/last value outside them, {@code fallback} when the clip doesn't touch it.
     */
    static float[] sample(List<Key> keys, float t, float[] fallback) {
        if (keys == null || keys.isEmpty()) return fallback;
        if (t <= keys.get(0).time()) return keys.get(0).value();
        Key last = keys.get(keys.size() - 1);
        if (t >= last.time()) return last.value();
        int i = 0;
        while (keys.get(i + 1).time() < t) i++;
        Key a = keys.get(i), b = keys.get(i + 1);
        float span = b.time() - a.time();
        float f = span <= 0 ? 1 : (t - a.time()) / span;
        float[] out = new float[3];
        if (a.smooth() || b.smooth()) {
            float[] p0 = keys.get(Math.max(0, i - 1)).value(), p3 = keys.get(Math.min(keys.size() - 1, i + 2)).value();
            for (int c = 0; c < 3; c++) out[c] = catmull(p0[c], a.value()[c], b.value()[c], p3[c], f);
        } else {
            for (int c = 0; c < 3; c++) out[c] = a.value()[c] + (b.value()[c] - a.value()[c]) * f;
        }
        return out;
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5F * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}
