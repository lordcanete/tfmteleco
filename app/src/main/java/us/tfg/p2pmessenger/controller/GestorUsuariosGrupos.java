package us.tfg.p2pmessenger.controller;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.crypto.SecretKey;

import rice.environment.logging.Logger;
import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Usuario;

import static us.tfg.p2pmessenger.controller.Controlador.TAM_PARTE_ALEATORIA_ID;

/**
 * Clase para el manejo y creacion de nuevos usuarios y grupos. Cuando
 * se desea crear un nuevo grupo se llama a la funcion pertinente y esta
 * clase se dedica a proporcionar todos los detalles necesarios para
 * llevar a cabo la tarea. A su vez tambien guarda las claves generadas
 * en el llavero para que estas no se pierdan.
 */
public class GestorUsuariosGrupos
{
    /**
     * para mostrar mensajes por pantalla
     */
    private final Logger logger;

    /**
     * Referencia al controlador de la aplicacion para acceder al llavero
     */
    private Controlador controlador;

    /**
     * Generador de numeros aleatorios
     */
    private SecureRandom random;

    /**
     * constructor de la clase.
     * @param logger
     * @param controlador
     */
    public GestorUsuariosGrupos(Logger logger,Controlador controlador)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".<init>("+logger+", "+controlador+")");
        this.logger=logger;
        this.controlador=controlador;
        this.random=new SecureRandom();
    }

    /**
     * Crea el objeto usuario que representa a quien inicio la aplicacion.
     * @param nombre
     * @return
     */
    public Usuario cargaUsuario(String nombre)
    {
        if(logger.level<=Logger.INFO) logger.log( getClass()+".cargaUsuario("+nombre+")");
        Usuario usuario=null;
        try
        {
            Id identificador= rice.pastry.Id.build(
                    controlador.getPastryIdFactory().buildId(nombre).toStringFull());
            usuario=new Usuario(identificador,nombre,
                    controlador.getLlavero().getCertificado(nombre));
                    //.setPrivateKey(controlador.getLlavero().getClavePrivada(nombre))
                    //.setClaveSimetrica(controlador.getLlavero().getClaveSimetrica(nombre));

        } catch (Exception e)
        {
            if(logger.level<=Logger.SEVERE)
                logger.logException("Error al leer las claves del usuario "+nombre,e);
        }
        return usuario;
    }

    /**
     * Crea un nuevo grupo partiendo de su nombre y el id. Recibe los
     * parametros del tipo y la longitud de la clave. Guarda los certificados
     * y las correspondientes partes privadas y las claves simetricas
     * que se utilizaran en el grupo.
     * @param id
     * @param name
     * @param lider
     * @param tipoClave
     * @param longitudClaveSimetrica
     * @param tipoClaveSimetrica
     * @param algoritmoFirma
     * @return
     * @throws Exception
     */
    public Grupo creaGrupo(Id id, String name, Usuario lider, String tipoClave,
                           int longitudClaveSimetrica, String tipoClaveSimetrica, String algoritmoFirma)
            throws Exception
    {

        if(logger.level<=Logger.INFO) logger.log(
                getClass()+".creaGrupo("+id+", "+name+", "+lider+", "+tipoClave+
                ", lengthAES = "+longitudClaveSimetrica+")");
        // Generacion de la clave del grupo
        KeyStore.PrivateKeyEntry entrada= ManejadorClaves.generaEntrada(tipoClave,
                longitudClaveSimetrica,algoritmoFirma);
        SecretKey sk=ManejadorClaves.generaClaveSimetrica(tipoClaveSimetrica);

        Grupo grupo=new Grupo(id, name,lider)
                .setClaveSimetrica(sk)
                .setClaveSimetricaCifrada(ManejadorClaves.encriptaClaveSimetrica(
                                sk, entrada.getCertificate().getPublicKey()))
                .setCertificado(entrada.getCertificate().getPublicKey())
                .setClavePrivada(entrada.getPrivateKey())
                .setPrivateKeyEntry(entrada);

        controlador.getLlavero().setClaveSimetrica(grupo.getId().toStringFull(),sk);
        controlador.getLlavero().setEntradaPrivada(grupo.getId().toStringFull(),entrada);
        controlador.getLlavero().guardarLlavero(lider.getNombre());
        return grupo;
    }


    /**
     * Genera claves para un nuevo usuario y el objeto para representarlo. Muestra
     * por pantalla las claves generadas
     * @param nombre
     * @param longitudClave
     * @param tipoClave
     * @param algoritmoFirma
     * @param algoritmoClaveSimetrica
     * @return
     */
    public Usuario creaUsuario(String nombre, int longitudClave,String tipoClave,
                               String algoritmoFirma, String algoritmoClaveSimetrica)
    {
        if(logger.level<=Logger.INFO) logger.log(
                getClass()+".creaUsuario("+nombre+", length key = "+longitudClave+", "
                +tipoClave+", "+algoritmoFirma+", "+algoritmoClaveSimetrica+")");
        String aleatorio=generateSessionKey(TAM_PARTE_ALEATORIA_ID);
        Usuario nuevoUsuario=null;
        try
        {
            KeyStore.PrivateKeyEntry entrada = ManejadorClaves.
                    generaEntrada(tipoClave,longitudClave,algoritmoFirma);
            SecretKey claveSimetrica= ManejadorClaves.
                    generaClaveSimetrica(algoritmoClaveSimetrica);
            // TODO: 24/8/17 borrar al finalizar el proyecto
            if(logger.level<=Logger.INFO) logger.log(ManejadorClaves.
                    imprimirClave(entrada.getPrivateKey()));
            if(logger.level<=Logger.INFO) logger.log(ManejadorClaves.
                    imprimirClave(entrada.getCertificate().getPublicKey()));

            nuevoUsuario=new Usuario(controlador.getPastryIdFactory()
                    .buildId(nombre),nombre, (X509Certificate) entrada.getCertificate())

                    .setPrivateKeyEntry(entrada).setClaveSimetrica(claveSimetrica)
                    .setBloqueMensajesImportantes(
                            controlador.getPastryIdFactory().buildId(aleatorio));

        } catch (GeneralSecurityException e)
        {
            if(logger.level<=Logger.SEVERE) logger.
                    logException("Error al generar la clave de tipo "
                    +tipoClave+" longitud "+longitudClave+
                            " algoritmo de firma "+algoritmoFirma,e);
        }
        return nuevoUsuario;

    }


    /**
     * Genera una cadena aleatoria del tamano que se le indique utilizando
     * los valores que hay en alphabet
     * @param length
     * @return
     */
    public String generateSessionKey(int length)
    {
        if(logger.level<=Logger.INFO) logger.log( getClass()+".generateSessionKey( length = "+length+")");
        String alphabet =
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int n = alphabet.length(); //10

        StringBuilder sb = new StringBuilder( length );
        for( int i = 0; i < length; i++ )
            sb.append( alphabet.charAt( random.nextInt(n)));
        return sb.toString();

    }

}
