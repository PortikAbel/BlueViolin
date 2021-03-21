package Server;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;
    private String refTable, refColumn;
    private boolean pk;
    private boolean fk;
    private boolean notNull;
    private boolean unique;

    public Attribute(JSONObject o) {

        this.name = (String) o.get("name");
        this.refTable = (String) o.get("refTable");
        this.refColumn = (String) o.get("refColumn");
        this.pk = (boolean) o.get("pk");
        this.fk = (boolean) o.get("fk");
        this.notNull = (boolean) o.get("notNull");
        this.unique = (boolean) o.get("unique");

    }

    public Attribute(String name, String refTable, String refColumn, boolean pk, boolean fk, boolean notNull, boolean unique) {
        this.name = name;
        this.refTable = refTable;
        this.refColumn = refColumn;
        this.pk = pk;
        this.fk = fk;
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

    public boolean isPk() {
        return pk;
    }

    public void setPk(boolean pk) {
        this.pk = pk;
    }

    public boolean isFk() {
        return fk;
    }

    public void setFk(boolean fk) {
        this.fk = fk;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }
}

