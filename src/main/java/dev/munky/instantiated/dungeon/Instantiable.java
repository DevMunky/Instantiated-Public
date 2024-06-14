package dev.munky.instantiated.dungeon;

import dev.munky.instantiated.util.ImmutableLocation;
import org.bukkit.Location;

public interface Instantiable<P> {
    P getMaster();
    ImmutableLocation getLocation();
    Location toRelative(Location location);
    boolean isInstanced();
}
