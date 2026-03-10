module com.zhizun.licenseadmin {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.bouncycastle.provider;

    exports com.zhizun.licenseadmin;
    exports com.zhizun.licenseadmin.controller;

    opens com.zhizun.licenseadmin.controller to javafx.fxml;
}
