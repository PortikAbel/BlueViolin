package Server;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBManager {
    private final MongoClient mongoClient;
    private MongoDatabase currentDatabase = null;

    public MongoDBManager() {
        MongoClientURI uri = new MongoClientURI("mongodb+srv://Csongi:admin@blueviolin.j4yyd.mongodb.net/test?retryWrites=true&w=majority");
        this.mongoClient = new MongoClient(uri);
    }

    public void insert (String tableName, String key, String value){
        MongoCollection<Document> collection = currentDatabase.getCollection(tableName);
        collection.insertOne(new Document(key,value));
    }

    public void insertUniqueIndex (String indexName, String key, String value) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        collection.insertOne(new Document(key,value));
    }

    public void insertNotUniqueIndex (String indexName, String attribute, String primaryKey) {
        MongoCollection<Document> collection = currentDatabase.getCollection(indexName);
        // append primaryKey to value of attribute
    }

    public boolean valueIsUnique(String tableName, String value, int index){
        MongoCollection<Document> currentTable = currentDatabase.getCollection(tableName);
        for (Document document : currentTable.find()) {
            String field = document.getString("value");
            if(field.split("#")[index].equals(value))
                return false;
        }
        return true;
    }

    public void use(String databaseName){
        currentDatabase = mongoClient.getDatabase(databaseName);
    }
}
