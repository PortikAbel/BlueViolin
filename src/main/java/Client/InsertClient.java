package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class InsertClient {
    private BufferedReader serverToClientReader;
    private PrintWriter clientToServerWriter;

    public InsertClient() {
        try {
            Socket clientSocket = new Socket("localhost", 4242);
            serverToClientReader = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()
                    )
            );
            clientToServerWriter = new PrintWriter(
                    clientSocket.getOutputStream(), true
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String send(String msg) {
        clientToServerWriter.println(msg);
        clientToServerWriter.println(".");
        StringBuilder respBuilder = new StringBuilder();
        String inputLine;
        try {
            while (!(inputLine = serverToClientReader.readLine()).equals(".")) {
                respBuilder.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            return "";
        }
        return respBuilder.toString();
    }

    public void insertData(String dbName, String tableName, int[] maxInt, int rowCount) {
        System.out.println((send("USE "+dbName)));
        Random random = new Random();
        for (int i = 0; i < rowCount; i++) {
            send(String.format("INSERT INTO %s VALUES(%s);",
                    tableName,
                    i + "," + Arrays.stream(maxInt)
                        .map(random::nextInt)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining(","))
            ));
        }
    }

    public static void main(String[] args) {
        InsertClient insertClient = new InsertClient();
        insertClient.insertData(
                "BigDB",
                "BigTable",
                new int[]{ 10, 500 },
                100000);
    }
}
