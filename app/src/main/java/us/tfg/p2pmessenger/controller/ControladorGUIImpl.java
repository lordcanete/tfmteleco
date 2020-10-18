package us.tfg.p2pmessenger.controller;

import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;
import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.identity.BindStrategy;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;
import org.mpisws.p2p.transport.sourceroute.SourceRoute;
import org.mpisws.p2p.transport.sourceroute.factory.MultiAddressSourceRouteFactory;
import org.mpisws.p2p.transport.ssl.SSLTransportLayer;
import org.mpisws.p2p.transport.ssl.SSLTransportLayerImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
import rice.persistence.PersistentStorageConsole;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;
import rice.pastry.NodeHandle;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.TransportLayerNodeHandle;
import us.tfg.p2pmessenger.model.BloqueMensajes;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.EntradaMensaje;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.GrupoCifrado;
import us.tfg.p2pmessenger.model.ManejadorBBDDConsola;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.MensajeCifrado;
import us.tfg.p2pmessenger.model.Usuario;
import us.tfg.p2pmessenger.util.Base64;
import us.tfg.p2pmessenger.view.Vista;
import us.tfg.p2pmessenger.view.VistaConsolaPublic;
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
 */
public class ControladorGUIImpl implements ControladorApp
{


    // Para almacenar las propiedades de pastry
    private Environment env;

    // Gestor de logs
    private Logger logger;

    // Para generar los ids de los nodos de forma aleatoria
    private NodeIdFactory nodeIdFactory;

    // Para la fabrica de sockets
    private PastryNodeFactory factory;

    // Para la creacion de los id de los objetos almacenados en "almacenamiento"
    private PastryIdFactory pastryIdFactory;

    // El nodo que forma parte del anillo DHT
    private PastryNode node;

    // Objeto que realizara las labores de chat de la aplicacion
    private Mensajero mensajero;

    // Objeto que realizara las labores de persistencia de objetos
    private Past almacenamiento;

    // Si es necesario, incluir otro objeto para gestionar la entrada
    // y salida de ficheros
    //private GestorFicherosConsola gestorFicheros;

    // Objeto para el acceso a la base de datos
    private ManejadorBBDDConsola db;

    // Variable para sincronizar la llamada a buscaUsuario
    private boolean varBuscaUsuario;

    private Conversacion conversacionAbierta;
    private VistaConsolaPublic vista;
    private InetAddress miDireccion;
    private int puertoEscucha;
    private String ipEscucha;
    private Usuario yo;

    private GestorUsuariosGrupos gestorUsuariosGrupos;

    private Llavero llavero;

    private int modo;

    private Map<Id, Map<String, PingWithTimeout>> observadoresPing;

    private int error;
    private KeyStore sslKeyStore;    

    public ControladorGUIImpl(String ipEscucha, int puertoEscucha, VistaConsolaPublic vista, File keystore) throws Exception
    {
        // hack for JCE Unlimited Strength
        Field field = null;
        try
        {
            field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, false);
        } catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        this.puertoEscucha = puertoEscucha;
        this.ipEscucha = ipEscucha;
        this.observadoresPing = new HashMap<>();
        this.vista = vista;
        if (keystore != null){
            // create the keystore    
            this.sslKeyStore = KeyStore.getInstance("PKCS12");
            this.sslKeyStore.load(new FileInputStream(keystore), "".toCharArray());        
        }else{
            this.sslKeyStore = null;
        }        
    }

    @Override
    public void onCreateEntorno()
    {

        try
        {
            env = new Environment();
            logger = env.getLogManager().getLogger(getClass(), "");

            if (logger.level <= Logger.INFO) logger.log("Creando aplicacion");

            // genera los ids de nodo de forma aleatoria
            nodeIdFactory = new RandomNodeIdFactory(env);

            InetAddress ia_ipEscucha;
            // crea la fabrica de nodos            
            if (this.ipEscucha == null) {
                ia_ipEscucha = InetAddress.getLocalHost();                
            } else {
                ia_ipEscucha = InetAddress.getByName(ipEscucha);                
            }
            if(this.sslKeyStore != null){
                factory = new SocketPastryNodeFactory(nodeIdFactory, ia_ipEscucha, puertoEscucha, env){
                    @Override
                    protected TransportLayer<SourceRoute<MultiInetSocketAddress>, ByteBuffer> getSourceRouteTransportLayer(
                        TransportLayer<MultiInetSocketAddress, ByteBuffer> etl, 
                        PastryNode pn, 
                        MultiAddressSourceRouteFactory esrFactory) {
              
                      // get the default layer by calling super
                      TransportLayer<SourceRoute<MultiInetSocketAddress>, ByteBuffer> sourceRoutingTransportLayer =
                              super.getSourceRouteTransportLayer(etl, pn, esrFactory);
                      
                      try {
                        // return our layer
                        return new SSLTransportLayerImpl<SourceRoute<MultiInetSocketAddress>, ByteBuffer>
                                (sourceRoutingTransportLayer,sslKeyStore,sslKeyStore,pn.getEnvironment());
                      } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                      }
                    }
              
                  };
            }else{
                factory = new SocketPastryNodeFactory(nodeIdFactory, ia_ipEscucha, puertoEscucha, env);
            }
            
            // inicializacion de la fabrica de ids para los objetos almacenados
            pastryIdFactory = new PastryIdFactory(env);

            db = new ManejadorBBDDConsola(env.getLogManager().getLogger(
                    ManejadorBBDDConsola.class, "us.tfg.domferpir.ManejadorBBDDConsola"));

            db.inicializarBD();

            modo = MODO_APAGADO;

            gestorUsuariosGrupos = new GestorUsuariosGrupos(
                    env.getLogManager().getLogger(
                            GestorUsuariosGrupos.class, "GestorUsuariosGrupos"), this);
        } catch (Exception e)
        {
            if (logger.level <= Logger.SEVERE) logger.logException("Error onCreateEntorno()", e);
        }
    }

    @Override
    public void onStart()
    {
        // puede no requerir permisos porque se guardara en el almacenamiento interno. Quizas
        // sea necesario proporcionarle el fichero tal cual y no una ruta en un string.

        int error = 0;
        ArrayList<InetSocketAddress> direccionArranque = null;


        int mode = getModo();
        if (mode == MODO_APAGADO || mode == MODO_NECESARIA_DIRECION)
        {            
            try
            {
                direccionArranque = db.getDireccionesDeArranque();
            } catch (Exception e)
            {
                error = Vista.ERROR_BASE_DE_DATOS;
                vista.excepcion(e);
                //                if(logger.level<=Logger.CONFIG) logger.logException("Error en BBDD",e);
            }
            if (direccionArranque != null && direccionArranque.size() == 0)
            {
                mode = MODO_NECESARIA_DIRECION;
            } else
                mode = MODO_INICIO_SESION;
        }
        this.modo = mode;

        if (mode == MODO_INICIO_SESION)
        {            
            GestorFicherosConsola gfich = new GestorFicherosConsola();
            if (gfich.existeFichero(ControladorGUIImpl.NOMBRE_FICHERO_USUARIO))
            {
                String usuario = "";
                String contr = "";
                try
                {
                    GestorFicherosConsola gestorFicheros = new GestorFicherosConsola();

                    byte[] buffer = gestorFicheros.leeDeFichero(ControladorGUIImpl.NOMBRE_FICHERO_USUARIO);
                    JsonReader jsonReader = Json
                            .createReader(new ByteArrayInputStream(buffer));
                    JsonObject objeto = jsonReader.readObject();
                    usuario = objeto.getString("usuario");
                    contr = objeto.getString("contrasena");
                } catch (Exception e)
                {
                    error = Vista.ERROR_LEER_USU_PASS;
                    vista.excepcion(e);
                    //                    if(logger.level<=Logger.CONFIG) logger.logException("Error al leer usuario y contraseña",e);
                }

                if (error == 0)
                {
                    try
                    {
                        this.llavero = new ControladorLlaveroConsola(usuario, contr);
                    } catch (Exception e)
                    {
                        error = Vista.ERROR_INICIAR_LLAVERO;
                        vista.excepcion(e);
                        if (logger.level <= Logger.CONFIG) logger.logException("Error al iniciar el llavero", e);
                        gfich.eliminaFichero(ControladorGUIImpl.NOMBRE_FICHERO_USUARIO);
                    }
                }

                if (error == 0)
                {
                    try
                    {
                        this.yo = gestorUsuariosGrupos.cargaUsuario(usuario);
                    } catch (Exception e)
                    {
                        error = Vista.ERROR_CARGAR_USUARIO;
                        vista.excepcion(e);
                        if (logger.level <= Logger.CONFIG) logger.logException("Error al cargar el usuario", e);
                    }
                }

                if (error == 0)
                {
                    mode = MODO_SESION_INICIADA;
                }
            } else
            {
                mode = MODO_INICIO_SESION;
            }
        }
        this.modo = mode;

        if (mode == MODO_REGISTRO || mode == MODO_SESION_INICIADA)
        {            
            try
            {
                direccionArranque = db.getDireccionesDeArranque();
            } catch (Exception e)
            {
                error = Vista.ERROR_BASE_DE_DATOS;
                vista.excepcion(e);
                //                if(logger.level<=Logger.WARNING) logger.logException("Error al leer la BBDD",e);
            }
            if (direccionArranque != null && direccionArranque.size() > 0)
            {
                error = arrancaNodo(direccionArranque);
            }
            if (error == Vista.ERROR_BOOT_NODO)
            {
                onStop();
                this.modo = MODO_NECESARIA_DIRECION;
                try
                {
                    db.vaciarDirecciones();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                if (logger.level <= Logger.WARNING) logger.log("No se puede unir el nodo a la red");
            }
        }
        this.error = error;
    }

    @Override
    public void guardaGrupo(Grupo grupo, Key clave, String autor)
    {
        try
        {
            this.db.insertarGrupo(grupo, clave, autor);
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
    }

    @Override
    public Grupo obtenerGrupo(String id)
    {
        Grupo ret = null;
        try
        {
            ret = this.db.obtenerGrupo(id, llavero.getClaveSimetrica(id), yo.getNombre());
        } catch (Exception e)
        {
            vista.excepcion(e);
        }


        return ret;
    }

    @Override
    public ArrayList<Grupo> obtenerGrupos()
    {
        ArrayList<Grupo> grupos = null;
        try
        {
            grupos = db.obtenerTodosLosGruposDeUsuario(this.yo.getNombre(), llavero.getClaveSimetrica(yo.getNombre()));
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        return grupos;
    }

    @Override
    public ArrayList<Contacto> obtenerContactos()
    {
        ArrayList<Contacto> contactos = null;
        try
        {
            contactos = db
                    .obtenerTodosLosContactosDeUsuario(this.yo.getNombre(), llavero.getClaveSimetrica(yo.getNombre()));
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        return contactos;
    }

    @Override
    public ArrayList<Conversacion> obtenerConversacionesAbiertas()
    {
        ArrayList<Conversacion> conversaciones = null;
        String nombre = yo.getNombre();
        Key claveSim = llavero.getClaveSimetrica(nombre);
        try
        {
            conversaciones = db.obtenerConversacionesAbiertas(nombre);
            if (conversaciones != null)
            {
                for (Conversacion conv : conversaciones)
                {
                    ArrayList<Mensaje> mensajes = db.getMensajes(claveSim, nombre,
                            0, 0, conv.getId().toStringFull());

                    if (mensajes != null && mensajes.size() > 0)
                    {
                        Mensaje mensaje = mensajes.get(0);
                        String texto = "";
                        if (mensaje.getOrigen().equals(yo.getId()))
                        {
                            texto = "Yo: " + mensaje.getContenido();
                        } else
                        {
                            Contacto c = db.obtenerContacto(mensaje.getOrigen(), claveSim, nombre);
                            if (c != null)
                            {
                                texto = c.getAlias() + ": " + mensaje.getContenido();
                            } else
                            {
                                texto = mensaje.getOrigen() + ": " + mensaje.getContenido();
                            }
                        }
                        conv.setMensaje(texto);
                    }
                }
            }
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        return conversaciones;
    }

    @Override
    public ArrayList<String> obtenerConversacionesPendiente()
    {
        ArrayList<String> lista = null;
        try
        {
            lista = db.obtenerConversacionesPendiente();
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        return lista;
    }

    private int arrancaNodo(ArrayList<InetSocketAddress> direccionArranque)
    {
        int error = 0;


        String directorioAlmacenamiento = "objetosPastry";

        if (modo == MODO_SESION_INICIADA)
        {
            // Nombre final del directorio "almacenamiento/nombreDeUsuario/"
            try
            {
                this.node = factory.newNode((rice.pastry.Id) yo.getId());
            } catch (Exception e)
            {
                error = Vista.ERROR_CREAR_NODO;
                vista.excepcion(e);
                if (logger.level <= Logger.CONFIG) logger.logException("Error al crear el nodo", e);
            }
        } else
        {
            if (logger.level <= Logger.CONFIG) logger.log("No se ha proporcionado ningun nombre de usuario." +
                    " Utilizando uno aleatorio.");
            try
            {
                this.node = factory.newNode();
            } catch (Exception e)
            {
                error = Vista.ERROR_CREAR_NODO;
                vista.excepcion(e);
                if (logger.level <= Logger.CONFIG) logger.logException("Error al crear el nodo", e);
            }
        }

        try
        {
            // Creamos la parte persistente, aqui se guardaran los objetos en memoria permanente
            Storage almacen = new PersistentStorageConsole(pastryIdFactory,
                    directorioAlmacenamiento, 10 * 1024 * 1024, node.getEnvironment());

            // Creamos el objeto que maneja el almacenamiento
            this.almacenamiento = new GCPastImpl(this.node, new StorageManagerImpl(pastryIdFactory, almacen,
                    new LRUCache(new MemoryStorage(pastryIdFactory), 512 * 1024, node.getEnvironment())),
                    10, "us.es.tfg.domferpir.almacenamiento", new PastPolicy.DefaultPastPolicy(), 10);
            //            ((GCPastImpl)this.almacenamiento).

        } catch (Exception e)
        {
            vista.excepcion(e);
            error = Vista.ERROR_CREAR_ALMACENAMIENTO;
            if (logger.level <= Logger.CONFIG) logger.logException("Error al crear el almacenamiento", e);

        }

        //al programar el cliente scribe, instanciarlo y guardarlo en la variable
        if (modo == MODO_SESION_INICIADA)
            this.mensajero = new MensajeroImpl(this.node, this);

        this.node.boot(direccionArranque);

        try
        {
            synchronized (node)
            {
                while (!node.isReady() && !node.joinFailed())
                {
                    node.wait(500);
                    if (node.joinFailed())
                    {
                        if (logger.level <= Logger.SEVERE) logger.log("No se pudo unir a la red. Motivo: "
                                + node.joinFailedReason());
                        error = Vista.ERROR_BOOT_NODO;
                    }
                }
            }

            if (error == 0 && modo == MODO_SESION_INICIADA)
            {
                error = subscribirYActualizarGrupos();
            }
        } catch (Exception e)
        {
            error = Vista.ERROR_BOOT_NODO;
            vista.excepcion(e);
            if (logger.level <= Logger.CONFIG) logger.logException("Error al iniciar el nodo", e);

        }
        return error;

    }

    @Override
    public int subscribirYActualizarGrupos()
    {
        ArrayList<Grupo> grupos = null;
        try
        {
            grupos = db.obtenerTodosLosGruposDeUsuario(
                    yo.getNombre(), llavero.getClaveSimetrica(yo.getNombre())
            );
            for (Grupo g : grupos)
            {
                mensajero.subscribe(g.getId());
                if (logger.level <= Logger.INFO) logger.log("Buscando grupo " + g.getId().toStringFull());
                almacenamiento.lookup(g.getId(), false, new Continuation<PastContent, Exception>()
                {
                    @Override
                    public void receiveResult(PastContent result)
                    {
                        if (result != null)
                        {
                            if (result instanceof GrupoCifrado)
                            {
                                GrupoCifrado gcifrado = (GrupoCifrado) result;
                                if (logger.level <= Logger.INFO) logger.log("Actualizando grupo " + gcifrado.getId());
                                try
                                {
                                    Grupo g = gcifrado
                                            .desencriptar(llavero.getClavePrivada(gcifrado.getId().toStringFull()));
                                    db.actualizaGrupo(g, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                                } catch (Exception e)
                                {
                                    if (logger.level <= Logger.WARNING)
                                        logger.logException(
                                                "Recibida excepcion al actualizar el grupo " + gcifrado.getId(), e);

                                    error = Vista.ERROR_ACTUALIZACION_GRUPO;
                                    vista.excepcion(e);
                                }
                            } else if (logger.level <= Logger.INFO) logger.log("Obtenido algo que no es un grupo" +
                                    " cifrado. Obtenido = " + result);
                        } else
                        {
                            if (logger.level <= Logger.WARNING)
                                logger.log("Grupo no encontrado");
                        }

                    }

                    @Override
                    public void receiveException(Exception exception)
                    {
                        if (logger.level <= Logger.WARNING)
                            logger.logException("Recibida excepcion al actualizar los grupos ", exception);
                    }
                });
            }
        } catch (Exception e)
        {
            error = Vista.ERROR_SUBSCRIPCION_GRUPO;
            vista.excepcion(e);
        }
        return error;
    }

    @Override
    public int getModo()
    {
        return this.modo;
    }

    @Override
    public Environment getEnv()
    {
        return this.env;
    }

    @Override
    public void setModo(int mode)
    {
        this.modo = mode;
        if (this.modo == MODO_REGISTRO)
        {
            onStart();
        }
    }

    /**
     * Busca en DHT si existe un usuario y devuelve
     *
     * @param idGrupo
     */
    @Override
    public void buscaGrupo(String idGrupo)
    {
        this.almacenamiento.lookup(rice.pastry.Id.build(idGrupo),
                new Continuation<PastContent, Exception>()
                {
                    @Override
                    public void receiveResult(PastContent result)
                    {
                        if (result != null)
                        {
                            if (logger.level <= Logger.INFO) logger.log("Grupo encontrado");
                        } else if (logger.level <= Logger.INFO) logger.log("El grupo que esta " +
                                "buscando no existe");

                    }

                    @Override
                    public void receiveException(Exception exception)
                    {
                        if (logger.level <= Logger.WARNING) logger.logException("Excepcion en App.buscaGrupo." +
                                "Continuation.receiveException", exception);
                    }

                });
    }

    @Override
    public Usuario buscaUsuario(String usuario)
    {
        varBuscaUsuario = true;
        Usuario resultado = new Usuario();

        this.almacenamiento.lookup(this.pastryIdFactory.buildId(usuario),
                new Continuation<PastContent, Exception>()
                {
                    Usuario resultado;

                    Continuation<PastContent, Exception> inicializa(Usuario resultado)
                    {
                        this.resultado = resultado;
                        return this;
                    }

                    @Override
                    public void receiveResult(PastContent result)
                    {
                        varBuscaUsuario = false;
                        if (result != null)
                        {
                            this.resultado.rellenaCampos((Usuario) result);
                        }
                    }

                    @Override
                    public void receiveException(Exception exception)
                    {
                        varBuscaUsuario = false;
                        if (logger.level <= Logger.WARNING) logger.logException("Excepcion en App.buscaUsuario." +
                                "Continuation.receiveException", exception);
                    }

                }.inicializa(resultado));
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
    }

    @Override
    public void nuevoContacto(String nombreUsuario, String alias)
    {
        if (!buscaUsuario(nombreUsuario).estaVacio())
        {
            Usuario usr = buscaUsuario(nombreUsuario);
            Contacto contacto = new Contacto(alias, usr);
            try
            {
                db.insertarContacto(contacto, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
            } catch (Exception e)
            {
                if (logger.level <= Logger.INFO)
                    logger.logException("Error al insertar el contacto "
                            + nombreUsuario + " en la bd", e);
            }
            if (usr.getCertificado() != null)
            {
                try
                {
                    llavero.guardarCertificado(usr.getId().toStringFull(), usr.getCertificado());
                } catch (KeyStoreException e)
                {
                    if (logger.level <= Logger.INFO)
                        logger.logException("Error al insertar el certificado "
                                + usr.getId() + " en el llavero", e);
                }
            }
        } else
        {
            if (logger.level <= Logger.INFO)
                logger.log("El usuario que esta buscando no existe");
        }
    }

    @Override
    public void eliminaContacto(String id)
    {
        try
        {
            db.borrarContacto(id, yo.getNombre());
            llavero.guardarCertificado(id, null);
            Conversacion c = db.obtenerConversacionAbierta(id, yo.getNombre());
            if (c != null)
            {
                db.borrarConversacionAbierta(id, yo.getNombre());
                llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_ENTRANTE, null);
                llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_SALIENTE, null);
            }

        } catch (Exception e)
        {
            vista.excepcion(e);
        }
    }

    @Override
    public void eliminaConversacion(String id)
    {
        Grupo g = null;
        try
        {
            g = db.obtenerGrupo(id, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
            if (g != null)
            {
                abandonaGrupo(g.getId().toStringFull());
            } else
            {
                db.borrarConversacionAbierta(id, yo.getNombre());
                llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_ENTRANTE, null);
                llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_SALIENTE, null);
            }

        } catch (Exception e)
        {
            vista.excepcion(e);
        }

    }

    @Override
    public boolean boolEliminaConversacion(String id){
        boolean resultado = false;
        Grupo g = null;
        try
        {
            g = db.obtenerGrupo(id, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
            if (g != null)
            {
                abandonaGrupo(g.getId().toStringFull());
            } else
            {
                db.borrarConversacionAbierta(id, yo.getNombre());
                llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_ENTRANTE, null);
                llavero.setClaveSimetrica(id + Llavero.EXTENSION_CLAVE_SALIENTE, null);                
            }
            resultado = true;

        } catch (Exception e)
        {
            vista.excepcion(e);
        }finally{
            return resultado;
        }

    }

    @Override
    public void vaciaConversacion(String id)
    {
        try
        {
            db.borraMensajesDeConversacion(yo.getNombre(), id);
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
    }

    @Override
    public void registrarUsuario(String nombre, String contr) throws Exception
    {
        if (modo == MODO_REGISTRO)
        {
            JsonObject contenidoJson = Json.createObjectBuilder()
                                           .add("usuario", nombre)
                                           .add("contrasena", contr)
                                           .build();

            GestorFicherosConsola gfich = new GestorFicherosConsola();
            gfich.escribirAFichero(ControladorGUIImpl.NOMBRE_FICHERO_USUARIO, contenidoJson.toString().getBytes(StandardCharsets.UTF_8),
                    false);

            Llavero llavero = new ControladorLlaveroConsola(null, contr);

            Usuario nuevo = this.gestorUsuariosGrupos.creaUsuario(
                    nombre, ControladorGUIImpl.TAM_CLAVE_ASIMETRICA, Controlador.ALGORITMO_CLAVE_ASIMETRICA,
                    ControladorGUIImpl.ALGORITMO_FIRMA, ControladorGUIImpl.ALGORITMO_CLAVE_SIMETRICA);

            BloqueMensajes bloqueMensajes = new BloqueMensajes(nuevo.getBloqueMensajesImportantes()
                    , nuevo.getId(), false, null);

            llavero.setEntradaPrivada(nombre, nuevo.getPrivateKeyEntry());

            llavero.setClaveSimetrica(nombre, nuevo.getClaveSimetrica());

            guardaUsuarioEnPastry(nuevo, bloqueMensajes);

            llavero.guardarLlavero(nombre);

            llavero.cerrarLlavero(nombre);

            onStop();
            this.modo = MODO_INICIO_SESION;
        }


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
    }


    @Override
    public void crearGrupo(final String nombreGrupo, VistaConsolaPublic caller)
    {
        buscaIdDisponible(new Continuation<Id, Exception>()
        {
            @Override
            public void receiveResult(Id result)
            {
                try
                {
                    final Grupo nuevoGrupo = gestorUsuariosGrupos.creaGrupo(result, nombreGrupo, yo,
                            Controlador.ALGORITMO_CLAVE_ASIMETRICA, ControladorGUIImpl.TAM_CLAVE_ASIMETRICA,
                            ControladorGUIImpl.ALGORITMO_CLAVE_SIMETRICA, ControladorGUIImpl.ALGORITMO_FIRMA);


                    buscaIdDisponible(new Continuation<Id, Exception>()
                    {
                        @Override
                        public void receiveResult(Id result)
                        {
                            BloqueMensajes bloqueMensajes = new BloqueMensajes(result, nuevoGrupo.getId(), true, null);
                            nuevoGrupo.setBloqueMensajesImportantes(bloqueMensajes.getId());

                            final String idGrupo = nuevoGrupo.getId().toStringFull();
                            GrupoCifrado gc = null;
                            try
                            {
                                db.insertarGrupo(nuevoGrupo, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                            } catch (Exception e)
                            {
                                vista.excepcion(e);
                                error = Vista.ERROR_INSERCION_NUEVO_GRUPO_EN_BBDD;
                            }

                            try
                            {
                                db.insertarConversacionAbierta(nuevoGrupo.getId().toStringFull(),
                                        nuevoGrupo.getNombre(),
                                        yo.getNombre(),
                                        Conversacion.TIPO_GRUPO);
                            } catch (Exception e)
                            {
                                vista.excepcion(e);
                                error = Vista.ERROR_INSERCION_CONVERSACION_EN_BBDD;
                            }


                            try
                            {
                                gc = new GrupoCifrado(nuevoGrupo,
                                        (SecretKey) llavero.getClaveSimetrica(nuevoGrupo.getId().toStringFull()),
                                        llavero.getClavePrivada(nuevoGrupo.getId().toStringFull()));
                            } catch (Exception e)
                            {
                                vista.excepcion(e);
                                error = Vista.ERROR_CREACION_GRUPO_CIFRADO;
                            }


                            Continuation<Boolean[], Exception> cont = new Continuation<Boolean[], Exception>()
                            {
                                @Override
                                public void receiveResult(Boolean[] result)
                                {
                                    int inserciones = 0;
                                    for (boolean b : result)
                                    {
                                        if (b)
                                            inserciones++;
                                    }
                                    if (inserciones > 0)
                                    {
                                        if (logger.level <= Logger.INFO)
                                            logger.log("Insertado correctamente en " + inserciones + " ubicaciones");
                                    } else
                                    {
                                        if (logger.level <= Logger.INFO)
                                            logger.log("No se ha insertado el grupo " + idGrupo);
                                    }
                                }

                                @Override
                                public void receiveException(Exception exception)
                                {
                                    vista.excepcion(exception);
                                    error = Vista.ERROR_INSERCION_NUEVO_GRUPO_EN_PASTRY;
                                }
                            };

                            almacenamiento.insert(gc, cont);

                            if (logger.level <= Logger.INFO)
                                logger.log("grupo " + nuevoGrupo.getId().toStringFull() + " creado. Clave privada = " +
                                        Base64.getEncoder().encodeToString(
                                                llavero.getClavePrivada(nuevoGrupo.getId().toStringFull())
                                                       .getEncoded()));

                            almacenamiento.insert(bloqueMensajes, cont);
                            System.out.println("Caller notificargrupocreado");
                            caller.NotificarGrupoCreado();

                        }

                        @Override
                        public void receiveException(Exception exception)
                        {

                        }
                    });

                } catch (Exception e)
                {
                    vista.excepcion(e);
                    error = Vista.ERROR_CREACION_NUEVO_GRUPO;
                }
            }

            @Override
            public void receiveException(Exception exception)
            {

            }
        });
    }

    @Override
    public void crearGrupo(final String nombreGrupo)
    {
        buscaIdDisponible(new Continuation<Id, Exception>()
        {
            @Override
            public void receiveResult(Id result)
            {
                try
                {
                    final Grupo nuevoGrupo = gestorUsuariosGrupos.creaGrupo(result, nombreGrupo, yo,
                            Controlador.ALGORITMO_CLAVE_ASIMETRICA, ControladorGUIImpl.TAM_CLAVE_ASIMETRICA,
                            ControladorGUIImpl.ALGORITMO_CLAVE_SIMETRICA, ControladorGUIImpl.ALGORITMO_FIRMA);


                    buscaIdDisponible(new Continuation<Id, Exception>()
                    {
                        @Override
                        public void receiveResult(Id result)
                        {
                            BloqueMensajes bloqueMensajes = new BloqueMensajes(result, nuevoGrupo.getId(), true, null);
                            nuevoGrupo.setBloqueMensajesImportantes(bloqueMensajes.getId());

                            final String idGrupo = nuevoGrupo.getId().toStringFull();
                            GrupoCifrado gc = null;
                            try
                            {
                                db.insertarGrupo(nuevoGrupo, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                            } catch (Exception e)
                            {
                                vista.excepcion(e);
                                error = Vista.ERROR_INSERCION_NUEVO_GRUPO_EN_BBDD;
                            }

                            try
                            {
                                db.insertarConversacionAbierta(nuevoGrupo.getId().toStringFull(),
                                        nuevoGrupo.getNombre(),
                                        yo.getNombre(),
                                        Conversacion.TIPO_GRUPO);
                            } catch (Exception e)
                            {
                                vista.excepcion(e);
                                error = Vista.ERROR_INSERCION_CONVERSACION_EN_BBDD;
                            }


                            try
                            {
                                gc = new GrupoCifrado(nuevoGrupo,
                                        (SecretKey) llavero.getClaveSimetrica(nuevoGrupo.getId().toStringFull()),
                                        llavero.getClavePrivada(nuevoGrupo.getId().toStringFull()));
                            } catch (Exception e)
                            {
                                vista.excepcion(e);
                                error = Vista.ERROR_CREACION_GRUPO_CIFRADO;
                            }


                            Continuation<Boolean[], Exception> cont = new Continuation<Boolean[], Exception>()
                            {
                                @Override
                                public void receiveResult(Boolean[] result)
                                {
                                    int inserciones = 0;
                                    for (boolean b : result)
                                    {
                                        if (b)
                                            inserciones++;
                                    }
                                    if (inserciones > 0)
                                    {
                                        if (logger.level <= Logger.INFO)
                                            logger.log("Insertado correctamente en " + inserciones + " ubicaciones");
                                    } else
                                    {
                                        if (logger.level <= Logger.INFO)
                                            logger.log("No se ha insertado el grupo " + idGrupo);
                                    }
                                }

                                @Override
                                public void receiveException(Exception exception)
                                {
                                    vista.excepcion(exception);
                                    error = Vista.ERROR_INSERCION_NUEVO_GRUPO_EN_PASTRY;
                                }
                            };

                            almacenamiento.insert(gc, cont);

                            if (logger.level <= Logger.INFO)
                                logger.log("grupo " + nuevoGrupo.getId().toStringFull() + " creado. Clave privada = " +
                                        Base64.getEncoder().encodeToString(
                                                llavero.getClavePrivada(nuevoGrupo.getId().toStringFull())
                                                       .getEncoded()));

                            almacenamiento.insert(bloqueMensajes, cont);

                        }

                        @Override
                        public void receiveException(Exception exception)
                        {

                        }
                    });

                } catch (Exception e)
                {
                    vista.excepcion(e);
                    error = Vista.ERROR_CREACION_NUEVO_GRUPO;
                }
            }

            @Override
            public void receiveException(Exception exception)
            {

            }
        });
    }

    @Override
    public void abandonaGrupo(String idGrupo)
    {
        if (logger.level <= Logger.INFO) logger.log("Eliminando grupo, conversacion y claves asociado al grupo " +
                idGrupo);
        try
        {

            Id tmp = rice.pastry.Id.build(idGrupo);
            mensajero.abandonaGrupo(tmp);
            db.borrarConversacionAbierta(idGrupo, yo.getNombre());
            db.borrarGrupo(idGrupo, yo.getNombre());
            db.setValor(idGrupo + ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE, null);
            db.borraMensajesDeConversacion(yo.getNombre(), idGrupo);
            llavero.setClaveSimetrica(idGrupo, null);
            llavero.setEntradaPrivada(idGrupo, null);
            llavero.guardarLlavero(yo.getNombre());
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
    }

    @Override
    public void onStop()
    {
        if (modo == MODO_REGISTRO || modo == MODO_SESION_INICIADA)
        {
            noMuestresMensajes();
            // Algo para recibir mensajes sin conexion????
            //if (modo == MODO_SESION_INICIADA)

            this.node.destroy();
            try
            {
                this.env.getTimeSource().sleep(1000);
            } catch (InterruptedException e)
            {
                if (logger.level <= Logger.SEVERE) logger.logException("Error sleep tras destruir nodo.", e);
            }
            this.env.destroy();
            try
            {
                Thread.sleep(1500);
            } catch (InterruptedException e)
            {
                if (logger.level <= Logger.SEVERE) logger.logException("Error sleep tras destruir environment.", e);
            }
            this.onCreateEntorno();
        }
    }

    @Override
    public void onDestroy()
    {
        modo = MODO_APAGADO;
        this.env.destroy();
    }

    @Override
    public void cerrarSesion()
    {
        onStop();
        this.modo = MODO_INICIO_SESION;
        GestorFicherosConsola gfich = new GestorFicherosConsola();
        gfich.eliminaFichero(ControladorGUIImpl.NOMBRE_FICHERO_USUARIO);
    }

    @Override
    public void mensajeRecibido(Mensaje mensaje, Id idConversacion)
    {
        Conversacion destinatario = null;

        Contacto contacto;

        if (logger.level <= Logger.INFO) logger
                .log("Notificacion: recibido mensaje de " + idConversacion);
        String miNombre = yo.getNombre();
        Key clave = llavero.getClaveSimetrica(miNombre);
        if (clave == null)
        {
            if (logger.level <= Logger.WARNING) logger.logException(
                    "App.mensajeRecibido: No se ha podido obtener la clave", llavero.getError());
        } else
        {
            try
            {
                destinatario = db.obtenerConversacionAbierta(idConversacion.toStringFull(), miNombre);
                if (destinatario == null)
                {
                    contacto = db.obtenerContacto(idConversacion, llavero.getClaveSimetrica(miNombre),
                            miNombre);

                    if (contacto != null)
                    {
                        destinatario = new Conversacion(idConversacion, new Date(),
                                contacto.getAlias(), Conversacion.TIPO_INDIVIDUAL);
                        db.insertarConversacionAbierta(idConversacion.toStringFull(), destinatario.getAlias(),
                                miNombre, destinatario.getTipo());
                    } else
                    {
                        destinatario = new Conversacion(idConversacion, new Date(),
                                idConversacion.toStringFull(), Conversacion.TIPO_INDIVIDUAL)
                                .setMensaje(mensaje.getContenido());
                        db.insertarConversacionAbierta(idConversacion.toStringFull(), idConversacion.toStringFull(),
                                miNombre, destinatario.getTipo());
                    }
                } else
                {
                    destinatario.setMensaje(mensaje.getContenido());
                    db.insertarConversacionAbierta(idConversacion.toStringFull(), destinatario.getAlias(),
                            miNombre, destinatario.getTipo());
                }
            } catch (Exception e)
            {
                vista.excepcion(e);
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al actualizar la conversacion" +
                            " a la que pertenece el mensaje", e);
            }
            if (destinatario != null)
                this.guardaMensaje(mensaje, destinatario.getId());
        }

        if (conversacionAbierta != null && conversacionAbierta.getId().equals(idConversacion))
        {
            if (destinatario != null)
                this.vista.muestraMensaje(mensaje, destinatario.getAlias());
            else
                this.vista.muestraMensaje(mensaje, idConversacion.toStringFull());
        } else if (destinatario != null)
        {
            //muestra notificacion con que hay un nuevo mensaje
            vista.notificacion(destinatario.getAlias());
            vista.notificacionDestinatario(destinatario.getAlias());

        } else //muestra notificacion con que hay un nuevo mensaje
            vista.notificacion(idConversacion.toStringFull());
            vista.notificacionConversacion(idConversacion.toStringFull());
    }

    @Override
    public ArrayList<Mensaje> obtieneMensajes(String id, int primerMsj, int ultimoMsj, int tipoDeConversacion)
    {
        ArrayList<Mensaje> mensajes = null;
        try
        {
            mensajes = db.getMensajes(llavero.getClaveSimetrica(yo.getNombre()),
                    yo.getNombre(), primerMsj, ultimoMsj, id);
            
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        return mensajes;
    }

    @Override
    public void muestraMensajes(Conversacion conv)
    {
        this.conversacionAbierta = conv;
    }

    @Override
    public void noMuestresMensajes()
    {
        this.conversacionAbierta = null;
    }

    @Override
    public void enviaMensaje(int tipo, String contenido, String idString, boolean individual)
    {
        Id idDestino = rice.pastry.Id.build(idString);
        enviaMensaje(tipo, contenido, idDestino, individual);
    }

    @Override
    public void enviaMensaje(int tipo, String contenido, Id idDestino, boolean individual)
    {
        String miNombre = yo.getNombre();
        final Mensaje mensaje = new Mensaje(node.getId(),
                idDestino, contenido, tipo);
        Contacto contacto = null;
        if (mensaje.getClase() == Mensaje.INDIVIDUAL_IMPORTANTE)
        {
            try
            {
                contacto = db.obtenerContacto(idDestino, llavero.getClaveSimetrica(miNombre),
                        miNombre);
            } catch (Exception e)
            {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al leer el contacto con id " + idDestino + " de la bbdd", e);
            }
            if (contacto != null)
            {
                Id idBloque = contacto.getUsuario().getBloqueMensajesImportantes();
                this.insertaMensajeEnBloque(idBloque, mensaje);
            } else
            {
                // buscar usuario en pastry y obtener la id del bloque
                almacenamiento.lookup(idDestino, new Continuation<PastContent, Exception>()
                {
                    @Override
                    public void receiveResult(PastContent result)
                    {
                        Id bloqueUsuarioEncontrado = null;
                        if (result != null)
                        {
                            if (result instanceof GrupoCifrado)
                            {

                                insertaMensajeEnBloque(bloqueUsuarioEncontrado, mensaje);

                            } else if (result instanceof Usuario)
                            {
                                insertaMensajeEnBloque(bloqueUsuarioEncontrado, mensaje);
                            } else
                            {
                                if (logger.level <= Logger.INFO) logger.log(
                                        "El objeto leido no es de tipo " + GrupoCifrado.class + " o " + Usuario.class + " -> " + result);

                            }
                        } else
                        {
                            if (logger.level <= Logger.INFO) logger.log(
                                    "El objeto leido esta vacio");
                        }
                    }

                    @Override
                    public void receiveException(Exception exception)
                    {

                    }
                });
            }
        } else if (mensaje.getClase() == Mensaje.GRUPO_IMPORTANTE)
        {
            try
            {
                Grupo grupo = db.obtenerGrupo(idDestino, llavero.getClaveSimetrica(miNombre), yo.getNombre());
                Id idBloque;
                String ultimoIdConocido = db
                        .getValor(grupo.getId().toStringFull() + ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE);

                if (ultimoIdConocido.length() > 0)
                    idBloque = rice.pastry.Id.build(ultimoIdConocido);
                else
                    idBloque = grupo.getBloqueMensajesImportantes();

                this.insertaMensajeEnBloque(idBloque, mensaje);
            } catch (Exception e)
            {
                vista.excepcion(e);
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al obtener el contacto de la bbdd", e);
            }
        }
        mensajero.enviaMensaje(mensaje, individual);
    }

    @Override
    public void guardaMensaje(Mensaje mensaje, Id conversacion)
    {
        Key clave = llavero.getClaveSimetrica(yo.getNombre());
        if (clave != null)
        {
            try
            {
                db.insertarMensaje(mensaje, clave,
                        yo.getNombre(), conversacion.toStringFull());
            } catch (Exception e)
            {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.guardaMensaje: Error al guardar mensaje en la base de datos", e);
            }
        }
    }

    @Override
    public void errorEnviando(int error, String mensaje)
    {
        vista.errorEnviando(error, mensaje);
        switch (error)
        {
            case AppScribe.ERROR_LECTURA_CLAVE_SIMETRICA:

        }
    }

    @Override
    public boolean responderEcho()
    {
        boolean responder = true;
        try
        {
            String valor = db.getValor("responder_ping");
            responder = Boolean.parseBoolean(valor);
        } catch (Exception e)
        {
            if (logger.level <= Logger.INFO)
                logger.logException("Error al obtener la politica de respuesta de pings", e);
        }
        return responder;
    }

    @Override
    public void actualizaDireccion(InetAddress ip, int puerto)
    {
        if (ip != null)
            this.miDireccion = ip;
        if (puerto != -1)
            this.puertoEscucha = puerto;
    }


    // --- conectado a grupo

    private Grupo nuevoGrupo;
    private Continuation<Boolean[], Exception> continuacionInsertarGrupo;

    @Override
    public void conectaAGrupo(final String id, final String privateKeyString)
    {
        final Id grupo = rice.pastry.Id.build(id);
        nuevoGrupo = null;

        Continuation<PastContent, Exception> contBuscar = new Continuation<PastContent, Exception>()
        {
            private ControladorGUIImpl aplicacion;

            @Override
            public void receiveResult(PastContent result)
            {
                if (result != null)
                {
                    GrupoCifrado grupoCifrado = (GrupoCifrado) result;
                    try
                    {
                        //Key clavePrivada=llavero.getClavePrivada(id);
                        //Base64.getEncoder().encodeToString(llavero.getClavePrivada(nuevo.getId().toStringFull()).getEncoded()))
                        byte[] keyraw = Base64.getDecoder().decode(privateKeyString);
                        PrivateKey clavePrivada = ManejadorClaves
                                .leeClavePrivada(keyraw, Controlador.ALGORITMO_CLAVE_ASIMETRICA);
                        KeyStore.PrivateKeyEntry entrada = ManejadorClaves.entryFromKeys(
                                (PublicKey) grupoCifrado.getCertificado(), clavePrivada);
                        llavero.setEntradaPrivada(id, entrada);
                        llavero.guardarLlavero(yo.getNombre());
                        if (clavePrivada == null)
                        {
                            if (logger.level <= Logger.WARNING) logger.logException(
                                    "App.mensajeRecibido: No se ha podido obtener la clave",
                                    llavero.getError());
                        } else
                        {

                            nuevoGrupo = grupoCifrado.desencriptar(clavePrivada);

                            nuevoGrupo.insertar(yo);
                            llavero.setClaveSimetrica(id, nuevoGrupo.getClaveSimetrica());
                            llavero.guardarLlavero(yo.getNombre());

                            GrupoCifrado modificado = new GrupoCifrado(nuevoGrupo,
                                    (SecretKey) llavero.getClaveSimetrica(id), llavero.getClavePrivada(id));

                            nuevoGrupo = modificado.desencriptar(clavePrivada);

                            almacenamiento.insert(modificado, aplicacion.getContinuacionInsertarGrupo());
                        }
                    } catch (Exception e)
                    {
                        if (logger.level <= Logger.WARNING) logger.logException(
                                "App.conectaAGupo.Cont.receiveResult: Error al desencriptar" +
                                        " el grupo cifrado", e);
                    }

                } else if (logger.level <= Logger.INFO) logger.log("App.conectaAGupo.Cont.receiveResult: " +
                        "El grupo que esta buscando no existe");

                //                System.out.println(asdf);
            }

            @Override
            public void receiveException(Exception exception)
            {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.ContinuacionBuscar.Exception", exception);
            }

            Continuation<PastContent, Exception> inicializa(
                    ControladorGUIImpl aplicacion)
            {
                this.aplicacion = aplicacion;
                return this;
            }
        }.inicializa(this);

        continuacionInsertarGrupo = new Continuation<Boolean[], Exception>()
        {
            private Continuation<PastContent, Exception> continuacionBuscar;
            private int reintentos;

            public void receiveResult(Boolean[] result)
            {
                if (result.length > 0)
                {
                    try
                    {
                        Key claveBD = llavero.getClaveSimetrica(yo.getNombre());
                        //                        db.insertarGrupo(nuevoGrupo, claveBD, yo.getNombre());
                        db.insertarGrupo(nuevoGrupo, claveBD, yo.getNombre());
                        mensajero.subscribe(grupo);
                        db.insertarConversacionAbierta(nuevoGrupo.getId().toStringFull(),
                                nuevoGrupo.getNombre(), yo.getNombre(), Conversacion.TIPO_GRUPO);
                        //nuevoGrupo=null;
                        if (logger.level <= Logger.INFO) logger.log("Insertado de " +
                                "nuevo satisfactoriamente en " + result.length + "  ubicaciones");

                    } catch (Exception e)
                    {
                        if (logger.level <= Logger.INFO) logger.logException(
                                "App.conectaAGupo.Cont.receiveResult.Cont.ReceiveResult:" +
                                        " Error al insertar en la base de datos", e);
                    }
                } else if (logger.level <= Logger.INFO)
                    logger.log("Error al insertarlo de nuevo. 0 ubicaciones actualizadas");
            }

            @Override
            public void receiveException(Exception exception)
            {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.Cont.receiveResult.Cont.Exception", exception);
                if (reintentos < 3)
                {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.Cont.receiveResult.Cont.Exception: reintento " + reintentos++);
                    almacenamiento.lookup(grupo, this.continuacionBuscar);
                }
            }

            Continuation<Boolean[], Exception> inicializa(
                    Continuation<PastContent, Exception> continuacionBuscar)
            {
                this.continuacionBuscar = continuacionBuscar;
                reintentos = 0;
                return this;
            }
        }.inicializa(contBuscar);


        almacenamiento.lookup(grupo, contBuscar);

    }

    private Continuation<Boolean[], Exception> getContinuacionInsertarGrupo()
    {
        return this.continuacionInsertarGrupo;
    }

    // insertado de mensaje en el bloque

    private Continuation<Boolean[], Exception> continuacionInsertarMensaje;

    @Override
    public void insertaMensajeEnBloque(final Id idBloque, final Mensaje mensaje)
    {
        nuevoGrupo = null;

        Continuation<PastContent, Exception> contBuscar = new Continuation<PastContent, Exception>()
        {
            private ControladorGUIImpl aplicacion;

            @Override
            public void receiveResult(PastContent result)
            {
                if (result != null && result instanceof BloqueMensajes)
                {
                    final BloqueMensajes bloque = (BloqueMensajes) result;

                    if (bloque.isLleno())
                    {
                        almacenamiento.lookup(bloque.getSiguienteBloque(), this);
                    } else
                    {
                        Queue<EntradaMensaje> entradas = bloque.getMensajes();

                        MensajeCifrado mensajeCifrado;
                        EntradaMensaje nuevaEntrada;

                        Key clave = llavero.getClaveSimetrica(
                                bloque.getDestinatario().toStringFull() + Llavero.EXTENSION_CLAVE_SALIENTE);
                        if (clave == null)
                        {
                            try
                            {
                                clave = ManejadorClaves
                                        .generaClaveSimetrica(ControladorGUIImpl.ALGORITMO_CLAVE_SIMETRICA);
                                llavero.setClaveSimetrica(
                                        bloque.getDestinatario().toStringFull() + Llavero.EXTENSION_CLAVE_ENTRANTE,
                                        clave);
                            } catch (NoSuchAlgorithmException e)
                            {
                                if (logger.level <= Logger.WARNING) logger.logException(
                                        "Error al generar la clave simetrica para guardar el mensaje en el bloque",
                                        e);
                            } catch (KeyStoreException e)
                            {
                                if (logger.level <= Logger.WARNING) logger.logException(
                                        "Error al guardar en el llavero la clave generada",
                                        e);
                            }
                        }

                        try
                        {
                            mensajeCifrado = new MensajeCifrado(mensaje, clave);
                            X509Certificate certificado = llavero
                                    .getCertificado(bloque.getDestinatario().toStringFull());
                            if (certificado != null)
                            {
                                String claveSimetricaCifrada = ManejadorClaves
                                        .encriptaClaveSimetrica((SecretKey) clave, certificado.getPublicKey());
                                nuevaEntrada = new EntradaMensaje(mensajeCifrado, claveSimetricaCifrada, yo.getId());
                                bloque.insertarEntrada(nuevaEntrada);
                            } else
                            {
                                if (logger.level <= Logger.WARNING) logger.log(
                                        "No se encuentra el certificado del usuario con Id = " + bloque
                                                .getDestinatario());
                            }
                        } catch (Exception e)
                        {
                            if (logger.level <= Logger.WARNING) logger.logException(
                                    "Error al cifrar el mensaje", e);
                        }

                        if (bloque.isLleno())
                        {
                            buscaIdDisponible(new Continuation<Id, Exception>()
                            {
                                @Override
                                public void receiveResult(Id result)
                                {
                                    final BloqueMensajes siguienteBloque = new BloqueMensajes(result,
                                            bloque.getDestinatario(),
                                            bloque.isGrupo(), bloque.getId());
                                    almacenamiento.insert(siguienteBloque, new Continuation<Boolean[], Exception>()
                                    {
                                        @Override
                                        public void receiveResult(Boolean[] result)
                                        {
                                            int i = 0;
                                            for (boolean j : result)
                                                if (j) i++;
                                            if (logger.level <= Logger.INFO) logger.log(
                                                    "Insertado nuevo bloque " + siguienteBloque
                                                            .getId() + " correctamente en " + i + " ubicaciones");
                                        }

                                        @Override
                                        public void receiveException(Exception exception)
                                        {
                                            if (logger.level <= Logger.WARNING)
                                                logger.logException("Error al crear un nuevo bloque de mensajes",
                                                        exception);
                                        }

                                    });

                                    //insertar aqui el bloque que se acaba de llenar
                                    bloque.setSiguienteBloque(result);
                                    if (bloque.isGrupo())
                                    {
                                        try
                                        {
                                            db.setValor(bloque.getDestinatario()
                                                              .toStringFull() + ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE,
                                                    result.toStringFull());
                                        } catch (Exception e)
                                        {
                                            if (logger.level <= Logger.WARNING)
                                                logger.logException(
                                                        "Error al insertar un valor en la bbdd: " + bloque
                                                                .getDestinatario()
                                                                .toStringFull() + ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE + " : " + result
                                                                .toStringFull(),
                                                        e);
                                        }
                                    }
                                    almacenamiento.insert(bloque, aplicacion.getContinuacionInsertarMensaje());
                                }

                                @Override
                                public void receiveException(Exception exception)
                                {

                                }
                            });

                        } else
                        {
                            almacenamiento.insert(bloque, aplicacion.getContinuacionInsertarMensaje());
                            try
                            {
                                db.setValor(bloque.getDestinatario().toStringFull() +
                                        ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE, bloque.getId().toStringFull());
                            } catch (Exception e)
                            {
                                if (logger.level <= Logger.WARNING)
                                    logger.logException("Error al actualizar el valor del bloque en cache", e);
                            }
                        }
                    }
                } else if (logger.level <= Logger.WARNING)
                    logger.log("App.insertaMensajeEnBloque.Cont.receiveResult: " +
                            "El bloque que se esta buscando no existe");
                //                System.out.println(asdf);
            }

            @Override
            public void receiveException(Exception exception)
            {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.ContinuacionBuscar.Exception", exception);
            }

            Continuation<PastContent, Exception> inicializa(ControladorGUIImpl aplicacion)
            {
                this.aplicacion = aplicacion;
                return this;
            }
        }.inicializa(this);

        continuacionInsertarMensaje = new Continuation<Boolean[], Exception>()
        {
            private Continuation<PastContent, Exception> continuacionBuscar;
            private int reintentos;

            public void receiveResult(Boolean[] result)
            {
                int i = 0;
                for (boolean j : result)
                    if (j) i++;
                if (logger.level <= Logger.INFO) logger.log(
                        "Insertado bloque " + idBloque + " correctamente en " + i + " ubicaciones");
                if (i > 0)
                {
                    if (logger.level <= Logger.INFO)
                        logger.log("Mensaje importante enviado");

                    String ultimoIdConocido = "";
                    Mensaje mensajeRefrescoUltimoIdBloque = null;

                    if (!mensaje.getDestino().equals(yo.getId()))
                    {
                        try
                        {
                            ultimoIdConocido = db
                                    .getValor(mensaje.getDestino()
                                                     .toStringFull() + ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE);
                        } catch (Exception e)
                        {
                            if (logger.level <= Logger.WARNING) logger.logException(
                                    "Error al obtener el valor de " +
                                            mensaje.getDestino()
                                                   .toStringFull() + ControladorGUIImpl.EXTENSION_ULTIMO_BLOQUE, e);
                        }
                        if (ultimoIdConocido.length() > 0)
                        {
                            JsonObject contenidoJson = Json.createObjectBuilder()
                                                           .add("grupo", mensaje.getDestino().toStringFull())
                                                           .add("id_bloque", ultimoIdConocido)
                                                           .build();

                            mensajeRefrescoUltimoIdBloque = new Mensaje(yo.getId(), mensaje.getDestino(),
                                    contenidoJson.toString(),
                                    Mensaje.GRUPO_BLOQUE_UTILIZADO);
                            mensajero.enviaMensaje(mensajeRefrescoUltimoIdBloque, false);
                        }
                    }
                } else if (reintentos < 3)
                {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.Cont.receiveResult.Cont.receiveResult: reintento " + reintentos++);
                    almacenamiento.lookup(idBloque, this.continuacionBuscar);
                } else if (logger.level <= Logger.INFO)
                    logger.log("Error al insertarlo de nuevo. 0 ubicaciones actualizadas");
            }

            @Override
            public void receiveException(Exception exception)
            {
                if (logger.level <= Logger.WARNING) logger.logException(
                        "App.conectaAGupo.Cont.receiveResult.Cont.Exception", exception);
                if (reintentos < 3)
                {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "App.conectaAGupo.Cont.receiveResult.Cont.Exception: reintento " + reintentos++);
                    almacenamiento.lookup(idBloque, this.continuacionBuscar);
                }
            }

            Continuation<Boolean[], Exception> inicializa(
                    Continuation<PastContent, Exception> continuacionBuscar)
            {
                this.continuacionBuscar = continuacionBuscar;
                reintentos = 0;
                return this;
            }
        }.inicializa(contBuscar);


        almacenamiento.lookup(idBloque, contBuscar);

    }

    private Continuation<Boolean[], Exception> getContinuacionInsertarMensaje()
    {
        return this.continuacionInsertarMensaje;
    }

    // busqueda de un id que no este en uso

    @Override
    public void buscaIdDisponible(final Continuation<Id, Exception> cont)
    {
        final Id candidato = pastryIdFactory.buildId(gestorUsuariosGrupos.generateSessionKey(TAM_PARTE_ALEATORIA_ID));
        almacenamiento.lookup(candidato, new Continuation<PastContent, Exception>()
        {

            int reintentos;

            public Continuation<PastContent, Exception> inicializa()
            {
                this.reintentos = 0;
                return this;
            }

            @Override
            public void receiveResult(PastContent result)
            {
                if (result == null)
                    cont.receiveResult(candidato);
                else
                {
                    Id candidatoAlternativo = pastryIdFactory
                            .buildId(gestorUsuariosGrupos.generateSessionKey(TAM_PARTE_ALEATORIA_ID));
                    almacenamiento.lookup(candidatoAlternativo, this);
                }
            }

            @Override
            public void receiveException(Exception exception)
            {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al buscar el id " + candidato, exception);
                if (reintentos < 3)
                {
                    Id candidatoAlternativo = pastryIdFactory
                            .buildId(gestorUsuariosGrupos.generateSessionKey(TAM_PARTE_ALEATORIA_ID));
                    almacenamiento.lookup(candidatoAlternativo, this);
                    reintentos++;
                }
            }

        }.inicializa());
    }


    @Override
    public void actualizaDireccionBloque(Id grupo, Mensaje mensaje)
    {
        String idBloque = null;

        JsonReader jsonReader = Json
                .createReader(new ByteArrayInputStream(mensaje.getContenido().getBytes(StandardCharsets.UTF_8)));
        JsonObject objeto = jsonReader.readObject();
        idBloque = objeto.getString("id_bloque");
        if (grupo.toStringFull().equals(objeto.getString("grupo")))
        {
            try
            {
                db.setValor(grupo.toStringFull() + EXTENSION_ULTIMO_BLOQUE, idBloque);
                if (logger.level <= Logger.INFO)
                    logger.log("Actualizda direccion de bloque");

            } catch (Exception e)
            {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al insertar un valor en la bbdd: "
                            + grupo.toStringFull() + EXTENSION_ULTIMO_BLOQUE +
                            " : " + idBloque, e);
            }
        } else
        {
            if (logger.level <= Logger.WARNING)
                logger.log(
                        "actualizaDireccionBloque - no coinciden el valor de grupo " +
                                "y el id del que proviene el mensaje");

        }
    }


    @Override
    public PastryIdFactory getPastryIdFactory()
    {
        return pastryIdFactory;
    }

    @Override
    public void inicializaBBDD()
    {
        try
        {
            this.db.inicializarBD();
        } catch (Exception e)
        {
            if (logger.level <= Logger.WARNING) logger.logException("Error al iniciar la BBDD", e);
        }
    }

    @Override
    public void borraBBDD()
    {
        try
        {
            this.db.eliminaBD();
        } catch (Exception e)
        {
            if (logger.level <= Logger.WARNING) logger.logException("Error al borrar la BBDD", e);
        }
    }

    @Override
    public Logger getLogger()
    {
        return this.logger;
    }

    @Override
    public Usuario getUsuario()
    {
        return this.yo;
    }

    @Override
    public Conversacion getConversacion(Id id)
    {
        ArrayList<Conversacion> contactos = null;
        Conversacion devolver = null;
        try
        {
            contactos = this.db.obtenerConversacionesAbiertas(yo.getNombre());
            for (Conversacion c : contactos)
            {
                if (c.getId().equals(id))
                    devolver = c;
            }
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        return devolver;
    }

    @Override
    public void guardarConversacion(Conversacion conversacion)
    {
        if (conversacion != null)
        {
            try
            {
                db.insertarConversacionAbierta(conversacion.getId().toStringFull(),
                        conversacion.getAlias(), yo.getNombre(), conversacion.getTipo());
            } catch (Exception e)
            {
                vista.excepcion(e);
            }
        }
    }

    @Override
    public int getError()
    {
        return this.error;
    }

    private boolean resultado1;
    private boolean resultado2;

    @Override
    public void guardaUsuarioEnPastry(final Usuario usuario, final BloqueMensajes bloqueMensajes)
    {


        buscaIdDisponible(new Continuation<Id, Exception>()
        {
            @Override
            public void receiveResult(Id result)
            {
                usuario.setBloqueMensajesImportantes(result);

                almacenamiento.insert(usuario, new Continuation<Boolean[], Exception>()
                {
                    @Override
                    public void receiveResult(Boolean[] result)
                    {
                        int i = 0;
                        for (boolean j : result)
                            if (j) i++;
                        if (logger.level <= Logger.INFO) logger.log(
                                "Insertado correctamente en " + i + " ubicaciones");
                        resultado1 = false;
                    }

                    @Override
                    public void receiveException(Exception exception)
                    {
                        if (logger.level <= Logger.WARNING) logger.logException(
                                "App.guardarUsuarioEnPastry.buscarIdVacio.Cont.Exception", exception);
                        resultado1 = false;
                    }
                });
                almacenamiento.insert(bloqueMensajes, new Continuation<Boolean[], Exception>()
                {
                    @Override
                    public void receiveResult(Boolean[] result)
                    {
                        int i = 0;
                        for (boolean j : result)
                            if (j) i++;
                        if (logger.level <= Logger.INFO) logger.log(
                                "Insertado correctamente en " + i + " ubicaciones");
                        resultado2 = false;
                    }

                    @Override
                    public void receiveException(Exception exception)
                    {
                        if (logger.level <= Logger.WARNING) logger.logException(
                                "App.guardarUsuarioEnPastry.buscarIdVacio.Cont.Exception", exception);
                        resultado2 = false;
                    }
                });
            }

            @Override
            public void receiveException(Exception exception)
            {
                // la excepcion ya es tratada en buscarIdDisponible()
            }
        });
        resultado2 = true;
        resultado1 = true;
        int contador = 0;
        while ((resultado1 || resultado2) && contador < 100)
        {
            try
            {
                env.getTimeSource().sleep(100);
            } catch (InterruptedException e)
            {
                if (logger.level <= Logger.WARNING) logger.logException("Excepcion en App.guardarUsuarioEnPastry", e);
            }
            contador++;
        }
    }

    @Override
    public Llavero getLlavero()
    {
        return this.llavero;
    }

    @Override
    public void guardarLlavero()
    {
        try
        {
            this.llavero.cerrarLlavero(yo.getNombre());
        } catch (Exception e)
        {
            vista.excepcion(e);
            if (logger.level <= Logger.WARNING)
                logger.logException("Error al guardar el llavero en el sistema", e);
        }
    }

    @Override
    public InetSocketAddress getDireccion()
    {
        InetSocketAddress sock = null;
        try
        {
            sock = new InetSocketAddress(InetAddress.getLocalHost(), this.puertoEscucha);
        } catch (UnknownHostException e)
        {
            if (logger.level <= Logger.INFO) logger.logException("Error al obtener la direccion del nodo", e);
        }
        return sock;
    }

    @Override
    public ArrayList<String> obtenerClavesGuardadas() throws Exception
    {

        return this.llavero.obtenerClavesGuardadas();
    }

    @Override
    public boolean nuevaDireccionArranque(String ip, int puerto)
    {
        boolean ret = true;

        try
        {
            this.db.insertarDireccion(InetAddress.getByName(ip), puerto);
        } catch (Exception e)
        {
            vista.excepcion(e);
            ret = false;
        }
        GestorFicherosConsola gfich = new GestorFicherosConsola();
        if (ret)
            this.modo = MODO_INICIO_SESION;

        return ret;
    }

    @Override
    public boolean iniciarConversacion(String id)
    {
        boolean ret = false;

        Key claveSimetrica = llavero.getClaveSimetrica(yo.getNombre());
        Contacto contacto = null;
        try
        {
            contacto = db.obtenerContacto(id, claveSimetrica, yo.getNombre());
        } catch (Exception e)
        {
            vista.excepcion(e);
        }
        if (contacto != null)
        {
            String alias = contacto.getAlias();

            try
            {
                db.insertarConversacionAbierta(id, alias, yo.getNombre(), Conversacion.TIPO_INDIVIDUAL);
                ret = true;
            } catch (Exception e)
            {
                vista.excepcion(e);
            }
        }


        return ret;
    }

    @Override
    public Contacto obtenerContacto(String id)
    {
        Contacto contacto = null;
        Key claveSimetrica = llavero.getClaveSimetrica(yo.getNombre());
        try
        {
            contacto = db.obtenerContacto(id, claveSimetrica, yo.getNombre());
        } catch (Exception e)
        {
            vista.excepcion(e);
        }

        return contacto;
    }

    @Override
    public void inicioChatIndividual(final MensajeCifrado mcifrado)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".inicioChatIndividual("+mcifrado+")");
        if (mcifrado != null)
        {
            final Id interlocutor = mcifrado.getOrigen();

            Contacto c=null;
            try
            {
                c = db.obtenerContacto(mcifrado.getOrigen(),
                        llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
            } catch (Exception e)
            {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Error al leer de la base de datos el contacto: "+
                            mcifrado.getOrigen().toStringFull(),e);
            }
            if(c!=null)
            {
                System.out.println("el contacto existe");
                almacenamiento.lookup(interlocutor, new Continuation<PastContent, Exception>()
                {
                    @Override
                    public void receiveResult(PastContent result)
                    {
                        if (result != null && result instanceof Usuario)
                        {
                            System.out.println("Encontrado el usuario del otro extremo");
                            try
                            {
                                //todo comprobar si queremos contestar automaticamente cuando alguien
                                //TODO (continuacion linea superior) quiera comunicarse con nosotros
                                Usuario usr = (Usuario) result;
                                llavero.guardarCertificado(interlocutor.toStringFull(), usr.getCertificado());

                                JsonReader jsonReader = Json
                                        .createReader(new ByteArrayInputStream(
                                                mcifrado.getContenido().getBytes(StandardCharsets.UTF_8)));
                                JsonObject objeto = jsonReader.readObject();
                                String claveCifrada = null;
                                SecretKey sk = (SecretKey) llavero.getClaveSimetrica(
                                        interlocutor.toStringFull() + Llavero.EXTENSION_CLAVE_ENTRANTE);

                                boolean nueva = objeto.getBoolean("nueva");

                                if (nueva || sk == null)
                                {

                                    String algoritmo = objeto.getString("algoritmo");

                                    sk = ManejadorClaves.generaClaveSimetrica(algoritmo);

                                    llavero.setClaveSimetrica(
                                            interlocutor.toStringFull() + Llavero.EXTENSION_CLAVE_ENTRANTE, sk);

                                }
                                claveCifrada = ManejadorClaves
                                        .encriptaClaveSimetrica(sk, usr.getCertificado().getPublicKey());
                                mensajero.responderSolicitudClave(interlocutor, claveCifrada);

                            } catch (Exception e)
                            {
                                if (logger.level <= Logger.WARNING)
                                    logger.logException("Error al generar la clave simetrica " +
                                            "de sesion y compartirla con otro usuario", e);
                            }
                        }

                    }

                    @Override
                    public void receiveException(Exception exception)
                    {
                        if (logger.level <= Logger.WARNING)
                            logger.logException("Error al obtener el usuario", exception);
                    }
                });
            }
        }
    }

    @Override
    public void setPingPolicy(boolean allow)
    {
        try
        {
            db.setValor("responder_ping", Boolean.toString(allow));
        } catch (Exception e)
        {
            if (logger.level <= Logger.WARNING) logger.logException("Error al insertar valor", e);
        }
    }


    @Override
    public void ping(String objetivo, long timeout)
    {
        Id destino = null;
        if (objetivo.length() == yo.getId().toStringFull().length())
            destino = rice.pastry.Id.build(objetivo);
        else
            destino = this.pastryIdFactory.buildId(objetivo);

        String id = gestorUsuariosGrupos.generateSessionKey(50);

        String carga = "{\"id\": \"" + id + "\"}";

        Map<String, PingWithTimeout> map = this.observadoresPing.get(destino);

        if (map == null)
            map = new HashMap<>();

        map.put(id, new PingWithTimeout(this, timeout, destino, carga));

        this.observadoresPing.put(destino, map);

        mensajero.ping(destino, this, carga);
    }

    @Override
    public void notificarPing(int evento, Id objetivo, String carga)
    {

        JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(carga.getBytes(StandardCharsets.UTF_8)));
        JsonObject objeto = jsonReader.readObject();

        String id = objeto.getString("id");

        Map<String, PingWithTimeout> map = this.observadoresPing.get(objetivo);
        String respuesta = "";

        if (map != null)
        {
            PingWithTimeout observador = map.remove(id);
            if (observador != null)
            {
                long tiempo = new Date().getTime() - observador.getInicio().getTime();
                //64 bytes from 8.8.8.8: icmp_seq=0 ttl=57 time=11.565 ms

                switch (evento)
                {
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
            if (map.size() == 0)
            {
                this.mensajero.cancelarPing(objetivo);
                this.observadoresPing.remove(objetivo);
            }


            vista.notificarPing(respuesta);
        }
    }

    @Override
    public void mostrarMensajesImportantes(String idString)
    {
        Id id = rice.pastry.Id.build(idString);
        almacenamiento.lookup(id, new Continuation<PastContent, Exception>()
        {
            @Override
            public void receiveResult(PastContent result)
            {
                if (result != null)
                {
                    if (result instanceof BloqueMensajes)
                    {
                        BloqueMensajes bloque = (BloqueMensajes) result;
                        if (logger.level <= Logger.INFO)
                        {
                            // TODO entregar a la capa superior los mensajes decodificados
                            // TODO de este bloque, pasarle los 3 ids que viajan en el mismo
                            logger.log(bloque.toString());
                        }
                        if (bloque.getSiguienteBloque() != null)
                            almacenamiento.lookup(bloque.getSiguienteBloque(), this);
                    } else if (result instanceof GrupoCifrado)
                    {
                        GrupoCifrado grupo = (GrupoCifrado) result;
                        Key clave = llavero.getClavePrivada(grupo.getId().toStringFull());
                        if (clave != null)
                        {
                            Grupo g = null;
                            try
                            {
                                g = grupo.desencriptar(clave);
                                almacenamiento.lookup(g.getBloqueMensajesImportantes(), this);
                            } catch (Exception e)
                            {
                                if (logger.level <= Logger.INFO) logger.logException("Error al desencriptar grupo", e);
                            }
                        }
                    } else if (result instanceof Usuario)
                    {
                        Usuario usuario = ((Usuario) result);
                        almacenamiento.lookup(usuario.getBloqueMensajesImportantes(), this);
                    }
                }
            }

            @Override
            public void receiveException(Exception exception)
            {

            }
        });
    }

    @Override
    public void imprimeIdEnCache()
    {
        Map<String, String> valores = null;
        try
        {
            valores = db.getTodosLosValores(yo.getNombre());
            Set<String> keys = valores.keySet();
            for (String key : keys)
            {
                System.out.println("Value of '" + key + "' : '" + valores.get(key) + "'");
            }
        } catch (Exception e)
        {
            if (logger.level <= Logger.WARNING) logger.logException("Error al leer todos los valores", e);
        }
    }

    public void obtieneMensajesImportantes()
    {
        Grupo grupo = null;
        Id idBusqueda = null;
        String id = null;
        boolean isBloque = false;

        if (id != null)
        {
            if (!isBloque)
            {
                try
                {
                    String idCache = db.getValor(id + EXTENSION_ULTIMO_BLOQUE);
                    grupo = db.obtenerGrupo(id, llavero.getClaveSimetrica(yo.getNombre()), yo.getNombre());
                    if (!"".equals(idCache))
                    {
                        idBusqueda = rice.pastry.Id.build(idCache);
                    } else if (grupo != null)
                    {
                        idBusqueda = grupo.getBloqueMensajesImportantes();
                    }
                } catch (Exception e)
                {
                    vista.excepcion(e);
                }
            } else
            {
                idBusqueda = rice.pastry.Id.build(id);
            }
        } else
        {
            idBusqueda = yo.getBloqueMensajesImportantes();
        }

        if (idBusqueda != null)
        {
            almacenamiento.lookup(idBusqueda, new Continuation<PastContent, Exception>()
            {

                @Override
                public void receiveResult(PastContent pastContent)
                {
                    ArrayList<Mensaje> mensajes = null;
                    if (pastContent != null && pastContent instanceof BloqueMensajes)
                    {
                        BloqueMensajes bloque = (BloqueMensajes) pastContent;
                        Id destinatario = bloque.getDestinatario();

                        Queue<EntradaMensaje> entradas = bloque.getMensajes();
                        mensajes = new ArrayList<>();

                        Key clavePrivada;
                        if (destinatario.equals(yo.getId()))
                            clavePrivada = llavero.getClavePrivada(yo.getNombre());
                        else
                            clavePrivada = llavero.getClavePrivada(destinatario.toStringFull());

                        if (clavePrivada != null)
                        {
                            for (EntradaMensaje entrada : entradas)
                            {
                                String claveSimCifrada = entrada.getClaveSimetricaCifrada();
                                try
                                {
                                    Key claveSimetrica = ManejadorClaves.desencriptaClaveSimetrica(claveSimCifrada,
                                            clavePrivada, ALGORITMO_CLAVE_SIMETRICA);
                                    Mensaje mensaje = entrada.getMensaje().desencripta(claveSimetrica);
                                    mensajes.add(mensaje);

                                } catch (Exception e)
                                {
                                    mensajes.add(new Mensaje(entrada.getRemitente(),
                                            bloque.getDestinatario(), "Error en el mensaje",
                                            Mensaje.GRUPO_IMPORTANTE));
                                }
                            }
                        }

                        if (vista != null)
                        {
                            System.out.println(
                                    "[ " + bloque.getAnteriorBloque().toStringFull() + " <-- " + bloque.getId()
                                                                                                       .toStringFull() + " --> " +
                                            bloque.getSiguienteBloque().toStringFull() + " isGrupo = " + bloque
                                            .isGrupo() + "\n" + mensajes);
                        }
                    }
                }

                @Override
                public void receiveException(Exception e)
                {

                }
            });
        }


        //return grupo;
    }

    @Override
    public String obrenerCodigoInvitacion(String id)
    {
        String codigo = "";
        try
        {
            codigo = db.getValor(id + EXTENSION_CODIGO);
            if ("".equals(codigo))
            {
                codigo = gestorUsuariosGrupos.generateSessionKey(LONGITUD_CODIGO_UNIRSE_GRUPO);
                db.setValor(id + EXTENSION_CODIGO, codigo);
                Calendar cal = Calendar.getInstance(); // creates calendar
                cal.setTime(new Date()); // sets calendar time/date
                cal.add(Calendar.HOUR_OF_DAY, 1); // adds one hour
                Date fecha = cal.getTime(); // returns new date object, one hour in the future
                db.setValor(id + EXTENSION_CADUCIDAD_CODIGO, String.valueOf(fecha.getTime()));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return codigo;
    }

    public void enviarPeticionUnirAGrupo(String id, String codigo)
    {
        if (id != null && id.length() == yo.getId().toStringFull().length())
        {
            if (codigo != null && codigo.length() == LONGITUD_CODIGO_UNIRSE_GRUPO)
            {
                enviaMensaje(Mensaje.INDIVIDUAL_PETICION_UNIR_GRUPO, codigo, id, true);
            } else
            {
                System.out.println("fallo al leer el codigo");
            }
        } else
        {
            System.out.println("fallo al leer el codigo");
        }
    }


    public void peticionUnirAGrupo(Mensaje mensaje)
    {

        String idGrupoString = db.obtenerCodigosUnirGrupo(mensaje.getContenido());
        String respuesta = "";


        if (!"".equals(idGrupoString))
        {
            String idGrupo = idGrupoString.substring(0, yo.getId().toStringFull().length());
            String fechaCaducidadLong = null;
            try
            {
                fechaCaducidadLong = db.getValor(idGrupo +
                        EXTENSION_CADUCIDAD_CODIGO);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            Date fechaCaducidad;
            if (!"".equals(fechaCaducidadLong))
            {
                fechaCaducidad = new Date(Long.parseLong(fechaCaducidadLong));
                System.out.println("caducidad = " + fechaCaducidad + " ahora = " + new Date());
                if (fechaCaducidad.after(new Date()))
                {
                    Key clavePrivada = llavero.getClavePrivada(idGrupo);
                    String clavePrivadaString = Base64.getEncoder().encodeToString(clavePrivada.getEncoded());


                    JsonObject contenidoJson = Json.createObjectBuilder()
                                                   .add("id_grupo", idGrupo)
                                                   .add("clave_privada", clavePrivadaString)
                                                   .build();

                    respuesta = contenidoJson.toString();

                } else
                {
                    try
                    {
                        db.setValor(idGrupo + EXTENSION_CADUCIDAD_CODIGO, null);
                        db.setValor(idGrupo + EXTENSION_CODIGO, null);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    respuesta = "~~~El código solicitado no existe";
                }
            } else
                respuesta = "~~~El código solicitado no existe";
        } else
            respuesta = "~~~El código solicitado no existe";

        enviaMensaje(Mensaje.INDIVIDUAL_RESPUESTA_UNIR_GRUPO,
                respuesta, mensaje.getOrigen(), true);

    }

    @Override
    public void respuestaUnirAGrupo(Mensaje mensaje)
    {
        String inicio = mensaje.getContenido().substring(0, 3);
        System.out.println("inicio = " + inicio);
        if ("~~~".equals(inicio))
        {
            if (vista != null)
            {
                System.out.println("Error al unirse al grupo !!!!!");
            }
        } else
        {
            System.out.println("contenido = " + mensaje.getContenido());

            JsonReader jsonReader = Json
                    .createReader(new ByteArrayInputStream(mensaje.getContenido().getBytes(StandardCharsets.UTF_8)));
            JsonObject objeto = jsonReader.readObject();
            String idGrupo = objeto.getString("id_grupo");
            String clavePrivada = objeto.getString("clave_privada");
            conectaAGrupo(idGrupo, clavePrivada);
        }
    }


    //todo borrar al terminar el trabajo (puerta trasera)

    @Override
    public void guardarDireccion(InetAddress ip, int puerto)
    {
        try
        {
            db.insertarDireccion(ip, puerto);
        } catch (Exception e)
        {
            if (logger.level <= Logger.INFO)
                logger.logException("Error al guardar en la BBDD la direccion recibida", e);
        }
    }

}
