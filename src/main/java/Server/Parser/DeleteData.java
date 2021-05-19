package Server.Parser;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.Filter;
import Server.MongoDBManager;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeleteData {
    public static String delete(String command, Database usedDatabase, MongoDBManager mongoDBManager)
            throws DbExceptions.DataManipulationException {
        Pattern insertPattern = Pattern.compile(
                "^DELETE FROM ([a-zA-Z0-9_]+) WHERE (.*)$",
                Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(command);
        String tableName;
        String conditions;
        if(insertMatcher.find()) {
            tableName = insertMatcher.group(1);
            conditions = insertMatcher.group(2);
        }
        else{
            throw new DbExceptions.DataManipulationException("Incorrect delete syntax.");
        }

        Table currentTable = usedDatabase.getTable(tableName);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't delete from non-existent table: " + tableName);

        // indexes of attributes in key/value string

        HashMap< String, Attribute> attributeByName = new HashMap<>();
        HashMap< Attribute, Integer > indexOf = new HashMap<>();
        int keyIndex = 0, valueIndex = 0;
        for (Attribute attribute : currentTable.getAttributes()) {
            attributeByName.put(attribute.getName(), attribute);
            if (attribute.isPk())
                indexOf.put(attribute, keyIndex++);
            else
                indexOf.put(attribute, valueIndex++);
        }

        // parse conditions
        HashMap<Filter, Attribute > filterHashMap = new HashMap<>();
        ArrayList<ArrayList<Filter>> orFilters = new ArrayList<>();
        Pattern condPattern = Pattern.compile("^(.+)([<>=!]+)[\"']?([^\"']+)[\"']?$");

        for (String andCond : conditions.split("(?i) OR ")) {
            ArrayList<Filter> andFilters = new ArrayList<>();
            for (String cond : andCond.split("(?i) AND ")) {
                Matcher condMatcher = condPattern.matcher(cond);
                String attributeName, operator, rightOperand;
                if (condMatcher.find()) {
                    attributeName = condMatcher.group(1);
                    operator = condMatcher.group(2);
                    rightOperand = condMatcher.group(3);
                } else {
                    throw new DbExceptions.DataManipulationException("Incorrect where condition: " + cond);
                }
                Filter filter = new Filter(operator, rightOperand);
                andFilters.add(filter);
                filterHashMap.put(filter, attributeByName.get(attributeName));
            }
            orFilters.add(andFilters);
        }

        ArrayList<Object> keysToDelete = new ArrayList<>();
        HashMap< String, HashSet<Object>> keysToDeleteFromIndex = new HashMap<>();
        HashMap< String, HashMap<Object, ArrayList<String>> > pksToDeleteAtKeyNotUniqueIndex = new HashMap<>();
        HashMap< String, Boolean > indexOnInteger = new HashMap<>();
        currentTable.getAttributes().stream()
                .filter(attribute -> !attribute.getIndex().equals(""))
                .forEach(attribute -> {
                    String indexName = attribute.getIndex();
                    indexOnInteger.put(
                            indexName,
                            "int".equalsIgnoreCase(attribute.getDataType())
                    );
                    if (attribute.isUnique())
                        keysToDeleteFromIndex.put(indexName, new HashSet<>());
                    else
                        pksToDeleteAtKeyNotUniqueIndex.put(indexName, new HashMap<>());
                });

        List<Attribute> pks = currentTable.getAttributes().stream()
                .filter(Attribute::isPk)
                .collect(Collectors.toList());
        boolean oneIntPK = pks.size() == 1 && "int".equalsIgnoreCase(pks.get(0).getDataType());

        FindIterable<Document> documents = mongoDBManager.findAll(tableName);
        for (Document document : documents) {
            Object key = document.get("_id");
            String[] values = document.getString("value").split("#");

            if (orFilters.stream().map(
                    andFilters -> andFilters.stream().map(filter -> {
                        Attribute filterOn = filterHashMap.get(filter);
                        if (filterOn.isPk())
                            if (oneIntPK)
                                return filter.eval((Integer) key);
                            else
                                return filter.eval(
                                        (String.valueOf(key)).split("#")[indexOf.get(filterOn)]
                                );
                        else
                            return filter.eval(values[indexOf.get(filterOn)]);
                    })
                            .reduce(Boolean::logicalAnd).orElse(true))
                    .reduce(Boolean::logicalOr).orElse(true)
            ) {
                keysToDelete.add(key);
                currentTable.getAttributes().stream()
                        .filter(attribute -> !attribute.isPk())
                        .forEach(attribute -> {
                            String indexName = attribute.getIndex();
                            if (!indexName.equals("")) {
                                if (attribute.isUnique()) {
                                    if (indexOnInteger.get(indexName))
                                        keysToDeleteFromIndex.get(indexName)
                                                .add(Integer.parseInt(values[indexOf.get(attribute)]));
                                    else
                                        keysToDeleteFromIndex.get(indexName)
                                                .add(values[indexOf.get(attribute)]);
                                } else {
                                    String notUniqueValue = values[indexOf.get(attribute)];
                                    ArrayList<String> pksToDelete;
                                    if (indexOnInteger.get(indexName)) {
                                        pksToDelete = pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                .get(Integer.parseInt(notUniqueValue));
                                        if (pksToDelete == null) {
                                            pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .put(Integer.parseInt(notUniqueValue), new ArrayList<>());
                                            pksToDelete = pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .get(Integer.parseInt(notUniqueValue));
                                        }
                                    }
                                    else {
                                        pksToDelete = pksToDeleteAtKeyNotUniqueIndex
                                                .get(indexName)
                                                .get(notUniqueValue);
                                        if (pksToDelete == null) {
                                            pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .put(notUniqueValue, new ArrayList<>());
                                            pksToDelete = pksToDeleteAtKeyNotUniqueIndex.get(indexName)
                                                    .get(notUniqueValue);
                                        }
                                    }
                                    pksToDelete.add(String.valueOf(key));
                                }
                            }
                        });
            }
        }

        mongoDBManager.delete(tableName, keysToDelete);
        keysToDeleteFromIndex.forEach(mongoDBManager::delete);
        pksToDeleteAtKeyNotUniqueIndex.forEach(mongoDBManager::deleteFromNotUniqueIndex);

        return keysToDelete.size() + " rows deleted successfully";
    }
}
