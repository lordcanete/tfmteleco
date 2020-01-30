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

/**
 * Created by PCANO on 30/01/20.
 */
public class VistaGUI implements Vista
{
    public VistaGUI()
    {
    }

    public static void main(String args[])
    {
        System.out.println("Funcionando Vista WEB\n");
    }

    @Override
    public void muestraMensaje(Mensaje mensaje,String alias)
    {
        System.out.println(mensaje);
    }

    @Override
    public void notificacion(String origen)
    {
        System.out.println("Notificacion: Nuevo mensaje de "+origen);
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
        System.out.println(respuesta);
    }
}