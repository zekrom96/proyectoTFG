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

//TODO Revisar try-catch y avisos
//TODO Volver a poner fichero logs

public class Main extends Application {
    String correoPreferences;
    boolean alerta2 = false;
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
        log.info("Iniciando aplicación...");
        supa = new Supabase();
        Preferences preferencias = Preferences.userRoot().node("fastmenu");
        //Se recupera si había un correo guardado y su valor para iniciar una vista u otra
        correoPreferences = preferencias.get("logged_in_user_email", null);
        log.info("Correo recuperado de preferencias: " + correoPreferences);
        boolean estado_login = supa.comprobarEstadoCampoUsuarioLogueado(correoPreferences);
        log.info("Estado de login comprobado: " + estado_login);
        vistaMenu(estado_login);
        log.info("Aplicación iniciada");
    }

    private void vistaMenu(boolean usuarioLogueado) {
        log.info("Mostrando vista de menú, usuario logueado: " + usuarioLogueado);
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
                    log.info("Botón 'Modificar' seleccionado");
                    if (loginCargado) {
                        log.info("Login ya cargado, estableciendo loginCargado a false");
                        loginCargado = false;
                    } else {
                        cargarVentanaModificacion();
                        log.info("El usuario accedió a la ventana de Modificación");
                    }
                } else if (boton == botonCrear) {
                    log.info("Botón 'Crear' seleccionado");
                    cargarVentanaCreacion();
                    log.info("El usuario accedió a la ventana de Creación");
                } else if (boton == botonCancelar) {
                    log.info("Botón 'Cancelar' seleccionado");
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
            log.info("El usuario accedió a la ventana de Login");
        }
    }

    private void cargarVentanaModificacion() {
        log.info("Cargando ventana de modificación");
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaModificacion.fxml"));
        cargarVentana(loader, false);
    }

    private void cargarVentanaCreacion() {
        log.info("Cargando ventana de creación");
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaCrear.fxml"));
        cargarVentana(loader, true);
    }

    private void cargarVentana(FXMLLoader loader, boolean ocultar) {
        log.info("Cargando ventana, ocultar: " + ocultar);
        try {
            Parent root = loader.load();
            Scene nuevaScene = new Scene(root);
            Controlador controlador = loader.getController();
            Preferences preferences = Preferences.userRoot().node("fastmenu");

            String correoShared = preferences.get("logged_in_user_email", null);
            log.info("Correo compartido recuperado: " + correoShared);
            //Recupera el id de la empresa del correo del usuario logueado
            int idEmpresaActual = supa.obtenerIdEmpresaPorCorreo(correoShared);
            log.info("ID de la empresa actual: " + idEmpresaActual);

            //Se usa la variable ocultar para evitar se vea la seleccion de menus al cargar la ventana de Crear
            if (!ocultar) {
                //Recupera los nombres de los menus de la empresa del usuario logueado
                List<String> nombresMenus = supa.obtenerNombresMenuPorIdEmpresa(idEmpresaActual);
                log.info("Nombres de los menús obtenidos: " + nombresMenus);
                //Si no tiene menus se le redirige a la ventana de crear
                if (nombresMenus.isEmpty()) {
                    log.warn("No hay menús, redirigiendo a la ventana de creación");
                    cargarVentanaCreacion();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Error del usuario");
                    alert.setHeaderText("No tienes menús");
                    alert.setContentText("Ha sido redirigido a crear");
                    alert.showAndWait();
                    loginCargado = true;
                    Main.log.warn("El usuario " + correoShared + " intentó acceder a modificar sin tener menús, redirigiendo a la ventana de crear");
                } else {
                    String menuElegido = mostrarNombresMenuEnDialogo(nombresMenus);
                    log.info("Menú elegido: " + menuElegido);
                    if (menuElegido != null) {
                        String nombreMenuCodificado = URLEncoder.encode(menuElegido, StandardCharsets.UTF_8);
                        int idMenu = supa.obtenerIdMenuPorNombre(nombreMenuCodificado);
                        log.info("ID del menú elegido: " + idMenu);
                        //Recupera los platos de la empresa del usuario logueado
                        List<Plato> listaPlatos = supa.obtenerPlatosPorIdMenu(idMenu);
                        log.info("Lista de platos obtenida: " + listaPlatos);
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
                        log.info("No se seleccionó ningún menú");
                        if (!alerta2) {
                            // Mostrar alerta de selección cancelada
                            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                            alerta.setTitle("Información");
                            alerta.setHeaderText("Selección cancelada");
                            alerta.setContentText("No se ha seleccionado ningún menú, volviendo al inicio de sesión...");
                            alerta.showAndWait();
                            cargarVentanaLogin();
                            loginCargado = true;
                        } else {
                            alerta2 = false;
                        }
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
            log.error("No se pudo cargar la ventana", e);
            throw new RuntimeException(e);
        }
    }

    public static void cargarVentanaLogin() {
        log.info("Cargando ventana de login");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaLogin.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 449, 438);
            Stage stage = new Stage();
            stage.setTitle("LoginZekrom");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            log.error("No se pudo cargar la ventana de login", e);
            throw new RuntimeException(e);
        }
    }

    public String mostrarNombresMenuEnDialogo(List<String> nombresMenus) {
        log.info("Mostrando diálogo de selección de menú");
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(null, nombresMenus);
        dialogo.setTitle("Selección de Menú");
        dialogo.setHeaderText("Selecciona un Menú");
        dialogo.setContentText("Menús disponibles:");

        ButtonType buttonTypeBorrar = new ButtonType("Borrar");
        dialogo.getDialogPane().getButtonTypes().clear(); // Clear any existing buttons
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, buttonTypeBorrar, ButtonType.CANCEL);

        dialogo.setResultConverter(dialogButton -> {
            if (dialogButton == buttonTypeBorrar) {
                log.info("Botón 'Borrar' seleccionado");
                if (dialogo.getSelectedItem() != null) {
                    log.info("Menú seleccionado para borrar: " + dialogo.getSelectedItem());
                    try {
                        String nombreMenuCodificado = URLEncoder.encode(dialogo.getSelectedItem(), StandardCharsets.UTF_8);

                        // Crear alerta de confirmación
                        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmacion.setTitle("Confirmación de borrado");
                        confirmacion.setHeaderText("Borrar Menú");
                        confirmacion.setContentText("¿Estás seguro de que deseas borrar el menú y todos sus platos?");

                        // Mostrar y esperar la respuesta del usuario
                        Optional<ButtonType> respuesta = confirmacion.showAndWait();
                        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
                            log.info("Confirmación de borrado aceptada");
                            // Si el usuario confirma, proceder con el borrado
                            supa.borrarPlatosDeMenu(supa.obtenerIdMenuPorNombre(nombreMenuCodificado), supa.obtenerIdEmpresaPorCorreo(correoPreferences));
                            supa.borrarMenu(nombreMenuCodificado, supa.obtenerIdEmpresaPorCorreo(correoPreferences));

                            // Mostrar alerta de borrado finalizado
                            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                            alerta.setTitle("Información");
                            alerta.setHeaderText("Borrado finalizado");
                            alerta.setContentText("Se borró el menú y sus platos, finalizando la aplicación...");
                            alerta.showAndWait();
                            Platform.exit();
                            alerta2 = true;
                            return null;
                        } else {
                            // Si el usuario cancela, no hacer nada
                            log.info("Confirmación de borrado cancelada");
                            Alert alertaCancelacion = new Alert(Alert.AlertType.INFORMATION);
                            alertaCancelacion.setTitle("Cancelación");
                            alertaCancelacion.setHeaderText("Borrado cancelado");
                            alertaCancelacion.setContentText("No se ha borrado el menú.");
                            alertaCancelacion.showAndWait();
                            alerta2 = false;
                        }
                    } catch (IOException e) {
                        log.error("Error al borrar el menú", e);
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
            return null;
        }
        return resultado.orElse(null); // Handle the "Aceptar" or "Cancelar" actions
    }

    public static void main(String[] args) {
        launch();
    }
}