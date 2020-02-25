package us.tfg.p2pmessenger.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;

import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.controller.ControladorApp;
import us.tfg.p2pmessenger.controller.ControladorConsolaImpl;
import us.tfg.p2pmessenger.controller.GestorFicherosConsola;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.view.VistaConsola;


import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.File;
import java.net.URL;
/**
 * Created by PCANO on 30/01/20.
 */
public class VistaGUI extends Application
{
    
    /** for communication to the Javascript engine. */
    private JSObject javascriptConnector;
    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector = new JavaConnector();
    private WebView webView;
    private WebEngine webEngine;
    private Scene scene;


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
    
    @Override
    public void start(Stage primaryStage) throws Exception {       


        URL url = new File("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/appWindow.html").toURI().toURL();

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

                /*Aqui hay que iniciar el servicio y generar los listener para interactuar con el frontend*/
                
            }
        });

        scene = new Scene(webView, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // now load the page
        webEngine.load(url.toString());
        /*
        VistaConsolaPublic servicio = new VistaConsolaPublic("10.0.2.4", 9001);
        servicio.getApp().onCreateEntorno();
        servicio.getApp().onStart();

        if(servicio.getApp().getError()!=0)
        {
            servicio.procesaError();
        }
        while (!servicio.getApagar())
        {

            switch (servicio.getApp().getModo())
            {
                case ControladorApp.MODO_APAGADO:
                    System.out.println("El nodo no se ha encendido");
                    servicio.setApagar(true);
                    break;
                case ControladorApp.MODO_SESION_INICIADA:
                    servicio.modoSesionIniciada();
                    break;
                case ControladorApp.MODO_NECESARIA_DIRECION:
                    servicio.modoNuevaDireccion();
                    break;
                case ControladorApp.MODO_INICIO_SESION:
                    servicio.modoInicioSesion();
                    break;
                case ControladorApp.MODO_REGISTRO:
                    servicio.modoRegistro();
                    break;
            }

        }
        System.out.println("Cerrando scanner");
        servicio.scanner.close();
        System.out.println("onStop");
        servicio.getApp().onStop();
        System.out.println("onDestroy");
        servicio.getApp().onDestroy();*/
        
    }

    /*Clase para conectar el FrontEnd Web con el BackEnd Java*/
    public class JavaConnector {

        public void cargarPagina(String rutaPagina){         
            try {
                URL url = new File(rutaPagina).toURI().toURL();
                webEngine.load(url.toString());
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[])
    {
        Application.launch(VistaGUI.class, args);       
       
    }
}
