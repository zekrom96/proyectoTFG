package classes.services;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

//TODO RECUERDA EL DIA LA PRESENTACION ENTRAR A AWS Y COGER LOS TOKENS NUEVOS
public class AwsS3 {
    public void subirPDFaS3(String clave_acceso, String clave_secreta, String token, File pdf, String nombreMenu) {
        // Crea un proveedor de credenciales estático con las credenciales temporales
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(clave_acceso,
                clave_secreta, token);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(sessionCredentials);

        // Crea el cliente de S3 utilizando el proveedor de credenciales estático
        // Nombre del bucket y archivo local

        try (S3Client s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Especifica tu región
                .credentialsProvider(credentialsProvider)
                .build())
        {
            String bucketName = "pruebazekrom";
            // Subir archivo al bucket
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(nombreMenu)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(pdf));
            System.out.println("¡Archivo subido exitosamente a S3!");
        } catch (Exception e) {
            System.err.println("Error al subir archivo a S3: " + e.getMessage());
        }
    }
}