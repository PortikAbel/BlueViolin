package Server.DbStructure;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Table {
    private final String name;
    private final List<Attribute> attributes;

    public Table(JSONObject jsonObject) {

        name = (String) jsonObject.get("name");
        attributes = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) jsonObject.get("attributes");
        for (Object o : jsonArray) {
            Attribute currentAttribute = new Attribute((JSONObject)o);
            attributes.add(currentAttribute);
        }
    }

    public Table(String name) {
        this.name = name;
        attributes = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(Attribute newAttribute){
        attributes.add(newAttribute);
    }
}
