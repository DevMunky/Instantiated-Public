package dev.munky.instantiated.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DungeonStartCacheEvent extends Event implements Cancellable {
      public static HandlerList handlers = new HandlerList();
      @Override
      public @NotNull HandlerList getHandlers() {
            return handlers;
      }
      public static HandlerList getHandlerList(){
            return handlers;
      }
      private boolean cancelled;
      public UUID mag;
      public DungeonStartCacheEvent(UUID cache) {
            super(!Bukkit.isPrimaryThread());
            this.mag = cache;
      }
      @Override
      public boolean isCancelled() {
            return this.cancelled;
      }
      @Override
      public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
      }
}
