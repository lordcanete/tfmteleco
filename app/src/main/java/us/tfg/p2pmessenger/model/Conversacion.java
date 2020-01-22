package us.tfg.p2pmessenger.model;

import java.util.Date;

import rice.p2p.commonapi.Id;

/**
 * Objeto que representa una conversacion abierta. Contiene
 * el usuario interlocutor y el ultimo mensaje que se envio
 */
public class Conversacion
{
    static final long serialVersionUID = 5591756460937247097L;

    public static final int TIPO_GRUPO = 1;
    public static final int TIPO_INDIVIDUAL = 2;

    /**
     * Fecha del ultimo mensaje
     */
    private Date fecha;

    /**
     * {@link Id} del interlocutor (o id de grupo o de usuario
     * individual)
     */
    private Id id;

    /**
     * Alias que tiene el usuario o el grupo asignado
     */
    private String alias;

    /**
     * Ultimo mensaje enviado
     */
    private String mensaje;

    /**
     * Si es una conversacion de grupo o de
     */
    private int tipo;

    /**
     * Si tiene algun mensaje pendiente de leer
     */
    private transient boolean pendiente;

    /**
     * Constructor de la clase
     * @param id
     * @param fecha
     * @param alias
     * @param tipo
     */
    public Conversacion(Id id, Date fecha, String alias, int tipo)
    {
        this.id=id;
        this.fecha=fecha;
        this.alias=alias;
        this.tipo=tipo;
        this.pendiente=false;
    }

    public boolean isPendiente() {
        return pendiente;
    }

    public void setPendiente(boolean pendiente) {
        this.pendiente = pendiente;
    }

    public int getTipo()
    {
        return tipo;
    }

    public Conversacion setTipo(int tipo)
    {
        this.tipo = tipo;
        return this;
    }

    public Date getFecha()
    {
        return fecha;
    }

    public Conversacion setFecha(Date fecha)
    {
        this.fecha = fecha;
        return this;
    }

    public Id getId()
    {
        return id;
    }

    public Conversacion setId(Id id)
    {
        this.id = id;
        return this;
    }

    public String getAlias()
    {
        return alias;
    }

    public Conversacion setAlias(String alias)
    {
        this.alias = alias;
        return this;
    }

    public String getMensaje()
    {
        return mensaje;
    }

    public Conversacion setMensaje(String mensaje)
    {
        this.mensaje = mensaje;
        return this;
    }

    @Override
    public String toString() {
        return "Conversacion(tipo = "+tipo+"): "+alias+"("+id+")(pend = "+pendiente+") "+fecha+": "+mensaje;
    }

    /**
     * Metodo equals()
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
        boolean ret=false;
        if(obj instanceof Conversacion)
        {
            Conversacion c=(Conversacion)obj;
            if((this.alias.equals(c.getAlias()))&&
                    (this.tipo==c.getTipo())&&
                    (this.id.equals(c.getId()))
                    )
                ret=true;
            else
                ret=false;
        }
        else
            ret=false;

        return ret;
    }


}
