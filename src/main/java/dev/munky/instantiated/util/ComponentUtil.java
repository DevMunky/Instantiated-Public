package dev.munky.instantiated.util;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.ListenerFactory;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ComponentUtil {
    public static MiniMessage miniMessage = MiniMessage.miniMessage();

    public static String toString(Component component){
        String result = "NULL";
        if (component==null) Instantiated.logger().warning("Null Component attempted serialization");
        else result = PlainTextComponentSerializer.plainText().serialize(component);
        return result;
    }
    public static Component toComponent(String minimessage){
        Component result = Component.text("NULL").color(NamedTextColor.RED);
        if (minimessage==null) Instantiated.logger().warning("Null string attempted deserialization");
        else result = miniMessage.deserialize(minimessage);
        return result.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
    public static Component toComponent(Component... components) {
        Component text = null;
        Iterator<Component> iterator = Arrays.stream(components).iterator();
        while (iterator.hasNext()) {
            Component next = iterator.next();
            // TitanLib.getInstance().getLogger().info("Updated line -> " + ComponentUtil.toString(next));
            if (text == null) text = next;
            else text = text.append(next).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);;
            if (iterator.hasNext()) {
                text = text.append(Component.newline());
            }
        }
        if (text == null) {
            Instantiated.logger().warning("Array of Components to single was called with an empty array!");
            text = ComponentUtil.toComponent("<red>Empty List");
        }
        return text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
    public static Component toComponent(String... strings){
        StringBuilder text = new StringBuilder();
        Iterator<String> iterator = Arrays.stream(strings).iterator();
        while (iterator.hasNext()) {
            String mini = iterator.next();
            mini = "<reset>" + mini;
            text.append(mini);
            if (iterator.hasNext()) {
                text.append("<newline>");
            }
        }
        if (text.isEmpty()) {
            Instantiated.logger().warning("Array of Strings to single Component was called with an empty array!");
            text = new StringBuilder("<red>Empty List");
        }
        return toComponent(text.toString());
    }

    /**
     * Typically used for certain times when a minimessage input must come from a component, like chat events
     * @param mini
     * @return
     */
    public static Component extractMinimessage(Component mini){
        if (mini==null) return toComponent("<red>Null");
        String miniStr = toString(mini);
        return toComponent(miniStr);
    }
    public static List<Component> toComponentList(String... strings){
        List<Component> compList = new ArrayList<>();
        Iterator<String> iterator = Arrays.stream(strings).iterator();
        while (iterator.hasNext()) {
            String mini = iterator.next();
            mini = "<reset>" + mini;
            compList.add(toComponent(mini));
        }
        if (compList.isEmpty()) {
            Instantiated.logger().warning("Array of Strings to single Component was called with an empty array!");
            compList.add(toComponent("<red>Empty List"));
        }
        
        return compList;
    }
    public static class Questionnaire {
        static final HoverEvent<?> QUESTION_HOVER_EVENT = ComponentUtil.toComponent("<gray><italic>Click me to set").asHoverEvent();
        static final ClickCallback.Options QUESTION_CLICKABLE_OPTIONS = ClickCallback.Options.builder().lifetime(Duration.of(25, ChronoUnit.SECONDS)).uses(1).build();
        public static Component create(
                Component label,
                boolean overrideDefaultHover,
                ClickCallback<Audience> callback
        ){
            if (!overrideDefaultHover)
                return label.hoverEvent(QUESTION_HOVER_EVENT).clickEvent(ClickEvent.callback(callback, QUESTION_CLICKABLE_OPTIONS));
            else
                return label.clickEvent(ClickEvent.callback(callback, QUESTION_CLICKABLE_OPTIONS));
        }
    }
}
