package net.gameoverse.lockedchests;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Locked Chests, after Minecraft Infinite's (idea only, all code and art our own). Bosses drop Bronze, Silver, Gold,
 * Diamond and Divine keys ({@link KeyDrops}); a share of structure loot chests generate locked ({@link ChestPlacer}), holding
 * their own loot plus a better bonus roll, unlocked per player ({@link LockedChestBlockEntity}).
 */
public final class LockedChests implements ModInitializer {
    public static final String MOD_ID = "gameoverse_locked_chests";
    public static final Map<KeyTier, Item> KEYS = new EnumMap<>(KeyTier.class);
    public static Block LOCKED_CHEST;
    public static BlockEntityType<LockedChestBlockEntity> LOCKED_CHEST_ENTITY;

    @Override
    public void onInitialize() {
        Config.load();
        Rarity[] rarities = {Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.EPIC, Rarity.EPIC};
        for (KeyTier tier : KeyTier.values()) {
            String name = tier.getSerializedName() + "_key";
            Rarity rarity = rarities[tier.ordinal()];
            KEYS.put(tier, item(name, Item::new, new Item.Properties().stacksTo(16).rarity(rarity)));
        }
        LOCKED_CHEST = block("locked_chest", LockedChestBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(-1.0F, 3600000.0F).sound(SoundType.WOOD)
            .pushReaction(PushReaction.BLOCK).noLootTable().noOcclusion());
        Item chestItem = item("locked_chest", p -> new BlockItem(LOCKED_CHEST, p), new Item.Properties().useBlockDescriptionPrefix());
        LOCKED_CHEST_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("locked_chest"),
            FabricBlockEntityTypeBuilder.create(LockedChestBlockEntity::new, LOCKED_CHEST).build());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            for (KeyTier tier : KeyTier.values()) output.accept(KEYS.get(tier));
            output.accept(chestItem);
        });
        KeyDrops.register();
        ChestPlacer.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        properties.setId(ResourceKey.create(Registries.ITEM, id(name)));
        return Registry.register(BuiltInRegistries.ITEM, id(name), factory.apply(properties));
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        properties.setId(ResourceKey.create(Registries.BLOCK, id(name)));
        return Registry.register(BuiltInRegistries.BLOCK, id(name), factory.apply(properties));
    }
}
