package Server;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;

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

    public void use(String databaseName){
        currentDatabase = mongoClient.getDatabase(databaseName);
    }
}
