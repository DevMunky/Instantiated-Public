package dev.munky.instantiated.logger;

import dev.munky.instantiated.InstantiatedPlugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class InstantiatedLogger extends Logger {
    private final InstantiatedPlugin PLUGIN;
    public InstantiatedLogger(InstantiatedPlugin plugin) {
        super(plugin.getName(),null); // the super Logger really doesnt matter here.
        this.PLUGIN = plugin;
    }
    public void debug(String message){
        if (PLUGIN.debug())
            getLogger(ConsoleColors.FG_RED + "Debug - " + PLUGIN.getName() + ConsoleColors.RESET).info( ConsoleColors.FG_CYAN + message + ConsoleColors.RESET);
    }
    @Override
    public void severe(String message){
        getLogger(ConsoleColors.FG_RED.toString() + ConsoleColors.RAPID_BLINK + "Error - " + PLUGIN.getName() + ConsoleColors.RESET).severe("\n" + ConsoleColors.BG_RED + ConsoleColors.FG_CYAN + message + ConsoleColors.RESET);
    }
    @Override
    public void warning(String message){
        getLogger(ConsoleColors.FG_YELLOW + "Warning - " + PLUGIN.getName() + ConsoleColors.RESET).warning(ConsoleColors.FG_YELLOW + message + ConsoleColors.RESET);
    }
    @Override
    public void info(String message){
        getLogger(ConsoleColors.FG_BLUE + PLUGIN.getName() + ConsoleColors.RESET).info(ConsoleColors.FG_GREEN + message + ConsoleColors.RESET);
    }
    @Override
    public void log(Level level, String msg){
        switch(level.getName()){
            case "INFO"->info(msg);
            case "WARNING"->warning(msg);
            case "SEVERE"->severe(msg);
            default -> {
                severe("Unhandled Log Level '" + level.getName() + "':");
                warning(msg);
            }
        }
    }
    @Override
    public void log(Level level, String msg, Throwable thrown){
        // if (!isLoggable(level)) return;
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(thrown);
        log(lr);
        // Im not sure how to customize this one
    }
}
