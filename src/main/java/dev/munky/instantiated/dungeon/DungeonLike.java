package dev.munky.instantiated.dungeon;

import dev.munky.instantiated.dungeon.room.RoomLike;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Map;

public interface DungeonLike<R extends RoomLike<?,?>> extends ParentLike{
    String getIdentifier();
    File getSchematic();
    Vector getSpawnVector();
    Map<String, R> getRooms();
}
