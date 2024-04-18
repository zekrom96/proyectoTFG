package com.example.fastmenuproyectotfg;

import Controladores.Controlador;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.prefs.Preferences;

public class FastMenu extends Application {

    Supabase supa;
    //Sesion sesion = new Sesion();
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
    //TODO URGENTE, HACER CAMPO CORREO EN EMPRESA QUE SEA VALOR UNICO, LO DEJO HECHO PERO MODIFICA EL CODIGO LO COMPRUEBE
    @Override
    public void start(Stage stage) throws IOException {
        supa = new Supabase();
        //sesion = sesion.getInstance();
        Preferences preferences = Preferences.userRoot().node("com.example.myapp");
        String correoShared = preferences.get("logged_in_user_email", null);
        boolean estado_login = supa.comprobarEstadoCampoUsuarioLogueado(correoShared);
        //sesion.setUsuarioLogueado(true);
        //sesion.getCorreoLogueado();
        vistaMenu(stage, estado_login);
        //System.out.println(sesion.getUsuarioLogueado());
    }

    private void vistaMenu(Stage stage, boolean valor) throws IOException {
        if (valor) {
            // Crear una alerta con tipo de alerta de confirmación
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación de acción");
            alerta.setHeaderText("¿Qué acción deseas realizar?");
            alerta.setContentText("Elige una opción:");

            // Agregar botones al cuadro de diálogo
            ButtonType botonModificar = new ButtonType("Modificar");
            ButtonType botonCrear = new ButtonType("Crear");

            alerta.getButtonTypes().setAll(botonModificar, botonCrear);

            // Mostrar la alerta y esperar a que se seleccione una opción
            alerta.showAndWait().ifPresent(boton -> {
                if (boton == botonModificar) {
                    // Acción a realizar si se selecciona "Modificar"
                    System.out.println("Se seleccionó Modificar");
                    // Aquí puedes agregar la lógica para la acción de "Modificar"
                } else if (boton == botonCrear) {
                    // Acción a realizar si se selecciona "Crear"
                    System.out.println("Se seleccionó Crear");
                    // Aquí puedes agregar la lógica para la acción de "Crear"
                    try {
                        FXMLLoader loader = new FXMLLoader(FastMenu.class.getResource("vistaMenu.fxml"));
                        Parent root = loader.load();

                        Scene nuevaScene = new Scene(root);
                        Controlador controlador = loader.getController();
                        Preferences preferences = Preferences.userRoot().node("com.example.myapp");
                        String correoShared = preferences.get("logged_in_user_email", null);
                        controlador.obtenerCorreo(correoShared);
                        // Establecer la nueva escena en una nueva ventana
                        Stage nuevaVentana = new Stage();
                        nuevaVentana.setScene(nuevaScene);
                        nuevaVentana.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            FXMLLoader fxmlLoader = new FXMLLoader(FastMenu.class.getResource("vistaLogin.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 521, 294);
            stage.setTitle("LoginZekrom");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        }
    }


    public static void main(String[] args) {
        launch();
    }
}