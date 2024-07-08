package dev.munky.instantiated.event.testing;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.event.room.mob.InstancedDungeonMobSpawnEvent;
import dev.munky.instantiated.util.ComponentUtil;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class TestingMobs {
    public TestingMobs(){
        Instantiated.logger().warning("[TESTINGMOBS] We are testing mobs");
        ListenerFactory.registerEvent(InstancedDungeonMobSpawnEvent.class,e->{
            EntityType type = EntityType.fromName(e.getDungeonMob().getCustom().get("entity-type"));
            if (type!=null){
                if (type.getEntityClass() == null){
                    Instantiated.logger().warning("[TESTINGMOBS] Entity class is null, type is not");
                    return;
                }
                LivingEntity ent = (LivingEntity) e.getSpawnLocation().getWorld().spawn(e.getSpawnLocation(), type.getEntityClass(), entity->{
                    entity.customName();
                    TextDisplay display = e.getSpawnLocation().getWorld().spawn(e.getSpawnLocation(), TextDisplay.class);
                    //display.setTransformation(new Transformation(
                    //      new Vector3f(0,0.5f,0),
                    //      new AxisAngle4f(),
                    //      new Vector3f(),
                    //      new AxisAngle4f()
                    //));
                    display.text(ComponentUtil.toComponent("<green>" + e.getDungeonMob().getIdentifier() + "<newline><blue>Test mob:" + type));
                    display.setBillboard(Display.Billboard.CENTER);
                    boolean riding = entity.addPassenger(display);
                    if (!riding){
                        Instantiated.logger().warning("NOT RIDING");
                    }
                    entity.setCustomNameVisible(true);
                });
                e.setLivingEntity(ent);
            }else{
                Instantiated.logger().warning("[TESTINGMOBS] Entity type is null");
            }
        });
    }
}
