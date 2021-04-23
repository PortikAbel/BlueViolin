package Server;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;

import static com.mongodb.client.model.Updates.set;

public class MongoDBManager {
    private final MongoClient mongoClient;
    private MongoDatabase currentDatabase = null;

    public MongoDBManager() {
        MongoClientURI uri = new MongoClientURI("mongodb+srv://Csongi:admin@blueviolin.j4yyd.mongodb.net/test?retryWrites=true&w=majority");
        this.mongoClient = new MongoClient(uri);
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
        collection.insertOne(new Document("key", key).append("value", value));
    }

    public void insertUniqueIndex (String indexName, String key, String value) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        collection.insertOne(new Document("key", key).append("value", value));
    }

    public void insertNotUniqueIndex (String indexName, String attribute, String primaryKey) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        FindIterable<Document> result = collection.find(Filters.eq("key", attribute));
        Document docToUpdate = result.first();
        if (docToUpdate != null) {
            String newValue = docToUpdate.getString("value");
            newValue += "#" + primaryKey;
            collection.findOneAndUpdate(
                    Filters.eq("key", attribute),
                    set("value", newValue)
            );
        } else {
            collection.insertOne(new Document("key", attribute).append("value", primaryKey));
        }
    }

    public void delete (String tableName, ArrayList<String> keysToDelete) {
        MongoCollection<Document> collection = currentDatabase.getCollection(tableName);
        for(String key : keysToDelete) {
            collection.deleteOne(Filters.eq("key", key));
        }
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
