package dev.munky.instantiated.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Void Generator for the world that one-block islands will be in.
 */
public class VoidGenerator extends ChunkGenerator {
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {return List.of();}
    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {}
    @Override
    public void generateSurface(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {}
    @Override
    public void generateBedrock(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {}
    @Override
    public void generateCaves(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {}
    @Override
    @Nullable
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {return new VoidBiomeProvider();}
    @Override
    public boolean canSpawn(@NotNull World world, int x, int z) { return true; }
    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0, 100, 0);
    }
    private static class VoidBiomeProvider extends BiomeProvider {
        @Override
        public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {return Biome.THE_END;}
        @Override
        public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {return List.of(Biome.THE_END);}
        // TODO Add some values from here to config
    }
}
