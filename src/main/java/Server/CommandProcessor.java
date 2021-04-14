package Server;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
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
            throws DbExceptions.DataDefinitionException,
            DbExceptions.UnknownCommandException,
            DbExceptions.UnsuccessfulDeleteException,
            DbExceptions.DataManipulationException,
            IOException
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
                Json.saveDatabases(databases);
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
                Json.saveDatabases(databases);
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
        Pattern createTablePattern = Pattern.compile("^CREATE TABLE ([a-zA-Z0-9_]+)\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
        Matcher createTableMatcher = createTablePattern.matcher(command);

        Pattern pkPattern = Pattern.compile("^PRIMARY KEY\\((.+)\\)", Pattern.CASE_INSENSITIVE);
        Pattern fkPattern = Pattern.compile("REFERENCES ([^()]+)\\(([^()]+)\\)", Pattern.CASE_INSENSITIVE);
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
        Pattern insertPattern = Pattern.compile(
                "INSERT INTO ([a-zA-Z0-9_]+)[ ]?(\\(([^()]+)\\))?VALUES\\(([^()]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(command);
        String tableName, intoCols, values;
        if(insertMatcher.find()) {
            tableName = insertMatcher.group(1);
            intoCols = insertMatcher.group(3);
            values = insertMatcher.group(4);
        }
        else{
            throw new DbExceptions.DataManipulationException("Incorrect insert syntax.");
        }
        
        Table currentTable = usedDatabase.getTables().stream()
                .filter(o -> o.getName().equals(tableName))
                .findFirst()
                .orElse(null);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException("Can't insert into table, because table does not exists: " + tableName);

        if (intoCols == null) {
            intoCols = currentTable.getAttributes().stream()
                    .map(Attribute::getName)
                    .collect(Collectors.joining(","));
        }

        List<String> intoColumn, value;
        intoColumn = Arrays.asList(intoCols.split(","));
        value = Arrays.asList(values.split(","));

        List<String> valuesToInsert = new ArrayList<>();
        List<String> keysToInsert = new ArrayList<>();

        for (Attribute attribute : currentTable.getAttributes())
        {
            int i = intoColumn.indexOf(attribute.getName());
            if (i < 0) {
                if (attribute.isNotNull())
                    throw new DbExceptions.DataManipulationException("This field must not be null.");
                valuesToInsert.add("null");
            }
            else {
                String val = value.get(i);
                // type check
                if ("int".equalsIgnoreCase(attribute.getDataType())) {
                    try {
                        Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        throw new DbExceptions.DataManipulationException(
                                "Integer expected for attribute " + attribute.getName());
                    }
                /*} else if ("bool".equalsIgnoreCase(attribute.getDataType())) {
                    if (!"true".equalsIgnoreCase(val) || !"false".equalsIgnoreCase(val))
                        throw new DbExceptions.DataManipulationException(
                                "Bool expected for attribute " + attribute.getName());*/
                } else {
                    Matcher varcharTypeMatcher = Pattern
                            .compile("VARCHAR\\(([0-9]+)\\)", Pattern.CASE_INSENSITIVE)
                            .matcher(attribute.getDataType());
                    Matcher varcharValueMatcher = Pattern.compile("[\"']([^\"'])[\"']").matcher(val);
                    if (!varcharTypeMatcher.find()) {
                        throw new DbExceptions.DataManipulationException(
                                "Attribute " + attribute.getName() +
                                        " has unknown datatype:" + attribute.getDataType()
                        );
                    }
                    if (!varcharValueMatcher.find()) {
                        throw new DbExceptions.DataManipulationException(
                                "Varchar variable was given incorrectly: " + val
                        );
                    }
                    int maxLen = Integer.parseInt(varcharTypeMatcher.group(1));
                    String varchar = varcharValueMatcher.group(1);
                    if (varchar.length() -2 > maxLen)
                        throw (new DbExceptions.DataManipulationException("The string is too long."));
                    val = varchar;
                }

                // unique check
                if(attribute.isUnique()){
                    if(!mongoDBManager.valueIsUnique(
                            currentTable.getName(), val,
                            attribute.isPk() ? keysToInsert.size() : valuesToInsert.size())
                    )
                        throw new DbExceptions.DataManipulationException(
                                "This field must be unique.");
                }

                // not null check
                if(attribute.isNotNull() && val.equalsIgnoreCase("null"))
                    throw new DbExceptions.DataManipulationException("This field must not be null.");

                // pk check
                if (attribute.isPk())
                    keysToInsert.add(val);
                else
                    valuesToInsert.add(val);
            }
        }

        mongoDBManager.insert(
                tableName,
                String.join("#", keysToInsert),
                String.join("#", valuesToInsert)
        );
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
