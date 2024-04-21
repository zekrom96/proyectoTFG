package models;

public class Menu {
    String nombre;
    int id_empresa;

    public Menu(String nombre, int id_empresa) {
        this.nombre = nombre;
        this.id_empresa = id_empresa;
    }

    public String getNombre() {
        return nombre;
    }

    public int getId_empresa() {
        return id_empresa;
    }
}
