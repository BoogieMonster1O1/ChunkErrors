package io.github.boogiemonster1o1.chunkerrors.mixin;

import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.math.noise.NoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

@Mixin(NoiseChunkGenerator.class)
public class NoiseChunkGeneratorMixin {
	@Unique
	private long signInvertedSeed;
	@Unique
	private ChunkRandom signInvertedRandom;
	@Unique
	private OctavePerlinNoiseSampler signInvertedLowerInterpolatedNoise;
	@Unique
	private OctavePerlinNoiseSampler signInvertedUpperInterpolatedNoise;
	@Unique
	private OctavePerlinNoiseSampler signInvertedInterpolationNoise;
	@Unique
	private NoiseSampler signInvertedSurfaceDepthNoise;
	@Unique
	private OctavePerlinNoiseSampler signInvertedDensityNoise;
	@Unique
	private SimplexNoiseSampler signInvertedIslandNoise;
	@Unique
	private boolean inverted = false;

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;", ordinal = 1), method = "<init>(Lnet/minecraft/world/biome/source/BiomeSource;Lnet/minecraft/world/biome/source/BiomeSource;JLjava/util/function/Supplier;)V")
	private void init(BiomeSource populationSource, BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings, CallbackInfo ci) {
		System.out.println("This line is printed by an example mod mixin!");
		this.signInvertedSeed = -seed;
		this.signInvertedRandom = new ChunkRandom(this.signInvertedSeed);
	}

	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/biome/source/BiomeSource;Lnet/minecraft/world/biome/source/BiomeSource;JLjava/util/function/Supplier;)V")
	private void afterInit(BiomeSource populationSource, BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings, CallbackInfo ci) {
		this.signInvertedLowerInterpolatedNoise = new OctavePerlinNoiseSampler(this.signInvertedRandom, IntStream.rangeClosed(-15, 0));
		this.signInvertedUpperInterpolatedNoise = new OctavePerlinNoiseSampler(this.signInvertedRandom, IntStream.rangeClosed(-15, 0));
		this.signInvertedInterpolationNoise = new OctavePerlinNoiseSampler(this.signInvertedRandom, IntStream.rangeClosed(-7, 0));
		this.signInvertedSurfaceDepthNoise = (settings.get().getGenerationShapeConfig().hasSimplexSurfaceNoise() ? new OctaveSimplexNoiseSampler(this.signInvertedRandom, IntStream.rangeClosed(-3, 0)) : new OctavePerlinNoiseSampler(this.signInvertedRandom, IntStream.rangeClosed(-3, 0)));
		this.signInvertedRandom.consume(2620);
		this.signInvertedDensityNoise = new OctavePerlinNoiseSampler(this.signInvertedRandom, IntStream.rangeClosed(-15, 0));
		if (settings.get().getGenerationShapeConfig().hasIslandNoiseOverride()) {
			ChunkRandom chunkRandom = new ChunkRandom(-seed);
			chunkRandom.consume(17292);
			this.signInvertedIslandNoise = new SimplexNoiseSampler(chunkRandom);
		} else {
			this.signInvertedIslandNoise = null;
		}
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/noise/OctavePerlinNoiseSampler;getOctave(I)Lnet/minecraft/util/math/noise/PerlinNoiseSampler;", ordinal = 0), method = "sampleNoise")
	private PerlinNoiseSampler modifyLowerInterpolated(OctavePerlinNoiseSampler octavePerlinNoiseSampler, int octave) {
		if (this.inverted) {
			return this.signInvertedLowerInterpolatedNoise.getOctave(octave);
		}
		return octavePerlinNoiseSampler.getOctave(octave);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/noise/OctavePerlinNoiseSampler;getOctave(I)Lnet/minecraft/util/math/noise/PerlinNoiseSampler;", ordinal = 1), method = "sampleNoise")
	private PerlinNoiseSampler modifyUpperInterpolated(OctavePerlinNoiseSampler octavePerlinNoiseSampler, int octave) {
		if (this.inverted) {
			return this.signInvertedUpperInterpolatedNoise.getOctave(octave);
		}
		return octavePerlinNoiseSampler.getOctave(octave);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/noise/OctavePerlinNoiseSampler;getOctave(I)Lnet/minecraft/util/math/noise/PerlinNoiseSampler;", ordinal = 2), method = "sampleNoise")
	private PerlinNoiseSampler modifyInterpolated(OctavePerlinNoiseSampler octavePerlinNoiseSampler, int octave) {
		if (this.inverted) {
			return this.signInvertedInterpolationNoise.getOctave(octave);
		}
		return octavePerlinNoiseSampler.getOctave(octave);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/noise/OctavePerlinNoiseSampler;sample(DDDDDZ)D"), method = "getRandomDensityAt")
	private double interceptGetRandomDensityAt(OctavePerlinNoiseSampler octavePerlinNoiseSampler, double x, double y, double z, double d, double e, boolean bl) {
		if (this.inverted) {
			return this.signInvertedDensityNoise.sample(x, y, z, d, e, bl);
		}
		return octavePerlinNoiseSampler.sample(x, y, z, d, e, bl);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/noise/NoiseSampler;sample(DDDD)D"), method = "buildSurface")
	private double interceptBuildSurface(NoiseSampler noiseSampler, double x, double y, double d, double e) {
		if (this.inverted) {
			return this.signInvertedSurfaceDepthNoise.sample(x, y, d, e);
		}
		return noiseSampler.sample(x, y, d, e);
	}

	@ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/source/TheEndBiomeSource;getNoiseAt(Lnet/minecraft/util/math/noise/SimplexNoiseSampler;II)F"), method = "sampleNoiseColumn([DII)V")
	private SimplexNoiseSampler modifySampleNoiseColumn(SimplexNoiseSampler original) {
		if (this.inverted) {
			return this.signInvertedIslandNoise;
		}
		return original;
	}

	@Inject(method = "populateNoise", at = @At("HEAD"))
	private void onPopulateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk, CallbackInfo ci) {
		this.inverted = !this.inverted;
	}
}
