package controllers;

import classes.services.AwsS3;
import classes.utils.GeneradorQR;
import classes.services.Supabase;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import fastmenu.Main;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Menu;
import models.Plato;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;


public class Controlador implements Initializable {
    public ListView listaPlatosMenu = new ListView<>();
    public ListView listaPlatos;
    private String correoEmpresa, nombreMenuModificar;
    private List<Plato> platosAModificar;
    public TextField textfieldNombrePlato, textfieldPrecio, textfieldNombreMenu;
    public TextArea textareaDescripcionPlato;
    public ComboBox comboBoxTipoPlato;
    public Button botonAgregarPlato, botonGenerarPDF, botonSalir, botonGuardarCambios;
    private ObservableList<Plato> listaPlatosObservable = FXCollections.observableArrayList();
    private List<Plato> platos = new ArrayList<>();
    Properties properties = new Properties();
    Supabase supa;
    AwsS3 s3 = new AwsS3();
    GeneradorQR qr = new GeneradorQR();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            properties.load(getClass().getResourceAsStream("/properties/configuraciones.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        supa = new Supabase();
        comboBoxTipoPlato.getItems().addAll("PRIMERO", "SEGUNDO", "POSTRE");
        listaPlatosMenu.setOnMouseClicked(this::onPlatoSeleccionado);
        Main.log.info("Iniciando Controlador");
    }

    // Método para manejar la selección de un plato en el ListView
    private void onPlatoSeleccionado(MouseEvent event) {
        String nombrePlatoSeleccionado = (String) listaPlatosMenu.getSelectionModel().getSelectedItem();
        if (nombrePlatoSeleccionado != null) {
            // Buscar el objeto Plato correspondiente al nombre seleccionado
            Plato platoSeleccionado = null;
            for (Plato plato : platosAModificar) {
                if (plato.getNombrePlato().equals(nombrePlatoSeleccionado)) {
                    platoSeleccionado = plato;
                    break;
                }
            }
            if (platoSeleccionado != null) {
                // Rellenar los campos con los datos del plato seleccionado
                textfieldNombrePlato.setText(platoSeleccionado.getNombrePlato());
                textfieldPrecio.setText(String.valueOf(platoSeleccionado.getPrecioPlato()));
                textareaDescripcionPlato.setText(platoSeleccionado.getDescripcionPlato());
                comboBoxTipoPlato.setValue(platoSeleccionado.getTipoPlato());
            }
        }
    }

    //Metodo se llama al hacer click en el boton de generar pdf
    public void onClickBotonPDF() {
        if (textfieldNombreMenu.getText().isEmpty()) {
            // Mostrar una alerta de error si faltan datos
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("Rellene el campo de nombre de menu por favor");
            alertaError.showAndWait();
        } else {
            //Preparo un documento
            Document document = new Document();
            // Código para crear el documento PDF y agregar platos
            try {
                String pdfPath = "./" + textfieldNombreMenu.getText() + ".pdf";
                PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
                document.open();

                // Agrupar los platos por tipo y agregar al documento
                agregarPlatosPorTipo(document, "PRIMERO");
                agregarPlatosPorTipo(document, "SEGUNDO");
                agregarPlatosPorTipo(document, "POSTRE");
                document.close();

                // Recupero primero el id de empresa del correo dado
                int idEmpresa = supa.obtenerIdEmpresaPorCorreo(correoEmpresa);
                System.out.println(idEmpresa);
                Menu menu = new Menu(textfieldNombreMenu.getText(), idEmpresa);
                // Agrego el menu con los datos y el id de empresa obtenido
                supa.agregarMenu(menu);
                // Recupero el id del menu actual
                int idMenu = supa.obtenerIdMenuPorIdEmpresa(menu);
                for (Plato plato : platos) {
                    // Agrego cada plato con sus correspondientes datos
                    supa.agregarPlato(plato, idEmpresa, idMenu);
                }
                // Llamada al metodo sube el pdf al bucket de aws s3
                s3.subirPDFaS3(properties.getProperty("aws_access_key_id"), properties.getProperty("aws_secret_access_key"),
                        properties.getProperty("aws_session_token"), pdfPath);
                // Llamada al metodo para acceder y generar un qr que redireccione al pdf alojado en s3
                qr.generarQR("pruebazekrom", "archivo2.txt", "./qrzekrom.png",
                        properties.getProperty("aws_access_key_id"),
                        properties.getProperty("aws_secret_access_key"), properties.getProperty("aws_session_token"));
                // Llamada al metodo para previsualizar los platos
                guardarPDFYMostrar(platos, pdfPath);
                // Abrir un cuadro de diálogo de guardado de archivos
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Guardar PDF");
                fileChooser.setInitialFileName(textfieldNombreMenu.getText() + ".pdf");
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)",
                        "*.pdf");
                fileChooser.getExtensionFilters().add(extFilter);
                Stage stage = (Stage) botonGenerarPDF.getScene().getWindow();
                File selectedFile = fileChooser.showSaveDialog(stage);

                // Guardar el PDF en la ubicación seleccionada por el usuario
                if (selectedFile != null) {
                    try {
                        String pdfFile = "./" + textfieldNombreMenu.getText() + ".pdf";
                        java.nio.file.Files.copy(new File(pdfFile).toPath(), selectedFile.toPath());
                        System.out.println("PDF guardado en: " + selectedFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Main.log.error("Error al guardar el PDF");
                    }
                }
            } catch (DocumentException | IOException e) {
                e.printStackTrace();
                Main.log.error("Error al generar el PDF");
            }
        }
        Main.log.info("Se creo el pdf para el menú" + textfieldNombreMenu.getText());
    }

    private void regenerarPDf() {
        Document document = new Document();
        // Código para crear el documento PDF y agregar platos
        try {
            String pdfPath = "./" + this.nombreMenuModificar + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Agrupar los platos por tipo y agregar al documento
            agregarPlatosPorTipo(document, "PRIMERO");
            agregarPlatosPorTipo(document, "SEGUNDO");
            agregarPlatosPorTipo(document, "POSTRE");
            document.close();

            int idMenu = supa.obtenerIdMenuPorNombre(this.nombreMenuModificar);
            // Llamada al metodo sube el pdf al bucket de aws s3
            s3.subirPDFaS3(properties.getProperty("aws_access_key_id"), properties.getProperty("aws_secret_access_key"),
                    properties.getProperty("aws_session_token"), pdfPath);
            // Llamada al metodo para acceder y generar un qr que redireccione al pdf alojado en s3
            qr.generarQR("pruebazekrom", "archivo2.txt", "./qrzekrom.png",
                    properties.getProperty("aws_access_key_id"),
                    properties.getProperty("aws_secret_access_key"), properties.getProperty("aws_session_token"));
            // Llamada al metodo para previsualizar los platos
            List<Plato> platos = supa.obtenerPlatosPorIdMenu(idMenu);
            guardarPDFYMostrar(platos, pdfPath);
            // Abrir un cuadro de diálogo de guardado de archivos
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF");
            fileChooser.setInitialFileName(textfieldNombreMenu.getText() + ".pdf");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)",
                    "*.pdf");
            fileChooser.getExtensionFilters().add(extFilter);
            Stage stage = (Stage) botonGuardarCambios.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);

            // Guardar el PDF en la ubicación seleccionada por el usuario
            if (selectedFile != null) {
                try {
                    String pdfFile = "./" + textfieldNombreMenu.getText() + ".pdf";
                    java.nio.file.Files.copy(new File(pdfFile).toPath(), selectedFile.toPath());
                    System.out.println("PDF guardado en: " + selectedFile.getAbsolutePath());
                } catch (IOException e) {
                    Main.log.error("Error al regenerar el PDF");
                    e.printStackTrace();
                }
            }
        } catch (DocumentException | IOException e) {
            Main.log.error("Error al regenerar el PDF");
            e.printStackTrace();
        }
        Main.log.info("Se regenero el pdf para el menú" + nombreMenuModificar);
    }

    // Método para guardar el PDF y mostrarlo
    // Método para guardar el PDF y mostrarlo
    private void guardarPDFYMostrar(List<Plato> platos, String pdfPath) {
        try {
            // Ordenar los platos por tipo
            Map<String, List<Plato>> platosPorTipo = new HashMap<>();
            for (Plato plato : platos) {
                String tipoPlato = plato.getTipoPlato();
                platosPorTipo.computeIfAbsent(tipoPlato, k -> new ArrayList<>()).add(plato);
            }

            // Definir el orden deseado de los tipos de platos
            List<String> ordenTiposPlatos = Arrays.asList("PRIMERO", "SEGUNDO", "POSTRE");

            // Crear el documento PDF
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Agregar los platos al documento PDF, en el orden deseado
            for (String tipoPlato : ordenTiposPlatos) {
                if (platosPorTipo.containsKey(tipoPlato)) {
                    // Agregar título del tipo de plato al documento
                    Paragraph tipoTitle = new Paragraph(tipoPlato);
                    tipoTitle.setAlignment(Element.ALIGN_CENTER);
                    document.add(tipoTitle);

                    // Agregar un salto de línea después de cada categoría
                    document.add(new Paragraph("\n"));

                    // Obtener la lista de platos para esta categoría
                    List<Plato> platosDeEstaCategoria = platosPorTipo.get(tipoPlato);

                    // Definir el ancho de la página
                    float anchoPagina = document.getPageSize().getWidth();

                    // Agregar cada plato del tipo al documento
                    for (Plato plato : platosDeEstaCategoria) {
                        // Formatear el nombre y el precio del plato
                        String nombrePrecio = String.format("%-60s %50.2f€", plato.getNombrePlato(), plato.getPrecioPlato());
                        document.add(new Paragraph(nombrePrecio));

                        // Agregar la descripción del plato
                        document.add(new Paragraph(plato.getDescripcionPlato()));

                        // Agregar una línea en blanco entre platos
                        document.add(new Paragraph("\n"));
                    }

                    // Agregar espacio entre tipos de platos
                    document.add(new Paragraph("\n"));
                }
            }

            // Cerrar el documento PDF
            document.close();
            System.out.println("PDF guardado localmente en: " + pdfPath);

            // Mostrar el PDF después de guardarlo
            mostrarPDF(pdfPath);
        } catch (DocumentException | IOException e) {
            Main.log.error("Error al guardar y mostrar el PDF");
            e.printStackTrace();
        }
    }

    //Metodo abre el pdf
    private void mostrarPDF(String pdfPath) {
        try {
            File file = new File(pdfPath);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                System.out.println("No se puede abrir el archivo PDF automáticamente.");
            }
        } catch (IOException e) {
            Main.log.error("Error al mostrar el PDF");
            e.printStackTrace();
        }
    }

    public void onClickBotonAgregar(MouseEvent mouseEvent) {
        if (!textfieldPrecio.getText().isEmpty() && !isNumeric(textfieldPrecio.getText())) {
            // Mostrar una alerta si el precio no es un número válido
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Precio inválido");
            alertaError.setContentText("Por favor, ingrese un número válido para el precio.");
            alertaError.showAndWait();
        } else if (!textareaDescripcionPlato.getText().isEmpty() &&
                !textfieldNombrePlato.getText().isEmpty() &&
                !comboBoxTipoPlato.getSelectionModel().isEmpty()) {

            double precio = Double.parseDouble(textfieldPrecio.getText());
            Plato nuevoPlato = new Plato(textfieldNombrePlato.getText(),
                    textareaDescripcionPlato.getText(),
                    comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(),
                    precio);
            platos.add(nuevoPlato);
            listaPlatosObservable.setAll(platos);
            listaPlatos.setItems(listaPlatosObservable);
            listaPlatos.refresh();
        } else {
            // Mostrar una alerta de error si faltan datos
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("Todos los datos han de ser rellenados");
            alertaError.showAndWait();
        }
        Main.log.info("Se agregó el plato " + textfieldNombrePlato);
    }

    public void onClickBotonAgregarModificar(MouseEvent mouseEvent) {
        if (!textfieldPrecio.getText().isEmpty() && !isNumeric(textfieldPrecio.getText())) {
            // Mostrar una alerta si el precio no es un número válido
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Precio inválido");
            alertaError.setContentText("Por favor, ingrese un número válido para el precio.");
            alertaError.showAndWait();
        } else if (!textareaDescripcionPlato.getText().isEmpty() &&
                !textfieldNombrePlato.getText().isEmpty() &&
                !comboBoxTipoPlato.getSelectionModel().isEmpty()) {

            double precio = Double.parseDouble(textfieldPrecio.getText());
            Plato nuevoPlato = new Plato(textfieldNombrePlato.getText(),
                    textareaDescripcionPlato.getText(),
                    comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(),
                    precio);
            platos.add(nuevoPlato);
            listaPlatosObservable.setAll(platos);
            listaPlatos.setItems(listaPlatosObservable);
            listaPlatos.refresh();
            if (listaPlatosMenu.getSelectionModel().getSelectedItem() == null) {
                Plato platoNuevo = new Plato(textfieldNombrePlato.getText(), textareaDescripcionPlato.getText(),
                        comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(), Double.parseDouble(textfieldPrecio.getText()));
                supa.agregarPlato(platoNuevo, supa.obtenerIdEmpresaPorCorreo(correoEmpresa), supa.obtenerIdMenuPorNombre(nombreMenuModificar));
            } else {

                Plato platoModificado = new Plato(textfieldNombrePlato.getText(), textareaDescripcionPlato.getText(),
                        comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(), Double.parseDouble(textfieldPrecio.getText()));
                supa.modificarPlatos(platoModificado, listaPlatosMenu.getSelectionModel().getSelectedItem().toString(),supa.obtenerIdMenuPorNombre(nombreMenuModificar), supa.obtenerIdEmpresaPorCorreo(correoEmpresa));
            }

        } else {
            // Mostrar una alerta de error si faltan datos
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("Todos los datos han de ser rellenados");
            alertaError.showAndWait();
        }
        Main.log.info("Se agregó el plato " + textfieldNombrePlato);
    }

    // Método auxiliar para verificar si una cadena es numérica
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //Metodo organizar los platos
    private void agregarPlatosPorTipo(Document document, String tipoPlato) throws DocumentException {
        Paragraph tipoTitle = new Paragraph(tipoPlato);
        tipoTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(tipoTitle);
        for (Plato plato : platos) {
            if (plato.getTipoPlato().equals(tipoPlato)) {
                // Agregar plato al documento
                Paragraph platoParagraph = new Paragraph(plato.toString());
                document.add(platoParagraph);
            }
        }
        document.add(new Paragraph("\n")); // Agregar espacio entre tipos de platos
    }

    //Metodo pasarle el correo de la vista login a la de creacion o modificacion de platos
    public void obtenerCorreo(String correoUsuario) {
        this.correoEmpresa = correoUsuario;
        System.out.println("Correo del usuario registrado: " + correoUsuario);
        Main.log.info("Correo del usuario registrado: " + correoEmpresa);
    }

    public void obtenerPlatosModificar(List<Plato> platos) {
        this.platosAModificar = platos;
        System.out.println("Platos leidos correctamente");
    }

    public void obtenerMenu(String menuModifcar) {
        this.nombreMenuModificar = menuModifcar;
        System.out.println("Menu a modificar : " + menuModifcar);
        Main.log.info("Menu a modificar : " + nombreMenuModificar);
    }

    //Al hacer click en Salir acaba la aplicacion
    public void onClickBotonDesconectarse() {
        supa.modificarCampoUsuarioLogueado(correoEmpresa, false);
        Platform.exit();
    }

    //Al hacer cick en previsualizar muestro el pdf se generaria
    public void onBtnClickPrevisualizar(MouseEvent mouseEvent) {
        String pdfPath = "./" + textfieldNombreMenu.getText() + ".pdf";
        guardarPDFYMostrar(platos, pdfPath);
    }

    public void onClickBotonGuardar(MouseEvent mouseEvent) {
        regenerarPDf();
    }
}