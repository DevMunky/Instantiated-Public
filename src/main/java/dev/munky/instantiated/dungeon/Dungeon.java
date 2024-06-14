package dev.munky.instantiated.dungeon;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.dungeon.room.DungeonRoom;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Dungeon implements DungeonLike<DungeonRoom> {
    private final File schematic;
    private final String identifier;
    private final Vector spawnLocation;
    private final Map<String, DungeonRoom> rooms = new HashMap<>();
    public Dungeon(String identifier, File schematic, Vector spawnLocation){
        this.identifier =  identifier;
        this.schematic = schematic;
        this.spawnLocation = spawnLocation;
    }
    public String toString(){
        return "Dungeon["+identifier+"]@"+hashCode();
    }
    @Override
    public Map<String, DungeonRoom> getRooms() {
        return this.rooms;
    }
    @Override
    public String getIdentifier() {
        return this.identifier;
    }
    @Override
    public File getSchematic() {
        return this.schematic;
    }
    @Override
    public Vector getSpawnVector(){
        return this.spawnLocation;
    }
    public static StringArgument getDungeonArgument(String identifier){
        StringArgument arg = new StringArgument(identifier);
        arg.replaceSuggestions(ArgumentSuggestions.stringsAsync(
                info-> CompletableFuture.supplyAsync(()->
                        Instantiated.getDungeonManager().getDungeons().values().stream().map(i->i.identifier).toArray(String[]::new)
                )
        ));
        return arg;
    }
}
