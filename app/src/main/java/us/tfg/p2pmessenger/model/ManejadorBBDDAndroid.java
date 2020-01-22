package us.tfg.p2pmessenger.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Key;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import us.tfg.p2pmessenger.controller.ManejadorClaves;
import us.tfg.p2pmessenger.util.Base64;

import static us.tfg.p2pmessenger.controller.Controlador.EXTENSION_CONVERSACION_PENDIENTE;
import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Clase que encapsula las operaciones de la base de datos.
 * Forma parte del modelo en el esquema Modelo - Vista - Controlador
 */
public class ManejadorBBDDAndroid extends SQLiteOpenHelper
{

    // Nombre del fichero donde se ubica la base de datos
    private static final String DATABASE = "pastryAppDB.db";
    private static final int VERSION_BASEDATOS = 1;


    // tablas existentes en la base de datos y las sentencias SQL para crearlas
    private static final String TABLA_GRUPOS = "grupos";
    private static final String CREAR_TABLA_GRUPOS = "CREATE TABLE IF NOT EXISTS " + TABLA_GRUPOS +
            " (id TEXT NOT NULL, grupo TEXT, creador TEXT NOT NULL , primary key (id,creador));";
    private static final String ELIMINA_TABLA_GRUPOS = "DROP TABLE " + TABLA_GRUPOS + ";";


    private static final String TABLA_CONTACTOS = "contactos";
    private static final String CREAR_TABLA_CONTACTOS = "CREATE TABLE IF NOT EXISTS " + TABLA_CONTACTOS +
            " (id TEXT NOT NULL, contacto TEXT, alias TEXT, creador TEXT NOT NULL, primary key (id,creador));";
    private static final String ELIMINA_TABLA_CONTACTOS = "DROP TABLE " + TABLA_CONTACTOS + ";";

    private static final String TABLA_DIR_ARRANQUE = "direcciones_arranque";
    private static final String CREAR_TABLA_DIR_ARRANQUE = "CREATE TABLE IF NOT EXISTS " + TABLA_DIR_ARRANQUE +
            " (numero INTEGER PRIMARY KEY NOT NULL, ip TEXT, puerto INT);";
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
    //private static final String url = "jdbc:sqlite:" + DATABASE;

    // constructor
    public ManejadorBBDDAndroid(Context context) {
        super(context, DATABASE, null, VERSION_BASEDATOS);
    }

    /**
     * inicializa la base de datos
     * db base de datos que se inicializa
     * @return devuelve falso si ocurrio un error
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREAR_TABLA_CONVERSACIONES_ABIERTAS);
        db.execSQL(CREAR_TABLA_DIR_ARRANQUE);
        db.execSQL(CREAR_TABLA_MENSAJES);
        db.execSQL(CREAR_TABLA_MISCELANEA);
        db.execSQL(CREAR_TABLA_CONTACTOS);
        db.execSQL(CREAR_TABLA_GRUPOS);
    }

    /**
     * al actualizar la aplicacion, se crea de nuevo la base de datos
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ELIMINA_TABLA_CONVERSACIONES_ABIERTAS);
        db.execSQL(ELIMINA_TABLA_DIR_ARRANQUE);
        db.execSQL(ELIMINA_TABLA_MISCELANEA);
        db.execSQL(ELIMINA_TABLA_MENSAJES);
        db.execSQL(ELIMINA_TABLA_CONTACTOS);
        db.execSQL(ELIMINA_TABLA_GRUPOS);
        onCreate(db);
    }

    /**
     * inicializa la base de datos
     *
     * @return devuelve falso si ocurrio un error
     */
    /*public boolean inicializarBD()
    {
        boolean ok = true;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement())
        {
            DatabaseMetaData meta = conn.getMetaData();
            // crea las tablas en una base de datos vacia
            // create a new table
            stmt.execute(CREAR_TABLA_GRUPOS);
            stmt.execute(CREAR_TABLA_CONTACTOS);
            stmt.execute(CREAR_TABLA_DIR_ARRANQUE);
            stmt.execute(CREAR_TABLA_MENSAJES);
            stmt.execute(CREAR_TABLA_MISCELANEA);
            stmt.execute(CREAR_TABLA_CONVERSACIONES_ABIERTAS);
        } catch (SQLException e)
        {
            if (logger.level <= Logger.WARNING) logger.logException("ManejadorBBDDAndroid.inicializarBD", e);
            ok = false;
        }
        return ok;
    }*/

    /*public boolean eliminaBD() throws Exception
    {
        boolean ok = true;
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement())
        {
            // elimina la base de datos existente
            stmt.execute(ELIMINA_TABLA_GRUPOS);
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
    }*/

    public void cerrarDB()
    {
        SQLiteDatabase db=getWritableDatabase();
        db.close();
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

        //try (Connection conn = this.connect();
        //     PreparedStatement pstmt = conn.prepareStatement(sql))

        SQLiteDatabase db=getReadableDatabase();
        if(db!=null)
        {
            try( Cursor rs = db.rawQuery(sql, new String[] {id, creador} )) {

                // loop through the result set
                while (rs.moveToNext()) {
                    String grupoCrifrado = rs.getString(0);
                    String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                    byte[] raw = Base64.getDecoder().decode(grupoBase64);
                    InputBuffer input = new SimpleInputBuffer(raw);
                    short tipo = input.readShort();
                    if (tipo == Grupo.TYPE)
                        grupo = new Grupo(input);
                    else {
                        throw new Exception("No se puede deserializar");
                    }
                }
            }
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
    public String obtenerNombreUsuarioQueEncriptoGrupo(Id id,String creador)
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
    public String obtenerNombreUsuarioQueEncriptoGrupo(String id, String creador)
    {

        String autor = null;

        String sql = "SELECT creador FROM " + TABLA_GRUPOS + " WHERE id=? and creador = ?";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor rs = db.rawQuery(sql, new String[] {id, creador} ))
        {
            while (rs.moveToNext())
            {
                autor = rs.getString(0);
            }

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
        SQLiteDatabase db=getReadableDatabase();

        try (Cursor rs = db.rawQuery(sql, new String[] {creador} ))
        {
            grupos = new ArrayList<>();
            // loop through the result set
            while (rs.moveToNext())
            {
                Grupo grupo;
                String grupoCrifrado = rs.getString(1);
                String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);
                InputBuffer input = new SimpleInputBuffer(raw);
                int tipo = input.readShort();
                if (tipo == Grupo.TYPE)
                    grupo = new Grupo(input);
                else {
                    throw new Exception("No se puede deserializar");
                }
                grupos.add(grupo);
            }

        }
        return grupos;
    }

    /**
     * Inserta un nuevo grupo en la base de datos
     *
     * @param grupo grupo que se va a insertar
     * @param creador usuario que inserta el grupo
     * @param clave Clave de cifrado de la informacion que va a ser guardada
     * @return devuelve falso si ocurrio algun error
     * @throws Exception
     */
    public boolean insertarGrupo(Grupo grupo, Key clave, String creador) throws Exception
    {

        String sql = "insert into " + TABLA_GRUPOS + " values (? ,? , ?)";
        SQLiteDatabase db=getWritableDatabase();

        long rowId=-1;
        if(db!=null) {
            String id = grupo.getId().toStringFull();

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(grupo.getType());
            grupo.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());
            String cifrado = ManejadorClaves.encrypt(serializado, clave);

            SQLiteStatement statement = db.compileStatement(sql);

            statement.bindString(1, id); // These match to the two question marks in the sql string
            statement.bindString(2, cifrado);
            statement.bindString(3, creador);

            rowId = statement.executeInsert();

        }

            return rowId!=-1;

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

        String sql = "replace into " + TABLA_GRUPOS + " values (?, ?, ?)";

        SQLiteDatabase db=getWritableDatabase();
        int affected = -1;

        if(db!=null)
        {
            SQLiteStatement pstmt = db.compileStatement(sql);
            pstmt.bindString(1, grupo.getId().toStringFull());

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(grupo.getType());
            grupo.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());


            pstmt.bindString(2, ManejadorClaves.encrypt(serializado, clave));
            pstmt.bindString(3, autor);
            affected=pstmt.executeUpdateDelete();
        }

        return affected==1;

    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un grupo
     *
     * @param id grupo que queremos borrar
     * @return devuelve falso si ocurrio un error
     */
    public boolean borrarGrupo(Id id,String creador)
    {
        return borrarGrupo(id.toStringFull(),creador);
    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un grupo
     *
     * @param id grupo que queremos borrar
     * @return devuelve falso si ocurrio un error
     */
    public boolean borrarGrupo(String id, String creador)
    {

        //String sql = "delete from " + TABLA_GRUPOS + " where id = ? and creador = ?";

        SQLiteDatabase db=getWritableDatabase();
        int affected = -1;

        if(db!=null)
        {
            affected=db.delete(TABLA_GRUPOS," id = ? and creador = ?",new String[]{id,creador});
        }
        return affected==1;

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
        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c= db.rawQuery(sql,new String[]{id,creador}))
        {
            // loop through the result set
            while (c.moveToNext())
            {
                String grupoCrifrado = c.getString(0);
                String grupoBase64 = ManejadorClaves.decrypt(grupoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);
                InputBuffer input = new SimpleInputBuffer(raw);
                int tipo = input.readShort();
                if (tipo == Contacto.TYPE)
                    contacto = new Contacto(input);
                else {
                    throw new Exception("No se puede deserializar el contenido");
                }
            }
        }
        return contacto;
    }

    /**
     * Obtiene el usuario que creo un contacto concreto en la base de datos
     *
     * @param id contacto del que queremos saber el creador
     * @return nombre de usuario del creador o null ni no existe el usuario
     */
    public String obtenerNombreUsuarioQueEncriptoContacto(Id id)
    {
        return obtenerNombreUsuarioQueEncriptoContacto(id.toStringFull());
    }

    /**
     * Obtiene el usuario que creo un contacto concreto en la base de datos
     *
     * @param id contacto del que queremos saber el creador
     * @return nombre de usuario del creador o null ni no existe el usuario
     */
    public String obtenerNombreUsuarioQueEncriptoContacto(String id)
    {
        String autor = null;

        String sql = "select creador from " + TABLA_CONTACTOS + " where id = ?";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{id}))
        {
            // loop through the result set
            while (c.moveToNext())
            {
                autor = c.getString(0);
            }
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

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{creador}))
        {
            grupos = new ArrayList<>();
            // loop through the result set
            while (c.moveToNext())
            {
                Contacto contacto;
                String contactoCrifrado = c.getString(1);
                String contactoBase64 = ManejadorClaves.decrypt(contactoCrifrado, clave);
                byte[] raw = Base64.getDecoder().decode(contactoBase64);

                InputBuffer input = new SimpleInputBuffer(raw);
                int tipo = input.readShort();
                if (tipo == Contacto.TYPE)
                    contacto = new Contacto(input);
                else {
                    throw new Exception("No se puede deserializar el contenido");
                }
                grupos.add(contacto);
            }
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

        String sql = "insert into " + TABLA_CONTACTOS + " values (? , ? , ? , ?)";

        SQLiteDatabase db=getWritableDatabase();

        long rowId=-1;
        if(db!=null) {
            String id = contacto.getId().toStringFull();

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(contacto.getType());
            contacto.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());
            String cifrado = ManejadorClaves.encrypt(serializado, clave);

            SQLiteStatement pstmt = db.compileStatement(sql);

            pstmt.bindString(1, id);
            pstmt.bindString(2, cifrado);
            pstmt.bindString(3, contacto.getAlias());
            pstmt.bindString(4, autor);
            rowId = pstmt.executeInsert();
        }
        return rowId!=1;

    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un contacto
     *
     * @param id contacto que queremos borrar
     * @return devuelve falso si ocurrio un error
     */
    public boolean borrarContacto(Id id, String creador)
    {
        return borrarContacto(id.toStringFull(),creador);
    }

    /**
     * Elimina de la base de datos la entrada correspondiente a un contacto
     *
     * @param id contacto que queremos borrar
     * @return devuelve falso si ocurrio un error
     */
    public boolean borrarContacto(String id, String creador)
    {
        //String sql = "delete from " + TABLA_CONTACTOS + " where id = ? and creador = ?";

        SQLiteDatabase db=getWritableDatabase();

        int affected = -1;

        if(db!=null)
        {
            affected=db.delete(TABLA_CONTACTOS," id = ? and creador = ? ",new String[]{id,creador});
        }
        return affected==1;

    }

    /**
     * Devuelve una relacion de id con el alias asociado al contacto
     * @param terminoBusqueda alias del que se desea obtener el id
     * @param creador quien creo el contacto
     * @return valores encontrados
     */
    public Map<String, String> buscaContacto(String terminoBusqueda, String creador)
    {
        Map<String, String> contactos = null;
        //        WHERE name LIKE '%"+terminoBusqueda+"%';

        String sql = "select id,alias from " + TABLA_CONTACTOS + " where alias like ? and creador=?";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{"%"+terminoBusqueda+"%",creador})) {
            contactos = new HashMap<>();
            // loop through the result set
            while (c.moveToNext()) {
                contactos.put(c.getString(0), c.getString(1));
            }
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
     */
    public boolean insertarDireccion(InetAddress ip, int puerto)
    {
        String sql = "insert or replace into " + TABLA_DIR_ARRANQUE + " values (null, ? , ?)";

        SQLiteDatabase db=getWritableDatabase();

        long rowId=-1;
        if(db!=null)
        {
            SQLiteStatement pstmt = db.compileStatement(sql);
            pstmt.bindString(1, ip.getHostAddress());
            pstmt.bindLong(2, puerto);

            rowId = pstmt.executeInsert();
        }
        return rowId!=-1;
    }

    /**
     * Obtiene todas las direcciones conocidas
     *
     * @return lista de objetos {@link InetSocketAddress} conocidos
     */
    public ArrayList<InetSocketAddress> getDireccionesDeArranque()
    {

        ArrayList<InetSocketAddress> dirs = null;

        String sql = "select ip,puerto from " + TABLA_DIR_ARRANQUE;

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{}))
        {

            dirs = new ArrayList<>();
            // loop through the result set
            while (c.moveToNext())
            {

                String ip = c.getString(0);
                int puerto = c.getInt(1);
                InetSocketAddress addr = new InetSocketAddress(ip, puerto);
                dirs.add(addr);
            }
        }

        return dirs;
    }

    /**
     * Elimina todas las direcciones que tenemos almacenadas. Normalmente es cuando
     * se ha iniciado la aplicacion satisfactoriamente y se van a actualizar las entradas
     *
     * @return verdad si no ocurrio ningun problema
     */
    public boolean vaciarDirecciones()
    {
        SQLiteDatabase db=getWritableDatabase();
        //String sql = "delete from " + TABLA_DIR_ARRANQUE;
        int affected = 0;

        if(db!=null)
        {
            affected = db.delete(TABLA_DIR_ARRANQUE,"1",new String[]{});
        }

        return affected != 0;
    }

    // -------------- fin operaciones sobre tabla dir_arranque -------------

    // -------------- operaciones sobre tabla mensajes -------------

    public boolean insertarMensaje(Mensaje mensaje, Key clave, String creador, String conversacion) throws Exception
    {
        //        SELECT * FROM (
        //                SELECT * FROM table ORDER BY id DESC LIMIT 50
        //        ) sub
        //        ORDER BY id ASC
        String sql = "insert into " + TABLA_MENSAJES + " values (null, ?, ?, ?, ?, ?)";


        SQLiteDatabase db=getReadableDatabase();
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());

        long rowId=-1;

        if(db!=null)
        {
            SQLiteStatement pstmt = db.compileStatement(sql);
            pstmt.bindString(1, df.format(mensaje.getFecha()));
            pstmt.bindLong(2, mensaje.getClase());

            SimpleOutputBuffer buf = new SimpleOutputBuffer();
            buf.writeShort(mensaje.getType());
            mensaje.serialize(buf);
            String serializado = Base64.getEncoder().encodeToString(buf.getBytes());

            pstmt.bindString(3, ManejadorClaves.encrypt(serializado, clave));
            pstmt.bindString(4, conversacion);
            pstmt.bindString(5, creador);

            rowId = pstmt.executeInsert();
        }
        return rowId!=-1;
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

        SQLiteDatabase db=getReadableDatabase();
        StringBuilder sqlBuilder = new StringBuilder().append(" select numero,mensaje from ")
                .append(TABLA_MENSAJES)
                .append(" where numero in ( select numero from ")
                .append(TABLA_MENSAJES).append(" where creador=? ");

        ArrayDeque<String> lista = new ArrayDeque<>();
            if (clase != -1)
                sqlBuilder.append(" and tipo=? ");
            if (conversacion != null)
                sqlBuilder.append(" and conversacion=? ");

            sqlBuilder.append(" order by numero desc limit ?-? offset ?) ")
                    .append(" order by numero asc ");

            String sql = sqlBuilder.toString();


        if (primero == 0 && ultimo == 0)
            ultimo++;

        int parameterIndex = 1;
        lista.addLast(creador);

        if (clase != -1)
            lista.addLast(String.valueOf(clase));

        if (conversacion != null)
            lista.addLast(conversacion);

        lista.addLast(String.valueOf(ultimo));
        lista.addLast(String.valueOf(primero));
        lista.addLast(String.valueOf(primero));


        String args[] = new String[lista.size()];
        args = lista.toArray(args);

        try (Cursor c = db.rawQuery(sql,args))
        {
            mensajes = new ArrayList<>();
            // loop through the result set
            while (c.moveToNext()) {
                String mensajeCifrado = c.getString(1);
                String grupoBase64 = ManejadorClaves.decrypt(mensajeCifrado, clave);
                byte[] raw = Base64.getDecoder().decode(grupoBase64);

                InputBuffer input = new SimpleInputBuffer(raw);
                Mensaje mensaje = null;

                int tipo = input.readShort();
                if (tipo == Mensaje.TYPE)
                    mensaje = new Mensaje(input);
                else {
                    throw new Exception("No se puede deserializar el contenido");
                }
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
     */
    public int getNumeroMensajesAlmacenados(String creador, String conversacion)
    {
        SQLiteDatabase db=getReadableDatabase();
        int almacenados = 0;

        String sql = "select count(numero) as almacenados from " + TABLA_MENSAJES +
                " where creador=? and conversacion=?";

        try (Cursor c = db.rawQuery(sql,new String[]{creador,conversacion}))
        {
            while(c.moveToNext()) {
                almacenados = c.getInt(0);
            }
        }

        return almacenados;
    }

    /**
     * Elimina los mensajes cuyos indices esten incluidos en la lista 'mensajes'
     *
     * @param creador la persona que inserto esos mensajes
     * @param conversacion de donde se quieren eliminar los mensajes
     * @return verdad si se ejecuto correctamente
     */
    public boolean borraMensajesDeConversacion(String creador, String conversacion)
    {
        SQLiteDatabase db=getWritableDatabase();
        int affected = 0;

        if(db!=null)
        {
            affected = db.delete(TABLA_MENSAJES," creador = ? and conversacion = ? "
                    ,new String[]{creador,conversacion});
        }

        return affected != 0;

    }

    /**
     * Devuelve la fecha en la que se guardo un mensaje
     *
     * @param mensaje
     * @return
     * @throws ParseException
     */
    public Date obtieneFechaDeMensaje(int mensaje) throws ParseException {
        Date fecha = null;

        String sql = "select fecha from " + TABLA_MENSAJES +
                " where numero=?";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{String.valueOf(mensaje)}))
        {
            DateFormat df = new SimpleDateFormat(FORMATO_FECHA,
                    Locale.getDefault());
            while(c.moveToNext()) {
                 fecha = df.parse(c.getString(0));
            }
        }

        return fecha;
    }

    // -------------- fin operaciones sobre tabla mensajes -------------
    // -------------- operaciones sobre tabla miscelaneo -------------

    /**
     * Devuelve el valor del atributo determinado por el nombre proporcionado
     * @param atributo
     * @return
     */
    public String getValor(String atributo)
    {
        String sql = "select valor from " + TABLA_MISCELANEA +
                " where atributo=?";
        String valor = "";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{atributo}))
        {
            while(c.moveToNext()) {
                valor = c.getString(0);
            }
        }

        return valor;
    }

    /**
     * Devuelve un {@link Map} con todas las parejas atributo - valor
     * @param creador
     * @return
     */
    public Map<String,String> getTodosLosValores(String creador)
    {
        //String sql = "select * from "+TABLA_MISCELANEA+ "where creador=?";
        String sql = "select atributo,valor from "+TABLA_MISCELANEA+" where creador=?";
        Map<String,String> valores=null;


        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{creador}))
        {
            valores = new HashMap<>();
            while(c.moveToNext()) {
                String atributo = c.getString(0);
                String valor = c.getString(1);
                valores.put(atributo,valor);
            }
        }

        return valores;

    }

    /**
     * Inserta o actualiza el valor de un atributo. Si no existe lo
     * crea, y si ya existe lo actualiza. Si la entrada es null, lo borra
     * @param atributo
     * @param valor
     * @return
     */
    public boolean setValor(String atributo, String valor)
    {

        String sql = "";
        long rowId=-1;
        boolean ret=false;
        SQLiteDatabase db=getWritableDatabase();
        if(db!=null)
        {
            if (valor == null) {
                //sql = "delete from " + TABLA_MISCELANEA + " where atributo=?";
                rowId=db.delete(TABLA_MISCELANEA,"atributo=?",new String[]{atributo});
                ret=rowId!=0;
            }
            else {
                sql = "insert or replace into " + TABLA_MISCELANEA + " values (?,?)";
                SQLiteStatement pstmt = db.compileStatement(sql);
                pstmt.bindString(1, atributo);
                pstmt.bindString(2, valor);
                rowId = pstmt.executeInsert();
                ret = rowId!=-1;
            }
        }

        return ret;

    }

    /**
     * Devuelve todos los valores que tienen la extension
     * {@link us.tfg.p2pmessenger.controller.Controlador#EXTENSION_CONVERSACION_PENDIENTE}
     * @return
     */
    public ArrayList<String> obtenerConversacionesPendiente()
    {
        ArrayList<String> contactos = null;
        //        WHERE name LIKE '%"+terminoBusqueda+"%';

        String sql = "select valor from " + TABLA_MISCELANEA + " where atributo like ?";
        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{"%"+ EXTENSION_CONVERSACION_PENDIENTE}))
        {
            contactos = new ArrayList<>();
            while(c.moveToNext()) {
                contactos.add(c.getString(0));
            }
        }

        return contactos;
    }

    /**
     * Devuelve los codigos que se generan para invitar a usuarios a un grupo
     * @param codigo
     * @return
     */
    public String obtenerCodigosUnirGrupo(String codigo)
    {
        //        WHERE name LIKE '%"+terminoBusqueda+"%';

        String sql = "select atributo from " + TABLA_MISCELANEA + " where valor = ?";
        SQLiteDatabase db=getReadableDatabase();
        String idGrupo="";

        try (Cursor c = db.rawQuery(sql,new String[]{codigo}))
        {
            while(c.moveToNext()) {
                idGrupo = c.getString(0);
            }
        }

        return idGrupo;
    }

    // -------------- fin operaciones sobre tabla miscelaneo -------------

    // -------------- operaciones sobre tabla conversaciones abiertas -------------

    /**
     * Inerta una nueva conversacion
     * @param id
     * @param alias
     * @param creador
     * @param tipo
     * @return
     */
    public boolean insertarConversacionAbierta(String id, String alias, String creador, int tipo)
    {
        String sql = "insert or replace into " + TABLA_CONVERSACIONES_ABIERTAS + " values (?,?,?,?,?)";
        long rowId=-1;

        SQLiteDatabase db=getWritableDatabase();
        if(db!=null) {
            SQLiteStatement pstmt = db.compileStatement(sql);
            pstmt.bindLong(1, new Date().getTime());
            pstmt.bindString(2, id);
            pstmt.bindString(3, creador);
            pstmt.bindString(4, alias);
            pstmt.bindLong(5, tipo);

            rowId = pstmt.executeInsert();
        }
        return rowId!=-1;
    }

    /**
     * Obtiene la lista de conversaciones abiertas que pertenecen a un
     * usuario concreto
     * @param creador
     * @return
     */
    public ArrayList<Conversacion> obtenerConversacionesAbiertas(String creador)
    {
        ArrayList<Conversacion> conversaciones;
        String sql = "select id,fecha,alias,tipo from " + TABLA_CONVERSACIONES_ABIERTAS +
                " where creador=? order by fecha desc";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{creador}))
        {
            conversaciones = new ArrayList<>();
            while(c.moveToNext()) {
                String idString = c.getString(0);
                Id id = rice.pastry.Id.build(idString);
                long milis = c.getLong(1);
                String alias = c.getString(2);
                int tipo = c.getInt(3);
                Date fecha = new Date(milis);

                Conversacion conv = new Conversacion(id, fecha, alias, tipo);
                conversaciones.add(conv);
            }
        }

        return conversaciones;
    }

    /**
     * Obtiene la conversacion definida por un {@link Id} que pertenezca
     * al usuario proporcionado
     * @param id
     * @param creador
     * @return
     */
    public Conversacion obtenerConversacionAbierta(String id, String creador)
    {
        Conversacion devolver = null;
        String sql = "select fecha,alias,tipo from " + TABLA_CONVERSACIONES_ABIERTAS +
                " where creador=? and id=?";

        SQLiteDatabase db=getReadableDatabase();

        try (Cursor c = db.rawQuery(sql,new String[]{creador,id}))
        {
            while(c.moveToNext()) {
                long milis = c.getInt(0);
                String alias = c.getString(1);
                int tipo = c.getInt(2);
                devolver = new Conversacion(rice.pastry.Id.build(id), new Date(milis), alias, tipo);
            }
        }

        return devolver;
    }

    /**
     * Elimina una conversacion abierta proporcionando el {@link Id}
     * y el creador de la misma
     * @param id
     * @param creador
     * @return
     */
    public boolean borrarConversacionAbierta(String id, String creador)
    {
        //String sql = "delete from " + TABLA_CONVERSACIONES_ABIERTAS + " where id = ? and creador=?";

        long rowId=-1;
        boolean ret=false;
        SQLiteDatabase db=getWritableDatabase();

        if(db!=null) {
            //sql = "delete from " + TABLA_MISCELANEA + " where atributo=?";
            rowId = db.delete(TABLA_CONVERSACIONES_ABIERTAS, "id = ? and creador=?", new String[]{id, creador});
        }

        return rowId!=0;

    }

}
