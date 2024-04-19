package classes;

import java.security.SecureRandom;

public class GeneradorPassword {

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";

    //Metodo me genera un pw aleatoria entre 32 y 64 de longitud, sin cifrar
    public static String generarPassword() {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        int longitud = random.nextInt(33) + 32; // Genera un número aleatorio entre 32 y 64, ambos inclusive
        for (int i = 0; i < longitud; i++) {
            int index = random.nextInt(CARACTERES.length());
            sb.append(CARACTERES.charAt(index));
        }
        return sb.toString();
    }
}
