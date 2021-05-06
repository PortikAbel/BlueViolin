package Server;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
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

    public String processCommand(String command)
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
                        return createDatabase(command);
                    case "TABLE":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return createTable(command);
                    case "INDEX":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return createIndex(command);
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
            case "DELETE":
                switch (dividedCommand[1].toUpperCase()) {
                    case "DATABASE":
                        return deleteDatabase(dividedCommand[2]);
                    case "TABLE":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return deleteTable(dividedCommand[2]);
                    case "FROM":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return delete(command);
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
            case "INSERT":
                if ( usedDatabase == null)
                    throw new DbExceptions.DataDefinitionException("Unspecified database");
                return insert(command);
            case "USE":
                return use(dividedCommand[1]);
            case "SELECT":
                if ( usedDatabase == null)
                    throw new DbExceptions.DataDefinitionException("Unspecified database");
                return select(command);
            default:
                throw new DbExceptions.UnknownCommandException();
        }
    }

    private String createDatabase(String command) throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
        //CREATE DATABASE <databasename>
        String[] dividedCommand = command.split(" ");
        if (databases.stream().noneMatch(o -> o.getName().equals(dividedCommand[2]))) {
            databases.add(new Database(dividedCommand[2]));
        } else {
            throw (new DbExceptions.DataDefinitionException("database already exists: " + dividedCommand[2]));
        }
        Json.saveDatabases(databases);
        return "database " + dividedCommand[2] + " created successfully";
    }
    private String createTable(String command) throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
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
            Json.saveDatabases(databases);
            return "table " + tableName + " created successfully";
        } else {
            throw new DbExceptions.DataDefinitionException("Incorrect create table syntax");
        }
    }
    private String createIndex(String command) throws DbExceptions.DataManipulationException, IOException, DbExceptions.UnsuccessfulDeleteException {
        Pattern createIndexPattern = Pattern.compile(
                "^CREATE INDEX ([a-zA-Z0-9_]+)? ON ([a-zA-Z0-9_]+)\\(([^()]+)\\)$",
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

        Json.saveDatabases(databases);
        return "index " + indexName + " created successfully";
    }

    private String deleteDatabase(String databaseName) throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
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

        Json.saveDatabases(databases);
        return "database " + databaseName + " deleted successfully";
    }
    private String deleteTable(String tableName) throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
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

        Json.saveDatabases(databases);
        return "table " + tableName + " deleted successfully";
    }

    private String insert(String command) throws DbExceptions.DataManipulationException, DbExceptions.DataDefinitionException {
        Pattern insertPattern = Pattern.compile(
                "INSERT INTO ([a-zA-Z0-9_]+)[ ]?(\\(([^()]+)\\))?VALUES\\(([^()]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(command);
        String tableName, intoCols, valueList;
        if(insertMatcher.find()) {
            tableName = insertMatcher.group(1);
            intoCols = insertMatcher.group(3);
            valueList = insertMatcher.group(4);
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

        List<String> colNames, values;
        colNames = Arrays.asList(intoCols.split(","));
        values = Arrays.asList(valueList.split(","));

        List<String> valuesToInsert = new ArrayList<>();
        List<String> keysToInsert = new ArrayList<>();

        int pkCount = (int) currentTable.getAttributes().stream().filter(Attribute::isPk).count();
        boolean intPK = (pkCount == 1) &&
                currentTable.getAttributes().stream()
                        .filter(Attribute::isPk)
                        .map(Attribute::getDataType)
                        .map(dt -> dt.equalsIgnoreCase("int"))
                        .reduce(true, Boolean::logicalAnd);

        for (Attribute attribute : currentTable.getAttributes())
        {
            int i = colNames.indexOf(attribute.getName());
            if (i < 0) {
                if (attribute.isNotNull())
                    throw new DbExceptions.DataManipulationException("This field must not be null.");
                valuesToInsert.add("null");
            }
            else {
                String val = values.get(i);
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
                    int refTablePkCount = (int) refTable.getAttributes().stream().filter(Attribute::isPk).count();
                    if (refTablePkCount == 1 && refAttr.isPk() &&
                            refAttr.getDataType().equalsIgnoreCase("int")) {
                        if (mongoDBManager.isUniqueKey(refTable.getName(), val))
                            throw new DbExceptions.DataManipulationException
                                    ("Referencing table does not contains value: " + val);
                    } else {
                        if (mongoDBManager.isUniqueValue(
                                refTable.getName(), val,
                                refAttr.isPk() ? "_id" : "value",
                                refAttr.isPk() ? pkIndex : valueIndex
                        ))
                            throw new DbExceptions.DataManipulationException
                                    ("Referencing table does not contains value: " + val);
                    }
                }

                // unique check
                if(attribute.isUnique()){
                    if (intPK && attribute.isPk()) {
                        if (!mongoDBManager.isUniqueKey(currentTable.getName(), val))
                            throw new DbExceptions.DataManipulationException("This field must be unique.");
                    } else {
                        if (!mongoDBManager.isUniqueValue(
                                currentTable.getName(), val,
                                attribute.isPk() ? "_id" : "value",
                                attribute.isPk() ? keysToInsert.size() : valuesToInsert.size())
                        )
                            throw new DbExceptions.DataManipulationException("This field must be unique.");
                    }
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

        String key = String.join("#", keysToInsert);
        String value = String.join("#", valuesToInsert);

        if (intPK)
            mongoDBManager.insertIntKey(tableName, Integer.parseInt(key), value);
        else
            mongoDBManager.insert(tableName, key, value);

        HashMap<String, Attribute> attributeHashMap = new HashMap<>();
        currentTable.getAttributes()
                .forEach(attribute -> attributeHashMap.put(attribute.getName(), attribute));

        // indexing
        for (int i = 0; i < colNames.size(); i++) {
            Attribute attribute = attributeHashMap.get(colNames.get(i));

            if (!attribute.getIndex().equals(""))
            {
                if ("int".equalsIgnoreCase(attribute.getDataType())) {
                    Integer val = Integer.parseInt(values.get(i));
                    if (attribute.isUnique())
                        mongoDBManager.insertIntKey(attribute.getIndex(), val, key);
                    else {
                        mongoDBManager.insertNotUniqueIndexIntKey(attribute.getIndex(), val, key);
                    }
                } else {
                    String val = values.get(i);
                    val = val.substring(1, val.length()-1);
                    if (attribute.isUnique())
                        mongoDBManager.insert(attribute.getIndex(), val, key);
                    else {
                        mongoDBManager.insertNotUniqueIndex(attribute.getIndex(), val, key);
                    }
                }
            }
        }

        return "1 row inserted successfully";
    }
    private String delete(String command) throws DbExceptions.DataManipulationException {
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

        ArrayList<Object> keysToDelete = new ArrayList<>();
        HashMap< String, HashSet<Object> > keysToDeleteFromIndex = new HashMap<>();
        HashMap< String, HashMap<Object, ArrayList<String>> > pksToDeleteAtKeyNotUniqueIndex = new HashMap<>();
        HashMap< String, Boolean > indexOnInteger = new HashMap<>();
        currentTable.getAttributes().stream()
                .filter(attribute -> !attribute.getIndex().equals(""))
                .forEach(attribute -> {
                    String indexName = attribute.getIndex();
                    indexOnInteger.put(
                            indexName,
                            "int".equalsIgnoreCase(attribute.getDataType())
                    );
                    if (attribute.isUnique())
                        keysToDeleteFromIndex.put(indexName, new HashSet<>());
                    else
                        pksToDeleteAtKeyNotUniqueIndex.put(indexName, new HashMap<>());
                });

        List<Attribute> pks = currentTable.getAttributes().stream()
                .filter(Attribute::isPk)
                .collect(Collectors.toList());
        boolean oneIntPK = pks.size() == 1 && "int".equalsIgnoreCase(pks.get(0).getDataType());

        FindIterable<Document> documents = mongoDBManager.findAll(tableName);
        for (Document document : documents) {
            Object key = document.get("_id");
            String[] values = document.getString("value").split("#");

            if (orFilters.stream().map(
                    andFilters -> andFilters.stream().map(filter -> {
                        Attribute filterOn = filterHashMap.get(filter);
                        if (filterOn.isPk())
                            if (oneIntPK)
                                return filter.eval((Integer) key);
                            else
                                return filter.eval(
                                        (String.valueOf(key)).split("#")[indexOf.get(filterOn)]
                                );
                        else
                            return filter.eval(values[indexOf.get(filterOn)]);
                    })
                            .reduce(Boolean::logicalAnd).orElse(true))
                    .reduce(Boolean::logicalOr).orElse(true)
            ) {
                keysToDelete.add(key);
                currentTable.getAttributes().stream()
                        .filter(attribute -> !attribute.isPk())
                        .forEach(attribute -> {
                            String indexName = attribute.getIndex();
                            if (!indexName.equals("")) {
                                if (attribute.isUnique()) {
                                    if (indexOnInteger.get(indexName))
                                        keysToDeleteFromIndex.get(indexName)
                                                .add(Integer.parseInt(values[indexOf.get(attribute)]));
                                    else
                                        keysToDeleteFromIndex.get(indexName)
                                                .add(values[indexOf.get(attribute)]);
                                } else {
                                    String notUniqueValue = values[indexOf.get(attribute)];
                                    ArrayList<String> pksToDelete;
                                    if (indexOnInteger.get(indexName)) {
                                        pksToDelete = pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                .get(Integer.parseInt(notUniqueValue));
                                        if (pksToDelete == null) {
                                            pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .put(Integer.parseInt(notUniqueValue), new ArrayList<>());
                                            pksToDelete = pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .get(Integer.parseInt(notUniqueValue));
                                        }
                                    }
                                    else {
                                        pksToDelete = pksToDeleteAtKeyNotUniqueIndex
                                                .get(indexName)
                                                .get(notUniqueValue);
                                        if (pksToDelete == null) {
                                            pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .put(notUniqueValue, new ArrayList<>());
                                            pksToDelete = pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .get(notUniqueValue);
                                        }
                                    }
                                    pksToDelete.add(String.valueOf(key));
                                }
                            }
                        });
            }
        }

        mongoDBManager.delete(tableName, keysToDelete);
        keysToDeleteFromIndex.forEach(mongoDBManager::delete);
        pksToDeleteAtKeyNotUniqueIndex.forEach(mongoDBManager::deleteFromNotUniqueIndex);

        return keysToDelete.size() + " rows deleted successfully";
    }

    private String select(String command) throws DbExceptions.DataManipulationException {
        Pattern selectPattern = Pattern.compile(
                "^SELECT (\\*|[a-zA-Z0-9_,]+) FROM ([a-zA-Z0-9_]+)(?: WHERE (.+))?$",
                Pattern.CASE_INSENSITIVE);
        Matcher selectMatcher = selectPattern.matcher(command);

        String projection, tableName, conditions;
        if (selectMatcher.find()) {
            projection = selectMatcher.group(1);
            tableName = selectMatcher.group(2);
            conditions = selectMatcher.group(3);
        } else {
            throw new DbExceptions.DataManipulationException("Incorrect SELECT syntax");
        }

        Table currentTable = usedDatabase.getTable(tableName);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't delete from non-existent table: " + tableName);

        // indexes of attributes in key/value string

        HashMap< String, Attribute > attributeByName = new HashMap<>();
        HashMap< Attribute, Integer > indexInDocument = new HashMap<>();
        int pkCount = (int) currentTable.getAttributes().stream().filter(Attribute::isPk).count();
        int pkIndex = 0, valueIndex = 0;
        for (Attribute attribute : currentTable.getAttributes()) {
            attributeByName.put(attribute.getName(), attribute);
            if (attribute.isPk())
                indexInDocument.put(attribute, pkIndex++);
            else
                indexInDocument.put(attribute, pkCount + valueIndex++);
        }

        // parse conditions
        HashMap< Attribute, ArrayList<Filter> > filtersOnAttribute = new HashMap<>();
        currentTable.getAttributes().forEach(attribute -> filtersOnAttribute.put(attribute, new ArrayList<>()));

        if (conditions != null) {
            Pattern condPattern = Pattern.compile("^([^<>=!]+)([<>=!]+)[\"']?([^\"'<>=!]+)[\"']?$");

            for (String cond : conditions.split("(?i) AND ")) {
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
                filtersOnAttribute.get(attributeByName.get(attributeName)).add(filter);
            }
        }

        // find primary keys from indexes
        Stack<List<String>> pkListsToIntersect = new Stack<>();
        filtersOnAttribute.forEach((attribute, filters) -> {
            if (!attribute.getIndex().equals("") && filters.size() > 0) {
                List<Bson> bsonList;
                if (attribute.getDataType().equalsIgnoreCase("int"))
                    bsonList = filters.stream().map(Filter::asMongoFilterOnIntID).collect(Collectors.toList());
                else
                    bsonList = filters.stream().map(Filter::asMongoFilterOnID).collect(Collectors.toList());

                FindIterable<Document> documents =
                        mongoDBManager.findFiltered(attribute.getIndex(), Filters.and(bsonList));
                List<String> filteredPKs = new ArrayList<>();
                for (Document document : documents) {
                    String pks = document.getString("value");
                    if (attribute.isUnique()) {
                        filteredPKs.add(pks);
                    } else {
                        filteredPKs.addAll(Arrays.asList(pks.split("##")));
                    }
                }
                pkListsToIntersect.push(filteredPKs);
            }
        });

        // construct a filter on PK
        Bson filter = null;
        Attribute pk = null;
        if (pkCount == 1) {    // single PK
            pk = currentTable.getAttributes().stream()
                    .filter(Attribute::isPk)
                    .findFirst()
                    .orElse(null);
        }
        if (pk != null && filtersOnAttribute.get(pk).size() > 0) {
            if (pk.getDataType().equalsIgnoreCase("int")) {
                filter = Filters.and(filtersOnAttribute.get(pk).stream()
                                .map(Filter::asMongoFilterOnIntID)
                                .collect(Collectors.toList())
                );
            } else {
                filter = Filters.and(filtersOnAttribute.get(pk).stream()
                                .map(Filter::asMongoFilterOnID)
                                .collect(Collectors.toList())
                );
            }
        }

        if (!pkListsToIntersect.empty()) {

            // intersection of primary key lists
            List<String> filteredPKs = pkListsToIntersect.pop();
            while (!pkListsToIntersect.empty()) {
                filteredPKs = pkListsToIntersect.pop().stream()
                        .filter(filteredPKs::contains)
                        .collect(Collectors.toList());
            }

            if (pk != null) {
                if (pk.getDataType().equalsIgnoreCase("int")) {
                    if (filter != null)
                        filter = Filters.and(
                                Filters.in("_id", filteredPKs.stream()
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList())),
                                filter
                        );
                    else
                        filter = Filters.in("_id", filteredPKs.stream()
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList()));
                } else {
                    if (filter != null)
                        filter = Filters.and(
                                Filters.in("_id", filteredPKs),
                                filter
                        );
                    else
                        filter = Filters.in("_id", filteredPKs);
                }
            } else {
                filter = Filters.in("_id", filteredPKs);
            }
        }

        // find documents from main collection filtered by indexes
        FindIterable<Document> documents = mongoDBManager.findFiltered(currentTable.getName(), filter);
        List<List<String>> result = new ArrayList<>();
        for (Document document : documents) {
            List<String> row = new ArrayList<>();
            row.addAll(Arrays.asList(document.get("_id").toString().split("#")));
            row.addAll(Arrays.asList(document.getString("value").split("#")));

            List<Boolean> passed = new ArrayList<>();
            filtersOnAttribute.forEach((attribute, filters) -> {
                if (attribute.getIndex().equals("") && !attribute.isPk()) {
                    passed.add(
                            filters.stream()
                                    .map(fil -> fil.eval(row.get(indexInDocument.get(attribute))))
                                    .reduce(Boolean::logicalAnd).orElse(true)
                    );
                }
            });
            if (passed.stream().reduce(Boolean::logicalAnd).orElse(true))
                result.add(row);
        }

        // projection
        if (!projection.equals("*")) {
            List<Integer> selectedIndexes = Arrays.stream(projection.split(","))
                    .map(attributeByName::get)
                    .map(indexInDocument::get)
                    .collect(Collectors.toList());
            result = result.stream()
                    .map(row -> selectedIndexes.stream()
                            .map(row::get)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
        } else {
            projection = currentTable.getAttributes().stream()
                    .map(Attribute::getName)
                    .collect(Collectors.joining(","));
        }

        return toHTMLTable(Arrays.asList(projection.split(",")), result);
    }

    private String use(String databaseName) throws DbExceptions.DataDefinitionException {
        usedDatabase = databases.stream()
                .filter(o -> o.getName().equals(databaseName))
                .findAny().orElse(null);
        if(usedDatabase == null){
            throw (new DbExceptions.DataDefinitionException("Database does not exist: " + databaseName));
        }
        mongoDBManager.use(usedDatabase.getName());

        return "Using database " + databaseName;
    }

    private String toHTMLTable(List<String> header, List<List<String>> data) {
        StringBuilder table = new StringBuilder();
        table.append("<table>");
        table.append("<tr>");
        header.forEach(th -> {
            table.append("<th>");
            table.append(th);
            table.append("</th>");
        });
        table.append("</tr>");
        data.forEach(tr -> {
            table.append("<tr>");
            tr.forEach(td -> {
                table.append("<td>");
                table.append(td);
                table.append("</td>");
            });
            table.append("</tr>");
        });
        table.append("</table>");

        return table.toString();
    }
}
