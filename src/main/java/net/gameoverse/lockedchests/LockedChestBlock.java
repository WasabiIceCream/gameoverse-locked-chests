package net.gameoverse.lockedchests;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A chest that needs a key of its tier or better. Unbreakable outside creative (hardness -1) and blast-proof, like
 * the loot containers {@code gameoverse-content-fixes} protects. Each player unlocks their own copy, see
 * {@link LockedChestBlockEntity}.
 */
public class LockedChestBlock extends BaseEntityBlock {
    public static final MapCodec<LockedChestBlock> CODEC = simpleCodec(LockedChestBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<KeyTier> TIER = EnumProperty.create("tier", KeyTier.class);

    public LockedChestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, KeyTier.COMMON));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Matches the chest models (one block wide, two thirds tall, inset front and back). */
    private static final VoxelShape SHAPE_NS = Block.box(0, 0, 1.33, 16, 10.67, 14.67);
    private static final VoxelShape SHAPE_EW = Block.box(1.33, 0, 0, 14.67, 10.67, 16);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest)) return InteractionResult.PASS;
        if (chest.isUnlockedFor(player)) {
            if (player instanceof ServerPlayer sp) chest.open(sp);
            return InteractionResult.SUCCESS;
        }
        KeyTier key = KeyTier.of(stack);
        if (key == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        KeyTier lock = state.getValue(TIER);
        if (!key.opens(lock)) {
            if (!level.isClientSide()) tooWeak(player, lock, pos, level);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
            chest.unlock(sp);
            chest.open(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest)) return InteractionResult.PASS;
        if (chest.isUnlockedFor(player)) {
            if (player instanceof ServerPlayer sp) chest.open(sp);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) tooWeak(player, state.getValue(TIER), pos, level);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    private static void tooWeak(Player player, KeyTier lock, BlockPos pos, Level level) {
        level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.gameoverse_locked_chests.needs_key",
            Component.translatable(lock.key().getDescriptionId())));
    }
}
