package us.tfg.p2pmessenger.model;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Key;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import rice.environment.logging.Logger;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import us.tfg.p2pmessenger.controller.ControladorApp;
import us.tfg.p2pmessenger.controller.ManejadorClaves;
import us.tfg.p2pmessenger.util.Base64;

/**
 * Created by FPiriz on 21/6/17.
 */
public class ManejadorBBDDConsola
{

    // Para imprimir mensajes de estado
    private Logger logger;

    // Nombre del fichero donde se ubica la base de datos
    private static final String DATABASE = "pastryAppDB.db";

    // tablas existentes en la base de datos y las sentencias SQL para crearlas
    private static final String TABLA_GRUPOS = "grupos";
    private static final String CREA_TABLA_GRUPOS = "CREATE TABLE IF NOT EXISTS " + TABLA_GRUPOS +
            " (id TEXT NOT NULL, grupo TEXT, creador TEXT NOT NULL , primary key (id,creador));";
    private static final String ELIMINA_TABLA_GRUPO = "DROP TABLE " + TABLA_GRUPOS + ";";


    private static final String TABLA_CONTACTOS = "contactos";
    private static final String CREA_TABLA_CONTACTOS = "CREATE TABLE IF NOT EXISTS " + TABLA_CONTACTOS +
            " (id TEXT NOT NULL, contacto TEXT, alias TEXT, creador TEXT NOT NULL, primary key (id,creador));";
    private static final String ELIMINA_TABLA_CONTACTOS = "DROP TABLE " + TABLA_CONTACTOS + ";";

    private static final String TABLA_DIR_ARRANQUE = "direcciones_arranque";
    private static final String CREAR_TABLA_DIR_ARRANQUE = "CREATE TABLE IF NOT EXISTS " + TABLA_DIR_ARRANQUE +
            " (numero INTEGER PRIMARY KEY NOT NULL, ip STRING, puerto INT);";
    private static final String ELIMINA_TABLA_DIR_ARRANQUE = "DROP TABLE " + TABLA_DIR_ARRANQUE + ";";

    private static final String TABLA_MENSAJES = "mensajes";
    private static final String CREAR_TABLA_MENSAJES = "CREATE TABLE IF NOT EXISTS " + TABLA_MENSAJES +
            " (numero INTEGER PRIMARY KEY AUTOINCREMENT, fecha TEXT, tipo INT, mensaje TEXT," +
            " conversacion TEXT NOT NULL,creador TEXT NOT NULL);";
    private static final String ELIMINA_TABLA_MENSAJES = "DROP TABLE " + TABLA_MENSAJES + ";";

    private static final String TABLA_MISCELANEA = "miscelaneo";
    private static final String CREAR_TABLA_MISCELANEA = "CREATE TABLE IF NOT EXISTS " + TABLA_MISCELANEA +
            " (atributo TEXT PRIMARY KEY NOT NULL, valor TEXT);";
    private static final String ELIMINA_TABLA_MISCELANEA = "DROP TABLE " + TABLA_MISCELANEA + ";";

    private static final String TABLA_CONVERSACIONES_ABIERTAS = "conversaciones_abiertas";
    private static final String CREAR_TABLA_CONVERSACIONES_ABIERTAS = "CREATE TABLE IF NOT EXISTS " +
            TABLA_CONVERSACIONES_ABIERTAS + " (fecha INTEGER NOT NULL, id TEXT NOT NULL, creador " +
            "TEXT, alias TEXT ,tipo TEXT NOT NULL, PRIMARY KEY (id,creador));";
    private static final String ELIMINA_TABLA_CONVERSACIONES_ABIERTAS = "DROP TABLE " + TABLA_CONVERSACIONES_ABIERTAS + ";";

    // identificador de recurso de la base de datos
    private static final String url = "JDBC:sqlite:" + DATABASE;

    // constructor
    public ManejadorBBDDConsola(Logger logger)
    {
        //TODO: cambiar para que sea acceso a la bbdd
        this.logger = logger;
    }


    private Connection connect()
    {
        // SQLite connection string
        Connection conn = null;
        try
        {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e)
        {
            if (logger.level <= Logger.SEVERE) logger.logException("Error al conectar a la base de daros", e);
        }
        return conn;
    }

    /**
     * inicializa la base de datos
     *
     * @return devuelve falso si ocurrio un error
     */
    public boolean inicializarBD()
    {
        boolean ok = true;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement())
        {
            DatabaseMetaData meta = conn.getMetaData();
            // crea las tablas en una base de datos vacia
            // create a new table
            stmt.execute(CREA_TABLA_GRUPOS);
            stmt.execute(CREA_TABLA_CONTACTOS);
            stmt.execute(CREAR_TABLA_DIR_ARRANQUE);
            stmt.execute(CREAR_TABLA_MENSAJES);
            stmt.execute(CREAR_TABLA_MISCELANEA);
            stmt.execute(CREAR_TABLA_CONVERSACIONES_ABIERTAS);
        } catch (SQLException e)
        {
            if (logger.level <= Logger.WARNING) logger.logException("ManejadorBBDDConsola.inicializarBD", e);
            ok = false;
        }
        return ok;
    }

    public boolean eliminaBD() throws Exception
    {
        boolean ok = true;
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement())
        {
            // elimina la base de datos existente
            stmt.execute(ELIMINA_TABLA_GRUPO);
            stmt.execute(ELIMINA_TABLA_CONTACTOS);
            stmt.execute(ELIMINA_TABLA_DIR_ARRANQUE);
            stmt.execute(ELIMINA_TABLA_MENSAJES);
            stmt.execute(ELIMINA_TABLA_MISCELANEA);
            stmt.execute(ELIMINA_TABLA_CONVERSACIONES_ABIERTAS);
        } catch (Exception e)
        {
            throw e;
        }
        return ok;
    }


    // -------------- operaciones sobre tabla grupos -----------------

    /**
     * Obtener un grupo de la base de datos a partir de su id
     *
     * @param id    Id del grupo que se quiere obtener
     * @param clave Clave de cifrado de la informacion en la entrada
     * @return El objeto grupo descifrado
     */
    public Grupo obtenerGrupo(Id id, Key clave, String creador) throws Exception
    {
        return obtenerGrupo(id.toStringFull(), clave, creador);
    }

    /**
     * Obtener un grupo de la base de datos a partir de su id
     *
     * @param id    Id del grupo que se quiere obtener
     * @param clave Clave de cifrado de la informacion en la entrada
     * @return El objeto grupo descifrado
     */
    public Grupo obtenerGrupo(String id, Key clave, String creador) throws Exception
    {
        Grupo grupo = null;
        String sql = "SELECT grupo FROM " + TABLA_GRUPOS + " WHERE id=? and creador = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);
            pstmt.setString(2, creador);
            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                String grupoCrifrado = rs.getString("grupo");
                String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);
                InputBuffer input = new SimpleInputBuffer(raw);
                short tipo = input.readShort();
                if (tipo == Grupo.TYPE)
                    grupo = new Grupo(input);
                else
                    throw new Exception("No se puede deserializar");
            }

        } catch (Exception e)
        {
            throw e;
        }

        return grupo;
    }

    /**
     * Obtiene el usuario que creo un grupo concreto en la base de datos
     *
     * @param id grupo del que queremos saber el creador
     * @return nombre de usuario del creador o null ni no existe el usuario
     * @throws Exception
     */
    public String obtenerNombreUsuarioQueEncriptoGrupo(Id id,String creador) throws Exception
    {
        return obtenerNombreUsuarioQueEncriptoGrupo(id.toStringFull(),creador);
    }

    /**
     * Obtiene el usuario que creo un grupo concreto en la base de datos
     *
     * @param id grupo del que queremos saber el creador
     * @return nombre de usuario del creador o null ni no existe el usuario
     * @throws Exception
     */
    public String obtenerNombreUsuarioQueEncriptoGrupo(String id, String creador) throws Exception
    {

        String autor = null;

        String sql = "SELECT creador FROM " + TABLA_GRUPOS + " WHERE id=? and creador = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);
            pstmt.setString(2, creador);
            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                autor = rs.getString("creador");
            }

        } catch (Exception e)
        {
            throw e;
        }
        return autor;
    }


    /**
     * Obtiene todos los grupos a los que pertenece un usuario
     *
     * @param creador usuario que nos interesa
     * @param clave   Clave de cifrado de la informacion en la entrada
     * @return lista con todos los usuarios encontrados en la base de datos
     * @throws Exception
     */
    public ArrayList<Grupo> obtenerTodosLosGruposDeUsuario(String creador, Key clave) throws Exception
    {

        ArrayList<Grupo> grupos = null;

        String sql = "SELECT id,grupo FROM " + TABLA_GRUPOS + " WHERE creador=?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, creador);
            ResultSet rs = pstmt.executeQuery();

            grupos = new ArrayList<>();
            // loop through the result set
            while (rs.next())
            {
                Grupo grupo;
                String grupoCrifrado = rs.getString("grupo");
                String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);
                InputBuffer input = new SimpleInputBuffer(raw);
                int tipo = input.readShort();
                if (tipo == Grupo.TYPE)
                    grupo = new Grupo(input);
                else
                    throw new Exception("No se puede deserializar");
                grupos.add(grupo);
            }

        } catch (Exception e)
        {
            throw e;
        }
        return grupos;
    }

    /**
     * Inserta un nuevo grupo en la base de datos
     *
     * @param grupo grupo que se va a insertar
     * @param autor usuario que inserta el grupo
     * @param clave Clave de cifrado de la informacion que va a ser guardada
     * @return devuelve falso si ocurrio algun error
     * @throws Exception
     */
    public boolean insertarGrupo(Grupo grupo, Key clave, String autor) throws Exception
    {

        boolean ok = true;
        String sql = "insert into " + TABLA_GRUPOS + " values (? ,? , ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, grupo.getId().toStringFull());

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(grupo.getType());
            grupo.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());


            pstmt.setString(2, ManejadorClaves.encrypt(serializado, clave));
            pstmt.setString(3, autor);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;

    }

    /**
     * Actualiza un grupo en la base de datos
     *
     * @param grupo grupo que queremos acualizar
     * @param autor usuario que inserta el grupo
     * @param clave Clave de cifrado de la informacion que va a ser guardada
     * @return devuelve falso si ocurrio algun error
     * @throws Exception
     */
    public boolean actualizaGrupo(Grupo grupo, Key clave, String autor) throws Exception
    {

        boolean ok = true;
        String sql = "replace into " + TABLA_GRUPOS + " values (?, ?, ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, grupo.getId().toStringFull());

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(grupo.getType());
            grupo.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());


            pstmt.setString(2, ManejadorClaves.encrypt(serializado, clave));
            pstmt.setString(3, autor);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;

    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un grupo
     *
     * @param id grupo que queremos borrar
     * @return devuelve falso si ocurrio un error
     * @throws Exception
     */
    public boolean borrarGrupo(Id id,String creador) throws Exception
    {
        return borrarGrupo(id.toStringFull(),creador);
    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un grupo
     *
     * @param id grupo que queremos borrar
     * @return devuelve falso si ocurrio un error
     * @throws Exception
     */
    public boolean borrarGrupo(String id, String creador) throws Exception
    {

        boolean ok = true;
        String sql = "delete from " + TABLA_GRUPOS + " where id = ? and creador = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);
            pstmt.setString(2, creador);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;

    }

    // -------------- fin operaciones sobre tabla grupos -----------------

    // -------------- operaciones sobre tabla contactos -------------

    /**
     * Obtener un contacto de la base de datos a partir de su id
     *
     * @param id    Id del contacto que se quiere obtener
     * @param clave Clave de cifrado de la informacion en la entrada
     * @return El objeto contacto descifrado
     */
    public Contacto obtenerContacto(Id id, Key clave, String creador) throws Exception
    {
        return obtenerContacto(id.toStringFull(), clave, creador);
    }

    /**
     * Obtener un contacto de la base de datos a partir de su id
     *
     * @param id    Id del contacto que se quiere obtener
     * @param clave Clave de cifrado de la informacion en la entrada
     * @return El objeto contacto descifrado
     */
    public Contacto obtenerContacto(String id, Key clave, String creador) throws Exception
    {
        Contacto contacto = null;

        String sql = "select contacto from " + TABLA_CONTACTOS + " where id = ? and creador = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);
            pstmt.setString(2, creador);

            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                String grupoCrifrado = rs.getString("contacto");
                String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);
                InputBuffer input = new SimpleInputBuffer(raw);
                int tipo = input.readShort();
                if (tipo == Contacto.TYPE)
                    contacto = new Contacto(input);
                else
                    throw new Exception("No se puede deserializar el contenido");
            }
        } catch (Exception e)
        {
            throw e;
        }

        return contacto;
    }

    /**
     * Obtiene el usuario que creo un contacto concreto en la base de datos
     *
     * @param id contacto del que queremos saber el creador
     * @return nombre de usuario del creador o null ni no existe el usuario
     * @throws Exception
     */
    public String obtenerNombreUsuarioQueEncriptoContacto(Id id) throws Exception
    {
        return obtenerNombreUsuarioQueEncriptoContacto(id.toStringFull());
    }

    /**
     * Obtiene el usuario que creo un contacto concreto en la base de datos
     *
     * @param id contacto del que queremos saber el creador
     * @return nombre de usuario del creador o null ni no existe el usuario
     * @throws Exception
     */
    public String obtenerNombreUsuarioQueEncriptoContacto(String id) throws Exception
    {
        String autor = null;

        String sql = "select creador from " + TABLA_CONTACTOS + " where id = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);

            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                autor = rs.getString("creador");
            }
        } catch (Exception e)
        {
            throw e;
        }

        return autor;
    }


    /**
     * Obtiene todos los contactos de un usuario
     *
     * @param creador usuario que nos interesa
     * @param clave   Clave de cifrado de la informacion en la entrada
     * @return lista con todos los usuarios encontrados en la base de datos
     * @throws Exception
     */
    public ArrayList<Contacto> obtenerTodosLosContactosDeUsuario(String creador, Key clave) throws Exception
    {

        ArrayList<Contacto> grupos = null;

        String sql = "select id,contacto from " + TABLA_CONTACTOS + " where creador = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, creador);

            ResultSet rs = pstmt.executeQuery();

            grupos = new ArrayList<>();
            // loop through the result set
            while (rs.next())
            {
                Contacto contacto;
                String grupoCrifrado = rs.getString("contacto");
                String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);

                InputBuffer input = new SimpleInputBuffer(raw);
                int tipo = input.readShort();
                if (tipo == Contacto.TYPE)
                    contacto = new Contacto(input);
                else
                    throw new Exception("No se puede deserializar el contenido");

                grupos.add(contacto);
            }
        } catch (Exception e)
        {
            throw e;
        }


        return grupos;
    }

    /**
     * Inserta un nuevo contacto en la base de datos
     *
     * @param contacto el contacto que se quiere insertar
     * @param autor    usuario que inserta el contacto
     * @param clave    Clave de cifrado de la informacion que va a ser guardada
     * @return devuelve falso si ocurrio algun error
     * @throws Exception
     */
    public boolean insertarContacto(Contacto contacto, Key clave, String autor) throws Exception
    {

        boolean ok = true;
        String sql = "insert into " + TABLA_CONTACTOS + " values (? , ? , ? , ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, contacto.getId().toStringFull());

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(contacto.getType());
            contacto.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());

            pstmt.setString(2, ManejadorClaves.encrypt(serializado, clave));
            pstmt.setString(3, contacto.getAlias());
            pstmt.setString(4, autor);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;

    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un contacto
     *
     * @param id contacto que queremos borrar
     * @return devuelve falso si ocurrio un error
     * @throws Exception
     */
    public boolean borrarContacto(Id id, String creador) throws Exception
    {
        return borrarContacto(id.toStringFull(),creador);
    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un contacto
     *
     * @param id contacto que queremos borrar
     * @return devuelve falso si ocurrio un error
     * @throws Exception
     */
    public boolean borrarContacto(String id, String creador) throws Exception
    {

        boolean ok = true;
        String sql = "delete from " + TABLA_CONTACTOS + " where id = ? and creador = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);
            pstmt.setString(2, creador);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }

        return ok;
    }

    public Map<String, String> buscaContacto(String terminoBusqueda, String creador) throws Exception
    {
        Map<String, String> contactos = null;
        //        WHERE name LIKE '%"+terminoBusqueda+"%';

        String sql = "select id,alias from " + TABLA_CONTACTOS + " where alias like ? and creador=?";


        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(2, creador);
            pstmt.setString(1, "%" + terminoBusqueda + "%");

            ResultSet rs = pstmt.executeQuery();

            contactos = new HashMap<>();
            // loop through the result set
            while (rs.next())
            {
                contactos.put(rs.getString("id"), rs.getString("alias"));
            }
        } catch (Exception e)
        {
            throw e;
        }


        return contactos;
    }

    // -------------- fin operaciones sobre tabla contactos -------------

    // -------------- operaciones sobre tabla dir_arranque -------------

    /**
     * Obtener la lista de ips conocidas para poder unirnos a la red pastry a partir de ella
     *
     * @param ip     direccion del nodo del que hemos recibido la direccion. Puede ser IPv4 o IPv6
     * @param puerto puerto del nodo del que hemos recibido la direccion.
     * @return devuelve verdad si se guardo correctamente
     * @throws Exception
     */
    public boolean insertarDireccion(InetAddress ip, int puerto) throws Exception
    {
        boolean ok = true;
        String sql = "insert or replace into " + TABLA_DIR_ARRANQUE + " values (null, ? , ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, ip.getHostAddress());
            pstmt.setInt(2, puerto);

            pstmt.executeUpdate();
            System.out.println("Ejecutada insercion direccion ok en BBDD");
        } catch (Exception e)
        {
            ok=false;
            System.out.println("Error al insertar direccion ok en BBDD");
            throw e;
        }
        return ok;
    }

    /**
     * Obtiene todas las direcciones conocidas
     *
     * @return lista de objetos {@link InetSocketAddress} conocidos
     * @throws Exception
     */
    public ArrayList<InetSocketAddress> getDireccionesDeArranque() throws Exception
    {

        ArrayList<InetSocketAddress> dirs = null;


        String sql = "select ip,puerto from " + TABLA_DIR_ARRANQUE;


        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            ResultSet rs = pstmt.executeQuery();

            dirs = new ArrayList<>();
            // loop through the result set
            while (rs.next())
            {
                String ip = rs.getString("ip");
                int puerto = rs.getInt("puerto");
                InetSocketAddress addr = new InetSocketAddress(ip, puerto);
                dirs.add(addr);
            }
        } catch (Exception e)
        {
            throw e;
        }

        return dirs;
    }

    /**
     * Elimina todas las direcciones que tenemos almacenadas. Normalmente es cuando
     * se ha iniciado la aplicacion satisfactoriamente y se van a actualizar las entradas
     *
     * @return verdad si no ocurrio ningun problema
     * @throws Exception
     */
    public boolean vaciarDirecciones() throws Exception
    {
        boolean ok = true;
        String sql = "delete from " + TABLA_DIR_ARRANQUE;

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.executeUpdate();
        }
        return ok;
    }

    // -------------- fin operaciones sobre tabla dir_arranque -------------

    // -------------- operaciones sobre tabla mensajes -------------

    public boolean insertarMensaje(Mensaje mensaje, Key clave, String creador, String conversacion) throws Exception
    {
        //        SELECT * FROM (
        //                SELECT * FROM table ORDER BY id DESC LIMIT 50
        //        ) sub
        //        ORDER BY id ASC
        boolean ok = true;
        String sql = "insert into " + TABLA_MENSAJES + " values (null, ?, ?, ?, ?, ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            DateFormat df = new SimpleDateFormat(ControladorApp.FORMATO_FECHA);

            pstmt.setString(1, df.format(mensaje.getFecha()));
            pstmt.setInt(2, mensaje.getClase());


            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(mensaje.getType());
            mensaje.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());

            pstmt.setString(3, ManejadorClaves.encrypt(serializado, clave));
            pstmt.setString(4, conversacion);
            pstmt.setString(5, creador);
            pstmt.executeUpdate();
        }
        return ok;

    }

    /**
     * Leera los mensajes desde el mas nuevo hacia el mas antiguo. Con este
     * metodo le dices en que numero quiere empezar a leer (siempre desde el
     * mas nuevo hacia el mas antiguo). Si intentas leer mas mensjes de los
     * que hay, te devolvera solo los que tiene. '0' significa el mensaje
     * mas reciente y -1 el mas antiguo. Para saber cuantos mensajes hay
     * almacenados, llamar a {@link #getNumeroMensajesAlmacenados(String, String)}
     *
     * @param clave        Para desencriptar los mensajes.
     * @param creador      Usuario que envio o recibio los mensajes
     * @param primero      primer mensaje que quieras leer contando desde el mas nuevo
     * @param ultimo       ultimo mensaje que se quiera leer, este no incluido
     * @param conversacion conversacion a la que pertenece un mensaje
     * @return devuelve una lista con los mensajes
     * @throws Exception
     */
    public ArrayList<Mensaje> getMensajes(Key clave, String creador
            , int primero, int ultimo, String conversacion) throws Exception
    {

        return getMensajesPorClase(clave, creador, primero, ultimo, -1, conversacion);
    }

    /**
     * Leera los mensajes desde el mas nuevo hacia el mas antiguo. Con este
     * metodo le dices en que numero quiere empezar a leer (siempre desde el
     * mas nuevo hacia el mas antiguo). Si intentas leer mas mensjes de los
     * que hay, te devolvera solo los que tiene. '0' significa el mensaje
     * mas reciente y -1 el mas antiguo. Para saber cuantos mensajes hay
     * almacenados, llamar a {@link #getNumeroMensajesAlmacenados(String, String)}
     *
     * @param clave        Para desencriptar los mensajes.
     * @param creador      Usuario que envio o recibio los mensajes
     * @param primero      primer mensaje que quieras leer contando desde el mas nuevo
     * @param ultimo       ultimo mensaje que se quiera leer, este no incluido
     * @param clase        clase del mensaje que se quiere obtener. Si -1 devuelve todas las clases
     * @param conversacion conversacion a la que pertenece un mensaje
     * @return devuelve una lista con los mensajes
     * @throws Exception
     */
    public ArrayList<Mensaje> getMensajesPorClase(Key clave, String creador, int primero
            , int ultimo, int clase, String conversacion) throws Exception
    {
        ArrayList<Mensaje> mensajes = null;

        /*
            select * from TABLA where numero in (
                select numero from mensajes where creador=CREADOR
                order by numero desc limit FINAL-INICIAL offset INICIAL
            ) order by numero asc;
         */


        StringBuilder sqlBuilder = new StringBuilder().append("select numero,mensaje from ")
                                                      .append(TABLA_MENSAJES)
                                                      .append(" where numero in ( select numero from ")
                                                      .append(TABLA_MENSAJES).append(" where creador=?");

        if (clase != -1)
            sqlBuilder.append(" and tipo=?");
        if (conversacion != null)
            sqlBuilder.append(" and conversacion=?");

        sqlBuilder.append(" order by numero desc limit ?-? offset ?)")
                  .append(" order by numero asc");

        String sql = sqlBuilder.toString();

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            if (primero == 0 && ultimo == 0)
                ultimo++;

            int parameterIndex = 1;
            pstmt.setString(parameterIndex++, creador);

            if (clase != -1)
                pstmt.setInt(parameterIndex++, clase);

            if (conversacion != null)
                pstmt.setString(parameterIndex++, conversacion);

            pstmt.setInt(parameterIndex++, ultimo);
            pstmt.setInt(parameterIndex++, primero);
            pstmt.setInt(parameterIndex, primero);

            ResultSet rs = pstmt.executeQuery();

            mensajes = new ArrayList<>();
            // loop through the result set
            while (rs.next())
            {
                String mensajeCifrado = rs.getString("mensaje");
                String grupoBase64 = ManejadorClaves.decrypt(mensajeCifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);

                InputBuffer input = new SimpleInputBuffer(raw);
                Mensaje mensaje = null;

                int tipo = input.readShort();
                if (tipo == Mensaje.TYPE)
                    mensaje = new Mensaje(input);
                else
                    throw new Exception("No se puede deserializar el contenido");

                mensajes.add(mensaje);
            }
        }

        return mensajes;
    }

    /**
     * Obtiene el numero de mensajes almacenados en la base de datos para un usuario
     *
     * @param creador usuario del que queremos obtener los mensajes
     * @return el numero de mensajes que existe
     * @throws Exception
     */
    public int getNumeroMensajesAlmacenados(String creador, String conversacion) throws Exception
    {
        int almacenados = 0;

        String sql = "select count(numero) as almacenados from " + TABLA_MENSAJES +
                " where creador=? and conversacion=?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, creador);
            pstmt.setString(2, conversacion);

            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                almacenados = rs.getInt("almacenados");
            }
        } catch (Exception e)
        {
            throw e;
        }

        return almacenados;
    }

    /**
     * Elimina los mensajes cuyos indices esten incluidos en la lista 'mensajes'
     *
     * @param creador la persona que inserto esos mensajes
     * @param conversacion de donde se quieren eliminar los mensajes
     * @return verdad si se ejecuto correctamente
     * @throws Exception
     */
    public boolean borraMensajesDeConversacion(String creador, String conversacion) throws Exception
    {
        boolean ok = true;
        String sql = "delete from " + TABLA_MENSAJES + " where creador=? and conversacion=?";
        //String csv = String.join(",",mensajes);


        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, creador);
            pstmt.setString(2, conversacion);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;

    }

    /**
     * Devuelve la fecha en la que se guardo un mensaje
     *
     * @param mensaje
     * @return
     * @throws Exception
     */
    public Date obtieneFechaDeMensaje(int mensaje) throws Exception
    {
        Date fecha = null;

        String sql = "select fecha from " + TABLA_MENSAJES +
                " where numero=?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setInt(1, mensaje);

            ResultSet rs = pstmt.executeQuery();
            DateFormat df = new SimpleDateFormat(ControladorApp.FORMATO_FECHA);

            // loop through the result set
            while (rs.next())
            {

                fecha = df.parse(rs.getString("fecha"));
            }
        } catch (Exception e)
        {
            throw e;
        }

        return fecha;
    }

    // -------------- fin operaciones sobre tabla mensajes -------------
    // -------------- operaciones sobre tabla miscelaneo -------------

    public String getValor(String atributo) throws Exception
    {
        String sql = "select valor from " + TABLA_MISCELANEA +
                " where atributo=?";
        String valor = "";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, atributo);

            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {

                valor = rs.getString("valor");
            }
        } catch (Exception e)
        {
            throw e;
        }
        return valor;
    }

    public Map<String,String> getTodosLosValores(String creador) throws Exception
    {
        //TODO: cambiar la tabla miscelanea para que tambien incluya el creador
        //String sql = "select * from "+TABLA_MISCELANEA+ "where creador=?";
        String sql = "select atributo,valor from "+TABLA_MISCELANEA;
        Map<String,String> valores=null;
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

//            pstmt.setString(1, creador);

            ResultSet rs = pstmt.executeQuery();

            valores = new HashMap<>();

            // loop through the result set
            while (rs.next())
            {
                String atributo=rs.getString("atributo");
                String valor=rs.getString("valor");
                valores.put(atributo,valor);
            }
        } catch (Exception e)
        {
            throw e;
        }
        return valores;

    }

    public boolean setValor(String atributo, String valor) throws Exception
    {
        boolean ok = true;

        String sql = "";

        if (valor == null)
            sql = "delete from " + TABLA_MISCELANEA + " where atributo=?";
        else
            sql = "insert or replace into " + TABLA_MISCELANEA + " values (?,?)";


        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, atributo);
            if (valor != null)
                pstmt.setString(2, valor);

            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;
    }

    public ArrayList<String> obtenerConversacionesPendiente() throws Exception
    {
            ArrayList<String> contactos = null;
            //        WHERE name LIKE '%"+terminoBusqueda+"%';

            String sql = "select valor from" + TABLA_MISCELANEA + " where atribto like ?";


            try (Connection conn = this.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql))
            {
                pstmt.setString(1, "%" + ControladorApp.EXTENSION_CONVERSACION_PENDIENTE);

                ResultSet rs = pstmt.executeQuery();

                contactos = new ArrayList<>();
                // loop through the result set
                while (rs.next())
                {
                    contactos.add(rs.getString("valor"));
                }
            } catch (Exception e)
            {
                throw e;
            }


            return contactos;
    }


    public String obtenerCodigosUnirGrupo(String codigo)
    {
        //        WHERE name LIKE '%"+terminoBusqueda+"%';

        String sql = "select atributo from " + TABLA_MISCELANEA + " where valor = ?";
        String id ="";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, codigo);

            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                id=rs.getString("atributo");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return id;
    }


    // -------------- fin operaciones sobre tabla miscelaneo -------------

    // -------------- operaciones sobre tabla conversaciones abiertas -------------

    public boolean insertarConversacionAbierta(String id, String alias, String creador, int tipo) throws Exception
    {
        // todo borrar
        boolean ok = true;
        String sql = "insert or replace into " + TABLA_CONVERSACIONES_ABIERTAS + " values (?,?,?,?,?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setLong(1, new Date().getTime());
            pstmt.setString(2, id);
            pstmt.setString(3, creador);
            pstmt.setString(4, alias);
            pstmt.setInt(5, tipo);

            pstmt.executeUpdate();
        } catch (Exception e)
        {
            ok = false;
            throw e;
        }
        return ok;
    }

    public ArrayList<Conversacion> obtenerConversacionesAbiertas(String creador) throws Exception
    {
        ArrayList<Conversacion> conversaciones;
        String sql = "select id,fecha,alias,tipo from " + TABLA_CONVERSACIONES_ABIERTAS +
                " where creador=? order by fecha desc";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, creador);

            ResultSet rs = pstmt.executeQuery();

            conversaciones = new ArrayList<>();
            // loop through the result set
            while (rs.next())
            {
                String idString = rs.getString("id");
                Id id = rice.pastry.Id.build(idString);
                long milis = rs.getLong("fecha");
                String alias = rs.getString("alias");
                int tipo = rs.getInt("tipo");
                Date fecha = new Date(milis);

                Conversacion conv = new Conversacion(id, fecha, alias, tipo);
                conversaciones.add(conv);
            }
        } catch (Exception e)
        {
            throw e;
        }

        return conversaciones;
    }

    public Conversacion obtenerConversacionAbierta(String id, String creador) throws Exception
    {
        Conversacion devolver = null;
        String sql = "select fecha,alias,tipo from " + TABLA_CONVERSACIONES_ABIERTAS +
                " where creador=? and id=?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, creador);
            pstmt.setString(2, id);

            ResultSet rs = pstmt.executeQuery();

            // loop through the result set
            while (rs.next())
            {
                long milis = rs.getInt("fecha");
                String alias = rs.getString("alias");
                int tipo = rs.getInt("tipo");
                devolver = new Conversacion(rice.pastry.Id.build(id), new Date(milis), alias, tipo);
            }
        } catch (Exception e)
        {
            throw e;
        }
        return devolver;
    }

    public boolean borrarConversacionAbierta(String id, String creador) throws Exception
    {
        boolean ok = true;
        String sql = "delete from " + TABLA_CONVERSACIONES_ABIERTAS + " where id = ? and creador=?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, id);
            pstmt.setString(2, creador);
            pstmt.executeUpdate();
        } catch (Exception e)
        {
            throw e;
        }
        return ok;

    }

}
