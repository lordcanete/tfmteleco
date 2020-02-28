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


        URL url = new File("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/index.html").toURI().toURL();

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

        scene = new Scene(webView, 800, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // now load the page
        webEngine.load(url.toString());
        
        
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
        if (args.length < 1){
            System.out.println("Uso:\nLinux -> java -cp \"../../lib/*\" --module-path /directorioJavaFX/lib --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI puertoEscucha"+
            "\nWindows -> java -cp \"../../lib/*\" --module-path \"C:\\directorioJavaFX\\lib\" --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI puertoEscucha\n");
        } else
        {
            Application.launch(VistaGUI.class, args);       
        }
       
    }
}
