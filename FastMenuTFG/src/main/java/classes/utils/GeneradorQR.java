package classes.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

public class GeneradorQR {

    public void generarQR(String bucketName, String objectKey, String qrFilePath, String keyid, String keysecret,
                          String token) {
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
        } catch (IOException | WriterException | S3Exception e) {
            System.err.println("Error al generar el código QR: " + e.getMessage());
        } finally {
            // Cerrar cliente de S3 y S3Presigner
            s3Client.close();
            presigner.close();
        }
    }
}