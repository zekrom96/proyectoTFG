package com.example.fastmenuproyectotfg;

public class Empresa {
    String nombreEmpresa;

    public Empresa(String nombreEmpresa) {
        this.nombreEmpresa = nombreEmpresa;
    }

    @Override
    public String toString() {
        return "Empresa{" +
                "nombreEmpresa='" + nombreEmpresa + '\'' +
                '}';
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }
}
