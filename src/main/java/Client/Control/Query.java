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
            mainWindow.send(textArea.getText());
        else
            mainWindow.send(textArea.getSelectedText());
    }
}
