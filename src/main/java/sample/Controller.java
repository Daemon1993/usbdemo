package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;




public class Controller {

    @FXML
    Button bt_action;

    @FXML
    public void action(ActionEvent event){
        bt_action.setText("BBB");
    }
}
