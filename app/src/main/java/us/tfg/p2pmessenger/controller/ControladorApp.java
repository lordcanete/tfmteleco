package us.tfg.p2pmessenger.controller;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Key;
import java.util.ArrayList;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Id;
import rice.pastry.commonapi.PastryIdFactory;
import us.tfg.p2pmessenger.model.BloqueMensajes;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.MensajeCifrado;
import us.tfg.p2pmessenger.model.Usuario;

/**
 * Interfaz del controlador de la aplicacion completa. Es utilizado
 * por la vista. De esta interfaz se crea {@link ControladorConsolaImpl} para
 * ser utilizada en android
 */

public interface ControladorApp extends Controlador, ObservadorPing
{
    String ALGORITMO_CLAVE_ASIMETRICA = "RSA";
    String FORMATO_FECHA = "dd/MM/yyyy HH:mm:ss.SS";
    String ALGORITMO_CLAVE_SIMETRICA = "AES";
    String ALGORITMO_FIRMA = "SHA256withRSA";
    int TAM_CLAVE_ASIMETRICA = 2048;
    String NOMBRE_FICHERO_USUARIO = "usuario";

    String EXTENSION_CONVERSACION_PENDIENTE = "-pendiente";
    String EXTENSION_CODIGO = "-codigo-unir-grupo";
    String EXTENSION_CADUCIDAD_CODIGO = "-fecha-caducidad-codigo-grupo";
    int LONGITUD_CODIGO_UNIRSE_GRUPO = 50;

    String EXTENSION_ULTIMO_BLOQUE = "-ultimo_bloque_conocido";
    int MODO_APAGADO = 0;
    int MODO_SESION_INICIADA = 1;
    int MODO_NECESARIA_DIRECION = 2;
    int MODO_INICIO_SESION = 3;
    int MODO_REGISTRO = 4;
    int TAM_PARTE_ALEATORIA_ID = 40;

    void onCreateEntorno();

    void onStart();

    void guardaGrupo(Grupo grupo, Key clave, String autor);

    Grupo obtenerGrupo(String id);

    ArrayList<Grupo> obtenerGrupos();

    ArrayList<Contacto> obtenerContactos();

    ArrayList<Conversacion> obtenerConversacionesAbiertas();

    ArrayList<String> obtenerConversacionesPendiente();

    int subscribirYActualizarGrupos();

    int getModo();

    Environment getEnv();

    void setModo(int mode);

    /**
     * Busca en DHT si existe un usuario y devuelve
     *
     * @param idGrupo
     */
    void buscaGrupo(String idGrupo);

    Usuario buscaUsuario(String usuario);

    void nuevoContacto(String nombreUsuario, String alias);

    void eliminaContacto(String id);

    void eliminaConversacion(String id);

    void vaciaConversacion(String id);

    void registrarUsuario(String nombre, String contr) throws Exception;

    void crearGrupo(String nombreGrupo);

    void abandonaGrupo(String idGrupo);

    void onStop();

    void onDestroy();

    void cerrarSesion();

    void mensajeRecibido(Mensaje mensaje, Id idConversacion);

    ArrayList<Mensaje> obtieneMensajes(String id, int primerMsj, int ultimoMsj, int tipoDeConversacion);

    void muestraMensajes(Conversacion conv);

    void noMuestresMensajes();

    void enviaMensaje(int tipo, String contenido, String idString, boolean individual);

    void enviaMensaje(int tipo, String contenido, Id idDestino, boolean individual);

    void guardaMensaje(Mensaje mensaje, Id conversacion);

    void errorEnviando(int error, String mensaje);

    boolean responderEcho();

    void actualizaDireccion(InetAddress ip, int puerto);

    void conectaAGrupo(String id, String privateKeyString);

    void insertaMensajeEnBloque(Id idBloque, Mensaje mensaje);

    void buscaIdDisponible(Continuation<Id, Exception> cont);

    void actualizaDireccionBloque(Id grupo, Mensaje mensaje);

    PastryIdFactory getPastryIdFactory();

    void inicializaBBDD();

    void borraBBDD();

    Logger getLogger();

    Usuario getUsuario();

    Conversacion getConversacion(Id id);

    void guardarConversacion(Conversacion conversacion);

    int getError();

    void guardaUsuarioEnPastry(Usuario usuario, BloqueMensajes bloqueMensajes);

    Llavero getLlavero();

    void guardarLlavero();

    InetSocketAddress getDireccion();

    ArrayList<String> obtenerClavesGuardadas() throws Exception;

    boolean nuevaDireccionArranque(String ip, int puerto);

    boolean iniciarConversacion(String id);

    Contacto obtenerContacto(String id);

    void inicioChatIndividual(MensajeCifrado mcifrado);

    void setPingPolicy(boolean allow);

    void ping(String objetivo, long timeout);

    @Override
    void notificarPing(int evento, Id objetivo, String carga);

    void mostrarMensajesImportantes(String idString);

    void imprimeIdEnCache();

    void obtieneMensajesImportantes();

    String obrenerCodigoInvitacion(String id);

    void enviarPeticionUnirAGrupo(String id, String codigo);

    void respuestaUnirAGrupo(Mensaje mensaje);

    void peticionUnirAGrupo(Mensaje mensaje);

    @Override
    void guardarDireccion(InetAddress ip, int puerto);

}

