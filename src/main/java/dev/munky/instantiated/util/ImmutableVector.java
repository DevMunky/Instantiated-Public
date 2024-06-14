package dev.munky.instantiated.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector3;
import dev.munky.instantiated.data.json.JsonSerializable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ImmutableVector implements JsonSerializable {
    final double x;
    final double y;
    final double z;
    public ImmutableVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
    public Vector toVector(){
        return new Vector(x,y,z);
    }
    public Location toLocation(World world){
        return new Location(world,x,y,z);
    }
    public BlockVector3 toBlockVector3(){
        return BlockVector3.at(x,y,z);
    }
    public String toString(){
        return "ImmutableVector["+x+","+y+","+z+"]";
    }
    public static ImmutableVector of(Vector vector){
        return new ImmutableVector(vector.getX(), vector.getY(), vector.getZ());
    }
    public static ImmutableVector of(Location location){
        return new ImmutableVector(location.getX(), location.getY(), location.getZ());
    }
    public static ImmutableVector of(BlockVector3 vector){
        return new ImmutableVector(vector.getX(), vector.getY(), vector.getZ());
    }
    @Override
    public JsonElement serialize() {
        JsonObject object = new JsonObject();
        object.addProperty("x",x);
        object.addProperty("y",y);
        object.addProperty("z",z);
        return object;
    }
}
