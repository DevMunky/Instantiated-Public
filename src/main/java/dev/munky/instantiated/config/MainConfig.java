package dev.munky.instantiated.config;

import dev.munky.instantiated.Instantiated;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class MainConfig extends InstantiatedConfiguration {
    /**
     * {@code default false}
     */
    public final boolean DEBUG;
    /**
     * {@code Default LOCAL_JSON}
     */
    public final @NotNull String DATA_STORAGE_MODE;
    /**
     * {@code default "world"}
     */
    public final String DUNGEON_WORLD_NAME;
    /**
     * {@code default 5}
     */
    public final int PARTICLE_RENDER_REFRESH_RATE;
    /**
     * {@code default 20}
     */
    public final int PARTICLE_RENDER_LINE_RESOLUTION;
    /**
     * {@code default true}
     */
    public final boolean DROPPED_KEYS_USE_BUKKIT_GLOW;
    /**
     * {@code default "BLUE"}
     */
    public final NamedTextColor DROPPED_KEYS_GLOW_COLOR;
    public final int PHYSICAL_DUNGEON_GRID_SIZE;
    public final int DUNGEON_CACHE_SIZE;
    public MainConfig() {
        super(Instantiated.instance(), "config.yml");
        int grid_size;
        DEBUG = getYaml().getBoolean("debug.enabled", false);
        DUNGEON_WORLD_NAME = getYaml().getString("dungeon.world","world");
        DATA_STORAGE_MODE = getYaml().getString("dungeon.data-storage.mode","LOCAL_JSON?");
        PARTICLE_RENDER_LINE_RESOLUTION = getYaml().getInt("dungeon.edit-mode.line-resolution", 20);
        PARTICLE_RENDER_REFRESH_RATE = getYaml().getInt("dungeon.edit-mode.particle-refresh-rate", 5);
        DROPPED_KEYS_USE_BUKKIT_GLOW = getYaml().getBoolean("dungeon.keys.dropped-keys-glow.enabled", true);
        String sColor = getYaml().getString("dungeon.keys.dropped-keys-glow.color","BLUE");
        DROPPED_KEYS_GLOW_COLOR = NamedTextColor.NAMES.valueOr(sColor,NamedTextColor.BLUE);
        grid_size = getYaml().getInt("dungeon.grid-size");
        if (grid_size < 50){
            // grid_size = 100;
            Instantiated.logger().severe("Grid size for dungeons (%d) is too small or undefined!".formatted(grid_size));
        }
        PHYSICAL_DUNGEON_GRID_SIZE = grid_size;
        DUNGEON_CACHE_SIZE = getYaml().getInt("dungeon.cache-size-per-dungeon",1);
    }
}
