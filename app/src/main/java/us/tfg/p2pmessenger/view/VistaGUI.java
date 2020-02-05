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
public class VistaGUI implements Vista
{
    private ControladorApp app;

    public static final Scanner scanner = new Scanner(System.in);

    private boolean esperar;

    private boolean apagar;

    private ArrayList<Conversacion> conversaciones;
    
    private int puerto;
    private String ip;

    public VistaGUI(String ip, int puerto)
    {
        this.puerto=puerto;
        this.ip=ip;
        app = new ControladorConsolaImpl(ip, puerto,this);
    }
/*
    @Override
    public void start(Stage primaryStage) throws Exception {
        URL url = new File("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/index.html").toURI().toURL();

        WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();

        Scene scene = new Scene(webView, 450, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // now load the page
        webEngine.load(url.toString());
    }
*/
    public static void main(String args[])
    {
        //Application.launch(VistaGUI.class, args);
        try {
            URL url = new File("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/index.html").toURI().toURL();

            WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        /*Scene scene = new Scene(webView, 450, 800);
        primaryStage.setScene(scene);
        primaryStage.show();*/

        // now load the page
        webEngine.load(url.toString());
        
        String name = "";
        String hint = null;
        String usuario = null;
        
        if (args.length < 1)
            System.out.println("usage -> java -cp \"../../lib/*\" --module-path /usr/share/openjfx/lib --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI puertoEscucha [ip]");
        else
        {/*
            int puerto = Integer.parseInt(args[0]);
            String ip = null;
            if (args.length > 1) {
                ip = args[1];
            }
            VistaGUI servicio = new VistaGUI(ip, puerto);

            servicio.app.onCreateEntorno();
            servicio.app.onStart();

            if(servicio.app.getError()!=0)
            {
                servicio.procesaError();
            }

            while (!servicio.apagar)
            {

                switch (servicio.app.getModo())
                {
                    case ControladorApp.MODO_APAGADO:
                        System.out.println("El nodo no se ha encendido");
                        servicio.apagar=true;
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
            scanner.close();
            System.out.println("onStop");
            servicio.app.onStop();
            System.out.println("onDestroy");
            servicio.app.onDestroy();*/
        }

        System.out.println("Fin de la aplicacion");
        } catch (Exception e){
            e.printStackTrace();
        }
        

        
    }
    
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
    }
}
