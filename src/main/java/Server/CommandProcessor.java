package Server;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public void processCommand(String command)
            throws DatabaseExceptions.DataDefinitionException,
            DatabaseExceptions.UnknownCommandException,
            DatabaseExceptions.UnsuccesfulDeleteException
    {
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
        String[] dividedCommand = command.split("\\s+");
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

                    Pattern columnsPattern = Pattern.compile("^CREATE TABLE [a-zA-Z0-9_]\\((.*)\\)$");
                    Matcher columnsMatcher = columnsPattern.matcher(command);
                    if (columnsMatcher.find())
                    {
                        Pattern columnPattern = Pattern.compile("([^(),]+(\\([^()]*\\)[^(),]*)?),?");
                        Matcher columnMatcher = columnPattern.matcher(columnsMatcher.group(1));
                        while (columnMatcher.find()) {
                            String commandRow = columnMatcher.group(1).replace("^\\s+", "");
                            String[] columnDef = commandRow.split("\\s+");
                            if(columnDef[0].equalsIgnoreCase("PRIMARY")){
                                Pattern pkPattern = Pattern.compile("^PRIMARY[\\s]+KEY[\\s]*\\((.*)\\),?$");
                                Matcher pkMatcher = pkPattern.matcher(commandRow);
                                if (pkMatcher.find())
                                {
                                    String pks = pkMatcher.group(1);
                                    for (String pk : pks.split(",")) {
                                        Attribute currentAttribute = newTable.getAttributes().stream()
                                                .filter(o -> o.getName().equals(pk)).findAny().orElse(null);
                                        assert currentAttribute != null;
                                        currentAttribute.setPk(true);
                                    }
                                }
                            }
                            else{
                                if(newTable.getAttributes().stream().noneMatch(o -> o.getName().equals(columnDef[0]))) {
                                    newTable.addAttribute(new Attribute(columnDef));
                                }
                                else{
                                    throw (new DatabaseExceptions.DataDefinitionException("Column already exist in this table: " + columnDef[0]));
                                }
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
