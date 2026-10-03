package net.gameoverse.lockedchests;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

/**
 * Jade names a block after its own translation unless told to use its picked item. The Locked Chest's picked item
 * carries the tier ("Epic Locked Chest", see {@link LockedChestBlock#getCloneItemStack}), so pick it.
 */
public class JadeTierName implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.blockOperations().pick(ResourceKey.create(Registries.BLOCK, LockedChests.id("locked_chest")));
    }
}
