package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {
    private final Socket clientSocket;
    private final BufferedReader serverToClientReader;
    private final PrintWriter clientToServerWriter;

    public ClientMain(String hostname, int portNumber) throws IOException {
        clientSocket = new Socket(hostname, portNumber);
        serverToClientReader = new BufferedReader(
                new InputStreamReader(
                        clientSocket.getInputStream()
                )
        );
        clientToServerWriter = new PrintWriter(
                clientSocket.getOutputStream(), true
        );
    }

    public void send(String msg){
        clientToServerWriter.write(msg);
    }

    public void stop() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        try {
            new ClientMain("localhost", 4242);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
