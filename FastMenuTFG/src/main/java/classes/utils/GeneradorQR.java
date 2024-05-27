package classes.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import controllers.Controlador;
import fastmenu.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.sound.sampled.Control;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

public class GeneradorQR {

    public void generarQRYSubirAs3(String bucketName, String objectKey, String qrFilePath, String keyid, String keysecret,
                                   String token, Button botonGenerarPDF) {
        int width = 300;
        int height = 300;
        String format = "png";
        // Configurar credenciales de AWS
        AwsSessionCredentials credentials = AwsSessionCredentials.create(keyid, keysecret, token);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        // Crear cliente de S3
        S3Client s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Reemplazar con tu región
                .credentialsProvider(credentialsProvider)
                .build();

        // Crear S3Presigner
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1) // Reemplazar con tu región
                .credentialsProvider(credentialsProvider)
                .build();

        try {
            // Generar URL prefirmada para descargar el objeto desde S3
            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(
                    builder -> builder
                            .getObjectRequest(r -> r
                                    .bucket(bucketName)
                                    .key(objectKey))
                            .signatureDuration(Duration.ofDays(7)));

            String presignedUrl = presignedGetObjectRequest.url().toString();

            // Generar el código QR con la URL prefirmada incrustada
            BitMatrix bitMatrix = new QRCodeWriter().encode(presignedUrl, BarcodeFormat.QR_CODE, width, height);

            // Escribir el código QR en un archivo
            Path path = FileSystems.getDefault().getPath(qrFilePath);
            MatrixToImageWriter.writeToPath(bitMatrix, format, path);
            System.out.println("Código QR generado con éxito en: " + qrFilePath);

            // Abrir un cuadro de diálogo de guardado de archivos
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PNG");
            fileChooser.setInitialFileName(objectKey.replace(".pdf", "") + ".png"); // Eliminar ".pdf" del nombre de archivo
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos PNG (*.png)",
                    "*.png");
            fileChooser.getExtensionFilters().add(extFilter);
            Stage stage = (Stage) botonGenerarPDF.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);

            // Guardar el PDF en la ubicación seleccionada por el usuario
            if (selectedFile != null) {
                try {
                    String objectKeyWithoutExtension = objectKey.replace(".pdf", ""); // Eliminar la extensión ".pdf"
                    String pdfFile = "./" + objectKeyWithoutExtension + ".png";
                    java.nio.file.Files.copy(new File(pdfFile).toPath(), selectedFile.toPath());
                    System.out.println("PDF guardado en: " + selectedFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Main.log.error("Error al guardar el PDF");
                }
            }
        } catch (IOException | WriterException | S3Exception e) {
            System.err.println("Error al generar el código QR: " + e.getMessage());
        } finally {
            // Cerrar cliente de S3 y S3Presigner
            s3Client.close();
            presigner.close();
        }
    }
}