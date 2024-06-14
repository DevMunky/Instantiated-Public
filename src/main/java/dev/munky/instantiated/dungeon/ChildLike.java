package dev.munky.instantiated.dungeon;

public interface ChildLike<P extends ParentLike>{
    P getParent();
}
