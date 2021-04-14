package Client;

import Server.Attribute;
import Server.Database;
import Server.Json;
import Server.Table;
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

    public String send(String msg){
        clientToServerWriter.println(msg);
        clientToServerWriter.println("");
        System.out.println("Message sent: "+msg);
        String resp;
        try {
            resp = serverToClientReader.readLine();
        } catch (IOException e) {
            resp = "";
        }
        System.out.println("Got response: "+resp);
        return resp;
    }

    public void exitApplication() {
        send("exit");
    }

    public void addDatabase(String name) {
        // CREATE DATABASE -database name-
        String resp = send("CREATE DATABASE " + name);
        if (resp.equals("OK"))
            selectedItem.getChildren().add(new TreeItem<>(name));
        else
            System.out.println(resp);
    }

    private void deleteDatabase(TreeItem<String> database) {
        // DELETE DATABASE -database name-
        String resp = send("DELETE DATABASE " + database.getValue());
        if (resp.equals("OK"))
            database.getParent().getChildren().remove(database);
        else
            System.out.println(resp);
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
        String resp = send(command);
        if (resp.equals("OK"))
            selectedItem.getChildren().add(new TreeItem<>(name));
        else
            System.out.println(resp);
    }

    private void deleteTable(TreeItem<String> table) {
        String resp = send("DELETE TABLE " + table.getValue()
                + " " + table.getParent().getValue()
        );

        if (resp.equals("OK"))
            table.getParent().getChildren().remove(table);
        else
            System.out.println(resp);
    }

    public void insertRows(ObservableList<ObservableList<SimpleStringProperty>> rows) {
        rows.forEach(row -> {
            String resp = send(String.format(
                    "INSERT INTO %s VALUES %s;",
                    selectedItem.getValue(),
                    String.format("(%s)",
                            row.stream()
                                    .map(SimpleStringProperty::getValue)
                                    .collect(Collectors.joining(","))
                    ))
            );
            if (!resp.equals("OK"))
                System.out.println(resp);
        });
    }

    public void createIndex(String indexName, String columnNames) {
        String resp = send(String.format("CREATE INDEX %s ON %s(%s)",
                indexName,
                selectedItem.getValue(),
                columnNames)
        );
        System.out.printf("CREATE INDEX %s ON %s(%s)%n",
                indexName,
                selectedItem.getValue(),
                columnNames);
        System.out.println(resp);
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
                    if (!selectedDatabase.equals(selectedItem.getValue())) {
                        selectedDatabase = selectedItem.getValue();
                        String resp = send(String.format("USE %s;", selectedDatabase));
                        if (!resp.equals("OK"))
                            System.out.println(resp);
                    }
                    MenuItem deleteDatabaseOption = new MenuItem("delete database");
                    deleteDatabaseOption.setOnAction( actionEvent -> deleteDatabase(selectedItem));

                    MenuItem newTableOption = new MenuItem("new table");
                    newTableOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("NewTable.fxml"));
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
                        String resp = send(String.format("USE %s;", selectedDatabase));
                        if (!resp.equals("OK"))
                            System.out.println(resp);
                    }

                    MenuItem deleteTableOption = new MenuItem("delete table");
                    deleteTableOption.setOnAction( actionEvent -> deleteTable(selectedItem));

                    MenuItem createIndexOption = new MenuItem("create index");
                    createIndexOption.setOnAction( actionEvent -> {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("CreateIndexWindow.fxml"));
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
                        loader.setLocation(getClass().getResource("InsertRowsWindow.fxml"));
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
