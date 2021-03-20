package Client;

import Server.Attribute;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class NewTable implements Initializable {
    @FXML
    private MainWindow mainWindow;
    @FXML
    private TextField tableNameTextField;
    @FXML
    private TableView<Attribute> tableOfCols;
    @FXML
    private TableColumn<Attribute, String> nameCol, refTableCol, refColumnCol;
    @FXML
    private TableColumn<Attribute, Boolean> pkCol, fkCol, notNullCol, uqCol;
    @FXML
    private TextField nameTextField, refTableTextField, refColTextField;
    @FXML
    private CheckBox pkChkBox, fkChkBox, notNullChkBox, uqChkBox;

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void create(){
        mainWindow.addTable(tableNameTextField.getText(), tableOfCols.getItems());
        Stage stage = (Stage) tableNameTextField.getScene().getWindow();
        stage.close();
    }

    public void pkSelected() {
        if (pkChkBox.isSelected()) {
            notNullChkBox.setSelected(true);
            notNullChkBox.setDisable(true);
            uqChkBox.setSelected(true);
            uqChkBox.setDisable(true);
        }
        else {
            notNullChkBox.setDisable(false);
            uqChkBox.setDisable(false);
        }
    }

    public void addColumn() {
        tableOfCols.getItems().add(
                new Attribute(
                        nameTextField.getText(),
                        refTableTextField.getText(),
                        refColTextField.getText(),
                        pkChkBox.isSelected(),
                        fkChkBox.isSelected(),
                        notNullChkBox.isSelected(),
                        uqChkBox.isSelected()
                )
        );
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        refTableCol.setCellValueFactory(new PropertyValueFactory<>("refTable"));
        refColumnCol.setCellValueFactory(new PropertyValueFactory<>("refColumn"));
        pkCol.setCellValueFactory(new PropertyValueFactory<>("pK"));
        fkCol.setCellValueFactory(new PropertyValueFactory<>("fK"));
        notNullCol.setCellValueFactory(new PropertyValueFactory<>("notNull"));
        uqCol.setCellValueFactory(new PropertyValueFactory<>("unique"));
    }
}
