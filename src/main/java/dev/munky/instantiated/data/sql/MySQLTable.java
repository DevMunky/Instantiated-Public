package dev.munky.instantiated.data.sql;

import dev.munky.instantiated.Instantiated;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

@SuppressWarnings("unused")
public abstract class MySQLTable {
    private static final String managerDisconnected = "SQL Manager is not connected! Cannot execute statements";
    private static final String exceptionMessage = "Sql exception: ";
    public final MySQLManager manager;
    public final String name;
    public final MySQLColumn[] columns;
    public final MySQLColumn primaryColumn;
    public MySQLTable(MySQLManager manager, String name, MySQLColumn... columns) {
        this.manager = manager;
        this.name = name;
        if (columns.length == 0) throw new IllegalArgumentException("Cannot create a table with 0 columns");
        this.columns = columns;
        this.primaryColumn = columns[0];
    }
    private void error_SqlManagerDisconnected(){
        manager.plugin.getLogger().severe(managerDisconnected);
    }
    private void error_ExceptionOccurred(Exception e){
        manager.plugin.getLogger().severe(exceptionMessage + e.getMessage());
        e.printStackTrace();
    }
    @Blocking
    public boolean createTable() {
        StringBuilder columns = new StringBuilder();
        for (MySQLColumn column : this.columns) {
            columns.append(column).append(",");
        }
        AtomicBoolean result = new AtomicBoolean(false);
        Iterator it = columns.chars().iterator();
        Stream.of(it);
        execute("CREATE TABLE IF NOT EXISTS " +
                this.name +
                "(" + columns + "PRIMARY KEY (" +
                this.primaryColumn.name +
                "))",
                ps->{
            try{
                ps.executeUpdate();
                result.set(true);
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        });
        return result.get();
    }
    public @NotNull List<Map<MySQLColumn,Object>> readTable(){
        return readTable(this.columns);
    }
    @Blocking
    public @NotNull List<Map<MySQLColumn,Object>> readTable(final @NotNull MySQLColumn... columns){
        if (columns.length == 0) throw new IllegalArgumentException("Cannot read 0 columns from table");
        List<Map<MySQLColumn,Object>> result = new ArrayList<>();
        StringBuilder columnNames = new StringBuilder();
        for (MySQLColumn column : columns) {
            columnNames.append(",").append(column.name);
            result.add(new HashMap<>());
        }
        columnNames.deleteCharAt(0); // removing the first comma
        execute("SELECT %s FROM %s".formatted(columnNames, this.name),ps->{
            try{
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    for (int i = 0; i < result.size(); i++) {
                        result.get(i).put(columns[i],rs.getObject(columns[i].name));
                    }
                }
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        });
        return result;
    }

    /**
     * ORDER MATTERS! THE FIRST ENTRY IS THE FIRST COLUMN!
     * @param entries the columns and values to insert
     * @return true if success, otherwise false.
     */
    @Blocking
    public boolean insertIntoTable(final @NotNull MySQLEntry<?>... entries) {
        if (entries.length != columns.length){
            throw new IllegalArgumentException("Not enough entries to satisfy every column: " + Arrays.toString(columns));
        }
        StringBuilder questionMarkString = new StringBuilder();
        for (int i = 1; i <= entries.length; i++) {
            questionMarkString.append(",").append("?");
        }
        questionMarkString.deleteCharAt(0); // remove first comma that does not belong
        AtomicBoolean result = new AtomicBoolean(false);
        execute("INSERT IGNORE INTO %s VALUES (%s)".formatted(this.name, questionMarkString),ps->{
            try{
                int i = 1;
                for (MySQLEntry<?> entry : entries) {
                    ps.setObject(i, entry.value, entry.column.getSQLType());
                    i++;
                }
                ps.executeUpdate();
                result.set(true);
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        });
        return result.get();
    }
    @Blocking
    public <K,V> boolean updateTable(
            final K key,
            final MySQLColumn column,
            final V value
    ){
        Class<?> clazz = MySQLColumn.validDataTypes.get(column.getStringType());
        if (!(clazz.isInstance(value))){
            throw new IllegalArgumentException("Value is not an dungeon of the column type '" + column.getStringType() + "'!");
        }
        clazz = MySQLColumn.validDataTypes.get(primaryColumn.getStringType());
        if (!(clazz.isInstance(key))){
            throw new IllegalArgumentException("Key is not an dungeon of the primary key type '" + primaryColumn.getStringType() + "'!");
        }
        AtomicBoolean result = new AtomicBoolean(false);
        execute("UPDATE %s SET %s = ? WHERE %s = ?".formatted(this.name,column.name,primaryColumn.name),ps->{
            try{
                ps.setObject(1,value, column.getSQLType());
                ps.setObject(2,key, primaryColumn.getSQLType());
                ps.executeUpdate();
                result.set(true);
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        });
        return result.get();
    }
    @Nullable
    public <K> List<Map<MySQLColumn,Object>> readRow(
            final K key
    ){
        Class<?> keyClass = MySQLColumn.validDataTypes.get(primaryColumn.getStringType());
        if (!(keyClass.isInstance(key))){
            throw new IllegalArgumentException("Key is not an dungeon of the primary key type '" + primaryColumn.getStringType() + "'!");
        }

        AtomicReference<List<Map<MySQLColumn,Object>>> result = new AtomicReference<>();
        execute("SELECT * FROM %s WHERE %s = ?".formatted(this.name,primaryColumn.name),ps->{
            try {
                ps.setObject(1, key, primaryColumn.getSQLType());
                ResultSet rs = ps.executeQuery();
                List<Map<MySQLColumn,Object>> multiDimensionalList = new ArrayList<>();
                int countColumn = rs.getMetaData().getColumnCount();
                if (countColumn!=this.columns.length){
                    throw new DataFormatException("Columns in row does not match columns in table. Row: " + countColumn + ", Table: " + this.columns.length);
                }
                if (countColumn!=0) {
                    Map<MySQLColumn,Object> tempList;
                    while (rs.next()) {
                        tempList = new HashMap<>();
                        for (int i = 1; i <= countColumn; i++) {
                            tempList.put(this.columns[i-1],rs.getObject(this.columns[i-1].name));
                        }
                        multiDimensionalList.add(tempList);
                    }
                }
                result.set(multiDimensionalList);
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        });
        return result.get();
    }
    public <K> boolean removeRow(K key){
        Class<?> keyClass = MySQLColumn.validDataTypes.get(primaryColumn.getStringType());
        if (!(keyClass.isInstance(key))){
            throw new IllegalArgumentException("Key is not an dungeon of the primary key type '" + primaryColumn.getStringType() + "'!");
        }
        AtomicBoolean result = new AtomicBoolean(false);
        execute("DELETE FROM %s WHERE %s = ?".formatted(this.name,primaryColumn.name),ps->{
            try {
                ps.setObject(1, key, primaryColumn.getSQLType());
                ps.executeUpdate();
                result.set(true);
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        });
        return result.get();
    }
    protected void execute(final String statement, final Consumer<PreparedStatement> function){
        if (manager.isConnected()){
            try(PreparedStatement ps = manager.getConnection().prepareStatement(statement)){
                function.accept(ps);
            } catch (Exception e) {
                error_ExceptionOccurred(e);
            }
        }else{
            error_SqlManagerDisconnected();
        }
    }
    public record MySQLEntry<Type>(
            @NotNull MySQLColumn column,
            @NotNull Type value
    ){
    }
    public record MySQLColumn(
            String name,
            String type
    ){
        public static final Map<String,Class<?>> validDataTypes;
        static{
            validDataTypes = new HashMap<>();
            validDataTypes.put("CHAR",String.class);
            validDataTypes.put("VARCHAR",String.class);
            validDataTypes.put("BINARY",byte[].class);
            validDataTypes.put("VARBINARY",byte[].class);
            validDataTypes.put("TINYBLOB",Object.class);
            validDataTypes.put("TINYTEXT",String.class);
            validDataTypes.put("TEXT",String.class);
            validDataTypes.put("BLOB",Object.class);
            validDataTypes.put("MEDIUMTEXT",String.class);
            validDataTypes.put("MEDIUMBLOB",Object.class);
            validDataTypes.put("LONGTEXT",String.class);
            validDataTypes.put("LONGBLOB",Object.class);
            validDataTypes.put("ENUM",Object.class);
            validDataTypes.put("SET",Object.class);
            validDataTypes.put("BIT",byte.class);
            validDataTypes.put("TINYINT",int.class);
            validDataTypes.put("BOOL",boolean.class);
            validDataTypes.put("BOOLEAN",boolean.class);
            validDataTypes.put("SMALLINT",int.class);
            validDataTypes.put("MEDIUMINT",int.class);
            validDataTypes.put("INT",int.class);
            validDataTypes.put("INTEGER",int.class);
            validDataTypes.put("BIGINT",long.class);
            validDataTypes.put("FLOAT",float.class);
            validDataTypes.put("DOUBLE",double.class);
            validDataTypes.put("DOUBLE PRECISION", double.class);
            validDataTypes.put("DECIMAL",double.class);
            validDataTypes.put("DEC",double.class);
        }
        public MySQLColumn{
            if (validDataTypes.keySet().stream().noneMatch(type::contains)){
                throw new IllegalArgumentException(
                        "Data Type supplied to a Column record was not a valid data type! Valid types below" + "\n" +
                                Arrays.toString(validDataTypes.keySet().toArray())
                );
            }
        }
        public String getStringType(){
            return this.type.split("\\(")[0];
        }
        public JDBCType getSQLType() {
            try {
                return JDBCType.valueOf(this.getStringType());
            } catch (Exception e) {
                Instantiated.logger().warning("An SQL Column has an invalid type somehow, contact DevMunky:" +
                        "Column: " + this.name + ", Type: " + this.getStringType());
                return null;
            }
        }
        public String toString(){
            return name + " " + type;
        }
    }
}
