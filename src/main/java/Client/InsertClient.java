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
            Socket clientSocket = new Socket("Localhost", 4242);
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
        clientToServerWriter.println("");
        String resp;
        try {
            resp = serverToClientReader.readLine();
        } catch (IOException e) {
            resp = "";
        }
        return resp;
    }

    public void insertData(String dbName, String tableName, int[] maxInt, int rowCount) {
        if (!"OK".equals(send("USE "+dbName)))
            return;
        Random random = new Random();
        String resp;
        for (int i = 0; i < rowCount; i++) {
            resp = send(String.format("INSERT INTO %s VALUES(%s);",
                    tableName,
                    i + "," + Arrays.stream(maxInt)
                        .map(random::nextInt)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining(","))
            ));
            System.out.println(resp);
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
