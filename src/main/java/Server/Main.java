package Server;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        FileReader file = new FileReader("C:\\Users\\erdei\\Documents\\Egyetem\\NegyedikFelev\\AB2\\miniABKR\\src\\main\\resources\\database.json");
        BufferedReader bufferedReader = new BufferedReader(file);
        String string = bufferedReader.lines().collect(Collectors.joining());
        JsonNode node = Json.parse(string);
        Database database = Json.fromJson(node, Database.class);

        System.out.println(node.fields());
    }
}
