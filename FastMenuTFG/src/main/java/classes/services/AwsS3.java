package classes.services;

import fastmenu.Main;
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
        Main.log.info("Iniciando el proceso de subida del archivo PDF a S3");
        // Crea un proveedor de credenciales estático con las credenciales temporales
        AwsSessionCredentials credencialesSesion = AwsSessionCredentials.create(clave_acceso, clave_secreta, token);
        StaticCredentialsProvider credencialesProveedor = StaticCredentialsProvider.create(credencialesSesion);
        // Crea el cliente de S3 utilizando el proveedor de credenciales estático
        try (S3Client clienteS3 = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(credencialesProveedor).build()) {
            Main.log.info("Cliente S3 creado con éxito");

            // Nombre del bucket y archivo local
            String nombreBucket = "pruebazekrom";
            Main.log.info("Nombre del bucket: " + nombreBucket);

            // Subir archivo al bucket
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(nombreBucket)
                    .key(nombreMenu)
                    .build();
            clienteS3.putObject(request, RequestBody.fromFile(pdf));
            Main.log.info("¡Archivo subido exitosamente a S3!");
        } catch (Exception e) {
            Main.log.error("Error al subir archivo a S3: " + e.getMessage());
        }
    }
}