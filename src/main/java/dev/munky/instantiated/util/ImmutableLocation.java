package dev.munky.instantiated.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.munky.instantiated.data.json.JsonSerializable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ImmutableLocation implements JsonSerializable {
    final World world;
    final double x;
    final double y;
    final double z;
    final float yaw;
    final float pitch;
    public ImmutableLocation(World world,double x, double y, double z){
        this(world,x,y,z,0,0);
    }
    public ImmutableLocation(World world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    public World getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Vector toVector(){
        return new Vector(x,y,z);
    }
    public Location toLocation(){
        return new Location(world,x,y,z,yaw,pitch);
    }
    public ImmutableVector toImmutableVector(){
        return new ImmutableVector(x,y,z);
    }
    public String toString(){
        return "ImmutableLocation["+world+","+x+","+y+","+z+","+yaw+","+pitch+"]";
    }
    public static ImmutableLocation of(Location location){
        return new ImmutableLocation(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
    public int hashCode(){
        return world.hashCode() + (int) x + (int) y + (int) z + (int) yaw + (int) pitch;
    }
    @Override
    public JsonElement serialize() {
        JsonObject object = new JsonObject();
        object.addProperty("world",world.getName());
        object.addProperty("x",x);
        object.addProperty("y",y);
        object.addProperty("z",z);
        object.addProperty("yaw",yaw);
        object.addProperty("pitch",pitch);
        return object;
    }
}
