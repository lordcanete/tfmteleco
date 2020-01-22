package us.tfg.p2pmessenger.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;

import javax.crypto.SecretKey;

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
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import us.tfg.p2pmessenger.controller.ManejadorClaves;
import us.tfg.p2pmessenger.util.Base64;

import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_CLAVE_ASIMETRICA;
import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_CLAVE_SIMETRICA;
import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_FIRMA;

/**
 * Envoltura del grupo, para facilitar el encriptado y la
 * validacion.
 * Se compone de tres partes:
 * - certificado: en formato {@link String}
 * - firma: en formato {@link String}
 * - contenido: objeto grupo serializado y encriptado
 *
 * Al guardarlo en pastry, se comprueba que el certificado
 * del grupo permanece inalterado (si se esta sobreescribiendo)
 * y la firma es consistente con el contenido.
 */
public class GrupoCifrado implements GCPastContent
{
    static final long serialVersionUID = 2991527880361551469L;

    /**
     * Version del grupo (modificaciones que ha sufrido)
     */
    private long version;
    // private Key certificado

    /**
     * Certificado en formato {@link Base64}
     * para comprobar la validez de la firma
     */
    private String certificadoCodificado;
    //    private SignedObject objetoFirmado;

    /**
     * {@link Grupo} serializado
     */
    private String contenido;

    /**
     * Firma obtenida con la clave privada
     * asociada al certificado del objeto.
     * Si la firma no esta validada, el objeto
     * no se almacena.
     */
    private String firma;

    /**
     * {@link Id} del grupo
     */
    private Id idGrupo;

    private boolean cifrado;

    protected static final short TYPE= 317;

    /**
     * Constructor que deserializa desde la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param input
     * @throws Exception
     */
    public GrupoCifrado(InputBuffer input) throws IOException, ClassNotFoundException
    {
        short tipo=input.readShort();
        if(rice.pastry.Id.TYPE==tipo)
            this.idGrupo=rice.pastry.Id.build(input);
        else
            throw new IOException("No se puede leer el id");

        this.cifrado=input.readBoolean();

        this.version=input.readLong();
        this.certificadoCodificado=input.readUTF();
        this.firma=input.readUTF();
        this.contenido=input.readUTF();

    }

    /**
     * Serializa el objeto a la forma raw de pastry
     * @see rice.p2p.past.gc.rawserialization.RawGCPastContent
     * @param buf
     * @throws Exception
     */
    public void serialize(OutputBuffer buf) throws IOException
    {
        buf.writeShort(idGrupo.getType());
        idGrupo.serialize(buf);
        buf.writeBoolean(this.cifrado);
        buf.writeLong(version);
        buf.writeUTF(certificadoCodificado);
        buf.writeUTF(firma);
        buf.writeUTF(contenido);
    }

    /**
     * Utiliza la clave privada para desencriptar la clave
     * simetrica y con esta desencripta el resto del
     * contenido. A partir de la cadena desencriptada,
     * deserializa de la forma raw de pastry al objeto
     * Grupo de nuevo.
     * @param clavePrivada
     * @return
     * @throws Exception
     */
    public Grupo desencriptar(Key clavePrivada) throws Exception
    {
        Grupo grupo=null;

        boolean verificado;
        // crea el objeto firma
        Signature sig = Signature.getInstance(ALGORITMO_FIRMA);

        // deserializa el certificado
        Key certificado= ManejadorClaves.leeClavePublica(
                us.tfg.p2pmessenger.util.Base64.getDecoder().decode(certificadoCodificado), ALGORITMO_CLAVE_ASIMETRICA);

        // inicializa el objeto firma
        sig.initVerify((PublicKey) certificado);

        // anade el contenido
        sig.update(this.contenido.getBytes(StandardCharsets.UTF_8));

        // anade la firma
        verificado = sig.verify( Base64.getDecoder().decode(this.firma) );

        // si se verifica la firma, sigue desencriptando
        if(verificado)
        {

            byte[] todo = us.tfg.p2pmessenger.util.Base64.getDecoder().decode(contenido);
            InputBuffer in = new SimpleInputBuffer(todo);
            String claveSimetricaCifrada = in.readUTF();
            SecretKey claveSimetrica = ManejadorClaves.desencriptaClaveSimetrica(
                    claveSimetricaCifrada, clavePrivada, ALGORITMO_CLAVE_SIMETRICA);

            String contenidoCifrado = in.readUTF();


            String contenidoBase64 = ManejadorClaves.decrypt(contenidoCifrado, claveSimetrica);
            byte[] contenidoByte = Base64.getDecoder().decode(contenidoBase64);
            in = new SimpleInputBuffer(contenidoByte);
            short tipo=in.readShort();
            if(tipo==Grupo.TYPE)
                grupo = new Grupo(in).setClaveSimetrica(claveSimetrica).setClavePrivada(clavePrivada);
            else
                throw new Exception("El objeto encriptado no es un grupo");
        }
        else
            throw new SignatureException("La firma no es correcta");

        return grupo;
    }

    /**
     * Constructor de la clase
     * @param grupo
     * @param claveSimetrica
     * @param clavePrivada
     * @throws Exception
     */
    public GrupoCifrado(Grupo grupo,SecretKey claveSimetrica, Key clavePrivada) throws Exception
    {
        SimpleOutputBuffer out=new SimpleOutputBuffer();
        out.writeShort(grupo.getType());
        grupo.serialize(out);
        String contenidoBase64= Base64.getEncoder().encodeToString(out.getBytes());
        String contenidoCifrado=ManejadorClaves.encrypt(contenidoBase64,claveSimetrica);

        out.close();

        out=new SimpleOutputBuffer();

        out.writeUTF(grupo.getClaveSimetricaCifrada());
        out.writeUTF(contenidoCifrado);

        byte[] todo=out.getBytes();

        this.contenido= Base64.getEncoder().encodeToString(todo);

        Signature signature = Signature.getInstance(ALGORITMO_FIRMA);

        signature.initSign((PrivateKey) clavePrivada);

        signature.update(contenido.getBytes(StandardCharsets.UTF_8));


        this.firma=Base64.getEncoder().encodeToString(signature.sign());

        this.cifrado=true;
        this.certificadoCodificado= Base64.getEncoder().encodeToString(grupo.getCertificado().getEncoded());
        this.idGrupo=grupo.getId();
        this.version=grupo.getVersion();

        out.close();

    }


    //    /**
    //     * El metodo firmar tiene una doble funcion, rellena el contenido del grupo
    //     * y lo firma.
    //     * @param clave
    //     * @param contenido
    //     * @throws NoSuchAlgorithmException
    //     * @throws InvalidKeyException
    //     * @throws IOException
    //     * @throws SignatureException
    //     */
    //    public void firmar(PrivateKey clave,String contenido)
    //            throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException
    //    {
    //        if((clave!=null)&&(contenido!=null))
    //        {
    //            this.objetoFirmado = new SignedObject(contenido, clave, Signature.getInstance(clave.getAlgorithm()));
    //            version++;
    //        }
    //    }

    public GrupoCifrado setVersion(long version)
    {
        this.version = version;
        return this;
    }

    public Key getCertificado()
    {
        Key certificado=null;
        try
        {
            certificado = ManejadorClaves.leeClavePublica(
                    Base64.getDecoder().decode(certificadoCodificado), ALGORITMO_CLAVE_ASIMETRICA);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return certificado;
    }

    public SignedObject getObjetoFirmado()
    {
        return null;
    }

    @Override
    public long getVersion()
    {
        return this.version;
    }

    @Override
    public GCPastContentHandle getHandle(GCPast local, long expiration)
    {
        return new ManejadorGrupoCifrado(local.getLocalNodeHandle(),getId(),getVersion(),expiration);
    }

    @Override
    public GCPastMetadata getMetadata(long expiration) {
        return new GCPastMetadata(expiration);
    }

    /**
     * Cuando se inserta un objeto en pastry, se comprueba si
     * es valido hacerlo a traves de este metodo.
     *
     * Si se inserta un grupo en un lugar vacio:
     * Comprueba que la firma que se proporciona
     * valida el contenido, en caso contrario, no
     * almacena nada
     *
     * Si se sobreescribe:
     * Comprueba que la version sea mayor
     * @param id the key identifying the object
     * @param existingContent
     * @return
     * @throws PastException
     */
    @Override
    public PastContent checkInsert(Id id, PastContent existingContent)
            throws PastException
    {

        GrupoCifrado devolver= (GrupoCifrado) existingContent;

        if(existingContent!=null)
        {
            GrupoCifrado existing = (GrupoCifrado) existingContent;
            if (existing.getVersion() < this.getVersion())
            {
                boolean verificado=false;
                boolean verificado2=false;
                try
                {
                    Signature sig = Signature.getInstance(ALGORITMO_FIRMA);

                    sig.initVerify((PublicKey) this.getCertificado());
                    sig.update(this.contenido.getBytes(StandardCharsets.UTF_8));
                    verificado = sig.verify( Base64.getDecoder().decode(this.firma) );

                    Signature sig2 =  Signature.getInstance(ALGORITMO_FIRMA);

                    sig2.initVerify((PublicKey) existing.getCertificado());
                    sig2.update(this.contenido.getBytes(StandardCharsets.UTF_8));
                    verificado2=sig2.verify(us.tfg.p2pmessenger.util.Base64.getDecoder().decode(this.firma));

                }
                catch (Exception e)
                {
                    throw new PastException(e.toString());
                }
                if(verificado&&verificado2)
                    devolver=this;
                else
                    throw new PastException("Firma no valida");
            }
            else
            {
                throw new PastException("Grupo.checkinsert: El grupo que quiere " +
                        "insertar tiene un nuero de version inferior al que ya existe");
            }

        }
        else
        {
            this.version=1;
            boolean verificado=false;
            try
            {
                Signature sig = Signature.getInstance(ALGORITMO_FIRMA);

                Key certificado=ManejadorClaves.leeClavePublica(
                        Base64.getDecoder().decode(certificadoCodificado), ALGORITMO_CLAVE_ASIMETRICA);

                sig.initVerify((PublicKey) certificado);

                sig.update(this.contenido.getBytes(StandardCharsets.UTF_8));

                verificado = sig.verify( Base64.getDecoder().decode(this.firma) );

            }
            catch (Exception e)
            {
                throw new PastException(e.toString());
            }
            if(verificado)
                devolver=this;
            else
                throw new PastException("Firma no valida");
        }
        return devolver;
    }

    /**
     * States if this content object is mutable. Mutable objects are
     * not subject to dynamic caching in Past.
     *
     * @return true if this object is mutable, else false
     */
    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public PastContentHandle getHandle(Past local)
    {
        return new ManejadorGrupoCifrado(local.getLocalNodeHandle(),
                this.idGrupo,this.version,-1);
    }

    @Override
    public Id getId()
    {
        return this.idGrupo;
    }

    @Override
    public String toString()
    {
        String ret=null;
        try
        {
            ret= "id= "+ idGrupo.toStringFull()+" version "+this.version+" contenido = "+contenido+" firma = "+firma;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

}
