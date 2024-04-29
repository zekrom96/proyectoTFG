package controllers;

import classes.utils.CifradoyDescifrado;
import classes.services.Correo;
import classes.utils.GeneradorPassword;
import classes.services.Supabase;
import fastmenu.*;
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
import models.Usuario;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class ControladorLogin implements Initializable {
    public TextField textfieldCorreo, textfieldPw;
    public Button btnRegistrase, btnLogin;
    Properties props = new Properties();
    Supabase supa;
    CifradoyDescifrado crypt;
    Correo correo = new Correo();
    GeneradorPassword generadorPw = new GeneradorPassword();

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

    public void onClickBtnRegistrarse(MouseEvent mouseEvent) throws Exception {
        /*
        Muestro el dialogo y si el resultado no es null proceso los datos, recojo el valor del correo y la pw
        y luego le paso los datos al metodo de crearUsuario que crea el usuario en la Auth de supabase finalmente
        procedo a enviar al usuario a la ventana de CrearPlatos ya que al ser un nuevo usuario no va tener platos que
        modificar aún, ni nombre de Empresa
         */
        String[] resultado = mostrarDialogoRegistro(mouseEvent);
        if (resultado != null) {
            String correo = resultado[0];
            String pw = resultado[1];
            String nombreEmpresa = resultado[2];

            // Llamar a la función createUsuario con los datos ingresados
            String pwCifrada = crypt.encriptar(pw);
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
                Usuario nuevoUsuario = new Usuario(pwCifrada, correo);
                supa.crearUsuario(nuevoUsuario);
                Empresa empresa = new Empresa(nombreEmpresa);
                supa.agregarEmpresa(empresa, correo);

                // Resto del código para cargar la nueva vista y cerrar la ventana actual
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaMenu.fxml"));
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

    public void login(String correo, String pw) {
        Usuario usuario = new Usuario(correo, pw);
        if (supa.iniciarSesion(usuario)) {
            //boolean campo = supa.comprobarEstadoCampoRestablecerPw(textfieldCorreo.getText());
            //System.out.println(campo);
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

                dialogo.showAndWait().ifPresent(boton -> {
                    if (boton == botonConfirmar) {
                        String nuevaPw = campoPw.getText();
                        String confirmarPw = campoConfirmar.getText();
                        if (nuevaPw.equals(confirmarPw)) {
                            String pwTemporalCifrada = crypt.encriptar(nuevaPw);
                            Usuario nuevoUsuario = new Usuario(textfieldCorreo.getText(), pwTemporalCifrada);
                            supa.modificarPassword(nuevoUsuario);
                            supa.modificarCampoUsuarioRestablecerPw(textfieldCorreo.getText(), false);
                            System.out.println("Contraseña cambiada exitosamente.");
                            //sesion.setUsuarioLogueado(true);
                            //sesion.setCorreoLogueado(textfieldCorreo.getText());
                            //sesion.setPassword(textfieldPw.getText());
                            vistaMenu();
                            guardarEstadoLoginPreferences(textfieldCorreo.getText());
                            supa.modificarCampoUsuarioLogueado(textfieldCorreo.getText(), true);
                        } else {
                            Alert alerta = new Alert(Alert.AlertType.ERROR);
                            alerta.setTitle("Error");
                            alerta.setHeaderText("Las contraseñas no coinciden.");
                            alerta.setContentText("Por favor intenta nuevamente.");
                            alerta.showAndWait();
                        }
                    }
                });
            } else {
                //sesion.setUsuarioLogueado(true);
                //sesion.setCorreoLogueado(textfieldCorreo.getText());
                //sesion.setPassword(textfieldPw.getText());
                guardarEstadoLoginPreferences(textfieldCorreo.getText());
                supa.modificarCampoUsuarioLogueado(textfieldCorreo.getText(), true);
                vistaMenu();
                //System.out.println(sesion.getUsuarioLogueado());
            }
        }
    }

    private void guardarEstadoLoginPreferences(String correoUsuario) {
        Preferences preferences = Preferences.userRoot().node("fastmenu");
        preferences.put("logged_in_user_email", correoUsuario);
    }

    private String obtenerPreferences() {
        Preferences preferences = Preferences.userRoot().node("fastmenu");
        return preferences.get("logged_in_user_email", null);
    }

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
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaModificacion.fxml"));
                Parent root = null;
                try {
                    root = fxmlLoader.load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Scene nuevaScene = new Scene(root);
                Controlador controlador = fxmlLoader.getController();
                controlador.obtenerCorreo(textfieldCorreo.getText());
                // Establecer la nueva escena en una nueva ventana
                Stage nuevaVentana = new Stage();
                nuevaVentana.setScene(nuevaScene);
                nuevaVentana.show();
                // Acción a realizar si se selecciona "Modificar"
                System.out.println("Se seleccionó Modificar");
                // Aquí puedes agregar la lógica para la acción de "Modificar"
            } else if (boton == botonCrear) {
                // Acción a realizar si se selecciona "Crear"
                System.out.println("Se seleccionó Crear");
                // Aquí puedes agregar la lógica para la acción de "Crear"
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/vistaMenu.fxml"));
                    Parent root = fxmlLoader.load();

                    Scene nuevaScene = new Scene(root);
                    Controlador controlador = fxmlLoader.getController();
                    controlador.obtenerCorreo(textfieldCorreo.getText());
                    // Establecer la nueva escena en una nueva ventana
                    Stage nuevaVentana = new Stage();
                    nuevaVentana.setScene(nuevaScene);
                    nuevaVentana.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public String[] mostrarDialogoRegistro(MouseEvent mouseEvent) {
        // Diseño del registro
        // Crear los campos de texto
        TextField correoTextField = new TextField();
        PasswordField pw = new PasswordField();
        PasswordField confirmarPw = new PasswordField(); // Nuevo campo de confirmación de contraseña
        TextField nombreEmpresaTextField = new TextField(); // Nuevo campo para el nombre de la empresa

        correoTextField.setPromptText("Correo");
        pw.setPromptText("Contraseña");
        confirmarPw.setPromptText("Confirmar Contraseña"); // Texto de ayuda para el campo de confirmación
        nombreEmpresaTextField.setPromptText("Nombre de la Empresa"); // Texto de ayuda para el campo del nombre de la empresa

        // Crear el grid layout y añadir los nodos
        GridPane grid = new GridPane();
        grid.add(new Label("Correo:"), 0, 0);
        grid.add(correoTextField, 1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(pw, 1, 1);
        grid.add(new Label("Confirmar Contraseña:"), 0, 2); // Etiqueta para confirmar contraseña
        grid.add(confirmarPw, 1, 2); // Campo de confirmación de contraseña
        grid.add(new Label("Nombre de la Empresa:"), 0, 3); // Etiqueta para el nombre de la empresa
        grid.add(nombreEmpresaTextField, 1, 3); // Campo para el nombre de la empresa
        // Crear el diálogo
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Registro");

        // Agregar 2 botones uno con el texto Registrar y el otro Cancelar
        ButtonType registrarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registrarButtonType, ButtonType.CANCEL);

        // Asignar el gridPane al diálogo
        dialog.getDialogPane().setContent(grid);

        // Habilitar el botón "Registrar" cuando todos los campos de texto no están vacíos y las contraseñas coinciden
        Node registrarButton = dialog.getDialogPane().lookupButton(registrarButtonType);
        registrarButton.setDisable(true);

        correoTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || pw.getText().isEmpty() ||
                    !newValue.equals(confirmarPw.getText()) || nombreEmpresaTextField.getText().isEmpty());
        });

        pw.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || correoTextField.getText().isEmpty() ||
                    !newValue.equals(confirmarPw.getText()) || nombreEmpresaTextField.getText().isEmpty());
        });

        confirmarPw.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || correoTextField.getText().isEmpty() ||
                    !newValue.equals(pw.getText()) || nombreEmpresaTextField.getText().isEmpty());
        });

        nombreEmpresaTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || correoTextField.getText().isEmpty() ||
                    pw.getText().isEmpty() || !pw.getText().equals(confirmarPw.getText()));
        });

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
        return result.orElse(null); // Devuelve el resultado o null si se cancela el diálogo
    }

    public void onClickRecuperar(MouseEvent mouseEvent) throws Exception {
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
            dialogo.getDialogPane().lookupButton(ButtonType.OK).setDisable(newValue.trim().isEmpty());
        });

        // Mostrar el diálogo y esperar a que el usuario interactúe con él
        dialogo.showAndWait().ifPresent(resultado -> {
            if (resultado == ButtonType.OK) {
                String pwTemporal = generadorPw.generarPassword();
                // Si el usuario hizo clic en "OK", enviar el correo
                try {
                    correo.enviarGmail(props.getProperty("cuenta_correo"), props.getProperty("keygmail"),
                            correoTextField.getText(), pwTemporal);
                    String pwTemporalCifrada = crypt.encriptar(pwTemporal);
                    Usuario nuevoUsuario = new Usuario(correoTextField.getText(), pwTemporalCifrada);
                    supa.modificarPassword(nuevoUsuario);
                    supa.modificarCampoUsuarioRestablecerPw(correoTextField.getText(), true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}