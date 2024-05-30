package classes.services;

import classes.utils.CifradoyDescifrado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import models.Empresa;
import models.Menu;
import models.Plato;
import models.Usuario;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Supabase {
    Properties properties = new Properties();
    CifradoyDescifrado crypt;
    static String apiKey;

    // Cargo el fichero properties en el constructor de la clase para luego usar las variables tengo almacenadas
    public Supabase() {
        try {
            properties.load(getClass().getResourceAsStream("/properties/configuraciones.properties"));
            apiKey = properties.getProperty("supabase_key");
            System.out.println("Archivo properties cargado correctamente.");
            crypt = new CifradoyDescifrado(properties.getProperty("secret_key"));
        } catch (IOException e) {
            System.err.println("Error al cargar el archivo properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

   /*
   //**********************************PARTE DE AGREGAR DATOS A LAS TABLAS********************************************
    *                                                                                                                *
    *                                                                                                                *
    ******************************************************************************************************************/

    public void agregarPlato(Plato plato, int idEmpresa, int idMenu) {
        try {
            // Crear cliente HTTP y solicitud POST
            HttpClient clienteHttp = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_platos"));

            // Crear objeto JSON con los datos del plato
            JSONObject platoJson = new JSONObject();
            platoJson.put("nombre", plato.getNombrePlato());
            platoJson.put("descripcion", plato.getDescripcionPlato());
            platoJson.put("tipo", plato.getTipoPlato());
            platoJson.put("precio", plato.getPrecioPlato());
            platoJson.put("id_empresa", idEmpresa);
            platoJson.put("id_menu", idMenu);

            mandarSolicitudPost(platoJson, httpPost);

            // Ejecutar la solicitud POST
            HttpResponse response = clienteHttp.execute(httpPost);

            // Obtener el código de estado de la respuesta
            int codigoStatus = response.getStatusLine().getStatusCode();

            // Verificar el código de estado de la respuesta
            if (codigoStatus == 200 || codigoStatus == 201) {
                System.out.println("Plato agregado correctamente.");
            } else {
                System.out.println("Error al agregar el plato. Código de estado: " + codigoStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void agregarEmpresa(Empresa empresa, String correo) {
        try {
            HttpClient clienteHttp = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_empresa"));

            JSONObject empresaJson = new JSONObject();
            empresaJson.put("nombreEmpresa", empresa.getNombreEmpresa());
            empresaJson.put("correo", correo);

            mandarSolicitudPost(empresaJson, httpPost);

            HttpResponse respuesta = clienteHttp.execute(httpPost);

            int codigoStatus = respuesta.getStatusLine().getStatusCode();

            if (codigoStatus >= 200 && codigoStatus < 300) {
                System.out.println("La empresa se agregó correctamente.");
            } else if (codigoStatus == 409) {
                Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                alerta.setTitle("Información");
                alerta.setHeaderText("Ya existe la empresa");
                alerta.setContentText("No se pudo crear la empresa");
                alerta.showAndWait();
                System.out.println("Error al agregar la empresa. Conflicto: la empresa ya existe.");
                String contenidoRespuesta = obtenerContenidoRespuesta(respuesta);
                System.out.println("Contenido de la respuesta: " + contenidoRespuesta);
            } else {
                System.out.println("Error al agregar la empresa. Código de estado: " + codigoStatus);
                String contenidoRespuesta = obtenerContenidoRespuesta(respuesta);
                System.out.println("Contenido de la respuesta: " + contenidoRespuesta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void agregarMenu(Menu menu) {
        try {
            HttpClient clienteHttp = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_menus"));

            JSONObject empresaJson = new JSONObject();
            empresaJson.put("Nombre", menu.getNombre());
            empresaJson.put("id_empresa", menu.getId_empresa());

           mandarSolicitudPost(empresaJson, httpPost);

            HttpResponse respuesta = clienteHttp.execute(httpPost);

            int codigoStatus = respuesta.getStatusLine().getStatusCode();

            if (codigoStatus >= 200 && codigoStatus < 300) {
                System.out.println("El menu se agregó correctamente.");
            } else {
                System.out.println("Error al agregar el menu. Código de estado: " + codigoStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //**********************************PARTE DE RECUPERAR DATOS******************************************************//

    public int obtenerIdEmpresaPorCorreo(String correoEmpresa) {
        try {
            String url = properties.getProperty("supabase_url_empresa") + "?correo=eq." + correoEmpresa;
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("apikey", apiKey);

            // Ejecutar la solicitud HTTP GET
            HttpResponse response = httpClient.execute(httpGet);

            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(responseBody);
            if (jsonArray.length() > 0) {
                // Obtener el ID de la primera empresa encontrada
                JSONObject empresaJson = jsonArray.getJSONObject(0);
                return empresaJson.getInt("id");
            } else {
                System.out.println("No se encontro ninguna empresa con el correo indicado");
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<Plato> obtenerPlatosPorIdMenu(int idMenu) {
        List<Plato> platos = new ArrayList<>();
        try {
            String url = properties.getProperty("supabase_url_platos") + "?id_menu=eq." + idMenu;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            // Verificar si la respuesta es un arreglo JSON
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject platoJson = jsonArray.getJSONObject(i);
                String nombre = platoJson.getString("nombre");
                double precio = platoJson.getDouble("precio");
                String tipoPlato = platoJson.getString("tipo");
                String descripcion = platoJson.getString("descripcion");

                // Crear objeto Plato y añadirlo a la lista
                Plato plato = new Plato(nombre, descripcion, tipoPlato, precio);
                platos.add(plato);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return platos;
    }


    public int obtenerIdMenuPorNombre(String nombreMenu) {
        try {
            String nombreMenuCodificado = URLEncoder.encode(nombreMenu, StandardCharsets.UTF_8);
            String url = properties.getProperty("supabase_url_menus") + "?Nombre=eq." + nombreMenu;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);

            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(responseBody);
            if (jsonArray.length() > 0) {
                // Obtener el ID de la primera empresa encontrada
                JSONObject empresaJson = jsonArray.getJSONObject(0);
                return empresaJson.getInt("id");
            } else {
                System.out.println("No se encontro ninguna menu");
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<String> obtenerNombresMenuPorIdEmpresa(int idEmpresa) {
        List<String> nombresMenus = new ArrayList<>();
        try {
            String url = properties.getProperty("supabase_url_menus") + "?id_empresa=eq." + idEmpresa;
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject menuJson = jsonArray.getJSONObject(i);
                String nombreMenu = menuJson.getString("Nombre");
                nombresMenus.add(nombreMenu);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nombresMenus;
    }

    //Metodo obtener el id de menu asignado a una empresa
    public int obtenerIdMenuPorIdEmpresa(Menu menu) {
        try {
            String url = properties.getProperty("supabase_url_menus") + "?id_empresa=eq." + menu.getId_empresa() +
                    "&Nombre=eq." + menu.getNombre();

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);

            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(responseBody);
            if (jsonArray.length() > 0) {
                // Obtener el ID de la primera empresa encontrada
                JSONObject empresaJson = jsonArray.getJSONObject(0);
                return empresaJson.getInt("id");
            } else {
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    //Recupera un plato por su id_empresa, nombre, y id_menu
    public int recuperarIdPlato(String nombrePlato, int id_empresa, int id_menu) {
        int idPlato = 0;
        try {
            HttpClient clienteHttp = HttpClients.createDefault();

            String url = properties.getProperty("supabase_url_platos") +
                    "?nombre=eq." + URLEncoder.encode(nombrePlato, StandardCharsets.UTF_8.toString()) +
                    "&id_empresa=eq." + id_empresa + "&id_menu=eq." + id_menu;

            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = clienteHttp.execute(httpGet);

            int codigoStatus = response.getStatusLine().getStatusCode();

            if (codigoStatus == 200) {
                String contenidoRespuesta = obtenerContenidoRespuesta(response);
                JSONArray platos = new JSONArray(contenidoRespuesta);
                if (platos.length() > 0) {
                    JSONObject primerPlato = platos.getJSONObject(0);
                    System.out.println(primerPlato);
                    idPlato = primerPlato.getInt("id_plato");
                } else {
                    System.out.println("No se encontró ningún plato con el nombre '" + nombrePlato);
                }
            } else {
                System.out.println("Error al recuperar el ID del plato. Código de estado: " + codigoStatus);
                String contenidoRespuesta = obtenerContenidoRespuesta(response);
                System.out.println("Contenido de la respuesta: " + contenidoRespuesta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(idPlato);
        return idPlato;
    }


      /*
   //**********************************PARTE DE AUTH SIMULADA*********************************************************
    *                                                                                                                *
    *                                                                                                                *
    ******************************************************************************************************************/

    /*
    Metodo crea un usuario en tabla de usuarios
     */
    public void crearUsuario(Usuario usuario) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_usuarios"));
            JSONObject userData = new JSONObject();
            userData.put("password", usuario.getPassword());
            userData.put("email", usuario.getEmail());
            
            mandarSolicitudPost(userData, httpPost);

            // Ejecutar la solicitud HTTP
            HttpResponse response = httpClient.execute(httpPost);

            // Verificar el código de estado de la respuesta
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                System.out.println("Usuario creado correctamente.");
            } else {
                System.out.println("Error al crear usuario. Código de estado: " + statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
    Metodo "autenticarse" contra la bd de Usuarios, por un lado recupero de la bd la pw cifrada, la descifro y compruebo
    que son la misma contraseña, si es asi podrá acceder a su panel de modificar o crear
     */
    public boolean iniciarSesion(Usuario usuario) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + usuario.getEmail();
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONArray jsonArray = new JSONArray(responseBody);

                if (jsonArray.length() > 0) {
                    // Usuario encontrado, recuperar la contraseña cifrada
                    JSONObject userData = jsonArray.getJSONObject(0);
                    String pwCifrada = userData.getString("password");
                    System.out.println(pwCifrada);
                    // Descifrar la contraseña
                    String pwDescifrada = crypt.desencriptar(pwCifrada);
                    System.out.println(pwDescifrada);
                    // Verificar si la contraseña ingresada coincide con la contraseña descifrada
                    if (usuario.getPassword().equals(pwDescifrada)) {
                        // Las contraseñas coinciden, el inicio de sesión es exitoso
                        System.out.println("Inicio de sesión exitoso para el usuario: " + usuario.getEmail());
                        return true;
                        // Lógica para continuar con el flujo de la aplicación después del inicio de sesión exitoso
                    } else {
                        System.out.println("La contraseña es incorrecta para el usuario: " + usuario.getEmail());
                        return false;
                    }
                } else {
                    System.out.println("El usuario " + usuario.getEmail() + " no existe.");
                    return false;
                }
            } else {
                System.out.println("Error al realizar la solicitud HTTP. Código de estado: " + statusCode);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    //Metodo encargado de realizar el cambio de la pw en la bd y comprobar que se ha cambiado correctamente
    public void modificarPassword(Usuario usuario) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("password", usuario.getPassword());
            HttpClient httpClient = HttpClients.createDefault();
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + usuario.getEmail();
            HttpPatch httpPatch = new HttpPatch(url);

            mandarSolicitudPath(httpPatch, requestBody);

            HttpResponse response = httpClient.execute(httpPatch);
            int statusCode = response.getStatusLine().getStatusCode();

            //El codigo 204 es por que cuando realiza una accion y no devuelve nada da codigo 204
            //Por eso compruebo manualmente que la pw haya sido cambiada al menos mientras depuraba la app
            if (statusCode == 204) {
                // Verificar si el cambio se realizó correctamente
                boolean cambioRealizado = verificarCambioPassword(usuario);
                if (cambioRealizado) {
                    System.out.println("Contraseña actualizada correctamente para el usuario con correo: "
                            + usuario.getEmail());
                } else {
                    System.out.println("Error: No se pudo confirmar que el cambio se realizó correctamente"
                            + "para el usuario con correo: " + usuario.getEmail());
                }
            } else {
                System.out.println("Error al actualizar la contraseña para el usuario con correo: "
                        + usuario.getEmail());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //Metodo encargado comprobar se ha cambiado la contraseña
    public boolean verificarCambioPassword(Usuario nuevoUsuario) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + nuevoUsuario.getEmail();

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);
                if (jsonArray.length() > 0) {
                    JSONObject usuario = jsonArray.getJSONObject(0);
                    String passwordActual = usuario.getString("password");
                    return passwordActual.equals(nuevoUsuario.getPassword());
                } else {
                    System.out.println("Error: No se encontró ningún usuario con el correo especificado.");
                    return false;
                }
            } else {
                System.out.println("Error: No se pudo obtener los detalles del usuario. Código de estado: " + statusCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Metodo modifica el campo restablecer_pw de un usuario, la usaré ponerla en true o false segun el caso
    public void modificarCampoUsuarioRestablecerPw(String correoUsuario, boolean nuevoValor) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("restablecer_pw", nuevoValor);

            HttpClient httpClient = HttpClients.createDefault();
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;
            HttpPatch httpPatch = new HttpPatch(url);

            mandarSolicitudPath(httpPatch, requestBody);

            HttpResponse response = httpClient.execute(httpPatch);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println(statusCode);

            if (statusCode == 204) {
                boolean cambioRealizado = verificarCambioCampoRestablecerPw(correoUsuario, nuevoValor);
                if (cambioRealizado) {
                    System.out.println("Campo restablecer_pw actualizado correctamente para el usuario con correo: " + correoUsuario);
                } else {
                    System.out.println("Error: No se pudo confirmar que el cambio se realizó correctamente para el usuario con correo: " + correoUsuario);
                }
            } else {
                System.out.println("Error al actualizar el campo restablecer_pw para el usuario con correo: " + correoUsuario);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Metodo verifica el cambio de valor en el campo restablecer pw
    public boolean verificarCambioCampoRestablecerPw(String correoUsuario, boolean nuevoValor) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {

                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);

                if (jsonArray.length() > 0) {
                    JSONObject usuario = jsonArray.getJSONObject(0);
                    boolean valorActual = usuario.getBoolean("restablecer_pw");
                    return valorActual == nuevoValor;
                } else {
                    System.out.println("Error: No se encontró ningún usuario con el correo especificado.");
                    return false;
                }
            } else {
                System.out.println("Error: No se pudo obtener los detalles del usuario. Código de estado: " + statusCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Metodo poner el campo usuario logueado en x valor de un correo
    public void modificarCampoUsuarioLogueado(String correoUsuario, boolean nuevoValor) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("usuario_logueado", nuevoValor);

            HttpClient httpClient = HttpClients.createDefault();
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;
            HttpPatch httpPatch = new HttpPatch(url);

            mandarSolicitudPath(httpPatch, requestBody);

            HttpResponse response = httpClient.execute(httpPatch);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println(statusCode);

            if (statusCode == 204) {
                boolean cambioRealizado = verificarCambioCampoUsuarioLogueado(correoUsuario, nuevoValor);
                if (cambioRealizado) {
                    System.out.println("Campo usuario logueado actualizado correctamente para el usuario con correo: " + correoUsuario);
                } else {
                    System.out.println("Error: No se pudo confirmar que el cambio se realizó correctamente para el usuario con correo: " + correoUsuario);
                }
            } else {
                System.out.println("Error al actualizar el campo restablecer_pw para el usuario con correo: " + correoUsuario);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Metodo comprueba el estado del campo restablecerpw, si esta en false o true, segun como este el programa hara x
    public boolean comprobarEstadoCampoRestablecerPw(Usuario nuevoUsuario) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + nuevoUsuario.getEmail();

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);
                if (jsonArray.length() > 0) {
                    JSONObject usuario = jsonArray.getJSONObject(0);
                    boolean valorActual = usuario.getBoolean("restablecer_pw");
                    return valorActual;
                } else {
                    System.out.println("Error: No se encontró ningún usuario con el correo especificado.");
                    return false;
                }
            } else {
                System.out.println("Error: No se pudo obtener los detalles del usuario. Código de estado: " + statusCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Metodo comprueba el estado del campo usuario logueado, si esta en false o true, segun como este el programa hara x
    public boolean comprobarEstadoCampoUsuarioLogueado(String correoUsuario) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);
                if (jsonArray.length() > 0) {
                    JSONObject usuario = jsonArray.getJSONObject(0);
                    boolean valorActual = usuario.getBoolean("usuario_logueado");
                    return valorActual;
                } else {
                    System.out.println("Error: No se encontró ningún usuario con el correo especificado.");
                    return false;
                }
            } else {
                System.out.println("Error: No se pudo obtener los detalles del usuario. Código de estado: " + statusCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Comprueba se han realizado los cambios del campo usuario logueado
    public boolean verificarCambioCampoUsuarioLogueado(String correoUsuario, boolean nuevoValor) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);
            HttpResponse response = httpClient.execute(httpGet);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);
                if (jsonArray.length() > 0) {
                    JSONObject usuario = jsonArray.getJSONObject(0);
                    boolean valorActual = usuario.getBoolean("usuario_logueado");
                    return valorActual == nuevoValor;
                } else {
                    System.out.println("Error: No se encontró ningún usuario con el correo especificado.");
                    return false;
                }
            } else {
                System.out.println("Error: No se pudo obtener los detalles del usuario. Código de estado: " + statusCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    //Metodo comprueba si en la tabla usuario existe un correo dado, ya que un valor unico el email
    public boolean comprobarExisteCorreo(String correo) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?select=email&email=eq." + correo;
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", apiKey);
            HttpResponse response = httpClient.execute(httpGet);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);
                return jsonArray.length() > 0;
            } else {
                System.out.println("Error: No se pudo obtener los detalles del usuario. Código de estado: " + statusCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Modifica los datos de un plato o varios
    public void modificarPlatos(Plato platoModificado, String nombrePlatoOriginal, int idMenu, int idEmpresa) {
        try {
                int idPlato = recuperarIdPlato(nombrePlatoOriginal, idEmpresa, idMenu);
                if (idPlato != 0) {
                    HttpClient clienteHttp = HttpClients.createDefault();
                    // Construir la URL de la solicitud PUT utilizando el ID del plato encontrado
                    String urlModificarPlato = properties.getProperty("supabase_url_platos") + "?id_plato=eq." + idPlato;
                    HttpPut httpPut = new HttpPut(urlModificarPlato);
                    httpPut.setHeader("Content-type", "application/json");
                    httpPut.setHeader("apikey", apiKey);

                    // Crear objeto JSON con los datos del plato modificado
                    JSONObject platoJson = new JSONObject();
                    platoJson.put("nombre", platoModificado.getNombrePlato());
                    platoJson.put("descripcion", platoModificado.getDescripcionPlato());
                    platoJson.put("tipo", platoModificado.getTipoPlato());
                    platoJson.put("precio", platoModificado.getPrecioPlato());
                    platoJson.put("id_plato", idPlato);

                    // Agregar el objeto JSON al cuerpo de la solicitud PUT
                    StringEntity entidadJson = new StringEntity(platoJson.toString(), StandardCharsets.UTF_8);
                    httpPut.setEntity(entidadJson);

                    // Ejecutar la solicitud PUT
                    HttpResponse responseModificarPlato = clienteHttp.execute(httpPut);
                    int codigoStatusModificarPlato = responseModificarPlato.getStatusLine().getStatusCode();

                    // Verificar el código de estado de la respuesta
                    if (codigoStatusModificarPlato == 200 || codigoStatusModificarPlato == 204) {
                        System.out.println("Plato modificado correctamente. Nombre: " + platoModificado.getNombrePlato() + ", ID del Menú: " + idMenu);
                    } else {
                        System.out.println("Error al modificar el plato. Código de estado: " + codigoStatusModificarPlato);
                        String contenidoRespuestaModificarPlato = obtenerContenidoRespuesta(responseModificarPlato);
                        System.out.println("Contenido de la respuesta: " + contenidoRespuestaModificarPlato);
                    }
                } else {
                    System.out.println("No se encontró el plato '" + platoModificado.getNombrePlato() + "' en el menú con ID " + idMenu + " de la empresa con ID " + idEmpresa);
                }
            } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void borrarPlato(String nombrePlatoOriginal, int idMenu, int idEmpresa) {
        try {
            int idPlato = recuperarIdPlato(nombrePlatoOriginal, idEmpresa, idMenu);

            if (idPlato != 0) {
                HttpClient clienteHttp = HttpClients.createDefault();
                // Construir la URL de la solicitud DELETE utilizando el ID del plato encontrado
                String urlBorrarPlato = properties.getProperty("supabase_url_platos") + "?id_plato=eq." + idPlato;
                HttpDelete httpDelete = new HttpDelete(urlBorrarPlato);
                httpDelete.setHeader("Content-type", "application/json");
                httpDelete.setHeader("apikey", apiKey);

                // Ejecutar la solicitud DELETE
                HttpResponse responseBorrarPlato = clienteHttp.execute(httpDelete);
                int codigoStatusBorrarPlato = responseBorrarPlato.getStatusLine().getStatusCode();

                // Verificar el código de estado de la respuesta
                if (codigoStatusBorrarPlato == 200 || codigoStatusBorrarPlato == 204) {
                    System.out.println("Plato borrado correctamente. Nombre: " + nombrePlatoOriginal + ", ID del Menú: " + idMenu);
                } else {
                    System.out.println("Error al borrar el plato. Código de estado: " + codigoStatusBorrarPlato);
                    String contenidoRespuestaBorrarPlato = obtenerContenidoRespuesta(responseBorrarPlato);
                    System.out.println("Contenido de la respuesta: " + contenidoRespuestaBorrarPlato);
                }
            } else {
                System.out.println("No se encontró el plato '" + nombrePlatoOriginal + "' en el menú con ID " + idMenu + " de la empresa con ID " + idEmpresa);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void borrarPlatosDeMenu(int idMenu, int idEmpresa) throws IOException {
        // Recuperar todos los platos asignados al menú
        int[] idsPlatos = recuperarIdsPlatosDeMenu(idMenu, idEmpresa);
        HttpClient clienteHttp = HttpClients.createDefault();

        // Borrar cada plato recuperado
        for (int idPlato : idsPlatos) {
            String urlBorrarPlato = properties.getProperty("supabase_url_platos") + "?id_plato=eq." + idPlato;
            HttpDelete httpDelete = new HttpDelete(urlBorrarPlato);
            httpDelete.setHeader("Content-type", "application/json");
            httpDelete.setHeader("apikey", apiKey);

            HttpResponse responseBorrarPlato = clienteHttp.execute(httpDelete);
            int codigoStatusBorrarPlato = responseBorrarPlato.getStatusLine().getStatusCode();

            if (codigoStatusBorrarPlato != 200 && codigoStatusBorrarPlato != 204) {
                String contenidoRespuestaBorrarPlato = obtenerContenidoRespuesta(responseBorrarPlato);
                System.out.println("Error al borrar el plato con ID " + idPlato + ". Código de estado: " + codigoStatusBorrarPlato);
                System.out.println("Contenido de la respuesta: " + contenidoRespuestaBorrarPlato);
            } else {
                System.out.println("Plato borrado" + idPlato + ". Código de estado: " + codigoStatusBorrarPlato);
            }
        }
    }

    public void borrarMenu(String nombre, int idEmpresa) throws IOException {
        HttpClient clienteHttp = HttpClients.createDefault();
        String urlBorrarMenu = properties.getProperty("supabase_url_menus") + "?Nombre=eq." + nombre + "&id_empresa=eq." + idEmpresa;
        HttpDelete httpDelete = new HttpDelete(urlBorrarMenu);
        httpDelete.setHeader("Content-type", "application/json");
        httpDelete.setHeader("apikey", apiKey);

        HttpResponse responseBorrarMenu = clienteHttp.execute(httpDelete);
        int codigoStatusBorrarMenu = responseBorrarMenu.getStatusLine().getStatusCode();

        if (codigoStatusBorrarMenu == 200 || codigoStatusBorrarMenu == 204) {
            System.out.println("Menú borrado correctamente. ID del Menú: " + nombre + ", ID de la Empresa: " + idEmpresa);
        } else {
            String contenidoRespuestaBorrarMenu = obtenerContenidoRespuesta(responseBorrarMenu);
            System.out.println("Error al borrar el menú. Código de estado: " + codigoStatusBorrarMenu);
            System.out.println("Contenido de la respuesta: " + contenidoRespuestaBorrarMenu);
        }
    }

    private int[] recuperarIdsPlatosDeMenu(int idMenu, int idEmpresa) throws IOException {
        HttpClient clienteHttp = HttpClients.createDefault();
        String urlRecuperarPlatos = properties.getProperty("supabase_url_platos") + "?id_menu=eq." + idMenu + "&id_empresa=eq." + idEmpresa;
        HttpGet httpGet = new HttpGet(urlRecuperarPlatos);
        httpGet.setHeader("Content-type", "application/json");
        httpGet.setHeader("apikey", apiKey);

        HttpResponse response = clienteHttp.execute(httpGet);
        int codigoStatus = response.getStatusLine().getStatusCode();

        if (codigoStatus == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.getEntity().getContent());
            List<Integer> idPlatosList = new ArrayList<>();

            for (JsonNode node : rootNode) {
                int idPlato = node.get("id_plato").asInt();
                idPlatosList.add(idPlato);
            }

            return idPlatosList.stream().mapToInt(i -> i).toArray();
        } else {
            System.out.println("Error al recuperar los IDs de los platos. Código de estado: " + codigoStatus);
            String contenidoRespuesta = obtenerContenidoRespuesta(response);
            System.out.println("Contenido de la respuesta: " + contenidoRespuesta);
            return new int[0];
        }
    }

    //*****************************************METODOS PRIVADOS*******************************************************//

    //Imprime la respuesta del servidcr, sirve para la depuracion prinicipalmente
    private String obtenerContenidoRespuesta(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    //Metodo ejecutar las solicitudes post
    private void mandarSolicitudPost(JSONObject json, HttpPost post) {
        // Configurar entidad JSON para la solicitud
        StringEntity entidad = new StringEntity(json.toString(), StandardCharsets.UTF_8);
        post.setEntity(entidad);
        // Configurar encabezados de la solicitud
        post.setHeader("Content-type", "application/json");
        post.setHeader("apikey", apiKey);
    }

    //Metodo ejecutar las solicitudes patch
    private void mandarSolicitudPath(HttpPatch httpPatch, JSONObject requestBody) throws UnsupportedEncodingException {
        httpPatch.setHeader("Content-type", "application/json");
        httpPatch.setHeader("apikey", apiKey);
        httpPatch.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));
    }
}
