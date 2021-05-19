package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.Table;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static Server.Parser.Select.AttributeFinder.getTableOfAttribute;

public class Projection {
    protected static List<List<String>> projectionOnResult(
            String projection,
            List<List<String>> result,
            ArrayList<Table> selectedTables,
            Database usedDatabase,
            HashMap<Attribute, Integer > indexOfAttribute)
    {
        Pattern taPattern = Pattern.compile(
                "(?:([a-zA-Z0-9_]+)\\.)?([a-zA-Z0-9_]+)",
                Pattern.CASE_INSENSITIVE);
        List<Integer> selectedIndexes = Arrays.stream(projection.split(","))
                .map(ta -> {
                    Matcher taMatcher = taPattern.matcher(ta);
                    if (!taMatcher.find())
                        return null;
                    String tableName = taMatcher.group(1);
                    String attributeName = taMatcher.group(2);
                    if (tableName == null) {
                        tableName = getTableOfAttribute(selectedTables, attributeName);
                        if (tableName == null)
                            return null;
                    }
                    Table table = usedDatabase.getTable(tableName);
                    if (table == null)
                        return null;
                    return table.getAttribute(attributeName);
                })
                .filter(Objects::nonNull)
                .map(indexOfAttribute::get)
                .collect(Collectors.toList());
        return result.stream()
                .map(row -> selectedIndexes.stream()
                        .map(row::get)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    protected static String projectionAllOnTables(ArrayList<Table> selectedTables) {
        return selectedTables.stream()
                .map(Table::getAttributes)
                .map(attributes -> attributes.stream()
                        .map(Attribute::getName)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining(","));
    }
}
