package Server.Parser;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.MongoDBManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InsertData {
    public static String insert(String command, Database usedDatabase, MongoDBManager mongoDBManager)
            throws DbExceptions.DataManipulationException, DbExceptions.DataDefinitionException {
        Pattern insertPattern = Pattern.compile(
                "INSERT INTO ([a-zA-Z0-9_]+)[ ]?(\\(([^()]+)\\))?VALUES\\(([^()]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(command);
        String tableName, intoCols, valueList;
        if(insertMatcher.find()) {
            tableName = insertMatcher.group(1);
            intoCols = insertMatcher.group(3);
            valueList = insertMatcher.group(4);
        }
        else{
            throw new DbExceptions.DataManipulationException("Incorrect insert syntax.");
        }

        Table currentTable = usedDatabase.getTable(tableName);
        if(currentTable == null)
            throw new DbExceptions.DataManipulationException(
                    "Can't insert into non-existent table: " + tableName);

        if (intoCols == null) {
            intoCols = currentTable.getAttributes().stream()
                    .map(Attribute::getName)
                    .collect(Collectors.joining(","));
        }

        List<String> colNames, values;
        colNames = Arrays.asList(intoCols.split(","));
        values = Arrays.asList(valueList.split(","));

        List<String> valuesToInsert = new ArrayList<>();
        List<String> keysToInsert = new ArrayList<>();

        int pkCount = (int) currentTable.getAttributes().stream().filter(Attribute::isPk).count();
        boolean intPK = (pkCount == 1) &&
                currentTable.getAttributes().stream()
                        .filter(Attribute::isPk)
                        .map(Attribute::getDataType)
                        .map(dt -> dt.equalsIgnoreCase("int"))
                        .reduce(true, Boolean::logicalAnd);

        for (Attribute attribute : currentTable.getAttributes())
        {
            int i = colNames.indexOf(attribute.getName());
            if (i < 0) {
                if (attribute.isNotNull())
                    throw new DbExceptions.DataManipulationException("This field must not be null.");
                valuesToInsert.add("null");
            }
            else {
                String val = values.get(i);
                // type check
                if ("int".equalsIgnoreCase(attribute.getDataType())) {
                    try {
                        Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        throw new DbExceptions.DataManipulationException(
                                "Integer expected for attribute " + attribute.getName());
                    }
                } else {
                    Matcher varcharTypeMatcher = Pattern
                            .compile("VARCHAR\\(([0-9]+)\\)", Pattern.CASE_INSENSITIVE)
                            .matcher(attribute.getDataType());
                    if (!varcharTypeMatcher.find()) {
                        throw new DbExceptions.DataManipulationException(
                                "Attribute " + attribute.getName() +
                                        " has unknown datatype:" + attribute.getDataType()
                        );
                    }
                    Matcher varcharValueMatcher = Pattern.compile("^'([^']+)'|\"([^\"]+)\"$").matcher(val);
                    if (!varcharValueMatcher.find()) {
                        throw new DbExceptions.DataManipulationException(
                                "Varchar variable was given incorrectly: " + val
                        );
                    }
                    int maxLen = Integer.parseInt(varcharTypeMatcher.group(1));
                    String varchar = varcharValueMatcher.group(1);
                    if (varchar == null)
                        varchar = varcharValueMatcher.group(2);
                    if (varchar.length() - 2 > maxLen)
                        throw new DbExceptions.DataManipulationException("The string is too long.");
                    val = varchar;
                }

                // fk check

                if(attribute.isFk()) {
                    Table refTable = usedDatabase.getTable(attribute.getRefTable());
                    if (refTable == null)
                        throw new DbExceptions.DataDefinitionException
                                ("Non-existent table referenced: " + attribute.getRefTable());
                    Iterator<Attribute> attributeIterator = refTable.getAttributes().iterator();
                    int pkIndex = 0, valueIndex = 0;
                    Attribute refAttr = null;
                    while (attributeIterator.hasNext() &&
                            !(refAttr = attributeIterator.next()).getName().equals(attribute.getRefColumn())
                    ) {
                        if (refAttr.isPk())
                            pkIndex++;
                        else
                            valueIndex++;
                    }
                    if (refAttr == null)
                        throw new DbExceptions.DataDefinitionException
                                ("Non-existent attribute referenced: " + attribute.getRefColumn());
                    int refTablePkCount = (int) refTable.getAttributes().stream().filter(Attribute::isPk).count();
                    if (refTablePkCount == 1 && refAttr.isPk() &&
                            refAttr.getDataType().equalsIgnoreCase("int")) {
                        if (mongoDBManager.isUniqueKey(refTable.getName(), val))
                            throw new DbExceptions.DataManipulationException
                                    ("Referencing table does not contains value: " + val);
                    } else {
                        if (mongoDBManager.isUniqueValue(
                                refTable.getName(), val,
                                refAttr.isPk() ? "_id" : "value",
                                refAttr.isPk() ? pkIndex : valueIndex
                        ))
                            throw new DbExceptions.DataManipulationException
                                    ("Referencing table does not contains value: " + val);
                    }
                }

                // unique check
                if(attribute.isUnique()){
                    if (intPK && attribute.isPk()) {
                        if (!mongoDBManager.isUniqueKey(currentTable.getName(), val))
                            throw new DbExceptions.DataManipulationException("This field must be unique.");
                    } else {
                        if (!mongoDBManager.isUniqueValue(
                                currentTable.getName(), val,
                                attribute.isPk() ? "_id" : "value",
                                attribute.isPk() ? keysToInsert.size() : valuesToInsert.size())
                        )
                            throw new DbExceptions.DataManipulationException("This field must be unique.");
                    }
                }

                // not null check
                if(attribute.isNotNull() && val.equalsIgnoreCase("null"))
                    throw new DbExceptions.DataManipulationException("This field must not be null.");

                // pk check
                if (attribute.isPk())
                    keysToInsert.add(val);
                else
                    valuesToInsert.add(val);

            }
        }

        String key = String.join("#", keysToInsert);
        String value = String.join("#", valuesToInsert);

        if (intPK)
            mongoDBManager.insertIntKey(tableName, Integer.parseInt(key), value);
        else
            mongoDBManager.insert(tableName, key, value);

        HashMap<String, Attribute> attributeHashMap = new HashMap<>();
        currentTable.getAttributes()
                .forEach(attribute -> attributeHashMap.put(attribute.getName(), attribute));

        // indexing
        for (int i = 0; i < colNames.size(); i++) {
            Attribute attribute = attributeHashMap.get(colNames.get(i));

            if (!attribute.getIndex().equals(""))
            {
                if ("int".equalsIgnoreCase(attribute.getDataType())) {
                    Integer val = Integer.parseInt(values.get(i));
                    if (attribute.isUnique())
                        mongoDBManager.insertIntKey(attribute.getIndex(), val, key);
                    else {
                        mongoDBManager.insertNotUniqueIndexIntKey(attribute.getIndex(), val, key);
                    }
                } else {
                    String val = values.get(i);
                    val = val.substring(1, val.length()-1);
                    if (attribute.isUnique())
                        mongoDBManager.insert(attribute.getIndex(), val, key);
                    else {
                        mongoDBManager.insertNotUniqueIndex(attribute.getIndex(), val, key);
                    }
                }
            }
        }

        return "1 row inserted successfully";
    }
}
