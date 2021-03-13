package Server;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;
    private String fKReference;
    private boolean pK;
    private boolean fK;
    private boolean notNUll;
    private boolean unique;


    public Attribute(JSONObject o) {

        this.name = (String) o.get("name");
        this.fKReference = (String) o.get("fKReference");
        this.pK = (boolean) o.get("pK");
        this.fK = (boolean) o.get("fK");
        this.notNUll = (boolean) o.get("notNULL");
        this.unique = (boolean) o.get("unique");

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getfKReference() {
        return fKReference;
    }

    public void setfKReference(String fKReference) {
        this.fKReference = fKReference;
    }

    public boolean ispK() {
        return pK;
    }

    public void setpK(boolean pK) {
        this.pK = pK;
    }

    public boolean isfK() {
        return fK;
    }

    public void setfK(boolean fK) {
        this.fK = fK;
    }

    public boolean isNotNUll() {
        return notNUll;
    }

    public void setNotNUll(boolean notNUll) {
        this.notNUll = notNUll;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }
}
