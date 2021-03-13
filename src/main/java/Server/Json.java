package Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Json {

    private static final ObjectMapper objectMapper = getDefaultObjectMapper();

    private static ObjectMapper getDefaultObjectMapper() {
        //defaultObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new ObjectMapper();
    }

    public static List<Database> buildDatabases() throws IOException, ParseException {
        List<Database> databases = new ArrayList<>();
        File dir = new File(".\\src\\main\\");
        File[] filesInDir = dir.listFiles();
        assert filesInDir != null;
        for (File file : filesInDir){
            if(file.toString().contains(".json"))
            {
                JSONParser parser = new JSONParser();
                FileReader fileReader = new FileReader(file);

                Object obj = parser.parse(fileReader);

                JSONObject jsonObject = (JSONObject)obj;

                Database database = new Database(jsonObject);
                databases.add(database);
            }
        }
        return databases;
    }

    public static String nodeToString(Object obj) throws JsonProcessingException {
        ObjectWriter objectWriter = objectMapper.writer();
        objectWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);
        return objectWriter.writeValueAsString(obj);
    }
}
