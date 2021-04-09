package Server;

import org.json.simple.parser.ParseException;

import java.io.*;

public class Main {

    public static void main(String[] args) throws ParseException {

        /* for the client
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

        } catch (IOException | DatabaseExceptions.DataDefinitionException | DatabaseExceptions.UknownCommandException e) {
            e.printStackTrace();
        }*/

        try{
            CommandProcessor commandProcessor = new CommandProcessor();
            commandProcessor.processCommand("create database University");
            commandProcessor.processCommand("USE University");

            commandProcessor.processCommand("CREATE TABLE disciplines (\n" +
                    "  DiscID varchar(5) PRIMARY KEY,\n" +
                    "  DName varchar(30),\n" +
                    "  CreditNr int\n" +
                    ");");
            Json.saveDatabases(commandProcessor.getDatabases());

        } catch (IOException | DatabaseExceptions.UnknownCommandException | DatabaseExceptions.DataDefinitionException | DatabaseExceptions.UnsuccesfulDeleteException | DatabaseExceptions.DatabaseNotExistsException e) {
            e.printStackTrace();
        }

    }
}
