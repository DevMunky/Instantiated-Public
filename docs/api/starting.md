# The Instantiated API
> As with any API, the maintainers of Instantiated hope that the API is as logical and accessible as possible. If you feel there is a way to improve the API or functionality of Instantiated, please reach out in our [discord](https://discord.gg/XggaTq7kjR).

The API begins with the entry point, `InstantiatedAPI`. You can access the it with this call:
```java
InstantiatedAPI.getAPI()
```

The members of `InstantiatedAPI` look like this:
```java
public interface InstantiatedAPI {
    DungeonManager getDungeonManager();
    Logger getLogger();
    PluginState getState();
    void loadData();
    void saveData();
    boolean debug();
    void reload(boolean save);
    boolean isMythicSupportEnabled();
}
```
> Please let us know if you want something exposed in this class!

The `DungeonManager` does the bulk of the instancing work. It handles starting new instances, caching instances for later use, and removing instances. It also handles the `Dungeon World`, which is almost an instanced world dedicated to holding dungeons inside.
> Currently, it is only possible to have one dedicated dungeon world. In the future, this may change to support different server configurations.

The dungeon world is deleted and recreated upon server stop and start respectively, so **any changes made in this world will be lost**. Make sure to use a dungeon to create instances, and not manually building something!

The metadata keys that Instantiated also occasionally uses are exposed within the DungeonManager interface as well, if you were curious.

Here is the `DungeonManager` interface:
```java
public interface DungeonManager {
    boolean startDungeon(String identifier, boolean force, Player... players);
    void cacheDungeon(String identifier);
    void createDungeonWorld();
    Optional<InstancedDungeon> getCurrentDungeon(UUID player);
    void cleanup();
    World getDungeonWorld();
    Map<IdentifiableKey, DungeonFormat> getDungeons();
    Stream<InstancedDungeon> getInstancedDungeons();
}
```

Instantiated gives server owners the option to create a number of cached instances at the start of the server, those of which will persist. In other words, a cached instance will cache another instance following its scheduled removal for any reason other than the plugin disabling.

> "cache another instance" here just means that certain things are reset to prepare for another group to start the dungeon

