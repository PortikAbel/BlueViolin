package Server;

import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws ParseException {

        int portNumber = 4242;

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            CommandProcessor commandProcessor = new CommandProcessor();

            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            System.out.println("server started");

            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
                if (inputLine.equals("exit")) {
                    System.out.println("bye");
                    out.println("bye");
                    Json.saveDatabases(commandProcessor.getDatabases());
                    break;
                }
                commandProcessor.processCommand(inputLine);
                out.println("OK");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
