package net.gameoverse.lockedchests;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A key drops when a player kills an Apotheosis Invader or Elite (tier by its rarity) or one of the configured bosses.
 * Rarities live in Apotheosis's persistent entity data ({@code apoth.boss.rarity}, {@code apoth.miniboss.rarity}).
 */
final class KeyDrops {
    private static final boolean APOTHEOSIS = FabricLoader.getInstance().isModLoaded("apotheosis");

    private KeyDrops() {
    }

    static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level) || !(source.getEntity() instanceof Player)) return;
            KeyTier tier = keyFor(entity);
            if (tier != null) entity.spawnAtLocation(level, new ItemStack(tier.key()));
        });
    }

    private static KeyTier keyFor(LivingEntity entity) {
        Config config = Config.INSTANCE;
        KeyTier boss = KeyTier.byName(config.bossKeys.getOrDefault(
            BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), ""));
        if (boss != null) return boss;
        if (!APOTHEOSIS) return null;
        CompoundTag data = ApotheosisData.get(entity);
        if (data.contains("apoth.boss")) {
            return KeyTier.byName(config.invaderKeys.getOrDefault(data.getStringOr("apoth.boss.rarity", ""), ""));
        }
        if (data.contains("apoth.miniboss")) {
            return KeyTier.byName(config.eliteKeys.getOrDefault(data.getStringOr("apoth.miniboss.rarity", ""), ""));
        }
        return null;
    }

    /**
     * {@code PersistentDataComponent.get(Entity)}, looked up by reflection: the class is a Cardinal Components
     * component, and compiling against it would need Cardinal Components' nested jars too.
     */
    private static final class ApotheosisData {
        private static java.lang.reflect.Method get;
        private static boolean failed;

        static CompoundTag get(LivingEntity entity) {
            if (failed) return new CompoundTag();
            try {
                if (get == null) {
                    get = Class.forName("dev.shadowsoffire.apotheosis.util.PersistentDataComponent")
                        .getMethod("get", net.minecraft.world.entity.Entity.class);
                }
                return (CompoundTag) get.invoke(null, entity);
            } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
                failed = true;
                LOG.warn("Can't read Apotheosis entity data (Apotheosis changed?), Invaders and Elites drop no keys: {}", e.toString());
                return new CompoundTag();
            }
        }
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LockedChests.MOD_ID);
}
