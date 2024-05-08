module com.example.fastmenuproyectotfg {
    requires javafx.controls;
    requires javafx.fxml;
    requires itextpdf;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires java.sql;
    requires github.api;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires org.json;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.s3;
    requires software.amazon.awssdk.auth;
    requires java.mail;
    requires java.prefs;
    requires org.apache.logging.log4j;


    opens fastmenu to javafx.fxml;
    exports fastmenu;
    exports controllers;
    opens controllers to javafx.fxml;
}