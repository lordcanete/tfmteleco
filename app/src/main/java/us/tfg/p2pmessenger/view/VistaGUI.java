package us.tfg.p2pmessenger.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonArray;

import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.controller.ControladorApp;
import us.tfg.p2pmessenger.controller.ControladorConsolaImpl;
import us.tfg.p2pmessenger.controller.GestorFicherosConsola;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.view.VistaConsolaPublic;


import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;
/**
 * Created by PCANO on 30/01/20.
 */
public class VistaGUI extends Application
{
    public static final String RUTA_HTML = "../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/";
    public static final String ERROR_DIRARRANQUE = "Error al tratar de conectar con la red Pastry";
    public static final String ERROR_CREACIONUSUARIO = "Error al crear el usuario";
    public static final String ERROR_USUARIONODISPONIBLE = "El usuario ya existe. Por favor, seleccione otro nombre de usuario";
    public static final String ERROR_INICIOSESION = "Error al iniciar sesión. Por favor, inténtelo de nuevo";
    public static final String ERROR_CREDENCIALESNOVALIDAS = "Usuario o contraseña no válidos. Por favor, inténtelo de nuevo.";
    public static final String ERROR_OBTENERLISTACONTACTOS = "Ocurrió un error al obtener los contactos guardados";
    
    /** for communication to the Javascript engine. */
    private JSObject javascriptConnector;
    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector = new JavaConnector();
    private WebView webView;
    private WebEngine webEngine;
    private Scene scene;
    private VistaConsolaPublic servicio;
    private int puerto;
    private String ip;


    //Setters & Getters
    public WebView getWebView(){
        return this.webView;
    }
    public void setWebView(WebView paramWebView){
        this.webView = paramWebView;
    }
    public WebEngine getWebEngine(){
        return this.webEngine;
    }
    public void setWebEngine(WebEngine paramWebEngine){
        this.webEngine = paramWebEngine;
    }
    public Scene getScene(){
        return this.scene;
    }
    public void setScene(Scene paramScene){
        this.scene = paramScene;
    }
    public JSObject getJavascriptConnector(){
        return this.javascriptConnector;
    }
    public void setJavascriptConnector(JSObject paramJavascriptConnector){
        this.javascriptConnector = paramJavascriptConnector;
    }
    public JavaConnector getJavatConnector(){
        return this.javaConnector;
    }
    public void setJavaConnector(JavaConnector paramJavaConnector){
        this.javaConnector = paramJavaConnector;
    }
    public String getIP(){
        return this.ip;
    }
    public void setIP(String paramIp){
        this.ip = paramIp;
    }
    public int getPuerto(){
        return this.puerto;
    }
    public void setPuerto(int paramPuerto){
        this.puerto = paramPuerto;
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {       
        Parameters params = getParameters();
        List<String> listParams = params.getRaw();
        puerto = Integer.parseInt(listParams.get(0));
        ip = null;    
        if (listParams.size()>1){
            ip = listParams.get(1);
        }    

        webView = new WebView();
        webEngine = webView.getEngine();

        // set up the listener
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // set an interface object named 'javaConnector' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
                // get the Javascript connector object. 
                javascriptConnector = (JSObject) webEngine.executeScript("getJsConnector()");
            }
        });

        scene = new Scene(webView, 800, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        URL url = new File(RUTA_HTML + "index.html").toURI().toURL();       
        // now load the page
        webEngine.load(url.toString());

    }

    /*Clase para conectar el FrontEnd Web con el BackEnd Java*/
    public class JavaConnector {

        public void cargarPagina(String pagina){         
            try {
                URL url = new File(RUTA_HTML + pagina).toURI().toURL();
                webEngine.load(url.toString());
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void iniciarServicio(){
            servicio = new VistaConsolaPublic(ip,puerto);
            servicio.appOnCreateEntorno();
            servicio.appOnStart();
            if(servicio.appGetError()!=0) {
                servicio.procesaError();
                servicio.appOnStop();
                servicio.appOnDestroy();
                System.exit(0);
            } else{
                javascriptConnector.call("comprobarEstadoCallback");
            }
        }

        public void comprobarEstado(){            
            switch (servicio.appGetModo())
            {
                case ControladorApp.MODO_APAGADO:
                    System.out.println("MODO_APAGADO");
                    break;
                case ControladorApp.MODO_SESION_INICIADA:
                    System.out.println("MODO_SESION_INICIADA");   
                    cargarPagina("appWindow.html");
                    break;
                case ControladorApp.MODO_NECESARIA_DIRECION:  
                    System.out.println("MODO NECESARIA DIRECCION");
                    cargarPagina("nuevaDireccion.html");
                    break;
                case ControladorApp.MODO_INICIO_SESION:
                    System.out.println("MODO_INICIO_SESION");
                    cargarPagina("inicioSesion.html");
                    break;
                case ControladorApp.MODO_REGISTRO:
                    System.out.println("MODO_REGISTRO");
                    cargarPagina("registro.html");
                    break;
            }  
        }

        public void accederRegistroUsuario(){
            servicio.appSetModo(ControladorApp.MODO_REGISTRO);            
            javascriptConnector.call("comprobarEstadoCallback");
        }

        public void accederInicioSesion(){
            servicio.appSetModo(ControladorApp.MODO_INICIO_SESION);            
            javascriptConnector.call("comprobarEstadoCallback");
        }

        public void conectarPastry(String inputIp, int inputPuerto){
            try
            {
                if(servicio.appNuevaDireccionArranque(inputIp, inputPuerto)){                    
                    servicio.appOnStart();
                    if (servicio.appGetError() != 0)
                    {
                        servicio.procesaError();
                    }else{
                        javascriptConnector.call("comprobarEstadoCallback");
                    }
                }else {
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_DIRARRANQUE);
                }                
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        public void registrarUsuario(String inputUsuario, String inputPasswd){
            boolean disponible = false;   
            boolean errorRegistro = false;         
            try
            {
                disponible = servicio.compruebaNombre(inputUsuario);
                if(disponible){
                    errorRegistro = servicio.appRegistrarUsuario(inputUsuario, inputPasswd);
                    if (!errorRegistro){
                        servicio.appOnStart();
                        if(servicio.appGetError()!=0) {
                            servicio.procesaError();
                            javascriptConnector.call("notificarError", VistaGUI.ERROR_CREACIONUSUARIO);
                        } else{
                            javascriptConnector.call("comprobarEstadoCallback");
                        }
                    }                    
                    
                }else{
                    System.out.println("Nombre de usuario " + inputUsuario + " en uso");
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_USUARIONODISPONIBLE);
                }
            } catch (Exception e)
            {
                System.out.println("Error al crear un usuario nuevo");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_CREACIONUSUARIO);
                e.printStackTrace();
            }
        }

        public void iniciarSesion(String inputUsuario, String inputPasswd){
            try
            {
                JsonObject contenidoJson = Json.createObjectBuilder()
                                                .add("usuario", inputUsuario)
                                                .add("contrasena", inputPasswd)
                                                .build();

                GestorFicherosConsola gfich = new GestorFicherosConsola();
                gfich.escribirAFichero(ControladorConsolaImpl.NOMBRE_FICHERO_USUARIO,
                        contenidoJson.toString().getBytes(), false);

                servicio.appOnStart();
                if(servicio.appGetError()!=0) {
                    servicio.procesaError();
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_CREDENCIALESNOVALIDAS);
                } else{
                    javascriptConnector.call("comprobarEstadoCallback");
                }
            } catch (Exception e)
            {
                System.out.println("Error al iniciar sesion");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_INICIOSESION);            
                e.printStackTrace();
            }
        } 
        
        public void cerrarSesion(){
            servicio.appGuardarLlavero();
            servicio.appCerrarSesion();
            javascriptConnector.call("comprobarEstadoCallback");
        }

        public void obtenerListaContactos(){
            ArrayList<Contacto> contactos=servicio.appObtenerContactos();
            JsonArrayBuilder listaContactosJsonBuilder = Json.createArrayBuilder();
            JsonArray listaContactosJson = null;
            if(contactos!=null)
            {
                for (Contacto contacto : contactos)
                {
                    System.out.println("alias: "+contacto.getAlias()+" usuario: "+contacto.getUsuario().getNombre());                    
                    listaContactosJsonBuilder.add(Json.createObjectBuilder()
                                                .add("alias", contacto.getAlias())
                                                .add("usuario", contacto.getUsuario().getNombre()).build());
                }                
                listaContactosJson = listaContactosJsonBuilder.build();                
                System.out.println("Contactos a devolver en json: \n"+listaContactosJson.toString());
                javascriptConnector.call("abrirPanelAgenda", listaContactosJson.toString());
            }
            else
            {
                System.out.println("Error al obtener los contactos guardados");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_OBTENERLISTACONTACTOS); 
            }
                
        }

        public void crearContacto(String inputUsuario, String inputAlias){
            /*try{
                existeUsuario = !servicio.compruebaNombre(inputUsuario);
                if(existeUsuario){
                    servicio.appNuevoContacto(inputUsuario, inputAlias);
                    javascriptConnector.call("contactoCreadoOK");
                }else{
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_USUARIONOEXISTENTE); 
                }
                
            }catch(Exception e){
                javascriptConnector.call("notificarError", VistaGUI.ERROR_CREARCONTACTO); 
            }*/
            
        }
        



    }

    public static void main(String args[])
    {
        if (args.length < 1){
            System.out.println("Uso:\nLinux -> java -cp \"../../lib/*\" --module-path /directorioJavaFX/lib --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI puertoEscucha [ip]"+
            "\nWindows -> java -cp \"../../lib/*\" --module-path \"C:\\directorioJavaFX\\lib\" --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI puertoEscucha [ip]\n");
            System.exit(0);
        } else
        {
            Application.launch(VistaGUI.class, args);       
        }
       
    }
}
