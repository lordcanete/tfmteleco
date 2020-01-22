package us.tfg.p2pmessenger.model;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.crypto.SecretKey;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import us.tfg.p2pmessenger.controller.ManejadorClaves;

import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Contiene los usuarios que pertenecen al grupo,
 * quien lo creo (el lider), el nombre, la fecha en
 * la que se creo y el certificado del grupo. Tambien
 * tiene la clave simetrica del grupo pero encriptada
 * con el certificado. Para que cuando alguien se
 * conecte desencripte con la clave privada y
 * comience a recibir los mensajes del grupo.
 */
public class Grupo
{
    static final long serialVersionUID = -265382007053619834L;

    /**
     * Componentes del grupo
     */
    private ArrayList<Usuario> componentes;

    /**
     * id del grupo
     */
    private Id idGrupo;

    /**
     * Nombre del grupo
     */
    private String nombre;
    //    private Id fotoGrupo;

    /**
     * Id del usuario que creo el grupo
     */
    private Id lider;

    /**
     * fecha de creacion del grupo
     */
    private Date fechaCreacion;

    /**
     * enlace a donde se debe almacenar el contenido multimedia
     */
    private String enlaceAServicio;


    private int version;

    /**
     * Certificado del grupo
     */
    private Key certificado;

    /**
     * La clavePrivada simetrica esta encriptada con el certificado
     */
    private String claveSimetricaCifrada;

    //
    /**
     * Primera direccion de pastry para empezar a leer los mensajes
     */
    private Id bloqueMensajesImportantes;

    //
    /**
     * Clave simetrica sin encriptar
     */
    private transient SecretKey claveSimetrica;

    //
    /**
     *  Pareja asimetrica para facilitar la creacion y busqueda del objeto
     */
    private transient Key clavePrivada;

    /**
     * Entrada privada con la clave privada y el certificado.
     * Este atributo es utilizado cuando se genera el grupo
     */
    private transient KeyStore.PrivateKeyEntry entrada;

    public static final short TYPE=316;


    public Grupo()
    {
    }

    /**
     * Constructor de la clase
     * @param id
     * @param name
     * @param lider
     */
    public Grupo(Id id, String name,Usuario lider) {
        this.idGrupo=id;
        this.nombre=name;
        this.componentes=new ArrayList<>();
        lider.setPrivateKeyEntry(null);
        this.componentes.add(lider);
        this.lider=lider.getId();
        this.fechaCreacion=new Date();
        this.enlaceAServicio="";
        this.version=1;
        this.bloqueMensajesImportantes=null;
    }

    /**
     * Constructor que deserializa desde la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public Grupo(InputBuffer buf)
            throws IOException, ParseException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException
    {
        // construir grupo a partir de buffer

        short tipo=buf.readShort();
        if(rice.pastry.Id.TYPE==tipo)
            this.idGrupo=rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");
        this.nombre=buf.readUTF();
        this.version=buf.readInt();
        //        tipo=buf.readShort();
        //        this.fotoGrupo=rice.pastry.Id.build(buf);
        tipo=buf.readShort();
        if(rice.pastry.Id.TYPE==tipo)
            this.lider=rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");
        int num=buf.readInt();

        this.componentes=new ArrayList<>(num);

        Usuario usr;
        for(int i=0;i<num;i++)
        {
            usr=new Usuario(buf);
            this.componentes.add(usr);
        }

        DateFormat df = new SimpleDateFormat(FORMATO_FECHA,Locale.getDefault());

        this.fechaCreacion= df.parse(buf.readUTF());
        this.enlaceAServicio=buf.readUTF();
        String tipoCert=buf.readUTF();
        int tam=buf.readInt();
        byte[] key= new byte[tam];
        buf.read(key);
        this.certificado= ManejadorClaves.leeClavePublica(key,tipoCert);
        this.claveSimetricaCifrada=buf.readUTF();

        tipo=buf.readShort();
        if(rice.pastry.Id.TYPE==tipo)
            this.bloqueMensajesImportantes=rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id del primer bloque de mensajes");
    }

    /**
     * metodo toString()
     */
    @Override
    public String toString() {
        StringBuilder list= new StringBuilder();
        Iterator<Usuario> iterador=componentes.iterator();

        Usuario integrante=iterador.next();
        if(integrante.getId().equals(lider))
            list.append("||"+integrante.getNombre()+"||");
        else
            list.append(integrante.getNombre());

        for (;iterador.hasNext();)
        {
            integrante=iterador.next();
            if(integrante.getId().equals(lider))
                list.append(", ||"+integrante.getNombre()+"||");
            else
                list.append(", ").append(integrante.getNombre());
        }
        return "Grupo: "+this.nombre+" [ "+list.toString()+" ]\n\tid =" +this.idGrupo.toStringFull()+"" +
                " - Primeros mensajes importantes en "+bloqueMensajesImportantes;
    }

    /**
     * Inserta un nuevo usuario en el grupo
     * @param integrante
     * @return
     */
    public Grupo insertar(Usuario integrante)
    {
        integrante.setPrivateKeyEntry(null);
        this.componentes.add(integrante);
        this.version++;
        return this;
    }

    /**
     * Borra a un integrante a partir del nombre de usuario
     * @param integrante
     * @return
     */
    public Grupo borrar(Usuario integrante)
    {
        int index=this.componentes.indexOf(integrante);
        if(index!=-1){
            this.componentes.remove(index);
            this.version++;
        }
        return this;
    }

    /**
     * Devuelve la lista completa de usuarios en el grupo
     * @return
     */
    public ArrayList<Usuario> getComponentes()
    {
        return this.componentes;
    }

    public String getNombre()
    {
        return this.nombre;
    }

    public Grupo setNombre(String nombre)
    {
        this.nombre=nombre;
        this.version++;
        return this;
    }

    public Id getIdLider()
    {
        return this.lider;
    }

    public Grupo setIdLider(Id lider)
    {
        this.lider=lider;
        return this;
    }

    public Usuario getLider()
    {
        Usuario usuarioLider=null;
        for(Usuario integrante:this.componentes)
        {
            if(integrante.getId().equals(this.lider))
                usuarioLider=integrante;
        }
        return usuarioLider;
    }

    public Grupo setLider(Id lider) throws Exception
    {
        int posicion=-1;
        for(Iterator<Usuario> iterador=componentes.iterator();iterador.hasNext();posicion++)
        {
            Usuario usr=iterador.next();
            if(usr.getNombre().equals(lider))
            {
                this.lider=lider;
            }

        }
        if(posicion==-1)
        {
            throw new Exception("Esta intentando establecer como lider a un usuario que" +
                    " no pertenece al grupo");
        }
        else
        {
            this.version++;
            this.lider = lider;
        }
        return this;
    }

    public Id getId()
    {
        return this.idGrupo;
    }

    public Date getFechaCreacion()
    {
        return this.fechaCreacion;
    }

    public Grupo setCertificado(PublicKey certificado)
    {
        this.certificado = certificado;
        return this;
    }

    public Key getCertificado()
    {
        return this.certificado;
    }

    public Grupo setClavePrivada(Key clavePrivada)
    {
        this.clavePrivada = clavePrivada;
        return this;
    }

    public Key getClavePrivada()
    {
        return this.clavePrivada;
    }

    public String getClaveSimetricaCifrada()
    {
        return this.claveSimetricaCifrada;
    }

    public Grupo setClaveSimetricaCifrada(String claveSimetricaCifrada)
    {
        this.claveSimetricaCifrada = claveSimetricaCifrada;
        return this;
    }

    public Grupo setPrivateKeyEntry(KeyStore.PrivateKeyEntry entrada)
    {
        this.entrada=entrada;
        return this;
    }

    public KeyStore.PrivateKeyEntry getPrivateKeyEntry()
    {
        KeyStore.PrivateKeyEntry entrada=this.entrada;
        this.entrada=null;
        return entrada;
    }

    public SecretKey getClaveSimetrica()
    {
        return claveSimetrica;
    }

    public Grupo setClaveSimetrica(SecretKey claveSimetrica)
    {
        this.claveSimetrica = claveSimetrica;
        return this;
    }

    public int getVersion()
    {
        return version;
    }

    public Grupo setVersion(int version)
    {
        this.version = version;
        return this;
    }

    public short getType()
    {
        return this.TYPE;
    }

    public Id getBloqueMensajesImportantes()
    {
        return bloqueMensajesImportantes;
    }

    public Grupo setBloqueMensajesImportantes(Id bloqueMensajesImportantes)
    {
        this.bloqueMensajesImportantes = bloqueMensajesImportantes;
        return this;
    }

    /**
     * Serializa a la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public void serialize(OutputBuffer buf) throws Exception
    {
        //serializador de grupo (antes de encriptar)

        buf.writeShort(idGrupo.getType());
        idGrupo.serialize(buf);
        buf.writeUTF(nombre);
        buf.writeInt(version);
        //buf.writeShort(fotoGrupo.getType());
        //fotoGrupo.serialize(buf);
        buf.writeShort(lider.getType());
        lider.serialize(buf);

        buf.writeInt(this.componentes.size());
        Usuario usr;
        for(Iterator<Usuario> iterador=getComponentes().iterator();iterador.hasNext();)
        {
            usr=iterador.next();
            usr.serialize(buf);
        }

        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());

        buf.writeUTF(df.format(fechaCreacion));
        buf.writeUTF(enlaceAServicio);
        buf.writeUTF(certificado.getAlgorithm());
        byte[] key=certificado.getEncoded();
        buf.writeInt(key.length);
        buf.write(key,0,key.length);
        if(claveSimetricaCifrada!=null)
            buf.writeUTF(claveSimetricaCifrada);
        else if(claveSimetrica!=null)
        {
            this.claveSimetricaCifrada=ManejadorClaves.encriptaClaveSimetrica
                    (this.claveSimetrica,certificado);
            buf.writeUTF(this.claveSimetricaCifrada);
        }
        else
            throw new Exception("No se encuentra la clave simetrica");

        buf.writeShort(bloqueMensajesImportantes.getType());
        bloqueMensajesImportantes.serialize(buf);
    }

    public Grupo setComponentes(ArrayList<Usuario> componentes) {
        this.componentes = componentes;
        return this;
    }

    public Id getIdGrupo() {
        return idGrupo;
    }

    public Grupo setIdGrupo(Id idGrupo) {
        this.idGrupo = idGrupo;
        return this;
    }

    public Grupo setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
        return this;
    }

    public String getEnlaceAServicio() {
        return enlaceAServicio;
    }

    public Grupo setEnlaceAServicio(String enlaceAServicio) {
        this.enlaceAServicio = enlaceAServicio;
        return this;
    }

    public Grupo setCertificado(Key certificado) {
        this.certificado = certificado;
        return this;
    }
}


