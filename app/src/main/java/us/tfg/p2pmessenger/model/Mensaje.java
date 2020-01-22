package us.tfg.p2pmessenger.model;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawMessage;

import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Objeto que respalda un mensaje entre dos usuarios.
 * Este mensaje tiene los tipos definidos en la clase
 * y estan organizados segun sean referidos a conversaciones
 * privadas o de grupo.
 */
public class Mensaje implements RawMessage
{
    static final long serialVersionUID = 2298861684921217418L;

    // el rango de mensajes de control es de 0 > 100
    public static final int ECHO_REPLY=0;
    public static final int HOST_UNREACHABLE=3;
    public static final int ECHO_REQUEST=8;

    public static final int CONTROL_MAX_TIPO = 100;

    // el rango de mensajes de grupo es de 200 > 300
    public static final int GRUPO_MIN_TIPO = 200;

    public static final int GRUPO_IMPORTANTE=201;
    public static final int GRUPO_NORMAL=202;
    public static final int GRUPO_ENVIANDO_FICHERO =203;
    public static final int GRUPO_PEDIR_ADDR =204;
    public static final int GRUPO_RESPUESTA_IPADDR =205;
    public static final int GRUPO_ENTRA=206;
    public static final int GRUPO_SALE =207;
    public static final int GRUPO_CAMBIO_LIDER =208;
    public static final int GRUPO_BLOQUE_UTILIZADO=209;
    //public static final int DESCONECTO=9;
    //public static final int RESPUESTA_FICHERO=210;

    public static final int GRUPO_MAX_TIPO = 300;

    // el rango de tipos para los mensajes individuales es 300 > 400
    public static final int INDIVIDUAL_MIN_TIPO = 300;

    public static final int INDIVIDUAL_IMPORTANTE=310;
    public static final int INDIVIDUAL_NORMAL=320;
    public static final int INDIVIDUAL_ENVIANDO_FICHERO=330;
    public static final int INDIVIDUAL_PETICION_INICIO_CHAT = 340;
    public static final int INDIVIDUAL_RESPUESTA_INICIO_CHAT = 341;
    public static final int INDIVIDUAL_PETICION_UNIR_GRUPO = 350;
    public static final int INDIVIDUAL_RESPUESTA_UNIR_GRUPO = 351;

    public static final int INDIVIDUAL_MAX_TIPO = 400;


    public static final int CLAVE_SESION_TIMEOUT = 970;
    public static final int PING_TIMEOUT = 980;

    private int clase;

    public static final short TYPE=319;

    /**
     * Destino del mensaje. Si es un grupo es el id del grupo
     * Si es individual, se contestara de forma individual
     */
    private Id destino;

    /**
     * Quien envia el mensaje, se pondra siempre el nodo de origen
     */
    private Id origen;

    /**
     * Contenido del mensaje.
     */
    private String contenido;

    /**
     * Fecha de creacion del mensaje
     */
    private Date fecha;


    /**
     * Prioridad del mensaje (tiene significado para pastry)
     */
    private int priority;

    public Mensaje()
    {}
    /**
     * Contructor de la clase
     * @param origen
     * @param destino
     * @param contenido
     * @param clase
     */
    public Mensaje(Id origen, Id destino, String contenido, int clase )
    {
        this.origen=origen;
        this.destino=destino;
        this.contenido=contenido;
        this.clase=clase;
        this.fecha=new Date();

        if(clase>=50)
            this.priority=HIGH_PRIORITY;
        else if(clase==GRUPO_IMPORTANTE||clase==INDIVIDUAL_IMPORTANTE)
            this.priority=MEDIUM_PRIORITY;
        else
            this.priority=LOW_PRIORITY;

    }

    /**
     * Constructor que deserializa desde la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public Mensaje(InputBuffer buf) throws IOException
    {

        short tipo=buf.readShort();
        if(rice.pastry.Id.TYPE==tipo)
            this.origen=rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");
        tipo=buf.readShort();
        if(rice.pastry.Id.TYPE==tipo)
            this.destino=rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");

        this.contenido=buf.readUTF();
        this.clase=buf.readInt();
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA,Locale.getDefault());

        try
        {
            this.fecha=df.parse(buf.readUTF());
        } catch (ParseException e)
        {
            //this.fecha=new Date();
        }

        this.priority=buf.readInt();
    }

    public int getClase()
    {
        return clase;
    }

    public Id getDestino()
    {
        return destino;
    }

    public Id getOrigen()
    {
        return origen;
    }

    public String getContenido()
    {
        return contenido;
    }

    public Date getFecha()
    {
        return this.fecha;
    }

    public Mensaje setClase(int clase) {
        this.clase = clase;
        return this;
    }

    public Mensaje setDestino(Id destino) {
        this.destino = destino;
        return this;
    }

    public Mensaje setOrigen(Id origen) {
        this.origen = origen;
        return this;
    }

    public Mensaje setContenido(String contenido) {
        this.contenido = contenido;
        return this;
    }

    public Mensaje setFecha(Date fecha) {
        this.fecha = fecha;
        return this;
    }

    public Mensaje setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Metodo toString()
     * @return
     */
    @Override
    public String toString()
    {
       DateFormat df = new SimpleDateFormat(FORMATO_FECHA,Locale.getDefault());
        return df.format(this.fecha)+" (" +
                this.clase + ") [" + origen.toString() +
                " --> " + destino.toString() +
                " : <" + contenido + ">]";
    }

    /**
     * Metodo equals()
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        boolean iguales=false;
        if((o!=null)&&(o instanceof Mensaje))
        {
            Mensaje otro=(Mensaje)o;
            iguales = (otro.getPriority() == this.priority) &&
                    (otro.getOrigen().equals(this.origen)) &&
                    (otro.getDestino().equals(this.destino)) &&
                    (otro.getFecha().equals(this.fecha)) &&
                    (otro.getClase() == this.getClase());
        }
        return iguales;
    }

    @Override
    public int getPriority()
    {
        return this.priority;
    }


    @Override
    public short getType()
    {
        return this.TYPE;
    }

    /**
     * Metodo que serializa el objeto a la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    @Override
    public void serialize(OutputBuffer buf) throws IOException
    {
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());

        buf.writeShort(origen.getType());
        origen.serialize(buf);
        buf.writeShort(destino.getType());
        destino.serialize(buf);
        buf.writeUTF(contenido);
        buf.writeInt(clase);
        buf.writeUTF(df.format(fecha));
        buf.writeInt(priority);
    }


}