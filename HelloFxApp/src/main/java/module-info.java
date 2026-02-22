module org.example.hellofxapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.hellofxapp to javafx.fxml;
    exports org.example.hellofxapp;
}