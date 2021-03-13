package Client;

import Server.Database;
import Server.Json;
import Server.Table;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {
    private Socket clientSocket;
    private BufferedReader serverToClientReader;
    private PrintWriter clientToServerWriter;

    private TreeItem<String> root;
    private TreeItem<String> selectedDatabase;

    @FXML
    private TreeView<String> treeView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /*try {
            clientSocket = new Socket("Localhost", 4242);
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
        }*/

        root = new TreeItem<>("databases");
        root.setExpanded(true);

        try {
            List<Database> databases = Json.buildDatabases();
            for(Database database : databases) {
                TreeItem<String> newDatabaseItem = new TreeItem<>(database.getName());
                for (Table table : database.getTables()) {
                    newDatabaseItem.getChildren().add(new TreeItem<>(table.getName()));
                }
                root.getChildren().add(newDatabaseItem);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        treeView = new TreeView<>();
        treeView.setRoot(root);/*
        treeView.setOnMouseClicked( e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
            }
        });*/
    }

    public void send(String msg){
        clientToServerWriter.write(msg);
    }

    public void stop() {
        clientToServerWriter.write("exit");
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
