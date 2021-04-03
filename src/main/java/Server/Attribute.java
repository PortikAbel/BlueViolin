package Server;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;
    private String dataType;
    private String refTable, refColumn;
    private boolean pk;
    private boolean fk;
    private boolean notNull;
    private boolean unique;
    private boolean index;

    public Attribute(JSONObject o) {

        this.name = (String) o.get("name");
        this.dataType = (String) o.get("dataType");
        this.refTable = (String) o.get("refTable");
        this.refColumn = (String) o.get("refColumn");
        this.pk = (boolean) o.get("pk");
        this.fk = (boolean) o.get("fk");
        this.notNull = (boolean) o.get("notNull");
        this.unique = (boolean) o.get("unique");
        this.index = (boolean) o.get("index");

    }

    public Attribute(String name, String dataType, String refTable, String refColumn, boolean pk, boolean fk, boolean notNull, boolean unique) {
        this.name = name;
        this.dataType = dataType;
        this.refTable = refTable;
        this.refColumn = refColumn;
        this.pk = pk;
        this.fk = fk;
        this.notNull = notNull;
        this.unique = unique;
        this.index = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
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

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }
}

