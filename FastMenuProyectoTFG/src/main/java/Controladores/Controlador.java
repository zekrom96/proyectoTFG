package Controladores;

import com.example.fastmenuproyectotfg.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.util.*;


public class Controlador implements Initializable {
    private String correoEmpresa;
    public TextField textfieldNombrePlato, textfieldPrecio, textfieldNombreMenu;
    public TextArea textareaDescripcionPlato;
    public ComboBox comboBoxTipoPlato;
    public Button botonAgregarPlato, botonGenerarPDF, botonSalir;
    public ListView listaPlatos;
    private ObservableList<Plato> listaPlatosObservable = FXCollections.observableArrayList();
    private List<Plato> platos = new ArrayList<>();
    Properties properties = new Properties();
    bdSupabase supa;
    awsS3 s3 = new awsS3();
    GeneradorQR qr = new GeneradorQR();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            properties.load(getClass().getResourceAsStream("/properties/configuraciones.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        supa = new bdSupabase();
        comboBoxTipoPlato.getItems().addAll("PRIMERO", "SEGUNDO", "POSTRE");
    }

    public void onClickBotonPDF(MouseEvent mouseEvent) {
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

            // Lógica para agregar la empresa y sus platos a la base de datos
            int idEmpresa = supa.obtenerIdEmpresaPorCorreo(correoEmpresa);
            System.out.println(idEmpresa);
            supa.agregarMenu(textfieldNombreMenu.getText(), idEmpresa);
            int idMenu = supa.obtenerIdMenuPorIdEmpresa(textfieldNombreMenu.getText(), idEmpresa);
            for (Plato plato : platos) {
                supa.agregarPlato(plato.getNombrePlato(), plato.getDescripcionPlato(), plato.getTipoPlato(),
                        plato.getPrecioPlato(), idEmpresa, idMenu);
            }
            s3.subirPDFaS3(properties.getProperty("aws_access_key_id"), properties.getProperty("aws_secret_access_key"),
                    properties.getProperty("aws_session_token"), pdfPath);
            qr.generarQR("pruebazekrom", "archivo2.txt", "./qrzekrom.png",
                    properties.getProperty("aws_access_key_id"),
                    properties.getProperty("aws_secret_access_key"), properties.getProperty("aws_session_token"));
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
                }
            }
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
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
                    Paragraph tipoTitle = new Paragraph(tipoPlato);
                    tipoTitle.setAlignment(Element.ALIGN_CENTER);
                    document.add(tipoTitle);

                    // Agregar cada plato del tipo al documento
                    for (Plato plato : platosPorTipo.get(tipoPlato)) {
                        document.add(new Paragraph(plato.toString()));
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
            //s3.descargarPDFdeS3(properties.getProperty("aws_access_key_id"), properties.getProperty("aws_secret_access_key"),
                    //properties.getProperty("aws_session_token"));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarPDF(String pdfPath) {
        try {
            File file = new File(pdfPath);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                System.out.println("No se puede abrir el archivo PDF automáticamente.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickBotonAgregar(MouseEvent mouseEvent) {
        if (!textareaDescripcionPlato.getText().isEmpty() && !textfieldNombrePlato.getText().isEmpty() &&
                !comboBoxTipoPlato.getSelectionModel().isEmpty()) {
            Plato nuevoPlato = new Plato(textfieldNombrePlato.getText(), textareaDescripcionPlato.getText(),
                    comboBoxTipoPlato.getSelectionModel().getSelectedItem().toString(),
                    Double.parseDouble(textfieldPrecio.getText()));
            platos.add(nuevoPlato);
            listaPlatosObservable.setAll(platos);
            listaPlatos.setItems(listaPlatosObservable);
            listaPlatos.refresh();
        } else {
            // Mostrar una alerta de error
            Alert alertaError = new Alert(Alert.AlertType.ERROR);
            alertaError.setTitle("Error");
            alertaError.setHeaderText("Este es un mensaje de error");
            alertaError.setContentText("Todos los datos han de ser rellenados");
            alertaError.showAndWait();
        }
    }

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

    public void obtenerCorreo(String correoUsuario) {
        this.correoEmpresa = correoUsuario;
        // Hacer lo que necesites con el correo del usuario, como mostrarlo en la nueva vista
        System.out.println("Correo del usuario registrado: " + correoUsuario);
    }

    public void onClickBotonSalir(MouseEvent mouseEvent) {
        supa.cambiarPw("davidpeka20@gmail.com", "pelopicopata96",
                properties.getProperty("supabase_url"), properties.getProperty("supabase_key"), properties.getProperty("supabase_bearer_token"));
        Platform.exit();
    }
    public void onBtnClickPrevisualizar(MouseEvent mouseEvent) {
        String pdfPath = "./" + textfieldNombreMenu.getText() + ".pdf";
        guardarPDFYMostrar(platos, pdfPath);
    }

}