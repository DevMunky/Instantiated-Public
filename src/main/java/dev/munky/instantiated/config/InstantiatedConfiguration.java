package dev.munky.instantiated.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.munky.instantiated.InstantiatedPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

/**
 * Put field assignments in the constructor, to keep loading of a configuration easy!
 */
public abstract class InstantiatedConfiguration {
    private final InstantiatedPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    public InstantiatedConfiguration(InstantiatedPlugin plugin, String fileName){
        this(plugin,fileName,true);
    }
    public InstantiatedConfiguration(InstantiatedPlugin plugin, String fileName, boolean isYaml){
        File file = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + fileName);
        if (!file.exists()){
            boolean result;
            if (!plugin.getDataFolder().exists()){
                plugin.getDataFolder().mkdirs();
            }
            try {
                result = file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error trying to create new '%s' configuration -> %s".formatted(fileName,e.getMessage()));
                result = false;
            }
            if (!result) {
                plugin.getLogger().warning("The file '%s' could not be created! There may be errors reading that configuration.".formatted(file.getName()));
            }else{
                InputStream in = plugin.getResource(fileName);
                if (in==null){
                    plugin.getLogger().warning("There was no resource found by the name '%s'!".formatted(fileName));
                }else{
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(in.readAllBytes());
                        out.close();
                        in.close();
                    } catch (IOException e) {
                        plugin.getLogger().severe("Error trying to save resource '%s' -> ".formatted(fileName) + e.getMessage());
                    }
                }
            }
        }
        if (isYaml)
            this.yaml = YamlConfiguration.loadConfiguration(file);
        else this.yaml = null;
        this.file = file;
        this.plugin = plugin;
    }
    public BufferedInputStream getInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(getFile()));
    }
    public BufferedOutputStream getOutputStream() throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(getFile()));
    }
    public JsonElement getJson() throws FileNotFoundException {
        return JsonParser.parseReader(new BufferedReader(new FileReader(getFile())));
    }
    public YamlConfiguration getYaml(){
        return this.yaml;
    }
    public File getFile(){
        return this.file;
    }
    public InstantiatedPlugin getPlugin(){
        return this.plugin;
    }
}
