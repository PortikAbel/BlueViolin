package Server;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws ParseException {
        // for the client
        int portNumber = 4242;

        String insert = "INSERT INTO tabla(mezo1,mezo2) VALUES(ertek1,ertek2)";
        String insert2 = "INSERT INTO tabla VALUES(ertek1,ertek2)";
        Pattern insertPattern = Pattern.compile("INSERT INTO ([a-zA-Z0-9_]+)(\\(([^()]+)\\))? VALUES\\(([^()]+)\\)");
        Matcher m1 = insertPattern.matcher(insert);
        Matcher m2 = insertPattern.matcher(insert2);
        if (m1.find() && m2.find())
            for(int i = 1; i <= 4; i++)
            {
                System.out.println(m1.group(i));
                System.out.println(m2.group(i));
            }

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
                msg = msg.replaceAll("\\s+"," ");
                msg = msg.replaceAll("(\\s+)?([(),;{}<>=])(\\s+)?","$2");

                for (String command : msg.split(";")) {
                    if ("".equals(command))
                        break;
                    if (command.equals("exit")) {
                        out.println("bye");
                        break mainWhile;
                    }
                    try {
                        commandProcessor.processCommand(command);
                        Json.saveDatabases(commandProcessor.getDatabases());
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
