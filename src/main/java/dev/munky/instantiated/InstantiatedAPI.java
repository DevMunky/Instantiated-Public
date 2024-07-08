package dev.munky.instantiated;

import dev.munky.instantiated.oldDungeon.DungeonManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Please let me know what you guys want added, I will add stuff i just dont know what to add.
 */
public interface InstantiatedAPI {
      static InstantiatedAPI getAPI(){
            return Instantiated.instance();
      }
      static JavaPlugin getPlugin(){
            return Instantiated.instance();
      }
      DungeonManager getDungeonManager();
      Logger getLogger();
      PluginState getState();
      void loadData();
      void saveData();
      boolean debug();
      void reload(boolean save);
      boolean isMythicSupportEnabled();
}
