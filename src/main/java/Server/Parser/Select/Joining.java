package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Table;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Joining {
    public static List<List<String>> joinResults(HashMap<Table, List<List<String>>> results,
                                                 Table mainTable,
                                                 HashMap<Attribute, Integer > indexOfAttribute,
                                                 HashMap< Table, Pair<Attribute, Attribute>> joinedOn)
    {
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

        return result;
    }
}
