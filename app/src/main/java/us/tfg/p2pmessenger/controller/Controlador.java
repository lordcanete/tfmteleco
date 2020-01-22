package us.tfg.p2pmessenger.controller;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import rice.p2p.commonapi.Id;
import rice.pastry.commonapi.PastryIdFactory;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.MensajeCifrado;
import us.tfg.p2pmessenger.model.Usuario;

/**
 * Interfaz utilizada por el modulo de comunicacion de pastry
 * para interactuar con el resto de la aplicacion.
 */
public interface Controlador
{
    int ID_NOTIFICACION = 9001;
    String FORMATO_FECHA = "dd/MM/yyyy HH:mm:ss.SS";
    String EXTENSION_CONVERSACION_PENDIENTE = "-pendiente";
    String EXTENSION_CODIGO = "-codigo-unir-grupo";
    String EXTENSION_CADUCIDAD_CODIGO = "-fecha-caducidad-codigo-grupo";
    int LONGITUD_CODIGO_UNIRSE_GRUPO = 50;
    int MODO_APAGADO = 0;
    int MODO_SESION_INICIADA = 1;
    int MODO_NECESARIA_DIRECION = 2;
    int MODO_INICIO_SESION = 3;
    int MODO_REGISTRO = 4;
    int TAM_PARTE_ALEATORIA_ID = 40;


    String ALGORITMO_CLAVE_SIMETRICA = "AES";
    String ALGORITMO_FIRMA = "SHA256withRSA";
    int TAM_CLAVE_ASIMETRICA = 2048;
    String ALGORITMO_CLAVE_ASIMETRICA = "RSA";

    String TIPO_KEYSTORE_ANDROID = KeyStore.getDefaultType();
    String TIPO_KEYSTORE_CONSOLA = "JCEKS";

    String NOMBRE_FICHERO_USUARIO = "usuario";
    String NOMBRE_OBJETOS_PASTRY = "objetosDePastry";

    String IDENTIFICADOR_USUARIO = "usr";
    String IDENTIFICADOR_SECRETO_DE_USUARIO = "secreto_usr";

    String EXTENSION_ULTIMO_BLOQUE = "-ultimo_bloque_conocido";


    Llavero getLlavero();

    PastryIdFactory getPastryIdFactory();

    InetSocketAddress getDireccion();

    Usuario getUsuario();

    void guardarDireccion(InetAddress ip,int puerto);

    void mensajeRecibido(Mensaje mensaje, Id idGrupo);

    void guardaMensaje(Mensaje mensaje,Id conversacion);

    void actualizaDireccionBloque(Id grupo, Mensaje mensaje);

    void errorEnviando(int error,String mensaje);

    boolean responderEcho();

    void inicioChatIndividual(MensajeCifrado mcifrado);

    void peticionUnirAGrupo(Mensaje mensaje);

    void respuestaUnirAGrupo(Mensaje mensaje);

}
