package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Table;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AttributeFinder {
    public static String getTableOfAttribute(List<Table> selectedTables, String attributeName) {
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

    public static void indexAttributesInTable(
            Table table,
            HashMap < Table, HashMap< String, Attribute >> attributeByNameInTable,
            HashMap< Attribute, Integer > indexInDocument
    ) {
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
