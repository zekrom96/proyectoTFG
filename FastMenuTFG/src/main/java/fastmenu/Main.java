package fastmenu;

import classes.services.Supabase;
import controllers.Controlador;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.collections.ObservableList;
import models.Plato;


public class Main extends Application {

    Supabase supa;

    //TODO RECUERDA CAMBIAR LOS TOKEN EL DIA DE LA PRESENTACION DEL TFG!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //TODO Revisar los try-catch en todas las clases y manejas correctamente los errores
    //TODO Diseñar el apartado de moficicación o plantear como lo haré
    //TODO Diseñar una ventana con 4 o más diseños de plantilla para el pdf
    //TODO Dejar bonita la ventana de crear
    //TODO Crear un fichero de logs
    //TODO Crear pattern para el correo
    //TODO Revisar nombres de variables pdfs se suban con el nombre de la empresa o preguntarle por el nombre del menu
    //TODO URGENTE, HACER CAMPO CORREO EN EMPRESA QUE SEA VALOR UNICO, LO DEJO HECHO PERO MODIFICA EL CODIGO LO COMPRUEBE
    //TODO REVISAR CUANDO SE REGISTRA Y AGREGA UN PLATO DA ERROR


    @Override
    public void start(Stage stage) throws IOException {
        supa = new Supabase();
        Preferences preferences = Preferences.userRoot().node("fastmenu");
        String correoPreferences = preferences.get("logged_in_user_email", null);
        boolean estado_login = supa.comprobarEstadoCampoUsuarioLogueado(correoPreferences);
        vistaMenu(stage, estado_login);
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
                    FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaModificacion.fxml"));
                    Parent root = null;
                    try {
                        root = loader.load();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Scene nuevaScene = new Scene(root);
                    Controlador controlador = loader.getController();
                    Preferences preferences = Preferences.userRoot().node("fastmenu");
                    String correoShared = preferences.get("logged_in_user_email", null);
                    // Acción a realizar si se selecciona "Modificar"
                    System.out.println("Se seleccionó Modificar");
                    int idEmpresaActual = supa.obtenerIdEmpresaPorCorreo(correoShared);
                    List<String> nombresMenus = supa.obtenerNombresMenuPorIdEmpresa(idEmpresaActual);
                    ObservableList<String> data = FXCollections.observableArrayList(nombresMenus);

                    String menuElegido = mostrarNombresMenuEnDialogo(nombresMenus);
                    int idMenu = supa.obtenerIdMenuPorNombre(menuElegido);
                    System.out.println(idMenu);
                    List<Plato> listaPlatos = supa.obtenerPlatosPorIdMenu(idMenu);
                    ObservableList<String> nombresPlatos = FXCollections.observableArrayList();
                    for (Plato plato : listaPlatos) {
                        nombresPlatos.add(plato.getNombrePlato());
                    }
                    controlador.listaPlatosMenu.setItems(nombresPlatos);
                    controlador.listaPlatosMenu.refresh();

                    //System.out.println(nombresPlatos);
                    System.out.println("menu elegido: " + menuElegido);
                    controlador.obtenerCorreo(correoShared);
                    controlador.obtenerPlatosModificar(listaPlatos);
                    System.out.println(listaPlatos);
                    // Establecer la nueva escena en una nueva ventana
                    Stage nuevaVentana = new Stage();
                    nuevaVentana.setScene(nuevaScene);
                    nuevaVentana.show();
                    // Aquí puedes agregar la lógica para la acción de "Modificar"
                } else if (boton == botonCrear) {
                    // Acción a realizar si se selecciona "Crear"
                    System.out.println("Se seleccionó Crear");
                    // Aquí puedes agregar la lógica para la acción de "Crear"
                    try {
                        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaMenu.fxml"));
                        Parent root = loader.load();

                        Scene nuevaScene = new Scene(root);
                        Controlador controlador = loader.getController();
                        Preferences preferences = Preferences.userRoot().node("fastmenu");
                        String correoShared = preferences.get("logged_in_user_email", null);
                        //TODO REVISA ESTA LINEA URGENTE, AGREGA TODOS LOS PLATOS Y MENUS AL MISMO ID
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
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaLogin.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 449, 438);
            stage.setTitle("LoginZekrom");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        }
    }

    public String mostrarNombresMenuEnDialogo(List<String> nombresMenus) {
        // Crear el diálogo de selección
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(null, nombresMenus);
        dialogo.setTitle("Selección de Menú");
        dialogo.setHeaderText("Selecciona un Menú");
        dialogo.setContentText("Menús disponibles:");

        // Mostrar el diálogo y esperar a que el usuario seleccione una opción
        Optional<String> resultado = dialogo.showAndWait();

        // Verificar si el usuario seleccionó una opción y devolverla
        if (resultado.isPresent()) {
            return resultado.get(); // Devolver el menú seleccionado
        } else {
            return null; // Devolver null si el usuario cancela el diálogo
        }
    }


    public static void main(String[] args) {
        launch();
    }
}