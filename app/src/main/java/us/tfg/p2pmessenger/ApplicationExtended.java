package us.tfg.p2pmessenger;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import in.co.madhur.chatbubblesdemo.ChatListAdapter;
import in.co.madhur.chatbubblesdemo.ConversacionActivity;
import in.co.madhur.chatbubblesdemo.NativeLoader;
import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import in.co.madhur.chatbubblesdemo.model.Status;
import in.co.madhur.chatbubblesdemo.model.UserType;
import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.controller.ControladorAndroidImpl;
import us.tfg.p2pmessenger.controller.ControladorAppAIDL;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.Usuario;
import us.tfg.p2pmessenger.model.parcelables.ContactoAndroid;
import us.tfg.p2pmessenger.model.parcelables.ConversacionAndroid;
import us.tfg.p2pmessenger.model.parcelables.GrupoAndroid;
import us.tfg.p2pmessenger.model.parcelables.MensajeAndroid;
import us.tfg.p2pmessenger.model.parcelables.UsuarioAndroid;
import us.tfg.p2pmessenger.util.NetworksUtil;
import us.tfg.p2pmessenger.view.AddContactoActivity;
import us.tfg.p2pmessenger.view.LoginActivity;
import us.tfg.p2pmessenger.view.MensajesImportantesActivity;
import us.tfg.p2pmessenger.view.PrincipalGrupoActivity;
import us.tfg.p2pmessenger.view.SignupActivity;
import us.tfg.p2pmessenger.view.UnirseAGrupoActivity;

import static in.co.madhur.chatbubblesdemo.ChatListAdapter.num_colores;
import static us.tfg.p2pmessenger.ActivityBase.TAG;


/**
 * Clase intermediaria entre el controlador y la vista.
 * Bajo esta clase se extiende la jerarquia de las
 * diferentes actividades de las que se compone la aplicacion.
 */
public class ApplicationExtended extends Application {

    /**
     * usado por {@link ConversacionActivity} para la
     * renderizacion de los emojis
     */
    private static ApplicationExtended Instance;

    /**
     * Variable para el renderizado de los emojis
     *
     * @see in.co.madhur.chatbubblesdemo.AndroidUtilities
     */
    public static volatile Handler applicationHandler = null;
    //private ControladorAndroidImpl.ContainsLocal myBinder;

    /**
     * Si se esta enlazado con el servicio o no
     */
    private boolean bounded = false;
    //private ControladorAndroidImpl controlador;

    /**
     * Objeto que implementa la interfaz AIDL y
     * se comunica con el controlador.
     */
    private ControladorAppAIDL controlador;

    /**
     * Actividad que actualmente se encuentra mostrada
     */
    private ActivityBase actual;

    /**
     * Objeto para manejar el momento en el que se termina el enlazado y
     * en el que se finaliza la desconexion.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * Completamente desconectado
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, getClass() + ".onServiceDisconnected(" + name + ")");
            bounded = false;
        }


        /**
         * Enlazado correcto
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, getClass() + ".onServiceConnected(" + name + ", " + service + ")");
            //myBinder = (ControladorAndroidImpl.ContainsLocal) service;
            controlador = ControladorAppAIDL.Stub.asInterface(service);
            bounded = true;
            try {
                controlador.setVista(vista);
            } catch (RemoteException e) {
                Log.d(TAG, "Error al inicializar la vista");
                if (actual != null)
                    actual.finish();
                Handler handler = new Handler(Looper.getMainLooper());

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ApplicationExtended.this, "Error al iniciar", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        /*
                @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, getClass() + ".conectado");
            controlador = ControladorAppAIDL.Stub.asInterface(service);
            bounded = true;
            if (actual != null)
                actual.onServiceLoaded();
        }
         */
    };

    @Override
    public void onCreate() {
        Log.d(TAG, getClass() + ".onCreate()");
        super.onCreate();

        Instance = this;

    }

    /**
     * Poner a funcionar el enlzace con el controlador
     */
    public void iniciar() {
        Log.d(TAG, getClass() + ".iniciar()");
        applicationHandler = new Handler(getInstance().getMainLooper());

        NativeLoader.initNativeLibs(ApplicationExtended.getInstance());

        if (!bounded) {
            Intent intent = new Intent(this, ControladorAndroidImpl.class);
            startService(intent);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Si el controlador esta enlzado
     *
     * @return
     */
    public boolean isIniciado() {
        Log.d(TAG, getClass() + ".isIniciado()");
        return controlador != null;
    }

    /**
     * Le indica al controlador a que modo debe cambiar
     *
     * @param modo
     */
    public void setModo(int modo) {
        Log.d(TAG, getClass() + ".setModo(" + modo + ")");
        try {
            controlador.setModo(modo);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Devuelve el modo en el que se encuentra la aplicacion
     * en cierto momento
     *
     * @return
     */
    public int getModo() {
        Log.d(TAG, getClass() + ".getModo()");
        //return this.modo;
        int ret = 0;
        try {
            ret = controlador.getModo();
        } catch (RemoteException e) {
            Log.d(TAG, "Error al solicitar el modo", e);
        }
        Log.d(TAG, getClass() + ".getModo() = " + ret);
        return ret;
    }

    /**
     * Usado por el renderizador de emojis
     *
     * @return
     * @see ConversacionActivity
     */
    public static ApplicationExtended getInstance() {
        return Instance;
    }

    /**
     * Inicializa la actividad actual
     *
     * @param activity
     * @return
     */
    public ApplicationExtended setCurrentActivity(ActivityBase activity) {
        Log.d(TAG, getClass() + ".setCurrentActivity(" + activity + ")");
        this.actual = activity;
        return this;
    }

    /**
     * Elimina la referencia a la actividad actual
     */
    public void deleteCurrentActivity() {
        Log.d(TAG, getClass() + ".deleteCurrentActivity");
        this.actual = null;
    }

    /**
     * Inicia el proceso de comprobacion de nombre en el controlador
     *
     * @param nombre
     */
    public void compruebaNombre(String nombre) {
        Log.d(TAG, getClass() + ".compruebaNombre(" + nombre + ")");
        try {
            controlador.compruebaNombre(nombre);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    public void actualizaGrupo(String id) {
        Log.d(TAG, getClass() + ".actualizaGrupo(" + id + ")");
        try {
            controlador.actualizaGrupo(id);
        } catch (RemoteException e) {
            Log.d(TAG, "error al pedir al controlador el grupo actualizado", e);
        }
    }


    /**
     * Inicia el proceso de inicio de sesion en el controlador
     *
     * @param usr
     * @param contr
     */
    public void iniciaSesion(String usr, String contr) {
        Log.d(TAG, getClass() + ".iniciaSesion(" + usr + ", ********)");
        try {
            controlador.iniciaSesion(usr, contr);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Inicia el proceso de registro en el controlador
     *
     * @param nombre
     * @param contr
     */
    public void registrarUsuario(String nombre, String contr) {
        Log.d(TAG, getClass() + ".registrarUsuario(" + nombre + " , " + "(contraseña)");
        try {
            controlador.registrarUsuario(nombre, contr);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Entrega al controlador los datos necesarios para unirse
     * a un grupo
     *
     * @param id
     * @param codigo
     */
    public void enviarPeticionUnirAGrupo(String id, String codigo) {
        Log.d(TAG, getClass() + ".conectarAGrupo(" + id + ", " + codigo + ")");
        try {
            controlador.enviarPeticionUnirAGrupo(id, codigo);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Inicia el proceso de cierre de sesion en el controlador
     */
    public void cerrarSesion() {
        Log.d(TAG, getClass() + ".cerrarSesion()");
        try {
            controlador.cerrarSesion();
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Pide al controlador que cree un nuevo contacto
     *
     * @param nombre
     * @param alias
     */
    public void nuevoContacto(Usuario nombre, String alias) {
        Log.d(TAG, getClass() + ".nuevoContacto(" + nombre + ", " + alias + ")");
        try {
            controlador.nuevoContacto(new UsuarioAndroid(nombre), alias);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Pide al controlador la lista de las conversaciones abiertas
     *
     * @return
     */
    public List<Conversacion> obtenerConversacionesAbiertas() {
        Log.d(TAG, getClass() + ".obtenerConversacionesAbiertas()");
        List<ConversacionAndroid> convers = null;
        List<String> pendientes = null;
        try {
            convers = controlador.obtenerConversacionesAbiertas();
            pendientes = controlador.obtenerConversacionesPendiente();

            if (convers != null && pendientes != null) {
                Iterator<ConversacionAndroid> it = convers.iterator();
                while (it.hasNext() && pendientes.size() > 0) {
                    Conversacion c = it.next();
                    if (c != null) {
                        c.setPendiente(pendientes.remove(c.getId().toStringFull()));
                    }
                }
            } else {
                convers = new ArrayList<>();
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
            if (convers == null)
                convers = new ArrayList<>();
        }

        ArrayList<Conversacion> convs;
        if(convers!=null)
        {
            convs=new ArrayList<>(convers.size());
            for(ConversacionAndroid m : convers)
            {
                convs.add(m);
            }
        } else
        {
            convs= null;
        }
        return convs;
    }

    /**
     * Pide al controlador la lista de los contactos
     *
     * @return
     */
    public List<Contacto> obtenerContactos() {
        Log.d(TAG, getClass() + ".obtenerContactos()");
        List<ContactoAndroid> ret = null;
        ArrayList<Contacto> contactos=null;
        try {
            ret = controlador.obtenerContactos();
            if(ret!=null)
            {
                contactos=new ArrayList<>(ret.size());
                for(ContactoAndroid m : ret)
                {
                    contactos.add(m);
                }
            } else
            {
                contactos = null;
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }

        return contactos;
    }

    /**
     * Pide al controlador el id del usuario actual
     *
     * @return
     */
    public Id getMyId() {

        Id id = null;
        Usuario u = null;
        try {
            u = controlador.obtenerUsuario();
            if (u != null)
                id = u.getId();
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }

        return id;

    }

    /**
     * Pide al controlador una cantidad de mensajes indicada por
     * primerMsj y ultimoMsj para la conversacion con el id
     * proporcionado.
     *
     * @param id
     * @param primerMsj
     * @param ultimoMsj
     * @param tipoDeConversacion
     * @return
     */
    public ArrayDeque<ChatMessage> obtieneMensajes(String id, int primerMsj, int ultimoMsj,
                                                   int tipoDeConversacion) {
        Log.d(TAG, getClass() + ".obtieneMensajes(" + id + ", primerMsj=" + primerMsj +
                ", ultimoMsj=" + ultimoMsj + ", tipo=" + tipoDeConversacion + ")");

        List<MensajeAndroid> mensajes;
        ArrayDeque<ChatMessage> mensajesProcesados = new ArrayDeque<>();
        Map<String, String> aliases = new LinkedHashMap<>();

        try {
            mensajes = controlador.obtieneMensajes(id, primerMsj, ultimoMsj, tipoDeConversacion);
            if (mensajes != null) {
                mensajesProcesados = new ArrayDeque<>(mensajes.size());
                Usuario yo = controlador.obtenerUsuario();
                if (yo != null) {
                    if (tipoDeConversacion == Conversacion.TIPO_INDIVIDUAL) {
                        for (Mensaje msj : mensajes) {
                            ChatMessage chatMessage = new ChatMessage();
                            chatMessage.setFecha(msj.getFecha().getTime());

                            if (msj.getDestino().toStringFull().equals(yo.getId().toStringFull()))
                                chatMessage.setUserType(UserType.PRIVATE);
                            else
                                chatMessage.setUserType(UserType.SELF);
                            chatMessage.setContenido(msj.getContenido());


                            chatMessage.setMessageStatus(Status.DELIVERED);

                            if (msj.getClase() == Mensaje.INDIVIDUAL_IMPORTANTE)
                                chatMessage.setImportante(true);
                            else
                                chatMessage.setImportante(false);

                            mensajesProcesados.addLast(chatMessage);
                        }
                    } else if (tipoDeConversacion == Conversacion.TIPO_GRUPO) {
                        Grupo grupo = controlador.obtenerGrupo(id);

                        if (grupo != null) {
                            ArrayList<Usuario> usuarios = grupo.getComponentes();

                            for (Usuario usr : usuarios) {
                                String idUsuario = usr.getId().toStringFull();
                                Contacto c = controlador.obtenerContacto(idUsuario);
                                if (c != null) {
                                    aliases.put(idUsuario, c.getAlias());
                                } else {
                                    aliases.put(idUsuario, idUsuario);
                                }
                            }

                            List<String> indexes = new ArrayList<>(aliases.keySet());

                            for (Mensaje msj : mensajes) {
                                int i = indexes.indexOf(msj.getOrigen().toStringFull());
                                if (i == -1)
                                    i = ChatListAdapter.colores.length - 1;
                                Log.d(TAG, "indice = " + i);
                                i = i % num_colores;
                                Log.d(TAG, "indice tras el modulo = " + i);

                                ChatMessage message = new ChatMessage();
                                message.setMessageStatus(Status.DELIVERED);
                                message.setFecha(msj.getFecha().getTime());
                                if (msj.getClase() == Mensaje.GRUPO_NORMAL ||
                                        msj.getClase() == Mensaje.GRUPO_IMPORTANTE) {

                                    if (msj.getOrigen().equals(yo.getId()))
                                        message.setUserType(UserType.SELF);
                                    else {
                                        message.setUserType(UserType.GROUP);
                                        message.setAlias(aliases.get(msj.getOrigen().toStringFull()));
                                    }
                                    message.setContenido(msj.getContenido());
                                } else if(msj.getClase() == Mensaje.GRUPO_ENTRA){
                                    message.setUserType(UserType.CONTROL);
                                    Contacto cont = controlador.obtenerContacto(msj.getContenido());
                                    if(cont!=null)
                                    {
                                        message.setContenido(cont.getAlias()+" se unió");
                                    } else {
                                        message.setContenido("<0x"+msj.getContenido().substring(0,6)+"..> se unió");
                                    }
                                } else if(msj.getClase() == Mensaje.GRUPO_SALE) {
                                    message.setUserType(UserType.CONTROL);
                                    Contacto cont = controlador.obtenerContacto(msj.getContenido());
                                    if (cont != null) {
                                        message.setContenido(cont.getAlias() + " salió");
                                    } else {
                                        message.setContenido("<0x"+msj.getContenido().substring(0,6)+"..> salió");
                                    }
                                }


                                message.setNumeroContacto(i);

                                if (msj.getClase() == Mensaje.GRUPO_IMPORTANTE)
                                    message.setImportante(true);
                                else
                                    message.setImportante(false);

                                mensajesProcesados.addLast(message);
                            }
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }


        return mensajesProcesados;
    }


    /**
     * Inicia en el controlador el proceso para crear un grupo nuevo
     *
     * @param nombre
     */
    public void crearGrupo(String nombre) {
        Log.d(TAG, getClass() + ".crearGrupo(" + nombre + ")");
        try {
            controlador.crearGrupo(nombre);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Pide al controlador que cree un nuevo objeto de {@link Conversacion}
     * para una nueva conversacion que se va a abrir
     *
     * @param id
     * @return
     */
    public boolean iniciarConversacion(String id) {
        Log.d(TAG, getClass() + ".iniciarConversacion(" + id + ")");
        boolean ret = false;
        try {
            ret = controlador.iniciarConversacion(id);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
        return ret;
    }

    /**
     * Inicia en el controlador el proceso para obtener los mensajes importantes
     *
     * @param id
     * @param isBloque
     * @return
     */
    public String obtenerMensajesImportantes(String id, boolean isBloque) {
        Grupo grupo;
        String alias = "";
        try {
            if (!id.equals(""))
                grupo = controlador.obtieneMensajesImportantes(id, isBloque);
            else {
                grupo = null;
                controlador.obtieneMensajesImportantes(null, isBloque);
            }
            if (grupo == null)
                alias = "Yo";
            else
                alias = grupo.getNombre();
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }

        return alias;
    }

    /**
     * Pide al controlador que envie un menaje
     *
     * @param tipo
     * @param contenido
     * @param idDestino
     * @param individual
     */
    public void enviaMensaje(int tipo, String contenido, String idDestino,
                             boolean individual) {
        Log.d(TAG, getClass() + ".enviaMensaje(tipo = " + tipo + ", " + contenido + ", " + idDestino +
                ", individual = " + individual + ")");
        try {
            controlador.enviaMensaje(tipo, contenido, idDestino, individual);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Pide al controlador el codigo de invitacion correspondiente al grupo
     * indicado como parametro
     *
     * @param idGrupo
     * @return
     */
    public String obtenerCodigoInvitacion(String idGrupo) {
        Log.d(TAG, getClass() + ".obtenerCodigoInvitacion(" + idGrupo + ")");
        String codigo = "";
        try {
            codigo = controlador.obtenerCodigoInvitacion(idGrupo);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
        return codigo;
    }

    /**
     * Pide al controlador un grupo por su id
     *
     * @param id
     * @return
     */
    public Grupo obtenerGrupo(String id) {
        Log.d(TAG, getClass() + ".obtenerGrupo(" + id + ")");
        Grupo grupo = null;
        try {
            grupo = controlador.obtenerGrupo(id);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }

        return grupo;

    }

    /**
     * Pide al controlador los contactos de cada usuario
     * de un grupo
     *
     * @param grupo
     * @return
     */
    public ArrayList<Contacto> obtenerContactosDeGrupo(Grupo grupo) {
        Log.d(TAG, getClass() + ".obtenerContactosDeGrupo(" + grupo + ")");
        ArrayList<Contacto> contactos = new ArrayList<>();

        for (Usuario u : grupo.getComponentes()) {

            try {
                Contacto c = controlador.obtenerContacto(u.getId().toStringFull());
                if (c != null)
                    contactos.add(c);
                else
                    contactos.add(new Contacto(u.getNombre(), u));
            } catch (RemoteException e) {
                Log.d(TAG, "Error de comunicacion con el servicio", e);
            }
        }

        return contactos;
    }

    /**
     * Pide al controlador que borre la informacion sobre
     * la red a la que pertenece en este momento para
     * iniciar la suya propia con nueva informacion.
     */
    public void crearRedPastry() {
        Log.d(TAG, getClass() + ".crearRedPastry()");
        try {
            controlador.crearRedPastry();
            nuevaDireccionArranque(obtenerIpPuerto());
        } catch (RemoteException e) {
            Log.d(TAG, "Error al crear la nueva red");
            Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ApplicationExtended.this, "Error al crear la nueva red", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Pregunta al controlador la ip y puerto del nodo actual para
     * que otro usuario se una a pastry
     *
     * @return
     */
    public String obtenerIpPuerto() {
        Log.d(TAG, getClass() + ".obtenerIpPuerto()");
        // test functions
        //Utils.getMACAddress("wlan0");
        //Utils.getMACAddress("eth0");
        Log.d(TAG, getClass() + "IPv4 = " + NetworksUtil.getIPAddress(NetworksUtil.USE_IPV4));// IPv6
        String ipv4 = NetworksUtil.getIPAddress(NetworksUtil.USE_IPV4); // IPv4
        int puerto = 0;
        try {
            puerto = controlador.obtenerPuerto();
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }

        return ipv4 + ":" + puerto;

    }

    /**
     * Pide al controlador que guarde una nueva direccion
     * de inicio para la red pastry
     *
     * @param ipPuerto
     * @return
     */
    public boolean nuevaDireccionArranque(String ipPuerto) {
        Log.d(TAG, getClass() + ".nuevaDireccionArranque(" + ipPuerto + ")");
        String ip = "";
        int puerto = 9001;

        String IP_PORT_PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5]):" +
                        "([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";

        Pattern p;

        p = Pattern.compile(IP_PORT_PATTERN);

        if (p.matcher(ipPuerto).matches()) {
            ip = ipPuerto.substring(0, ipPuerto.indexOf(':'));
            puerto = Integer.parseInt(ipPuerto.substring(ipPuerto.indexOf(':') + 1));
        }
        boolean ret = true;

        try {
            controlador.nuevaDireccionArranque(ip, puerto);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
            ret = false;
        }
        return ret;
    }

    /**
     * Inicia el proceso en el controlador para abandonar un grupo
     *
     * @param id
     */
    public void abandonaGrupo(String id) {
        Log.d(TAG, getClass() + ".abandonaGrupo(" + id + ")");
        try {
            controlador.abandonaGrupo(id);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Inicia el proceso en el controlador para eliminar una
     * conversacion
     *
     * @param id
     */
    public void eliminarConversacion(String id) {
        Log.d(TAG, getClass() + ".eliminarConversacion(" + id + ")");
        try {
            controlador.eliminaConversacion(id);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Elimina los mensajes de una conversacion dada por su id
     *
     * @param id
     */
    public void vaciarConversacion(String id) {
        Log.d(TAG, getClass() + ".vaciarConversacion(" + id + ")");
        try {
            controlador.vaciaConversacion(id);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Elimina un contacto
     *
     * @param id
     */
    public void eliminarContacto(String id) {
        Log.d(TAG, getClass() + ".eliminarContacto(" + id + ")");
        try {
            controlador.eliminaContacto(id);
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Dice al controlador que apague el nodo pastry
     */
    public void apagar() {
        Log.d(TAG, getClass() + ".apagar()");
        try {
            vista.apagar();
        } catch (RemoteException e) {
            Log.d(TAG, "Error de comunicacion con el servicio", e);
        }
    }

    /**
     * Objeto que implementa la interfaz AIDL para la comunicacion
     * entre procesos. El proceso con el que se debe comunicar es
     * el controlador.
     */
    VistaAIDL.Stub vista = new VistaAIDL.Stub() {

        public void setModo(int mod) {

        }

        /**
         * Comprueba si el id proporcionado es el id de la
         * conversacion actual
         * @param id
         * @return
         */
        public boolean isActualId(String id) {
            Log.d(TAG, getClass() + ".isActualId(" + id + ")");
            boolean ret = false;

            if (id != null && !"".equals(id) && actual != null
                    && actual instanceof ConversacionActivity) {
                ret = actual.getIdConversacion().equals(id);
            }
            Log.d(TAG, getClass() + ".isActualId(" + id + ") = " + ret);

            return ret;
        }


        /**
         * Aviso del controlador de que ha terminado de iniciarse
         */
        public void onServiceLoaded() {
            Log.d(TAG, getClass() + ".onServiceLoaded()");
            if (actual != null)
                actual.onServiceLoaded();
        }

        /**
         * Callback para mostrar en la vista el resultado de haber
         * comprobado un nombre de usuario
         * @param usuario
         * @param disponible
         */
        public void resultadoNombreUsuario(UsuarioAndroid usuario, int disponible) {
            Log.d(TAG, getClass() + ".resultadoNombreUsuario(" + usuario +
                    ", disponible = " + (disponible != 0) + ")");
            if (actual != null && actual instanceof SignupActivity)
                actual.resultadoNombreUsuario(usuario, disponible);
            else if (actual != null && actual instanceof AddContactoActivity)
                actual.resultadoNombreUsuario(usuario, disponible);
        }


        /**
         * Callback para mostrar en la vista el resultado de
         * haber iniciado sesion correctamente
         */
        public void onLoginSuccess() {
            Log.d(TAG, getClass() + ".onLoginSuccess()");
            if (actual != null && actual instanceof LoginActivity) {
                ((LoginActivity) actual).onLoginSuccess();
            }
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haber un error en el inicio de sesion
         */
        public void onLoginFailed() {
            Log.d(TAG, getClass() + ".onLoginFailed()");
            if (actual != null && actual instanceof LoginActivity) {
                ((LoginActivity) actual).onLoginFailed();
            }
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haberse regisrado correctamente a un usuario
         */
        public void onSignupSuccess() {
            Log.d(TAG, getClass() + ".onSignupSuccess()");
            if (actual != null) {
                actual.onSignupSuccess();
            }
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haber un error al registrar a un usuario
         */
        public void onSignupFailed() {
            Log.d(TAG, getClass() + ".onSignupFailed()");
            SharedPreferences.Editor editor = getSharedPreferences("inicioSesion", MODE_PRIVATE).edit();
            editor.putString("usr", "");
            editor.putString("contenido_desconocido", "");
            editor.apply();
            if (actual != null)
                actual.onSignupFailed();
        }

        /**
         * Callback para mostrar en la vista el resultado de
         */
        public void onJoinGroupSuccess() {
            Log.d(TAG, getClass() + ".onJoinGroupSuccess()");
            Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Unido", Toast.LENGTH_SHORT).show();
                }
            });

            if (actual != null && actual instanceof UnirseAGrupoActivity)
                actual.onJoinGroupSuccess();
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haber un error al unirse a un grupo
         */
        public void onJoinGroupFailed() {
            Log.d(TAG, getClass() + ".onJoinGroupFailed()");
            Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Error al unirse", Toast.LENGTH_LONG).show();
                }
            });
            if (actual != null && actual instanceof UnirseAGrupoActivity)
                actual.onJoinGroupFailed();
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haberse enviado correctamente un mensaje importante
         */
        public void onMensajeImportanteEnviadoCorrecto() {
            Log.d(TAG, getClass() + ".onMensajeImportanteEnviadoCorrecto()");
            if (actual != null && (actual instanceof MensajesImportantesActivity
                    || actual instanceof ConversacionActivity))
                actual.onMensajeImportanteEnviadoCorrecto();
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * no haberse ocurrido un error al enviar un mensaje importante
         */
        public void onMensajeImportanteEnviadoFallido() {
            Log.d(TAG, getClass() + ".onMensajeImportanteEnviadoFallido()");
            if (actual != null && (actual instanceof MensajesImportantesActivity
                    || actual instanceof ConversacionActivity))
                actual.onMensajeImportanteEnviadoFallido();
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haber creado un grupo correctamente
         */
        public void onCreateGroupSuccess() {
            Log.d(TAG, getClass() + ".onCreateGroupSuccess()");
            if (actual != null) {
                actual.onCreateGroupSuccess();
            }
        }

        /**
         * Callback para mostrar en la vista el resultado de
         * haber un error al crear un grupo
         */
        public void onCreateGroupFailed() {
            Log.d(TAG, getClass() + ".onCreateGroupFailed()");
            if (actual != null) {
                actual.onCreateGroupFailed();
            }
        }

        /**
         * Metodo llamado por el controlador cuando ha leido los mensajes
         * importantes de un bloque
         * @param mensajes
         * @param idAnterior
         * @param idActual
         * @param idSiguiente
         * @param grupo
         */
        public void resultadoMensajesImportantes(final List<MensajeAndroid> mensajes
                , String idAnterior, String idActual, String idSiguiente, boolean grupo) {

            Log.d(TAG, getClass() + ".resultadoMensajesImportantes(<mensajes(" +
                    mensajes.size() + ")>, " + idAnterior + " <- " + idActual + " -> " + idSiguiente);

            // procesa los mensajes para entregarselos a la vista

            ArrayDeque<ChatMessage> mensajesProcesados = new ArrayDeque<>(mensajes.size());
            try {
                Usuario yo = controlador.obtenerUsuario();

                if (!grupo) {
                    for (Mensaje msj : mensajes) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setFecha(msj.getFecha().getTime());

                        Contacto c = null;
                        String id = "";
                        if (msj.getDestino().equals(yo.getId())) {
                            chatMessage.setUserType(UserType.SELF);
                            id = msj.getOrigen().toStringFull();
                        } else {
                            chatMessage.setUserType(UserType.PRIVATE);
                            id = msj.getDestino().toStringFull();
                        }

                        c = controlador.obtenerContacto(id);
                        if (c != null)
                            chatMessage.setAlias(c.getAlias());
                        else
                            chatMessage.setAlias(id);

                        chatMessage.setContenido(msj.getContenido());
                        chatMessage.setMessageStatus(Status.DELIVERED);

                        chatMessage.setImportante(true);

                        mensajesProcesados.addLast(chatMessage);
                    }
                } else {

                    Map<String, String> aliases = new LinkedHashMap<>();

                    for (Mensaje msj : mensajes) {
                        ChatMessage message = new ChatMessage();

                        String idUsr = msj.getOrigen().toStringFull();
                        String alias = aliases.get(idUsr);
                        if (alias == null) {
                            Contacto c = controlador.obtenerContacto(idUsr);
                            if (c != null) {
                                message.setAlias(c.getAlias());
                                aliases.put(idUsr, c.getAlias());
                            } else {
                                message.setAlias(idUsr);
                                aliases.put(idUsr, idUsr);
                            }
                        }

                        message.setFecha(msj.getFecha().getTime());
                        message.setUserType(UserType.GROUP);

                        message.setContenido(msj.getContenido());
                        message.setNumeroContacto(num_colores - 1);

                        message.setImportante(true);

                        mensajesProcesados.addLast(message);
                    }
                }
            } catch (RemoteException e) {
                Log.d(TAG, "Error de comunicacion con el servicio", e);
            }

            if (actual != null && actual instanceof MensajesImportantesActivity) {
                actual.resultadoMensajesImportantes(mensajesProcesados, idAnterior, idActual, idSiguiente);
            }
        }

        /**
         * El controlador ha recibido un mensaje nuevo y quiere mostrarlo
         * por pantalla
         * @param mensaje
         * @param alias
         */
        @Override
        public void muestraMensaje(MensajeAndroid mensaje, String alias) {
            Log.d(TAG, getClass() + ".muestraMensaje(" + mensaje + ", " + alias + ")");

            ChatMessage mensajeProcesado = new ChatMessage();
            String destino = "";

            Log.d(TAG, "creado chatMessage");
            if (mensaje.getDestino().equals(getMyId())) {
                Log.d(TAG, "mensajeIndividualParaMi");
                //el mensaje es individual para mi
                destino = mensaje.getOrigen().toStringFull();

                if (actual != null && actual instanceof ConversacionActivity &&
                        actual.getIdConversacion().equals(mensaje.getOrigen().toStringFull())) {
                    mensajeProcesado.setImportante(mensaje.getClase() == Mensaje.INDIVIDUAL_IMPORTANTE);
                    mensajeProcesado.setAlias(alias);
                    mensajeProcesado.setUserType(UserType.PRIVATE);
                    mensajeProcesado.setContenido(mensaje.getContenido());
                    mensajeProcesado.setFecha(mensaje.getFecha().getTime());
                    mensajeProcesado.setMessageStatus(Status.DELIVERED);
                    actual.onReceivedMessage(mensajeProcesado);
                } else {
                    // TODO enviar notificacion
                    Log.d(TAG, getClass() + ".notificacion: Mensaje de " + alias + ": " + mensaje.getContenido());
                }
            } else {
                Log.d(TAG, "mensajeDeGrupoParaMi");
                destino = mensaje.getDestino().toStringFull();
                Map<String, String> aliases = new LinkedHashMap<>();
                //el mensaje es para un grupo
                Grupo grupo = null;
                try {
                    grupo = controlador.obtenerGrupo(mensaje.getDestino().toStringFull());
                } catch (RemoteException e) {
                    Log.d(TAG, "Error de comunicacion con el servicio", e);
                }
                if (grupo != null) {
                    ArrayList<Usuario> usuarios = grupo.getComponentes();

                    for (Usuario usr : usuarios) {
                        Contacto c = null;
                        try {
                            c = controlador.obtenerContacto(usr.getId().toStringFull());
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error de comunicacion con el servicio", e);
                        }
                        String id = usr.getId().toStringFull();
                        if (c != null) {
                            aliases.put(id, c.getAlias());
                        } else {
                            aliases.put(id, id);
                        }

                        List<String> indexes = new ArrayList<>(aliases.keySet());

                        int i = indexes.indexOf(mensaje.getOrigen().toStringFull());
                        if (i == -1)
                            i = num_colores - 1;
                        i = i % num_colores;

                        mensajeProcesado.setMessageStatus(Status.DELIVERED);
                        mensajeProcesado.setFecha(mensaje.getFecha().getTime());

                        mensajeProcesado.setUserType(UserType.GROUP);

                        mensajeProcesado.setContenido(mensaje.getContenido());
                        mensajeProcesado.setNumeroContacto(i);

                        mensajeProcesado.setImportante(mensaje.getClase() == Mensaje.GRUPO_IMPORTANTE);
                        mensajeProcesado.setAlias(aliases.get(mensaje.getOrigen().toStringFull()));
                    }
                }

                if (actual != null && actual instanceof ConversacionActivity) {
                    Log.d(TAG, getClass() + ".enviandoMensajeAConversacionActual(" + mensajeProcesado.getContenido() + ")");
                    actual.onReceivedMessage(mensajeProcesado);
                } else {
                    Log.d(TAG, getClass() + ".notificacion(" + mensajeProcesado.getContenido() + ")");
                    // TODO notificacion del mensaje
                }

            }
            Log.d(TAG, "fin " + getClass() + ".muestraMensaje()");
        }

        /**
         * Procesa una excepcion lanzada en el controlador que debe ser
         * notificada al cliente
         * @param e
         */
        @Override
        public void excepcion(String e) {
            Log.d(TAG, getClass() + ".excepcion(excepcion)");
            Log.d(TAG, "excepcion: " + e);
        }

        /**
         * Ocurrio un error al enviar un mensaje
         * @param error
         * @param mensaje
         */
        @Override
        public void errorEnviando(int error, String mensaje) {

        }

        /**
         * Se ha recibido una respuesta a una peticion de eco
         * @param respuesta
         */
        @Override
        public void notificarPing(String respuesta) {

        }

        /**
         * El usuario desea apagar el nodo pastry para dejar de
         * recibir mensajes
         */
        @Override
        public void apagar() {
            Log.d(TAG, getClass() + ".apagar()");
            try {
                controlador.onStop();
            } catch (RemoteException e) {
                Log.d(TAG, "Error de comunicacion con el servicio", e);
            }
            unbindService(mServiceConnection);
            Intent intent = new Intent(ApplicationExtended.this, ControladorAndroidImpl.class);
            stopService(intent);
        }

        /**
         * Metodo asincrono ejecutado cuando se termina de cargar la actualizacion
         * de un grupo
         * @param grupoActualizado
         */
        public void onGrupoActualizado(GrupoAndroid grupoActualizado) {
            Log.d(TAG, getClass() + ".onGrupoActualizado(" + grupoActualizado + ")");
            if (grupoActualizado != null) {
                if (actual != null && actual instanceof PrincipalGrupoActivity &&
                        actual.getIdConversacion().equals(
                                grupoActualizado.getId().toStringFull())) {
                    actual.onGrupoActualizado(grupoActualizado);
                }
            }
        }
    };
}
