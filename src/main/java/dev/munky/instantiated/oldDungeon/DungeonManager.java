package dev.munky.instantiated.oldDungeon;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.dungeon.DungeonFormat;
import dev.munky.instantiated.dungeon.IdentifiableKey;
import dev.munky.instantiated.dungeon.InstancedDungeon;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface DungeonManager {
    String METADATA_DELETE_UPON_LOAD = "instantiated-delete-during-load";
    String METADATA_KILL_ON_SIGHT = "instantiated-kill-on-sight";
    String METADATA_DUNGEON_MOB = "instantiated-dungeon-mob";
    String METADATA_PERSISTENT_ENTITY = "instantiated-persist";
    String METADATA_KEY_ENTITY = "instantiated-dungeon-key";
    String METADATA_CURRENT_DUNGEON_ROOM = "instantiated-current-dungeon";
    NamespacedKey NSK_OWNED_ENTITY = new NamespacedKey(Instantiated.instance(),"owned-entity");
    NamespacedKey NSK_EDIT_TOOL = new NamespacedKey(Instantiated.instance(),"edit-tool");
    double DEFAULT_DIFFICULTY = 1.0d;
    static DungeonManager getInstance(){
        return Instantiated.instance().getDungeonManager();
    }
    boolean startDungeon(String identifier, boolean force,Player... players);
    void cacheDungeon(String identifier);
    void createDungeonWorld();
    Optional<InstancedDungeon> getCurrentDungeon(UUID player);
    void cleanup();
    World getDungeonWorld();
    Map<IdentifiableKey, DungeonFormat> getDungeons();
    Stream<InstancedDungeon> getInstancedDungeons();
}
