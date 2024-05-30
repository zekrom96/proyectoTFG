package fastmenu;

import classes.services.Supabase;
import controllers.Controlador;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    //TODO Mejorar logica nombreMenu?
    //TODO Borrar menu?
    String correoPreferences;
    Supabase supa;
    public static final Logger log = LogManager.getLogger(Main.class);
    /*
    loginCargado:
    Variable que se usa para saber si se cargo correctamente la ventana de login en caso de que el usuario intente
    acceder a la vista de modificar platos sin tener menus y ha sido redirigido a la de crear platos.
     */
    private boolean loginCargado = false;

    @Override
    public void start(Stage stage) {
        supa = new Supabase();
        Preferences preferencias = Preferences.userRoot().node("fastmenu");
        //Se recupera si había un correo guardado y su valor para iniciar una vista u otra
        correoPreferences = preferencias.get("logged_in_user_email", null);
        boolean estado_login = supa.comprobarEstadoCampoUsuarioLogueado(correoPreferences);
        vistaMenu(estado_login);
        log.info("Iniciando aplicación...Aplicación iniciada");
    }

    private void vistaMenu(boolean usuarioLogueado) {
        if (usuarioLogueado) {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación de acción");
            alerta.setHeaderText("¿Qué acción deseas realizar?");
            alerta.setContentText("Elige una opción:");

            ButtonType botonModificar = new ButtonType("Modificar");
            ButtonType botonCrear = new ButtonType("Crear");
            ButtonType botonCancelar = new ButtonType("Cancelar");

            alerta.getButtonTypes().setAll(botonModificar, botonCrear, botonCancelar);

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
                } else if (boton == botonCancelar) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Informacion");
                    alert.setHeaderText("Cancelacion de seleccion");
                    alert.setContentText("Ha sido redirigido al login");
                    alert.showAndWait();
                    cargarVentanaLogin();
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
            Controlador controlador = loader.getController();
            Preferences preferences = Preferences.userRoot().node("fastmenu");

            String correoShared = preferences.get("logged_in_user_email", null);
            //Recupera el id de la empresa del correo del usuario logueado
            int idEmpresaActual = supa.obtenerIdEmpresaPorCorreo(correoShared);

            //Se usa la variable ocultar para evitar se vea la seleccion de menus al cargar la ventana de Crear
            if (!ocultar) {
                //Recupera los nombres de los menus de la empresa del usuario logueado
                List<String> nombresMenus = supa.obtenerNombresMenuPorIdEmpresa(idEmpresaActual);
                //Si no tiene menus se le redirige a la ventana de crear
                if (nombresMenus.isEmpty()) {
                    cargarVentanaCreacion();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Error del usuario");
                    alert.setHeaderText("No tienes menús");
                    alert.setContentText("Ha sido redirigido a crear");
                    alert.showAndWait();
                    loginCargado = true;
                    Main.log.warn("El usuario " + correoShared + " intento acceder a modificar sin tener menus" +
                            "redirigiendo a la ventana de crear");
                } else {
                    String menuElegido = mostrarNombresMenuEnDialogo(nombresMenus);
                    if (menuElegido != null) {
                        String nombreMenuCodificado = URLEncoder.encode(menuElegido, StandardCharsets.UTF_8);
                        int idMenu = supa.obtenerIdMenuPorNombre(nombreMenuCodificado);
                        //Recupera los platos de la empresa del usuario logueado
                        List<Plato> listaPlatos = supa.obtenerPlatosPorIdMenu(idMenu);
                        ObservableList<String> nombresPlatos = FXCollections.observableArrayList();
                        for (Plato plato : listaPlatos) {
                            nombresPlatos.add(plato.getNombrePlato());
                        }
                        //Al controlador de la vista se le pasan los datos que requiere
                        controlador.listaPlatosMenu.setItems(nombresPlatos);
                        controlador.listaPlatosMenu.refresh();
                        controlador.obtenerPlatosModificar(listaPlatos);
                        controlador.obtenerMenu(menuElegido);
                    } else {
                        // Mostrar alerta de selección cancelada
                        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                        alerta.setTitle("Información");
                        alerta.setHeaderText("Selección cancelada");
                        alerta.setContentText("No se ha seleccionado ningún menú, volviendo al inicio de sesión...");
                        alerta.showAndWait();
                        cargarVentanaLogin();
                        loginCargado = true;
                    }
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

    public static void cargarVentanaLogin() {
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

        ButtonType buttonTypeBorrar = new ButtonType("Borrar");
        dialogo.getDialogPane().getButtonTypes().clear(); // Clear any existing buttons
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, buttonTypeBorrar, ButtonType.CANCEL);

        dialogo.setResultConverter(dialogButton -> {
            if (dialogButton == buttonTypeBorrar) {
                if (dialogo.getSelectedItem() != null) {
                    System.out.println(dialogo.getSelectedItem());
                    try {
                        supa.borrarPlatosDeMenu(supa.obtenerIdMenuPorNombre(dialogo.getSelectedItem()), supa.obtenerIdEmpresaPorCorreo(correoPreferences));
                        supa.borrarMenu(dialogo.getSelectedItem(), supa.obtenerIdEmpresaPorCorreo(correoPreferences));
                        // Mostrar alerta de selección cancelada
                        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                        alerta.setTitle("Información");
                        alerta.setHeaderText("Borrado finalizado");
                        alerta.setContentText("Se borro el menu y sus platos finalizando la aplicacion...");
                        alerta.showAndWait();
                        Platform.exit();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (dialogButton == ButtonType.OK) {
                return dialogo.getSelectedItem();
            } else {
                return null;
            }
            return null;
        });

        Optional<String> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get().equals("Borrar")) {
            return null; // Handle the "Borrar" action
        }
        return resultado.orElse(null); // Handle the "Aceptar" or "Cancelar" actions
    }

    public static void main(String[] args) {
        launch();
    }
}