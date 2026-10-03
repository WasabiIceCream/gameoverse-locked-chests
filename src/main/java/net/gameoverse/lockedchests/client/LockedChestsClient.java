package net.gameoverse.lockedchests.client;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.gameoverse.lockedchests.KeyTier;
import net.gameoverse.lockedchests.LockedChests;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers the animated chest renderer and the per-bone part models it draws. */
public final class LockedChestsClient implements ClientModInitializer {
    static final Logger LOG = LoggerFactory.getLogger(LockedChests.MOD_ID);
    static final String[] BONES = {"bone", "top", "bone3", "bone2", "bone4", "octagon"};
    static final Map<KeyTier, Map<String, ExtraModelKey<BlockStateModel>>> PARTS = new EnumMap<>(KeyTier.class);

    @Override
    public void onInitializeClient() {
        for (KeyTier tier : KeyTier.values()) {
            Map<String, ExtraModelKey<BlockStateModel>> byBone = new HashMap<>();
            for (String bone : BONES) byBone.put(bone, ExtraModelKey.create(() -> tier.getSerializedName() + "/" + bone));
            PARTS.put(tier, byBone);
        }
        ModelLoadingPlugin.register(context -> PARTS.forEach((tier, byBone) -> byBone.forEach((bone, key) ->
            context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(Identifier.fromNamespaceAndPath(LockedChests.MOD_ID,
                "block/chest_parts/" + tier.getSerializedName() + "/" + bone))))));
        BlockEntityRenderers.register(LockedChests.LOCKED_CHEST_ENTITY, context -> new LockedChestRenderer());
    }
}
