// InterfazControlador.aidl
package us.tfg.p2pmessenger.controller;

// Declare any non-default types here with import statements
import us.tfg.p2pmessenger.model.parcelables.ContactoAndroid;
import us.tfg.p2pmessenger.model.parcelables.ConversacionAndroid;
import us.tfg.p2pmessenger.model.parcelables.GrupoAndroid;
import us.tfg.p2pmessenger.model.parcelables.MensajeAndroid;
import us.tfg.p2pmessenger.model.parcelables.UsuarioAndroid;
import us.tfg.p2pmessenger.VistaAIDL;

interface ControladorAppAIDL
{
    // para enviar las respuestas a la aplicacion
    oneway void setVista(VistaAIDL vista1);
    oneway void deleteVista();

    GrupoAndroid obtenerGrupo(String id);

    List<ContactoAndroid> obtenerContactos();

    List<ConversacionAndroid> obtenerConversacionesAbiertas();

    List<String> obtenerConversacionesPendiente();

    int subscribirYActualizarGrupos();

    oneway void actualizaGrupo(String id);

    int getModo();

    oneway void setModo(int mode);

    /**
     * Busca en DHT si existe un usuario y devuelve
     *
     * @param idGrupo
     */
    oneway void buscaGrupo(String idGrupo);

    oneway void nuevoContacto(in UsuarioAndroid usuario, String alias);

    oneway void eliminaContacto(String id);

    oneway void eliminaConversacion(String id);

    oneway void vaciaConversacion(String id);

    oneway void registrarUsuario(String nombre, String contr);

    oneway void crearGrupo(String nombreGrupo);

    oneway void abandonaGrupo(String idGrupo);

    oneway void onStop();

    oneway void cerrarSesion();

    void crearRedPastry();

    List<MensajeAndroid> obtieneMensajes(String id, int primerMsj, int ultimoMsj, int tipoDeConversacion);

    oneway void muestraMensajes(in ConversacionAndroid conv);

    oneway void enviaMensaje(int tipo, String contenido, String idString, boolean individual);

    oneway void conectaAGrupo(String id, String privateKeyString);

    oneway void guardarConversacion(in ConversacionAndroid conversacion);

    int getError();

    oneway void guardarLlavero();

    List<String> obtenerClavesGuardadas();

    oneway void nuevaDireccionArranque(String ip, int puerto);

    boolean iniciarConversacion(String id);

    ContactoAndroid obtenerContacto(String id);

    UsuarioAndroid obtenerUsuario();

    oneway void setPingPolicy(boolean allow);

    oneway void ping(String objetivo, long timeout);

    oneway void mostrarMensajesImportantes(String idString);

    oneway void imprimeIdEnCache();

    GrupoAndroid obtieneMensajesImportantes(String id, boolean isBloque);

    oneway void compruebaNombre(String nombre);

    oneway void iniciaSesion(String usr, String contr);

    oneway void enviarPeticionUnirAGrupo(String id,String codigo);

    String obtenerCodigoInvitacion(String id);

    int obtenerPuerto();
}
