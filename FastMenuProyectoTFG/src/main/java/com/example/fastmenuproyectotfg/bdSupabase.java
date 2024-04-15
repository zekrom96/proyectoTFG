package com.example.fastmenuproyectotfg;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;

public class bdSupabase {
    Properties properties = new Properties();
    String accessToken;

    //Cargo el fichero properties en el constructor de la clase para luego usar las variables tengo almacenadas
    public bdSupabase() {
        try {
            properties.load(getClass().getResourceAsStream("/properties/configuraciones.properties"));
            System.out.println("Archivo properties cargado correctamente.");
        } catch (IOException e) {
            System.err.println("Error al cargar el archivo properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
    Metodo para agregar Platos a la tabla Platos en Supabase y enlazandolas con el id de empresa actual que se
    ha logueado o registrado en la App
     */
    public void agregarPlato(String nombrePlato, String descripcionPlato, String tipoPlato, double precioPlato,
                             int idEmpresa, int idMenu) {
        try {
            HttpClient clienteHttp = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_platos"));

            JSONObject platoJson = new JSONObject();
            platoJson.put("nombre", nombrePlato);
            platoJson.put("descripcion", descripcionPlato);
            platoJson.put("tipo", tipoPlato);
            platoJson.put("precio", precioPlato);
            platoJson.put("id_empresa", idEmpresa);
            platoJson.put("id_menu", idMenu);

            StringEntity entidad = new StringEntity(platoJson.toString());
            httpPost.setEntity(entidad);

            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("apikey", properties.getProperty("supabase_key"));

            HttpResponse response = clienteHttp.execute(httpPost);

            // Obtener el código de estado de la respuesta
            int codigoStatus = response.getStatusLine().getStatusCode();

            // Verificar si la operación fue exitosa (código de estado 200 o 201)
            if (codigoStatus == 200 || codigoStatus == 201) {
                System.out.println("Plato agregado correctamente.");
                // Manejar cualquier acción adicional después de agregar el plato
            } else {
                System.out.println("Error al agregar el plato. Código de estado: " + codigoStatus);
                // Manejar el error de acuerdo a tus necesidades
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Metodo para agregar el nombre de la empresa en la tabla Empresa en Supabase
    public void agregarEmpresa(String nombreEmpresa, String correo) {
        try {
            //Me conecto a la URL de la empresa
            HttpClient clienteHttp = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_empresa"));

            // Crear un objeto JSON solo con el nombre de la empresa
            JSONObject empresaJson = new JSONObject();
            empresaJson.put("nombreEmpresa", nombreEmpresa);
            empresaJson.put("correo", correo);

            // Convertir el objeto JSON en una cadena y establecerlo como el cuerpo de la solicitud
            StringEntity entity = new StringEntity(empresaJson.toString());
            httpPost.setEntity(entity);

            // Establecer el encabezado de tipo de contenido como JSON
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("apikey", properties.getProperty("supabase_key"));

            // Ejecutar la solicitud HTTP POST
            HttpResponse respuesta = clienteHttp.execute(httpPost);

            // Obtener el código de estado de la respuesta
            int codigoStatus = respuesta.getStatusLine().getStatusCode();

            // Verificar el código de estado para determinar si la operación fue exitosa
            if (codigoStatus >= 200 && codigoStatus < 300) {
                System.out.println("La empresa se agregó correctamente.");
                // Aquí puedes agregar cualquier lógica adicional que desees ejecutar cuando la empresa se agregue correctamente
            } else {
                System.out.println("Error al agregar la empresa. Código de estado: " + codigoStatus);
                // Aquí puedes manejar el error de acuerdo a tus necesidades
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void agregarMenu(String nombreMenu, int idEmpresa) {
        try {
            //Me conecto a la URL de la empresa
            HttpClient clienteHttp = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url_menus"));

            // Crear un objeto JSON solo con el nombre de la empresa
            JSONObject empresaJson = new JSONObject();
            empresaJson.put("Nombre", nombreMenu);
            empresaJson.put("id_empresa", idEmpresa);

            // Convertir el objeto JSON en una cadena y establecerlo como el cuerpo de la solicitud
            StringEntity entity = new StringEntity(empresaJson.toString());
            httpPost.setEntity(entity);

            // Establecer el encabezado de tipo de contenido como JSON
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("apikey", properties.getProperty("supabase_key"));

            // Ejecutar la solicitud HTTP POST
            HttpResponse respuesta = clienteHttp.execute(httpPost);

            // Obtener el código de estado de la respuesta
            int codigoStatus = respuesta.getStatusLine().getStatusCode();

            // Verificar el código de estado para determinar si la operación fue exitosa
            if (codigoStatus >= 200 && codigoStatus < 300) {
                System.out.println("El menu se agregó correctamente.");
                // Aquí puedes agregar cualquier lógica adicional que desees ejecutar cuando la empresa se agregue correctamente
            } else {
                System.out.println("Error al agregar el menu. Código de estado: " + codigoStatus);
                // Aquí puedes manejar el error de acuerdo a tus necesidades
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
    Metodo para recuperar los Platos de una determinada Empresa, al loguearse el usuario indica un correo, en cada
    Empresa asocio el correo a la Empresa, por lo que busco la empresa con ese correo, recupero el id de la empresa
    y luego busco los platos con ese id de empresa
     */
    public void recuperarPlatosPorCorreo(String correoEmpresa) {
        try {
            // 1. Buscar en la tabla Empresa la empresa con el correo dado
            int idEmpresa = obtenerIdEmpresaPorCorreo(correoEmpresa);

            if (idEmpresa != -1) {
                // 2. Recuperar los platos asociados con el ID de la empresa
                HttpClient httpClient = HttpClients.createDefault();
                String urlPlatos = properties.getProperty("supabase_url_platos") + "?id_empresa=eq." + idEmpresa;
                HttpGet httpGetPlatos = new HttpGet(urlPlatos);
                httpGetPlatos.setHeader("apikey", properties.getProperty("supabase_key"));

                HttpResponse responsePlatos = httpClient.execute(httpGetPlatos);
                int codigoStatusPlatos = responsePlatos.getStatusLine().getStatusCode();

                if (codigoStatusPlatos == 200) {
                    // Convertir la respuesta a JSON
                    String jsonPlatos = EntityUtils.toString(responsePlatos.getEntity());
                    JSONArray platos = new JSONArray(jsonPlatos);

                    // Procesar los platos recuperados
                    for (int i = 0; i < platos.length(); i++) {
                        JSONObject plato = platos.getJSONObject(i);
                        // Aquí puedes hacer lo que necesites con cada plato
                        System.out.println(plato);
                    }
                } else {
                    System.out.println("Error al recuperar los platos. Código de estado: " + codigoStatusPlatos);
                }
            } else {
                System.out.println("No se encontró ninguna empresa con el correo electrónico especificado.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                // No se encontró ninguna empresa con el correo electrónico especificado
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    //Metodo para crear un usuario en la Auth de Supabase con correo y contraseña
    public void crearUsuario(String email, String password) {
        try {
            // Crear un cliente HTTP
            HttpClient httpClient = HttpClients.createDefault();

            // Definir la URL para crear un nuevo usuario en Supabase
            HttpPost httpPost = new HttpPost(properties.getProperty("supabase_url") + "/auth/v1/signup");

            // Crear un objeto JSON con los datos del nuevo usuario
            JSONObject userData = new JSONObject();
            userData.put("email", email);
            userData.put("password", password);

            // Configurar la solicitud HTTP con el cuerpo JSON
            StringEntity entity = new StringEntity(userData.toString());
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("apikey", properties.getProperty("supabase_key"));

            // Ejecutar la solicitud HTTP
            HttpResponse response = httpClient.execute(httpPost);

            // Verificar el código de estado de la respuesta
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                System.out.println("Usuario creado correctamente.");
            } else {
                System.out.println("Error al crear usuario. Código de estado: " + statusCode);
                // Puedes manejar el error de acuerdo a tus necesidades
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Metodo para iniciar sesion en supabase, el usuario manda su correo y contraseña y se comprueba este en Auth
    public boolean iniciarSesion(String email, String password) {
        try {
            String signInUrl = properties.getProperty("supabase_url") + "/auth/v1/token?grant_type=password";

            // Crea el objeto JSON con los datos de inicio de sesión
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("password", password);

            // Convierte el JSON a una cadena y envíalo en el cuerpo de la solicitud
            String jsonBody = new ObjectMapper().writeValueAsString(requestBody);

            // Crea la conexión HTTP y configura la solicitud
            URL url = new URL(signInUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("apikey", properties.getProperty("supabase_key"));
            con.setDoOutput(true);
            con.getOutputStream().write(jsonBody.getBytes("utf-8"));

            // Lee la respuesta
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Verifica el código de estado de la respuesta
            if (responseCode >= 200 && responseCode < 300) {
                // La solicitud fue exitosa
                System.out.println("Inicio de sesión exitoso. Response Code: " + responseCode);
                System.out.println("Response: " + response.toString());
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("access_token")) {
                    accessToken = jsonResponse.getString("access_token");
                    System.out.println("Inicio de sesión exitoso. Token de acceso: " + accessToken);
                } else {
                    System.out.println("No se encontró el token de acceso en la respuesta.");
                }
                return true;
            } else {
                // La solicitud falló
                System.out.println("Error al iniciar sesión. Response Code: " + responseCode);
                System.out.println("Response: " + response.toString());
                return false;
            }
        } catch (Exception e) {
            // Manejar cualquier excepción que pueda ocurrir
            System.err.println("Error al iniciar sesión: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean enviarCorreoRestablecerPw(String correoUsuario) {
        try {
            // URL del endpoint para enviar el correo de restablecimiento de contraseña
            String url = properties.getProperty("supabase_url") + "/auth/v1/recover";

            // API Key
            String apiKey = properties.getProperty("supabase_key");

            // Construir la solicitud HTTP POST
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);

            // Agregar la API Key a la cabecera de la solicitud
            httpPost.setHeader("apikey", apiKey);

            // Agregar el correo del usuario al cuerpo de la solicitud
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", correoUsuario);
            StringEntity entity = new StringEntity(requestBody.toString());
            entity.setContentType("application/json");
            httpPost.setEntity(entity);

            // Ejecutar la solicitud HTTP POST
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            // Verificar el código de estado de la respuesta
            if (statusCode == 200) {
                System.out.println("Solicitud de restablecimiento enviada");
                // Correo electrónico de restablecimiento de contraseña enviado exitosamente
                return true;
            } else {
                // Error al enviar el correo electrónico
                System.out.println("Error al enviar el correo electrónico de restablecimiento de contraseña. " +
                        "Código de estado: " + statusCode);
                return false;
            }
        } catch (Exception e) {
            // Manejar cualquier excepción que pueda ocurrir
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerIdMenuPorIdEmpresa(String nombreMenu, int idEmpresa) {
        try {
            // URL del endpoint para obtener datos en Supabase
            String url = properties.getProperty("supabase_url_menus") + "?id_empresa=eq." + idEmpresa +
                    "&Nombre=eq." + nombreMenu;
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
                // No se encontró ninguna empresa con el correo electrónico especificado
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void cambiarPw(String email, String newPassword, String API_URL, String apikey, String bearertoken) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(API_URL + "/auth/v1/user");

            // Configuración de la cabecera
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + bearertoken);
            httpPost.setHeader("apikey", apikey);

            // Creación del cuerpo de la solicitud
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", newPassword);

            // Establecer el cuerpo de la solicitud
            StringEntity requestEntity = new StringEntity(requestBody.toString());
            httpPost.setEntity(requestEntity);

            // Envío de la solicitud y obtención de la respuesta
            HttpResponse response = httpClient.execute(httpPost);

            // Obtener el cuerpo de la respuesta
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            // Procesamiento de la respuesta
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("Contraseña cambiada exitosamente para el usuario con email: " + email);
            } else {
                System.out.println("Hubo un error al cambiar la contraseña. Código de estado: " + statusCode);
                System.out.println("Mensaje del servidor: " + responseBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
