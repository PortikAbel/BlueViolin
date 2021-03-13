package Server;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;
    private boolean pK;
    private boolean fK;
    private boolean notNUll;


    public Attribute(JSONObject o) {

        this.name = (String) o.get("name");
    }

    public String getName() {
        return name;
    }
}
