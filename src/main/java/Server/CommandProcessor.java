package Server;

import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;

import static Server.Parser.Create.*;
import static Server.Parser.Delete.deleteDatabase;
import static Server.Parser.Delete.deleteTable;
import static Server.Parser.DeleteData.delete;
import static Server.Parser.InsertData.insert;
import static Server.Parser.Select.Select.select;

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
                        return createDatabase(command, databases);
                    case "TABLE":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return createTable(command, databases, usedDatabase);
                    case "INDEX":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return createIndex(command, databases, usedDatabase);
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
            case "DELETE":
                switch (dividedCommand[1].toUpperCase()) {
                    case "DATABASE":
                        return deleteDatabase(dividedCommand[2], databases, usedDatabase, mongoDBManager);
                    case "TABLE":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return deleteTable(dividedCommand[2], databases, usedDatabase, mongoDBManager);
                    case "FROM":
                        if ( usedDatabase == null)
                            throw new DbExceptions.DataDefinitionException("Unspecified database");
                        return delete(command, usedDatabase, mongoDBManager);
                    default:
                        throw new DbExceptions.UnknownCommandException();
                }
            case "INSERT":
                if ( usedDatabase == null)
                    throw new DbExceptions.DataDefinitionException("Unspecified database");
                return insert(command, usedDatabase, mongoDBManager);
            case "USE":
                return use(dividedCommand[1]);
            case "SELECT":
                if ( usedDatabase == null)
                    throw new DbExceptions.DataDefinitionException("Unspecified database");
                return select(command, usedDatabase, mongoDBManager);
            default:
                throw new DbExceptions.UnknownCommandException();
        }
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
}
