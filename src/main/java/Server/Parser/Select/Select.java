package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.Filter;
import Server.MongoDBManager;
import javafx.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static Server.Parser.Select.Mongo.findFromTableByFilters;
import static Server.Parser.Select.AttributeFinder.*;
import static Server.Parser.Select.ToHTML.toHTMLTable;
import static Server.Parser.Select.Table.tableParser;

public class Select {
    public static String select(String command, Database usedDatabase, MongoDBManager mongoDBManager)
            throws DbExceptions.DataManipulationException {
        Pattern selectPattern = Pattern.compile(
                "^SELECT (\\*|[a-zA-Z0-9_,.]+)" +
                        " FROM ([a-zA-Z0-9_]+(?: (?:AS )?[a-zA-Z0-9_])?)" +
                        "((?: JOIN [a-zA-Z0-9_]+(?: (?:AS )?[a-zA-Z0-9_])?" +
                        " ON [a-zA-Z0-9_.]+=[a-zA-Z0-9_.]+)+)?" +
                        "(?: WHERE (.+))?$",
                Pattern.CASE_INSENSITIVE);
        Matcher selectMatcher = selectPattern.matcher(command);

        String projection, mainTableName, joins, conditions;
        if (selectMatcher.find()) {
            projection = selectMatcher.group(1);
            mainTableName = selectMatcher.group(2);
            joins = selectMatcher.group(3);
            conditions = selectMatcher.group(4);
        } else {
            throw new DbExceptions.DataManipulationException("Incorrect SELECT syntax");
        }

        Pattern tableAliasPattern = Pattern.compile(
                "([a-zA-Z0-9_]+)(?: (?:AS )?([a-zA-Z0-9_]))?",
                Pattern.CASE_INSENSITIVE);
        HashMap<String, Table> tableAS = new HashMap<>();
        HashMap< Table, HashMap< String, Attribute> > attributeByNameInTable = new HashMap<>();
        HashMap< Attribute, Integer > indexOfAttribute = new HashMap<>();
        ArrayList<Table> selectedTables = new ArrayList<>();
        HashMap< Table, Pair<Attribute, Attribute>> joinedOn = new HashMap<>();

        // --------------------------------------------
        // parsing selected tables and their attributes
        // --------------------------------------------

        // main table
        Matcher mainTableMatcher = tableAliasPattern.matcher(mainTableName);
        Table mainTable = tableParser(mainTableMatcher, usedDatabase, tableAS,
                selectedTables, attributeByNameInTable, indexOfAttribute);

        // joined tables
        if (joins != null) {
            Pattern joinedTableNamePattern = Pattern.compile(
                    "JOIN ([a-zA-Z0-9_]+(?: (?:AS )?[a-zA-Z0-9_])?)" +
                            " ON (?:([a-zA-Z0-9_]+)\\.)?([a-zA-Z0-9_]+)=(?:([a-zA-Z0-9_]+)\\.)?([a-zA-Z0-9_]+)");
            Matcher joinedTableMatcher = joinedTableNamePattern.matcher(joins);
            while (joinedTableMatcher.find()) {
                Matcher joinedTableNameMatcher = tableAliasPattern.matcher(joinedTableMatcher.group(1));
                Table joinedTable = tableParser(joinedTableNameMatcher, usedDatabase, tableAS,
                        selectedTables, attributeByNameInTable, indexOfAttribute);

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

                joinedOn.put(joinedTable, new Pair<>(attribute1, attribute2));
            }
        }

        // parse conditions
        HashMap< Table, HashMap< Attribute, ArrayList<Filter> > > filtersOnAttributeInTable = new HashMap<>();
        selectedTables.forEach(table -> {
            HashMap< Attribute, ArrayList<Filter> > filtersOnAttribute = new HashMap<>();
            table.getAttributes().forEach(attribute -> filtersOnAttribute.put(attribute, new ArrayList<>()));
            filtersOnAttributeInTable.put(table, filtersOnAttribute);
        });

        if (conditions != null) {
            Pattern condPattern = Pattern.compile("^(?:([^<>=!]+)\\.)?([^<>=!]+)([<>=!]+)[\"']?([^\"'<>=!]+)[\"']?$");

            for (String cond : conditions.split("(?i) AND ")) {
                Matcher condMatcher = condPattern.matcher(cond);
                String tableAlias, attributeName, operator, rightOperand;
                if (condMatcher.find()) {
                    tableAlias = condMatcher.group(1);
                    attributeName = condMatcher.group(2);
                    operator = condMatcher.group(3);
                    rightOperand = condMatcher.group(4);
                } else {
                    throw new DbExceptions.DataManipulationException("Incorrect where condition: " + cond);
                }
                // if containing table was not given to attribute
                if (tableAlias == null) {
                    tableAlias = getTableOfAttribute(selectedTables, attributeName);
                    if (tableAlias == null)
                        throw new DbExceptions.DataManipulationException(
                                "Could not determine to which table attribute belongs: " + attributeName);
                }
                Filter filter = new Filter(operator, rightOperand);
                filtersOnAttributeInTable
                        .get(tableAS.get(tableAlias))
                        .get(attributeByNameInTable
                                .get(tableAS.get(tableAlias))
                                .get(attributeName))
                        .add(filter);
            }
        }

        // getting the selection result
        HashMap<Table, List<List<String>>> results = new HashMap<>();
        selectedTables.forEach(table -> results
                .put(table, findFromTableByFilters(
                        table, mongoDBManager,
                        filtersOnAttributeInTable.get(table),
                        indexOfAttribute)
                ));

        List<List<String>> result = results.remove(mainTable);
        for (Table tableToJoin : results.keySet()) {
            List<List<String>> newResult = new ArrayList<>();
            List<List<String>> resultToJoin = results.get(tableToJoin);
            int index1 = indexOfAttribute.get(joinedOn.get(tableToJoin).getKey());
            int index2 = indexOfAttribute.get(joinedOn.get(tableToJoin).getValue());
            for (List<String> r1 : result) {
                for (List<String> r2: resultToJoin) {
                    if (r1.get(index1).equals(r2.get(index2))) {
                        List<String> newRow = new ArrayList<>(r1);
                        newRow.addAll(r2);
                        newResult.add(newRow);
                    }
                }
            }
            int prevLength = result.get(0).size();
            tableToJoin.getAttributes().forEach(
                    attribute -> indexOfAttribute.put(attribute,
                            indexOfAttribute.get(attribute) + prevLength)
            );
            result = newResult;
        }

        // projection
        if (!projection.equals("*")) {
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
            result = result.stream()
                    .map(row -> selectedIndexes.stream()
                            .map(row::get)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
        } else {
            projection = selectedTables.stream()
                    .map(Table::getAttributes)
                    .map(attributes -> attributes.stream()
                            .map(Attribute::getName)
                            .collect(Collectors.joining(",")))
                    .collect(Collectors.joining(","));
        }

        return toHTMLTable(Arrays.asList(projection.split(",")), result);
    }
}
