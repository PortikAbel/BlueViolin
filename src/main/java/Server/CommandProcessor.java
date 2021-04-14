package Server;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
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
            throws DbExceptions.DataDefinitionException,
            DbExceptions.UnknownCommandException,
            DbExceptions.UnsuccessfulDeleteException,
            DbExceptions.DataManipulationException
    {
        String[] dividedCommand = command.split(" ");
        switch (dividedCommand[0].toUpperCase()) {
            case "CREATE":
                switch (dividedCommand[1].toUpperCase()) {
                    case "DATABASE":
                        createDatabase(command);
                        break;
                    case "TABLE":
                        createTable(command);
                        break;
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
                break;
            case "DELETE":
                switch (dividedCommand[1].toUpperCase()) {
                    case "DATABASE":
                        deleteDatabase(dividedCommand[2]);
                        break;
                    case "TABLE":
                        deleteTable(dividedCommand[2]);
                        break;
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
                break;
            case "INSERT":
                insert(command);
                break;
            case "USE":
                use(dividedCommand[1]);
                break;
            default:
                throw new DbExceptions.UnknownCommandException();
        }
    }

    private void createDatabase(String command) throws DbExceptions.DataDefinitionException {
        //CREATE DATABASE <databasename>
        String[] dividedCommand = command.split(" ");
        if (databases.stream().noneMatch(o -> o.getName().equals(dividedCommand[2]))) {
            databases.add(new Database(dividedCommand[2]));
        } else {
            throw (new DbExceptions.DataDefinitionException("database already exists: " + dividedCommand[2]));
        }
    }
    private void createTable(String command) throws DbExceptions.DataDefinitionException {
        // CREATE TABLE -table name- (
        //  -column name- -column type- -NOT NULL- -UNIQUE- -REFERENCES table_name(column_name)-,
        //  );
        Pattern createTablePattern = Pattern.compile("^CREATE TABLE ([a-zA-Z0-9_])\\((.+)\\)$");
        Matcher createTableMatcher = createTablePattern.matcher(command);

        Pattern pkPattern = Pattern.compile("^PRIMARY KEY\\((.+)\\)");
        Pattern fkPattern = Pattern.compile("REFERENCES ([^()]+)\\(([^()])\\)");
        Matcher pkMatcher, fkMatcher;

        if (createTableMatcher.find()) {
            String tableName = createTableMatcher.group(1);
            String attrDefinitions = createTableMatcher.group(2);
            if (usedDatabase.getTables().stream().noneMatch(o -> o.getName().equals(tableName))) {
                Table newTable = new Table(tableName);

                for (String attrDef : attrDefinitions.split(",")) {
                    // primary key constraint
                    pkMatcher = pkPattern.matcher(attrDef);
                    if (pkMatcher.find()) {
                        String pks = pkMatcher.group(1);
                        for (String pk : pks.split(",")) {
                            newTable.getAttributes().stream()
                                    .filter(o -> o.getName().equals(pk))
                                    .findAny()
                                    .ifPresent(currentAttribute -> currentAttribute.setPk(true));
                        }
                    }
                    else {
                        // foreign key constraint
                        fkMatcher = fkPattern.matcher(attrDef);
                        if (fkMatcher.find()) {
                            String refTableName = fkMatcher.group(1);
                            Table refTable = usedDatabase.getTables().stream()
                                    .filter(table -> table.getName().equals(refTableName))
                                    .findAny().orElse(null);
                            if (refTable == null)
                                throw new DbExceptions.DataDefinitionException(
                                        "Referencing table does not exists: " + refTableName);
                            else {
                                String refColName = fkMatcher.group(2);
                                Attribute refCol = refTable.getAttributes().stream()
                                        .filter(attribute -> attribute.getName().equals(refColName))
                                        .findAny().orElse(null);
                                if (refCol == null)
                                    throw new DbExceptions.DataDefinitionException(
                                            "Referencing column does not exists: "+ refColName);
                                else if (!refCol.isUnique())
                                    throw new DbExceptions.DataDefinitionException(
                                            "Referencing column is not unique" + refColName);
                            }
                        }
                        String[] attrDefDivided = attrDef.split(" ");

                        if (newTable.getAttributes().stream()
                                .noneMatch(o -> o.getName().equals(attrDefDivided[0]))
                        )
                            newTable.addAttribute(new Attribute(attrDefDivided));
                        else {
                            throw new DbExceptions.DataDefinitionException(
                                    "Column already exist in this table: " + attrDefDivided[0]);
                        }
                    }
                }
                usedDatabase.addTable(newTable);
            }
            else{
                throw new DbExceptions.DataDefinitionException(
                        "Table already exist in this database: " + tableName);
            }
        }
    }

    private void deleteDatabase(String databaseName) throws DbExceptions.DataDefinitionException {
        //DELETE DATABASE <databasename>
        Database database = databases.stream()
                .filter(o -> o.getName().equals(databaseName))
                .findAny().orElse(null);
        if (database == null) {
            throw new DbExceptions.DataDefinitionException("Database does not exist: " + databaseName);
        }
        databases.remove(database);
    }
    private void deleteTable(String tableName) throws DbExceptions.DataDefinitionException {
        //DELETE TABLE <tablename>
        Table table = usedDatabase.getTables().stream()
                .filter(o -> o.getName().equals(tableName))
                .findAny().orElse(null);
        if(table == null){
            throw new DbExceptions.DataDefinitionException("Table does not exist: " + tableName);
        }
        usedDatabase.removeTable(table);
    }

    private void insert(String command) throws DbExceptions.DataManipulationException {
        Matcher regexMatcher = Pattern.compile("\\((.*?)\\)").matcher(command);
        String columns;
        if(regexMatcher.find()) {
            columns = regexMatcher.group(1);
        }
        else{
            throw (new DbExceptions.DataManipulationException("Insert does not found what columns to insert into."));
        }
        String values;
        if(regexMatcher.find()) {
            values = regexMatcher.group(1);
        }
        else{
            throw (new DbExceptions.DataManipulationException("Insert does not found what values to insert."));
        }
        Table currentTable = usedDatabase.getTables().stream()
                .filter(o -> o.getName().equals(command.split(" ", 4)[2]))
                .findFirst()
                .orElse(null);
        if(currentTable == null)
            throw (new DbExceptions.DataManipulationException("Can't insert into table, because table does not exists."));
        String[] parameters = Arrays.stream(values.split(",")).map(o -> o.replaceFirst("^\\s*", "")).toArray(String[]::new);
        String[] valuestoInsert = new String[currentTable.getAttributes().size()];
        Iterator<Attribute> it = currentTable.getAttributes().iterator();
        int i = 0;
        int j = 0;
        while (i < valuestoInsert.length && it.hasNext())
        {
            Attribute column = it.next();
            if(columns.contains(column.getName())) {
                if ("INT".equalsIgnoreCase(column.getDataType())) {
                    int number = Integer.parseInt(parameters[j]);
                } else {
                    Matcher m = Pattern.compile("\\((.*?)\\)").matcher(column.getDataType());
                    if (m.find()) {
                        String num = m.group(1);
                        if (Integer.parseInt(num) < parameters[j].length() -2)
                            throw (new DbExceptions.DataManipulationException("The string is too long."));
                        parameters[j] = parameters[j].substring(1,parameters[j].length()-1);
                    }
                }
                if(column.isUnique()){
                    if(!mongoDBManager.valueIsUnique(currentTable.getName(),parameters[j],i - 1))
                        throw (new DbExceptions.DataManipulationException("This field must be unique."));
                }

                if(column.isNotNull() && parameters[j].equals("null"))
                    throw (new DbExceptions.DataManipulationException("This field must not be null."));
                valuestoInsert[i] = parameters[j];
                j++;

            }else{
                if(column.isNotNull())
                    throw (new DbExceptions.DataManipulationException("This field must not be null."));
                valuestoInsert[i] = "null";
            }
            i++;
        }
        mongoDBManager.insert(currentTable.getName(), valuestoInsert[0], Arrays.stream(valuestoInsert).skip(1).collect(Collectors.joining("#")));
    }

    private void use(String databaseName) throws DbExceptions.DataDefinitionException {
        usedDatabase = databases.stream()
                .filter(o -> o.getName().equals(databaseName))
                .findAny().orElse(null);
        if(usedDatabase == null){
            throw (new DbExceptions.DataDefinitionException("Database does not exist: " + databaseName));
        }
        mongoDBManager.use(usedDatabase.getName());
    }
}
