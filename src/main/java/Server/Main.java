package Server;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws ParseException {

        /* for the client*/
        int portNumber = 4242;

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            CommandProcessor commandProcessor = new CommandProcessor();

            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            char[] buffer = new char[1024];
            String msg, inputLine;
            StringBuilder msgBuilder;
            System.out.println("server started");

            mainWhile:
            while (true) {
                /*
                int len = Integer.parseInt(in.readLine());
                while (len > 0) {
                    len -= in.read(buffer, 0, Math.min(1024, len));
                    msgBuilder.append(String.valueOf(buffer));
                }
                 */
                msgBuilder = new StringBuilder();
                while (!(inputLine = in.readLine()).equals("")) {
                    msgBuilder.append(inputLine);
                }
                msg = msgBuilder.toString();

                for (String command : msg.split(";")) {
                    if ("".equals(command))
                        break;
                    try {
                        if (command.equals("exit")) {
                            System.out.println("bye");
                            out.println("bye");
                            Json.saveDatabases(commandProcessor.getDatabases());
                            break mainWhile;
                        }
                        commandProcessor.processCommand(command);
                        out.println("OK");
                    } catch (DatabaseExceptions.DataDefinitionException | DatabaseExceptions.UnsuccesfulDeleteException | DatabaseExceptions.UnknownCommandException e) {
                        out.println(e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        try{
            CommandProcessor commandProcessor = new CommandProcessor();
            commandProcessor.processCommand("delete database University;");
            commandProcessor.processCommand("create database University;");
            commandProcessor.processCommand("USE University;");

            commandProcessor.processCommand("CREATE TABLE disciplines (\n" +
                    "  DiscID varchar(5) PRIMARY KEY,\n" +
                    "  DName varchar(30),\n" +
                    "  CreditNr int\n" +
                    ");");
            commandProcessor.processCommand("insert into disciplines (DiscID,DName,CreditNr) values ('DB1','Databases 1', 7);");
            Json.saveDatabases(commandProcessor.getDatabases());

        } catch (IOException | DatabaseExceptions.UnknownCommandException | DatabaseExceptions.DataDefinitionException | DatabaseExceptions.UnsuccesfulDeleteException e) {
            e.printStackTrace();
        }
         */
    }
}
