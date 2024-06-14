package dev.munky.instantiated.config;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.event.room.mob.InstancedDungeonMobSpawnEvent;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.adapters.AbstractWorld;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MythicMobsSupportHandler extends InstantiatedConfiguration {
    private static Listener EVENT_LISTENER = null;
    private final Map<String, String> MOBS = new HashMap<>();
    public MythicMobsSupportHandler() {
        super(Instantiated.instance(), "mythic-mobs.yml");

        ConfigurationSection mobs = getYaml().getConfigurationSection("mobs");
        if (mobs!=null) {
            Set<String> dungeonIdentifiers = mobs.getKeys(false);
            for (String dungeonIdentifier : dungeonIdentifiers) {
                String mythicIdentifier = mobs.getString(dungeonIdentifier);
                MythicMob mythicMob = MythicBukkit.inst().getAPIHelper().getMythicMob(mythicIdentifier);
                if (mythicMob==null){
                    Instantiated.logger().warning("Mythic mob '" + mythicIdentifier + "' does not exist");
                }else
                    MOBS.put(dungeonIdentifier,mythicIdentifier);
            }
        }

        if (EVENT_LISTENER!=null){
            HandlerList.unregisterAll(EVENT_LISTENER);
        }

        EVENT_LISTENER = ListenerFactory.registerEvent(InstancedDungeonMobSpawnEvent.class,(e)->{
            String identifier = e.getDungeonMob().getMobIdentifier();
            String mythicIdentifier = MOBS.get(identifier);
            if (mythicIdentifier!=null){
                try{
                    MythicMob mythicMob = MythicBukkit.inst().getAPIHelper().getMythicMob(mythicIdentifier);
                    if (mythicMob!=null){
                        LivingEntity livingEntity = (LivingEntity) MythicBukkit.inst().getAPIHelper().spawnMythicMob(mythicMob,e.getSpawnLocation(),1);
                        e.setLivingEntity(livingEntity);
                        e.getDungeonMob().getCustom().put("mythic-identifier",mythicIdentifier);
                    }
                }catch (Exception ex){
                    Instantiated.logger().warning("Error while spawning mythic mob:\n" + ex.getMessage());
                }
            }
        });
    }
}
