package controllers;

import classes.services.AwsS3;
import classes.utils.GeneradorQR;
import classes.services.Supabase;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import fastmenu.Main;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;


public class Controlador implements Initializable {
    public ListView listaPlatosMenu = new ListView<>();
    public ListView listaPlatos;
    public TextField textfieldNombrePlato, textfieldPrecio, textfieldNombreMenu;
    public TextArea textareaDescripcionPlato;
    public ComboBox comboBoxTipoPlato;
    public Button botonAgregarPlato, botonGenerarPDF, botonSalir, botonRegenerarPDF;
    private String correoEmpresa, nombreMenuModificar, nombreMenuNuevo;
    private List<Plato> platosAModificar;
    private ObservableList<Plato> listaPlatosObservable = FXCollections.observableArrayList();
    private List<Plato> platos = new ArrayList<>();
    private Plato platoSeleccionado;
    private Properties properties = new Properties();
    private Supabase supa;
    private AwsS3 s3 = new AwsS3();
    private GeneradorQR qr = new GeneradorQR();

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

    // Método que se llama al hacer clic en el botón de generar PDF
    public void onClickBotonPDF() {
        if (!textfieldNombreMenu.getText().isEmpty()) {
            nombreMenuNuevo = textfieldNombreMenu.getText();
        }
        if (nombreMenuNuevo == null) {
            // Mostrar una alerta de error si faltan datos
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("Rellene el campo de nombre de menú por favor");
            alertaError.showAndWait();
            Main.log.warn("El usuario " + correoEmpresa + " no introdujo el campo de menú, alerta mostrada");
        } else {
            if (platos.isEmpty()) {
                Alert alertaError = new Alert(Alert.AlertType.ERROR);
                alertaError.setTitle("Error");
                alertaError.setHeaderText("Este es un mensaje de error");
                alertaError.setContentText("No hay platos para guardar");
                alertaError.showAndWait();
                Main.log.error("No hay platos para guardar");
            } else {
                // Preparo un documento
                Document document = new Document();
                // Código para crear el documento PDF y agregar platos
                try {
                    String pdfPath = "./" + nombreMenuNuevo + ".pdf";
                    PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
                    document.open();

                    // Agrupar los platos por tipo y agregar al documento
                    agregarPlatosPorTipo(document, "PRIMERO");
                    agregarPlatosPorTipo(document, "SEGUNDO");
                    agregarPlatosPorTipo(document, "POSTRE");
                    document.close();

                    // Recupero primero el id de empresa del correo recibe el controlador
                    int idEmpresa = supa.obtenerIdEmpresaPorCorreo(correoEmpresa);
                    System.out.println(idEmpresa);
                    Menu menu = new Menu(nombreMenuNuevo, idEmpresa);
                    // Agrego el menú con los datos y el id de empresa obtenido
                    supa.agregarMenu(menu);
                    // Recupero el id del menú actual
                    int idMenu = supa.obtenerIdMenuPorIdEmpresa(menu);
                    for (Plato plato : platos) {
                        // Agrego cada plato con sus correspondientes datos al menú
                        supa.agregarPlato(plato, idEmpresa, idMenu);
                    }
                    File pdf = new File("./" + textfieldNombreMenu.getText() + ".pdf");
                    // Llamada al método para subir el PDF al bucket de AWS S3
                    s3.subirPDFaS3(properties.getProperty("aws_access_key_id"),
                            properties.getProperty("aws_secret_access_key"),
                            properties.getProperty("aws_session_token"), pdf,
                            textfieldNombreMenu.getText() + ".pdf");
                    // Llamada al método para acceder y generar un QR que redireccione al PDF alojado en S3
                    qr.generarQrYEnlazarConS3("pruebazekrom", textfieldNombreMenu.getText() +
                                    ".pdf", "./" + textfieldNombreMenu.getText() + ".png",
                            properties.getProperty("aws_access_key_id"),
                            properties.getProperty("aws_secret_access_key"), properties.getProperty("aws_session_token")
                            , botonGenerarPDF);
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
                Main.log.info("Se creó el PDF para el menú" + nombreMenuNuevo);
                Alert alertaError = new Alert(Alert.AlertType.INFORMATION);
                alertaError.setTitle("Menú");
                alertaError.setHeaderText("Este es un mensaje de información");
                alertaError.setContentText("El menú se creó correctamente, saliendo de FastMenu...");
                alertaError.showAndWait();
                Platform.exit();
            }
        }
    }

    // Método para agregar platos a la lista en la vista de Crear
    public void onClickBotonAgregar() {
        if (!textfieldNombreMenu.getText().isEmpty()) {
            nombreMenuNuevo = textfieldNombreMenu.getText();
            textfieldNombreMenu.setText(nombreMenuNuevo);
            textfieldNombreMenu.setEditable(false);
            Main.log.info("El usuario " + correoEmpresa + " introdujo un nombre de menú, bloqueando el campo...");
        }
        if (!textfieldPrecio.getText().isEmpty() && !esUnNumero(textfieldPrecio.getText())) {
            // Mostrar una alerta si el precio no es un número válido
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Precio inválido");
            alertaError.setContentText("Por favor, ingrese un número válido para el precio.");
            alertaError.showAndWait();
            Main.log.error("El usuario " + correoEmpresa + " introdujo un número no válido");
        } else if (!textareaDescripcionPlato.getText().isEmpty() && !textfieldNombrePlato.getText().isEmpty() &&
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
            Main.log.error("Al usuario " + correoEmpresa + " le faltan datos por rellenar...");
        }
        Main.log.info("Se agregó el plato a la lista " + textfieldNombrePlato.getText());
        // Se limpian los campos
        onClickBotonLimpiar();
    }

    // Método para modificar los datos de un plato
    public void onClickBotonAgregarModificar() {
        if (!textfieldPrecio.getText().isEmpty() && !esUnNumero(textfieldPrecio.getText())) {
            // Mostrar una alerta si el precio no es un número válido
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Precio inválido");
            alertaError.setContentText("Por favor, ingrese un número válido para el precio.");
            alertaError.showAndWait();
            Main.log.error("El usuario " + correoEmpresa + " introdujo un número no válido");
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
        /*
        Si no tiene ningún plato seleccionado se entiende lo que hace es agregar un nuevo plato a ese menú y si no
        lo que haría sería editar los datos del plato seleccionado
         */
            if (listaPlatosMenu.getSelectionModel().getSelectedItem() == null) {
                Plato platoNuevo = new Plato(textfieldNombrePlato.getText(), textareaDescripcionPlato.getText(),
                        comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(),
                        Double.parseDouble(textfieldPrecio.getText()));
                String nombreMenuCodificado = URLEncoder.encode(nombreMenuModificar, StandardCharsets.UTF_8);
                supa.agregarPlato(platoNuevo, supa.obtenerIdEmpresaPorCorreo(correoEmpresa),
                        supa.obtenerIdMenuPorNombre(nombreMenuCodificado));
                // Crear una alerta de información
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Modificación de Plato");
                alert.setHeaderText(null);
                alert.setContentText("El plato fue agregado correctamente.");

                // Mostrar la alerta y esperar a que el usuario la cierre
                alert.showAndWait();
                Main.log.info("Se agregó el plato " + textfieldNombrePlato.getText() + " al menú " + nombreMenuModificar);
            } else {
                Plato platoModificado = new Plato(textfieldNombrePlato.getText(), textareaDescripcionPlato.getText(),
                        comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(),
                        Double.parseDouble(textfieldPrecio.getText()));
                System.out.println(platoModificado.getTipoPlato());
                System.out.println(platoSeleccionado.getTipoPlato());
                if (platoSeleccionado.toString().equals(platoModificado.toString())
                        && platoSeleccionado.getTipoPlato() == platoModificado.getTipoPlato()) {
                    // Mostrar una alerta de error si faltan datos
                    Alert alertaError = new Alert(Alert.AlertType.ERROR);
                    alertaError.setTitle("Error");
                    alertaError.setHeaderText("Este es un mensaje de error");
                    alertaError.setContentText("Los datos del plato no han sido cambiados, cancelando modificación");
                    alertaError.showAndWait();
                } else {
                    try {
                        String nombreMenuCodificado = URLEncoder.encode(nombreMenuModificar, StandardCharsets.UTF_8);
                        // Lógica para modificar el plato
                        supa.modificarPlatos(platoModificado,
                                listaPlatosMenu.getSelectionModel().getSelectedItem().toString(),
                                supa.obtenerIdMenuPorNombre(nombreMenuCodificado),
                                supa.obtenerIdEmpresaPorCorreo(correoEmpresa));
                        Main.log.info("Se modificó el plato " + textfieldNombrePlato.getText() + " del menú " +
                                nombreMenuModificar);
                        // Crear una alerta de información
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Modificación de Plato");
                        alert.setHeaderText(null);
                        alert.setContentText("El plato se modificó correctamente.");
                        alert.showAndWait();
                    } catch (Exception e) {
                        // Manejo de errores
                        Main.log.error("Error al modificar el plato: " + e.getMessage());
                        e.printStackTrace();
                        // Crear una alerta de error
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Ocurrió un error al modificar el plato. Por favor, intente nuevamente.");
                        // Mostrar la alerta y esperar a que el usuario la cierre
                        alert.showAndWait();
                    }
                }
            }
        } else {
            // Mostrar una alerta de error si faltan datos
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("Todos los datos han de ser rellenados");
            alertaError.showAndWait();
            Main.log.error("Al usuario " + correoEmpresa + " le faltan datos por rellenar");
        }
        // Actualiza los cambios
        obtenerPlatos();
        onClickBotonLimpiarModificar();
    }


    // Método con la lógica para borrar un plato de un menú
    public void onClickBotonBorrarPlato() {
        if (listaPlatosMenu.getSelectionModel().getSelectedItem() != null) {
            // Obtener el plato seleccionado
            String platoSeleccionado = listaPlatosMenu.getSelectionModel().getSelectedItem().toString();
            if (platoSeleccionado != null) {
                // Crear la alerta de confirmación
                Alert alertaConfirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                alertaConfirmacion.setTitle("Confirmación de Borrado");
                alertaConfirmacion.setHeaderText("¿Estás seguro de que quieres borrar el plato?");
                alertaConfirmacion.setContentText("Plato: " + platoSeleccionado);
                // Mostrar la alerta y esperar la respuesta del usuario
                alertaConfirmacion.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        String nombreMenuCodificado = URLEncoder.encode(nombreMenuModificar, StandardCharsets.UTF_8);
                        // Si el usuario confirma, proceder con el borrado
                        supa.borrarPlato(platoSeleccionado,
                                supa.obtenerIdMenuPorNombre(nombreMenuCodificado),
                                supa.obtenerIdEmpresaPorCorreo(correoEmpresa));
                        // Recuperar los platos de la empresa del usuario logueado
                        obtenerPlatos();
                        onClickBotonLimpiarModificar();
                        Alert alertaOk = new Alert(Alert.AlertType.INFORMATION);
                        alertaOk.setTitle("Plato borrado");
                        alertaOk.setHeaderText("El plato " + platoSeleccionado + " fue borrado");
                        alertaOk.showAndWait();
                        Main.log.info("El plato " + platoSeleccionado + " fue borrado del menú " + nombreMenuModificar);
                    }
                });
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error borrado");
            alert.setHeaderText("Error");
            alert.setContentText("No se seleccionó ningún plato, no se puede borrar");
            alert.showAndWait();
            Main.log.error("No se seleccionó ningún plato para borrar");
        }
    }

    // Al hacer clic en previsualizar, muestra el PDF que se generaría
    public void onBtnClickPrevisualizar() {
        if (platos.isEmpty()) {
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("No hay platos para mostrar");
            alertaError.showAndWait();
            Main.log.error("No hay platos para mostrar y no se puede previsualizar");
        } else {
            String pdfPath = "./" + textfieldNombreMenu.getText() + ".pdf";
            guardarPDFYMostrar(platos, pdfPath);
            Main.log.info("Se previsualizó el PDF del menú " + textfieldNombreMenu.getText());
        }
    }

    // Regenerar el PDF en la vista de modificar
    public void onClickBotonGuardar() {
        regenerarPDf();
    }

    // Al hacer clic en Salir, termina la aplicación
    public void onClickBotonDesconectarse() {
        supa.modificarCampoUsuarioLogueado(correoEmpresa, false);
        Platform.exit();
        Main.log.info("El usuario " + correoEmpresa + " se desconectó de la aplicación");
    }

    public void onClickBotonBorrarCrear(MouseEvent mouseEvent) {
        if (listaPlatos.getSelectionModel().getSelectedItem() != null) {
            Plato platoSeleccionado = (Plato) listaPlatos.getSelectionModel().getSelectedItem();
            // Crear una alerta de confirmación
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmación de borrado");
            alert.setHeaderText("Está a punto de eliminar un plato");
            alert.setContentText("¿Está seguro de que desea eliminar el plato seleccionado?");

            // Mostrar la alerta y esperar a que el usuario confirme
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Eliminar el plato si el usuario confirma
                    platos.remove(platoSeleccionado);
                    listaPlatosObservable.setAll(platos);
                    listaPlatos.setItems(listaPlatosObservable);
                    listaPlatos.refresh();
                    Main.log.info("Se eliminó el plato " + platoSeleccionado.getNombrePlato() + " de la lista de platos");
                }
            });
        }
    }

// Métodos para recibir datos

    public void obtenerCorreo(String correoUsuario) {
        this.correoEmpresa = correoUsuario;
        System.out.println("Correo del usuario registrado: " + correoUsuario);
        Main.log.info("Correo del usuario registrado: " + correoEmpresa);
    }

    public void obtenerPlatosModificar(List<Plato> platos) {
        this.platosAModificar = platos;
        System.out.println("Platos leídos correctamente");
    }

    public void obtenerMenu(String menuModificar) {
        this.nombreMenuModificar = menuModificar;
        System.out.println("Menú a modificar: " + menuModificar);
        Main.log.info("Menú a modificar: " + nombreMenuModificar);
    }

// Métodos privados

    // Método para manejar la selección de un plato en el ListView y actualizar los campos con sus datos
    private void onPlatoSeleccionado(MouseEvent event) {
        String nombrePlatoSeleccionado = (String) listaPlatosMenu.getSelectionModel().getSelectedItem();
        System.out.println(nombrePlatoSeleccionado);
        if (nombrePlatoSeleccionado != null) {
            // Buscar el objeto Plato correspondiente al nombre seleccionado
            platoSeleccionado = null;
            // platosAModificar es la lista que contiene los platos y sus datos
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

    private void regenerarPDf() {
        Document document = new Document();
        String pdfPath = "./" + this.nombreMenuModificar + ".pdf";
        File pdfFile = new File(pdfPath);

        // Verificar si el archivo PDF ya está abierto
        if (pdfFile.exists() && !pdfFile.renameTo(pdfFile)) {
            // Mostrar una alerta de que el archivo PDF está abierto
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Advertencia");
            alert.setHeaderText("Archivo PDF Abierto");
            alert.setContentText("Por favor, cierre el archivo PDF antes de regenerarlo.");
            alert.showAndWait();
            return;
        }

        try {
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Agrupar los platos por tipo y agregar al documento
            agregarPlatosPorTipo(document, "PRIMERO");
            agregarPlatosPorTipo(document, "SEGUNDO");
            agregarPlatosPorTipo(document, "POSTRE");

            document.close();

            // Se obtiene el ID del menú deseado a modificar
            int idMenu = supa.obtenerIdMenuPorNombre(this.nombreMenuModificar);
            // Regenero la lista de platos con los platos nuevos agregados o modificados
            List<Plato> platos = supa.obtenerPlatosPorIdMenu(idMenu);
            guardarPDFYMostrar(platos, pdfPath);

            // Llamada al método para subir el PDF al bucket de AWS S3
            s3.subirPDFaS3(properties.getProperty("aws_access_key_id"), properties.getProperty("aws_secret_access_key"),
                    properties.getProperty("aws_session_token"), pdfFile, nombreMenuModificar + ".pdf");

            // Llamada al método para generar un QR que redireccione al PDF alojado en S3
            qr.generarQrYEnlazarConS3("pruebazekrom", nombreMenuModificar + ".pdf", "./"
                            + nombreMenuModificar + ".png",
                    properties.getProperty("aws_access_key_id"),
                    properties.getProperty("aws_secret_access_key"), properties.getProperty("aws_session_token"),
                    botonRegenerarPDF);

            // Abrir un cuadro de diálogo de guardado de archivos
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF");
            fileChooser.setInitialFileName(nombreMenuModificar + ".pdf");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)",
                    "*.pdf");
            fileChooser.getExtensionFilters().add(extFilter);
            Stage stage = (Stage) botonRegenerarPDF.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);

            // Guardar el PDF en la ubicación seleccionada por el usuario
            if (selectedFile != null) {
                try {
                    java.nio.file.Files.copy(new File(pdfPath).toPath(), selectedFile.toPath());
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
        Main.log.info("Se regeneró el PDF para el menú " + nombreMenuModificar);
    }

    // Método para organizar los platos por tipo y agruparlos
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
                    Paragraph tipoTitle = new Paragraph(tipoPlato,
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
                    tipoTitle.setAlignment(Element.ALIGN_CENTER);
                    document.add(tipoTitle);

                    // Agregar un salto de línea después de cada categoría
                    document.add(new Paragraph("\n"));

                    // Obtener la lista de platos para esta categoría
                    List<Plato> platosDeEstaCategoria = platosPorTipo.get(tipoPlato);

                    // Agregar cada plato del tipo al documento
                    for (Plato plato : platosDeEstaCategoria) {
                        // Crear una tabla con dos columnas para el nombre y el precio
                        PdfPTable table = new PdfPTable(2);
                        table.setWidthPercentage(100);
                        table.setWidths(new int[]{70, 30});

                        // Agregar el nombre del plato a la primera celda
                        PdfPCell nombreCell = new PdfPCell(new Phrase(plato.getNombrePlato(),
                                FontFactory.getFont(FontFactory.HELVETICA, 12)));
                        nombreCell.setBorder(PdfPCell.NO_BORDER); // Eliminar borde
                        table.addCell(nombreCell);
                        // Agregar el precio del plato a la segunda celda
                        PdfPCell precioCell = new PdfPCell(new Phrase(String.format("%.2f€", plato.getPrecioPlato()),
                                FontFactory.getFont(FontFactory.HELVETICA, 12)));
                        precioCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        precioCell.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(precioCell);

                        // Agregar la tabla al documento
                        document.add(table);

                        // Agregar la descripción del plato en una nueva línea
                        Paragraph descripcion = new Paragraph(plato.getDescripcionPlato(),
                                FontFactory.getFont(FontFactory.HELVETICA, 10));
                        document.add(descripcion);

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

    // Método para abrir el PDF generado
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

    private void obtenerPlatos() {
        String nombreMenuCodificado = URLEncoder.encode(nombreMenuModificar, StandardCharsets.UTF_8);
        List<Plato> listaPlatos = supa.obtenerPlatosPorIdMenu(supa.obtenerIdMenuPorNombre(nombreMenuCodificado));
        ObservableList<String> nombresPlatos = FXCollections.observableArrayList();
        for (Plato plato : listaPlatos) {
            nombresPlatos.add(plato.getNombrePlato());
        }
        listaPlatosMenu.setItems(nombresPlatos);
        listaPlatosMenu.refresh();
        platosAModificar = listaPlatos;
    }

    // Limpiar los campos en la pantalla de crear
    @FXML
    private void onClickBotonLimpiar() {
        if (!textfieldNombreMenu.isEditable()) {
            textfieldNombreMenu.setText(nombreMenuNuevo);
        } else {
            textfieldNombreMenu.clear();
        }
        textfieldNombrePlato.clear();
        textfieldPrecio.clear();
        textareaDescripcionPlato.clear();
        comboBoxTipoPlato.getSelectionModel().clearSelection();
    }

    //Limpiar los campos en la pantalla de modificar
    @FXML
    private void onClickBotonLimpiarModificar() {
        textfieldNombrePlato.clear();
        textfieldPrecio.clear();
        textareaDescripcionPlato.clear();
        comboBoxTipoPlato.getSelectionModel().clearSelection();
        listaPlatosMenu.getSelectionModel().clearSelection();
    }

    //Método auxiliar para verificar si una cadena es numérica
    private boolean esUnNumero(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}