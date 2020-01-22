package us.tfg.p2pmessenger.controller;

import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.model.MensajeCifrado;

/**
 * Interfaz mediadora entre el controlador globar de
 * la aplicacion y el modulo de comunicacion de pastry.
 * Permite mostrar una interfaz de mensajeria
 * transparente de implementacion. Facilita el migrado
 * a otra biblioteca si asi se deseara.
 */
public interface Mensajero
{
    void abandonaGrupo(Id idGrupo);

    void subscribe(Id grupo);

    void procesa(Id fuente, MensajeCifrado mensaje, boolean entrante);

    Llavero getLlavero();

    void enviaMensaje(us.tfg.p2pmessenger.model.Mensaje mensaje, boolean individual);

    void errorEnviando(int error,String mensaje);

    boolean responderEcho();

    void ping(Id objetivo, ObservadorPing observador,String carga);

    void cancelarPing(Id objetivo);

    void responderSolicitudClave(Id interlocutor,String claveCifrada);

    void cancelarClave(Id interlocutor);

}
