package dev.munky.instantiated;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

public enum PluginState {
      UNDEFINED,
      LOADING,
      ENABLING,
      RELOADING,
      PROCESSING,
      DISABLING,
      DISABLED;
      static final Multimap<Pair<PluginState, Boolean>, Consumer<Instantiated>> TASKS = MultimapBuilder.hashKeys().arrayListValues().build();
      static Instantiated PLUGIN;
      
      public boolean isStartup() {
            return this == LOADING || this == ENABLING;
      }
      
      public boolean isDisabled() {
            return this == DISABLING || this == DISABLED;
      }
      
      public boolean isSafe() {
            return this == PROCESSING;
      }
      
      public void registerEvent(PluginState state, boolean async, Consumer<Instantiated> func) {
            if (state == DISABLED || state == UNDEFINED) {
                  throw new IllegalArgumentException("Cannot listen to an unobtainable state");
            }
            TASKS.put(Pair.of(state, async), func);
      }
      
      public boolean isState(PluginState state) {
            return Instantiated.getLoadState() == state;
      }
      
      PluginState runTasks(Instantiated plugin) {
            PLUGIN = plugin;
            if (this != LOADING && this != DISABLING) {
                  Bukkit.getServer().getAsyncScheduler().runNow(plugin, t -> {
                        TASKS.get(Pair.of(this, true)).forEach(c -> c.accept(plugin));
                  });
            }
            TASKS.get(Pair.of(this, false)).forEach(c -> c.accept(plugin));
            return this;
      }
}
