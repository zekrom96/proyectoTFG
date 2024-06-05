package classes.services;

import fastmenu.Main;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Correo {
    // Metodo envia un correo dado un origen, una key, un destino y la nueva contraseña
    public void enviarGmail(String origen, String key, String destinatario, String nuevaPw) throws MessagingException {
        Main.log.info("Iniciando el proceso de envío de correo electrónico");

        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.host", "smtp.gmail.com");
        propiedades.put("mail.smtp.port", "587");
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.starttls.enable", "true");

        Session sesionCorreo = Session.getInstance(propiedades, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(origen, key);
            }
        });

        try {
            Message mensaje = new MimeMessage(sesionCorreo);
            mensaje.setFrom(new InternetAddress(origen));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            mensaje.setSubject("Restablecimiento de contraseña");
            mensaje.setText("Nueva password temporal generada use la siguiente en el próximo inicio de sesión: " + nuevaPw);

            Transport.send(mensaje);
            Main.log.info("Correo electrónico enviado satisfactoriamente a " + destinatario);
        } catch (MessagingException e) {
            Main.log.error("Error al enviar el correo electrónico: " + e.getMessage(), e);
            throw e;
        }
    }
}