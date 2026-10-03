package net.gameoverse.lockedchests.client;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.gameoverse.lockedchests.KeyTier;
import net.gameoverse.lockedchests.LockedChestBlock;
import net.gameoverse.lockedchests.LockedChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a Locked Chest from the art pack's part models and plays its keyframes: {@code idle_mouve} (a loop) while
 * nobody has it open, {@code opening} ({@code opening_rare} for Legendary and Divine) when someone opens it, and
 * {@code closing} when the last viewer closes it. Rotations follow Blockbench's convention (X and Y negated, applied
 * Z, Y, X). Without the art pack there's no animation file and this draws nothing (the block model shows instead).
 */
public final class LockedChestRenderer implements BlockEntityRenderer<LockedChestBlockEntity, LockedChestRenderer.State> {
    private static final float[] ZERO = {0, 0, 0};
    private static final float[] ONE = {1, 1, 1};

    public static final class State extends BlockEntityRenderState {
        KeyTier tier;
        Direction facing;
        String clip;
        float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LockedChestBlockEntity chest, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTick, camera, crumbling);
        state.tier = chest.getBlockState().getValue(LockedChestBlock.TIER);
        state.facing = chest.getBlockState().getValue(LockedChestBlock.FACING);
        long now = chest.getLevel() == null ? 0 : chest.getLevel().getGameTime();
        float sinceChange = (now - chest.openChangedTick + partialTick) / 20F;
        ChestAnimation anim = ChestAnimation.get(state.tier);
        if (chest.openCount > 0) {
            state.clip = state.tier.ordinal() >= KeyTier.LEGENDARY.ordinal() ? "opening_rare" : "opening";
            state.time = sinceChange;
        } else if (chest.openChangedTick != Long.MIN_VALUE && anim != null && anim.clips.containsKey("closing")
            && sinceChange < anim.clips.get("closing").length()) {
            state.clip = "closing";
            state.time = sinceChange;
        } else {
            state.clip = "idle_mouve";
            // Offset per chest so neighbours don't wiggle in step.
            long offset = Math.floorMod(chest.getBlockPos().asLong() * 31, 60);
            float length = anim != null && anim.clips.containsKey("idle_mouve") ? anim.clips.get("idle_mouve").length() : 3;
            state.time = ((now + offset + partialTick) / 20F) % Math.max(length, 0.05F);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        ChestAnimation anim = ChestAnimation.get(state.tier);
        if (anim == null) return;
        ChestAnimation.Clip clip = anim.clips.get(state.clip);
        float t = clip == null ? 0 : (clip.loop() ? state.time : Math.min(state.time, clip.length()));
        pose.pushPose();
        pose.translate(0.5F, 0, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-yRotation(state.facing)));
        pose.translate(-0.5F, 0, -0.5F);
        for (ChestAnimation.Bone bone : anim.bones.values()) {
            if (bone.parent() == null) renderBone(anim, bone, clip, t, state, pose, collector);
        }
        pose.popPose();
    }

    private void renderBone(ChestAnimation anim, ChestAnimation.Bone bone, @Nullable ChestAnimation.Clip clip, float t,
                            State state, PoseStack pose, SubmitNodeCollector collector) {
        var channels = clip == null ? null : clip.channels().get(bone.name());
        float[] pos = channels == null ? ZERO : ChestAnimation.sample(channels.get("position"), t, ZERO);
        float[] rot = channels == null ? ZERO : ChestAnimation.sample(channels.get("rotation"), t, ZERO);
        float[] scale = channels == null ? ONE : ChestAnimation.sample(channels.get("scale"), t, ONE);
        if (Math.abs(scale[0]) < 1e-4 && Math.abs(scale[1]) < 1e-4 && Math.abs(scale[2]) < 1e-4) return;
        float[] pivot = bone.pivot();
        pose.pushPose();
        pose.translate(pos[0] / 16F, pos[1] / 16F, pos[2] / 16F);
        pose.translate(pivot[0] / 16F, pivot[1] / 16F, pivot[2] / 16F);
        pose.mulPose(Axis.ZP.rotationDegrees(rot[2]));
        pose.mulPose(Axis.YP.rotationDegrees(-rot[1]));
        pose.mulPose(Axis.XP.rotationDegrees(-rot[0]));
        pose.scale(scale[0], scale[1], scale[2]);
        pose.translate(-pivot[0] / 16F, -pivot[1] / 16F, -pivot[2] / 16F);
        BlockStateModel model = ((FabricModelManager) Minecraft.getInstance().getModelManager())
            .getModel(LockedChestsClient.PARTS.get(state.tier).get(bone.name()));
        if (model != null) {
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(42L), parts);
            if (!parts.isEmpty()) {
                collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), parts, BlockModelRenderState.EMPTY_TINTS,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
        }
        for (ChestAnimation.Bone child : anim.bones.values()) {
            if (bone.name().equals(child.parent())) renderBone(anim, child, clip, t, state, pose, collector);
        }
        pose.popPose();
    }

    /** The blockstate file's y rotation for each facing (north is the models' front). */
    private static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
