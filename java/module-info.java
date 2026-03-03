module com.smsproject {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.smsproject to javafx.fxml;
    exports com.smsproject;
}
