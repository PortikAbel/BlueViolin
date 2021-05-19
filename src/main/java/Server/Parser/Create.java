package Server.Parser;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.Json;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Create {
    public static String createDatabase(String command, List<Database> databases) throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
        //CREATE DATABASE <databaseName>
        String[] dividedCommand = command.split(" ");
        if (databases.stream().noneMatch(o -> o.getName().equals(dividedCommand[2]))) {
            databases.add(new Database(dividedCommand[2]));
        } else {
            throw (new DbExceptions.DataDefinitionException("database already exists: " + dividedCommand[2]));
        }
        Json.saveDatabases(databases);
        return "database " + dividedCommand[2] + " created successfully";
    }
    public static String createTable(String command, List<Database> databases, Database usedDatabase) throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
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
    public static String createIndex(String command, List<Database> databases, Database usedDatabase) throws DbExceptions.DataManipulationException, IOException, DbExceptions.UnsuccessfulDeleteException {
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
}
