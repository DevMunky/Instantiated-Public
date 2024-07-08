package dev.munky.instantiated.event;

import com.google.gson.JsonObject;
import dev.munky.instantiated.dungeon.DungeonFormat;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DungeonLoadEvent extends DungeonEvent{
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    private final JsonObject dungeonJson;
    public DungeonLoadEvent(DungeonFormat dungeon, JsonObject json) {
        super(dungeon, !Bukkit.isPrimaryThread());
        this.dungeonJson = json;
    }
    public JsonObject getDungeonJson(){
        return this.dungeonJson;
    }
}
