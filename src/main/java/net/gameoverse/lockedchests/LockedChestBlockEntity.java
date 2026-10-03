package net.gameoverse.lockedchests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Holds the loot table of the chest it replaced and one 27-slot inventory per player who has unlocked it. Unlocking
 * rolls that table for the player (their own seed, so two players get different loot), plus the tier's bonus table
 * ({@code gameoverse_locked_chests:chests/<tier>}). Players who haven't unlocked it still need a key.
 */
public class LockedChestBlockEntity extends BlockEntity {
    public static final int SIZE = 27;
    private ResourceKey<LootTable> lootTable;
    private long seed;
    private final Map<UUID, NonNullList<ItemStack>> contents = new HashMap<>();

    public LockedChestBlockEntity(BlockPos pos, BlockState state) {
        super(LockedChests.LOCKED_CHEST_ENTITY, pos, state);
    }

    public void setLoot(ResourceKey<LootTable> lootTable, long seed) {
        this.lootTable = lootTable;
        this.seed = seed;
        setChanged();
    }

    public boolean isUnlockedFor(Player player) {
        return contents.containsKey(player.getUUID());
    }

    public void unlock(ServerPlayer player) {
        ServerLevel level = player.level();
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        Container container = new ListContainer(items, this);
        long playerSeed = seed ^ player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits() * 31;
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withLuck(player.getLuck())
            .create(LootContextParamSets.CHEST);
        if (lootTable != null) {
            level.getServer().reloadableRegistries().getLootTable(lootTable).fill(container, params, playerSeed);
        }
        KeyTier tier = getBlockState().getValue(LockedChestBlock.TIER);
        ResourceKey<LootTable> bonus = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(LockedChests.MOD_ID, "chests/" + tier.getSerializedName()));
        List<ItemStack> extra = level.getServer().reloadableRegistries().getLootTable(bonus).getRandomItems(params, playerSeed + 1);
        RandomSource random = RandomSource.create(playerSeed + 2);
        for (ItemStack stack : extra) {
            List<Integer> empty = new ArrayList<>();
            for (int i = 0; i < SIZE; i++) if (items.get(i).isEmpty()) empty.add(i);
            if (empty.isEmpty()) break;
            items.set(empty.get(random.nextInt(empty.size())), stack);
        }
        contents.put(player.getUUID(), items);
        setChanged();
    }

    public void open(ServerPlayer player) {
        NonNullList<ItemStack> items = contents.get(player.getUUID());
        if (items == null) return;
        Container container = new ListContainer(items, this);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, container),
            Component.translatable("container.gameoverse_locked_chests.locked_chest")));
        player.level().playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
            player.level().getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lootTable = input.read("LootTable", ResourceKey.codec(Registries.LOOT_TABLE)).orElse(null);
        seed = input.getLongOr("LootTableSeed", 0L);
        contents.clear();
        for (ValueInput child : input.childrenListOrEmpty("Players")) {
            UUID id;
            try {
                id = UUID.fromString(child.getStringOr("Id", ""));
            } catch (IllegalArgumentException e) {
                continue;
            }
            NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(child, items);
            contents.put(id, items);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("LootTable", ResourceKey.codec(Registries.LOOT_TABLE), lootTable);
        output.putLong("LootTableSeed", seed);
        ValueOutput.ValueOutputList players = output.childrenList("Players");
        contents.forEach((id, items) -> {
            ValueOutput child = players.addChild();
            child.putString("Id", id.toString());
            ContainerHelper.saveAllItems(child, items, true);
        });
    }

    /** One player's inventory in this chest, written straight into the block entity's list. */
    private record ListContainer(NonNullList<ItemStack> items, LockedChestBlockEntity owner) implements Container {
        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
            if (!stack.isEmpty()) setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            stack.limitSize(getMaxStackSize(stack));
            setChanged();
        }

        @Override
        public void setChanged() {
            owner.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return !owner.isRemoved() && Container.stillValidBlockEntity(owner, player);
        }

        @Override
        public void clearContent() {
            items.clear();
        }
    }
}
