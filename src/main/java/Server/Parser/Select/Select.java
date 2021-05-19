package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.Filter;
import Server.MongoDBManager;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Server.Parser.Select.Condition.conditionParser;
import static Server.Parser.Select.Joining.joinResults;
import static Server.Parser.Select.Mongo.findFromTableByFilters;
import static Server.Parser.Select.TableParser.joinParser;
import static Server.Parser.Select.TableParser.tableParser;
import static Server.Parser.Select.ToHTML.toHTMLTable;
import static Server.Parser.Select.Projection.*;

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

                joinedOn.put(joinedTable, joinParser(joinedTableMatcher, selectedTables, tableAS));
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
                if (!conditionParser(condMatcher, selectedTables,
                        filtersOnAttributeInTable, tableAS, attributeByNameInTable))
                    throw new DbExceptions.DataManipulationException("Incorrect where condition: " + cond);
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

        // joining tables
        List<List<String>> result = joinResults(results, mainTable, indexOfAttribute, joinedOn);

        // projection
        if (!projection.equals("*")) {
            result = projectionOnResult(projection, result, selectedTables, usedDatabase, indexOfAttribute);
        } else {
            projection = projectionAllOnTables(selectedTables);
        }

        return toHTMLTable(Arrays.asList(projection.split(",")), result);
    }
}
