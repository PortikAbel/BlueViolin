package Server.Parser.Select;

import Server.DbStructure.Attribute;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GroupBy {
    protected static void orderRows(List<List<String>> result,
                                    List<Attribute> groupByAttributes,
                                    HashMap<Attribute, Integer> indexOfAttribute)
    {
        Collections.reverse(groupByAttributes);
        for (Attribute groupByAttribute : groupByAttributes) {
            if (groupByAttribute.getDataType().equalsIgnoreCase("INT")) {
                result.sort(Comparator.comparing(
                        row -> Integer.parseInt(
                                row.get(indexOfAttribute.get(groupByAttribute))
                        )
                ));
            } else {
                result.sort(Comparator.comparing(row ->
                        row.get(indexOfAttribute.get(groupByAttribute)))
                );
            }
        }
        Collections.reverse(groupByAttributes);
    }

    protected static void aggregateColumns(List<List<String>> result,
                                           List<Attribute> selectedAttributes,
                                           HashMap<Attribute, String> aggregation)
    {
        List<Integer> nonAggregatedColIndexes = new ArrayList<>();
        for (Attribute attribute : selectedAttributes) {
            if (!aggregation.containsKey(attribute))
                nonAggregatedColIndexes.add(selectedAttributes.indexOf(attribute));
        }

        HashMap<Integer, String> aggregateOnIndex = new HashMap<>();
        for (Attribute attribute : aggregation.keySet())
            aggregateOnIndex.put(selectedAttributes.indexOf(attribute), aggregation.get(attribute));

        List<String> lastRow, currentRow;
        List<List<String>> rowsToAggregate = new ArrayList<>();
        List<List<String>> aggregatedRows = new ArrayList<>();
        Iterator<List<String>> rowIterator = result.listIterator();

        lastRow = rowIterator.next();
        rowsToAggregate.add(lastRow);
        rowIterator.remove();

        while (rowIterator.hasNext()) {
            currentRow = rowIterator.next();
            if (!equalListsOnIndexes(lastRow, currentRow, nonAggregatedColIndexes)) {
                aggregatedRows.add(aggregate(rowsToAggregate, aggregateOnIndex));
                rowsToAggregate.clear();
            }
            rowsToAggregate.add(currentRow);
            lastRow = currentRow;
            rowIterator.remove();
        }

        if (aggregatedRows.size() > 0)
            aggregatedRows.add(aggregate(rowsToAggregate, aggregateOnIndex));

        result.addAll(aggregatedRows);
    }

    private static boolean equalListsOnIndexes(List<String> l1, List<String> l2, List<Integer> indexes) {
        return indexes.stream().map(i -> l1.get(i).equals(l2.get(i))).reduce(true, Boolean::logicalAnd);
    }

    private static List<String> aggregate(List<List<String>> rows, HashMap<Integer, String> aggregateOnIndex) {
        return IntStream.range(0, rows.get(0).size())
                .mapToObj(i -> {
                    if (aggregateOnIndex.containsKey(i)) {
                        Stream<String> valuesToAggregate = rows.stream().map(row -> row.get(i));
                        if (aggregateOnIndex.get(i).equalsIgnoreCase("COUNT"))
                            return String.valueOf(valuesToAggregate.count());
                        else {
                            IntStream integersToAggregate = valuesToAggregate.mapToInt(Integer::parseInt);
                            switch (aggregateOnIndex.get(i).toUpperCase()) {
                                case "MIN":
                                    return String.valueOf(integersToAggregate.min().getAsInt());
                                case "MAX":
                                    return String.valueOf(integersToAggregate.max().getAsInt());
                                case "SUM":
                                    return String.valueOf(integersToAggregate.sum());
                                case "AVG":
                                    return String.valueOf(integersToAggregate.average().getAsDouble());
                                default:
                                    return "NULL";
                            }
                        }
                    } else {
                        return rows.get(0).get(i);
                    }
                })
                .collect(Collectors.toList());
    }
}
