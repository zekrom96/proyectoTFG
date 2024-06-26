package models;

public class Plato {

    private String nombrePlato, descripcionPlato, tipoPlato;
    private double precioPlato;

    public Plato(String nombrePlato, String descripcionPlato, String tipoPlato, double precioPlato) {
        this.nombrePlato = nombrePlato;
        this.descripcionPlato = descripcionPlato;
        this.tipoPlato = tipoPlato;
        this.precioPlato = precioPlato;
    }

    @Override
    public String toString() {
        return nombrePlato + "\t\t\t\t\t" + precioPlato + "€" + "\n" + descripcionPlato;
    }

    public String getNombrePlato() {
        return nombrePlato;
    }

    public String getDescripcionPlato() {
        return descripcionPlato;
    }

    public String getTipoPlato() {
        return tipoPlato;
    }

    public double getPrecioPlato() {
        return precioPlato;
    }
}
