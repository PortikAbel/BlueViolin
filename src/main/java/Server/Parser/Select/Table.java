package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import static Server.Parser.Select.AttributeFinder.indexAttributesInTable;

public class Table {
    public static Server.DbStructure.Table tableParser(Matcher tableMather,
                                   Database usedDatabase,
                                   HashMap<String, Server.DbStructure.Table> tableAS,
                                   ArrayList<Server.DbStructure.Table> selectedTables,
                                   HashMap<Server.DbStructure.Table, HashMap< String, Attribute> > attributeByNameInTable,
                                   HashMap< Attribute, Integer > indexOfAttribute)
            throws DbExceptions.DataManipulationException
    {
        if (!tableMather.find())
            throw new DbExceptions.DataManipulationException("Incorrect SELECT syntax");
        Server.DbStructure.Table table = usedDatabase.getTable(tableMather.group(1));
        if(table == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't select from non-existent table: " + tableMather.group(1));
        tableAS.put(tableMather.group(1), table);
        if (tableMather.group(2) != null)
            tableAS.put(tableMather.group(2), table);
        selectedTables.add(table);

        indexAttributesInTable(table, attributeByNameInTable, indexOfAttribute);

        return table;
    }
}
