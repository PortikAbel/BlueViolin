package Server;

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
        tables = tables = new ArrayList<>();
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

    public void addTable (Table newtable){
        tables.add(newtable);
    };

    public void removeTable(Table table){
        tables.remove(table);
    }


}
