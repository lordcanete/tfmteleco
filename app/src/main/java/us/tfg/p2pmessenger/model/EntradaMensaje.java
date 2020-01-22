package us.tfg.p2pmessenger.model;

import java.io.IOException;
import java.io.Serializable;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawSerializable;

/**
 * Entrada de mensaje. Cada vez que alguien quiere enviar un
 * mensaje importante, crea un objeto de esta clase, le inserta
 * el mensaje cifrado y adjunta la clave con la que cifro, de
 * esta manera, el receptor puede descifrar aunque no haya hablado
 * nunca con ese otro usuario (o si esta desconectado)
 */
public class EntradaMensaje implements RawSerializable, Serializable
{
    static final long serialVersionUID = 6161880273780314501L;

    /**
     * Mensaje almacenado en la entrada
     */
    private MensajeCifrado mensaje;

    /**
     * Clave simetrica con la que se cifro el mensaje
     */
    private String claveSimetricaCifrada;

    /**
     * Id de la persona que envio el mensaje
     */
    private Id remitente;

    public static final short TYPE = 322;

    /**
     * Constructor de la clase
     * @param mensaje
     * @param claveSimetricaCifrada
     * @param remitente
     */
    public EntradaMensaje(MensajeCifrado mensaje, String claveSimetricaCifrada, Id remitente)
    {
        this.mensaje = mensaje;
        this.claveSimetricaCifrada = claveSimetricaCifrada;
        this.remitente= remitente;
    }

    /**
     * Constructor que deserializa desde la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public EntradaMensaje(InputBuffer buf) throws IOException
    {
        if (buf.readShort() == rice.pastry.Id.TYPE)
            this.remitente = rice.pastry.Id.build(buf);
        else
            throw new IOException("Error al leer el remitente. Los tipos no coinciden");

        this.claveSimetricaCifrada = buf.readUTF();

        if (buf.readShort() == MensajeCifrado.TYPE)
            this.mensaje = new MensajeCifrado(buf);
        else
            throw new IOException("Error al leer el mensaje. Los tipos no coinciden");
    }

    public Id getRemitente()
    {
        return remitente;
    }

    public EntradaMensaje setRemitente(Id remitente)
    {
        this.remitente = remitente;
        return this;
    }

    public MensajeCifrado getMensaje()
    {
        return mensaje;
    }

    public EntradaMensaje setMensaje(MensajeCifrado mensaje)
    {
        this.mensaje = mensaje;
        return this;
    }

    public String getClaveSimetricaCifrada()
    {
        return claveSimetricaCifrada;
    }

    public EntradaMensaje setClaveSimetricaCifrada(String claveSimetricaCifrada)
    {
        this.claveSimetricaCifrada = claveSimetricaCifrada;
        return this;
    }

    public short getType()
    {
        return this.TYPE;
    }

    /**
     * Metodo toString()
     */
    @Override
    public String toString()
    {
        return "Entrada: origen " + remitente + " clave = <"
                + claveSimetricaCifrada + "> | " + mensaje + "";
    }

    /**
     * Metodo equals()
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        boolean devolver = false;
        if (o!=null&&o instanceof EntradaMensaje)
        {
            EntradaMensaje comparable = (EntradaMensaje) o;
            if ((comparable.getRemitente().equals(this.remitente))
                    && (comparable.getMensaje().equals(this.mensaje))
                    && (comparable.getClaveSimetricaCifrada().equals(this.claveSimetricaCifrada)))
            {
                devolver=true;
            }

        }
        return devolver;
    }

    /**
     * Serializa a la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @throws Exception
     */
    @Override
    public void serialize(OutputBuffer out) throws IOException
    {
        out.writeShort(remitente.getType());
        remitente.serialize(out);

        out.writeUTF(claveSimetricaCifrada);
        out.writeShort(mensaje.getType());
        mensaje.serialize(out);
    }
}
