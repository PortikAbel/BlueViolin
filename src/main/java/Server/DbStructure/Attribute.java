package Server.DbStructure;

import org.json.simple.JSONObject;

public class Attribute {
    private String name;
    private String dataType;
    private String refTable, refColumn;
    private boolean pk;
    private boolean fk;
    private boolean notNull;
    private boolean unique;
    private String index;

    public Attribute(JSONObject o) {

        this.name = (String) o.get("name");
        this.dataType = (String) o.get("dataType");
        this.refTable = (String) o.get("refTable");
        this.refColumn = (String) o.get("refColumn");
        this.pk = (boolean) o.get("pk");
        this.fk = (boolean) o.get("fk");
        this.notNull = (boolean) o.get("notNull");
        this.unique = (boolean) o.get("unique");
        this.index = (String) o.get("index");

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
        this.index = "";
    }
    //-column name- -column type- -NOT NULL- -UNIQUE- -REFERENCES database_name(column_name)
    public Attribute(String[] command){
        this.name = command[0];
        this.dataType = command[1];
        this.refTable = null;
        this.refColumn = null;
        this.pk = false;
        this.fk = false;
        this.notNull = false;
        this.unique = false;
        this.index = "";
        int i = 2;
        while( i < command.length){
            switch (command[i].toUpperCase()) {
                case "PRIMARY":
                    if (command[i + 1].equals("KEY")) {
                        pk = true;
                        i += 2;
                    }
                    else {
                        i++;
                    }
                    break;
                case "NOT":
                    if (command[i + 1].equals("NULL")) {
                        notNull = true;
                        i += 2;
                    }
                    else {
                        i++;
                    }
                    break;
                case "UNIQUE":
                    unique = true;
                    i++;
                    break;
                case "REFERENCES":
                    fk = true;
                    String[] references = command[i+1].substring(0, command[i+1].length() - 1).split("\\(");
                    refTable = references[0];
                    refColumn = references[1];
                    i += 2;
                    break;
                default:
                    i++;
            }
        }
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

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}

