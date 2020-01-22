package us.tfg.p2pmessenger.model;


import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastException;
import rice.p2p.past.gc.GCPast;
import rice.p2p.past.gc.GCPastContent;
import rice.p2p.past.gc.GCPastContentHandle;
import rice.p2p.past.gc.GCPastMetadata;
import us.tfg.p2pmessenger.controller.ManejadorClaves;

import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Clase que mantiene un usuario de la aplicacion. Tiene un nombre
 * de usuario que es globalmente unico, y a partir del cual se puede
 * obtener el id asociado para referirse a el dentro de pastry.
 * <p>
 * Este objeto no podra ser modificado
 */
public class Usuario implements GCPastContent {
    public static final short TYPE = 314;
    static final long serialVersionUID = 2313803750563202340L;

    /**
     * utilizado para hacer mas sencilla la tarea
     * de busqueda de un usuario particular
     */
    private boolean vacio;

    /**
     * nombre es globalmente unico, y a partir de el se puede obtener el id
     */
    private String nombre;

    /**
     * obtenido a partir del nombre como PastryIdFactory.build(nombre)
     */
    private Id id;

    /**
     * Fecha en la que se creo el usuario
     */
    private Date fechaDeRegistro;

    /**
     * Numero de version del objeto, como no es mutable, siempre tendra version 1
     */
    private long version;

    private GCPastMetadata metadata;

    private long expiration;

    /**
     * Clave publica (certificado) del usuario, para validar su autenticidad
     */
    private X509Certificate certificado;

    /**
     * id primer bloque con los mensajes importantes
     */
    private Id bloqueMensajesImportantes;

    /**
     * Entrada del almacen de claves correspondientes al usuario, campo utilizado
     * al crear el usuario y manejarlo pero debe quedar luego vacio
     */
    private transient KeyStore.PrivateKeyEntry entrada;
    private transient Key clavePrivada;

    /**
     * Se utilizara para cuando se ha iniciado sesion en un dispositivo
     */
    private transient Key claveSimetrica;

    /**
     * Constructor por defecto
     */
    public Usuario() {
        this.vacio = true;
    }

    /**
     * Constructor de la clase
     *
     * @param id
     * @param nombre
     * @param certificado
     */
    public Usuario(Id id, String nombre, X509Certificate certificado) {
        this.id = id;
        this.nombre = nombre;
        this.version = 0;
        this.vacio = false;
        this.certificado = certificado;
        this.clavePrivada = null;
        this.fechaDeRegistro = new Date();
        this.bloqueMensajesImportantes = null;
    }

    /**
     * Constructor que deserializa desde la forma raw de pastry
     *
     * @param buf
     * @throws Exception
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     */
    public Usuario(InputBuffer buf)
            throws IOException, ParseException, CertificateException {
        short tipo = buf.readShort();
        if (rice.pastry.Id.TYPE == tipo)
            this.id = rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");
        this.nombre = buf.readUTF();
        this.version = 0;
        this.vacio = false;
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());
        this.fechaDeRegistro = df.parse(buf.readUTF());
        String tipoCertificado = buf.readUTF();
        String cert = buf.readUTF();
        this.certificado = ManejadorClaves.leeCertificado(cert, tipoCertificado);
        if (buf.readBoolean()) {
            tipo = buf.readShort();
            if (rice.pastry.Id.TYPE == tipo)
                this.bloqueMensajesImportantes = rice.pastry.Id.build(buf);
            else
                throw new IOException("No se puede leer el id del primer bloque de mensajes");
        }
    }

    public Date getFechaDeRegistro() {
        return fechaDeRegistro;
    }

    public Key getPrivateKey() {
        return this.clavePrivada;
    }

    public Usuario setPrivateKey(Key entrada) {
        this.clavePrivada = entrada;
        return this;
    }

    public X509Certificate getCertificado() {
        return this.certificado;
    }

    public Key getClaveSimetrica() {
        return this.claveSimetrica;
    }

    public Usuario setClaveSimetrica(Key claveSimetrica) {
        this.claveSimetrica = claveSimetrica;
        return this;
    }

    public Usuario setPrivateKeyEntry(KeyStore.PrivateKeyEntry entrada) {
        this.entrada = entrada;
        return this;
    }

    public KeyStore.PrivateKeyEntry getPrivateKeyEntry() {
        KeyStore.PrivateKeyEntry entry = this.entrada;
        this.entrada = null;
        return entry;
    }

    public boolean estaVacio() {
        return this.vacio;
    }

    public Id getBloqueMensajesImportantes() {
        return bloqueMensajesImportantes;
    }

    public Usuario setBloqueMensajesImportantes(Id bloqueMensajesImportantes) {
        this.bloqueMensajesImportantes = bloqueMensajesImportantes;
        return this;
    }

    @Override
    public String toString() {
        if (vacio)
            return "Usuario vacio";
        else
            return "Usuario: " + nombre + " con id " + id + " registrado el " + fechaDeRegistro;
    }

    @Override
    public boolean equals(Object o) {
        boolean iguales = false;
        if (o != null && o instanceof Usuario) {
            Usuario comparar = (Usuario) o;
            if (comparar.nombre != null &&
                    comparar.nombre.equals(this.nombre) &&
                    comparar.id != null &&
                    comparar.id.equals(this.id) &&
                    comparar.fechaDeRegistro.equals(this.fechaDeRegistro)) {
                try {
                    iguales = comparar.certificado.getEncoded().equals(this.certificado.getEncoded());
                } catch (CertificateEncodingException ignore) {
                    iguales = false;
                }
                iguales = iguales && comparar.bloqueMensajesImportantes.equals(this.bloqueMensajesImportantes);
            }
        }
        return iguales;
    }

    public String toStringFull() {
        return "Usuario = " + nombre + " Id = " + id.toStringFull() +
                " Fecha de registro = " + fechaDeRegistro;
    }

    /*
            @Override
            public short getType()
            {
            return TYPE;
            }

            @Override
            public void serialize(OutputBuffer buf) throws IOException
            {
                try
                {
                private boolean vacio;
                private String nombre;
                private Id id;
                private Date fechaDeRegistro;
                private int version;
                private GCPastMetadata metadata;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ObjectOutputStream oos = new ObjectOutputStream(baos);

            // write out object and find its length
            oos.writeObject(id);
            oos.writeObject();
            oos.close();

            byte[] temp = baos.toByteArray();


            buf.writeInt(temp.length);
            buf.write(temp, 0, temp.length);
            //    System.out.println("JavaSerializedGCPastContent.serialize() "+content+" length:"+temp.length);
            //    new Exception("Stack Trace").printStackTrace();
        }

    }

     */

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public GCPastContentHandle getHandle(GCPast local, long expiration) {
        this.expiration = expiration;
        return new ManejadorUsuario(local.getLocalNodeHandle(), getId(), getVersion(), expiration);
    }

    /**
     * Cuando se inserta un objeto en pastry, se comprueba si
     * es valido hacerlo a traves de este metodo.
     * Solo puede ser insertado en un objeto vacio. Si
     * se intenta sobreescribir, devuelve excepcion.
     * Para insertarlo debe tener todos los campos
     * rellenos salvo los correspondientes a las claves
     * privadas y simetricas
     *
     * @param id              the key identifying the object
     * @param existingContent
     * @return
     * @throws PastException
     */
    @Override
    public PastContent checkInsert(Id id, PastContent existingContent) throws PastException {
        // no se pueden sobreescribir estos objetos
        if (existingContent != null) {
            throw new PastException("Usuario.checkInsert: El usuario que intenta anadir ya existe");
        }

        // only allow correct content hash key
        if (!id.equals(getId())) {
            throw new PastException("Usuario.checkInsert: El usuario que intenta anadir ya existe");
        }

        if (this.certificado == null)
            throw new PastException("Usuario.checkInsert: El certificado no puede estar vacio");

        if (this.clavePrivada != null)
            throw new PastException("Usuario.checkInsert: La clave privada no debe almacenarse");

        if (this.bloqueMensajesImportantes == null)
            throw new PastException("Usuario.checkInsert: La direccion del primer bloque de " +
                    "mensajes no puede ser nula");

        this.fechaDeRegistro = new Date();
        return this;
    }

    @Override
    public GCPastMetadata getMetadata(long expiration) {
        this.metadata = new GCPastMetadata(expiration);
        return metadata;
    }

    @Override
    public PastContentHandle getHandle(Past local) {
        return new ManejadorUsuario(local.getLocalNodeHandle(), getId(), getVersion(), this.expiration);
    }

    @Override
    public Id getId() {
        return this.id;
    }

    public Usuario setId(Id id)
    {
        this.id=id;
        return this;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    public String getNombre() {
        return nombre;
    }

    public Usuario setNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public Usuario setVacio(boolean vacio) {
        this.vacio = vacio;
        return this;
    }

    public Usuario setFechaDeRegistro(Date fechaDeRegistro) {
        this.fechaDeRegistro = fechaDeRegistro;
        return this;
    }

    public Usuario setVersion(long version) {
        this.version = version;
        return this;
    }

    public Usuario setCertificado(X509Certificate certificado) {
        this.certificado = certificado;
        return this;
    }


    public void rellenaCampos(Usuario entrada)
    {
        this.vacio=false;
        this.fechaDeRegistro=entrada.fechaDeRegistro;
        this.version=entrada.version;
        this.certificado=entrada.certificado;
        this.clavePrivada =entrada.clavePrivada;
        this.expiration=entrada.expiration;
        this.id=entrada.id;
        this.nombre=entrada.nombre;
        this.bloqueMensajesImportantes=entrada.bloqueMensajesImportantes;

    }

    public short getType() {
        return this.TYPE;
    }

    /**
     * Serializa el objeto a la forma raw de pastry
     *
     * @param buf
     * @throws Exception
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     */
    public void serialize(OutputBuffer buf) throws IOException {
        buf.writeShort(id.getType());
        id.serialize(buf);
        buf.writeUTF(this.nombre);
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());
        buf.writeUTF(df.format(this.fechaDeRegistro));
        buf.writeUTF(certificado.getType());
        try {
            buf.writeUTF(ManejadorClaves.imprimeCertificado(certificado));
        } catch (CertificateEncodingException e) {
            throw new IOException("Error al codificar el certificado");
        }
        buf.writeBoolean(bloqueMensajesImportantes != null);
        if (bloqueMensajesImportantes != null) {
            buf.writeShort(bloqueMensajesImportantes.getType());
            bloqueMensajesImportantes.serialize(buf);
        }
    }

}
