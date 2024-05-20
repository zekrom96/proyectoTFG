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
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.collections.ObservableList;
import models.Plato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    //TODO Revisar try-catch y avisos
    //TODO Revisar pdf y código
    //TODO Que no puedo abrir la ventana de modificar si no tiene menus aun

    Supabase supa;
    public static final Logger log = LogManager.getLogger(Main.class);
    /*
    Variable se usa para saber si se cargo la ventana de login cuando el usuario intenta hacer a menu sin tener platos
    y se le redirige a la de crear platos
     */
    private boolean loginCargado = false;

    @Override
    public void start(Stage stage) {
        supa = new Supabase();
        Preferences preferencias = Preferences.userRoot().node("fastmenu");
        //Se recupera si había un correo guardado y su valor para iniciar una vista u otra
        String correoPreferences = preferencias.get("logged_in_user_email", null);
        boolean estado_login = supa.comprobarEstadoCampoUsuarioLogueado(correoPreferences);
        vistaMenu(estado_login);
        log.info("Iniciando aplicación...Aplicación iniciada");
    }

    private void vistaMenu(boolean estadoLogeado) {
        if (estadoLogeado) {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación de acción");
            alerta.setHeaderText("¿Qué acción deseas realizar?");
            alerta.setContentText("Elige una opción:");

            ButtonType botonModificar = new ButtonType("Modificar");
            ButtonType botonCrear = new ButtonType("Crear");

            alerta.getButtonTypes().setAll(botonModificar, botonCrear);

            alerta.showAndWait().ifPresent(boton -> {
                if (boton == botonModificar) {
                    if (loginCargado) {
                        loginCargado = false;
                    } else {
                        cargarVentanaModificacion();
                        log.info("El usuario accedio a la ventana de Modificación");
                    }
                } else if (boton == botonCrear) {
                    cargarVentanaCreacion();
                    log.info("El usuario accedio a la ventana de Creación");
                }
            });
        } else {
            cargarVentanaLogin();
            log.info("El usuario accedio a la ventana de Login");
        }
    }

    private void cargarVentanaModificacion() {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaModificacion.fxml"));
        cargarVentana(loader, false);
    }

    private void cargarVentanaCreacion() {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaCrear.fxml"));
        cargarVentana(loader, true);
    }

    private void cargarVentana(FXMLLoader loader, boolean ocultar) {
        try {
            Parent root = loader.load();
            Scene nuevaScene = new Scene(root);
            //Obtiene el controlador de la ventana
            Controlador controlador = loader.getController();
            Preferences preferences = Preferences.userRoot().node("fastmenu");
            //Obtiene el correo del usuario logueado
            String correoShared = preferences.get("logged_in_user_email", null);

            //Recupera el id de la empresa del correo del usuario logueado
            int idEmpresaActual = supa.obtenerIdEmpresaPorCorreo(correoShared);
            //Se usa la variable ocultar para evitar se vea la seleccion de menus al cargar la ventana de crear
            if (!ocultar) {
                //Recupera los nombres de los menus de la empresa del usuario logueado
                List<String> nombresMenus = supa.obtenerNombresMenuPorIdEmpresa(idEmpresaActual);
                //Si no tiene menus se le redirige a la ventana de crear
                if (nombresMenus.isEmpty()) {
                    cargarVentanaCreacion();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Alerta");
                    alert.setHeaderText("Alerta");
                    alert.setContentText("No tiene ningún menú creado, ha sido redirigido a crear");
                    alert.showAndWait();
                    loginCargado = true;
                    Main.log.warn("El usuario " + correoShared + " intento acceder a modificar sin tener menus" +
                            "redirigiendo a la ventana de crear");
                } else {
                    String menuElegido = mostrarNombresMenuEnDialogo(nombresMenus);
                    int idMenu = supa.obtenerIdMenuPorNombre(menuElegido);
                    //Recupera los platos de la empresa del usuario logueado
                    List<Plato> listaPlatos = supa.obtenerPlatosPorIdMenu(idMenu);
                    ObservableList<String> nombresPlatos = FXCollections.observableArrayList();
                    for (Plato plato : listaPlatos) {
                        nombresPlatos.add(plato.getNombrePlato());
                    }
                    controlador.listaPlatosMenu.setItems(nombresPlatos);
                    controlador.listaPlatosMenu.refresh();
                    controlador.obtenerPlatosModificar(listaPlatos);
                    controlador.obtenerMenu(menuElegido);
                }
            }
            controlador.obtenerCorreo(correoShared);
            if (loginCargado) {
                loginCargado = false;
            } else {
                Stage nuevaVentana = new Stage();
                nuevaVentana.setResizable(false);
                nuevaVentana.setScene(nuevaScene);
                nuevaVentana.show();
            }
        } catch (IOException e) {
            log.error("No se pudo cargar la ventana");
            throw new RuntimeException(e);
        }
    }

    private void cargarVentanaLogin() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaLogin.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 449, 438);
            Stage stage = new Stage();
            stage.setTitle("LoginZekrom");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String mostrarNombresMenuEnDialogo(List<String> nombresMenus) {
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(null, nombresMenus);
        dialogo.setTitle("Selección de Menú");
        dialogo.setHeaderText("Selecciona un Menú");
        dialogo.setContentText("Menús disponibles:");
        Optional<String> resultado = dialogo.showAndWait();
        return resultado.orElse(null);
    }

    public static void main(String[] args) {
        launch();
    }
}