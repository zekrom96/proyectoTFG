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

    //TODO Limpiar campos al agregar platos o modificar etc etc etc
    //TODO Revisar try-catch y avisos
    //TODO Revisar pdf y código, renombrar variables todas en español
    //TODO Pattern correo

    Supabase supa;
    public static final Logger log = LogManager.getLogger(Main.class);

    @Override
    public void start(Stage stage) throws IOException {
        supa = new Supabase();
        Preferences preferences = Preferences.userRoot().node("fastmenu");
        String correoPreferences = preferences.get("logged_in_user_email", null);
        boolean estado_login = supa.comprobarEstadoCampoUsuarioLogueado(correoPreferences);
        vistaMenu(estado_login);
        log.info("Iniciando aplicación...Aplicación iniciada");
    }

    private void vistaMenu(boolean valor) {
        if (valor) {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación de acción");
            alerta.setHeaderText("¿Qué acción deseas realizar?");
            alerta.setContentText("Elige una opción:");

            ButtonType botonModificar = new ButtonType("Modificar");
            ButtonType botonCrear = new ButtonType("Crear");

            alerta.getButtonTypes().setAll(botonModificar, botonCrear);

            alerta.showAndWait().ifPresent(boton -> {
                if (boton == botonModificar) {
                    log.info("El usuario accedio a la ventana de Modificación");
                    cargarVentanaModificacion();
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
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaMenu.fxml"));
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
            if (!ocultar) {
                //Recupera los nombres de los menus de la empresa del usuario logueado
                List<String> nombresMenus = supa.obtenerNombresMenuPorIdEmpresa(idEmpresaActual);
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

            controlador.obtenerCorreo(correoShared);
            //Muestra la ventana
            Stage nuevaVentana = new Stage();
            nuevaVentana.setScene(nuevaScene);
            nuevaVentana.show();
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