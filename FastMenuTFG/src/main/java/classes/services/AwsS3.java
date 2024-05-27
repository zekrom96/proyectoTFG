package classes.services;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

public class AwsS3 {
    //TODO RECUERDA EL DIA LA PRESENTACION ENTRAR A AWS Y COGER LOS TOKENS NUEVOS
    public void subirPDFaS3(String clave_acceso, String clave_secreta, String token, File pdf, String nombreMenu) {
        // Crea un proveedor de credenciales estático con las credenciales temporales
        AwsSessionCredentials credencialesSesion = AwsSessionCredentials.create(clave_acceso,
                clave_secreta, token);
        StaticCredentialsProvider credencialesProveedor = StaticCredentialsProvider.create(credencialesSesion);
        // Crea el cliente de S3 utilizando el proveedor de credenciales estático
        try (S3Client clienteS3 = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(credencialesProveedor).build())
        {
            // Nombre del bucket y archivo local
            String nombreBucket = "pruebazekrom";
            // Subir archivo al bucket
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(nombreBucket)
                    .key(nombreMenu)
                    .build();
            clienteS3.putObject(request, RequestBody.fromFile(pdf));
            System.out.println("¡Archivo subido exitosamente a S3!");
        } catch (Exception e) {
            System.err.println("Error al subir archivo a S3: " + e.getMessage());
        }
    }
}