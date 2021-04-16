package Server;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Pattern;

public class Main {
    private final static Pattern
            successiveWhitespaces = Pattern.compile(
                    "\\s+(?=(?:[^\"']*(?:(\"[^\"]*\")|('[^']*')))*[^\"']*$)"),
            delimiterSurroundingSpaces = Pattern.compile(
                    "\\s*([(){},;<>=])\\s*(?=(?:[^\"']*(?:(\"[^\"]*\")|('[^']*')))*[^\"']*$)");

    public static void main(String[] args) throws ParseException {
        // for the client
        int portNumber = 4242;

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            CommandProcessor commandProcessor = new CommandProcessor();

            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String msg, inputLine;
            StringBuilder msgBuilder;
            System.out.println("server started");

            mainWhile:
            while (true) {
                msgBuilder = new StringBuilder();
                while (!(inputLine = in.readLine()).equals("")) {
                    msgBuilder.append(inputLine);
                }
                msg = msgBuilder.toString();
                msg = successiveWhitespaces.matcher(msg).replaceAll(" ");
                msg = delimiterSurroundingSpaces.matcher(msg).replaceAll("$1");

                for (String command : msg.split(";")) {
                    if ("".equals(command))
                        break;
                    if (command.equals("exit")) {
                        out.println("bye");
                        break mainWhile;
                    }
                    try {
                        commandProcessor.processCommand(command);
                        out.println("OK");
                    }
                    catch (DbExceptions.DataDefinitionException |
                            DbExceptions.UnsuccessfulDeleteException |
                            DbExceptions.UnknownCommandException |
                            DbExceptions.DataManipulationException e
                    ) {
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
