package Client.Control;

import javafx.scene.control.TextField;

public class ValidateTextField extends TextField {
    private boolean valid;

    public ValidateTextField(String dataType){
        super();
        valid = true;
        if ("int".equalsIgnoreCase(dataType)) {
            textProperty().addListener((observable, oldValue, newValue) ->
                    valid = newValue.matches("-?[0-9]*"));
        } else {
            textProperty().addListener((observable, oldValue, newValue) ->
                    valid = newValue.matches("^(\"[^\"]*\")|('[^']*')$"));
        }
    }

    public boolean isValid() {
        return valid;
    }
}
