package com.example.fastmenuproyectotfg;

public class Sesion {
    private static Sesion instance;
    private Boolean usuarioLogueado = false;
    private String correoLogueado = "";
    private String password = "";


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCorreoLogueado() {
        return correoLogueado;
    }

    public void setCorreoLogueado(String correoLogueado) {
        this.correoLogueado = correoLogueado;
    }

    public Boolean getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public void setUsuarioLogueado(Boolean usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;
    }


    public Sesion() {
    }

    public Sesion getInstance() {
        if (instance == null) {
            instance = new Sesion();
        }
        return instance;
    }
}
