package Server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

class main {

    public static void main(String[] args) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader file = new FileReader("C:\\Users\\erdei\\Documents\\Egyetem\\NegyedikFelev\\AB2\\miniABKR\\src\\main\\resources\\database.json");

        Object obj = parser.parse(file);

        JSONObject jsonObject = (JSONObject)obj;

        Database database = new Database(jsonObject);

        System.out.println(database.getName());
        System.out.println(Json.nodeToString(database));
        System.out.println(database.getTables().get(0).getName());
        System.out.println(database.getTables().get(1).getName());
        System.out.println(database.getTables().get(0).getAttributes().get(0).getName());



    }
}
