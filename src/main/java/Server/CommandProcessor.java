package Server;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CommandProcessor {
    private List<Database> databases;

    public CommandProcessor() throws IOException, ParseException {
        databases = Json.buildDatabases();
    }

    public List<Database> getDatabases() {
        return databases;
    }

    public void processCommand(String command) {
        String[] dividedCommand = command.split(" ");
        switch (dividedCommand[0]) {
            case "CREATE":
                create(dividedCommand);
                break;
            case "DELETE":
                delete(dividedCommand);
                break;
            case "ADD":
                addAttribute(dividedCommand);
                break;
        }
    }

    public void create(String[] command){
        //CREATE DATABASE <databasename>
        //CREATE TABLE <tablename> <databasename>
        switch (command[1]){
            case "DATABASE":
                databases.add(new Database(command[2]));
                break;
            case "TABLE":
                Database database = (Database) databases.stream().filter(o -> o.getName().equals(command[3]));
                if(database.getTables().stream().noneMatch(o -> o.getName().equals(command[2]))) {
                    database.addTable(new Table(command[2]));
                }
                //else errormsg
                break;
                //default errormsg
        }
    }

    public void delete(String[] command){
        //DELETE DATABASE <databasename>
        //DELETE TABLE <tablename> <databasename>
        if(command[1].equals("DATABASE")) {
            Database database = (Database) databases.stream().filter(o -> o.getName().equals(command[2]));
            databases.remove(database);
        }
        else
            if(command[1].equals("TABLE")){
                Database database = (Database) databases.stream().filter(o -> o.getName().equals(command[3]));
                Table table = (Table) database.getTables().stream().filter(o -> o.getName().equals(command[2]));
                database.removeTable(table);
            }
        //default errormsg
    }

    // ADD ATTRIBUTE <tablename> name#refTable#refColumn#pk#fk##notNULL#unique <databasename>
    public void addAttribute(String[] command){
        Database database = (Database) databases.stream().filter(o -> o.getName().equals(command[4]));
        Table table = (Table) database.getTables().stream().filter(o -> o.getName().equals(command[2]));
        String[] attributeProperties = command[3].split("#");
        if(!attributeProperties[1].equals("-")){
            Table refTable = (Table) database.getTables().stream().filter(o -> o.getName().equals(attributeProperties[1]));
            Attribute refAttribute = (Attribute) refTable.getAttributes().stream().filter(o -> o.getName().equals(attributeProperties[2]));
            if(refAttribute.ispK()){
                table.addAttribute(new Attribute(attributeProperties[0], attributeProperties[1], attributeProperties[2], Boolean.parseBoolean(attributeProperties[3]), Boolean.parseBoolean(attributeProperties[4]), Boolean.parseBoolean(attributeProperties[5]), Boolean.parseBoolean(attributeProperties[6])));
            }
        }
        else{
            table.addAttribute(new Attribute(attributeProperties[0], attributeProperties[1], attributeProperties[2], Boolean.parseBoolean(attributeProperties[3]), Boolean.parseBoolean(attributeProperties[4]), Boolean.parseBoolean(attributeProperties[5]), Boolean.parseBoolean(attributeProperties[6])));
        }
    }
}
