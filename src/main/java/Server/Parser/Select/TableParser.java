package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TableParser {
    private final static Pattern tableAliasPattern = Pattern.compile(
            "^([a-zA-Z0-9_]+)(?: (?:AS )?([a-zA-Z0-9_]+))?$",
            Pattern.CASE_INSENSITIVE);
    private final static Pattern attributePattern = Pattern.compile(
            "^(?:([a-zA-Z0-9_]+)\\.)?([a-zA-Z0-9_]+)$");

    protected static Table tableParser(String tableName,
                                   Database usedDatabase,
                                   HashMap<String, Table> tableAS,
                                   ArrayList<Table> selectedTables,
                                   HashMap<Table, HashMap< String, Attribute> > attributeByNameInTable,
                                   HashMap< Attribute, Integer > indexOfAttribute)
            throws DbExceptions.DataManipulationException {
        Matcher tableMatcher = tableAliasPattern.matcher(tableName);
        if (!tableMatcher.find())
            throw new DbExceptions.DataManipulationException("Incorrect SELECT syntax");
        Table table = usedDatabase.getTable(tableMatcher.group(1));
        if(table == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't select from non-existent table: " + tableMatcher.group(1));
        tableAS.put(tableMatcher.group(1), table);
        if (tableMatcher.group(2) != null)
            tableAS.put(tableMatcher.group(2), table);
        selectedTables.add(table);

        indexAttributesInTable(table, attributeByNameInTable, indexOfAttribute);

        return table;
    }

    protected static Pair<Table, Attribute> attributeParser(String tableAttribute,
                                               ArrayList<Table> selectedTables,
                                               HashMap<String, Table> tableAS)
            throws DbExceptions.DataManipulationException {
        Matcher tableAttributeMatcher = attributePattern.matcher(tableAttribute);
        if (!tableAttributeMatcher.find()) {
            throw new DbExceptions.DataManipulationException("Incorrect attribute syntax:" + tableAttribute);
        }
        String tableName = tableAttributeMatcher.group(1);
        String attributeName = tableAttributeMatcher.group(2);
        if (tableName == null) {
            tableName = getTableOfAttribute(selectedTables, attributeName);
            if (tableName == null)
                throw new DbExceptions.DataManipulationException(
                        "Could not determine to which table attribute belongs: " + attributeName);
        }

        Table table1 = tableAS.get(tableName);
        if (table1 == null)
            throw new DbExceptions.DataManipulationException("Table not exists: " + tableName);
        return new Pair<>(table1, table1.getAttribute(attributeName));
    }

    protected static Pair<Attribute, Attribute> joinParser(Matcher joinedTableMatcher,
                                                           ArrayList<Table> selectedTables,
                                                           HashMap<String, Table> tableAS)
            throws DbExceptions.DataManipulationException {
        String tableAttribute1, tableAttribute2;
        tableAttribute1 = joinedTableMatcher.group(2);
        tableAttribute2 = joinedTableMatcher.group(3);

        Pair<Table, Attribute> tableAttributePair1 = attributeParser(tableAttribute1, selectedTables, tableAS);
        Pair<Table, Attribute> tableAttributePair2 = attributeParser(tableAttribute2, selectedTables, tableAS);

        Attribute attribute1 = tableAttributePair1.getValue();
        Attribute attribute2 = tableAttributePair2.getValue();
        Table table1 = tableAttributePair1.getKey();
        Table table2 = tableAttributePair2.getKey();
        if (!attribute1.isFk() && !attribute2.isFk())
            throw new DbExceptions.DataManipulationException("Could not join on non foreign key attributes");

        if (selectedTables.indexOf(table1) > selectedTables.indexOf(table2)) {
            Attribute temp = attribute1;
            attribute1 = attribute2;
            attribute2 = temp;
        }

        return new Pair<>(attribute1, attribute2);
    }

    protected static String getTableOfAttribute(List<Table> selectedTables, String attributeName) {
        List<Table> matchingTables = selectedTables.stream()
                .map(table -> {
                    int matchingAttributesCount = (int) table.getAttributes().stream()
                            .map(Attribute::getName)
                            .filter(name -> name.equalsIgnoreCase(attributeName))
                            .count();
                    if (matchingAttributesCount < 1)
                        return null;
                    return table;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (matchingTables.size() > 1)
            return null;
        return matchingTables.get(0).getName();
    }

    protected static void indexAttributesInTable(Table table,
                                                 HashMap < Table, HashMap< String, Attribute >> attributeByNameInTable,
                                                 HashMap< Attribute, Integer > indexInDocument) {
        HashMap< String, Attribute > attributeByName = new HashMap<>();
        int pkCount = (int) table.getAttributes().stream().filter(Attribute::isPk).count();
        int pkIndex = 0, valueIndex = 0;
        for (Attribute attribute : table.getAttributes()) {
            attributeByName.put(attribute.getName(), attribute);
            if (attribute.isPk())
                indexInDocument.put(attribute, pkIndex++);
            else
                indexInDocument.put(attribute, pkCount + valueIndex++);
        }

        attributeByNameInTable.put(table, attributeByName);
    }
}
