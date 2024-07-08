package dev.munky.instantiated.event;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.logger.ConsoleColors;
import dev.munky.instantiated.util.Util;
import dev.munky.instantiated.util.Util;
import kotlin.ExceptionsKt;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * MADE BY DEVMUNKY!!!!
 */
public class ListenerFactory {
    private ListenerFactory(){
        throw new UnsupportedOperationException("ListenerFactory cannot be instantiated!");
    }
    public static final Map<String,Listener> listeners = new HashMap<>();
    public static <T extends Event> Listener registerEvent(Class<T> eventClass, Consumer<T> event){
        return registerEvent(eventClass,EventPriority.NORMAL,event);
    }
    public static <T extends Event> Listener registerEvent(Class<T> eventClass, BiConsumer<T,Listener> event) {
        return registerEvent(eventClass,EventPriority.NORMAL,event);
    }
    public static <T extends Event> Listener registerEvent(Class<T> eventClass,EventPriority priority, Consumer<T> event){
        return registerEvent(eventClass,priority,(e,l)->event.accept(e));
    }
    public static <T extends Event> Listener registerEvent(Class<T> eventClass, EventPriority priority, BiConsumer<T,Listener> eventAndListener) {
        Listener listener = new Listener() {};
        EventExecutor executor = (listenerInstance, ev) -> {
            try{
                if (eventClass.isInstance(ev)) {
                    eventAndListener.accept(eventClass.cast(ev), listener);
                }
            }catch(Throwable t){
                System.err.print(ConsoleColors.BG_RED + "An event executor registered through Instantiated's ListenerFactory threw an exception" + ConsoleColors.RESET + "\n");
                Util.stackMessage(t);
            }
        };
        Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, Instantiated.instance());
        return listener;
    }
}
