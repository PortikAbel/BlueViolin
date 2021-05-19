package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import static Server.Parser.Select.AttributeFinder.getTableOfAttribute;
import static Server.Parser.Select.AttributeFinder.indexAttributesInTable;

public class TableParser {
    public static Table tableParser(Matcher tableMather,
                                   Database usedDatabase,
                                   HashMap<String, Table> tableAS,
                                   ArrayList<Table> selectedTables,
                                   HashMap<Table, HashMap< String, Attribute> > attributeByNameInTable,
                                   HashMap< Attribute, Integer > indexOfAttribute)
            throws DbExceptions.DataManipulationException
    {
        if (!tableMather.find())
            throw new DbExceptions.DataManipulationException("Incorrect SELECT syntax");
        Table table = usedDatabase.getTable(tableMather.group(1));
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

    public static Pair<Attribute, Attribute> joinParser(Matcher joinedTableMatcher,
                                                        ArrayList<Table> selectedTables,
                                                        HashMap<String, Table> tableAS)
            throws DbExceptions.DataManipulationException
    {
        String tableName1, attributeName1, tableName2, attributeName2;
        tableName1 = joinedTableMatcher.group(2);
        attributeName1 = joinedTableMatcher.group(3);
        tableName2 = joinedTableMatcher.group(4);
        attributeName2 = joinedTableMatcher.group(5);

        if (tableName1 == null) {
            tableName1 = getTableOfAttribute(selectedTables, attributeName1);
            if (tableName1 == null)
                throw new DbExceptions.DataManipulationException(
                        "Could not determine to which table attribute belongs: " + attributeName1);
        }
        if (tableName2 == null) {
            tableName2 = getTableOfAttribute(selectedTables, attributeName2);
            if (tableName2 == null)
                throw new DbExceptions.DataManipulationException(
                        "Could not determine to which table attribute belongs: " + attributeName2);
        }

        Table table1 = tableAS.get(tableName1);
        if (table1 == null)
            throw new DbExceptions.DataManipulationException("Table not exists: " + tableName1);
        Table table2 = tableAS.get(tableName2);
        if (table2 == null)
            throw new DbExceptions.DataManipulationException("Table not exists: " + tableName2);
        Attribute attribute1 = table1.getAttribute(attributeName1);
        if (attribute1 == null)
            throw new DbExceptions.DataManipulationException("Attribute not exists: " + attributeName1);
        Attribute attribute2 = table2.getAttribute(attributeName2);
        if (attribute2 == null)
            throw new DbExceptions.DataManipulationException("Attribute not exists: " + attributeName2);
        if (!attribute1.isFk() && !attribute2.isFk())
            throw new DbExceptions.DataManipulationException("Could not join on non foreign key attributes");

        return new Pair<>(attribute1, attribute2);
    }
}
