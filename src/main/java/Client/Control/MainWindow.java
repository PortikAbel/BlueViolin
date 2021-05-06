package Client.Control;

import Server.DbStructure.Attribute;
import Server.DbStructure.Database;
import Server.Json;
import Server.DbStructure.Table;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
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
import java.util.stream.Collectors;

public class MainWindow implements Initializable {
    private BufferedReader serverToClientReader;
    private PrintWriter clientToServerWriter;

    private static TreeItem<String> selectedItem;
    private String selectedDatabase = "master";

    @FXML
    private TreeView<String> treeView;
    @FXML
    private BorderPane borderPane;
    @FXML
    private WebView responseView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // initializing socket connections with server
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

        // building the tree view of databases & tables
        TreeItem<String> root = new TreeItem<>("databases");
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

    public boolean send(String msg) {
        clientToServerWriter.println(msg);
        clientToServerWriter.println(".");
        StringBuilder respBuilder = new StringBuilder();
        String inputLine;
        try {
            while (!(inputLine = serverToClientReader.readLine()).equals(".")) {
                respBuilder.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            return false;
        }
        String resp = respBuilder.toString();
        responseView.getEngine().loadContent(resp);

        return !resp.startsWith("error");
    }

    public void exitApplication() {
        send("exit");
    }

    public void addDatabase(String name) {
        // CREATE DATABASE -database name-
        if (send("CREATE DATABASE " + name))
            selectedItem.getChildren().add(new TreeItem<>(name));
    }

    private void deleteDatabase(TreeItem<String> database) {
        // DELETE DATABASE -database name-
        if (send("DELETE DATABASE " + database.getValue()))
            database.getParent().getChildren().remove(database);
    }

    public void addTable(String name, ObservableList<Attribute> attributes) {
        // CREATE TABLE -table name- (
        //  -column name- -column type- -NOT NULL- -UNIQUE- -FOREIGN KEY REFERENCES table_name(column_name)-,
        //  );
        String command = "CREATE TABLE ";
        command += name;
        command += " (\n";
        command += attributes.stream().map(attribute -> {
            String attrDef = attribute.getName();
            attrDef += " " + attribute.getDataType();
            if (attribute.isNotNull())
                attrDef += " NOT NULL";
            if (attribute.isUnique())
                attrDef += " UNIQUE";
            if (attribute.isFk())
                attrDef += String.format(" REFERENCES %s(%s)",
                        attribute.getRefTable(),
                        attribute.getRefColumn()
                );
            return attrDef;
        }).collect(Collectors.joining(",\n"));
        command += String.format(",\nPRIMARY KEY (%s)",
                attributes.stream()
                        .filter(Attribute::isPk)
                        .map(Attribute::getName)
                        .collect(Collectors.joining(","))
        );
        command += "\n);";

        if (send(command))
            selectedItem.getChildren().add(new TreeItem<>(name));
    }

    private void deleteTable(TreeItem<String> table) {
        String command = "DELETE TABLE " + table.getValue()
                + " " + table.getParent().getValue();

        if (send(command))
            table.getParent().getChildren().remove(table);
    }

    public void insertRows(ObservableList<ObservableList<SimpleStringProperty>> rows) {
        String command = rows.stream()
                .map(row -> String.format(
                        "INSERT INTO %s VALUES %s;",
                        selectedItem.getValue(),
                        String.format("(%s)",
                                row.stream()
                                        .map(SimpleStringProperty::getValue)
                                        .collect(Collectors.joining(","))
                        )))
                .collect(Collectors.joining());
        send(command);
    }

    public void createIndex(String indexName, String columnNames) {
        String command = String.format("CREATE INDEX %s ON %s(%s)",
                indexName,
                selectedItem.getValue(),
                columnNames);
        send(command);
    }

    public void newQuery() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("../View/Query.fxml"));
        try {
            AnchorPane queryDialogue = loader.load();
            Query queryController = loader.getController();
            queryController.setMainWindow(MainWindow.this);
            borderPane.setCenter(queryDialogue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MouseEventHandler implements EventHandler<MouseEvent>
    {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                ContextMenu options = new ContextMenu();
                selectedItem = treeView.getSelectionModel().getSelectedItem();

                //  new database
                if (selectedItem.getValue().equals("databases")) {
                    MenuItem newDatabaseOption = new MenuItem("new database");
                    newDatabaseOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("../View/NewDatabase.fxml"));
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
                    if (!selectedDatabase.equals(selectedItem.getValue())) {
                        selectedDatabase = selectedItem.getValue();
                        send(String.format("USE %s;", selectedDatabase));
                    }
                    MenuItem deleteDatabaseOption = new MenuItem("delete database");
                    deleteDatabaseOption.setOnAction( actionEvent -> deleteDatabase(selectedItem));

                    MenuItem newTableOption = new MenuItem("new table");
                    newTableOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("../View/NewTable.fxml"));
                        try {
                            AnchorPane newTableDialogue = loader.load();
                            NewTable newTableController = loader.getController();
                            newTableController.setMainWindow(MainWindow.this);
                            borderPane.setCenter(newTableDialogue);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    options.getItems().addAll(deleteDatabaseOption, newTableOption);
                    treeView.setContextMenu(options);
                }

                // delete table / create index / insert rows
                else {
                    if (!selectedDatabase.equals(selectedItem.getParent().getValue())) {
                        selectedDatabase = selectedItem.getParent().getValue();
                        send(String.format("USE %s;", selectedDatabase));
                    }

                    MenuItem deleteTableOption = new MenuItem("delete table");
                    deleteTableOption.setOnAction( actionEvent -> deleteTable(selectedItem));

                    MenuItem createIndexOption = new MenuItem("create index");
                    createIndexOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("../View/CreateIndexWindow.fxml"));
                        try {
                            AnchorPane createIndexDialogue = loader.load();
                            CreateIndexWindow createIndexController = loader.getController();
                            createIndexController.setMainWindow(MainWindow.this);
                            borderPane.setCenter(createIndexDialogue);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    MenuItem insertRowsOption = new MenuItem("insert rows");
                    insertRowsOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("../View/InsertRowsWindow.fxml"));
                        try {
                            AnchorPane insertRowsDialogue = loader.load();
                            InsertRowsWindow insertRowsController = loader.getController();
                            insertRowsController.setMainWindow(MainWindow.this);
                            borderPane.setCenter(insertRowsDialogue);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    options.getItems().addAll(deleteTableOption, createIndexOption, insertRowsOption);
                    treeView.setContextMenu(options);
                }
            }
        }
    }

    static public TreeItem<String> getSelectedItem(){
        return selectedItem;
    }
}
