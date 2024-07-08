package dev.munky.instantiated.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class VectorUtil {
    public static BlockVector3 toBlockVector(Location location){
        return BlockVector3.at(location.getX(),location.getY(), location.getZ());
    }
    public static Location toLocation(BlockVector3 vector, World world){
        return new Location(world,vector.getX(),vector.getY(),vector.getZ());
    }
    public static Location toLocation(Vector3 vector, World world){
        return new Location(world,vector.getX(),vector.getY(),vector.getZ());
    }
    public static Vector3 toVector3(Location location){
        return Vector3.at(location.getX(), location.getY(), location.getZ());
    }
    public static BlockVector3 toBlockVector(Vector vector){
        return BlockVector3.at(vector.getX(), vector.getY(), vector.getZ());
    }
    public static BlockVector3 toBlockVector(BlockVector3 vector){
        return BlockVector3.at(vector.getX(), vector.getY(), vector.getZ());
    }
    public static Vector toVector(BlockVector3 vector){
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }
}
