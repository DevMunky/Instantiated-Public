package dev.munky.instantiated.dungeon.room;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;

public interface RegionHolder<R extends AbstractRegion> {
    void setRegion(R newRegion);
    R getRegion();
    BlockVector3 getOrigin();
}
