package Server;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandProcessor {
    private final List<Database> databases;
    private Database usedDatabase;
    private final MongoDBManager mongoDBManager;

    public CommandProcessor() throws IOException, ParseException {
        databases = Json.buildDatabases();
        usedDatabase = null;
        mongoDBManager = new MongoDBManager();
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
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        createTable(command);
                        break;
                    case "INDEX":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        createIndex(command);
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
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        deleteTable(dividedCommand[2]);
                        break;
                    case "FROM":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        delete(command);
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
                Json.saveDatabases(databases);
                break;
            case "INSERT":
                if ( usedDatabase == null)
                    throw new DbExceptions.DataDefinitionException("Unspecified database");
                insert(command);
                break;
            case "USE":
                use(dividedCommand[1]);
                break;
            case "SELECT":
                if ( usedDatabase == null)
                    throw new DbExceptions.DataDefinitionException("Unspecified database");
                select(command);
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
                            Table refTable = usedDatabase.getTable(refTableName);
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
    private void createIndex(String command) throws DbExceptions.DataManipulationException {
        Pattern createIndexPattern = Pattern.compile(
                "^CREATE INDEX ([a-zA-Z0-9_]+ )?ON ([a-zA-Z0-9_]+)\\(([^()]+)\\)$",
                Pattern.CASE_INSENSITIVE);
        Matcher createIndexMatcher = createIndexPattern.matcher(command);

        String indexName, tableName, attributeNames;

        if (createIndexMatcher.find()) {
            indexName = createIndexMatcher.group(1);
            tableName = createIndexMatcher.group(2);
            attributeNames = createIndexMatcher.group(3);
        }
        else {
            throw new DbExceptions.DataManipulationException("Incorrect create index syntax.");
        }

        Table currentTable = usedDatabase.getTables().stream()
                .filter(o -> o.getName().equals(tableName))
                .findFirst()
                .orElse(null);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't create index on non-existent table: " + tableName);

        Attribute currentAttribute = currentTable.getAttributes().stream()
                .filter(o -> o.getName().equals(attributeNames))
                .findFirst()
                .orElse(null);
        if (currentAttribute == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't create index on non-existent attribute: " + attributeNames);

        if (indexName == null)
            indexName = tableName + "_" + attributeNames + "Index";

        currentAttribute.setIndex(indexName);
    }

    private void deleteDatabase(String databaseName) throws DbExceptions.DataDefinitionException {
        //DELETE DATABASE <databasename>
        Database database = databases.stream()
                .filter(o -> o.getName().equals(databaseName))
                .findAny().orElse(null);
        if (database == null) {
            throw new DbExceptions.DataDefinitionException("Database does not exist: " + databaseName);
        }
        usedDatabase = null;
        mongoDBManager.deleteDatabase(databaseName);
        databases.remove(database);
    }
    private void deleteTable(String tableName) throws DbExceptions.DataDefinitionException {
        //DELETE TABLE <tablename>
        Table table = usedDatabase.getTable(tableName);
        if(table == null){
            throw new DbExceptions.DataDefinitionException("Table does not exist: " + tableName);
        }
        for (Attribute attribute : table.getAttributes()) {
            if (!attribute.getIndex().equals(""))
                mongoDBManager.deleteTable(attribute.getIndex());
        }
        mongoDBManager.deleteTable(tableName);
        usedDatabase.removeTable(table);
    }

    private void insert(String command) throws DbExceptions.DataManipulationException, DbExceptions.DataDefinitionException {
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
        
        Table currentTable = usedDatabase.getTable(tableName);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't insert into non-existent table: " + tableName);

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
                } else {
                    Matcher varcharTypeMatcher = Pattern
                            .compile("VARCHAR\\(([0-9]+)\\)", Pattern.CASE_INSENSITIVE)
                            .matcher(attribute.getDataType());
                    if (!varcharTypeMatcher.find()) {
                        throw new DbExceptions.DataManipulationException(
                                "Attribute " + attribute.getName() +
                                        " has unknown datatype:" + attribute.getDataType()
                        );
                    }
                    Matcher varcharValueMatcher = Pattern.compile("^'([^']+)'|\"([^\"]+)\"$").matcher(val);
                    if (!varcharValueMatcher.find()) {
                        throw new DbExceptions.DataManipulationException(
                                "Varchar variable was given incorrectly: " + val
                        );
                    }
                    int maxLen = Integer.parseInt(varcharTypeMatcher.group(1));
                    String varchar = varcharValueMatcher.group(1);
                    if (varchar == null)
                        varchar = varcharValueMatcher.group(2);
                    if (varchar.length() - 2 > maxLen)
                        throw new DbExceptions.DataManipulationException("The string is too long.");
                    val = varchar;
                }

                // fk check

                if(attribute.isFk()) {
                    Table refTable = usedDatabase.getTable(attribute.getRefTable());
                    if (refTable == null)
                        throw new DbExceptions.DataDefinitionException
                                ("Non-existent table referenced: " + attribute.getRefTable());
                    Iterator<Attribute> attributeIterator = refTable.getAttributes().iterator();
                    int pkIndex = 0, valueIndex = 0;
                    Attribute refAttr = null;
                    while (attributeIterator.hasNext() &&
                            !(refAttr = attributeIterator.next()).getName().equals(attribute.getRefColumn())
                    ) {
                        if (refAttr.isPk())
                            pkIndex++;
                        else
                            valueIndex++;
                    }
                    if (refAttr == null)
                        throw new DbExceptions.DataDefinitionException
                                ("Non-existent attribute referenced: " + attribute.getRefColumn());
                    if (mongoDBManager.isUnique(
                            refTable.getName(), val,
                            refAttr.isPk() ? "_id" : "value",
                            refAttr.isPk() ? pkIndex : valueIndex
                    ))
                        throw new DbExceptions.DataManipulationException
                                ("Referencing table does not contains value: " + val);
                }

                // unique check
                if(attribute.isUnique()){
                    if(!mongoDBManager.isUnique(
                            currentTable.getName(), val,
                            attribute.isPk() ? "_id" : "value",
                            attribute.isPk() ? keysToInsert.size() : valuesToInsert.size())
                    )
                        throw new DbExceptions.DataManipulationException("This field must be unique.");
                }

                // not null check
                if(attribute.isNotNull() && val.equalsIgnoreCase("null"))
                    throw new DbExceptions.DataManipulationException("This field must not be null.");

                // pk check
                if (attribute.isPk())
                    keysToInsert.add(val);
                else
                    valuesToInsert.add(val);

                // index
                if (!attribute.getIndex().equals(""))
                {
                    String key = currentTable.getAttributes().stream()
                            .filter(Attribute::isPk)
                            .mapToInt(pk -> intoColumn.indexOf(pk.getName()))
                            .mapToObj(value::get)
                            .collect(Collectors.joining("#"));
                    if (attribute.isUnique())
                        mongoDBManager.insert(attribute.getIndex(), val, key);
                    else {
                        mongoDBManager.insertNotUniqueIndex(attribute.getIndex(), val, key);
                    }
                }
            }
        }

        mongoDBManager.insert(
                tableName,
                String.join("#", keysToInsert),
                String.join("#", valuesToInsert)
        );
    }
    private void delete(String command) throws DbExceptions.DataManipulationException {
        Pattern insertPattern = Pattern.compile(
                "^DELETE FROM ([a-zA-Z0-9_]+) WHERE (.*)$",
                Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(command);
        String tableName;
        String conditions;
        if(insertMatcher.find()) {
            tableName = insertMatcher.group(1);
            conditions = insertMatcher.group(2);
        }
        else{
            throw new DbExceptions.DataManipulationException("Incorrect delete syntax.");
        }

        Table currentTable = usedDatabase.getTable(tableName);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't delete from non-existent table: " + tableName);

        // indexes of attributes in key/value string

        HashMap< String, Attribute > attributeByName = new HashMap<>();
        HashMap< Attribute, Integer > indexOf = new HashMap<>();
        int keyIndex = 0, valueIndex = 0;
        for (Attribute attribute : currentTable.getAttributes()) {
            attributeByName.put(attribute.getName(), attribute);
            if (attribute.isPk())
                indexOf.put(attribute, keyIndex++);
            else
                indexOf.put(attribute, valueIndex++);
        }

        // parse conditions
        HashMap< Filter, Attribute > filterHashMap = new HashMap<>();
        ArrayList< ArrayList<Filter> > orFilters = new ArrayList<>();
        Pattern condPattern = Pattern.compile("^(.+)([<>=!]+)[\"']?([^\"']+)[\"']?$");

        for (String andCond : conditions.split("(?i) OR ")) {
            ArrayList<Filter> andFilters = new ArrayList<>();
            for (String cond : andCond.split("(?i) AND ")) {
                Matcher condMatcher = condPattern.matcher(cond);
                String attributeName, operator, rightOperand;
                if (condMatcher.find()) {
                    attributeName = condMatcher.group(1);
                    operator = condMatcher.group(2);
                    rightOperand = condMatcher.group(3);
                } else {
                    throw new DbExceptions.DataManipulationException("Incorrect where condition: " + cond);
                }
                Filter filter = new Filter(operator, rightOperand);
                andFilters.add(filter);
                filterHashMap.put(filter, attributeByName.get(attributeName));
            }
            orFilters.add(andFilters);
        }

        FindIterable<Document> documents = mongoDBManager.getAllDocuments(tableName);

        ArrayList<String> keysToDelete = new ArrayList<>();
        HashMap< String, HashSet<String> > keysToDeleteIndex = new HashMap<>();
        currentTable.getAttributes().stream()
                .map(Attribute::getIndex)
                .filter(index -> !index.equals(""))
                .forEach(index -> keysToDeleteIndex.put(index, new HashSet<>()));

        for (Document document : documents) {
            String[] keys = document.getString("_id").split("#");
            String[] values = document.getString("value").split("#");

            if (orFilters.stream().map(
                    andFilters -> andFilters.stream().map(filter -> {
                        Attribute filterOn = filterHashMap.get(filter);
                        if (filterOn.isPk())
                            return filter.eval(keys[indexOf.get(filterOn)]);
                        else
                            return filter.eval(values[indexOf.get(filterOn)]); })
                            .reduce(Boolean::logicalAnd).orElse(true))
                    .reduce(Boolean::logicalOr).orElse(true)
            ) {
                keysToDelete.add(document.getString("_id"));
                currentTable.getAttributes().stream()
                        .filter(attribute -> !attribute.isPk())
                        .forEach(attribute -> {
                            if (!attribute.getIndex().equals("")) {
                                keysToDeleteIndex.get(attribute.getIndex())
                                        .add(values[indexOf.get(attribute)]);
                            }
                        });
            }
        }

        mongoDBManager.delete(tableName, keysToDelete);
    }

    private void select(String command) throws DbExceptions.DataManipulationException {
        Pattern selectPattern = Pattern.compile(
                "^SELECT (\\*|[a-zA-Z0-9_]+) FROM ([a-zA-Z0-9_]+) (?:WHERE (.+))?$",
                Pattern.CASE_INSENSITIVE);
        Matcher selectMatcher = selectPattern.matcher(command);

        String projection, fromTable, conditions;
        if (selectMatcher.find()) {
            projection = selectMatcher.group(1);
            fromTable = selectMatcher.group(2);
            conditions = selectMatcher.group(3);
        } else {
            throw new DbExceptions.DataManipulationException("Incorrect SELECT syntax");
        }
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
