package application;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {

    @FXML
    private Button button;

    @FXML
    private void handleClick() {
        System.out.println("Кнопка нажата!");
    }
}