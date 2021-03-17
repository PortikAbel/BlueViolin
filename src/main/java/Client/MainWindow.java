package Client;

import Server.Database;
import Server.Json;
import Server.Table;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import java.util.stream.Stream;

public class MainWindow implements Initializable {
    private Socket clientSocket;
    private BufferedReader serverToClientReader;
    private PrintWriter clientToServerWriter;

    private TreeItem<String> root;

    @FXML
    private TreeView<String> treeView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // initializing socket connections with server
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

        // building the tree view of databases & tables
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

        // mouse events
        treeView.setOnMouseClicked(new MouseEventHandler());
    }

    public void send(String msg){
        clientToServerWriter.write(msg);
    }
    public Stream<String> receive() { return serverToClientReader.lines(); }

    public void stop() {
        clientToServerWriter.write("exit");
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDatabase(String name) {
        System.out.println("CREATE DATABASE " + name);
        treeView.getSelectionModel().getSelectedItem().getChildren().add(new TreeItem<>(name));
    }

    private class MouseEventHandler implements EventHandler<MouseEvent>
    {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                ContextMenu options = new ContextMenu();
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();

                //  new database
                if (selectedItem.getValue().equals("databases")) {
                    MenuItem newDatabaseOption = new MenuItem("new database");
                    newDatabaseOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("NewDatabase.fxml"));
                        try {
                            AnchorPane newDatabaseDialogue = loader.load();
                            NewDatabase newDatabaseController = loader.getController();
                            newDatabaseController.setMainWindow(MainWindow.this);
                            Stage dialogueStage = new Stage();
                            dialogueStage.setTitle("Create new database");
                            dialogueStage.setScene(new Scene(newDatabaseDialogue));
                            dialogueStage.showAndWait();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    options.getItems().add(newDatabaseOption);
                    treeView.setContextMenu(options);
                }

                //  delete database / new table
                else if (selectedItem.getParent().getValue().equals("databases")) {
                    MenuItem deleteDatabaseOption = new MenuItem("delete database");
                    deleteDatabaseOption.setOnAction( actionEvent -> {

                    });

                    MenuItem newTableOption = new MenuItem("new table");
                    newTableOption.setOnAction( actionEvent -> {

                    });
                    options.getItems().addAll(deleteDatabaseOption, newTableOption);
                    treeView.setContextMenu(options);
                }

                // delete table
                else {
                    MenuItem deleteTableOption = new MenuItem("delete table");
                    deleteTableOption.setOnAction( actionEvent -> {

                    });
                    options.getItems().add(deleteTableOption);
                    treeView.setContextMenu(options);
                }
            }
        }
    }
}
