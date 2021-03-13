package Client;

import Server.Database;
import Server.Json;
import Server.Table;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
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
        try {
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
        }

        root = new TreeItem<>("databases");
        root.setExpanded(true);

        try {
            List<Database> databases = Json.buildDatabases();
            for(Database database : databases) {
                TreeItem<String> newDatabaseItem = new TreeItem<>(database.getName());
                root.getChildren().add(newDatabaseItem);
                for (Table table : database.getTables()) {
                    newDatabaseItem.getChildren().add(new TreeItem<>(table.getName()));
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        treeView.setRoot(root);

        treeView.setOnMouseClicked( mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                ContextMenu options = new ContextMenu();
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();

                if (selectedItem.getValue().equals("databases")) {
                    MenuItem newDatabaseOption = new MenuItem("new database");
                    newDatabaseOption.setOnAction( actionEvent -> {
                        try {
                            AnchorPane newDatabaseDialogue = FXMLLoader.load(getClass().getResource("NewDatabase.fxml"));
                            Stage dialogueStage = new Stage();
                            dialogueStage.setTitle("Create new database");
                            dialogueStage.setScene(new Scene(newDatabaseDialogue));
                            dialogueStage.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    options.getItems().add(newDatabaseOption);
                    treeView.setContextMenu(options);
                }
            }
        });
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
