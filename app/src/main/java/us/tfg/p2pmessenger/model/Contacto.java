package us.tfg.p2pmessenger.model;

import java.io.IOException;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;

/**
 * Clase contacto. Contiene un usuario y el alias
 * que se le asigna a ese usuario. El alias tiene
 * sentido local al usuario que lo guardo.
 */
public class Contacto
{
    static final long serialVersionUID = 2991527880361551469L;

    /**
     * nombre del contacto
     */
    private String alias;

    /**
     * Si tiene algun mensaje pendiente de leer
     */
    transient boolean pendiente;

    /**
     * Usuario representado por el alias
     */
    private Usuario usuario;
    public static final short TYPE=320;

    /**
     * Constructor
     * @param alias
     * @param usuario
     */
    public Contacto(String alias,Usuario usuario)
    {
        this.usuario=usuario;
        this.alias=alias;
        this.pendiente=false;
    }

    /**
     * Constructor que deserializa desde la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public Contacto(InputBuffer buf) throws Exception
    {
        short tipo=buf.readShort();
        if(tipo==Usuario.TYPE)
            this.usuario=new Usuario(buf);
        else
            throw new Exception("No se puede deserializar el contenido");
        this.alias=buf.readUTF();
    }

    public String getAlias()
    {
        return alias;
    }

    public Contacto setAlias(String alias)
    {
        this.alias = alias;
        return this;
    }

    public Usuario getUsuario()
    {
        return usuario;
    }

    public Contacto setUsuario(Usuario usuario)
    {
        this.usuario = usuario;
        return this;
    }

    public Id getId()
    {
        return this.usuario.getId();
    }



    public short getType()
    {
        return this.TYPE;
    }

    /**
     * Metodo de serializacion RAW de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent

     * @param buf
     * @throws IOException
     */
    public void serialize(OutputBuffer buf) throws IOException
    {
        buf.writeShort(usuario.TYPE);
        usuario.serialize(buf);
        buf.writeUTF(alias);
    }

    /**
     * Metodo toString()
     * @return
     */
    public String toString()
    {
        return "Contacto: alias = "+alias+"\n\tusuario = "+usuario;
    }

}
