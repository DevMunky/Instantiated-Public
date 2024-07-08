package dev.munky.instantiated.data.sql;

import dev.munky.instantiated.InstantiatedPlugin;
import org.jetbrains.annotations.Blocking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class MySQLManager {
    public final InstantiatedPlugin plugin;
    public final String sql_host;
    public final String sql_port;
    public final String sql_database;
    public final String sql_username;
    private final String sql_password;
    public MySQLManager(
            InstantiatedPlugin plugin,
            String host,
            String port,
            String database,
            String username,
            String password
    ){
        this.plugin = plugin;
        this.sql_host = host;
        this.sql_port = port;
        this.sql_database = database;
        this.sql_username = username;
        this.sql_password = password;
    }
    private Connection connection;
    // connect
    public void connect() {
        if (!isConnected()) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" +
                                this.sql_host + ":" +
                                this.sql_port + "/" +
                                this.sql_database,
                        this.sql_username,
                        this.sql_password
                );
                plugin.getLogger().info("MYSQL connection started!");
            } catch (Exception e) {
                plugin.getLogger().severe("Could not connect to database ");
                e.printStackTrace();
            }
        }
    }

    // disconnect
    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
                plugin.getLogger().info("MYSQL connection closed.");
            } catch (Exception e) {
                plugin.getLogger().severe("SQL ERROR: " + e.getMessage());
            }
        }
    }

    // isConnected
    public boolean isConnected() {
        return (connection != null);
    }

    // getConnection
    public Connection getConnection() {
        return connection;
    }
    @Blocking
    public void executePreparedStatement(String statement) {
        // "CREATE TABLE IF NOT EXISTS titanquests_player_data (player_uuid VARCHAR(36),data VARCHAR(10000),PRIMARY KEY (player_uuid))"
        try (
                PreparedStatement ps = getConnection().prepareStatement(statement)
        ){
            ps.executeUpdate();
        }catch (Exception e){
            plugin.getLogger().severe("Sql exception: " + e.getMessage());
        }
    }
}
