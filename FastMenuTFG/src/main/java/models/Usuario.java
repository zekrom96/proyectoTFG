package models;

public class Usuario {
    private String password;
    private String email;
    private boolean restablecer_pw;
    private boolean usuario_logueado;

    public Usuario(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public boolean isRestablecer_pw() {
        return restablecer_pw;
    }

    public void setRestablecer_pw(boolean restablecer_pw) {
        this.restablecer_pw = restablecer_pw;
    }

    public boolean isUsuario_logueado() {
        return usuario_logueado;
    }

    public void setUsuario_logueado(boolean usuario_logueado) {
        this.usuario_logueado = usuario_logueado;
    }

}
