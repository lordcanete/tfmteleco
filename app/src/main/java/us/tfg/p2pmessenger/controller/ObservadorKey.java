package us.tfg.p2pmessenger.controller;

import rice.p2p.commonapi.Id;

/**
 * Interfaz del observador que recibira la notificacion
 * de que se ha obtenido la clave o se ha agotado el
 * temporizador
 */
public interface ObservadorKey
{
    void notificarClave(int evento,Id objetivo,String claveCodificada);

    void cancelarClave(Id interlocutor);
}
