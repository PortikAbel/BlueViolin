package Server;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Updates.set;

public class MongoDBManager {
    private final MongoClient mongoClient;
    private MongoDatabase currentDatabase;

    public MongoDBManager() {
        MongoClientURI uri = new MongoClientURI("mongodb+srv://Csongi:admin@blueviolin.j4yyd.mongodb.net/test?retryWrites=true&w=majority");
        this.mongoClient = new MongoClient(uri);
        currentDatabase = mongoClient.getDatabase("master");
    }

    public void deleteDatabase(String databaseName) {
        currentDatabase = mongoClient.getDatabase(databaseName);
        currentDatabase.drop();
    }

    public void deleteTable(String tableName) {
        MongoCollection<Document> collection = currentDatabase.getCollection(tableName);
        collection.drop();
    }

    public void insert (String tableName, String key, String value){
        MongoCollection<Document> collection = currentDatabase.getCollection(tableName);
        collection.insertOne(new Document("_id", key).append("value", value));
    }

    public void insertIntKey(String tableName, Integer key, String value){
        MongoCollection<Document> collection = currentDatabase.getCollection(tableName);
        collection.insertOne(new Document("_id", key).append("value", value));
    }

    public void insertNotUniqueIndex (String indexName, String attribute, String primaryKey) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        FindIterable<Document> result = collection.find(Filters.eq("_id", attribute));
        Document docToUpdate = result.first();
        if (docToUpdate != null) {
            String newValue = docToUpdate.getString("value");
            newValue += "##" + primaryKey;
            collection.findOneAndUpdate(
                    Filters.eq("_id", attribute),
                    set("value", newValue)
            );
        } else {
            collection.insertOne(new Document("_id", attribute).append("value", primaryKey));
        }
    }

    public void insertNotUniqueIndexIntKey (String indexName, Integer attribute, String primaryKey) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        FindIterable<Document> result = collection.find(Filters.eq("_id", attribute));
        Document docToUpdate = result.first();
        if (docToUpdate != null) {
            String newValue = docToUpdate.getString("value");
            newValue += "##" + primaryKey;
            collection.findOneAndUpdate(
                    Filters.eq("_id", attribute),
                    set("value", newValue)
            );
        } else {
            collection.insertOne(new Document("_id", attribute).append("value", primaryKey));
        }
    }

    public void delete (String tableName, Iterable<Object> keysToDelete) {
        MongoCollection<Document> collection = currentDatabase.getCollection(tableName);
        for(Object key : keysToDelete) {
            collection.deleteOne(Filters.eq("_id", key));
        }
    }

    public void deleteFromNotUniqueIndex (String indexName, HashMap<Object, ArrayList<String>> pksToDelete) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        pksToDelete.forEach((key, list) -> {
            Document docToUpdate = collection.find(Filters.eq("_id", key)).first();
            if (docToUpdate != null) {
                String newValue = Arrays.stream(docToUpdate.getString("value")
                        .split("##"))
                        .filter(pk -> !list.contains(pk))
                        .collect(Collectors.joining("##"));
                collection.findOneAndUpdate(
                        Filters.eq("_id", key),
                        set("value", newValue)
                );
            }
        });
    }

    public boolean isUnique(String tableName, String value, String key_value, int index){
        MongoCollection<Document> currentTable = currentDatabase.getCollection(tableName);
        for (Document document : currentTable.find()) {
            String field = document.getString(key_value);
            if(field.split("#")[index].equals(value))
                return false;
        }
        return true;
    }

    public FindIterable<Document> getAllDocuments(String tableName) {
        return currentDatabase.getCollection(tableName).find();
    }

    public void use(String databaseName){
        currentDatabase = mongoClient.getDatabase(databaseName);
    }
}
