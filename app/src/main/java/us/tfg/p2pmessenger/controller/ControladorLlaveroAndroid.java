package us.tfg.p2pmessenger.controller;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;

import javax.crypto.SecretKey;

import static us.tfg.p2pmessenger.ActivityBase.TAG;
import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_CLAVE_SIMETRICA;
import static us.tfg.p2pmessenger.controller.Controlador.TIPO_KEYSTORE_ANDROID;

/**
 * Implementacion de la interfaz llavero. Utilizada para
 * llevar el control de las claves utilizadas en la aplicacion.
 */
public class ControladorLlaveroAndroid implements Llavero
{

    /**
     * Llavero que almacena las claves de cifrado de los gupos y los
     * datos almacenados en la base de datos
     */
    private KeyStore ks;

    /**
     * es la con tra sena del usuario. Se guarda en memoria para no
     * estar pidiendola continuamente al usuario
     */
    private KeyStore.PasswordProtection secretoUsuario;

    /**
     * Contexto android. Necesario a la hora de guardar y leer el
     * llavero del sistema de ficheros de android
     */
    private Context context;

    /**
     * Se rellena este campo cuando se lanza una excepcion y se puede pedir
     * con el metodo {@link #getError()}
     */
    private Exception excepcion;

    /**
     * Constructor de la clase. Genera un nuevo objeto keystore a partir de
     * uno existente o crea uno nuevo si asi se le indica
     * @param usuario nombre del usuario dueno del llavero, usado como nombre del fichero
     * @param contrUsuario con trasena para desencriptar las entradas y el llavero
     * @param contexto contexto android
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public ControladorLlaveroAndroid(String usuario, String contrUsuario, Context contexto)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException
    {
        Log.d(TAG,getClass()+".<init>("+usuario+", ********, "+contexto+")");
        this.secretoUsuario=new KeyStore.PasswordProtection(contrUsuario.toCharArray());
        Log.v(TAG,"cargado el secreto");
        this.context = contexto;
        GestorFicherosAndroid gfich = new GestorFicherosAndroid();
        if(gfich.existeFichero(usuario+ ControladorLlaveroAndroid.EXTENSION_LLAVERO_ANDROID,context))
        {
            Log.v(TAG,"existe el fichero "+usuario+ ControladorLlaveroAndroid.EXTENSION_LLAVERO_ANDROID);
            ks = createKeyStore(usuario + ControladorLlaveroAndroid.EXTENSION_LLAVERO_ANDROID, this.secretoUsuario.getPassword());
        }
        else if(usuario==null)
        {
            Log.v(TAG,"usuario == null");
            ks= createKeyStore(null,this.secretoUsuario.getPassword());
        }
        else {
            Log.v(TAG,"los demas casos");
            throw new KeyStoreException("No se puede crear el llavero");
        }
    }

    /**
     * carga el keystore desde el fichero "fichero"
     * desbloqueandolo con la contrasena indicada
     *
     * @param storePassword
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    private KeyStore createKeyStore(String fichero,char[] storePassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException
    {
        Log.d(TAG,getClass()+".createKeyStore("+fichero+",  ********)");
        // Instantiate KeyStore
        KeyStore keyStore = KeyStore.getInstance(TIPO_KEYSTORE_ANDROID);

        // Load keystore
        try {
            if(fichero !=null)
            {
                InputStream readStream = new FileInputStream(
                        new File(context.getFilesDir(),fichero));
                keyStore.load(readStream, storePassword);
                readStream.close();
            }
            else
                keyStore= ManejadorClaves.createKeyStore(storePassword,TIPO_KEYSTORE_ANDROID);
        } catch (IOException e) { //theoretically should never happen
            throw new KeyStoreException(e);
        }

        return keyStore;
    }

    /**
     * Devuelve la clave simetrica (AES) que tiene por nombre el parametro.
     * El nombre de la clave es el id del usuario al que le pertenece
     * y una extension si se trata de una clave simetrica o asimetrica
     * (AES o RSA, respectivamente)
     * @param alias nombre de la clave que se desea obtener
     * @return
     */
    @Override
    public Key getClaveSimetrica(String alias)
    {
        Log.d(TAG,getClass()+".getClaveSimetrica("+alias+")");
        Key devolver = null;
        KeyStore.SecretKeyEntry entrada;
        try
        {
            if(ks==null)
                throw new Exception("No se ha inicializado el llavero");
            else
            {
                entrada = (KeyStore.SecretKeyEntry) ks.getEntry(
                        alias+ ALGORITMO_CLAVE_SIMETRICA, this.secretoUsuario);
                devolver = entrada.getSecretKey();
            }

        } catch (Exception e)
        {
            this.excepcion=e;
        }
        return devolver;
    }

    /**
     * Devuelve la clave privada (RSA) que tiene por nombre el parametro.
     * El nombre de la clave es el id del usuario al que le pertenece
     * y una extension si se trata de una clave simetrica o asimetrica
     * (AES o RSA, respectivamente)
     * @param alias
     * @return
     */
    @Override
    public Key getClavePrivada(String alias)
    {
        Log.d(TAG,getClass()+".getClavePrivada("+alias+")");
        Key devolver = null;
        KeyStore.PrivateKeyEntry entrada;
        try
        {
            if(ks==null)
                throw new Exception("No se ha inicializado el llavero");
            else
            {
                entrada = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, this.secretoUsuario);
                devolver = entrada.getPrivateKey();
            }
        } catch (Exception e)
        {
            this.excepcion=e;
        }
        return devolver;
    }

    /**
     * Devuelve el certificado (RSA) asociado a la clave que tiene por nombre el parametro.
     * El nombre de la clave es el id del usuario al que le pertenece
     * y una extension si se trata de una clave simetrica o asimetrica
     * (AES o RSA, respectivamente)
     * @param alias
     * @return
     */
    @Override
    public X509Certificate getCertificado(String alias)
    {
        Log.d(TAG,getClass()+".getCertificado("+alias+")");
        X509Certificate devolver = null;
        KeyStore.PrivateKeyEntry entrada;
        try
        {
            if(ks==null)
                throw new Exception("No se ha inicializado el llavero");
            else
            {
                entrada = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, this.secretoUsuario);
                devolver = (X509Certificate) entrada.getCertificate();
            }

        } catch (Exception e)
        {
            this.excepcion=e;
        }
        return devolver;
    }

    /**
     * Guarda la clave simetrica en el llavero con el nombre indicado
     * @param alias
     * @param clave
     * @return
     * @throws KeyStoreException
     */
    @Override
    public Llavero setClaveSimetrica(String alias, Key clave) throws KeyStoreException
    {
        Log.d(TAG,getClass()+".setClaveSimetrica("+alias+", clave )");
        if(clave!=null)
        {
            KeyStore.SecretKeyEntry entrada = new KeyStore.SecretKeyEntry((SecretKey) clave);
            this.ks.setEntry(alias + ALGORITMO_CLAVE_SIMETRICA, entrada, this.secretoUsuario);
        }
        else
        {
            this.ks.deleteEntry(alias + ALGORITMO_CLAVE_SIMETRICA);
        }
        return this;
    }

    /**
     * Guarda la pareja de certificado y clave privada bajo el nombre indicado
     * @param alias
     * @param entradaPrivada
     * @return
     * @throws KeyStoreException
     */
    @Override
    public Llavero setEntradaPrivada(String alias, KeyStore.PrivateKeyEntry entradaPrivada) throws KeyStoreException
    {
        Log.d(TAG,getClass()+".setClavePrivada("+alias+", entradaPrivada )");
        if(entradaPrivada!=null)
            this.ks.setEntry(alias,entradaPrivada,this.secretoUsuario);
        else
            this.ks.deleteEntry(alias);
        return this;
    }

    /**
     * Guarda el certificado de un usuario bajo el nombre indicado
     * @param alias
     * @param certificate
     * @throws KeyStoreException
     */
    @Override
    public void guardarCertificado(String alias, X509Certificate certificate) throws KeyStoreException
    {
        Log.d(TAG,getClass()+".guardarCertificado("+alias+", certificate)");
        if(certificate!=null)
        {
            this.ks.setCertificateEntry(alias,certificate);
        } else
        {
            this.ks.deleteEntry(alias);
        }
    }

    /**
     * Guarda el llavero en el fichero asociado a un usuario, lo cierra
     * y elimina el objeto llavero
     * @param usuario
     * @throws Exception
     */
    @Override
    public void cerrarLlavero(String usuario) throws Exception
    {
        Log.d(TAG,getClass()+".cerrarLlavero("+usuario+")");
        FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(),usuario+Llavero.EXTENSION_LLAVERO_ANDROID));
        this.ks.store(out,secretoUsuario.getPassword());
        out.close();

        this.ks=null;
        this.secretoUsuario.destroy();
        this.secretoUsuario=null;
    }

    /**
     * Guarda el objeto llavero pero no elimina el objeto que lo mantiene
     * @param usuario
     */
    @Override
    public void guardarLlavero(String usuario)
    {
        Log.d(TAG,getClass()+".guardarLlavero("+usuario+")");
        try
        {
            FileOutputStream out = new FileOutputStream(new File(
                    context.getFilesDir(),usuario+Llavero.EXTENSION_LLAVERO_ANDROID));
            this.ks.store(out,secretoUsuario.getPassword());
            out.close();
        }catch (Exception e)
        {
            this.excepcion=e;
        }
    }

    /**
     * Elimina el fichero que contiene el llavero, y con el todas las claves
     * @param usuario
     */
    @Override
    public void eliminarLlavero(String usuario)
    {
        Log.d(TAG,getClass()+".eliminarLlavero("+usuario+")");
        String dir = context.getFilesDir().getAbsolutePath();
        File f0 = new File(dir, usuario+ EXTENSION_LLAVERO_ANDROID);
        if(f0.delete())
        {
            Log.d(TAG,"fichero eliminado correctamente");
        } else {
            Log.d(TAG,"error al eliminar el fichero");
        }
    }

    /**
     * Devuelve si hubo algun error
     * @return
     */
    @Override
    public Exception getError()
    {
        Log.d(TAG,getClass()+".getError()");
        Exception ret=this.excepcion;
        this.excepcion=null;
        return ret;
    }

    /**
     * Obtiene los nombres de las claves y certificados guardados
     * @return
     * @throws Exception
     */
    @Override
    public ArrayList<String> obtenerClavesGuardadas() throws Exception
    {
        Log.d(TAG,getClass()+".obtenerClavesGuardadas()");
        ArrayList<String> claves= Collections.list(this.ks.aliases());
        return claves;
    }

}
