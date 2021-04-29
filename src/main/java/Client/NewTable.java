package Client;

import Server.DbStructure.Attribute;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class NewTable implements Initializable {
    @FXML
    private MainWindow mainWindow;
    @FXML
    private TextField tableNameTextField;
    @FXML
    private TableView<Attribute> tableOfAttributes;
    @FXML
    private TableColumn<Attribute, String> nameCol, dataTypeCol, refTableCol, refAttributeCol;
    @FXML
    private TableColumn<Attribute, Boolean> pkCol, fkCol, notNullCol, uqCol;
    @FXML
    private TextField nameTextField, refTableTextField, refAttributeTextField;
    @FXML
    private ComboBox<String> dataTypeComboBox;
    @FXML
    private CheckBox pkChkBox, fkChkBox, notNullChkBox, uqChkBox;

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void create(){
        mainWindow.addTable(tableNameTextField.getText(), tableOfAttributes.getItems());
    }

    public void addAttribute() {
        tableOfAttributes.getItems().add(
                new Attribute(
                        nameTextField.getText(),
                        dataTypeComboBox.getValue(),
                        refTableTextField.getText(),
                        refAttributeTextField.getText(),
                        pkChkBox.isSelected(),
                        fkChkBox.isSelected(),
                        notNullChkBox.isSelected(),
                        uqChkBox.isSelected()
                )
        );
    }

    public void contextMenu(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteOption = new MenuItem("delete selected attributes");
        deleteOption.setOnAction(event -> {
            ObservableList<Attribute> allAttributes, selectedAttributes;
            allAttributes = tableOfAttributes.getItems();
            selectedAttributes = tableOfAttributes.getSelectionModel().getSelectedItems();
            selectedAttributes.forEach(allAttributes::remove);
        });
        contextMenu.getItems().add(deleteOption);
        tableOfAttributes.setContextMenu(contextMenu);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        dataTypeCol.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        refTableCol.setCellValueFactory(new PropertyValueFactory<>("refTable"));
        refAttributeCol.setCellValueFactory(new PropertyValueFactory<>("refColumn"));
        pkCol.setCellValueFactory(new PropertyValueFactory<>("pk"));
        fkCol.setCellValueFactory(new PropertyValueFactory<>("fk"));
        notNullCol.setCellValueFactory(new PropertyValueFactory<>("notNull"));
        uqCol.setCellValueFactory(new PropertyValueFactory<>("unique"));
    }
}
