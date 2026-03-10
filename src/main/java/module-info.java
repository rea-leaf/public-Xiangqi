/**
 * 模块定义。
 * Java 模块声明，定义运行时依赖与导出包。
 */
open module Xiangqi {
    requires javafx.swing;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.sun.jna;
    requires javafx.media;
    requires com.sun.jna.platform;
    requires jnativehook;
    requires com.microsoft.onnxruntime;
    requires java.desktop;
    requires java.sql;
    requires org.bouncycastle.provider;

    exports com.chf.chess;
}
