package Server;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;
    private String refTable, refColumn;
    private boolean pK;
    private boolean fK;
    private boolean notNull;
    private boolean unique;

    public Attribute(JSONObject o) {

        this.name = (String) o.get("name");
        this.refTable = (String) o.get("refTable");
        this.refColumn = (String) o.get("refColumn");
        this.pK = (boolean) o.get("pK");
        this.fK = (boolean) o.get("fK");
        this.notNull = (boolean) o.get("notNULL");
        this.unique = (boolean) o.get("unique");

    }

    public Attribute(String name, String refTable, String refColumn, boolean pK, boolean fK, boolean notNull, boolean unique) {
        this.name = name;
        this.refTable = refTable;
        this.refColumn = refColumn;
        this.pK = pK;
        this.fK = fK;
        this.notNull = notNull;
        this.unique = unique;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRefTable() {
        return refTable;
    }

    public void setRefTable(String refTable) {
        this.refTable = refTable;
    }

    public String getRefColumn() {
        return refColumn;
    }

    public void setRefColumn(String refColumn) {
        this.refColumn = refColumn;
    }

    public boolean getPK() {
        return pK;
    }

    public void setPK(boolean pK) {
        this.pK = pK;
    }

    public boolean getFK() {
        return fK;
    }

    public void setFK(boolean fK) {
        this.fK = fK;
    }

    public boolean getNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public boolean getUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }
}
