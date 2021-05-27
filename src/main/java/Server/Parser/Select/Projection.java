package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static Server.Parser.Select.TableParser.attributeParser;

public class Projection {
    private static final Pattern aggPattern = Pattern.compile(
            "^(?:(COUNT|MIN|MAX|SUM|AVG)\\()?([A-Z0-9._]+)\\)?$",
            Pattern.CASE_INSENSITIVE);

    protected static List<Attribute> parseProjection(
            String projection,
            ArrayList<Table> selectedTables,
            HashMap<String, Table> tableAS,
            HashMap<Attribute, String> aggregationOfAttribute)
    {
        return Arrays.stream(projection.split(","))
                .map(column -> {
                    Matcher aggMatcher = aggPattern.matcher(column);
                    String attributeName, aggregation = null;
                    if (aggMatcher.find()) {
                        aggregation = aggMatcher.group(1);
                        attributeName = aggMatcher.group(2);
                    } else {
                        attributeName = column;
                    }
                    try {
                        Attribute attribute = attributeParser(attributeName, selectedTables, tableAS).getValue();
                        if (aggregation != null) {
                            aggregationOfAttribute.put(attribute, aggregation);
                        }
                        return attribute;
                    } catch (DbExceptions.DataManipulationException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected static List<List<String>> projectionOnResult(
            List<List<String>> result,
            List<Attribute> selectedAttributes,
            HashMap<Attribute, Integer> indexOfAttribute)
    {
        List<Integer> selectedIndexes = selectedAttributes.stream()
                .map(indexOfAttribute::get)
                .collect(Collectors.toList());

        indexOfAttribute.entrySet().removeIf(keyValue -> !selectedAttributes.contains(keyValue.getKey()));

        return result.stream()
                .map(row -> selectedIndexes.stream()
                        .map(row::get)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    protected static String projectionAllOnTables(ArrayList<Table> selectedTables) {
        return selectedTables.stream()
                .map(table -> {
                    String tableName = table.getName();
                    return table.getAttributes().stream()
                            .map(Attribute::getName)
                            .map(attributeName -> tableName + "." + attributeName)
                            .collect(Collectors.joining(","));
                })
                .collect(Collectors.joining(","));
    }

    protected static void checkProjection(
            List<Attribute> selectedAttributes,
            List<Attribute> groupByAttributes,
            HashMap<Attribute, String> aggregation) throws DbExceptions.DataManipulationException
    {
        for (Attribute selectedAttribute : selectedAttributes) {
            if (aggregation.containsKey(selectedAttribute)) {
                if (groupByAttributes != null && groupByAttributes.contains(selectedAttribute))
                    throw new DbExceptions.DataManipulationException(
                            "Aggregation on group by column");
                if (!aggregation.get(selectedAttribute).equalsIgnoreCase("COUNT")
                        && !selectedAttribute.getDataType().equalsIgnoreCase("INT"))
                    throw new DbExceptions.DataManipulationException(
                            "Incompatible aggregation on column type " + selectedAttribute.getDataType());
            } else {
                if (groupByAttributes == null || !groupByAttributes.contains(selectedAttribute))
                    throw new DbExceptions.DataManipulationException(
                            "Non group-by column was not aggregated.");
            }
        }
    }
}
