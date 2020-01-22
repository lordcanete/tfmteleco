package us.tfg.p2pmessenger;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayDeque;

import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Usuario;

/**
 * Actividad de la que derivan todas las presentes en la
 * aplicacion. Se utiliza para que el controlador tenga
 * un enlace a la vista que se esta mostrando en cada
 * momento y la vista tenga una referencia al controlador.
 * En cada metodo onStart() se le dice al controlador
 * que el metodo actual es el que llama. De esta manera
 * no hay que copiar este trozo de codigo en cada actividad
 *
 */
public class ActivityBase extends AppCompatActivity {


    /**
     * Etiqueta para localizar con facilidad los logs
     */
    public static final String TAG = "p2pMessenger";

    /**
     * Si esta enlazado con el servicio o no
     */
    protected boolean bounded=false;

    /**
     * Enlace de la vista con el controlador
     */
    protected ApplicationExtended controlador;

    /**
     * Al iniciar la actividad, mostrar mensaje de log y
     * si se esta conectado al servicio, indicar cual es la
     * actividad actual
     */
    @Override
    protected void onStart() {
        Log.d(TAG,getClass()+".onStart()");
        super.onStart();
        if(!bounded)
        {
            controlador=ApplicationExtended.getInstance().setCurrentActivity(this);
            bounded=true;
        }
    }

    /**
     * Al continuar la actividad, se indica cual es la actividad actual,
     * solo si se esta conectado al controlador
     */
    @Override
    protected void onResume()
    {
        Log.d(TAG,getClass()+".onResume()");
        super.onResume();
        if(!bounded)
        {
            controlador=ApplicationExtended.getInstance().setCurrentActivity(this);
            bounded=true;
        }
    }

    /**
     * Al pausar la actividad, se le indica al controlador que no
     * le pase mas informacion, asi no se perdera el resultado
     * de ninguna operacion
     */
    @Override
    protected void onPause()
    {
        Log.d(TAG,getClass()+".onPause()");
        super.onPause();
        if(bounded)
        {
            ApplicationExtended.getInstance().deleteCurrentActivity();
            bounded=false;
            controlador=null;
        }
    }

    protected void resultadoNombreUsuario(Usuario usuario, int disponible)
    {
    }

    protected void onSignupSuccess()
    {
    }

    public void onSignupFailed()
    {
    }

    protected void onServiceLoaded()
    {
    }

    public void onCreateGroupSuccess()
    {
    }

    public void onCreateGroupFailed()
    {
    }

    public void resultadoMensajesImportantes(ArrayDeque<ChatMessage> mensajes, String idAnterior, String idActual,
                                             String idSiguiente)
    {
    }

    public void onReceivedMessage(ChatMessage message)
    {
    }

    public void onJoinGroupSuccess() {
    }

    public void onJoinGroupFailed() {
    }

    public void onMensajeImportanteEnviadoCorrecto()
    {
    }

    public void onMensajeImportanteEnviadoFallido()
    {
    }

    public void onGrupoActualizado(final Grupo grupoActualizado)
    {
    }

    public String getIdConversacion()
    {
        return null;
    }
}
