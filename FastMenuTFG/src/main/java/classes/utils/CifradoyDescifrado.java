package classes.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class CifradoyDescifrado {

    private final String clave;

    public CifradoyDescifrado(String clave) {
        this.clave = clave;
    }

    public String encriptar(String texto) {
        try {
            // Generar una sal aleatoria
            byte[] sal = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(sal);

            // Crear la clave secreta y el vector de inicialización
            SecretKeySpec keySpec = new SecretKeySpec(clave.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(sal);

            // Inicializar el cifrado
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // Encriptar el texto
            byte[] textoCifrado = cipher.doFinal(texto.getBytes());

            // Concatenar el sal al texto cifrado
            byte[] resultado = new byte[sal.length + textoCifrado.length];
            System.arraycopy(sal, 0, resultado, 0, sal.length);
            System.arraycopy(textoCifrado, 0, resultado, sal.length, textoCifrado.length);

            // Codificar el resultado en Base64 y devolverlo como una cadena
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String desencriptar(String textoCifrado) {
        try {
            // Decodificar el texto cifrado desde Base64
            byte[] bytesCifrado = Base64.getDecoder().decode(textoCifrado);

            // Extraer el sal y el texto cifrado
            byte[] sal = new byte[16];
            byte[] textoCifradoBytes = new byte[bytesCifrado.length - 16];
            System.arraycopy(bytesCifrado, 0, sal, 0, 16);
            System.arraycopy(bytesCifrado, 16, textoCifradoBytes, 0, bytesCifrado.length - 16);

            // Crear la clave secreta y el vector de inicialización
            SecretKeySpec keySpec = new SecretKeySpec(clave.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(sal);

            // Inicializar el descifrado
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // Descifrar el texto cifrado
            byte[] textoBytes = cipher.doFinal(textoCifradoBytes);

            // Devolver el texto descifrado como una cadena
            return new String(textoBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

