package us.tfg.p2pmessenger.controller;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.crypto.SecretKey;

import in.co.madhur.chatbubblesdemo.ConversacionActivity;
import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastPolicy;
import rice.p2p.past.gc.GCPastImpl;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.PersistentStorageAndroid;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;
import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.VistaAIDL;
import us.tfg.p2pmessenger.model.BloqueMensajes;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.EntradaMensaje;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.GrupoCifrado;
import us.tfg.p2pmessenger.model.ManejadorBBDDAndroid;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.MensajeCifrado;
import us.tfg.p2pmessenger.model.Usuario;
import us.tfg.p2pmessenger.model.parcelables.ContactoAndroid;
import us.tfg.p2pmessenger.model.parcelables.ConversacionAndroid;
import us.tfg.p2pmessenger.model.parcelables.GrupoAndroid;
import us.tfg.p2pmessenger.model.parcelables.MensajeAndroid;
import us.tfg.p2pmessenger.model.parcelables.UsuarioAndroid;
import us.tfg.p2pmessenger.util.Base64;
import us.tfg.p2pmessenger.util.NetworksUtil;
import us.tfg.p2pmessenger.view.ConversacionesActivity;
import us.tfg.p2pmessenger.view.Vista;

import static in.co.madhur.chatbubblesdemo.Constants.TAG;
import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_CLAVE_ASIMETRICA;
import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_CLAVE_SIMETRICA;
import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_FIRMA;
import static us.tfg.p2pmessenger.controller.Controlador.EXTENSION_CADUCIDAD_CODIGO;
import static us.tfg.p2pmessenger.controller.Controlador.EXTENSION_CODIGO;
import static us.tfg.p2pmessenger.controller.Controlador.EXTENSION_CONVERSACION_PENDIENTE;
import static us.tfg.p2pmessenger.controller.Controlador.EXTENSION_ULTIMO_BLOQUE;
import static us.tfg.p2pmessenger.controller.Controlador.IDENTIFICADOR_SECRETO_DE_USUARIO;
import static us.tfg.p2pmessenger.controller.Controlador.IDENTIFICADOR_USUARIO;
import static us.tfg.p2pmessenger.controller.Controlador.ID_NOTIFICACION;
import static us.tfg.p2pmessenger.controller.Controlador.LONGITUD_CODIGO_UNIRSE_GRUPO;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_APAGADO;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_INICIO_SESION;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_NECESARIA_DIRECION;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_REGISTRO;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_SESION_INICIADA;
import static us.tfg.p2pmessenger.controller.Controlador.NOMBRE_FICHERO_USUARIO;
import static us.tfg.p2pmessenger.controller.Controlador.NOMBRE_OBJETOS_PASTRY;
import static us.tfg.p2pmessenger.controller.Controlador.TAM_CLAVE_ASIMETRICA;
import static us.tfg.p2pmessenger.controller.Controlador.TAM_PARTE_ALEATORIA_ID;

/**
 * Clase que realiza el papel de controlador en el patron
 * "modelo - vista - controlador". Esta clase toma como
 * modelo las clases que definen objetos que se almacenan
 * en pastry y la base de datos. En menor medida, tambien
 * es modelo la clase mensajera. Esta clase tambien a su vez
 * tiene un patron aplicado. {@link AppScribe} es el modelo,
 * pues de el se obtiene la informacion, el controlador
 * es {@link Mensajero} y la vista es esta clase, donde
 * se representa la informacion generada por ese controlador.
 * <p>
 * <p>
 * En android esta clase implementa la interfaz AIDL
 * {@link ControladorAppAIDL} para la comunicacion con
 * la vista. Es necesario AIDL porque tanto la vista como
 * el controlador se encuentran en procesos diferentes.
 * AIDL se encarga de toda la logica RMI. Si algun error
 * se produjera, se genera una {@link RemoteException}.
 */
public class ControladorAndroidImpl extends IntentService {


    /**
     * si ya hay una notificacion, se actualiza en vez de crearse una nueva
     */
    private boolean notificacionesPendientes;

    //
    /**
     * para contar el numero de conversaciones de las que provienen los mensajes
     */
    private ArrayList<Id> idConversacionesPendientes;

    //
    /**
     * lista de mensajes pendientes
     */
    private ArrayList<String> mensajesPendientes;

    //
    /**
     * si al abrir la aplicacio hay alguna notificacion, se cancela
     */
    private boolean notificado;

    //
    /**
     * vista de la aplicacion
     */
    private VistaAIDL vista;


    //
    /**
     * Para almacenar las propiedades de pastry
     */
    private Environment env;

    //
    /**
     * Gestor de logs
     */
    private Logger logger;

    //
    /**
     * Para generar los ids de los nodos de forma aleatoria
     */
    private NodeIdFactory nodeIdFactory;

    //
    /**
     * Para la fabrica de sockets
     */
    private PastryNodeFactory factory;

    //
    /**
     * Para la creacion de los id de los objetos almacenados en "almacenamiento"
     */
    private PastryIdFactory pastryIdFactory;

    //
    /**
     * El nodo que forma parte del anillo DHT
     */
    private PastryNode node;

    //
    /**
     * Objeto que realizara las labores de chat de la aplicacion
     */
    private Mensajero mensajero;

    //
    /**
     * Objeto que realizara las labores de persistencia de objetos
     */
    private Past almacenamiento;

    // Si es necesario, incluir otro objeto para gestionar la entrada
    // y salida de ficheros
    //private GestorFicherosAndroid gestorFicheros;

    //
    /**
     * Objeto para el acceso a la base de datos
     */
    private ManejadorBBDDAndroid db;

    //
    /**
     * Variable para sincronizar la llamada a buscaUsuario
     */
    private boolean varBuscaUsuario;

    //
    /**
     * conversacion que esta abierta en un momento concreto
     */
    private Conversacion conversacionAbierta;

    //
    /**
     * guarda mi direccion IP
     */
    private InetAddress miDireccion;

    //
    /**
     * puerto en el que escucha la aplicacion
     */
    private int puertoEscucha = 9001;

    //
    /**
     * manejador para crear nuevos usuarios y grupo
     */
    private GestorUsuariosGrupos gestorUsuariosGrupos;

    //
    /**
     * manejador de las claves simetricas y los certificados usados
     */
    private Llavero llavero;

    //
    /**
     * observadores de los pings que se realizan
     */
    private Map<Id, Map<String, PingWithTimeout>> observadoresPing;


    //*************************************************************
    // metodos que se sobreescriben

    //
    /**
     * estado actual en el que se encuentra la aplicacion
     */
    private int modo;

    //
    /**
     * si ocurrio algun error
     */
    private int error;

    //
    /**
     * usuario que esta utilizando actualmente le app
     */
    private Usuario yo;

    /**
     * Cuando el nodo termina de iniciarse, se activa
     * esta variable
     */
    private boolean encendido;
    // para avisar a la vista de que el servicio se ha iniciado correctamente
    //private boolean encendido;

    //
    /**
     * Valores que toma el valor del led de notificacion
     */
    public static int colores_led[] = {
            Color.TRANSPARENT,
            Color.WHITE,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.MAGENTA
    };

    /**
     * Constructor sin parametros
     */
    public ControladorAndroidImpl() {
        super("P2PMessenger");
        // hack for JCE Unlimited Strength
        notificado = false;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    /**
     * Primer metodo que se invoca al instanciar el servicio. En el
     * se crea el entorno de pastry y se instancia la base de datos
     */
    public void onCreate() {
        //encendido = false;
        Log.d(TAG, getClass() + ".onCreate()");
        super.onCreate();
        onCreateEntorno();
        encendido = false;
    }

    /**
     * Tarea asin
     */
    private class MetodoOnStart extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(TAG, ControladorAndroidImpl.this.getClass() + ".MetodoOnStart(" + objects + ")");
            onStart();
            return null;
        }

    }


    /**
     * Al instanciarse el servicio, se inicia. Este metodo solo se
     * ejecuta una vez al iniciar el servicio.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return Valor para que android no destruya el servicio a menos
     * que necesite los recursos
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, getClass() + ".onStartCommand(...)");
        //onStart();
        //new MetodoOnStart().execute();
        return START_NOT_STICKY;
    }

    /**
     * Metodo ejecutado cuando se finaliza el proceso
     */
    public void onDestroy() {
        Log.d(TAG, getClass() + ".onDestroy()");
        modo = MODO_APAGADO;
        // destruye el entorno pastry
        this.env.destroy();
        super.onDestroy();
    }

    /**
     * Al enlazarse la vista con el controlador, se llama a este metodo
     *
     * @param intent
     * @return
     */
    @Nullable
    public IBinder onBind(Intent intent) {
        Log.d(TAG, getClass() + ".onBind(" + intent + ")");

        return mBinder;
    }

    /**
     * Lanza una notificacion
     *
     * @param mensaje
     * @param alias
     */
    public void notifica(Mensaje mensaje, String alias) {

        Log.d(TAG, getClass() + ".notifica(" + mensaje + ", " + alias + ")");
        if (!notificacionesPendientes) {
            idConversacionesPendientes = new ArrayList<>();
            mensajesPendientes = new ArrayList<>();
            notificacionesPendientes = true;
        }

        NotificationCompat.Builder mBuilder;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getResources().getString(R.string.app_name));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean individual = mensaje != null && yo.getId().equals(mensaje.getDestino());

        boolean tono = true;
        boolean vibrar = true;
        int color_led = 0;

        String texto = "";
        Contacto c = null;
        String tipp = "";

        PendingIntent resultPendingIntent = null;

        if (individual) {
            tipp = String.valueOf(Conversacion.TIPO_INDIVIDUAL);
            tono = prefs.getBoolean("tono_notificacion_individual", true);
            vibrar = prefs.getBoolean("vibracion_notificacion_individual", true);
            color_led = Integer.parseInt(prefs.getString("luz_individual", "0"));
            int fin = Math.min(20, mensaje.getContenido().length());
            texto = alias + ": " + mensaje.getContenido().substring(0, fin);
            mensajesPendientes.add(texto);
            if (!idConversacionesPendientes.contains(mensaje.getOrigen())) {
                idConversacionesPendientes.add(mensaje.getOrigen());
                db.setValor(mensaje.getOrigen().toStringFull() +
                        EXTENSION_CONVERSACION_PENDIENTE, mensaje.getOrigen().toStringFull());
            }
        } else {
            tipp = String.valueOf(Conversacion.TIPO_GRUPO);
            tono = prefs.getBoolean("tono_notificacion_grupo", true);
            vibrar = prefs.getBoolean("vibracion_notificacion_grupo", true);
            color_led = Integer.parseInt(prefs.getString("luz_grupo", "0"));
            if (!idConversacionesPendientes.contains(mensaje.getDestino())) {
                idConversacionesPendientes.add(mensaje.getDestino());
                db.setValor(mensaje.getDestino().toStringFull() +
                        EXTENSION_CONVERSACION_PENDIENTE, mensaje.getDestino().toStringFull());
            }

            c = obtenerContacto(mensaje.getOrigen().toStringFull());
            if (c != null) {
                texto = c.getAlias() + " @ " + alias + ": " + mensaje.getContenido();
            } else {
                int fin = Math.min(20, mensaje.getContenido().length());
                texto = mensaje.getOrigen() + " @ " + alias + ": " + mensaje.getContenido().substring(0, fin);
            }

            mensajesPendientes.add(texto);
        }


        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        // Sets a title for the Inbox in expanded layout
        // Moves events into the expanded layout

        for (String entrada : mensajesPendientes) {
            inboxStyle.addLine(entrada);
        }

        Intent resultIntent;
        String summary = "";
        if (idConversacionesPendientes.size() == 1) {
            if (mensajesPendientes.size() == 1)
                summary = mensajesPendientes.size() + " mensaje";
            else
                summary = mensajesPendientes.size() + " mensajes";
            inboxStyle.setSummaryText(summary);

            // Creates an explicit intent for an Activity in your app
            resultIntent = new Intent(this, ConversacionActivity.class);
            String contactoJson = "";

            StringWriter strWriter = new StringWriter();

            try (JsonWriter writer = new JsonWriter(strWriter)) {
                writer.beginObject();

                if (individual)
                    writer.name("id").value(mensaje.getOrigen().toStringFull());
                else
                    writer.name("id").value(mensaje.getDestino().toStringFull());

                writer.name("alias").value(alias)
                        .name("tipo").value(tipp)
                        .endObject().close();
                contactoJson = strWriter.toString();
            } catch (IOException e) {
                Log.d(TAG, "Error al serializar el contacto en formato JSON", e);
            }

            resultIntent.putExtra("conversacion", contactoJson);
            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(ConversacionActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        } else {
            summary = mensajesPendientes.size() + " mensajes de "
                    + idConversacionesPendientes.size() + " conversaciones";
            inboxStyle.setSummaryText(summary);
            resultIntent = new Intent(this, ConversacionesActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        }

        mBuilder.setContentText(summary);

        if (tono) {
            mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
            mBuilder.setLights(colores_led[color_led], 400, 1000);
        }
        if (vibrar)
            mBuilder.setVibrate(new long[]{0, 300, 200, 300, 0});
        // Moves the expanded layout object into the notification object.
        mBuilder.setStyle(inboxStyle).setAutoCancel(true);


        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(ID_NOTIFICACION, mBuilder.build());
    }

    /**
     * Tarea asin
     */
    private class IniciaSocketPastry extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(TAG, ControladorAndroidImpl.this.getClass() + ".iniciaSocketPastry(" + objects + ")");
            PastryNodeFactory ret = null;
            try {
                ret = new SocketPastryNodeFactory(
                        (NodeIdFactory) objects[0],
                        InetAddress.getByName(NetworksUtil.getIPAddress(true)),
                        ((Integer) objects[1]).intValue(), (Environment) objects[2]);
            } catch (IOException e) {
                Log.d(TAG, "Error al iniciar la fabrica de sockets", e);
            }
            factory = ret;


            return ret;
        }

    }

    /**
     * Metodo que crea el entorno pastry, el enlace a la
     * base de datos {@link ManejadorBBDDAndroid}, la fabrica
     * de nodos de pastry {@link PastryNodeFactory}y el
     * gestor de usuarios y grupo {@link GestorUsuariosGrupos}
     */
    public void onCreateEntorno() {

        Log.d(TAG, getClass() + "+onCreateEntorno()");

        notificacionesPendientes = false;
        idConversacionesPendientes = new ArrayList<>();
        mensajesPendientes = new ArrayList<>();
        //this.puertoEscucha = puertoEscucha;
        //this.observadoresPing = new HashMap<>();
        //this.vista = vista;
        try {
            env = new Environment();
            logger = env.getLogManager().getLogger(getClass(), "");

            //if (logger.level <= Logger.INFO) logger.log("Creando aplicacion");

            // genera los ids de nodo de forma aleatoria
            nodeIdFactory = new RandomNodeIdFactory(env);

            // crea la fabrica de nodos
            //factory = new SocketPastryNodeFactory(nodeIdFactory, puertoEscucha, env);
            new IniciaSocketPastry().execute(nodeIdFactory, Integer.valueOf(puertoEscucha), env).get();
            Log.d(TAG, ".iniciaSockerPAstry().execute() - despues");

            // inicializacion de la fabrica de ids para los objetos almacenados
            pastryIdFactory = new PastryIdFactory(env);

            db = new ManejadorBBDDAndroid(getBaseContext());

            //this.db.inicializarBD();

            modo = MODO_APAGADO;

            gestorUsuariosGrupos = new GestorUsuariosGrupos(
                    env.getLogManager().getLogger(
                            GestorUsuariosGrupos.class, "GestorUsuariosGrupos"), controladorMensajes);
        } catch (Exception e) {
            Log.d(TAG, "Error onCreateEntorno()", e);
            //if (logger.level <= Logger.SEVERE) logger.logException("Error onCreateEntorno()", e);
        }
    }

    /**
     * No pertenece a la interfaz de android. Es llamado cuando se desea
     * poner a funcionar la capa pastry. Cuando se inicia comprueba cada
     * uno de los parametros necesarios para funcionar y va cambiando el
     * estado convenientemente.
     */
    public void onStart() {
        Log.d(TAG, getClass() + ".onStart()");

        error = 0;
        ArrayList<InetSocketAddress> direccionArranque = null;

        int mode = getModo();

        if (mode == MODO_APAGADO || mode == MODO_NECESARIA_DIRECION) {
            Log.d(TAG, getClass() + " - buscando direcciones");

            try {
                // obtiene las direcciones a las que puede unirse
                direccionArranque = db.getDireccionesDeArranque();
                Log.d(TAG, "db.direccionesDeArranque() = " + direccionArranque);
            } catch (Exception e) {
                error = Vista.ERROR_BASE_DE_DATOS;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                excepcion(content);     // if(logger.level<=Logger.CONFIG) logger.logException("Error en BBDD",e);
            }
            //si tiene alguna direccion, inicia con ellas, en otro caso, entra en el modo necesaria dir
            if (direccionArranque != null && direccionArranque.size() == 0) {
                mode = MODO_NECESARIA_DIRECION;
            } else
                mode = MODO_INICIO_SESION;
        }
        modo = mode;

        //para el modo de inicio de sesion
        if (mode == MODO_INICIO_SESION) {
            Log.d(TAG, getClass() + " hay que iniciar sesion");
            //obtiene usuario y contrasena
            SharedPreferences editor = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE);
            String usuario = "";
            String contr = "";


            if (editor != null) {
                usuario = editor.getString(IDENTIFICADOR_USUARIO, "");
                contr = editor.getString(IDENTIFICADOR_SECRETO_DE_USUARIO, "");
                if (!"".equals(usuario) && !"".equals(contr)) {
                    Log.d(TAG, "hay un usuario guardado");
                    try {
                        // inicia el llavero de claves del usuario
                        llavero = new ControladorLlaveroAndroid(usuario, contr, ControladorAndroidImpl.this);
                    } catch (Exception e) {
                        error = Vista.ERROR_INICIAR_LLAVERO;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        e.printStackTrace(ps);
                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        excepcion(content);
                        if (logger.level <= Logger.CONFIG)
                            logger.logException("Error al iniciar el llavero", e);
                        editor.edit().putString(IDENTIFICADOR_USUARIO, "")
                                .putString(IDENTIFICADOR_SECRETO_DE_USUARIO, "")
                                .apply();
                    }

                    if (error == 0) {
                        Log.d(TAG, "cargando usuario");
                        try {
                            yo = gestorUsuariosGrupos.cargaUsuario(usuario);
                        } catch (Exception e) {
                            error = Vista.ERROR_CARGAR_USUARIO;
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            PrintStream ps = new PrintStream(baos);
                            e.printStackTrace(ps);
                            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                            excepcion(content);
                            if (logger.level <= Logger.CONFIG)
                                logger.logException("Error al cargar el usuario", e);
                        }
                    }

                    if (error == 0) {
                        mode = MODO_SESION_INICIADA;
                    } else {
                        Log.d(TAG, "Error al iniciar el llavero o cargar el usuario. error =  " + error);
                        editor.edit().putString(IDENTIFICADOR_USUARIO, "")
                                .putString(IDENTIFICADOR_SECRETO_DE_USUARIO, "")
                                .apply();
                        if (vista != null) {
                            try {
                                vista.onLoginFailed();
                            } catch (RemoteException e) {
                                Log.d(TAG, "Error al enviar onLoginFailed", e);
                            }
                        }
                    }
                }
            } else {
                mode = MODO_INICIO_SESION;
            }
        }
        modo = mode;

        if (mode == MODO_REGISTRO || mode == MODO_SESION_INICIADA) {
            Log.d(TAG, "arrancar nodo");
            try {
                direccionArranque = db.getDireccionesDeArranque();
            } catch (Exception e) {
                error = Vista.ERROR_BASE_DE_DATOS;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                excepcion(content);     //                if(logger.level<=Logger.WARNING) logger.logException("Error al leer la BBDD",e);
            }
            if (direccionArranque != null && direccionArranque.size() > 0) {
                error = arrancaNodo(direccionArranque);
            }
            if (error == Vista.ERROR_BOOT_NODO) {
                Log.d(TAG, "error al arrancar");
                onStop();

                if (vista != null) {
                    try {
                        vista.onLoginFailed();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar onLoginFailed", e);
                    }
                }

                modo = MODO_NECESARIA_DIRECION;
                db.vaciarDirecciones();
                if (logger.level <= Logger.WARNING)
                    logger.log("No se puede unir el nodo a la red");
            } else if (mode == MODO_SESION_INICIADA) {
                if (vista != null) {
                    try {
                        Log.d(TAG, "Arrancado correctamente");
                        vista.onLoginSuccess();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar onLoginSuccess", e);
                    }
                }
            }
        }

        if (vista != null) {
            try {
                Log.d(TAG, "avisando a la vista del fin del encendido");
                vista.onServiceLoaded();
            } catch (RemoteException e) {
                Log.d(TAG, "Error al iniciar la vista desde el controlador", e);
            }
        }
        encendido = true;
    }

    /**
     * No pertenece a la interfaz de android. Es ejecutado cuando
     * se desea detener el nodo y terminar el entorno pastry,
     * necesario para ejecutar el nodo. Para que el finalizado
     * sea correcto, necesita esperar desde que se destruye el
     * nodo hasta que se termina destruyendo el entorno, de lo
     * contrario surgiran errores inesperados
     */
    public void onStop() {

        Log.d(TAG, getClass() + ".onStop()");
        if (modo == MODO_REGISTRO || modo == MODO_SESION_INICIADA) {
            Log.d(TAG, getClass() + ".onStop() - apagando...");
            //TODO: Debe hacerse lo ultimo!!!!
            noMuestresMensajes();
            // Algo para recibir mensajes sin conexion????
            //if (modo == MODO_SESION_INICIADA)

            try {
                node.destroy();
            } catch (Exception e) {
                Log.d(TAG, "error al apagar el nodo", e);
            }
            try {
                env.getTimeSource().sleep(1000);
            } catch (InterruptedException e) {
                if (logger.level <= Logger.SEVERE)
                    logger.logException("Error sleep tras destruir nodo.", e);
            }
            env.destroy();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                if (logger.level <= Logger.SEVERE)
                    logger.logException("Error sleep tras destruir environment.", e);
            }
            onCreateEntorno();
            //encendido = false;
        }
    }

    /**
     * Cerrar la conversacion actual
     */
    public void noMuestresMensajes() {
        Log.d(TAG, getClass() + ".noMuestresMensajes()");
        conversacionAbierta = null;
    }

    /**
     * Metodo para iniciar el nodo pastry en la red. Para iniciar
     * necesita la IP del nodo al que se quiere conectar. Si quiere
     * iniciar su propia red, necesitara indicar como direccion
     * la suya propia. Por otro lado, cuando una de las direcciones
     * ofrecidas resulta no estar respaldada por ningun otro nodo
     * pastry, introduce reenvios que provocan mucho retardo a la
     * hora de iniciar.
     *
     * @param direccionArranque
     * @return
     */
    private int arrancaNodo(ArrayList<InetSocketAddress> direccionArranque) {
        Log.d(TAG, getClass() + ".arrancaNodo(" + direccionArranque + ")");
        int error = 0;


        String directorioAlmacenamiento = NOMBRE_OBJETOS_PASTRY;

        if (modo == MODO_SESION_INICIADA) {
            // Nombre final del directorio "almacenamiento/nombreDeUsuario/"
            //directorioAlmacenamiento = yo.getNombre();
            try {
                node = factory.newNode((rice.pastry.Id) yo.getId());
            } catch (Exception e) {
                error = Vista.ERROR_CREAR_NODO;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                excepcion(content);
                if (logger.level <= Logger.CONFIG)
                    logger.logException("Error al crear el nodo", e);
            }
        } else {
            //directorioAlmacenamiento = "temporal";
            if (logger.level <= Logger.CONFIG)
                logger.log("No se ha proporcionado ningun nombre de usuario." +
                        " Utilizando uno aleatorio.");
            try {
                node = factory.newNode();
            } catch (Exception e) {
                error = Vista.ERROR_CREAR_NODO;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                excepcion(content);
                if (logger.level <= Logger.CONFIG)
                    logger.logException("Error al crear el nodo", e);
            }
        }

        Log.d(TAG, "nodo iniciado");

        try {
            // Creamos la parte persistente, aqui se guardaran los objetos en memoria permanente
            Storage almacen = new PersistentStorageAndroid(pastryIdFactory,
                    directorioAlmacenamiento, 10 * 1024 * 1024, node.getEnvironment());

            Log.d(TAG, "creando almacenamiento");
            // Creamos el objeto que maneja el almacenamiento
            almacenamiento = new GCPastImpl(node, new StorageManagerImpl(pastryIdFactory, almacen,
                    new LRUCache(new MemoryStorage(pastryIdFactory), 512 * 1024, node.getEnvironment())),
                    10, "us.es.tfg.domferpir.almacenamiento", new PastPolicy.DefaultPastPolicy(), 10);
            //            ((GCPastImpl)this.almacenamiento).

            Log.d(TAG, "almacenamiento = " + almacenamiento);
        } catch (Exception e) {
            Log.d(TAG, "Error al crear el almacenamiento");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            excepcion(content);
            error = Vista.ERROR_CREAR_ALMACENAMIENTO;
            if (logger.level <= Logger.CONFIG)
                logger.logException("Error al crear el almacenamiento", e);

        }

        //TODO: al programar el cliente scribe, instanciarlo y guardarlo en la variable
        if (modo == MODO_SESION_INICIADA)
            mensajero = new MensajeroImpl(node, controladorMensajes);

        Log.d(TAG, "arrancando nodo");
        node.boot(direccionArranque);

        try {
            synchronized (node) {
                while (!node.isReady() && !node.joinFailed()) {
                    node.wait(500);
                    if (node.joinFailed()) {
                        if (logger.level <= Logger.SEVERE)
                            logger.log("No se pudo unir a la red. Motivo: "
                                    + node.joinFailedReason());
                        error = Vista.ERROR_BOOT_NODO;
                    }
                }
            }

            if (error == 0 && modo == MODO_SESION_INICIADA) {
                error = mBinder.subscribirYActualizarGrupos();
            }
        } catch (Exception e) {
            error = Vista.ERROR_BOOT_NODO;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            excepcion(content);
            if (logger.level <= Logger.CONFIG)
                logger.logException("Error al iniciar el nodo", e);

        }
        Log.d(TAG, "fin arrancaNodo()");
        return error;

    }

    /**
     * Devuelve el estado en el que se encuentra la aplicacion.
     *
     * @return modo en el que se encuentra la aplicacion
     */
    public int getModo() {
        Log.d(TAG, getClass() + ".getModo() = " + modo);
        return modo;
    }

    /**
     * Le indica al {@link Mensajero} que envie un nuevo mensaje con el contenido
     * y el tipo indicado. Todos los mensajes enviados desde aqui  son encriptados.
     * <p>
     * Para los mensajes de grupo se utiliza la clave simetrica adjunta en el objeto
     * pastry que almacena el grupo. Si no la tuviese, el mensaje no se enviaria.
     * <p>
     * Si es un mensaje individual, necesitara la clave correspondiente al usuario
     * que recibira el mensaje. En una conversacion individual hay dos claves
     * en funcionamiento. Suponga cliente A y cliente B. Para los mensajes que van
     * A -> B , B genera una clave simetrica y se la envia a A. Para los mensajes
     * B -> A , A genera una clave simetrica y se la envia a B. En A, la clave
     * del sentido A -> B (generada por B) se llama clave saliente
     * {@link Llavero#EXTENSION_CLAVE_SALIENTE} para A y
     * {@link Llavero#EXTENSION_CLAVE_ENTRANTE} para B.
     * y para el entido B -> A los nombres se invierten.
     * <p>
     * En el momento en el que falta una clave simetrica para enviar un mensaje
     * individual, el que quiere mandar el mensaje primero pide una clave.
     * Suponga que A quiere enviar a B. Primero A envia un mensaje de tipo
     * {@link Mensaje#INDIVIDUAL_PETICION_INICIO_CHAT} y B le responde con la
     * clave simetrica generada (y cifrada con la clave publica de A, leida de
     * pastry) en un mensaje del tipo {@link Mensaje#INDIVIDUAL_RESPUESTA_INICIO_CHAT}.
     * Para responder B actuara de la misma manera pero en el sentido contrario.
     * Al generar las claves para recibir informacion, se almacenan para que
     * si se las vuelven a pedir, no se generen nuevas a menos que se
     * especifique lo contrario.
     *
     * @param tipo
     * @param contenido
     * @param idDestino
     * @param individual
     */
    public void enviaMensaje(int tipo, String contenido, Id idDestino, boolean individual) {
        Log.d(TAG, getClass() + ".enviaMensaje(tipo = " + tipo + ", " + contenido + ", " + idDestino.toStringFull()
                + ", individual = " + individual + ")");
        String miNombre = yo.getNombre();
        final Mensaje mensaje = new Mensaje(node.getId(),
                idDestino, contenido, tipo);
        Contacto contacto = null;


        if (mensaje.getClase() == Mensaje.INDIVIDUAL_IMPORTANTE) {
            try {
                contacto = db.obtenerContacto(idDestino, llavero.getClaveSimetrica(miNombre),
                        miNombre);
            } catch (Exception e) {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al leer el contacto con id " + idDestino + " de la bbdd", e);
            }
            if (contacto != null) {
                Id idBloque = contacto.getUsuario().getBloqueMensajesImportantes();
                insertaMensajeEnBloque(idBloque, mensaje);
            } else {
                // buscar usuario en pastry y obtener la id del bloque
                almacenamiento.lookup(idDestino, new Continuation<PastContent, Exception>() {
                    @Override
                    public void receiveResult(PastContent result) {
                        Id bloqueUsuarioEncontrado = null;
                        if (result != null) {
                            if (result instanceof Usuario) {
                                Usuario usr = (Usuario) result;
                                bloqueUsuarioEncontrado = usr.getBloqueMensajesImportantes();
                                insertaMensajeEnBloque(bloqueUsuarioEncontrado, mensaje);
                            } else {
                                if (logger.level <= Logger.INFO) logger.log(
                                        "El objeto leido no es de tipo "
                                                + Usuario.class + " -> " + result);
                            }
                        } else {
                            if (logger.level <= Logger.INFO) logger.log(
                                    "El objeto leido esta vacio");
                        }
                    }

                    @Override
                    public void receiveException(Exception exception) {

                    }
                });
            }
        } else if (mensaje.getClase() == Mensaje.GRUPO_IMPORTANTE) {
            try {
                Grupo grupo = db.obtenerGrupo(idDestino, llavero.getClaveSimetrica(miNombre), yo.getNombre());
                Id idBloque;
                String ultimoIdConocido = db
                        .getValor(grupo.getId().toStringFull() + EXTENSION_ULTIMO_BLOQUE);

                if (ultimoIdConocido.length() > 0)
                    idBloque = rice.pastry.Id.build(ultimoIdConocido);
                else
                    idBloque = grupo.getBloqueMensajesImportantes();

                this.insertaMensajeEnBloque(idBloque, mensaje);
            } catch (Exception e) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                excepcion(content);
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al obtener el contacto de la bbdd", e);
            }
        }
        mensajero.enviaMensaje(mensaje, individual);
    }


    /**
     * continuacion de insercion de bloque de mensajes
     */
    private Continuation<Boolean[], Exception> continuacionInsertarMensaje;

    /**
     * Funcion asincrona que inserta un mensaje importante en el bloque
     * proporcionado como parametro. Primero busca el bloque en pastry,
     * cuando lo tiene, inserta el mensaje en el bloque y este ultimo de
     * vuelta a pastry. Si la insercion en pastry es correcta, llama al
     * metodo {@link VistaAIDL#onMensajeImportanteEnviadoCorrecto()} de
     * forma asincrona.
     * <p>
     * Si no se puede insertar, se repite el proceso completo hasta tres
     * veces (leer de pastry, insertar el mensaje, insertar el bloque en
     * pastry). Si aun asi tampoco se hace, se ejecuta la llamada asincrona
     * {@link VistaAIDL#onMensajeImportanteEnviadoFallido()}
     *
     * @param idBloque
     * @param mensaje
     */
    public void insertaMensajeEnBloque(final Id idBloque, final Mensaje mensaje) {

        Log.d(TAG, getClass() + ".insertaMensajeEnBloque(" + idBloque + ", " + mensaje + ")");
        Continuation<PastContent, Exception> contBuscar = new Continuation<PastContent, Exception>() {
            private Mensaje mensaje;
            private int reintentos;

            @Override
            public void receiveResult(PastContent result) {
                Log.d(TAG, "insertaMensajeEnBloque.ContBusqueda.ReceivedResult(" + result + ")");
                if (result != null && result instanceof BloqueMensajes) {
                    BloqueMensajes bloque = (BloqueMensajes) result;


                    if (bloque.isLleno()) {
                        almacenamiento.lookup(bloque.getSiguienteBloque(), this);
                    } else {
                        MensajeCifrado mensajeCifrado;
                        EntradaMensaje nuevaEntrada;

                        Key clave = null;
                        if (!bloque.isGrupo()) {
                            clave = llavero.getClaveSimetrica(
                                    bloque.getDestinatario().toStringFull() + Llavero.EXTENSION_CLAVE_SALIENTE);
                        } else {
                            clave = llavero.getClaveSimetrica(bloque.getDestinatario().toStringFull());
                        }

                        if (clave == null) {
                            try {
                                clave = ManejadorClaves.generaClaveSimetrica(ALGORITMO_CLAVE_SIMETRICA);
                                if (!bloque.isGrupo()) {
                                    llavero.setClaveSimetrica(
                                            bloque.getDestinatario().toStringFull() + Llavero.EXTENSION_CLAVE_ENTRANTE,
                                            clave);
                                    llavero.guardarLlavero(yo.getNombre());
                                }
                            } catch (NoSuchAlgorithmException e) {
                                if (logger.level <= Logger.WARNING) logger.logException(
                                        "Error al generar la clave simetrica para guardar el mensaje en el bloque",
                                        e);
                            } catch (KeyStoreException e) {
                                if (logger.level <= Logger.WARNING) logger.logException(
                                        "Error al guardar en el llavero la clave generada",
                                        e);
                            }
                        }

                        if (clave != null) {
                            try {
                                mensajeCifrado = new MensajeCifrado(mensaje, clave);
                                X509Certificate certificado = llavero
                                        .getCertificado(bloque.getDestinatario().toStringFull());
                                if (certificado != null) {
                                    String claveSimetricaCifrada = ManejadorClaves
                                            .encriptaClaveSimetrica((SecretKey) clave, certificado.getPublicKey());
                                    nuevaEntrada = new EntradaMensaje(mensajeCifrado, claveSimetricaCifrada, yo.getId());
                                    bloque.insertarEntrada(nuevaEntrada);
                                } else {
                                    if (logger.level <= Logger.WARNING) logger.log(
                                            "No se encuentra el certificado del usuario con Id = " + bloque
                                                    .getDestinatario());
                                    vista.onMensajeImportanteEnviadoFallido();
                                }
                            } catch (Exception e) {
                                if (logger.level <= Logger.WARNING) logger.logException(
                                        "Error al cifrar el mensaje", e);
                                if (vista != null) {
                                    try {
                                        vista.onMensajeImportanteEnviadoFallido();
                                    } catch (RemoteException ex) {
                                        Log.d(TAG, "Error al enviar el resultado", ex);
                                    }
                                }
                            }

                            if (bloque.isLleno()) {
                                // busca el siguiente bloque en la lista
                                buscaIdDisponible(new Continuation<Id, Exception>() {
                                    private BloqueMensajes bloque;

                                    @Override
                                    public void receiveResult(Id result) {
                                        Log.d(TAG, "insertaMensajeEnBloque." +
                                                "ContBusqueda.Result.ContIdDisp.Result(" + result + ")");
                                        final BloqueMensajes siguienteBloque =
                                                new BloqueMensajes(result,
                                                        bloque.getDestinatario(),
                                                        bloque.isGrupo(), bloque.getId());
                                        almacenamiento.insert(siguienteBloque,
                                                new Continuation<Boolean[], Exception>() {
                                                    @Override
                                                    public void receiveResult(Boolean[] result) {
                                                        Log.d(TAG, "insertaMensajeEnBloque.ContBusqueda." +
                                                                "Result.ContIdDisp.Result.ContInsertar.Result(" + result + ")");
                                                        int i = 0;
                                                        for (boolean j : result)
                                                            if (j) i++;
                                                        if (logger.level <= Logger.INFO) logger.log(
                                                                "Insertado nuevo bloque " + siguienteBloque
                                                                        .getId() + " correctamente en " + i + " ubicaciones");
                                                    }

                                                    @Override
                                                    public void receiveException(Exception exception) {
                                                        if (logger.level <= Logger.WARNING)
                                                            logger.logException("Error al crear un nuevo bloque de mensajes",
                                                                    exception);

                                                        if (vista != null) {
                                                            try {
                                                                vista.onMensajeImportanteEnviadoFallido();
                                                            } catch (RemoteException ex) {
                                                                Log.d(TAG, "Error al enviar el resultado", ex);
                                                            }
                                                        }
                                                    }

                                                });

                                        //insertar aqui el bloque que se acaba de llenar
                                        bloque.setSiguienteBloque(result);
                                        if (bloque.isGrupo()) {
                                            try {
                                                db.setValor(bloque.getDestinatario().toStringFull() +
                                                                EXTENSION_ULTIMO_BLOQUE,
                                                        result.toStringFull());
                                            } catch (Exception e) {
                                                if (logger.level <= Logger.WARNING)
                                                    logger.logException(
                                                            "Error al insertar un valor en la bbdd: " + bloque
                                                                    .getDestinatario().toStringFull() +
                                                                    EXTENSION_ULTIMO_BLOQUE + " : " + result
                                                                    .toStringFull(), e);

                                                if (vista != null) {
                                                    try {
                                                        vista.onMensajeImportanteEnviadoFallido();
                                                    } catch (RemoteException ex) {
                                                        Log.d(TAG, "Error al enviar el resultado", ex);
                                                    }
                                                }
                                            }
                                        }
                                        almacenamiento.insert(bloque, getContinuacionInsertarMensaje());
                                    }

                                    @Override
                                    public void receiveException(Exception exception) {

                                    }

                                    public Continuation<Id, Exception> inicializa(BloqueMensajes bloque) {
                                        this.bloque = bloque;
                                        return this;
                                    }
                                }.inicializa(bloque));

                            } else {
                                almacenamiento.insert(bloque, getContinuacionInsertarMensaje());
                                try {
                                    db.setValor(bloque.getDestinatario().toStringFull() +
                                            EXTENSION_ULTIMO_BLOQUE, bloque.getId().toStringFull());
                                } catch (Exception e) {
                                    if (logger.level <= Logger.WARNING)
                                        logger.logException("Error al actualizar el valor del bloque en cache", e);
                                }
                            }
                        } else if (vista != null) {
                            try {
                                vista.onMensajeImportanteEnviadoFallido();
                            } catch (RemoteException e) {
                                Log.d(TAG, "Error al enviar el resultado", e);
                            }
                        }
                    }
                } else {
                    if (logger.level <= Logger.WARNING)
                        logger.log("App.insertaMensajeEnBloque.Cont.receiveResult: " +
                                "El bloque que se esta buscando no existe");
                    if (vista != null) {
                        try {
                            vista.onMensajeImportanteEnviadoFallido();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado", e);
                        }
                    }
                }
            }

            @Override
            public void receiveException(Exception exception) {
                //Log.d(TAG,"insertaMensajeEnBloque.Cont.Exception("+exception+")");
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.ContinuacionBuscar.Exception", exception);
                if (reintentos < 3) {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.ContinuacionBuscar.Exception: reintento " + reintentos++);
                    almacenamiento.lookup(idBloque, this);
                } else {
                    if (logger.level <= Logger.INFO)
                        logger.log("Error al insertarlo de nuevo. 0 ubicaciones actualizadas");
                    if (vista != null) {
                        try {
                            vista.onMensajeImportanteEnviadoFallido();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado, llamando a vista" +
                                    " onMensajeImportanteEnviadoFallido()", e);
                        }
                    }
                }
            }

            Continuation<PastContent, Exception> inicializa(Mensaje mensaje) {
                this.mensaje = mensaje;
                this.reintentos = 0;
                return this;
            }
        }.inicializa(mensaje);

        continuacionInsertarMensaje = new Continuation<Boolean[], Exception>() {
            private Continuation<PastContent, Exception> continuacionBuscar;
            private int reintentos;

            public void receiveResult(Boolean[] result) {
                Log.d(TAG, "insertaMensajeEnBloque.ContInsertar.result(" + result + ")");
                int i = 0;
                for (boolean j : result)
                    if (j) i++;
                if (logger.level <= Logger.INFO) logger.log(
                        "Insertado bloque " + idBloque + " correctamente en " + i + " ubicaciones");
                if (i > 0) {
                    if (logger.level <= Logger.INFO)
                        logger.log("Mensaje importante enviado");

                    String ultimoIdConocido = "";
                    Mensaje mensajeRefrescoUltimoIdBloque = null;

                    if (!mensaje.getDestino().equals(yo.getId())) {
                        try {
                            ultimoIdConocido = db
                                    .getValor(mensaje.getDestino()
                                            .toStringFull() + EXTENSION_ULTIMO_BLOQUE);
                        } catch (Exception e) {
                            if (logger.level <= Logger.WARNING)
                                logger.logException("Error al obtener el valor de " +
                                        mensaje.getDestino().toStringFull()
                                        + EXTENSION_ULTIMO_BLOQUE, e);
                        }
                        if (ultimoIdConocido.length() > 0) {
                            JSONObject contenidoJson = new JSONObject();
                            try {
                                contenidoJson.put("grupo", mensaje.getDestino().toStringFull());
                                contenidoJson.put("id_bloque", ultimoIdConocido);
                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            mensajeRefrescoUltimoIdBloque = new Mensaje(yo.getId(), mensaje.getDestino(),
                                    contenidoJson.toString(),
                                    Mensaje.GRUPO_BLOQUE_UTILIZADO);
                            mensajero.enviaMensaje(mensajeRefrescoUltimoIdBloque, false);
                            if (vista != null) {
                                try {
                                    vista.onMensajeImportanteEnviadoCorrecto();
                                } catch (RemoteException e) {
                                    Log.d(TAG, "Error al enviar el resultado", e);
                                }
                            }
                        }
                    }
                } else if (reintentos < 3) {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.Cont.receiveResult.Cont.receiveResult: reintento " + reintentos++);
                    almacenamiento.lookup(idBloque, this.continuacionBuscar);
                } else {
                    if (logger.level <= Logger.INFO)
                        logger.log("Error al insertarlo de nuevo. 0 ubicaciones actualizadas");
                    if (vista != null) {
                        try {
                            vista.onMensajeImportanteEnviadoFallido();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado, llamando a vista" +
                                    " onMensajeImportanteEnviadoFallido()", e);
                        }
                    }
                }
            }

            @Override
            public void receiveException(Exception exception) {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.Cont.receiveResult.Cont.Exception", exception);
                if (reintentos < 3) {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.Cont.receiveResult.Cont.Exception: reintento " + reintentos++);
                    almacenamiento.lookup(idBloque, this.continuacionBuscar);
                } else {
                    if (vista != null) {
                        try {
                            vista.onMensajeImportanteEnviadoFallido();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado", e);
                        }
                    }
                }
            }

            Continuation<Boolean[], Exception> inicializa(
                    Continuation<PastContent, Exception> continuacionBuscar) {
                this.continuacionBuscar = continuacionBuscar;
                reintentos = 0;
                return this;
            }
        }.inicializa(contBuscar);


        almacenamiento.lookup(idBloque, contBuscar);

    }

    /**
     * Objeto continuacion donde se comprueba que se ha insertado
     * correctamente el objeto en pastry.
     *
     * @return
     */
    private Continuation<Boolean[], Exception> getContinuacionInsertarMensaje() {
        Log.d(TAG, getClass() + ".getContinuacionInsertarMensaje()");
        return this.continuacionInsertarMensaje;
    }


    /**
     * Busqueda de un id que no este en uso. Genera un Id aleatorio, y
     * lo busca en pastry, si obtiene que esta ocupado, busca
     * uno nuevo, hasta que al final encuentre uno que esta vacio.
     * <p>
     * Cuando lo encuentra ejecuta {@link Continuation#receiveResult(Object)}
     * con argumento el id que acaba de comprobar. Esto ayuda a que no se
     * sobreescriba un objeto con otro de otro tipo (lo cual generaria una
     * excepcion.
     *
     * @param cont continuacion para que
     *             de manera asincrona, continue la ejecucion donde lo dejo.
     */
    public void buscaIdDisponible(final Continuation<Id, Exception> cont) {
        Log.d(TAG, getClass() + ".buscaIdDisponible(cont)");
        final Id candidato = pastryIdFactory.buildId(gestorUsuariosGrupos.generateSessionKey(TAM_PARTE_ALEATORIA_ID));
        almacenamiento.lookup(candidato, new Continuation<PastContent, Exception>() {

            int reintentos;

            public Continuation<PastContent, Exception> inicializa() {
                this.reintentos = 0;
                return this;
            }

            @Override
            public void receiveResult(PastContent result) {
                Log.d(TAG, "buscaIdDisponible.Cont.Result(" + result + ")");
                if (result == null)
                    cont.receiveResult(candidato);
                else {
                    Id candidatoAlternativo = pastryIdFactory
                            .buildId(gestorUsuariosGrupos.generateSessionKey(TAM_PARTE_ALEATORIA_ID));
                    almacenamiento.lookup(candidatoAlternativo, this);
                }
            }

            @Override
            public void receiveException(Exception exception) {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al buscar el id " + candidato, exception);
                if (reintentos < 3) {
                    Id candidatoAlternativo = pastryIdFactory
                            .buildId(gestorUsuariosGrupos.generateSessionKey(TAM_PARTE_ALEATORIA_ID));
                    almacenamiento.lookup(candidatoAlternativo, this);
                    reintentos++;
                }
            }

        }.inicializa());
    }

    private Grupo nuevoGrupo;
    /**
     * Objeto continuacion donde se comprueba que se ha insertado
     * correctamente el objeto en pastry.
     *
     * @return
     */
    private Continuation<Boolean[], Exception> continuacionInsertarGrupo;

    /**
     * Metodo que busca en pastry un objeto de tipo grupo. Cuando
     * lo tiene inserta en el objeto el usuario actual, para
     * despues volverlo a insertar en pastry. Reintenta el
     * proceso completo tres veces
     *
     * @param id
     * @param privateKeyString
     */
    public void conectaAGrupo(final String id, final String privateKeyString) {
        Log.d(TAG, getClass() + ".conectaAGrupo(" + id + ", clavePrivada)");
        final Id grupo = rice.pastry.Id.build(id);
        nuevoGrupo = null;

        Continuation<PastContent, Exception> contBuscar = new Continuation<PastContent, Exception>() {
            private ControladorAndroidImpl aplicacion;

            @Override
            public void receiveResult(PastContent result) {
                Log.d(TAG, "conectaAGrupo.ContBusqueda.Result(" + result + ")");
                if (result != null) {
                    GrupoCifrado grupoCifrado = (GrupoCifrado) result;
                    try {
                        //Key clavePrivada=llavero.getClavePrivada(id);
                        //Base64.getEncoder().encodeToString(llavero.getClavePrivada(nuevo.getId().toStringFull()).getEncoded()))
                        byte[] keyraw = Base64.getDecoder().decode(privateKeyString);
                        PrivateKey clavePrivada = ManejadorClaves
                                .leeClavePrivada(keyraw, ALGORITMO_CLAVE_ASIMETRICA);

                        if (clavePrivada == null) {
                            if (logger.level <= Logger.WARNING) logger.logException(
                                    "App.mensajeRecibido: No se ha podido obtener la clave",
                                    llavero.getError());
                            vista.onJoinGroupFailed();
                        } else {

                            KeyStore.PrivateKeyEntry entrada = ManejadorClaves.entryFromKeys(
                                    (PublicKey) grupoCifrado.getCertificado(), clavePrivada);
                            llavero.setEntradaPrivada(id, entrada);
                            llavero.guardarLlavero(yo.getNombre());

                            nuevoGrupo = grupoCifrado.desencriptar(clavePrivada);

                            nuevoGrupo.insertar(yo);
                            llavero.setClaveSimetrica(id, nuevoGrupo.getClaveSimetrica());
                            llavero.guardarLlavero(yo.getNombre());

                            GrupoCifrado modificado = new GrupoCifrado(nuevoGrupo,
                                    (SecretKey) llavero.getClaveSimetrica(id), llavero.getClavePrivada(id));

                            nuevoGrupo = modificado.desencriptar(clavePrivada);

                            almacenamiento.insert(modificado, getContinuacionInsertarGrupo());
                        }
                    } catch (Exception e) {
                        if (logger.level <= Logger.WARNING) logger.logException(
                                "App.conectaAGupo.Cont.receiveResult: Error al desencriptar" +
                                        " el grupo cifrado", e);
                        if (vista != null) {
                            try {
                                vista.onJoinGroupFailed();
                            } catch (RemoteException ex) {
                                Log.d(TAG, "Error al enviar el resultado", ex);
                            }
                        }
                    }

                } else {
                    if (logger.level <= Logger.INFO)
                        logger.log("App.conectaAGupo.Cont.receiveResult: " +
                                "El grupo que esta buscando no existe");
                    if (vista != null) {
                        try {
                            vista.onJoinGroupFailed();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado", e);
                        }
                    }

                }
            }

            @Override
            public void receiveException(Exception exception) {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.ContinuacionBuscar.Exception", exception);
                if (vista != null) {
                    try {
                        vista.onJoinGroupFailed();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar el resultado", e);
                    }
                }
            }

        };

        continuacionInsertarGrupo = new Continuation<Boolean[], Exception>() {
            private Continuation<PastContent, Exception> continuacionBuscar;
            private int reintentos;

            public void receiveResult(Boolean[] result) {
                Log.d(TAG, "conectaAGrupo.ContInsertar.Result(" + result + ")");
                int resultadosOk = 0;

                for (boolean b : result) {
                    if (b) resultadosOk++;
                }
                if (resultadosOk > 0) {
                    try {
                        Key claveBD = llavero.getClaveSimetrica(yo.getNombre());
                        //                        db.insertarGrupo(nuevoGrupo, claveBD, yo.getNombre());
                        db.insertarGrupo(nuevoGrupo, claveBD, yo.getNombre());
                        mensajero.subscribe(grupo);
                        db.insertarConversacionAbierta(nuevoGrupo.getId().toStringFull(),
                                nuevoGrupo.getNombre(), yo.getNombre(), Conversacion.TIPO_GRUPO);
                        //nuevoGrupo=null;
                        if (logger.level <= Logger.INFO) logger.log("Insertado de " +
                                "nuevo satisfactoriamente en " + result.length + "  ubicaciones");
                        vista.onJoinGroupSuccess();
                        enviaMensaje(Mensaje.GRUPO_ENTRA, yo.getId().toStringFull(),
                                nuevoGrupo.getId(), false);

                    } catch (Exception e) {
                        if (logger.level <= Logger.INFO) logger.logException(
                                "App.conectaAGupo.Cont.receiveResult.Cont.ReceiveResult:" +
                                        " Error al insertar en la base de datos", e);
                        if (vista != null) {
                            try {
                                vista.onJoinGroupFailed();
                            } catch (RemoteException ex) {
                                Log.d(TAG, "Error al enviar el resultado", ex);
                            }
                        }
                    }
                } else {
                    if (logger.level <= Logger.INFO)
                        logger.log("Error al insertarlo de nuevo. 0 ubicaciones actualizadas");
                    if (vista != null) {
                        try {
                            vista.onJoinGroupFailed();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado", e);
                        }
                    }
                }
            }

            @Override
            public void receiveException(Exception exception) {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.Cont.receiveResult.Cont.Exception", exception);
                if (reintentos < 3) {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.Cont.receiveResult.Cont.Exception: reintento " + reintentos++);
                    almacenamiento.lookup(grupo, this.continuacionBuscar);
                } else {
                    if (vista != null) {
                        try {
                            vista.onJoinGroupFailed();
                        } catch (RemoteException e) {
                            Log.d(TAG, "Error al enviar el resultado", e);
                        }
                    }
                }
            }

            Continuation<Boolean[], Exception> inicializa(
                    Continuation<PastContent, Exception> continuacionBuscar) {
                this.continuacionBuscar = continuacionBuscar;
                reintentos = 0;
                return this;
            }
        }.inicializa(contBuscar);


        almacenamiento.lookup(grupo, contBuscar);

    }

    /**
     * Objeto continuacion donde se comprueba que se ha insertado
     * correctamente el objeto en pastry.
     *
     * @return
     */
    private Continuation<Boolean[], Exception> getContinuacionInsertarGrupo() {
        Log.d(TAG, getClass() + ".getContinuacionInsertarGrupo()");
        return this.continuacionInsertarGrupo;
    }

    /**
     * Lee de la base de datos un contacto correspondiente al id
     *
     * @param id
     * @return
     */
    public Contacto obtenerContacto(String id) {
        Log.d(TAG, getClass() + ".obtenerContacto(" + id + ")");
        Contacto contacto = null;
        Key claveSimetrica = llavero.getClaveSimetrica(yo.getNombre());
        try {
            contacto = db.obtenerContacto(id, claveSimetrica, yo.getNombre());
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            excepcion(content);
        }
        return contacto;
    }

    /**
     * Cuando se produce una excepcion por una peticion de la vista,
     * se le avisa para que reciba el mensaje de error.
     *
     * @param contenido
     */
    public void excepcion(String contenido) {
        Log.d(TAG, getClass() + ".excepcion( excepcion )");
        if (vista != null) {
            try {
                vista.excepcion(contenido);
            } catch (RemoteException e) {
                Log.d(TAG, "Error al enviar la excepcion.\nOriginal:" + contenido + "\n\n", e);
            }
        }
    }

    /**
     * Interfaz controlador utilizada por los componentes del modelo,
     * como {@link Llavero} y {@link Mensajero}
     */
    private Controlador controladorMensajes = new Controlador() {

        /**
         * @return Llavero que tiene las claves del usuario que se esta iniciado en este momento
         */
        @Override
        public Llavero getLlavero() {
            return llavero;
        }

        /**
         *
         * @return Fabrica de id aleatorios
         */
        @Override
        public PastryIdFactory getPastryIdFactory() {
            return pastryIdFactory;
        }


        /**
         * @return Direccion IP del nodo actual, junto al puerto
         */
        @Override
        public InetSocketAddress getDireccion() {
            Log.d(TAG, getClass() + ".getDireccion()");
            InetSocketAddress sock = null;
            try {
                sock = new InetSocketAddress(InetAddress.getLocalHost(), puertoEscucha);
            } catch (UnknownHostException e) {
                if (logger.level <= Logger.INFO)
                    logger.logException("Error al obtener la direccion del nodo", e);
            }
            return sock;
        }

        /**
         * @return Usuario iniciado en este momento
         */
        @Override
        public Usuario getUsuario() {
            return yo;
        }

        /**
         * {@link Mensajero} llama a este metodo cuando
         * recibe un nuevo mensaje que debe ser procesado.
         * Aqui se procesa que se debe hacer con el mensaje
         * y a quien avisar. En este metodo tambien se
         * guarda el mensaje en la base de datos. Si el
         * mensaje recibido no pertenece a ninguna conversacion
         * abierta, se crea una nueva y se inserta en la
         * base de datos.
         * @param mensaje
         * @param idConversacion
         */
        @Override
        public void mensajeRecibido(Mensaje mensaje, Id idConversacion) {
            Log.d(TAG, getClass() + ".mensajeRecibido(" + mensaje + ", " + idConversacion + ")");
            Conversacion destinatario = null;

            Contacto contacto;

            if (logger.level <= Logger.INFO) logger
                    .log("Notificacion: recibido mensaje de " + idConversacion);
            String miNombre = yo.getNombre();
            Key clave = llavero.getClaveSimetrica(miNombre);
            if (clave == null) {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.mensajeRecibido: No se ha podido obtener la clave", llavero.getError());
            } else {
                try {
                    destinatario = db.obtenerConversacionAbierta(idConversacion.toStringFull(), miNombre);
                    if (destinatario == null) {
                        contacto = db.obtenerContacto(idConversacion, llavero.getClaveSimetrica(miNombre),
                                miNombre);

                        if (contacto != null) {
                            destinatario = new Conversacion(idConversacion, new Date(),
                                    contacto.getAlias(), Conversacion.TIPO_INDIVIDUAL);
                            db.insertarConversacionAbierta(idConversacion.toStringFull(), destinatario.getAlias(),
                                    miNombre, destinatario.getTipo());
                        } else {
                            destinatario = new Conversacion(idConversacion, new Date(),
                                    idConversacion.toStringFull(), Conversacion.TIPO_INDIVIDUAL)
                                    .setMensaje(mensaje.getContenido());
                            db.insertarConversacionAbierta(idConversacion.toStringFull(), idConversacion.toStringFull(),
                                    miNombre, destinatario.getTipo());
                        }
                    } else {
                        destinatario.setMensaje(mensaje.getContenido());
                        db.insertarConversacionAbierta(idConversacion.toStringFull(), destinatario.getAlias(),
                                miNombre, destinatario.getTipo());
                    }
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                    if (logger.level <= Logger.WARNING)
                        logger.logException("Error al actualizar la conversacion" +
                                " a la que pertenece el mensaje", e);
                }
                if (destinatario != null)
                    this.guardaMensaje(mensaje, destinatario.getId());
            }

            boolean abierto = false;
            try {
                abierto = vista.isActualId(idConversacion.toStringFull());
            } catch (RemoteException e) {
                Log.d(TAG, "Error al preguntar por el idActual");
            }
            boolean permitido = mensaje.getClase() == Mensaje.GRUPO_ENTRA
                    || mensaje.getClase() == Mensaje.GRUPO_CAMBIO_LIDER
                    || mensaje.getClase() == Mensaje.GRUPO_NORMAL
                    || mensaje.getClase() == Mensaje.GRUPO_IMPORTANTE
                    || mensaje.getClase() == Mensaje.GRUPO_SALE
                    || mensaje.getClase() == Mensaje.INDIVIDUAL_NORMAL
                    || mensaje.getClase() == Mensaje.INDIVIDUAL_IMPORTANTE;

            if (permitido) {
                if (vista != null && abierto) {
                    Log.d(TAG, "la conversacion " + idConversacion + " esta abierta");
                    try {
                        if (destinatario != null)
                            vista.muestraMensaje(new MensajeAndroid(mensaje), destinatario.getAlias());
                        else
                            vista.muestraMensaje(new MensajeAndroid(mensaje), idConversacion.toStringFull());
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar el resultado", e);
                    }
                } else if (destinatario != null) {
                    notifica(mensaje, destinatario.getAlias());
                } else
                    notifica(mensaje, idConversacion.toStringFull());
            }
        }

        /**
         * Inserta el mensaje en la base de datos, asociado a la conversacion
         * pasada como parametro
         * @param mensaje
         * @param conversacion
         */
        @Override
        public void guardaMensaje(Mensaje mensaje, Id conversacion) {
            Log.d(TAG, getClass() + ".guardaMensaje(" + mensaje + ", " + conversacion + ")");
            Key clave = llavero.getClaveSimetrica(yo.getNombre());
            boolean permitido = mensaje.getClase() == Mensaje.GRUPO_ENTRA
                    || mensaje.getClase() == Mensaje.GRUPO_CAMBIO_LIDER
                    || mensaje.getClase() == Mensaje.GRUPO_NORMAL
                    || mensaje.getClase() == Mensaje.GRUPO_IMPORTANTE
                    || mensaje.getClase() == Mensaje.GRUPO_SALE
                    || mensaje.getClase() == Mensaje.INDIVIDUAL_NORMAL
                    || mensaje.getClase() == Mensaje.INDIVIDUAL_IMPORTANTE;

            if (clave != null && permitido) {
                try {
                    db.insertarMensaje(mensaje, clave,
                            yo.getNombre(), conversacion.toStringFull());
                } catch (Exception e) {
                    if (logger.level <= Logger.WARNING) logger.logException(
                            "App.guardaMensaje: Error al guardar mensaje en la base de datos", e);
                }
            }
        }

        /**
         * Actualiza la cache del ultimo bloque del que la aplicacion para
         * aligerar el proceso de insercion en la siguiente ocasion.
         * @param grupo
         * @param mensaje
         */
        @Override
        public void actualizaDireccionBloque(Id grupo, Mensaje mensaje) {
            Log.d(TAG, getClass() + ".actualizaDireccionBloque(" + grupo + ", " + mensaje + ")");
            String idBloque = null;

            String grupoleido ="";

            try {
                JSONObject reader = new JSONObject(mensaje.getContenido());
                idBloque = reader.getString("id_bloque");
                grupoleido = reader.getString("grupo");
            } catch (JSONException e) {
                if (logger.level <= Logger.WARNING)
                    logger.log(
                            "actualizaDireccionBloque - no coinciden el valor de grupo " +
                                    "y el id del que proviene el mensaje");
            }
            if (grupo.toStringFull().equals(grupoleido)) {
                try {
                    db.setValor(grupo.toStringFull() + EXTENSION_ULTIMO_BLOQUE, idBloque);
                    if (logger.level <= Logger.INFO)
                        logger.log("Actualizda direccion de bloque");

                } catch (Exception e) {
                    if (logger.level <= Logger.WARNING)
                        logger.logException("Error al insertar un valor en la bbdd: "
                                + grupo.toStringFull() + EXTENSION_ULTIMO_BLOQUE +
                                " : " + idBloque, e);
                }
            } else {
                if (logger.level <= Logger.WARNING)
                    logger.log(
                            "actualizaDireccionBloque - no coinciden el valor de grupo " +
                                    "y el id del que proviene el mensaje");

            }
        }

        /**
         * @param error Error al enviar un mensaje
         * @param mensaje Mensaje erroneo
         */
        @Override
        public void errorEnviando(int error, String mensaje) {
            Log.d(TAG, getClass() + ".errorEnviando(error=" + error + ", " + mensaje + ")");
            if (vista != null) {
                try {
                    vista.errorEnviando(error, mensaje);
                } catch (RemoteException e) {
                    Log.d(TAG, "Error al enviar el resultado", e);
                }
                switch (error) {
                    case AppScribe.ERROR_LECTURA_CLAVE_SIMETRICA:

                }
            }
        }

        /**
         * @return Si debe responderse o no a la peticion de eco
         */
        @Override
        public boolean responderEcho() {
            Log.d(TAG, getClass() + ".responderEco()");
            //todo cambiarlo en android para que acceda a las preferencias en vez de a la base de datos
            boolean responder = true;
            try {
                String valor = db.getValor("responder_ping");
                if (!"".equals(valor))
                    responder = Boolean.parseBoolean(valor);
            } catch (Exception e) {
                if (logger.level <= Logger.INFO)
                    logger.logException("Error al obtener la politica de respuesta de pings", e);
            }
            return responder;
        }

        /**
         * Metodo que procesa el mensaje de tipo
         * {@link Mensaje#INDIVIDUAL_PETICION_INICIO_CHAT} Busca en pastry el
         * objeto que representa al usuario y de el toma el certificado
         * para cifrar la clave simetrica que se acaba de generar. De
         * esta manera evitamos que alguien suplante la identidad del
         * legitimo usuario del id que envio el mensaje. Cuando se
         * tiene la clave simetrica cifrada, se genera un nuevo mensaje
         * y se devuelve al origen.
         * @param mcifrado
         */
        @Override
        public void inicioChatIndividual(MensajeCifrado mcifrado) {
            Log.d(TAG, getClass() + ".inicioChatIndividual(" + mcifrado + ")");
            if (mcifrado != null) {
                final Id interlocutor = mcifrado.getOrigen();
                almacenamiento.lookup(interlocutor, new Continuation<PastContent, Exception>() {
                    private MensajeCifrado mcifrado;

                    /**
                     * Cuando se recibe el objeto usuario almacenado en pastry
                     * @param result The result of the command.
                     */
                    @Override
                    public void receiveResult(PastContent result) {
                        Log.d(TAG, "inicioChatIndividual.ContBuscar.Result(" + result + ")");
                        if (result != null && result instanceof Usuario) {
                            try {
                                //todo comprobar si queremos contestar automaticamente cuando alguien
                                //TODO (continuacion linea superior) quiera comunicarse con nosotros
                                Usuario usr = (Usuario) result;
                                llavero.guardarCertificado(interlocutor.toStringFull(), usr.getCertificado());
                                llavero.guardarLlavero(yo.getNombre());

                                String claveCifrada = null;
                                SecretKey sk = (SecretKey) llavero.getClaveSimetrica(
                                        interlocutor.toStringFull() + Llavero.EXTENSION_CLAVE_ENTRANTE);


                                JSONObject reader = new JSONObject(mcifrado.getContenido());

                                boolean nueva = reader.getBoolean("nueva");


                                if (nueva || sk == null) {

                                    String algoritmo = reader.getString("algoritmo");

                                    sk = ManejadorClaves.generaClaveSimetrica(algoritmo);

                                    llavero.setClaveSimetrica(
                                            interlocutor.toStringFull() + Llavero.EXTENSION_CLAVE_ENTRANTE, sk);
                                    llavero.guardarLlavero(yo.getNombre());

                                }
                                claveCifrada = ManejadorClaves
                                        .encriptaClaveSimetrica(sk, usr.getCertificado().getPublicKey());
                                mensajero.responderSolicitudClave(interlocutor, claveCifrada);

                            } catch (Exception e) {
                                if (logger.level <= Logger.WARNING)
                                    logger.logException("Error al generar la clave simetrica " +
                                            "de sesion y compartirla con otro usuario", e);
                            }
                        }

                    }

                    @Override
                    public void receiveException(Exception exception) {
                        if (logger.level <= Logger.WARNING)
                            logger.logException("Error al obtener el usuario", exception);
                    }

                    public Continuation<PastContent, Exception> inicializa(MensajeCifrado mcifrado) {
                        this.mcifrado = mcifrado;
                        return this;
                    }
                }.inicializa(mcifrado));
            }
        }

        /**
         * Cuando una persona se quiere unir al grupo, envia un mensaje especial
         * a la persona que lo invita, ese mensaje se procesa en este metodo.
         *
         * El mensaje contiene el codigo para unirse al grupo. Dicho codigo
         * es aleatorio y tiene un perido de validez de una hora. Dentro de
         * dicho tiempo, toda persona el que envie un mensaje especial a este
         * nodo con el codigo, sera respondida con el id del grupo y la clave
         * privada que nos permitira leer el objeto {@link GrupoCifrado} almacenado
         * en pastry.
         *
         * @param mensaje
         */
        @Override
        public void peticionUnirAGrupo(Mensaje mensaje) {

            Log.d(TAG, getClass() + ".peticionUnirAGrupo(" + mensaje + ")");
            String idGrupoString = db.obtenerCodigosUnirGrupo(mensaje.getContenido());
            String respuesta = "";


            if (!"".equals(idGrupoString)) {
                String idGrupo = idGrupoString.substring(0, yo.getId().toStringFull().length());
                String fechaCaducidadLong = null;
                try {
                    fechaCaducidadLong = db.getValor(idGrupo +
                            EXTENSION_CADUCIDAD_CODIGO);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Date fechaCaducidad;
                if (!"".equals(fechaCaducidadLong)) {
                    fechaCaducidad = new Date(Long.parseLong(fechaCaducidadLong));
                    if (fechaCaducidad.after(new Date())) {
                        Key clavePrivada = llavero.getClavePrivada(idGrupo);
                        String clavePrivadaString = Base64.getEncoder().encodeToString(clavePrivada.getEncoded());

                        StringWriter strWriter = new StringWriter();

                        try (JsonWriter writer = new JsonWriter(strWriter)) {
                            writer.beginObject()
                                    .name("id_grupo").value(idGrupo)
                                    .name("clave_privada").value(clavePrivadaString)
                                    .endObject().close();
                            respuesta = strWriter.toString();
                        } catch (IOException e) {
                            Log.d(TAG, "~~~Error al serializar el contacto en formato JSON", e);
                            respuesta = "~~~Error al obtener el código";
                        }

                    } else {
                        db.setValor(idGrupo + EXTENSION_CADUCIDAD_CODIGO, null);
                        db.setValor(idGrupo + EXTENSION_CODIGO, null);
                        respuesta = "~~~El código solicitado no existe";
                    }
                } else
                    respuesta = "~~~El código solicitado no existe";
            } else
                respuesta = "~~~El código solicitado no existe";

            enviaMensaje(Mensaje.INDIVIDUAL_RESPUESTA_UNIR_GRUPO,
                    respuesta, mensaje.getOrigen(), true);

        }

        /**
         * Aqui se procesa el mensaje de respuesta a la peticion
         * de un usuario a incluirse en un grupo. Este mensaje
         * contiene el id del grupo y la clave privada del mismo.
         * Esta informacion se le pasa al metodo
         * {@link ControladorAndroidImpl#conectaAGrupo(String, String)}
         * con el id del grupo y la clave privada.
         * @param mensaje
         */
        @Override
        public void respuestaUnirAGrupo(Mensaje mensaje) {

            Log.d(TAG, getClass() + ".respuestaUnirAGrupo(" + mensaje + ")");
            String inicio = mensaje.getContenido().substring(0, 3);
            if ("~~~".equals(inicio)) {
                if (vista != null) {
                    try {
                        vista.onJoinGroupFailed();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar el resultado", e);
                    }
                }
                Log.d(TAG, "no se unio al grupo porque : " + mensaje.getContenido());
            } else {
                try {
                    JSONObject reader = new JSONObject(mensaje.getContenido());
                    String idGrupo = reader.getString("id_grupo");
                    String clavePrivada = reader.getString("clave_privada");
                    conectaAGrupo(idGrupo, clavePrivada);
                } catch (JSONException e) {
                    Log.d(TAG, "Error al procesar la informacion", e);
                }
            }
        }


        //TODO borrar puerta trasera
        //todo borrar al terminar el trabajo (puerta trasera)
        @Override
        public void guardarDireccion(InetAddress ip, int puerto) {
            try {
                db.insertarDireccion(ip, puerto);
            } catch (Exception e) {
                if (logger.level <= Logger.INFO)
                    logger.logException("Error al guardar en la BBDD la direccion recibida", e);
            }
        }

        public void apagar() {
            if (vista != null)
                try {
                    vista.apagar();
                } catch (RemoteException e) {
                    Log.d(TAG, "Error al enviar el resultado", e);
                }
        }


        public void encolarComando(String comando) {
        }
    };


    /**
     * Interfaz utlizada para que la vista. Implementa la interfaz AIDL
     * de android, para la comunicacion entre procesos.
     */
    private final ControladorAppAIDL.Stub mBinder;

    {
        mBinder = new ControladorAppAIDL.Stub() {

            /**
             * Devuelve el grupo correspondiente al id
             *
             * @param id
             * @return
             */
            public GrupoAndroid obtenerGrupo(String id) {
                Log.d(TAG, getClass() + ".obtenerGrupo(" + id + ")");
                Grupo ret = null;
                try {
                    ret = db.obtenerGrupo(id, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }


                return new GrupoAndroid(ret);
            }


            /**
             * inicializa la vista que esta abierta en este momento
             *
             * @param vista1
             */
            public void setVista(VistaAIDL vista1) {
                Log.d(TAG, getClass() + ".setVista(" + vista1 + ")");
                vista = vista1;
                if (!notificado && vista1 != null) {
                    try {
                        vista.setModo(getModo());
                        if (encendido)
                            vista.onServiceLoaded();
                        else
                            onStart();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar el resultado", e);
                    }
                }
            }

            /**
             * elimina la vista actual
             */
            public void deleteVista() {
                Log.d(TAG, getClass() + ".deleteVista()");
                vista = null;
            }

            /**
             * Inicia sesion con un usuario
             *
             * @param usr
             * @param contr
             */
            public void iniciaSesion(String usr, String contr) {
                Log.d(TAG, getClass() + ".iniciaSesion(" + usr + ", " + "*******)");
                SharedPreferences.Editor editor = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).edit();
                editor.putString(IDENTIFICADOR_USUARIO, usr);
                editor.putString(IDENTIFICADOR_SECRETO_DE_USUARIO, contr);
                editor.apply();
                ControladorAndroidImpl.this.onStart();
            }

            /**
             * Cierra la sesion que esta abierta
             */
            public void cerrarSesion() {
                Log.d(TAG, getClass() + ".cerrarSesion()");
                SharedPreferences.Editor editor = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).edit();
                editor.putString(IDENTIFICADOR_USUARIO, "");
                editor.putString(IDENTIFICADOR_SECRETO_DE_USUARIO, "");
                editor.apply();
                modo = MODO_INICIO_SESION;
            }

            /**
             * Elimina toda la informacion relativa a la red pastry en la que
             * se encuentra actualmente el usuario e inicializa los valores
             * para empezar desde cero una nueva
             */
            public void crearRedPastry() {
                Log.d(TAG, getClass() + ".crearRedPastry()");
                ControladorAndroidImpl.this.onStop();
                // elimina la base de datos
                db.vaciarDirecciones();
                db.onUpgrade(db.getWritableDatabase(), 0, 0);
                // elimina los objetos almacenados por Pastry
                GestorFicherosAndroid gfich = new GestorFicherosAndroid();
                if (yo == null) {
                    gfich.eliminaFicherosPastry(ControladorAndroidImpl.this, null);
                } else {
                    gfich.eliminaFicherosPastry(ControladorAndroidImpl.this, yo.getNombre());
                }
                SharedPreferences.Editor editor = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).edit();
                editor.putString(IDENTIFICADOR_USUARIO, "");
                editor.putString(IDENTIFICADOR_SECRETO_DE_USUARIO, "");
                editor.apply();

            }

            /**
             * Busca en pastry si hay un usuario con ese nombre ya registrado.
             * Lo habra si en ese lugar hay un objeto usuario. Si el lugar esta
             * ocupado por un objeto de otro tipo, aunque estrictamente hablando
             * ese usuario no este utilizado, no se puede ocupar ese lugar en
             * pastry, ergo ese nombre queda inutilizado
             *
             * @param nombre
             */
            public void compruebaNombre(final String nombre) {
                Log.d(TAG, getClass() + ".compruebaNombre(" + nombre + ")");
        /*
         Log.d(TAG, getClass() + ".run()");
                        if (nombre.equals("sisi") || nombre.equals("nono"))
                        else
                            vista.resultadoNombreUsuario(nombre, false);
                        // onSignupFailed();
        */
                varBuscaUsuario = true;

                almacenamiento.lookup(pastryIdFactory.buildId(nombre),
                        new Continuation<PastContent, Exception>() {
                            @Override
                            public void receiveResult(PastContent result) {
                                Log.d(TAG, "compruebaNombre.ContBuscar.Result(" + result + ")");
                                if (result != null) {
                                    Log.d(TAG, "El usuario " + nombre + " esta ocupado");
                                    if (result instanceof Usuario && vista != null) {
                                        Usuario resultado = (Usuario) result;
                                        try {
                                            vista.resultadoNombreUsuario(new UsuarioAndroid(resultado), 0);
                                        } catch (RemoteException e) {
                                            Log.d(TAG, "Error al enviar el resultado de buscar usuario", e);
                                        }
                                    } else if (vista != null) {
                                        try {
                                            vista.resultadoNombreUsuario(null, 0);
                                        } catch (RemoteException e) {
                                            Log.d(TAG, "Error al enviar el resultado de buscar usuario", e);
                                        }
                                    }
                                } else if (vista != null) {
                                    Log.d(TAG, "El usuario " + nombre + " esta libre");
                                    try {
                                        vista.resultadoNombreUsuario(null, 1);
                                    } catch (RemoteException e) {
                                        Log.d(TAG, "Error al enviar el resultado de buscar usuario", e);
                                    }
                                }
                            }

                            @Override
                            public void receiveException(Exception exception) {
                                varBuscaUsuario = false;
                                if (logger.level <= Logger.WARNING)
                                    logger.logException("Excepcion en App.buscaUsuario." +
                                            "Continuation.receiveException", exception);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                PrintStream ps = new PrintStream(baos);
                                exception.printStackTrace(ps);
                                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                excepcion(content);
                                if (vista != null) {
                                    Log.d(TAG, "Error al leer el usuario " + nombre);
                                    try {
                                        vista.resultadoNombreUsuario(null, 0);
                                    } catch (RemoteException e) {
                                        Log.d(TAG, "Error al enviar el resultado de buscar usuario", e);
                                    }
                                }
                            }

                        });
            }

            /**
             * Realiza el proceso de crear un objeto usuario, con sus claves asociadas,
             * insertarlo en pastry, crear el llavero e inicializarlo y guardar
             * localmente la informacion relativa al inicio de sesion.
             *
             * @param nombre
             * @param contr
             */
            public void registrarUsuario(final String nombre, final String contr) {
        /*

        crear el fichero con la contrasena y el nombre
        crear un nuevo llavero con ese nombre
        crear un usuario y generar las claves
        guardar las claves en el llavero
        crear el usuario en pastry
        cerrar el llavero
        apagar el nodo
        volver a iniciar sesion con el nuevo nombre de usuario
         */

                Log.d(TAG, getClass() + ".registrarUsuario(" + nombre + " , ********* )");

                if (modo == MODO_REGISTRO) {
                    try {


                        Llavero llavero = new ControladorLlaveroAndroid(null, contr, ControladorAndroidImpl.this);
                        Log.d(TAG, "creado llavero");

                        Usuario nuevo = gestorUsuariosGrupos.creaUsuario(
                                nombre, TAM_CLAVE_ASIMETRICA, ALGORITMO_CLAVE_ASIMETRICA,
                                ALGORITMO_FIRMA, ALGORITMO_CLAVE_SIMETRICA);
                        Log.d(TAG, "creado usuario y claves");

                        BloqueMensajes bloqueMensajes = new BloqueMensajes(nuevo.getBloqueMensajesImportantes()
                                , nuevo.getId(), false, null);

                        llavero.setEntradaPrivada(nombre, nuevo.getPrivateKeyEntry());

                        llavero.setClaveSimetrica(nombre, nuevo.getClaveSimetrica());
                        Log.d(TAG, "guardadas las claves en el llavero");

                        llavero.guardarLlavero(nombre);

                        llavero.cerrarLlavero(nombre);

                        SharedPreferences.Editor editor = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).edit();
                        editor.putString(IDENTIFICADOR_USUARIO, nombre);
                        editor.putString(IDENTIFICADOR_SECRETO_DE_USUARIO, contr);
                        editor.apply();
                        guardaUsuarioEnPastry(nuevo, bloqueMensajes);

                    } catch (Exception e) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        e.printStackTrace(ps);
                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        excepcion(content);
                    }
                }

        /* contenido al realizar correctamente la insercion
        Log.d(TAG, getClass() + ".run()");
                        if (nombre.equals("sisi")) {
                            SharedPreferences.Editor editor = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).edit();
                            editor.putString(IDENTIFICADOR_USUARIO, nombre  );
                            editor.putString(IDENTIFICADOR_SECRETO_DE_USUARIO, contr);
                            editor.apply();
                            vista.onSignupSuccess();
                            modo = MODO_SESION_INICIADA;
                        } else
                            vista.onSignupFailed();
                        // onSignupFailed();
         */
            }

            /**
             * Guarda un objeto usuario (sin las claves en el) dentro de pastry,
             * para que los demas usuarios de la red sepan que esta ocupado.
             *
             * @param usuario
             * @param bloqueMensajes
             */
            public void guardaUsuarioEnPastry(Usuario usuario, final BloqueMensajes bloqueMensajes) {

                Log.d(TAG, getClass() + ".guardarUsuarioEnPastry(" + usuario + ", " + bloqueMensajes + ")");

                buscaIdDisponible(new Continuation<Id, Exception>() {
                    private Usuario usuario;

                    @Override
                    public void receiveResult(Id result) {
                        Log.d(TAG, "guardUsuarioEnPastry.ContIdDisp.Result(" + result + ")");
                        usuario.setBloqueMensajesImportantes(result);

                        almacenamiento.insert(usuario, new Continuation<Boolean[], Exception>() {
                            @Override
                            public void receiveResult(Boolean[] result) {
                                Log.d(TAG, "guardarUsuarioEnPasry.ContIdDisp." +
                                        "Result.ContInsert.Result(" + Arrays.toString(result) + ")");
                                int i = 0;
                                for (boolean j : result)
                                    if (j) i++;
                                if (logger.level <= Logger.INFO) logger.log(
                                        "Insertado correctamente en " + i + " ubicaciones");
                                almacenamiento.insert(bloqueMensajes, new Continuation<Boolean[], Exception>() {
                                    @Override
                                    public void receiveResult(Boolean[] result) {
                                        Log.d(TAG, "guardarUsuarioEnPasry.ContIdDisp." +
                                                "Result.ContInsert.Result.ContInserBloque" +
                                                ".Cont(" + Arrays.toString(result) + ")");
                                        int i = 0;
                                        for (boolean j : result)
                                            if (j) i++;
                                        if (logger.level <= Logger.INFO) logger.log(
                                                "Insertado correctamente en " + i + " ubicaciones");
                                        if (i > 0) {
                                            onSignupSuccess();
                                        } else {
                                            onSignupFailed();
                                        }
                                    }

                                    @Override
                                    public void receiveException(Exception exception) {
                                        if (logger.level <= Logger.WARNING) logger.logException(
                                                "App.guardarUsuarioEnPastry.buscarIdVacio.Cont.Exception", exception);
                                        onSignupFailed();
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        PrintStream ps = new PrintStream(baos);
                                        exception.printStackTrace(ps);
                                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                        excepcion(content);
                                    }
                                });
                            }

                            @Override
                            public void receiveException(Exception exception) {
                                if (logger.level <= Logger.WARNING) logger.logException(
                                        "App.guardarUsuarioEnPastry.buscarIdVacio.Cont.Exception", exception);
                                onSignupFailed();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                PrintStream ps = new PrintStream(baos);
                                exception.printStackTrace(ps);
                                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                excepcion(content);
                            }
                        });

                    }

                    @Override
                    public void receiveException(Exception exception) {
                        // la excepcion ya es tratada en buscarIdDisponible()
                        onSignupFailed();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        exception.printStackTrace(ps);
                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        excepcion(content);
                    }

                    public Continuation<Id, Exception> init(Usuario usr) {
                        this.usuario = usr;
                        return this;
                    }
                }.init(usuario));

            }

            /**
             * Metodo que llama a la vista para avisarle del correcto proceso de iniciado de sesion
             */
            public void onSignupSuccess() {
                Log.d(TAG, ControladorAndroidImpl.this.getClass() + ".onSignpSuccess()");

                onStop();
                modo = MODO_INICIO_SESION;
                onStart();

                if (vista != null) {
                    try {
                        vista.onSignupSuccess();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar onSignupSuccess", e);
                    }
                }
            }

            /**
             * Metodo que llama a la vista para avisarle de que durante el inicio
             * de sesion ha ocurrido un error
             */
            public void onSignupFailed() {
                Log.d(TAG, getClass() + ".onSignupFailed()");
                SharedPreferences.Editor editor = getSharedPreferences(
                        NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).edit();

                editor.putString(IDENTIFICADOR_USUARIO, "");
                editor.putString(IDENTIFICADOR_SECRETO_DE_USUARIO, "");
                editor.apply();

                if (vista != null) {
                    try {
                        vista.onSignupFailed();
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error al enviar onSignupFailed", e);
                    }
                }
            }

            /**
             * devuelve el puerto que esta utilizando a la vista
             *
             * @return
             */
            public int obtenerPuerto() {
                return puertoEscucha;
            }


// fin metodos que se deben mantener
//*************************************************************




/*
    public ArrayList<Conversacion> obtenerConversacionesAbiertas() {
        ArrayList<Conversacion> conversaciones = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Conversacion c = new Conversacion();
            c.setAlias("Conversacion " + i);
            c.setFecha(new Date());
            c.setMensaje("Ultimo mensaje de la conversacion " + i);
            if (i % 2 == 0) {
                c.setTipo(Conversacion.TIPO_GRUPO);
                c.setId(new Id(idGrupo + i));
            } else {
                c.setTipo(Conversacion.TIPO_INDIVIDUAL);
                c.setId(new Id(idExtremo + i));
            }
            conversaciones.add(c);
        }

        return conversaciones;
    }

    public ArrayList<String> obtenerConversacionesPendiente() {
        ArrayList<String> lista = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (i % 2 == 0) {
                lista.add(idGrupo + i);
            } else {
                lista.add(idExtremo + i);
            }
        }
        return lista;
    }

    public ArrayList<Contacto> obtenerContactos() {
        ArrayList<Contacto> contactos = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Contacto c = new Contacto();
            c.setAlias("Contacto " + i);
            Usuario u = new Usuario();
            u.setId(new Id(idExtremo + i));
            u.setNombre("Usuario " + i);
            c.setUsuario(u);
            contactos.add(c);
        }

        return contactos;
    }

    public int getModo() {
        Log.d(TAG, getClass() + ".getModo()");
        return modo;
    }

    public void setModo(int modo) {
        Log.d(TAG, getClass() + ".setModo(" + modo + ")");
        this.modo = modo;
    }

    public void onStart() {
        Log.d(TAG, getClass() + ".onStart()");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.d(TAG, "error en sleep", e);
        }

        yo = new Usuario();
        yo.setId(new Id(idLocal));
        yo.setNombre("Domingo");

        if (modo == 0) {
            modo = MODO_INICIO_SESION;
        }

        if (modo == MODO_INICIO_SESION) {
            String usr = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).getString(IDENTIFICADOR_USUARIO, "");
            String contr = getSharedPreferences(NOMBRE_FICHERO_USUARIO, MODE_PRIVATE).getString(IDENTIFICADOR_SECRETO_DE_USUARIO, "");

            if (usr.equals("") || contr.equals("")) {
                error = 2;
            } else if (usr.equals("sisi")) {
                error = 0;
                modo = MODO_SESION_INICIADA;
            } else {
                error = 1;
            }
        }

        Log.d(TAG, "error = " + error + " - modo = " + modo);
    }

    public int getError() {
        return error;
    }

    public void nuevoContacto(String nombreUsuario, String alias) {
        Log.d(TAG, getClass() + ".nuevoContacto(" + nombreUsuario + ", " + alias + ")");
    }

    public Usuario getUsuario() {
        return this.yo;
    }

    public Grupo obtenerGrupo(String id) {
        Usuario u = new Usuario();
        u.setId(new Id(idExtremo + "1"));
        u.setNombre("MasterUser");
        Grupo g = new Grupo(new Id(id), "pepitos", u);
        for (int i = 0; i < 13; i++) {
            Usuario u2 = new Usuario();
            u2.setId(new Id(idExtremo + String.valueOf(1 + i)));
            u2.setNombre("user" + i);
            g.insertar(u2);
        }
        return g;
    }

    public Contacto obtenerContacto(String id) {
        Contacto c = new Contacto();
        c.setAlias("Contacto <0x" + id.substring(0, 6) + ">");
        Usuario u = new Usuario();
        u.setNombre("dummmmmmmy");
        u.setId(new Id(id));
        c.setUsuario(u);
        return c;
    }

    public ArrayList<Mensaje> obtieneMensajes(String id, int primerMsj, int ultimoMsj, int tipoDeConversacion) {
        return new ArrayList<>();
    }

    public void crearGrupo(final String nombre) {
        Log.d(TAG, getClass() + ".crearGrupo(" + nombre + ")");
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, getClass() + ".run()");
                        if (nombre.equals("grupo malo")) {
                            vista.onCreateGroupFailed(nombre);
                        } else
                            vista.onCreateGroupSuccess(nombre);
                        // onSignupFailed();
                    }
                }, 3000);
    }

    public boolean iniciarConversacion(String id) {
        Log.d(TAG, getClass() + ".iniciarConversacion(" + id + ")");
        return true;
    }

    public static final String idBloqueA = "7d157d7c000ae27db146575c08ce30df893d3a64";
    public static final String idBloqueB = "31836aeaab22dc49555a97edb4c753881432e01d";
    public static final String idBloqueC = "39e0574a4abfd646565a3e436c548eeb1684fb57";
    public static final String idBloqueD = "550bf014e4efd9a926e0fa64812b0343578925d1";

    public Grupo obtieneMensajesImportantes(final String id, final boolean isBloque) {
        Log.d(TAG, getClass() + ".obtieneMensajesImportantes(" + id + ")");

        Grupo g = null;
        if (id != null && !isBloque)
            g = new Grupo(new Id(id), "grupo de prueba", new Usuario());

        if (isBloque && idBloqueA.equals(id)) {
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.d(TAG, getClass() + ".run()");
                            ArrayList<Mensaje> mensajes = new ArrayList<>();
                            for (int i = 0; i < 14; i++) {
                                Mensaje m = new Mensaje();
                                if (!id.equals("")) {
                                    m.setClase(Mensaje.GRUPO_IMPORTANTE);
                                } else {
                                    m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                                }
                                m.setContenido("Contenido importantisimo A " + i);
                                m.setDestino(new Id(id));
                                m.setOrigen(new Id(idExtremo + i));
                                m.setFecha(new Date());
                                mensajes.add(m);
                            }

                            vista.resultadoMensajesImportantes(mensajes, null, idBloqueA, idBloqueB, true);
                        }
                    }, 2000);
        } else if (isBloque && idBloqueB.equals(id)) {
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.d(TAG, getClass() + ".run()");
                            ArrayList<Mensaje> mensajes = new ArrayList<>();
                            for (int i = 0; i < 14; i++) {
                                Mensaje m = new Mensaje();
                                if (!id.equals("")) {
                                    m.setClase(Mensaje.GRUPO_IMPORTANTE);
                                } else {
                                    m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                                }
                                m.setContenido("Contenido importantisimo B " + i);
                                m.setDestino(new Id(id));
                                m.setOrigen(new Id(idExtremo + i));
                                m.setFecha(new Date());
                                mensajes.add(m);
                            }

                            vista.resultadoMensajesImportantes(mensajes, idBloqueA, idBloqueB, idBloqueC, true);
                        }
                    }, 2000);
        } else if (isBloque && idBloqueC.equals(id)) {
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.d(TAG, getClass() + ".run()");
                            ArrayList<Mensaje> mensajes = new ArrayList<>();
                            for (int i = 0; i < 14; i++) {
                                Mensaje m = new Mensaje();
                                if (!id.equals("")) {
                                    m.setClase(Mensaje.GRUPO_IMPORTANTE);
                                } else {
                                    m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                                }
                                m.setContenido("Contenido importantisimo C " + i);
                                m.setDestino(new Id(id));
                                m.setOrigen(new Id(idExtremo + i));
                                m.setFecha(new Date());
                                mensajes.add(m);
                            }

                            vista.resultadoMensajesImportantes(mensajes, idBloqueB, idBloqueC, idBloqueD, true);
                        }
                    }, 2000);
        } else if (isBloque && idBloqueD.equals(id)) {
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.d(TAG, getClass() + ".run()");
                            ArrayList<Mensaje> mensajes = new ArrayList<>();
                            for (int i = 0; i < 14; i++) {
                                Mensaje m = new Mensaje();
                                if (!id.equals("")) {
                                    m.setClase(Mensaje.GRUPO_IMPORTANTE);
                                } else {
                                    m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                                }
                                m.setContenido("Contenido importantisimo D " + i);
                                m.setDestino(new Id(id));
                                m.setOrigen(new Id(idExtremo + i));
                                m.setFecha(new Date());
                                mensajes.add(m);
                            }

                            vista.resultadoMensajesImportantes(mensajes, idBloqueC, idBloqueD, null, true);
                        }
                    }, 2000);
        } else if (!isBloque) {
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.d(TAG, getClass() + ".run()");
                            ArrayList<Mensaje> mensajes = new ArrayList<>();
                            for (int i = 0; i < 14; i++) {
                                Mensaje m = new Mensaje();
                                if (!"".equals(id)) {
                                    m.setClase(Mensaje.GRUPO_IMPORTANTE);
                                } else {
                                    m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                                }
                                m.setContenido("Contenido sin id importantisimo C " + i);
                                m.setDestino(new Id(id));
                                m.setOrigen(new Id(idExtremo + i));
                                m.setFecha(new Date());
                                mensajes.add(m);
                            }

                            vista.resultadoMensajesImportantes(mensajes, idBloqueB, idBloqueC, idBloqueD, true);
                        }
                    }, 2000);
        }

        /*
        *
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, getClass() + ".run()");
                        ArrayList<Mensaje> mensajes = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                            Mensaje m = new Mensaje();
                            if (!id.equals("")) {
                                m.setClase(Mensaje.GRUPO_IMPORTANTE);
                            } else {
                                m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                            }
                            m.setContenido("Contenido importantisimo " + (i + 4));
                            m.setDestino(new Id("IdDeGrupo"));
                            m.setOrigen(new Id("IdDeUsuario" + (i + 4)));
                            m.setFecha(new Date());
                            mensajes.add(m);
                        }
                        vista.resultadoMensajesImportantes(id, mensajes, true, false);
                    }
                }, 4000);

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, getClass() + ".run()");
                        ArrayList<Mensaje> mensajes = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                            Mensaje m = new Mensaje();
                            if (!id.equals("")) {
                                m.setClase(Mensaje.GRUPO_IMPORTANTE);
                            } else {
                                m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                            }
                            m.setContenido("Contenido importantisimo " + (i + 14));
                            m.setDestino(new Id("IdDeGrupo"));
                            m.setOrigen(new Id("IdDeUsuario" + (i + 14)));
                            m.setFecha(new Date());
                            mensajes.add(m);
                        }
                        vista.resultadoMensajesImportantes(id, mensajes, false, true);
                    }
                }, 6000);
* /
        return g;
    }


    public Grupo obtieneMensajesImportantes() {
        Log.d(TAG, getClass() + ".obtieneMensajesImportantes()");

       /* new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, getClass() + ".run()");
                        ArrayList<Mensaje> mensajes = new ArrayList<>();
                        for (int i = 0; i < 4; i++) {
                            Mensaje m = new Mensaje();
                            if (!id.equals("")) {
                                m.setClase(Mensaje.GRUPO_IMPORTANTE);
                            } else {
                                m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                            }
                            m.setContenido("Contenido importantisimo " + i);
                            m.setDestino(new Id("IdDeGrupo"));
                            m.setOrigen(new Id("IdDeUsuario" + i));
                            m.setFecha(new Date());
                            mensajes.add(m);
                        }

                        vista.resultadoMensajesImportantes(id, mensajes, false, false);
                    }
                }, 2000);

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, getClass() + ".run()");
                        ArrayList<Mensaje> mensajes = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                            Mensaje m = new Mensaje();
                            if (!id.equals("")) {
                                m.setClase(Mensaje.GRUPO_IMPORTANTE);
                            } else {
                                m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                            }
                            m.setContenido("Contenido importantisimo " + (i + 4));
                            m.setDestino(new Id("IdDeGrupo"));
                            m.setOrigen(new Id("IdDeUsuario" + (i + 4)));
                            m.setFecha(new Date());
                            mensajes.add(m);
                        }
                        vista.resultadoMensajesImportantes(id, mensajes, true, false);
                    }
                }, 4000);

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, getClass() + ".run()");
                        ArrayList<Mensaje> mensajes = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                            Mensaje m = new Mensaje();
                            if (!id.equals("")) {
                                m.setClase(Mensaje.GRUPO_IMPORTANTE);
                            } else {
                                m.setClase(Mensaje.INDIVIDUAL_IMPORTANTE);
                            }
                            m.setContenido("Contenido importantisimo " + (i + 14));
                            m.setDestino(new Id("IdDeGrupo"));
                            m.setOrigen(new Id("IdDeUsuario" + (i + 14)));
                            m.setFecha(new Date());
                            mensajes.add(m);
                        }
                        vista.resultadoMensajesImportantes(id, mensajes, false, true);
                    }
                }, 6000);
* /
        return null;
    }

    public static String idExtremo = "BD6E848B8FBEDD3E973C390A626723DBAFAE264";
    public static String idGrupo = "E5ACC21E298EA72D75A8D1BBAAAEDE8C584FCFA";
    public static String idLocal = "D33EFFD9E423213F5C2D0CA9D4179B8147CCA48F";

    public void enviaMensaje(int tipo, String contenido, String idDestino, final boolean individual) {
        Mensaje mensaje = new Mensaje(yo.getId(),
                new Id(idDestino), contenido, tipo);

        new android.os.Handler().postDelayed(
                new Runnable() {
                    private Mensaje mensaje;
                    private boolean individual;

                    public void run() {
                        Log.d(TAG, ControladorAndroidImpl.this.getClass() + ".enviaMensaje.run()");
                        Mensaje respuesta;
                        if (individual) {
                            respuesta = new Mensaje(mensaje.getDestino(), mensaje.getOrigen()
                                    , mensaje.getContenido(), mensaje.getClase());
                        } else {
                            respuesta = new Mensaje(new Id(idExtremo + "4"), mensaje.getDestino(),
                                    mensaje.getContenido() + " respuesta", mensaje.getClase());
                        }
                        if (vista != null) {
                            if (individual) {
                                if (vista.isActualId(respuesta.getOrigen().toStringFull())) {
                                    vista.onReceivedMessage(respuesta, "Mensaje automatichi");
                                } else {
                                    notifica(respuesta, "Mensaje automatichi");
                                }
                            } else {
                                if (vista.isActualId(respuesta.getDestino().toStringFull())) {
                                    vista.onReceivedMessage(respuesta, "Grupo BOT");
                                } else {
                                    notifica(respuesta, "Grupo BOT");
                                }
                            }
                        }
                    }

                    Runnable inicializa(Mensaje mensaje, boolean individual) {
                        this.mensaje = mensaje;
                        this.individual = individual;
                        return this;
                    }
                }.inicializa(mensaje, individual), 2000);
    }

    public String obtenerCodigoInvitacion(String id) {
        return generateSessionKey(30);
    }

    public String generateSessionKey(int length) {
        String alphabet =
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int n = alphabet.length(); //10
        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(alphabet.charAt(random.nextInt(n)));
        return sb.toString();

    }

    public void onStop() {
        Log.d(TAG, getClass() + ".onStop()");
    }

    public boolean nuevaDireccionArranque(String ip, int puerto) {
        return true;
    }

    public void conectaAGrupo(String id, String codigo) {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    private String id;

                    public void run() {
                        Log.d(TAG, ControladorAndroidImpl.this.getClass() + ".enviaMensaje.run()");
                        vista.onJoinGroupSuccess(this.id);
                    }

                    public Runnable inicializa(String id) {
                        this.id = id;
                        return this;
                    }
                }.inicializa(id), 3000);
    }

    public void abandonarGrupo(String id) {
        Log.d(TAG, getClass() + ".abandonarGrupo(" + id + ")");
    }

    public void vaciaConversacion(String id) {
        Log.d(TAG, getClass() + ".vaciaConversacion(" + id + ")");
    }

    public void eliminarConversacion(String id) {
        Log.d(TAG, getClass() + ".eliminaConversacion(" + id + ")");
    }

    public void eliminarContacto(String id) {
        Log.d(TAG, getClass() + ".eliminarContacto");
    }
*/

            /**
             * Consulta a la base de datos para obtener los contactos de un usuario
             *
             * @return
             */
            public ArrayList<ContactoAndroid> obtenerContactos() {
                Log.d(TAG, getClass() + ".obtenerContactos()");
                ArrayList<Contacto> contactos = null;
                try {
                    contactos = db.obtenerTodosLosContactosDeUsuario(
                            yo.getNombre(), llavero.getClaveSimetrica(yo.getNombre()));
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }

                ArrayList<ContactoAndroid> candroid;
                if (contactos != null) {
                    candroid = new ArrayList<>(contactos.size());
                    for (Contacto c : contactos) {
                        candroid.add(new ContactoAndroid(c));
                    }
                } else {
                    candroid = null;
                }
                return candroid;
            }

            /**
             * Consulta a la base de datos para obtener las conversaciones abiertas
             *
             * @return
             */
            public ArrayList<ConversacionAndroid> obtenerConversacionesAbiertas() {
                Log.d(TAG, getClass() + ".obtenerConversacionesAbiertas()");
                ArrayList<Conversacion> conversaciones = null;
                String nombre = yo.getNombre();
                Key claveSim = llavero.getClaveSimetrica(nombre);
                try {
                    conversaciones = db.obtenerConversacionesAbiertas(nombre);
                    if (conversaciones != null) {
                        for (Conversacion conv : conversaciones) {
                            ArrayList<Mensaje> mensajes = db.getMensajes(claveSim, nombre,
                                    0, 0, conv.getId().toStringFull());

                            if (mensajes != null && mensajes.size() > 0) {
                                Mensaje mensaje = mensajes.get(0);
                                String texto = "";
                                if (mensaje.getOrigen().equals(yo.getId())) {
                                    texto = "Yo: " + mensaje.getContenido();
                                } else {
                                    Contacto c = db.obtenerContacto(mensaje.getOrigen(), claveSim, nombre);
                                    if (c != null) {
                                        texto = c.getAlias() + ": " + mensaje.getContenido();
                                    } else {
                                        texto = mensaje.getOrigen() + ": " + mensaje.getContenido();
                                    }
                                }
                                conv.setMensaje(texto);
                            }
                        }
                    }
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
                ArrayList<ConversacionAndroid> candroid;
                if (conversaciones != null) {
                    candroid = new ArrayList<>(conversaciones.size());
                    for (Conversacion c : conversaciones) {
                        candroid.add(new ConversacionAndroid(c));
                    }
                } else {
                    candroid = null;
                }
                return candroid;
            }

            /**
             * Consulta a la base de datos para obtener las conversaciones con
             * mensajes pendientes
             *
             * @return
             */
            public ArrayList<String> obtenerConversacionesPendiente() {
                Log.d(TAG, getClass() + ".obtenerConversacionesPendientes()");
                ArrayList<String> lista = null;
                try {
                    lista = db.obtenerConversacionesPendiente();
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
                return lista;
            }

            /**
             * Metodo ejecutado al inicio en el que se actualizan los grupos
             * (para saber si alguien nuevo se unio) y se suscribe a los
             * canales de difusion de pastry en los que estos grupos transmiten
             *
             * @return
             */
            public int subscribirYActualizarGrupos() {
                Log.d(TAG, getClass() + ".subscribirYActualizarGrupos()");
                ArrayList<Grupo> grupos = null;
                try {
                    grupos = db.obtenerTodosLosGruposDeUsuario(
                            yo.getNombre(), llavero.getClaveSimetrica(yo.getNombre()));
                    for (Grupo g : grupos) {
                        mensajero.subscribe(g.getId());
                        if (logger.level <= Logger.INFO)
                            logger.log("Buscando grupo " + g.getId().toStringFull());
                        almacenamiento.lookup(g.getId(), false, new Continuation<PastContent, Exception>() {
                            @Override
                            public void receiveResult(PastContent result) {

                                Log.d(TAG, "subscribirYActualizarGrupos.ContBuscar.Result(" + result + ")");
                                if (result != null) {
                                    if (result instanceof GrupoCifrado) {
                                        GrupoCifrado gcifrado = (GrupoCifrado) result;
                                        if (logger.level <= Logger.INFO)
                                            logger.log("Actualizando grupo " + gcifrado.getId());
                                        try {
                                            Grupo g = gcifrado
                                                    .desencriptar(llavero.getClavePrivada(gcifrado.getId().toStringFull()));
                                            db.actualizaGrupo(g, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                                        } catch (Exception e) {
                                            if (logger.level <= Logger.WARNING)
                                                logger.logException(
                                                        "Recibida excepcion al actualizar el grupo " + gcifrado.getId(), e);

                                            error = Vista.ERROR_ACTUALIZACION_GRUPO;
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            PrintStream ps = new PrintStream(baos);
                                            e.printStackTrace(ps);
                                            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                            excepcion(content);
                                        }
                                    } else if (logger.level <= Logger.INFO)
                                        logger.log("Obtenido algo que no es un grupo" +
                                                " cifrado. Obtenido = " + result);
                                } else {
                                    if (logger.level <= Logger.WARNING)
                                        logger.log("Grupo no encontrado");
                                }

                            }

                            @Override
                            public void receiveException(Exception exception) {
                                if (logger.level <= Logger.WARNING)
                                    logger.logException("Recibida excepcion al actualizar los grupos ", exception);
                            }
                        });
                    }
                } catch (Exception e) {
                    error = Vista.ERROR_SUBSCRIPCION_GRUPO;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
                return error;
            }

            public void actualizaGrupo(String id) {
                almacenamiento.lookup(rice.pastry.Id.build(id), false, new Continuation<PastContent, Exception>() {
                    @Override
                    public void receiveResult(PastContent result) {

                        Log.d(TAG, "actualizarGrupo.ContBuscar.Result(" + result + ")");
                        if (result != null) {
                            if (result instanceof GrupoCifrado) {
                                GrupoCifrado gcifrado = (GrupoCifrado) result;
                                if (logger.level <= Logger.INFO)
                                    logger.log("Actualizando grupo " + gcifrado.getId());
                                try {
                                    Grupo g = gcifrado
                                            .desencriptar(llavero.getClavePrivada(gcifrado.getId().toStringFull()));
                                    db.actualizaGrupo(g, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                                    if (vista != null) {
                                        try {
                                            vista.onGrupoActualizado(new GrupoAndroid(g));
                                        } catch (RemoteException e) {
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            PrintStream ps = new PrintStream(baos);
                                            e.printStackTrace(ps);
                                            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                            excepcion(content);
                                        }
                                    }
                                } catch (Exception e) {
                                    if (logger.level <= Logger.WARNING)
                                        logger.logException(
                                                "Recibida excepcion al actualizar el grupo " + gcifrado.getId(), e);

                                    error = Vista.ERROR_ACTUALIZACION_GRUPO;
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    PrintStream ps = new PrintStream(baos);
                                    e.printStackTrace(ps);
                                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                    excepcion(content);
                                }
                            } else if (logger.level <= Logger.INFO)
                                logger.log("Obtenido algo que no es un grupo" +
                                        " cifrado. Obtenido = " + result);
                        } else {
                            if (logger.level <= Logger.WARNING)
                                logger.log("Grupo no encontrado");
                        }

                    }

                    @Override
                    public void receiveException(Exception exception) {
                        if (logger.level <= Logger.WARNING)
                            logger.logException("Recibida excepcion al actualizar los grupos ", exception);
                    }
                });
            }

            /**
             * Devuelve el modo en el que se encuentra actualmente el controlador
             *
             * @return
             */
            public int getModo() {

                return ControladorAndroidImpl.this.getModo();
            }

            /**
             * Establece un cambio en el modo en el que se encuentra actualmente
             * el controlador
             *
             * @param mode
             */
            public void setModo(int mode) {
                Log.d(TAG, getClass() + ".setModo(" + mode + ")");
                int antiguo = modo;
                if (antiguo == MODO_REGISTRO && mode == MODO_INICIO_SESION) {
                    ControladorAndroidImpl.this.onStop();
                    Log.d(TAG, getClass() + ".run() - setModo(" + MODO_INICIO_SESION + ") y reiniciado");
                    modo = mode;
                    ControladorAndroidImpl.this.onStart();
                }
                modo = mode;
                if (modo == MODO_REGISTRO) {
                    //new Thread(new Runnable() {
                    //@Override
                    //public void run() {
                    Log.d(TAG, getClass() + ".run() - setModo(" + MODO_REGISTRO + ") y reiniciado");
                    ControladorAndroidImpl.this.onStart();
                    //encendido = false;
                    //}
                    //}).start();
                    Log.d(TAG, getClass() + ".setModo() - after run");
                }


            }

            /**
             * Busca en pastry si existe un grupo y devuelve
             *
             * @param idGrupo
             */
            public void buscaGrupo(String idGrupo) {
                Log.d(TAG, getClass() + ".buscaGrupo(" + idGrupo + ")");
                almacenamiento.lookup(rice.pastry.Id.build(idGrupo),
                        new Continuation<PastContent, Exception>() {
                            @Override
                            public void receiveResult(PastContent result) {
                                Log.d(TAG, "buscaGrupo.ContBuscar.Result(" + result + ")");
                                if (result != null) {
                                    if (logger.level <= Logger.INFO) logger.log("Grupo encontrado");
                                } else if (logger.level <= Logger.INFO)
                                    logger.log("El grupo que esta " +
                                            "buscando no existe");

                            }

                            @Override
                            public void receiveException(Exception exception) {
                                if (logger.level <= Logger.WARNING)
                                    logger.logException("Excepcion en App.buscaGrupo." +
                                            "Continuation.receiveException", exception);
                            }

                        });
            }

    /*@Override
    public Usuario buscaUsuario(String usuario)
    {

        while (varBuscaUsuario)
        {
            try
            {
                this.env.getTimeSource().sleep(100);
            } catch (InterruptedException e)
            {
                if (logger.level <= Logger.WARNING) logger.logException("Excepcion en App.buscaUsuario", e);
            }
        }
        return resultado;
    }*/

            /**
             * Inserta en la base de datos un nuevo contacto
             *
             * @param usr
             * @param alias
             */
            public void nuevoContacto(UsuarioAndroid usr, String alias) {
                Log.d(TAG, getClass() + ".nuevoContacto(" + usr + ", " + alias + ")");
                if (usr != null) {
                    Contacto contacto = new Contacto(alias, usr);
                    try {
                        db.insertarContacto(contacto, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                    } catch (Exception e) {
                        if (logger.level <= Logger.INFO)
                            logger.logException("Error al insertar el contacto "
                                    + usr + " en la bd", e);
                    }
                    if (usr.getCertificado() != null) {
                        try {
                            llavero.guardarCertificado(usr.getId().toStringFull(), usr.getCertificado());
                            llavero.guardarLlavero(yo.getNombre());
                        } catch (KeyStoreException e) {
                            if (logger.level <= Logger.INFO)
                                logger.logException("Error al insertar el certificado "
                                        + usr.getId() + " en el llavero", e);
                        }
                    } else {
                        if (logger.level <= Logger.INFO)
                            logger.log("El usuario que se va a insertar no" +
                                    " tiene un certificado");
                    }
                } else {
                    if (logger.level <= Logger.INFO)
                        logger.log("El usuario que esta buscando no existe");
                }
            }

            /**
             * Elimina de la base de datos un contacto existente.
             * Elimina las conversaciones abiertas con el.
             *
             * @param id
             */
            public void eliminaContacto(String id) {
                Log.d(TAG, getClass() + ".eliminaContacto(" + id + ")");
                try {
                    db.borrarContacto(id, yo.getNombre());
                    llavero.guardarCertificado(id, null);
                    llavero.guardarLlavero(yo.getNombre());
                    Conversacion c = db.obtenerConversacionAbierta(id, yo.getNombre());
                    if (c != null) {
                        db.borrarConversacionAbierta(id, yo.getNombre());
                        llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_ENTRANTE, null);
                        llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_SALIENTE, null);
                        llavero.guardarLlavero(yo.getNombre());
                    }

                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
            }

            /**
             * Elimina de la base de datos una conversacion abierta
             *
             * @param id
             */
            public void eliminaConversacion(String id) {
                Log.d(TAG, getClass() + ".eliminaConversacion(" + id + ")");
                Grupo g = null;
                try {
                    g = db.obtenerGrupo(id, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                    if (g != null) {
                        abandonaGrupo(g.getId().toStringFull());
                    } else {
                        db.borrarConversacionAbierta(id, yo.getNombre());
                        db.borraMensajesDeConversacion(yo.getNombre(), id);
                        //llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_ENTRANTE, null);
                        llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_SALIENTE, null);
                        llavero.guardarLlavero(yo.getNombre());
                    }

                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }

            }

            /**
             * Elimina todos los mensajes asociado a una conversacion
             *
             * @param id
             */
            public void vaciaConversacion(String id) {
                Log.d(TAG, getClass() + ".vaciaConversacion(" + id + ")");
                try {
                    db.borraMensajesDeConversacion(yo.getNombre(), id);
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
            }

            /**
             * Crea un nuevo grupo, incluyendo las claves para comunicarse
             * y un id para usar ese canal de difusion de pastry. Se
             * le inserta como lider y primer miembro al usuario que creo el
             * grupo. Cuando se crea el objeto y la claves, se inserta en
             * pastry tanto el objeto grupo como el objeto bloque de mensajes
             * importantes.
             *
             * @param nombreGrupo
             */
            public void crearGrupo(final String nombreGrupo) {
                Log.d(TAG, getClass() + ".crearGrupo(" + nombreGrupo + ")");
                buscaIdDisponible(new Continuation<Id, Exception>() {
                    @Override
                    public void receiveResult(Id result) {
                        Log.d(TAG, "crearGrupo.ContIdDisp.Result(" + result + ")");
                        try {
                            final Grupo nuevoGrupo = gestorUsuariosGrupos.creaGrupo(result, nombreGrupo, yo,
                                    ALGORITMO_CLAVE_ASIMETRICA, TAM_CLAVE_ASIMETRICA,
                                    ALGORITMO_CLAVE_SIMETRICA, ALGORITMO_FIRMA);

                            buscaIdDisponible(new Continuation<Id, Exception>() {
                                @Override
                                public void receiveResult(Id result) {
                                    Log.d(TAG, "crearGrupo.ContIdDisp.Result.ContIdDisp.Result(" + result + ")");
                                    BloqueMensajes bloqueMensajes = new BloqueMensajes(result, nuevoGrupo.getId(), true, null);
                                    nuevoGrupo.setBloqueMensajesImportantes(bloqueMensajes.getId());

                                    final String idGrupo = nuevoGrupo.getId().toStringFull();
                                    GrupoCifrado gc = null;
                                    try {
                                        db.insertarGrupo(nuevoGrupo, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                                    } catch (Exception e) {
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        PrintStream ps = new PrintStream(baos);
                                        e.printStackTrace(ps);
                                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                        excepcion(content);
                                        error = Vista.ERROR_INSERCION_NUEVO_GRUPO_EN_BBDD;
                                    }

                                    try {
                                        db.insertarConversacionAbierta(nuevoGrupo.getId().toStringFull(),
                                                nuevoGrupo.getNombre(),
                                                yo.getNombre(),
                                                Conversacion.TIPO_GRUPO);
                                    } catch (Exception e) {
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        PrintStream ps = new PrintStream(baos);
                                        e.printStackTrace(ps);
                                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                        excepcion(content);
                                        error = Vista.ERROR_INSERCION_CONVERSACION_EN_BBDD;
                                    }


                                    try {
                                        gc = new GrupoCifrado(nuevoGrupo,
                                                (SecretKey) llavero.getClaveSimetrica(nuevoGrupo.getId().toStringFull()),
                                                llavero.getClavePrivada(nuevoGrupo.getId().toStringFull()));
                                    } catch (Exception e) {
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        PrintStream ps = new PrintStream(baos);
                                        e.printStackTrace(ps);
                                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                        excepcion(content);
                                        error = Vista.ERROR_CREACION_GRUPO_CIFRADO;
                                    }


                                    Continuation<Boolean[], Exception> cont = new Continuation<Boolean[], Exception>() {
                                        private boolean insertado = false;
                                        private boolean errorInsercion = false;

                                        @Override
                                        public void receiveResult(Boolean[] result) {
                                            Log.d(TAG, "crearGrupo.ContIdDisp.Result." +
                                                    "ContIdDisp.Result.ContInsert.Result(" + result + ")");
                                            int inserciones = 0;
                                            for (boolean b : result) {
                                                if (b)
                                                    inserciones++;
                                            }
                                            if (inserciones > 0) {
                                                if (logger.level <= Logger.INFO)
                                                    logger.log("Insertado correctamente en " + inserciones + " ubicaciones");
                                                if (insertado && !errorInsercion) {
                                                    if (vista != null) {
                                                        try {
                                                            vista.onCreateGroupSuccess();
                                                        } catch (RemoteException e) {
                                                            Log.d(TAG, "Error al enviar el resultado", e);
                                                        }
                                                    }
                                                } else if (!errorInsercion)
                                                    insertado = true;
                                            } else {
                                                if (logger.level <= Logger.INFO)
                                                    logger.log("No se ha insertado el grupo " + idGrupo);
                                                if (!errorInsercion) {
                                                    if (vista != null) {
                                                        try {
                                                            vista.onCreateGroupFailed();
                                                            errorInsercion = true;
                                                        } catch (RemoteException e) {
                                                            Log.d(TAG, "Error al enviar el resultado", e);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void receiveException(Exception exception) {
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            PrintStream ps = new PrintStream(baos);
                                            exception.printStackTrace(ps);
                                            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                            excepcion(content);
                                            error = Vista.ERROR_INSERCION_NUEVO_GRUPO_EN_PASTRY;
                                            if (!errorInsercion) {
                                                if (vista != null) {
                                                    try {
                                                        vista.onCreateGroupFailed();
                                                    } catch (RemoteException e) {
                                                        Log.d(TAG, "Error al enviar el resultado", e);
                                                    }
                                                }
                                                errorInsercion = true;
                                            }
                                        }
                                    };

                                    almacenamiento.insert(gc, cont);

                                    // TODO: cambiar a level CONFIG
                                    if (logger.level <= Logger.INFO)
                                        logger.log("grupo " + nuevoGrupo.getId().toStringFull() + " creado. Clave privada = " +
                                                Base64.getEncoder().encodeToString(llavero.getClavePrivada(
                                                        nuevoGrupo.getId().toStringFull()).getEncoded()));

                                    almacenamiento.insert(bloqueMensajes, cont);

                                }

                                @Override
                                public void receiveException(Exception exception) {

                                }
                            });

                        } catch (Exception e) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            PrintStream ps = new PrintStream(baos);
                            e.printStackTrace(ps);
                            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                            excepcion(content);
                            error = Vista.ERROR_CREACION_NUEVO_GRUPO;
                        }
                    }

                    @Override
                    public void receiveException(Exception exception) {

                    }
                });
            }

            /**
             * Elimina toda la informacion local referida a un grupo, como
             * claves y mensajes.
             *
             * @param idGrupo
             */
            public void abandonaGrupo(String idGrupo) {
                Log.d(TAG, getClass() + ".abandonaGrupo(" + idGrupo + ")");
                if (logger.level <= Logger.INFO)
                    logger.log("Eliminando grupo, conversacion y claves asociado al grupo " +
                            idGrupo);
                try {
                    Id tmp = rice.pastry.Id.build(idGrupo);
                    enviaMensaje(Mensaje.GRUPO_SALE, yo.getId().toStringFull(), idGrupo, false);
                    mensajero.abandonaGrupo(tmp);
                    db.borrarConversacionAbierta(idGrupo, yo.getNombre());
                    db.borrarGrupo(idGrupo, yo.getNombre());
                    db.setValor(idGrupo + EXTENSION_ULTIMO_BLOQUE, null);
                    db.borraMensajesDeConversacion(yo.getNombre(), idGrupo);
                    llavero.setClaveSimetrica(idGrupo, null);
                    llavero.setEntradaPrivada(idGrupo, null);
                    llavero.guardarLlavero(yo.getNombre());
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
            }

            /**
             * Cuando la vista detiene el controlador
             */
            public void onStop() {
                ControladorAndroidImpl.this.onStop();
            }

            /**
             * Consulta a la base de datos para obtener cierta cantidad
             * de mensajes correspondientes a una conversacion.
             *
             * @param id
             * @param primerMsj
             * @param ultimoMsj
             * @param tipoDeConversacion
             * @return
             */
            public ArrayList<MensajeAndroid> obtieneMensajes(
                    String id, int primerMsj, int ultimoMsj, int tipoDeConversacion) {
                Log.d(TAG, getClass() + ".obtieneMensajes(" + id + ", ini = " + primerMsj + ", fin = " +
                        ultimoMsj + ", tipo = " + tipoDeConversacion + ")");
                ArrayList<Mensaje> mensajes = null;
                try {
                    mensajes = db.getMensajes(llavero.getClaveSimetrica(yo.getNombre()),
                            yo.getNombre(), primerMsj, ultimoMsj, id);
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
                ArrayList<MensajeAndroid> candroid;
                if (mensajes != null) {
                    candroid = new ArrayList<>(mensajes.size());
                    for (Mensaje c : mensajes) {
                        candroid.add(new MensajeAndroid(c));
                    }
                } else {
                    candroid = null;
                }
                return candroid;
            }

            /**
             * La vista le indica cual es la conversacion abierta en este momento para
             * que lance las notificaciones correctamente.
             *
             * @param conv
             */
            public void muestraMensajes(ConversacionAndroid conv) {
                conversacionAbierta = conv;
            }

            /**
             * Llamada de la vista para enviar un mensaje
             *
             * @param tipo
             * @param contenido
             * @param idString
             * @param individual
             */
            public void enviaMensaje(int tipo, String contenido, String idString, boolean individual) {
                Id idDestino = rice.pastry.Id.build(idString);
                ControladorAndroidImpl.this.enviaMensaje(tipo, contenido, idDestino, individual);
            }

            /**
             * Inserta una direccion en la base de datos
             *
             * @param ip
             * @param puerto
             */
            public void actualizaDireccion(InetAddress ip, int puerto) {
                Log.d(TAG, getClass() + ".actualizaDireccion(" + ip + ", " + puerto + ")");
                if (ip != null)
                    miDireccion = ip;
                if (puerto != -1)
                    puertoEscucha = puerto;
            }


            /**
             * Metodo para conectarse a un grupo de pastry. Busca el objeto
             * {@link GrupoCifrado} en pastry y lo desencripta en caso de
             * encontrarlo. Al desencriptarlo se inserta en la lista de los
             * integrantes, firma el grupo con la modificacion y lo devuelve
             * a pastry. Si ocurriera un problema en alguno de los pasos, se
             * reintenta el proceso hasta tres veces.
             *
             * @param id
             * @param privateKeyString
             */
            public void conectaAGrupo(final String id, final String privateKeyString) {
                ControladorAndroidImpl.this.conectaAGrupo(id, privateKeyString);
            }

            /**
             * Sin uso en android
             */
            public void inicializaBBDD() {
            }

            /**
             * Sin uso en android
             */
            public void borraBBDD() {

            }

            /**
             * Guarda en la base de datos la conversacion abierta pasada como
             * parametro
             *
             * @param conversacion
             */
            public void guardarConversacion(ConversacionAndroid conversacion) {
                Log.d(TAG, getClass() + ".guardarConversacion(" + conversacion + ")");
                if (conversacion != null) {
                    try {
                        db.insertarConversacionAbierta(conversacion.getId().toStringFull(),
                                conversacion.getAlias(), yo.getNombre(), conversacion.getTipo());
                    } catch (Exception e) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        e.printStackTrace(ps);
                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        excepcion(content);
                    }
                }
            }

            /**
             * @return error que hubo
             */
            public int getError() {
                return error;
            }

            /**
             * Escribe en memoria persistente el llavero del usuario
             */
            public void guardarLlavero() {
                Log.d(TAG, getClass() + ".guardarLlavero()");
                try {
                    llavero.cerrarLlavero(yo.getNombre());
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                    if (logger.level <= Logger.WARNING)
                        logger.logException("Error al guardar el llavero en el sistema", e);
                }
            }

            /**
             * Consulta a la base de datos que devuelve los alias de las claves guardadas
             *
             * @return
             */
            public ArrayList<String> obtenerClavesGuardadas() {
                Log.d(TAG, getClass() + ".obtenerClavesGuardadas()");
                ArrayList<String> ret = new ArrayList<>();
                try {
                    ret = llavero.obtenerClavesGuardadas();
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
                return ret;
            }

            /**
             * Inserta en la base de datos una nueva ip y un puerto de un
             * nodo pastry que pertenece a la misma red que nosotros
             *
             * @param ip
             * @param puerto
             */
            public void nuevaDireccionArranque(String ip, int puerto) {

                Log.d(TAG, getClass() + ".nuevaDireccionArranque(" + ip + ", " + puerto + ")");
                boolean ret = true;

                try {
                    db.insertarDireccion(InetAddress.getByName(ip), puerto);
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                    ret = false;
                }
                //TODO: cambiar para ejecutarse en android, es leer la clave de usuario
                //        if (gfich.existeFichero(App.NOMBRE_FICHERO_USUARIO))
                if (ret)
                    modo = MODO_INICIO_SESION;

            }

            /**
             * Crea una nueva conversacion con el usuario en el id proporcionado
             *
             * @param id
             * @return
             */
            public boolean iniciarConversacion(String id) {
                Log.d(TAG, getClass() + ".iniciarConversacion(" + id + ")");
                boolean ret = false;

                Key claveSimetrica = llavero.getClaveSimetrica(yo.getNombre());
                Contacto contacto = null;
                try {
                    contacto = db.obtenerContacto(id, claveSimetrica, yo.getNombre());
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }
                if (contacto != null) {
                    String alias = contacto.getAlias();

                    try {
                        db.insertarConversacionAbierta(id, alias, yo.getNombre(), Conversacion.TIPO_INDIVIDUAL);
                        ret = true;
                    } catch (Exception e) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        e.printStackTrace(ps);
                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        excepcion(content);
                    }
                }


                return ret;
            }

            /**
             * Consulta a la base de datos para obtener el contacto asociado
             * a un id concreto pasado como parametro
             *
             * @param id
             * @return
             */
            public ContactoAndroid obtenerContacto(String id) {

                return new ContactoAndroid(ControladorAndroidImpl.this.obtenerContacto(id));
            }

            /**
             * Guarda en la base de datos que se debe hacer cuando se recibe
             * una solicitud de eco
             *
             * @param allow
             */
            public void setPingPolicy(boolean allow) {
                Log.d(TAG, getClass() + ".setPingPolicy(" + allow + ")");
                try {
                    db.setValor("responder_ping", Boolean.toString(allow));
                } catch (Exception e) {
                    if (logger.level <= Logger.WARNING)
                        logger.logException("Error al insertar valor", e);
                }
            }

            /**
             * Obtiene los mensajesi importantes asociados al id proporcionado
             *
             * @param idString
             */
            public void mostrarMensajesImportantes(String idString) {
                Log.d(TAG, getClass() + ".mostrarMensajesImportantes(" + idString + ")");
                Id id = rice.pastry.Id.build(idString);
                almacenamiento.lookup(id, new Continuation<PastContent, Exception>() {
                    @Override
                    public void receiveResult(PastContent result) {
                        Log.d(TAG, "mostrarMensajesImportantes.ContBuscar.Result(" + result + ")");
                        if (result != null) {
                            if (result instanceof BloqueMensajes) {
                                BloqueMensajes bloque = (BloqueMensajes) result;
                                if (logger.level <= Logger.INFO) {
                                    // TODO entregar a la capa superior los mensajes decodificados
                                    // TODO de este bloque, pasarle los 3 ids que viajan en el mismo
                                    logger.log(bloque.toString());
                                }
                                if (bloque.getSiguienteBloque() != null)
                                    almacenamiento.lookup(bloque.getSiguienteBloque(), this);
                            } else if (result instanceof GrupoCifrado) {
                                GrupoCifrado grupo = (GrupoCifrado) result;
                                Key clave = llavero.getClavePrivada(grupo.getId().toStringFull());
                                if (clave != null) {
                                    Grupo g = null;
                                    try {
                                        g = grupo.desencriptar(clave);
                                        almacenamiento.lookup(g.getBloqueMensajesImportantes(), this);
                                    } catch (Exception e) {
                                        if (logger.level <= Logger.INFO)
                                            logger.logException("Error al desencriptar grupo", e);
                                    }
                                }
                            } else if (result instanceof Usuario) {
                                Usuario usuario = ((Usuario) result);
                                almacenamiento.lookup(usuario.getBloqueMensajesImportantes(), this);
                            }
                        }
                    }

                    @Override
                    public void receiveException(Exception exception) {

                    }
                });
            }

            /**
             * Imprime un mensaje de log con los id de los bloques de los
             * mensajes importantes que hay en cache.
             */
            public void imprimeIdEnCache() {
                Log.d(TAG, getClass() + ".imprimeidEnCache()");
                Map<String, String> valores = null;
                try {
                    valores = db.getTodosLosValores(yo.getNombre());
                    Set<String> keys = valores.keySet();
                    for (String key : keys) {
                        Log.d(TAG, "Value of '" + key + "' : '" + valores.get(key) + "'");
                    }
                } catch (Exception e) {
                    if (logger.level <= Logger.WARNING)
                        logger.logException("Error al leer todos los valores", e);
                }
            }

            /**
             * Obtiene los mensajes importantes que hay en el bloque pasado
             * como parametro. Si lo que se ha pasado es un id de grupo,
             * este se busca en pastry, se toma el id del primer bloque
             * y se muestran esos mensajes importantes.
             *
             * @param id
             * @param isBloque
             * @return
             */
            public GrupoAndroid obtieneMensajesImportantes(String id, boolean isBloque) {
                Log.d(TAG, getClass() + ".obtieneMensajesImportantes(" + id + ", idBloque = " + isBloque + ")");
                Grupo grupo = null;
                Id idBusqueda = null;

                if (id != null) {
                    if (!isBloque) {
                        try {
                            String idCache = db.getValor(id + EXTENSION_ULTIMO_BLOQUE);
                            grupo = db.obtenerGrupo(id, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                            if (!"".equals(idCache)) {
                                idBusqueda = rice.pastry.Id.build(idCache);
                            } else if (grupo != null) {
                                idBusqueda = grupo.getBloqueMensajesImportantes();
                            }
                        } catch (Exception e) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            PrintStream ps = new PrintStream(baos);
                            e.printStackTrace(ps);
                            String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                            excepcion(content);
                        }
                    } else {
                        idBusqueda = rice.pastry.Id.build(id);
                    }
                } else {
                    idBusqueda = yo.getBloqueMensajesImportantes();
                }

                if (idBusqueda != null) {
                    almacenamiento.lookup(idBusqueda, new Continuation<PastContent, Exception>() {

                        @Override
                        public void receiveResult(PastContent pastContent) {
                            ArrayList<Mensaje> mensajes = null;
                            if (pastContent != null && pastContent instanceof BloqueMensajes) {
                                BloqueMensajes bloque = (BloqueMensajes) pastContent;
                                Id destinatario = bloque.getDestinatario();

                                Queue<EntradaMensaje> entradas = bloque.getMensajes();
                                mensajes = new ArrayList<>();

                                Key clavePrivada;
                                if (destinatario.equals(yo.getId()))
                                    clavePrivada = llavero.getClavePrivada(yo.getNombre());
                                else
                                    clavePrivada = llavero.getClavePrivada(destinatario.toStringFull());

                                if (clavePrivada != null) {
                                    for (EntradaMensaje entrada : entradas) {
                                        String claveSimCifrada = entrada.getClaveSimetricaCifrada();
                                        try {
                                            Key claveSimetrica = ManejadorClaves.desencriptaClaveSimetrica(claveSimCifrada,
                                                    clavePrivada, ALGORITMO_CLAVE_SIMETRICA);
                                            Mensaje mensaje = entrada.getMensaje().desencripta(claveSimetrica);
                                            mensajes.add(mensaje);

                                        } catch (Exception e) {
                                            mensajes.add(new Mensaje(entrada.getRemitente(),
                                                    bloque.getDestinatario(), "Error en el mensaje",
                                                    Mensaje.GRUPO_IMPORTANTE));
                                        }
                                    }
                                }

                                if (vista != null) {
                                    try {
                                        String ant = null;
                                        if (bloque.getAnteriorBloque() != null)
                                            ant = bloque.getAnteriorBloque().toStringFull();
                                        String sig = null;
                                        if (bloque.getSiguienteBloque() != null)
                                            sig = bloque.getSiguienteBloque().toStringFull();

                                        ArrayList<MensajeAndroid> msj;
                                        if(mensajes!=null)
                                        {
                                            msj=new ArrayList<MensajeAndroid>(mensajes.size());
                                            for(Mensaje m : mensajes)
                                            {
                                                msj.add(new MensajeAndroid(m));
                                            }
                                        } else
                                        {
                                            msj = null;
                                        }
                                        vista.resultadoMensajesImportantes(msj,
                                                ant, bloque.getId().toStringFull(), sig,
                                                bloque.isGrupo());
                                    } catch (RemoteException e) {
                                        Log.d(TAG, "Error al enviar el resultado", e);
                                    }
                                }
                            }
                        }

                        @Override
                        public void receiveException(Exception e) {

                        }
                    });
                }


                return new GrupoAndroid(grupo);
            }

            /**
             * Devuelve el usuario que esta en este momento iniciado en el nodo
             *
             * @return
             */
            @Override
            public UsuarioAndroid obtenerUsuario() {
                Log.d(TAG, getClass() + ".obtenerUsuario() = " + yo);
                return new UsuarioAndroid(yo);
            }

            /**
             * Devuelve el codigo de invitacion para el grupo pasado como parametro.
             * Si para ese grupo no existe codigo, se genera.
             *
             * @param id
             * @return
             */
            public String obtenerCodigoInvitacion(String id) {
                Log.d(TAG, getClass() + ".obtenerCodigoInvitacion(" + id + ")");
                String codigo = "";

                try {
                    codigo = db.getValor(id + EXTENSION_CODIGO);
                    if ("".equals(codigo)) {
                        codigo = gestorUsuariosGrupos.generateSessionKey(LONGITUD_CODIGO_UNIRSE_GRUPO);
                        db.setValor(id + EXTENSION_CODIGO, codigo);
                        Calendar cal = Calendar.getInstance(); // creates calendar
                        cal.setTime(new Date()); // sets calendar time/date
                        cal.add(Calendar.HOUR_OF_DAY, 1); // adds one hour
                        Date fecha = cal.getTime(); // returns new date object, one hour in the future
                        db.setValor(id + EXTENSION_CADUCIDAD_CODIGO, String.valueOf(fecha.getTime()));
                    }
                } catch (Exception e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    e.printStackTrace(ps);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    excepcion(content);
                }

                return codigo;
            }

            /**
             * Llamado por la vista para unirse a un grupo partiendo del
             * id de la persona que invita y el codigo de invitacion.
             * Envia un mensaje de tipo peticion para unirse a grupo.
             *
             * @param id
             * @param codigo
             */
            public void enviarPeticionUnirAGrupo(String id, String codigo) {
                Log.d(TAG, getClass() + ".enviarPeticionUnirAGrupo(" + id + ", " + codigo + ")");
                if (id != null && id.length() == yo.getId().toStringFull().length()) {
                    if (codigo != null && codigo.length() == LONGITUD_CODIGO_UNIRSE_GRUPO) {
                        enviaMensaje(Mensaje.INDIVIDUAL_PETICION_UNIR_GRUPO, codigo, id, true);
                    }
                }
            }

            /**
             * Envia un mensaje para comprobar si el otro extremo esta
             * conectado o no.
             *
             * @param objetivo
             * @param timeout  tiempo entre cada mensaje de peticion de eco
             */
            public void ping(String objetivo, long timeout) {
                Log.d(TAG, getClass() + ".ping(" + objetivo + ", " + timeout + ")");
                Id destino = null;
                if (objetivo.length() == yo.getId().toStringFull().length())
                    destino = rice.pastry.Id.build(objetivo);
                else
                    destino = pastryIdFactory.buildId(objetivo);

                String id = gestorUsuariosGrupos.generateSessionKey(50);

                String carga = "{\"id\": \"" + id + "\"}";

                Map<String, PingWithTimeout> map = observadoresPing.get(destino);

                if (map == null)
                    map = new HashMap<>();

                map.put(id, new PingWithTimeout(observer, timeout, destino, carga));

                observadoresPing.put(destino, map);

                mensajero.ping(destino, observer, carga);
            }

            /**
             * Observador para los mensajes de ping. Cuando se termina de recibir
             * el eco, se descarta este observador.
             */
            ObservadorPing observer = new ObservadorPing() {
                public void notificarPing(int evento, Id objetivo, String carga) {

                    Log.d(TAG, getClass() + ".notificarPing(" + evento + ", " + objetivo + ", " + carga + ")");

                    String id=null;

                    try {
                        JSONObject reader = new JSONObject(carga);
                        id = reader.getString("id");
                    } catch (JSONException e) {
                        Log.d(TAG, "Error al procesar la informacion", e);
                    }

                    Map<String, PingWithTimeout> map = observadoresPing.get(objetivo);
                    String respuesta = "";

                    if (map != null && id!=null) {
                        PingWithTimeout observador = map.remove(id);
                        if (observador != null) {
                            long tiempo = new Date().getTime() - observador.getInicio().getTime();
                            //64 bytes from 8.8.8.8: icmp_seq=0 ttl=57 time=11.565 ms

                            switch (evento) {
                                case Mensaje.ECHO_REPLY:
                                    respuesta = "From " + objetivo + ": time=" + tiempo + " ms";
                                    break;
                                case Mensaje.PING_TIMEOUT:
                                    respuesta = "Timeout for node " + objetivo;
                                    break;
                                case Mensaje.HOST_UNREACHABLE:
                                    respuesta = "Host " + objetivo + " unreachable.";
                                    break;

                                default:
                                    break;
                            }
                        }
                        if (map.size() == 0) {
                            mensajero.cancelarPing(objetivo);
                            observadoresPing.remove(objetivo);
                        }

                        if (vista != null) {
                            try {
                                vista.notificarPing(respuesta);
                            } catch (RemoteException e) {
                                Log.d(TAG, "Error al enviar el resultado", e);
                            }
                        }
                    }
                }
            };
        };
    }
}
