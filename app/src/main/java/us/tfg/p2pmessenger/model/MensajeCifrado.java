package us.tfg.p2pmessenger.model;

import java.io.IOException;
import java.security.Key;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import us.tfg.p2pmessenger.controller.ManejadorClaves;
import us.tfg.p2pmessenger.util.Base64;

/**
 * Envoltorio de la clase mensaje para
 * facilitar el encriptado y transmision
 * por la red.
 */
public class MensajeCifrado implements Message, ScribeContent
{
    static final long serialVersionUID = 2298861684921217418L;

    private int priority;

    public static final short TYPE = 321;

    /**
     * {@link Id} del origen del mensaje para que se puedan enviar
     * de vuelta mensajes de host unreachable y echo request / reply
     * sin tener que serializar dentro del mensaje un objeto mensaje
     */
    private Id origen;

    /**
     * Si envuelve a un mensaje cifrado, contiene la version encriptada
     * y serializada del mensaje. En otro caso, contiene contenido
     * sin cifrado
     */
    private String contenido;

    /**
     * Tipo del mensaje. Facilita a la hora de procesarlo antes
     * de desencriptar. Si se desencriptara siempre, en los
     * casos en los que la carga del mensaje fuera en texto claro,
     * se lanzaria una excepcion y no nos permitiria continuar.
     */
    private int clase;


    /**
     * Constructor de la clase. Construye el objeto a partir
     * de un {@link Mensaje} y la clave simetrica.
     * @param mensaje
     * @param claveSimetrica
     * @throws Exception
     */
    public MensajeCifrado(Mensaje mensaje, Key claveSimetrica) throws Exception
    {
        this.priority = mensaje.getPriority();
        this.clase = mensaje.getClase();
        SimpleOutputBuffer out = new SimpleOutputBuffer();
        mensaje.serialize(out);
        String contenidoBase64 = Base64.getEncoder().encodeToString(out.getBytes());
        this.contenido = ManejadorClaves.encrypt(contenidoBase64, claveSimetrica);
        this.origen = mensaje.getOrigen();
    }

    /**
     * Para los mensajes con texto claro que no contienen informacion
     * sensible
     * @param origen
     * @param contenido
     * @param priority
     * @param clase
     */
    public MensajeCifrado(Id origen, String contenido, int priority, int clase)
    {
        this.origen = origen;
        this.contenido = contenido;
        this.priority = priority;
        this.clase = clase;
    }

    /**
     * Constructor que deserializa desde la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public MensajeCifrado(InputBuffer buf) throws IOException
    {
        this.priority = buf.readInt();
        this.clase = buf.readInt();
        short tipo = buf.readShort();
        if (rice.pastry.Id.TYPE == tipo)
            this.origen = rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");
        this.contenido = buf.readUTF();
    }

    /**
     * Con la clave simetrica devuelve el objeto mensaje
     * que hay encriptado, desencriptado
     * @param claveSimetrica
     * @return
     * @throws Exception
     */
    public Mensaje desencripta(Key claveSimetrica) throws Exception
    {
        Mensaje mensaje = null;
        String contenidoBase64 = ManejadorClaves.decrypt(contenido, claveSimetrica);
        byte[] contenidoByte = Base64.getDecoder().decode(contenidoBase64);

        InputBuffer in = new SimpleInputBuffer(contenidoByte);
        mensaje = new Mensaje(in);
        return mensaje;

    }

    /**
     * Metodo toString
     * @return
     */
    @Override
    public String toString()
    {
        return "Mensaje cifrado (" + this.clase + "):" + this.contenido;
    }

    /**
     * Metodo equals()
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        /*
         private int priority;

    public static final short TYPE=321;

    //para que se puedan enviar mensajes de host unreachable y echo request / reply
    private Id origen;

    private String contenido;

    private int clase;
         */
        boolean devolver = false;
        if (o != null && o instanceof MensajeCifrado)
        {
            MensajeCifrado comparar= (MensajeCifrado) o;
            devolver = comparar.clase==this.clase&&
                    comparar.origen.equals(this.origen)&&
                    comparar.contenido.equals(this.contenido)&&
                    comparar.priority==this.priority;

        }

        return devolver;
    }

    @Override
    public int getPriority()
    {
        return this.priority;
    }


    public short getType()
    {
        return this.TYPE;
    }

    public int getClase()
    {
        return clase;
    }

    public MensajeCifrado setClase(int clase)
    {
        this.clase = clase;
        return this;
    }

    public Id getOrigen()
    {
        return this.origen;
    }

    public MensajeCifrado setOrigen(Id origen)
    {
        this.origen = origen;
        return this;
    }

    public String getContenido()
    {
        return this.contenido;
    }

    /**
     * Metodo que serializa el objeto a la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public void serialize(OutputBuffer buf) throws IOException
    {
        buf.writeInt(priority);
        buf.writeInt(clase);
        buf.writeShort(origen.getType());
        origen.serialize(buf);
        buf.writeUTF(contenido);
    }
}

