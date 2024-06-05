package classes.utils;

import fastmenu.Main;

import java.security.SecureRandom;

public class GeneradorPassword {
    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
    //Metodo genera un pw aleatoria entre 32 y 64 de longitud, sin cifrar
    public static String generarPassword() {
        try {
            StringBuilder sb = new StringBuilder();
            SecureRandom random = new SecureRandom();
            int longitud = random.nextInt(33) + 32; // Genera un número aleatorio entre 32 y 64, ambos inclusive
            for (int i = 0; i < longitud; i++) {
                int index = random.nextInt(CARACTERES.length());
                sb.append(CARACTERES.charAt(index));
            }
            String passwordGenerada = sb.toString();
            Main.log.info("Contraseña generada: {}", passwordGenerada);
            return passwordGenerada;
        } catch (Exception e) {
            Main.log.error("Error al generar la contraseña: {}", e.getMessage());
            return null;
        }
    }
}
