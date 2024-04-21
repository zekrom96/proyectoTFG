package models;

public class Empresa {

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

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

}
