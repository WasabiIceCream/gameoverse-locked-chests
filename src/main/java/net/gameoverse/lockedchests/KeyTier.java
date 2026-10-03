package net.gameoverse.lockedchests;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The five key and lock tiers, weakest first. A key opens its own tier and every tier below it. */
public enum KeyTier implements StringRepresentable {
    BRONZE, SILVER, GOLD, DIAMOND, DIVINE;

    private final String name = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return name;
    }

    public Item key() {
        return LockedChests.KEYS.get(this);
    }

    public boolean opens(KeyTier lock) {
        return ordinal() >= lock.ordinal();
    }

    /** The tier of a key item, or null for anything else. */
    public static KeyTier of(ItemStack stack) {
        for (KeyTier tier : values()) {
            if (stack.is(tier.key())) return tier;
        }
        return null;
    }

    public static KeyTier byName(String name) {
        for (KeyTier tier : values()) {
            if (tier.name.equals(name)) return tier;
        }
        return null;
    }
}
