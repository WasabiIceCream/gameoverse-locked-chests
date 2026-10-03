package net.gameoverse.lockedchests.mixin;

import net.gameoverse.lockedchests.ChestPlacer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs {@link ChestPlacer} once a chunk's structures and features are in place. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorLockedChestMixin {
    @Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
    private void gameoverse$lockedChests(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        ChestPlacer.replaceChests(level, chunk);
    }
}
