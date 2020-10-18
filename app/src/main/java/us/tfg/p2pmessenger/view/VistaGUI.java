package us.tfg.p2pmessenger.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import javax.json.JsonArray;

import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.controller.ControladorApp;
import us.tfg.p2pmessenger.controller.ControladorGUIImpl;
import us.tfg.p2pmessenger.controller.GestorFicherosConsola;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.Usuario;
import us.tfg.p2pmessenger.view.VistaConsolaPublic;
import us.tfg.p2pmessenger.controller.GoogleDriveController;


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
import com.google.api.services.drive.model.File;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.lang.Object;


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
    public static final String ERROR_USUARIONOEXISTENTE = "El usuario introducido no existe";
    public static final String ERROR_CREARCONTACTO = "Error al crear el contacto";
    public static final String ERROR_ELIMINARCONTACTO = "Error al eliminar el contacto";
    public static final String ERROR_OBTENERLISTACONVERSACIONESABIERTAS = "Error al obtener las conversaciones abiertas";
    public static final String ERROR_NOHAYCONVERSACIONESABIERTAS = "No hay conversaciones abiertas";
    public static final String ERROR_OBTENERMENSAJESCONVERSACION = "Error al obtener los mensajes de la conversación";
    public static final String ERROR_ABRIRNUEVACONVERSACION = "Error al iniciar una nueva conversacion";
    public static final String ERROR_UNIRSEAGRUPO = "Error al encontrar contacto para unirse a grupo";
    public static final String ERROR_ELIMINARCONVERSACION = "Error al eliminar la conversación";
    public static final String ERROR_ENVIOARCHIVODRIVE = "Error al compartir el archivo";

    private final static Logger LOGGER = Logger.getLogger("us.tfg.p2pmessenger.view.VistaGUI");
    
    
    
    /** for communication to the Javascript engine. */
    private GoogleDriveController gDriveController;
    private JSObject javascriptConnector;
    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector = new JavaConnector();
    private WebView webView;
    private WebEngine webEngine;
    private Scene scene;
    private VistaConsolaPublic servicio;
    private int puerto;
    private String ip;
    private java.io.File keystoreFile;
    

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
    public GoogleDriveController getGDriveController(){
        return this.gDriveController;
    }
    public void setGDriveController(GoogleDriveController gdc){
        this.gDriveController = gdc;
    }

    public java.io.File getKeystoreFile(){
        return this.keystoreFile;
    }
     
    public void setKeystoreFile(java.io.File ksfile){
        this.keystoreFile = ksfile;
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {       
        
            
        Parameters params = getParameters();
        List<String> listParams = params.getRaw();
        puerto = Integer.parseInt(listParams.get(0));
        keystoreFile = null;
        if (listParams.size()>2){
            // get the keystore file
            String keystoreFileName = listParams.get(2);
            keystoreFile = new java.io.File(keystoreFileName);
            if (!keystoreFile.exists()) throw new IllegalArgumentException("No se encontró el fichero de almacenamiento de claves/certificados: "+keystoreFileName);
        }  
        
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
                window.setMember("servicioConnector", servicio);
                // get the Javascript connector object. 
                javascriptConnector = (JSObject) webEngine.executeScript("getJsConnector()");
                //webEngine.executeScript("onPageReady()");
            }
        });

        scene = new Scene(webView, 800, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        URL url = new java.io.File(RUTA_HTML + "index.html").toURI().toURL();       
        // now load the page
        webEngine.load(url.toString());

    }

    /*Clase para conectar el FrontEnd Web con el BackEnd Java*/
    public class JavaConnector {
        
        private String rutaArchivoEncontrado;
        private boolean archivoEncontrado;
        private final String nombreDirectorioDriveAplicacion = "P2PMessenger";
        private final String formatoDirectorioDrive = "application/vnd.google-apps.folder";

        


        public void RecursiveSearch(java.io.File[] arr,int index, String nombreArchivo, long lastModifiedArchivo)  
        { 
            String lastModifiedFound;
            String lastModifiedToSearch = Long.toString(lastModifiedArchivo).substring(0,Long.toString(lastModifiedArchivo).length()-3);
            // Llega al ultimo fichero del directorio actual
            if(index == arr.length){
                return; 
            } 
                
            // Comprueba los archivos 
            if(arr[index].isFile()){
                lastModifiedFound = Long.toString(arr[index].lastModified()).substring(0,Long.toString(arr[index].lastModified()).length()-3);
                if(arr[index].getName().equals(nombreArchivo) && lastModifiedFound.equals(lastModifiedToSearch)){
                    rutaArchivoEncontrado = arr[index].getAbsolutePath();
                    archivoEncontrado = true;
                    return;
                }
            }                 
            // Indagando sobre directorios
            else if(arr[index].isDirectory()) 
            {  
                // Llamada recursiva 
                RecursiveSearch(arr[index].listFiles(), 0, nombreArchivo, lastModifiedArchivo); 
                if(archivoEncontrado)
                    return;
            }                 
            // recursion for main directory 
            RecursiveSearch(arr,++index, nombreArchivo, lastModifiedArchivo); 
        } 

        public String buscarArchivo(String nombreArchivo, long lastModified){
            //Directorio sobre el que se realizara la busqueda recursiva
            java.io.File root = new java.io.File("/home/");
            rutaArchivoEncontrado = null;       
            archivoEncontrado = false;                          
            LOGGER.log(Level.INFO, "Buscando archivo: " + nombreArchivo);
            if(root.exists() && root.isDirectory()) 
            { 
                java.io.File arr[] = root.listFiles();  
                RecursiveSearch(arr,0, nombreArchivo, lastModified);  
            }    
            LOGGER.log(Level.INFO, "Encontrado archivo: " + rutaArchivoEncontrado);
            return rutaArchivoEncontrado;
        }

        public String buscarDirectorioDrive(String nombreDirectorio, String formatoDirectorio) throws Exception{
            String idDir = null;
            
            String filtro = "name='"+nombreDirectorio+"' and mimeType = '"+formatoDirectorio+"'";
            try {
                List<com.google.api.services.drive.model.File> files = GoogleDriveController.buscarArchivosDirectorios(filtro);    
                if(!files.isEmpty()){
                    idDir = files.get(0).getId();
                }
                
            } catch (IOException e) {                
                idDir = null;
            }
            return idDir;
            
        }
        public void enviarArchivoDrive(String nombre, String tipo, long lastModified){      
            LOGGER.log(Level.INFO, "Compartiendo archivo: " + nombre);
            String ruta = buscarArchivo(nombre, lastModified);             
            if(ruta == null){
                //TODO      
                LOGGER.log(Level.INFO, "Imposible encontrar el fichero en el sistema");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_ENVIOARCHIVODRIVE);
            }else{   
                try {
                    LOGGER.log(Level.INFO, "Buscando directorio de aplicación en Google Drive");
                    String idDirectorioDrive = buscarDirectorioDrive(this.nombreDirectorioDriveAplicacion, this.formatoDirectorioDrive);                
                    if(idDirectorioDrive == null){
                        LOGGER.log(Level.INFO, "No existe el directorio en Google Drive. Creando directorio para la aplicación.");
                        com.google.api.services.drive.model.File nuevoDirectorio = GoogleDriveController.crearDirectorio(this.nombreDirectorioDriveAplicacion, this.formatoDirectorioDrive);
                        idDirectorioDrive = nuevoDirectorio.getId();
                    }
                    LOGGER.log(Level.INFO, "Subiendo archivo a Google Drive");
                    com.google.api.services.drive.model.File archivoCreado = GoogleDriveController.crearArchivoDesdeRuta(idDirectorioDrive, nombre, ruta, Files.probeContentType(Paths.get(ruta)));
                    String urlArchivo = GoogleDriveController.obtenerEnlaceCompartirArchivo(archivoCreado.getId());
                    LOGGER.log(Level.INFO, "Obtenida URL para compartición de archivo a través de Google Drive: " + urlArchivo);                
                    String mensajeComparticionFicheroDrive = "Archivo compartido: " + nombre + " | URL: "+urlArchivo;
                    enviarMensajeAConversacionSeleccionada(mensajeComparticionFicheroDrive);  
                } catch (Exception e) {
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_ENVIOARCHIVODRIVE);
                }                                  
            }        
        }        

        public void cargarPagina(String pagina){         
            try {
                URL url = new java.io.File(RUTA_HTML + pagina).toURI().toURL();
                webEngine.load(url.toString());
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void iniciarServicio() throws Exception{        
            servicio = new VistaConsolaPublic(ip,puerto,keystoreFile);
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
                    LOGGER.log(Level.INFO, "MODO_APAGADO");
                    //System.out.println("MODO_APAGADO");
                    break;
                case ControladorApp.MODO_SESION_INICIADA:
                    LOGGER.log(Level.INFO, "MODO_SESION_INICIADA");
                    //System.out.println("MODO_SESION_INICIADA");   
                    cargarPagina("appWindow.html");
                    break;
                case ControladorApp.MODO_NECESARIA_DIRECION:  
                    LOGGER.log(Level.INFO, "MODO_NECESARIA_DIRECCION");
                    //System.out.println("MODO NECESARIA DIRECCION");
                    cargarPagina("nuevaDireccion.html");
                    break;
                case ControladorApp.MODO_INICIO_SESION:
                    LOGGER.log(Level.INFO, "MODO_INICIO_SESION");
                    //System.out.println("MODO_INICIO_SESION");
                    cargarPagina("inicioSesion.html");
                    break;
                case ControladorApp.MODO_REGISTRO:
                    LOGGER.log(Level.INFO, "MODO_REGISTRO");
                    //System.out.println("MODO_REGISTRO");
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
                    LOGGER.log(Level.INFO, "Nombre de usuario {0} en uso", inputUsuario);
                    //System.out.println("Nombre de usuario " + inputUsuario + " en uso");
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_USUARIONODISPONIBLE);
                }
            } catch (Exception e)
            {
                LOGGER.log(Level.INFO, "Error al crear un usuario nuevo");
                //System.out.println("Error al crear un usuario nuevo");
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
                gfich.escribirAFichero(ControladorGUIImpl.NOMBRE_FICHERO_USUARIO,
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
                LOGGER.log(Level.INFO, "Error al iniciar sesion");
                //System.out.println("Error al iniciar sesion");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_INICIOSESION);            
                e.printStackTrace();
            }
        } 
        
        public void cerrarSesion(){
            servicio.appGuardarLlavero();
            servicio.appCerrarSesion();
            javascriptConnector.call("comprobarEstadoCallback");
        }

        public void cerrarAgenda(){
            refrescarConversaciones();          
        }

        public void refrescarConversaciones(){
            System.out.println("Entra refrescarConversaciones");
            if(servicio.getConversacionSeleccionada() != null){
                System.out.println("Entra refrescarConversaciones no null");
                obtenerListaConversacionesAbiertas(servicio.getConversacionSeleccionada().getAlias());
            }else{
                System.out.println("Entra refrescarConversaciones null");
                obtenerListaConversacionesAbiertas(null);
            }
        }

        public void obtenerListaConversacionesAbiertas(String aliasConversacionSeleccionada){
            ArrayList<Conversacion> conversaciones = servicio.appObtenerConversacionesAbiertas();
            JsonArrayBuilder listaConversacionesJsonBuilder = Json.createArrayBuilder();
            JsonObjectBuilder conversacionJsonBuilder = Json.createObjectBuilder();
            JsonObject conversacionJson = null;
            JsonArray listaConversacionesJson = null;
            boolean conversacionSeleccionadaAbierta = false;
            boolean errorAbrirNuevaConversacion = false;
            Conversacion nuevaConversacion = null;

            if(conversaciones!=null)
            {
                for (Conversacion conversacion : conversaciones)
                {
                    conversacionJsonBuilder = Json.createObjectBuilder()
                                                .add("idConversacion", conversacion.getId().toStringFull())
                                                .add("aliasRemitente", conversacion.getAlias())                                          
                                                .add("ultimoMensaje", conversacion.getMensaje() == null ? ": -" : conversacion.getMensaje())
                                                .add("fechaUltimoMensaje", conversacion.getFecha().getTime())
                                                .add("tipo", conversacion.getTipo())
                                                .add("pendiente", conversacion.isPendiente());   
                    if(aliasConversacionSeleccionada != null){
                        if(conversacion.getAlias().compareTo(aliasConversacionSeleccionada) == 0){
                            conversacionJsonBuilder.add("seleccionada", true);
                            conversacionSeleccionadaAbierta = true;                            
                            servicio.setConversacionSeleccionada(conversacion);
                        }                        
                    }else{
                        conversacionJsonBuilder.add("seleccionada", false);
                    }
                    conversacionJson = conversacionJsonBuilder.build();
                    listaConversacionesJsonBuilder.add(conversacionJson);
                }   
                if(aliasConversacionSeleccionada != null && !conversacionSeleccionadaAbierta) {
                    LOGGER.log(Level.INFO, "Creando nueva conversacion");
                    //System.out.println("Creando nueva conversacion");
                    ArrayList<Contacto> contactos = servicio.appObtenerContactos();
                    Contacto contactoNuevaConversacion = null;
                    for(Contacto contacto : contactos){
                        if(contacto.getAlias().compareTo(aliasConversacionSeleccionada) == 0){
                            contactoNuevaConversacion = contacto;
                        }
                    }
                    if(contactoNuevaConversacion != null){
                        nuevaConversacion = new Conversacion(contactoNuevaConversacion.getId(), new Date(),
                                                                      contactoNuevaConversacion.getAlias(), Conversacion.TIPO_INDIVIDUAL);
                        if (!servicio.appIniciarConversacion(contactoNuevaConversacion.getId().toStringFull()))
                        {                        
                            errorAbrirNuevaConversacion = true;
                        } else{
                            conversacionJsonBuilder = Json.createObjectBuilder()
                                                    .add("idConversacion", nuevaConversacion.getId().toStringFull())
                                                    .add("aliasRemitente", nuevaConversacion.getAlias())                                          
                                                    .add("ultimoMensaje", ":  -")
                                                    .add("fechaUltimoMensaje", nuevaConversacion.getFecha().getTime())
                                                    .add("tipo", nuevaConversacion.getTipo())
                                                    .add("pendiente", nuevaConversacion.isPendiente())
                                                    .add("seleccionada", true);   
                            conversacionSeleccionadaAbierta = true;                                                                 
                            servicio.setConversacionSeleccionada(nuevaConversacion);                                                
                            conversacionJson = conversacionJsonBuilder.build();
                            listaConversacionesJsonBuilder.add(conversacionJson);   
                        }
                    }
                    else{
                        LOGGER.log(Level.INFO, "Error al obtener el contacto remitente");
                        //System.out.println("Error al obtener el contacto remitente");
                    }
                    
                }        
                listaConversacionesJson = listaConversacionesJsonBuilder.build();
                if(nuevaConversacion != null && errorAbrirNuevaConversacion){
                    LOGGER.log(Level.INFO, "Error al iniciar una nueva conversacion");
                    //System.out.println("Error al iniciar una nueva conversacion");  
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_ABRIRNUEVACONVERSACION);                    
                }else{                                    
                    LOGGER.log(Level.INFO, "Conversaciones a devolver en json: {0}", listaConversacionesJson.toString());
                    //System.out.println("Conversaciones a devolver en json: \n"+listaConversacionesJson.toString());
                    javascriptConnector.call("actualizarPanelConversaciones", listaConversacionesJson.toString());
                }                
            }
            else
            {
                LOGGER.log(Level.INFO, "Error al obtener las conversaciones abiertas");
                //System.out.println("Error al obtener las conversaciones abiertas");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_OBTENERLISTACONVERSACIONESABIERTAS); 
            }
        }

        public void obtenerMensajesConversacionSeleccionada(int primerMensaje, int ultimoMensaje){
            Conversacion conversacionSeleccionada = servicio.getConversacionSeleccionada();
            ArrayList<Contacto> contactos = servicio.appObtenerContactos();
            Usuario usuarioLogado = servicio.appGetUsuario();
            ArrayList<Mensaje> mensajes = obtenerMensajesConversacion(conversacionSeleccionada, primerMensaje, ultimoMensaje);
            JsonArrayBuilder listaMensajesJsonBuilder = Json.createArrayBuilder();
            JsonObjectBuilder mensajeJsonBuilder = Json.createObjectBuilder();
            JsonArray listaMensajesJson = null;
            JsonObject mensajeJson = null;
            boolean contactoGuardado = false;
            String aliasContactoGuardado = "";
            if(mensajes!=null)
            {
                for (Mensaje mensaje : mensajes)
                {
                    if(!comprobarSiEsMensajeDeControl(mensaje)){
                        mensajeJsonBuilder = Json.createObjectBuilder()
                                                .add("contenido", mensaje.getContenido()) 
                                                .add("fecha", mensaje.getFecha().getTime());                      
                        if(mensaje.getOrigen().equals(usuarioLogado.getId())){
                            mensajeJsonBuilder.add("sentidoRecepcion", false);
                        }else{
                            mensajeJsonBuilder.add("sentidoRecepcion", true);
                            if(conversacionSeleccionada.getTipo() == Conversacion.TIPO_GRUPO){
                                contactoGuardado = false;
                                for(Contacto contacto : contactos){
                                    if (mensaje.getOrigen().equals(contacto.getUsuario().getId())){
                                        contactoGuardado = true;
                                        aliasContactoGuardado = contacto.getAlias();
                                        break;
                                    }
                                }
                                if(contactoGuardado){
                                    mensajeJsonBuilder.add("remitente", aliasContactoGuardado);
                                }else{
                                    mensajeJsonBuilder.add("remitente", mensaje.getOrigen().toStringFull());
                                }

                            }
                        }
                        mensajeJson = mensajeJsonBuilder.build();
                        listaMensajesJsonBuilder.add(mensajeJson); 
                    }
                                      
                }   
                listaMensajesJson = listaMensajesJsonBuilder.build();   
                LOGGER.log(Level.INFO, "Mensajes a devolver en json: \n{0}", listaMensajesJson.toString());             
                //System.out.println("Mensajes a devolver en json: \n"+listaMensajesJson.toString());
                javascriptConnector.call("actualizarPanelConversacionSeleccionada", listaMensajesJson.toString(), conversacionSeleccionada.getAlias(), conversacionSeleccionada.getTipo() == Conversacion.TIPO_GRUPO);
            }else
            {
                LOGGER.log(Level.INFO, "Error al obtener los mensajes de la conversación");
                //System.out.println("Error al obtener los mensajes de la conversación");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_OBTENERMENSAJESCONVERSACION); 
            }

        }
        public ArrayList<Mensaje> obtenerMensajesConversacion(Conversacion conversacion, int primerMensaje, int ultimoMensaje){
            return servicio.appObtieneMensajes(conversacion.getId().toStringFull(), primerMensaje, ultimoMensaje, conversacion.getTipo());
        }

        public void obtenerListaContactos(){
            ArrayList<Contacto> contactos=servicio.appObtenerContactos();
            JsonArrayBuilder listaContactosJsonBuilder = Json.createArrayBuilder();
            JsonArray listaContactosJson = null;
            if(contactos!=null)
            {
                for (Contacto contacto : contactos)
                {
                    listaContactosJsonBuilder.add(Json.createObjectBuilder()
                                                .add("alias", contacto.getAlias())
                                                .add("usuario", contacto.getUsuario().getNombre()).build());
                }                
                listaContactosJson = listaContactosJsonBuilder.build();    
                LOGGER.log(Level.INFO, "Contactos a devolver en json: \n{0}",listaContactosJson.toString());            
                //System.out.println("Contactos a devolver en json: \n"+listaContactosJson.toString());
                javascriptConnector.call("abrirPanelAgenda", listaContactosJson.toString());
            }
            else
            {
                LOGGER.log(Level.INFO, "Error al obtener los contactos guardados");
                //System.out.println("Error al obtener los contactos guardados");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_OBTENERLISTACONTACTOS); 
            }
                
        }

        public void crearContacto(String inputUsuario, String inputAlias){
            try{
                boolean existeUsuario = !servicio.compruebaNombre(inputUsuario);
                if(existeUsuario){
                    LOGGER.log(Level.INFO, "El usuario existe");
                    //System.out.println("El usuario existe.");
                    servicio.appNuevoContacto(inputUsuario, inputAlias);
                    javascriptConnector.call("actualizarPanelAgenda");
                }else{
                    LOGGER.log(Level.INFO, "El usuario no existe");
                    //System.out.println("El usuario no existe.");
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_USUARIONOEXISTENTE); 
                }
                
            }catch(Exception e){
                javascriptConnector.call("notificarError", VistaGUI.ERROR_CREARCONTACTO); 
            }
            
        }       

        public void crearGrupo(String nombreGrupo){         
            servicio.setFinCrearGrupo(false);
            servicio.appCrearGrupo(nombreGrupo);
            while(!servicio.getFinCrearGrupo()){
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    e.printStackTrace();
                }
                
            }
            if(servicio.getConversacionSeleccionada() != null){
                javascriptConnector.call("actualizarPanelConversacionesTrasCrearGrupo", servicio.getConversacionSeleccionada().getAlias());
            }else{
                javascriptConnector.call("actualizarPanelConversacionesTrasCrearGrupo");
            }
            
        }

        public void eliminarConversacion(String idConversacion){
            ArrayList<Conversacion> conversaciones = servicio.appObtenerConversacionesAbiertas();
            Conversacion conversacionSeleccionada = servicio.getConversacionSeleccionada();
            boolean resultado = false;
            if(conversaciones!=null)
            {
                for (Conversacion conversacion : conversaciones)
                {
                    if(conversacion.getId().toStringFull().compareTo(idConversacion) == 0){
                        LOGGER.log(Level.INFO, "Eliminando conversacion: {0}", idConversacion);
                        //System.out.println("Eliminando conversacion: " + idConversacion);                             
                        resultado = servicio.appEliminarConversacion(idConversacion);             
                        if (resultado == true)     {
                            if(conversacionSeleccionada.getAlias().compareTo(conversacion.getAlias()) == 0){
                            servicio.setConversacionSeleccionada(null);
                            obtenerListaConversacionesAbiertas(null);   
                            }
                            else if(conversacionSeleccionada == null){                            
                                obtenerListaConversacionesAbiertas(null);   
                            }else{
                                obtenerListaConversacionesAbiertas(conversacionSeleccionada.getAlias());
                            }  
                        }else {
                            javascriptConnector.call("notificarError", VistaGUI.ERROR_ELIMINARCONVERSACION);
                        }
                                              
                    }
                }
            }
                   
        }

        public void eliminarContacto(String usuario){
            ArrayList<Contacto> contactos=servicio.appObtenerContactos();
            Id idUsuario  = null;
            if(contactos!=null)
            {
                for (Contacto contacto : contactos)
                {
                    if(usuario.compareTo(contacto.getUsuario().getNombre()) == 0){
                        idUsuario = contacto.getId();
                   }
                }
                if (idUsuario != null){
                    servicio.appEliminaContacto(idUsuario.toStringFull());
                    javascriptConnector.call("actualizarPanelAgenda");
                }
                else{
                    LOGGER.log(Level.INFO, "Error al eliminar contacto");
                    //System.out.println("Error al eliminar contacto");
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_ELIMINARCONTACTO); 
                }                               
                
            }
            else
            {
                LOGGER.log(Level.INFO, "Error al obtener los contactos guardados");
                //System.out.println("Error al obtener los contactos guardados");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_OBTENERLISTACONTACTOS); 
            }
        }

        public void enviarMensajeAConversacionSeleccionada(String mensaje){
            servicio.appEnviarMensaje(mensaje);
            javascriptConnector.call("actualizarPanelesAppWindowTrasEnvioMensaje", servicio.getConversacionSeleccionada().getAlias());
        }

        public void actualizarTrasNotificacion(){
            if(servicio.getConversacionSeleccionada() != null){
                javascriptConnector.call("actualizarPanelesAppWindowTrasNotificacion", servicio.getConversacionSeleccionada().getAlias());
            }else{
                javascriptConnector.call("actualizarPanelesAppWindowTrasNotificacion");
            }
            
            
        }

        public void generarCodigoInvitacionGrupo(){
            System.out.println("Llamado metodo de generarCodigoInvitacionGrupo");
            Conversacion conversacionSeleccionada = servicio.getConversacionSeleccionada();
            if(conversacionSeleccionada != null){
                String codigoInvitacion = servicio.appObtenerCodigoInvitacion(conversacionSeleccionada.getId().toStringFull());
                javascriptConnector.call("actualizarCodigoInvitacionGrupo", codigoInvitacion);
            }          

        }

        public void unirseAGrupo(String usuario, String codigoInvitacion){
            ArrayList<Contacto> contactos=servicio.appObtenerContactos();
            Id idUsuario  = null;
            if(contactos!=null)
            {
                for (Contacto contacto : contactos)
                {
                    if(usuario.compareTo(contacto.getUsuario().getNombre()) == 0){
                        idUsuario = contacto.getId();
                   }
                }
                if (idUsuario != null){
                    servicio.appEnviarPeticionUnirAGrupo(idUsuario.toStringFull(), codigoInvitacion);
                    if(servicio.getConversacionSeleccionada() != null){
                        javascriptConnector.call("actualizarPanelConversacionesTrasUnirseAGrupo", servicio.getConversacionSeleccionada().getAlias());
                    }else{
                        javascriptConnector.call("actualizarPanelConversacionesTrasUnirseAGrupo");
                    }
                    
                }
                else{
                    LOGGER.log(Level.INFO, "Error al encontrar contacto para unirse a grupo");
                    //System.out.println("Error al encontrar contacto para unirse a grupo");
                    javascriptConnector.call("notificarError", VistaGUI.ERROR_UNIRSEAGRUPO); 
                }                               
                
            }
            else
            {
                LOGGER.log(Level.INFO, "Error al obtener los contactos guardados");
                //System.out.println("Error al obtener los contactos guardados");
                javascriptConnector.call("notificarError", VistaGUI.ERROR_OBTENERLISTACONTACTOS); 
            }           
            
        }

        public boolean comprobarSiEsMensajeDeControl(Mensaje mensaje){
            boolean validacion = false;
            validacion = comprobarSiEsMensajeUnionGrupo(mensaje);
            return validacion;
        }
        public boolean comprobarSiEsMensajeUnionGrupo(Mensaje mensaje){
            boolean validacion = false;
            String contenido = mensaje.getContenido();
            int longContenido = contenido.length();
            boolean tieneFormatoJSON = (contenido.substring(0,1).compareTo("{") == 0) && (contenido.substring(longContenido-1).compareTo("}") == 0);
            boolean contieneClavesValoresUnionGrupo = contenido.contains("\"id_grupo\":") && contenido.contains("\"clave_privada\":");
            validacion = tieneFormatoJSON && contieneClavesValoresUnionGrupo;
            return validacion;
        }

        public void generaLog(String logMensaje){
            LOGGER.log(Level.INFO, logMensaje);    
        }

        public boolean breakpoint(){
            try{
                System.in.read();
            }catch(Exception e){
                LOGGER.log(Level.SEVERE, e.toString());
            }
            
            return true;
        }



    }

    public static void main(String args[])
    {
        GoogleDriveController gdcontroller;
        
        if (args.length < 1){
            System.out.println("Uso:\nLinux -> java -cp \"../../lib/*\" --module-path /directorioJavaFX/lib --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI puertoEscucha [ip] [keystore]");
            System.exit(0);
        } else
        {
            try {
                System.out.println("Creando GoogleDriveController");        
                gdcontroller = new GoogleDriveController();
                System.out.println("Creado GoogleDriveController");
            } catch (Exception e) {
                e.printStackTrace();
            }               
            Application.launch(VistaGUI.class, args);       
        }
       
    }
}
