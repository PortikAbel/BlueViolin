package Server;

import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandProcessor {
    private final List<Database> databases;
    private Database usedDatabase;
    private final MongoDBManager mongoDBManager;

    public CommandProcessor() throws IOException, ParseException {
        databases = Json.buildDatabases();
        usedDatabase = databases.get(0);
        mongoDBManager = new MongoDBManager();
    }

    public List<Database> getDatabases() {
        return databases;
    }

    public void processCommand(String command) throws DatabaseExceptions.DataDefinitionException, DatabaseExceptions.UnknownCommandException, DatabaseExceptions.UnsuccesfulDeleteException {
        if(Character.compare(command.charAt(command.length() - 1),';') == 0){
            command = command.substring(0, command.length()-1);
        }
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
                if(databases.stream().noneMatch(o ->o.getName().equals(dividedCommand[2]))) {
                    databases.add(new Database(dividedCommand[2]));
                }
                else{
                    throw (new DatabaseExceptions.DataDefinitionException("database already exists: " + dividedCommand[2]));
                }
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
                                throw (new DatabaseExceptions.DataDefinitionException("Column already exist in this table: " + parameter[0]));
                            }
                        }
                    }
                    usedDatabase.addTable(newTable);
                }
                else{
                    throw (new DatabaseExceptions.DataDefinitionException("Table already exist in this database: " + dividedCommand[2]));
                }
                break;

            default:
                throw (new DatabaseExceptions.UnknownCommandException("Uknown command."));
        }
    }

    public void delete(String[] command) throws DatabaseExceptions.UnknownCommandException, DatabaseExceptions.DataDefinitionException {
        //DELETE DATABASE <databasename>
        //DELETE TABLE <tablename>
        switch (command[1].toUpperCase()){
            case "DATABASE":
                Database database = databases.stream()
                        .filter(o -> o.getName().equals(command[2]))
                        .findAny().orElse(null);
                if(database == null){
                    throw (new DatabaseExceptions.DataDefinitionException("Database does not exist: " + command[2]));
                }
                databases.remove(database);
                break;

            case "TABLE":
                Table table = usedDatabase.getTables().stream()
                        .filter(o -> o.getName().equals(command[2]))
                        .findAny().orElse(null);
                if(table == null){
                    throw (new DatabaseExceptions.DataDefinitionException("Table does not exist: " + command[2]));
                }
                usedDatabase.removeTable(table);
                break;
            default:
                throw (new DatabaseExceptions.UnknownCommandException("Uknown command."));
        }

    }

    public void insert(String[] dividedCommand){
        String parameters = dividedCommand[dividedCommand.length - 1];
        parameters = parameters.substring(1, parameters.length() - 1);
        String key = parameters.split(",")[0];
        parameters = Arrays.stream(parameters.split(",")).skip(1).collect(Collectors.joining("#"));
        System.out.println(key + " " + parameters);
        mongoDBManager.insert(dividedCommand[2], key, parameters);
    }

    public void use( String[] command) throws DatabaseExceptions.DataDefinitionException {
        usedDatabase = databases.stream()
                .filter(o -> o.getName().equals(command[1]))
                .findAny().orElse(null);
        if(usedDatabase == null){
            throw (new DatabaseExceptions.DataDefinitionException("Database does not exist: " + command[1]));
        }
        mongoDBManager.use(usedDatabase.getName());
    }
}
