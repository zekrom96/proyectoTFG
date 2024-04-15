package Controladores;

import com.example.fastmenuproyectotfg.FastMenu;
import com.example.fastmenuproyectotfg.bdSupabase;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.util.Pair;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ControladorLogin implements Initializable {
    public TextField textfieldCorreo, textfieldPw;
    public Button btnRegistrase, btnLogin;
    bdSupabase supa;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textfieldCorreo.setText("davidpeka20@gmail.com");
        textfieldPw.setText("root996");
        supa = new bdSupabase();
    }

    public void onClickBtnRegistrarse(MouseEvent mouseEvent) {
        /*
        Muestro el dialogo y si el resultado no es null proceso los datos, recojo el valor del correo y la pw
        y luego le paso los datos al metodo de crearUsuario que crea el usuario en la Auth de supabase finalmente
        procedo a enviar al usuario a la ventana de CrearPlatos ya que al ser un nuevo usuario no va tener platos que
        modificar aún, ni nombre de Empresa
         */
        Pair<String, String> resultado = mostrarDialogoRegistro(mouseEvent);
        if (resultado != null) {
            String[] partes = resultado.getKey().split("_");
            String correo = partes[0];
            String nombreEmpresa = partes[1];
            String pw = resultado.getValue();
            // Llamar a la función createUsuario con los datos ingresados
            supa.crearUsuario(correo, pw);
            supa.agregarEmpresa(nombreEmpresa, correo);
            // Resto del código para cargar la nueva vista y cerrar la ventana actual
            try {
                FXMLLoader loader = new FXMLLoader(FastMenu.class.getResource("vistaMenu.fxml"));
                Parent root = loader.load();
                Scene nuevaScene = new Scene(root);
                Controlador controlador = loader.getController();
                //Aqui le paso al controlador de la clase en la creo los platos, el correo del usuario registrado
                controlador.obtenerCorreo(correo);
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


    public void onClickBtnLogin(MouseEvent mouseEvent) {
        if (supa.iniciarSesion(textfieldCorreo.getText(), textfieldPw.getText())) {
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
                    supa.recuperarPlatosPorCorreo(textfieldCorreo.getText());
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
                        controlador.obtenerCorreo(textfieldCorreo.getText());
                        // Cerrar la ventana actual
                        Stage ventanaActual = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
                        ventanaActual.close();
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
    }

    public Pair<String, String> mostrarDialogoRegistro(MouseEvent mouseEvent) {
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
        Dialog<Pair<String, String>> dialog = new Dialog<>();
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
            registrarButton.setDisable(newValue.isEmpty() || pw.getText().isEmpty() || !newValue.equals(confirmarPw.getText()) || nombreEmpresaTextField.getText().isEmpty());
        });

        pw.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || correoTextField.getText().isEmpty() || !newValue.equals(confirmarPw.getText()) || nombreEmpresaTextField.getText().isEmpty());
        });

        confirmarPw.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || correoTextField.getText().isEmpty() || !newValue.equals(pw.getText()) || nombreEmpresaTextField.getText().isEmpty());
        });

        nombreEmpresaTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            registrarButton.setDisable(newValue.isEmpty() || correoTextField.getText().isEmpty() || pw.getText().isEmpty() || !pw.getText().equals(confirmarPw.getText()));
        });

        // Convertir el resultado del diálogo en un par Pair<String, String> al hacer clic en el botón "Registrar"
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                String correoConEmpresa = correoTextField.getText() + "_" + nombreEmpresaTextField.getText();
                return new Pair<>(correoConEmpresa, pw.getText());
            }
            return null;
        });


        // Mostrar el diálogo y devolver el resultado
        Optional<Pair<String, String>> result = dialog.showAndWait();
        return result.orElse(null); // Devuelve el resultado o null si se cancela el diálogo
    }


    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public void onClickRecuperar(MouseEvent mouseEvent) {
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
                // Si el usuario hizo clic en "OK", enviar el correo
                supa.enviarCorreoRestablecerPw(correoTextField.getText());
            }
        });
    }


}
