// VistaAIDL.aidl
package us.tfg.p2pmessenger;

import us.tfg.p2pmessenger.model.parcelables.MensajeAndroid;
import us.tfg.p2pmessenger.model.parcelables.UsuarioAndroid;
import us.tfg.p2pmessenger.model.parcelables.GrupoAndroid;
// Declare any non-default types here with import statements


interface VistaAIDL {


    oneway void muestraMensaje(in MensajeAndroid mensaje, String alias);

    boolean isActualId(String id);

    oneway void errorEnviando(int error,String mensaje);

    oneway void notificarPing(String respuesta);

    oneway void setModo(int mod);

    oneway void onServiceLoaded();

    // todo borrar al acabar el proyecto (puerta trasera)
    oneway void apagar();

    oneway void excepcion(String e);

    oneway void resultadoNombreUsuario(in UsuarioAndroid usuario, int disponible);

    oneway void onLoginSuccess();

    oneway void onLoginFailed();

    oneway void onSignupSuccess();

    oneway void onSignupFailed();

    oneway void onCreateGroupSuccess();

    oneway void onCreateGroupFailed();

    oneway void onJoinGroupSuccess();

    oneway void onJoinGroupFailed();

    oneway void onMensajeImportanteEnviadoCorrecto();

    oneway void onMensajeImportanteEnviadoFallido();

    oneway void resultadoMensajesImportantes(in List<MensajeAndroid> mensajes
            , String idAnterior, String idActual, String idSiguiente, boolean grupo);

    oneway void onGrupoActualizado(in GrupoAndroid grupoActualizado);
}
