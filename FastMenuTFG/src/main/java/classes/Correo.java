package classes;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Correo {

    // Metodo envia un correo dado un origen, una key, un destino y la nuevapw
    public void enviarGmail(String origen, String key, String destinatario,
                            String nuevaPw) throws MessagingException {
        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.host", "smtp.gmail.com");
        propiedades.put("mail.smtp.port", "587");
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.starttls.enable", "true");

        Session sesion = Session.getInstance(propiedades, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(origen, key);
            }
        });

        Message mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(origen));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject("Restablecimiento de contraseña");
        mensaje.setText("Nueva password temporal generada use la siguiente en el proximo inicio de sesion: " + nuevaPw);

        Transport.send(mensaje);
        System.out.println("Correo electrónico enviado satisfactoriamente.");
    }
}