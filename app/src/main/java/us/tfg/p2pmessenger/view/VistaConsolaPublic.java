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

public class VistaConsolaPublic extends VistaConsola 
{
    public VistaConsolaPublic(String ip, int puerto){
        super(ip, puerto);
    }

    public void appOnCreateEntorno(){
        this.app.onCreateEntorno();
    }
    public void appOnStart(){
        this.app.onStart();
    }
    public int appGetError(){
        return this.app.getError();
    }
    public int appGetModo(){
        return this.app.getModo();
    }

    @Override
    public void modoSesionIniciada(){
        super.modoSesionIniciada();
    }
    @Override
    public void modoNuevaDireccion(){
        super.modoNuevaDireccion();
    }
    @Override
    public void modoInicioSesion(){
        super.modoInicioSesion();
    }
    @Override
    public void modoRegistro(){
        super.modoRegistro();
    }


    public void setApagar(boolean apagarValue){
        this.apagar = apagarValue;
    }
    public boolean getApagar(){
        return this.apagar;
    }

}