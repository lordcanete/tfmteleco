package us.tfg.p2pmessenger.controller;

import rice.p2p.commonapi.Id;

/**
 * Interfaz del observador que recibira la notificacion
 * de que se ha obtenido la respuesta de eco o se ha agotado el
 * temporizador
 */
public interface ObservadorPing
{
    void notificarPing(int evento, Id objetivo, String carga);
}
