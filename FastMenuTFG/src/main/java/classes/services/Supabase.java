package classes.services;

import classes.utils.CifradoyDescifrado;
import models.Empresa;
import models.Menu;
import models.Plato;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Supabase {

    Properties properties = new Properties();
    CifradoyDescifrado crypt;

    // Cargo el fichero properties en el constructor de la clase para luego usar las variables tengo almacenadas
    public Supabase() {
        try {
            properties.load(getClass().getResourceAsStream("/properties/configuraciones.properties"));
            System.out.println("Archivo properties cargado correctamente.");
            crypt = new CifradoyDescifrado(properties.getProperty("secret_key"));
        } catch (IOException e) {
            System.err.println("Error al cargar el archivo properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

   //**********************************PARTE DE AGREGAR DATOS A LAS TABLAS)********************************************


    /*
     Metodo para agregar Platos a la tabla Platos en Supabase y asociandolos con el id de empresa actual que se
     ha logueado o registrado en la aplicacion
    */
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


    //Metodo para agregar el nombre y correo de la empresa en la tabla Empresa en Supabase
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
            } else {
                System.out.println("Error al agregar la empresa. Código de estado: " + codigoStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Metodo agregar un nuevo Menu a la tabla se le pasa el id de la empresa actual tambien
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

    //**********************************PARTE DE RECUPERAR DATOS********************************************************

    //Metodo para recuperar el id de una empresa en Supabase, dado un correo
    public int obtenerIdEmpresaPorCorreo(String correoEmpresa) {
        try {
            // URL del endpoint para obtener datos en Supabase
            String url = properties.getProperty("supabase_url_empresa") + "?correo=eq." + correoEmpresa;

            // API Key
            String apiKey = properties.getProperty("supabase_key");

            // Construir la solicitud HTTP GET
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            // Agregar la API Key a la cabecera de la solicitud
            httpGet.setHeader("apikey", apiKey);

            // Ejecutar la solicitud HTTP GET
            HttpResponse response = httpClient.execute(httpGet);

            // Leer la respuesta
            String responseBody = EntityUtils.toString(response.getEntity());

            // Analizar la respuesta JSON
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

    public List<String> obtenerNombresPlatosPorIdMenu(int idMenu) {
        List<String> nombresPlatos = new ArrayList<>();
        try {
            String url = properties.getProperty("supabase_url_platos") + "?id_menu=eq." + idMenu;
            String apiKey = properties.getProperty("supabase_key");

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity());

            // Verificar si la respuesta es un arreglo JSON
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject platoJson = jsonArray.getJSONObject(i);
                String nombrePlato = platoJson.getString("nombre");
                nombresPlatos.add(nombrePlato);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return nombresPlatos;
    }


    public int obtenerIdMenuPorNombre(String nombreMenu) {
        try {
            // URL del endpoint para obtener datos en Supabase
            String url = properties.getProperty("supabase_url_menus") + "?Nombre=eq." + nombreMenu;

            // API Key
            String apiKey = properties.getProperty("supabase_key");

            // Construir la solicitud HTTP GET
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            // Agregar la API Key a la cabecera de la solicitud
            httpGet.setHeader("apikey", apiKey);

            // Ejecutar la solicitud HTTP GET
            HttpResponse response = httpClient.execute(httpGet);

            // Leer la respuesta
            String responseBody = EntityUtils.toString(response.getEntity());

            // Analizar la respuesta JSON
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
            String apiKey = properties.getProperty("supabase_key");

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity());

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

            String apiKey = properties.getProperty("supabase_key");

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("apikey", apiKey);

            HttpResponse response = httpClient.execute(httpGet);

            String responseBody = EntityUtils.toString(response.getEntity());

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


    //**************************************PARTE DE AUTH SIMULADA******************************************************

    /*
    Metodo crea un usuario en tabla de usuarios, se le pasa como argumento el nombre usuario, el correo y la pw
    La contraseña que el envia en texto plano se guarda cifrada en la bd
    El campo de email es un campo unico en la tabla Usuarios, por lo que al registrarse compruebo no exista, si existe
    lanzo una alerta al usuario
     */
    public void crearUsuario(String password, String correo) {
        try {
            HttpClient httpClient = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_usuarios"));

            JSONObject userData = new JSONObject();
            userData.put("password", password);
            userData.put("email", correo);
            
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
    public boolean iniciarSesion(String email, String pw) {

        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + email;
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONArray jsonArray = new JSONArray(responseBody);

                if (jsonArray.length() > 0) {
                    // Usuario encontrado, recuperar la contraseña cifrada
                    JSONObject userData = jsonArray.getJSONObject(0); // Suponiendo que solo hay un usuario con el mismo nombre
                    String pwCifrada = userData.getString("password");
                    System.out.println(pwCifrada);
                    // Descifrar la contraseña
                    String pwDescifrada = crypt.desencriptar(pwCifrada);
                    System.out.println(pwDescifrada);
                    // Verificar si la contraseña ingresada coincide con la contraseña descifrada
                    if (pw.equals(pwDescifrada)) {
                        // Las contraseñas coinciden, el inicio de sesión es exitoso
                        System.out.println("Inicio de sesión exitoso para el usuario: " + email);
                        return true;
                        // Lógica para continuar con el flujo de la aplicación después del inicio de sesión exitoso
                    } else {
                        // Las contraseñas no coinciden, mostrar un mensaje de error al usuario
                        //TODO CREAR ALERTA
                        System.out.println("La contraseña es incorrecta para el usuario: " + email);
                        return false;
                    }
                } else {
                    //TODO CREAR ALERTA2
                    System.out.println("El usuario " + email + " no existe.");
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

    /*
    Metodo encargado de realizar el cambio de la pw en la bd y comprobar que se ha cambiado correctamente
     */
    public void modificarPassword(String correoUsuario, String nuevaPassword) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("password", nuevaPassword);

            HttpClient httpClient = HttpClients.createDefault();
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;
            HttpPatch httpPatch = new HttpPatch(url);

            mandarSolicitudPath(httpPatch, requestBody);

            HttpResponse response = httpClient.execute(httpPatch);
            int statusCode = response.getStatusLine().getStatusCode();

            //El codigo 204 es por que cuando realiza una accion y no devuelve nada da codigo 204
            //Por eso compruebo manualmente que la pw haya sido cambiada
            if (statusCode == 204) {
                // Verificar si el cambio se realizó correctamente
                boolean cambioRealizado = verificarCambioPassword(correoUsuario, nuevaPassword);
                if (cambioRealizado) {
                    System.out.println("Contraseña actualizada correctamente para el usuario con correo: " + correoUsuario);
                } else {
                    System.out.println("Error: No se pudo confirmar que el cambio se realizó correctamente para el usuario con correo: " + correoUsuario);
                }
            } else {
                System.out.println("Error al actualizar la contraseña para el usuario con correo: " + correoUsuario);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //Metodo encargado comprobar se ha cambiado la contraseña
    public boolean verificarCambioPassword(String correoUsuario, String nuevaPassword) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {

                String responseBody = obtenerContenidoRespuesta(response);
                JSONArray jsonArray = new JSONArray(responseBody);

                if (jsonArray.length() > 0) {
                    JSONObject usuario = jsonArray.getJSONObject(0);
                    String passwordActual = usuario.getString("password");
                    return passwordActual.equals(nuevaPassword);
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
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

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
    public boolean comprobarEstadoCampoRestablecerPw(String correoUsuario) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

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

    public boolean comprobarEstadoCampoUsuarioLogueado(String correoUsuario) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

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

    public boolean verificarCambioCampoUsuarioLogueado(String correoUsuario, boolean nuevoValor) {
        try {
            String url = properties.getProperty("supabase_url_usuarios") + "?email=eq." + correoUsuario;

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

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
            httpGet.setHeader("apikey", properties.getProperty("supabase_key"));

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


    private String obtenerContenidoRespuesta(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    private void mandarSolicitudPost(JSONObject json, HttpPost post) throws UnsupportedEncodingException {
        // Configurar entidad JSON para la solicitud
        StringEntity entidad = new StringEntity(json.toString());
        post.setEntity(entidad);

        // Configurar encabezados de la solicitud
        post.setHeader("Content-type", "application/json");
        post.setHeader("apikey", properties.getProperty("supabase_key"));
    }

    private void mandarSolicitudPath(HttpPatch httpPatch, JSONObject requestBody) throws UnsupportedEncodingException {
        httpPatch.setHeader("Content-type", "application/json");
        httpPatch.setHeader("apikey", properties.getProperty("supabase_key"));
        httpPatch.setEntity(new StringEntity(requestBody.toString()));
    }
}
