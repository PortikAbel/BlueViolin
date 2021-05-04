package Client.Control;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class Query {
    private MainWindow mainWindow;

    @FXML
    private TextArea textArea;

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void run() {
        if (textArea.getSelectedText().equals(""))
            mainWindow.sendQuery(textArea.getText());
        else
            mainWindow.sendQuery(textArea.getSelectedText());
    }
}
