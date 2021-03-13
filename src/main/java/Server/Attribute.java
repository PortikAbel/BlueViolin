package Server;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;

    public Attribute(JSONObject o) {
        this.name = (String) o.get("name");
    }

    public String getName() {
        return name;
    }
}
