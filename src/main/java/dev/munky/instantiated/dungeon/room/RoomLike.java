package dev.munky.instantiated.dungeon.room;

import com.sk89q.worldedit.regions.AbstractRegion;
import dev.munky.instantiated.dungeon.ChildLike;
import dev.munky.instantiated.dungeon.ParentLike;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;

import java.util.Map;

public interface RoomLike<P extends ParentLike,R extends AbstractRegion> extends RegionHolder<R>, ChildLike<P> {
    Map<String, DungeonMob> getEntities();
    String getIdentifier();
}
