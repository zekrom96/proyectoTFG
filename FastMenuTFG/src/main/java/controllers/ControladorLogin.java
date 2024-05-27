package controllers;

import classes.utils.CifradoyDescifrado;
import classes.services.Correo;
import classes.utils.GeneradorPassword;
import classes.services.Supabase;
import fastmenu.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import models.Empresa;
import models.Plato;
import models.Usuario;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class ControladorLogin implements Initializable {
    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");
    private Properties props = new Properties();
    private Supabase supa;
    private CifradoyDescifrado crypt;
    private Correo correo = new Correo();
    public TextField textfieldCorreo, textfieldPw;
    public Button btnRegistrase, btnLogin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            props.load(getClass().getResourceAsStream("/properties/configuraciones.properties"));
            System.out.println("Archivo properties cargado correctamente.");
            crypt = new CifradoyDescifrado(props.getProperty("secret_key"));
        } catch (IOException e) {
            System.err.println("Error al cargar el archivo properties: " + e.getMessage());
            e.printStackTrace();
        }
        textfieldCorreo.setText("davidpeka20@gmail.com");
        textfieldPw.setText("root996");
        supa = new Supabase();
    }

    // Método para mostrar el dialogo de registro
    public void onClickBtnRegistrarse(MouseEvent mouseEvent) throws Exception {
        String[] resultado = mostrarDialogoRegistro();
        if (resultado != null) {
            String correo = resultado[0];
            String pw = resultado[1];
            String nombreEmpresa = resultado[2];
            //Antes de registrar un nuevo usuario, compruebo que el correo no exista en la base de datos
            //Si no existe, creo el usuario en la base de datos
            boolean existecorreo = supa.comprobarExisteCorreo(correo);
            if (existecorreo) {
                System.out.println("El correo ya existe en la bd");
                // El correo ya existe en la base de datos
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("Correo Existente");
                alerta.setHeaderText(null);
                alerta.setContentText("El correo ya está registrado en la base de datos.");
                alerta.showAndWait();
            } else {
                System.out.println("El correo no existe en la bd");
                String pwCifrada = crypt.encriptar(pw);
                Usuario nuevoUsuario = new Usuario(correo, pwCifrada);
                supa.crearUsuario(nuevoUsuario);
                Empresa empresa = new Empresa(nombreEmpresa);
                supa.agregarEmpresa(empresa, correo);
                //Una vez creado el usuario y la empresa en la base de datos, se accede directamente a crear platos
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaCrear.fxml"));
                    Parent root = fxmlLoader.load();
                    Scene nuevaScene = new Scene(root);
                    Controlador controlador = fxmlLoader.getController();
                    // Aquí le paso al controlador de la clase en la que creo los platos, el correo del usuario registrado
                    controlador.obtenerCorreo(correo);
                    System.out.println();
                    Stage ventanaActual = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
                    ventanaActual.close();
                    Stage nuevaVentana = new Stage();
                    nuevaVentana.setScene(nuevaScene);
                    nuevaVentana.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onClickBtnLogin() {
        login(textfieldCorreo.getText(), textfieldPw.getText());
    }


    //Muestra el dialogo de registro para un usuario
    public String[] mostrarDialogoRegistro() {
        // Crear el formulario
        GridPane grid = crearFormularioRegistro();
        // Crear el diálogo
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Registro");
        // Agregar 2 botones uno con el texto Registrar y el otro Cancelar
        ButtonType registrarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registrarButtonType, ButtonType.CANCEL);
        // Asignar el gridPane al diálogo
        dialog.getDialogPane().setContent(grid);
        // Obtener los campos de texto y el botón "Registrar"
        TextField correoTextField = (TextField) grid.lookup("#correoTextField");
        PasswordField pw = (PasswordField) grid.lookup("#pw");
        PasswordField confirmarPw = (PasswordField) grid.lookup("#confirmarPw");
        TextField nombreEmpresaTextField = (TextField) grid.lookup("#nombreEmpresaTextField");
        Node registrarButton = dialog.getDialogPane().lookupButton(registrarButtonType);
        registrarButton.setDisable(true);
        // Listeners para habilitar/deshabilitar el botón "Registrar"
        agregarListenersRegistro(correoTextField, pw, confirmarPw, nombreEmpresaTextField, registrarButton);
        // Convertir el resultado del diálogo en un array de strings al hacer clic en el botón "Registrar"
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                String correo = correoTextField.getText();
                String pass = pw.getText();
                String nombreEmpresa = nombreEmpresaTextField.getText();
                return new String[]{correo, pass, nombreEmpresa};
            }
            return null;
        });
        // Mostrar el diálogo y devolver el resultado
        Optional<String[]> result = dialog.showAndWait();
        return result.orElse(null);
    }


    // Dialogo y logica para recuperar la contraseña de un usuario
    public void onClickRecuperar() {
        // Crear el diálogo
        Alert dialogo = new Alert(Alert.AlertType.NONE);
        dialogo.setTitle("Recuperar Contraseña");
        dialogo.setHeaderText("Introduce tu correo electrónico");
        // Crear los campos de texto
        TextField correoTextField = new TextField();
        correoTextField.setPromptText("Correo electrónico");
        // Crear el layout del diálogo y añadir los nodos
        GridPane grid = new GridPane();
        grid.add(correoTextField, 0, 0);
        // Asignar el layout al diálogo
        dialogo.getDialogPane().setContent(grid);
        // Añadir botones de acción
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        // Obtener el botón "OK" y deshabilitarlo inicialmente
        dialogo.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        // Habilitar el botón "OK" cuando se introduce un correo válido
        correoTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            dialogo.getDialogPane().lookupButton(ButtonType.OK).setDisable(!validarCorreo(newValue.trim()));
        });
        // Mostrar el diálogo y esperar a que el usuario interactúe con él
        dialogo.showAndWait().ifPresent(resultado -> {
            if (resultado == ButtonType.OK) {
                String pwTemporal = GeneradorPassword.generarPassword();
                try {
                    boolean correoEncontrado = supa.comprobarExisteCorreo(correoTextField.getText());
                    if (correoEncontrado) {
                        correo.enviarGmail(props.getProperty("cuenta_correo"), props.getProperty("keygmail"),
                                correoTextField.getText(), pwTemporal);

                        String pwTemporalCifrada = crypt.encriptar(pwTemporal);
                        Usuario nuevoUsuario = new Usuario(correoTextField.getText(), pwTemporalCifrada);
                        supa.modificarPassword(nuevoUsuario);
                        supa.modificarCampoUsuarioRestablecerPw(correoTextField.getText(), true);
                    } else {
                        // Crear una alerta de correo no encontrado
                        Alert alertaCorreoNoEncontrado = new Alert(Alert.AlertType.ERROR);
                        alertaCorreoNoEncontrado.setTitle("Correo No Encontrado");
                        alertaCorreoNoEncontrado.setHeaderText("Correo electrónico no encontrado");
                        alertaCorreoNoEncontrado.setContentText("El correo electrónico introducido no está registrado en el sistema.");
                        alertaCorreoNoEncontrado.showAndWait();
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    //********************************************METODOS PRIVADOS****************************************************//

    private void login(String correo, String pw) {
        Usuario usuario = new Usuario(correo, pw);
        if (supa.iniciarSesion(usuario)) {
            /*
            Se comprueba si el usuario solicito un cambio de contraseña, si lo hizo en el proximo inicio de sesión,
            se comprueba un valor en la bd, si es true, se le solicitara una nueva contraseña, en caso contrario,
            hara un login normal.
             */
            if (supa.comprobarEstadoCampoRestablecerPw(usuario)) {
                PasswordField campoPw = new PasswordField();
                campoPw.setPromptText("Nueva Contraseña");
                PasswordField campoConfirmar = new PasswordField();
                campoConfirmar.setPromptText("Confirmar Contraseña");

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("Nueva Contraseña:"), 0, 0);
                grid.add(campoPw, 1, 0);
                grid.add(new Label("Confirmar Contraseña:"), 0, 1);
                grid.add(campoConfirmar, 1, 1);

                Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
                dialogo.setTitle("Cambiar Contraseña");
                dialogo.setHeaderText("Por favor ingresa tu nueva contraseña.");
                dialogo.getDialogPane().setContent(grid);

                ButtonType botonConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
                dialogo.getButtonTypes().setAll(botonConfirmar, ButtonType.CANCEL);

                // Deshabilitar el botón de confirmar inicialmente
                dialogo.getDialogPane().lookupButton(botonConfirmar).setDisable(true);

                // Listener para habilitar el botón solo cuando las contraseñas coinciden y no están vacías
                campoPw.textProperty().addListener((observable, oldValue, newValue) -> {
                    String pw1 = campoPw.getText();
                    String pw2 = campoConfirmar.getText();
                    dialogo.getDialogPane().lookupButton(botonConfirmar).setDisable(pw1.isEmpty() || !pw1.equals(pw2));
                });

                campoConfirmar.textProperty().addListener((observable, oldValue, newValue) -> {
                    String pw1 = campoPw.getText();
                    String pw2 = campoConfirmar.getText();
                    dialogo.getDialogPane().lookupButton(botonConfirmar).setDisable(pw1.isEmpty() || !pw1.equals(pw2));
                });

                dialogo.showAndWait().ifPresent(boton -> {
                    if (boton == botonConfirmar) {
                        String pwTemporalCifrada = crypt.encriptar(campoPw.getText());
                        Usuario nuevoUsuario = new Usuario(correo, pwTemporalCifrada);
                        supa.modificarPassword(nuevoUsuario);
                        supa.modificarCampoUsuarioRestablecerPw(correo, false);
                        System.out.println("Contraseña cambiada exitosamente.");
                        vistaMenu();
                        guardarEstadoLoginPreferences(correo);
                        supa.modificarCampoUsuarioLogueado(correo, true);
                    }
                });
            } else {
                guardarEstadoLoginPreferences(textfieldCorreo.getText());
                supa.modificarCampoUsuarioLogueado(textfieldCorreo.getText(), true);
                vistaMenu();
            }
        } else {
            // Crear una alerta de correo no encontrado
            Alert alertaCorreoNoEncontrado = new Alert(Alert.AlertType.ERROR);
            alertaCorreoNoEncontrado.setTitle("Datos incorrectos");
            alertaCorreoNoEncontrado.setHeaderText("Datos incorrectos");
            alertaCorreoNoEncontrado.setContentText("Los datos introducidos no corresponden a ningun usuario");
            alertaCorreoNoEncontrado.showAndWait();
        }
    }

    //Guarda el estado del usuario logueado en la base de datos
    private void guardarEstadoLoginPreferences(String correoUsuario) {
        Preferences preferences = Preferences.userRoot().node("fastmenu");
        preferences.put("logged_in_user_email", correoUsuario);
    }

    //Metodo para abrir la ventana de creación o de modificación
    private void vistaMenu() {
        // Cerrar la ventana actual (ventana de login)
        Stage ventanaActual = (Stage) btnLogin.getScene().getWindow();
        ventanaActual.close();

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
                System.out.println("Se seleccionó Modificar");
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaModificacion.fxml"));
                Parent root;
                try {
                    root = loader.load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Scene nuevaScene = new Scene(root);
                Controlador controlador = loader.getController();
                Preferences preferences = Preferences.userRoot().node("fastmenu");

                String correoShared = preferences.get("logged_in_user_email", null);
                int idEmpresaActual = supa.obtenerIdEmpresaPorCorreo(correoShared);
                List<String> nombresMenus = supa.obtenerNombresMenuPorIdEmpresa(idEmpresaActual);
                if(nombresMenus.isEmpty()) {
                    // Mostrar alerta de selección cancelada
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Información");
                    alert.setHeaderText("Selección cancelada");
                    alert.setContentText("No puedes acceder a Modificar, no tienes aun menus, redirigiendo a crear...");
                    alert.showAndWait();
                    try {
                        FXMLLoader loaderr = new FXMLLoader(Main.class.getResource("/views/vistaCrear.fxml"));
                        Parent roott = loaderr.load();

                        Scene nuevaScenee = new Scene(roott);
                        Controlador controladore = loaderr.getController();
                        Preferences pref = Preferences.userRoot().node("fastmenu");
                        String correoShar = pref.get("logged_in_user_email", null);
                        controladore.obtenerCorreo(correoShar);
                        Stage nuevaVentana = new Stage();
                        nuevaVentana.setScene(nuevaScenee);
                        nuevaVentana.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String menuElegido = mostrarNombresMenuEnDialogo(nombresMenus);
                    if (menuElegido != null) {
                        int idMenu = supa.obtenerIdMenuPorNombre(menuElegido);
                        System.out.println(idMenu);

                        List<Plato> listaPlatos = supa.obtenerPlatosPorIdMenu(idMenu);

                        ObservableList<String> nombresPlatos = FXCollections.observableArrayList();
                        for (Plato plato : listaPlatos) {
                            nombresPlatos.add(plato.getNombrePlato());
                        }
                        controlador.listaPlatosMenu.setItems(nombresPlatos);
                        controlador.listaPlatosMenu.refresh();
                        System.out.println("Menu elegido: " + menuElegido);
                        controlador.obtenerCorreo(correoShared);
                        controlador.obtenerPlatosModificar(listaPlatos);
                        controlador.obtenerMenu(menuElegido);
                        System.out.println(listaPlatos);
                        // Establecer la nueva escena en una nueva ventana
                        Stage nuevaVentana = new Stage();
                        nuevaVentana.setScene(nuevaScene);
                        nuevaVentana.show();
                    }
                }
            } else if (boton == botonCrear) {
                System.out.println("Se seleccionó Crear");
                try {
                    FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/vistaCrear.fxml"));
                    Parent root = loader.load();

                    Scene nuevaScene = new Scene(root);
                    Controlador controlador = loader.getController();
                    Preferences preferences = Preferences.userRoot().node("fastmenu");
                    String correoShared = preferences.get("logged_in_user_email", null);
                    controlador.obtenerCorreo(correoShared);
                    Stage nuevaVentana = new Stage();
                    nuevaVentana.setScene(nuevaScene);
                    nuevaVentana.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Muestra el menú a elegir en un dialogo para luego modificarlo
    private String mostrarNombresMenuEnDialogo(List<String> nombresMenus) {
        // Crear el diálogo de selección
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(null, nombresMenus);
        dialogo.setTitle("Selección de Menú");
        dialogo.setHeaderText("Selecciona un Menú");
        dialogo.setContentText("Menús disponibles:");
        // Mostrar el diálogo y esperar a que el usuario seleccione una opción
        Optional<String> resultado = dialogo.showAndWait();
        // Verificar si el usuario seleccionó una opción y devolverla
        return resultado.orElse(null);
    }

    private GridPane crearFormularioRegistro() {
        TextField correoTextField = new TextField();
        PasswordField pw = new PasswordField();
        PasswordField confirmarPw = new PasswordField();
        TextField nombreEmpresaTextField = new TextField();
        correoTextField.setPromptText("Correo");
        pw.setPromptText("Contraseña");
        confirmarPw.setPromptText("Confirmar Contraseña");
        nombreEmpresaTextField.setPromptText("Nombre de la Empresa");
        correoTextField.setId("correoTextField");
        pw.setId("pw");
        confirmarPw.setId("confirmarPw");
        nombreEmpresaTextField.setId("nombreEmpresaTextField");
        // Crear el grid layout y añadir los nodos
        GridPane grid = new GridPane();
        grid.add(new Label("Correo:"), 0, 0);
        grid.add(correoTextField, 1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(pw, 1, 1);
        grid.add(new Label("Confirmar Contraseña:"), 0, 2);
        grid.add(confirmarPw, 1, 2);
        grid.add(new Label("Nombre de la Empresa:"), 0, 3);
        grid.add(nombreEmpresaTextField, 1, 3);
        
        return grid;
    }

    private void agregarListenersRegistro(TextField correoTextField, PasswordField pw, PasswordField confirmarPw,
                                          TextField nombreEmpresaTextField, Node registrarButton) {
        correoTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean camposVacios = correoTextField.getText().isEmpty() ||
                    pw.getText().isEmpty() || confirmarPw.getText().isEmpty() ||
                    nombreEmpresaTextField.getText().isEmpty();
            registrarButton.setDisable(!validarCorreo(newValue) || camposVacios);
        });

        pw.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean camposVacios = correoTextField.getText().isEmpty() || pw.getText().isEmpty() ||
                    confirmarPw.getText().isEmpty() || nombreEmpresaTextField.getText().isEmpty();
            registrarButton.setDisable(!validarCorreo(correoTextField.getText()) || camposVacios ||
                    !newValue.equals(confirmarPw.getText()));
        });

        confirmarPw.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean camposVacios = correoTextField.getText().isEmpty() || pw.getText().isEmpty() ||
                    confirmarPw.getText().isEmpty() || nombreEmpresaTextField.getText().isEmpty();
            registrarButton.setDisable(!validarCorreo(correoTextField.getText()) || camposVacios ||
                    !newValue.equals(pw.getText()));
        });

        nombreEmpresaTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean camposVacios = correoTextField.getText().isEmpty() || pw.getText().isEmpty() ||
                    confirmarPw.getText().isEmpty() || nombreEmpresaTextField.getText().isEmpty();
            registrarButton.setDisable(!validarCorreo(correoTextField.getText()) || camposVacios ||
                    !pw.getText().equals(confirmarPw.getText()));
        });
    }

    // Método para validar el correo electrónico con el patrón
    private boolean validarCorreo(String correo) {
        return PATRON_CORREO.matcher(correo).matches();
    }
}