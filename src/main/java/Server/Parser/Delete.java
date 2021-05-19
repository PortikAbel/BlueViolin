package Server.Parser;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.Json;
import Server.MongoDBManager;

import java.io.IOException;
import java.util.List;

public class Delete {
    public static String deleteDatabase(String databaseName, List<Database> databases, Database usedDatabase, MongoDBManager mongoDBManager)
            throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
        //DELETE DATABASE <databaseName>
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
    public static String deleteTable(String tableName, List<Database> databases, Database usedDatabase, MongoDBManager mongoDBManager)
            throws DbExceptions.DataDefinitionException, IOException, DbExceptions.UnsuccessfulDeleteException {
        //DELETE TABLE <tableName>
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
}
