package us.tfg.p2pmessenger.model;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

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

/**
 * Clase que representa un objeto almacenable en pastry
 * que contiene una lista de entradas
 * @see EntradaMensaje
 * cada entrada contiene un mendaje cifrado. Contiene
 * los ids del anterior y siguiente bloque, para que
 * se pueda recorrer la lista de los mensajes importantes
 * en ambos sentidos. Tiene tambien a quien pertenece
 * el bloque y cuantos mensajes tiene. Cada bloque admite
 * un maximo de {@link BloqueMensajes#TAM_MAX_BLOQUE}
 * mensajes. Cuando se llena, se debera crear uno nuevo.
 */
public class BloqueMensajes implements GCPastContent
{

    static final long serialVersionUID = 2050618708295373834L;

    /**
     * id del bloque
     */
    private Id idBloque;

    /**
     * lista de mensajes almacenados en este bloque. Cada mensaje
     * esta incluido en un objeto de tipo {@link EntradaMensaje}
     */
    private Queue<EntradaMensaje> mensajes;

    /**
     * A quien van destinado los mensajes que contiene este bloque
     */
    private Id destinatario;

    /**
     * {@link rice.pastry.Id} del siguiente bloque
     */
    private Id siguienteBloque;

    /**
     * {@link rice.pastry.Id} del bloque anterior
     */
    private Id anteriorBloque;

    /**
     * Si este bloque pertenece a un grupo o a un
     * usuario individual
     */
    private boolean grupo;

    /**
     * version del bloque
     */
    private long version;

    public static final short TYPE = 323;

    /**
     * Numero maximo de mensajes que se pueden
     * almacenar en el bloque
     */
    public static final int TAM_MAX_BLOQUE = 50;

    /**
     * Constructor del bloque. Inicializa las variables
     * @param idBloque
     * @param destinatario
     * @param isGrupo
     * @param anterior
     */
    public BloqueMensajes(Id idBloque, Id destinatario, boolean isGrupo,Id anterior)
    {
        System.out.println(getClass()+".<init>("+idBloque+", "+destinatario+
                ", isGrupo = "+isGrupo+", "+anterior+")");
        mensajes = new LinkedList<>();
        this.idBloque = idBloque;
        this.destinatario = destinatario;
        this.siguienteBloque = null;
        this.anteriorBloque=anterior;
        this.version = 0;
        this.grupo = isGrupo;
    }

    /**
     * Constructor para crear el bloque a partir de su serializacion
     * raw de patry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws IOException
     */
    public BloqueMensajes(InputBuffer buf) throws IOException
    {
        System.out.println(getClass()+".<init>(InputBuffer)");
        if (buf.readShort() == rice.pastry.Id.TYPE)
            this.idBloque = rice.pastry.Id.build(buf);
        else
            throw new IOException("Error al leer el remitente." +
                    " Los tipos no coinciden");

        int entradas = buf.readInt();
        for (int i = 0; i < entradas; i++)
        {
            EntradaMensaje entrada;

            if (buf.readShort() == EntradaMensaje.TYPE)
            {
                entrada = new EntradaMensaje(buf);
                this.mensajes.add(entrada);
            } else
                throw new IOException("Error al leer una entrada. Los tipos no coinciden");
        }
        if (buf.readShort() == rice.pastry.Id.TYPE)
            this.destinatario = rice.pastry.Id.build(buf);
        else
            throw new IOException("Error al leer el destinatario. Los tipos no coinciden");

        if (buf.readShort() == rice.pastry.Id.TYPE)
            this.siguienteBloque = rice.pastry.Id.build(buf);
        else
            throw new IOException("Error al leer siguienteBloque. Los tipos no coinciden");

        this.grupo = buf.readBoolean();
        this.version = buf.readLong();
    }

    public Id getSiguienteBloque()
    {
        return siguienteBloque;
    }

    public BloqueMensajes setSiguienteBloque(Id siguienteBloque)
    {
        this.siguienteBloque = siguienteBloque;
        return this;
    }

    public Id getAnteriorBloque()
    {
        return anteriorBloque;
    }

    public BloqueMensajes setAnteriorBloque(Id anteriorBloque)
    {
        this.anteriorBloque = anteriorBloque;
        return this;
    }

    public BloqueMensajes insertarEntrada(EntradaMensaje entrada)
    {
        System.out.println(getClass()+".insertarEntrada("+entrada+")");
        if (this.mensajes.size() < TAM_MAX_BLOQUE)
            this.mensajes.add(entrada);
        this.version = this.mensajes.size();
        return this;
    }

    public BloqueMensajes vaciaBloque()
    {
        System.out.println(getClass()+".vaciaBloque()");
        this.mensajes.clear();
        return this;
    }

    public Id getDestinatario()
    {
        return destinatario;
    }

    public Queue<EntradaMensaje> getMensajes()
    {
        return mensajes;
    }

    public int getTamBloque()
    {
        return this.mensajes.size();
    }

    public boolean isLleno()
    {
        return this.mensajes.size() == TAM_MAX_BLOQUE;
    }

    public boolean isGrupo()
    {
        return grupo;
    }

    public BloqueMensajes setGrupo(boolean grupo)
    {
        this.grupo = grupo;
        return this;
    }

    /**
     * Cuando se inserta un objeto en pastry, se comprueba si
     * es valido hacerlo a traves de este metodo.
     * Para admitir la insercion de un bloque en un sitio vacio:
     *
     * - es necesario que no tenga mensajes, que el
     * siguiente bloque sea null y que el id del bloque coincide
     * con el del id en el que se inserta.
     *
     * Para admitir la sobreescritura de un bloque:
     *
     * - es necesario que tenga los mismos metadatos
     * que el bloque que ya existia, que contenga
     * los mismos mensajes mas uno nuevo, solo uno y
     * tambien debe no estar lleno
     *
     * @param id the key identifying the object
     * @param existingContent
     * @return
     * @throws PastException
     */
    @Override
    public PastContent checkInsert(Id id, PastContent existingContent) throws PastException
    {
        PastContent ret = null;
        if (existingContent == null)
        {
            // si esta posicion esta vacia
            if ((this.mensajes.size() == 0) &&
                    (this.siguienteBloque == null) &&
                    (this.destinatario != null) &&
                    (this.idBloque != null)&&
                    id.equals(this.getId()))
                ret = this;
            else
                throw new PastException("El nuevo bloque que intenta insertar no esta vacio");
        } else
        {
            BloqueMensajes existente = (BloqueMensajes) existingContent;
            if ((existente.getId() == this.idBloque) &&
                    (existente.getDestinatario() == this.destinatario) &&
                    (id.equals(this.getId())) &&
                    (existente.getTamBloque() < TAM_MAX_BLOQUE))
            {
                Queue<EntradaMensaje> mensajesExistentes = existente.getMensajes();
                Queue<EntradaMensaje> pendientes = this.getMensajes();

                if ((existente.getTamBloque() + 1) == this.getTamBloque())
                {
                    if (!pendientes.containsAll(mensajesExistentes))
                        throw new PastException("Todos los mensajes existentes no se " +
                                "encuentra en la nueva lista que se quiere insertar");
                    else if (pendientes.size() == TAM_MAX_BLOQUE && this.siguienteBloque == null)
                    {
                        throw new PastException("Se ha llenado el bloque pero no se ha proporcionado el siguiente");
                    } else
                    {
                        ret = this;
                    }
                } else
                    throw new PastException("Se ha intentado insertar mas de un mensaje en el bloque, error");
            } else if (existente.getTamBloque() == TAM_MAX_BLOQUE)
            {
                throw new PastException("Este bloque esta lleno, cree uno nuevo");
            } else
            {
                throw new PastException("Los identificadores del bloque existente" +
                        " no coinciden con los del nuevo");
            }
        }

        return ret;
    }

    @Override
    public long getVersion()
    {
        return this.version;
    }

    @Override
    public GCPastContentHandle getHandle(GCPast local, long expiration)
    {
        return new ManejadorBloqueMensajes(local.getLocalNodeHandle(), getId(), getVersion(), expiration);
    }

    @Override
    public GCPastMetadata getMetadata(long expiration)
    {
        return null;
    }

    @Override
    public PastContentHandle getHandle(Past local)
    {
        return new ManejadorBloqueMensajes(local.getLocalNodeHandle(),
                this.idBloque, this.version, -1);
    }

    @Override
    public Id getId()
    {
        return idBloque;
    }

    /**
     * Deja de ser mutable cuando se llena
     * @return
     */
    @Override
    public boolean isMutable()
    {
        boolean ret = false;
        if (mensajes.size() < TAM_MAX_BLOQUE)
            ret = true;
        else
            ret = false;
        return ret;
    }

    /**
     * Metodo toString()
     * @return
     */
    @Override
    public String toString()
    {
        String devolver = "Bloque [<";

        StringBuilder builder=new StringBuilder(devolver);

        if(anteriorBloque!=null)
            builder.append(anteriorBloque.toStringFull());
        else
            builder.append("null");

        builder.append("> <-- <").append(idBloque.toStringFull()).append("> --> <");

        if(siguienteBloque!=null)
            builder.append(siguienteBloque.toStringFull());
        else
            builder.append("null");

        builder.append(">] Destinatario ");

        if(grupo)
            builder.append("grupo ");

        builder.append("<").append(destinatario.toStringFull()).append(">\n");


        for(EntradaMensaje entrada:mensajes)
        {
            builder.append("\t").append(entrada).append("\n");
        }

        return builder.toString();
    }

    /**
     * Metodo equals
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        boolean devolver=false;
        if(o instanceof BloqueMensajes)
        {
            BloqueMensajes comparar= (BloqueMensajes) o;

            boolean mensajesIguales = comparar.mensajes.containsAll(this.mensajes);
            devolver=comparar.idBloque.equals(this.idBloque)&&
                    comparar.destinatario.equals(this.destinatario)&&
                    comparar.siguienteBloque.equals(this.siguienteBloque)&&
                    mensajesIguales&&
                    (comparar.grupo==this.grupo);
        }

        return devolver;

    }

    /**
     * Metodo de serializacion RAW de pastry
     * @param buf
     * @throws IOException
     */
    public void serialize(OutputBuffer buf) throws IOException
    {
        buf.writeShort(idBloque.getType());
        idBloque.serialize(buf);

        buf.writeInt(this.mensajes.size());
        for (EntradaMensaje entrada : mensajes)
        {
            buf.writeShort(entrada.getType());
            entrada.serialize(buf);
        }

        buf.writeShort(destinatario.getType());
        destinatario.serialize(buf);

        buf.writeShort(siguienteBloque.getType());
        siguienteBloque.serialize(buf);

        buf.writeBoolean(this.grupo);
        buf.writeLong(this.version);

    }
}