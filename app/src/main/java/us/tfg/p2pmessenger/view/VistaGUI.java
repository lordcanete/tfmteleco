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
    
    @Override
    public void start(Stage primaryStage) throws Exception {

        

        URL url = new File("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/index.html").toURI().toURL();

        WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();

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

        Scene scene = new Scene(webView, 450, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // now load the page
        webEngine.load(url.toString());
        //cargarPagina("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/helloagain.html", webEngine);
    }
    /*
    public void cargarPagina(String rutaPagina, WebEngine we){
         
        try {
            url = new File(rutaPagina).toURI().toURL();
            we.load(url.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
         
 
    }*/

    public class JavaConnector {
        /**
         * called when the JS side wants a String to be converted.
         *
         * @param value
         *         the String to convert
         */
        public void toLowerCase(String value) {
            if (null != value) {
                javascriptConnector.call("showResult", value.toLowerCase());
            }
        }
    }

    public static void main(String args[])
    {
        Application.launch(VistaGUI.class, args);       
       
    }
}
