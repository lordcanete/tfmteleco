package us.tfg.p2pmessenger.controller;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * Interfaz del objeto que maneja las claves de un usuario
 */
public interface Llavero
{
    String EXTENSION_LLAVERO_CONSOLA = ".jceks";
    String EXTENSION_LLAVERO_ANDROID = "."+KeyStore.getDefaultType();
    String EXTENSION_CLAVE_SALIENTE="saliente";
    String EXTENSION_CLAVE_ENTRANTE = "entrante";


    Key getClaveSimetrica(String alias);

    Key getClavePrivada(String alias);

    X509Certificate getCertificado(String alias);

    Llavero setClaveSimetrica(String alias,Key clave) throws KeyStoreException;

    Llavero setEntradaPrivada(String alias, KeyStore.PrivateKeyEntry entradaPrivada) throws KeyStoreException;

    void cerrarLlavero(String usuario) throws Exception;

    void guardarLlavero(String usuario);

    Exception getError();

    ArrayList<String> obtenerClavesGuardadas() throws Exception;

    void eliminarLlavero(String usuario);

    void guardarCertificado(String alias, X509Certificate certificate) throws KeyStoreException;

}
