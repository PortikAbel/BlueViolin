package Server;

import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommandProcessor {
    private final List<Database> databases;
    private Database usedDatabase;

    public CommandProcessor() throws IOException, ParseException {
        databases = Json.buildDatabases();
        usedDatabase = databases.get(0);
    }

    public List<Database> getDatabases() {
        return databases;
    }

    public void processCommand(String command) throws DatabaseExceptions.DataDefinitionException, DatabaseExceptions.UnknownCommandException, DatabaseExceptions.UnsuccesfulDeleteException, DatabaseExceptions.DatabaseNotExistsException {
        String[] dividedCommand = command.split(" ");
        switch (dividedCommand[0].toUpperCase()) {
            case "CREATE":
                create(command);
                break;
            case "DELETE":
                delete(dividedCommand);
                break;
            case "INSERT":
                insert(dividedCommand);
                break;
            case "USE":
                use(dividedCommand);
                break;
            default:
                throw (new DatabaseExceptions.UnknownCommandException("Uknown command."));
        }
    }

    public void create(String command) throws DatabaseExceptions.DataDefinitionException, DatabaseExceptions.UnknownCommandException {
        //CREATE DATABASE <databasename>
        // CREATE TABLE -table name- (
        //  -column name- -column type- -NOT NULL- -UNIQUE- -REFERENCES table_name(column_name)-,
        //  );
        String[] dividedCommand = command.split(" ");
        switch (dividedCommand[1].toUpperCase()){
            case "DATABASE":
                databases.add(new Database(dividedCommand[2]));
                break;
            case "TABLE":
                if(usedDatabase.getTables().stream().noneMatch(o -> o.getName().equals(dividedCommand[2]))){
                    Table newTable = new Table(dividedCommand[2]);
                    String columns = command.split("\\(", 2)[1].replace("\n", "");
                    String[] columnsSeparated = columns.substring(0, columns.length()-2).split(",");
                    for(String column : columnsSeparated){
                        String[] parameter = column.replaceFirst("^\\s*", "").split(" ");
                        if(parameter[0].toUpperCase().equals("PRIMARY")){
                            int i = 2;
                            while(i < parameter.length){
                                int finalI = i;
                                Attribute currentAttribute = newTable.getAttributes().stream()
                                        .filter(o -> o.getName().equals(parameter[finalI])).findAny().orElse(null);
                                assert currentAttribute != null;
                                currentAttribute.setPk(true);
                                i++;
                            }
                        }
                        else{
                            if(newTable.getAttributes().stream().noneMatch(o -> o.getName().equals(parameter[0]))) {
                                newTable.addAttribute(new Attribute(parameter));
                            }
                            else{
                                throw (new DatabaseExceptions.DataDefinitionException("This column name already exist in this table."));
                            }
                        }
                    }
                    usedDatabase.addTable(newTable);
                }
                else{
                    throw (new DatabaseExceptions.DataDefinitionException("This tabl name already exist in this database."));
                }
                break;

            default:
                throw (new DatabaseExceptions.UnknownCommandException("Uknown command."));
        }
    }

    public void delete(String[] command) throws DatabaseExceptions.UnknownCommandException, DatabaseExceptions.UnsuccesfulDeleteException {
        //DELETE DATABASE <databasename>
        //DELETE TABLE <tablename>
        switch (command[1].toUpperCase()){
            case "DATABASE":
                Database database = databases.stream()
                        .filter(o -> o.getName().equals(command[2]))
                        .findAny().orElse(null);
                databases.remove(database);
                break;

            case "TABLE":
                Table table = usedDatabase.getTables().stream()
                        .filter(o -> o.getName().equals(command[2]))
                        .findAny().orElse(null);
                usedDatabase.removeTable(table);
                break;
            default:
                throw (new DatabaseExceptions.UnknownCommandException("Uknown command."));
        }

    }

    public void insert(String[] dividedCommand){

    }

    public void use( String[] command) throws DatabaseExceptions.DatabaseNotExistsException {
        usedDatabase = databases.stream()
                .filter(o -> o.getName().equals(command[1]))
                .findAny().orElse(null);
        if(usedDatabase == null){
            throw (new DatabaseExceptions.DatabaseNotExistsException("This Database not exists"));
        }
    }
}
