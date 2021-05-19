package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.Table;
import Server.Filter;
import Server.MongoDBManager;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

public class Mongo {
    protected static List<List<String>> findFromTableByFilters(
            Table table, MongoDBManager mongoDBManager,
            HashMap<Attribute, ArrayList<Filter>> filtersOnAttribute,
            HashMap< Attribute, Integer > indexInDocument)
    {
        // find primary keys from indexes
        Stack<List<String>> pkListsToIntersect = new Stack<>();
        filtersOnAttribute.forEach((attribute, filters) -> {
            if (!attribute.getIndex().equals("") && filters.size() > 0) {
                List<Bson> bsonList;
                if (attribute.getDataType().equalsIgnoreCase("int"))
                    bsonList = filters.stream().map(Filter::asMongoFilterOnIntID).collect(Collectors.toList());
                else
                    bsonList = filters.stream().map(Filter::asMongoFilterOnID).collect(Collectors.toList());

                FindIterable<Document> documents =
                        mongoDBManager.findFiltered(attribute.getIndex(), Filters.and(bsonList));
                List<String> filteredPKs = new ArrayList<>();
                for (Document document : documents) {
                    String pks = document.getString("value");
                    if (attribute.isUnique()) {
                        filteredPKs.add(pks);
                    } else {
                        filteredPKs.addAll(Arrays.asList(pks.split("##")));
                    }
                }
                pkListsToIntersect.push(filteredPKs);
            }
        });

        // construct a filter on PK
        Bson filter = null;
        Attribute pk = null;
        int pkCount = (int) table.getAttributes().stream().filter(Attribute::isPk).count();
        if (pkCount == 1) {    // single PK
            pk = table.getAttributes().stream()
                    .filter(Attribute::isPk)
                    .findFirst()
                    .orElse(null);
        }
        if (pk != null && filtersOnAttribute.get(pk).size() > 0) {
            if (pk.getDataType().equalsIgnoreCase("int")) {
                filter = Filters.and(filtersOnAttribute.get(pk).stream()
                        .map(Filter::asMongoFilterOnIntID)
                        .collect(Collectors.toList())
                );
            } else {
                filter = Filters.and(filtersOnAttribute.get(pk).stream()
                        .map(Filter::asMongoFilterOnID)
                        .collect(Collectors.toList())
                );
            }
        }

        if (!pkListsToIntersect.empty()) {

            // intersection of primary key lists
            List<String> filteredPKs = pkListsToIntersect.pop();
            while (!pkListsToIntersect.empty()) {
                filteredPKs = pkListsToIntersect.pop().stream()
                        .filter(filteredPKs::contains)
                        .collect(Collectors.toList());
            }

            if (pk != null) {
                if (pk.getDataType().equalsIgnoreCase("int")) {
                    if (filter != null)
                        filter = Filters.and(
                                Filters.in("_id", filteredPKs.stream()
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList())),
                                filter
                        );
                    else
                        filter = Filters.in("_id", filteredPKs.stream()
                                .map(Integer::parseInt)
                                .collect(Collectors.toList()));
                } else {
                    if (filter != null)
                        filter = Filters.and(
                                Filters.in("_id", filteredPKs),
                                filter
                        );
                    else
                        filter = Filters.in("_id", filteredPKs);
                }
            } else {
                filter = Filters.in("_id", filteredPKs);
            }
        }

        // find documents from main collection filtered by indexes
        FindIterable<Document> documents = mongoDBManager.findFiltered(table.getName(), filter);
        List<List<String>> result = new ArrayList<>();
        for (Document document : documents) {
            List<String> row = new ArrayList<>();
            row.addAll(Arrays.asList(document.get("_id").toString().split("#")));
            row.addAll(Arrays.asList(document.getString("value").split("#")));

            List<Boolean> passed = new ArrayList<>();
            filtersOnAttribute.forEach((attribute, filters) -> {
                if (attribute.getIndex().equals("") && !attribute.isPk()) {
                    passed.add(
                            filters.stream()
                                    .map(fil -> fil.eval(row.get(indexInDocument.get(attribute))))
                                    .reduce(Boolean::logicalAnd).orElse(true)
                    );
                }
            });
            if (passed.stream().reduce(Boolean::logicalAnd).orElse(true))
                result.add(row);
        }

        return result;
    }
}
