package dev.munky.instantiated.dungeon;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.util.ImmutableLocation;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public interface DungeonManager {
    DungeonManager INSTANCE = new DungeonManagerImpl();
    int GRID_SIZE = 2000;
    int PERSISTENT_DUNGEON_INSTANCES = 4;
    String METADATA_DELETE_UPON_LOAD = "instantiated-delete-during-load";
    String METADATA_KILL_ON_SIGHT = "instantiated-kill-on-sight";
    @Deprecated
    String METADATA_OWNED_ENTITY = "instantiated-owned-entity";
    String METADATA_DUNGEON_MOB = "instantiated-dungeon-mob";
    String METADATA_PERSISTENT_ENTITY = "instantiated-persist";
    String METADATA_KEY_ENTITY = "instantiated-dungeon-key";
    NamespacedKey NSK_OWNED_ENTITY = new NamespacedKey(Instantiated.instance(),"owned-entity");
    NamespacedKey NSK_EDIT_TOOL = new NamespacedKey(Instantiated.instance(),"edit-tool");
    boolean startDungeon(String identifier, Player... players);
    void createDungeonWorld();
    InstancedDungeon getInstancedDungeon(Player player);
    void cleanup();
    World getDungeonWorld();
    Map<String, Dungeon> getDungeons();
    Map<String,List<InstancedDungeon>> getInstancedDungeons();
    Map<ImmutableLocation, InstancedDungeon> getInstancedDungeonLocations();
    void setDungeonKeyItem(ItemStack item);
    ItemStack getDungeonKeyItem();
}
