package com.example.fastmenuproyectotfg;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class FastMenu extends Application {

    //TODO RECUERDA CAMBIAR LOS TOKEN EL DIA DE LA PRESENTACION DEL TFG!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //TODO Revisar los try-catch en todas las clases y manejas correctamente los errores
    //TODO Comprobar los campos antes de enviar los datos
    //TODO Diseñar el apartado de moficicación o plantear como lo haré
    //TODO Diseñar una ventana con 4 o más diseños de plantilla para el pdf
    //TODO Dejar bonita la ventana de crear
    //TODO Crear un fichero de logs
    //TODO Crear pattern para el correo
    //TODO Revisar nombres de variables pdfs se suban con el nombre de la empresa o preguntarle por el nombre del menu

    //TODO La contraseña podría generar una de forma aleatoria, enviarsela a su correo, cambiarla en Supabase
    //TODO Y que en el siguiente login de alguna forma detecte si ha reseteado la pw el usuario y entonces
    //TODO Le pido que el introduzca una nueva pw y ya asigno eso nueva


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(FastMenu.class.getResource("vistaLogin.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 521, 294);
        stage.setTitle("LoginZekrom");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}