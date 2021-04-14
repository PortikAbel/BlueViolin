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
