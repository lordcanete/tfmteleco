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
    public VistaGUI()
    {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL url = new File("./web/html/index.html").toURI().toURL();

        WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();

        Scene scene = new Scene(webView, 300, 150);
        primaryStage.setScene(scene);
        primaryStage.show();

        // now load the page
        webEngine.load(url.toString());
    }

    public static void main(String args[])
    {
        Application.launch(VistaGUI.class, args);
    }
    /*
    @Override
    public void muestraMensaje(Mensaje mensaje,String alias)
    {
     
    }

    @Override
    public void notificacion(String origen)
    {
     
    }

    @Override
    public void excepcion(Exception e)
    {
        e.printStackTrace();
    }

    @Override
    public void setSeguir(boolean seguir)
    {

    }

    @Override
    public void notificarPing(String respuesta)
    {
     
    }
    
    @Override
    public void errorEnviando(int error,String mensaje)
    {
    }*/
}