package Server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws IOException, ParseException {
        /*JSONParser parser = new JSONParser();
        FileReader file = new FileReader(".\\src\\main\\database.json");

        Object obj = parser.parse(file);

        JSONObject jsonObject = (JSONObject)obj;

        Database database = new Database(jsonObject);

        System.out.println(database.getName());
        System.out.println(Json.nodeToString(database));
        System.out.println(database.getTables().get(0).getName());
        System.out.println(database.getTables().get(1).getName());
        System.out.println(database.getTables().get(0).getAttributes().get(0).getName());*/

        int portNumber = 4242;

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);

            while(true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                    if (inputLine.equals("exit"))
                        break;
                }
                break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
