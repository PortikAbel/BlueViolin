package Server.DbStructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Database {
    private String name;
    private List<Table> tables;

    public Database(JSONObject jsonObject) throws JsonProcessingException {
        name = (String) jsonObject.get("name");
        tables = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) jsonObject.get("tables");
        for (Object o : jsonArray){
            Table currentTable = new Table((JSONObject) o);
            tables.add(currentTable);
        }
    }

    public Database(String name) {
        this.name = name;
        tables = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Table> getTables() {
        return tables;
    }

    public Table getTable(String tableName) {
        return tables.stream()
                .filter(o -> o.getName().equals(tableName))
                .findFirst()
                .orElse(null);
    }

    public void addTable (Table newTable){
        tables.add(newTable);
    };

    public void removeTable(Table table){
        tables.remove(table);
    }


}
