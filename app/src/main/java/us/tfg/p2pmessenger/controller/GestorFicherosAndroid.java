package us.tfg.p2pmessenger.controller;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import us.tfg.p2pmessenger.ApplicationExtended;

import static us.tfg.p2pmessenger.ActivityBase.TAG;
import static us.tfg.p2pmessenger.controller.Controlador.NOMBRE_OBJETOS_PASTRY;
import static us.tfg.p2pmessenger.controller.Llavero.EXTENSION_LLAVERO_ANDROID;

/**
 * Created by FPiriz on 27/6/17.
 */
public class GestorFicherosAndroid {

    /**
     * Constructor de la clase
     */
    public GestorFicherosAndroid() {
        Log.d(TAG, getClass() + ".<init>()");
    }

    public void guardaEnFichero(String nombreFichero, byte contenido[]) throws IOException {
        System.out.println(getClass() + ".guardaEnFichero(" + nombreFichero + ", " + contenido + ")");
        File f = new File(nombreFichero);
        if (f.exists() && !f.isDirectory())
            throw new IOException("El fichero " + nombreFichero + " ya existe, elija un nombre nuevo");
        else {
            FileOutputStream fos = new FileOutputStream(nombreFichero);
            fos.write(contenido);
            fos.close();
        }
    }

    public byte[] leeDeFichero(String nombreFichero) throws IOException {
        System.out.println(getClass() + ".leeDeFichero(" + nombreFichero + ")");
        File f = new File(nombreFichero);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[(int) f.length()];
        dis.readFully(bytes);
        dis.close();
        return bytes;
    }


    /**
     * @param nombreFichero Fichero en el que se desea escribir
     * @param content       Lo que se quiere escribir en el fichero
     * @param alFinal       Indica si escribir el contenido al final del fichero o sobreescribir
     *                      el contenido
     */
    public void escribirAFichero(String nombreFichero,
                                 String content, boolean alFinal)
            throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(nombreFichero, alFinal)));
        out.println(content);
        out.close();
    }


    /**
     * @param nombreFichero Fichero en el que se desea escribir
     * @param content       Lo que se quiere escribir en el fichero
     * @param alFinal       Indica si escribir el contenido al final del fichero o sobreescribir
     *                      el contenido
     * @return
     */
    public void escribirAFichero(String nombreFichero,
                                 byte[] content, boolean alFinal)
            throws IOException {
        System.out.println(getClass() + ".escribirAFichero(" + nombreFichero + ", " + content + ", " + alFinal + ")");
        FileOutputStream fos = new FileOutputStream(nombreFichero, alFinal);
        fos.write(content);
        fos.close();
//        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(nombreFichero,alFinal)));
//            out.println(Base64.getEncoder().encodeToString(content));
//            out.close();
    }


    /**
     * Comprueba si un fichero existe
     * @param nombreFichero fichero que se quiere comprobar
     * @param context contexto de la aplicacion
     * @return si existe o no
     */
    public boolean existeFichero(String nombreFichero, Context context) {
        System.out.println(getClass() + ".existeFichero(" + nombreFichero + ")");
        boolean existe = false;

        File f = new File(context.getFilesDir(), nombreFichero);
        if (f.exists() && !f.isDirectory()) {
            existe = true;
        }
        return existe;
    }

    /**
     * elimina un fichero dado por el nombre
     * @param nombre nombre del fichero que se quiere eliminar
     * @param context
     */
    public void eliminaFichero(String nombre, Context context) {
        System.out.println(getClass() + ".eliminaFichero(" + nombre + ", " + context + ")");
        if (existeFichero(nombre, context)) {

            File dir = context.getFilesDir();
            File file = new File(dir, nombre);
            file.delete();
        }
    }

    /**
     * Elimina los ficheros relacionados con el almacenamiento de pastry.
     * Borra la replica local de la informacion de la red pastry
     * @param context
     */
    public void eliminaFicherosPastry(Context context,String usuario) {
        System.out.println(getClass() + ".eliminaFicherosPastry(" + context + ", "+usuario+")");
        File rootDirectory = ApplicationExtended.getInstance().getDir(NOMBRE_OBJETOS_PASTRY, Context.MODE_PRIVATE);
        eliminaRecursivamente(rootDirectory);
        if (usuario != null) {
            rootDirectory = new File(context.getFilesDir(), usuario + EXTENSION_LLAVERO_ANDROID);
            eliminaRecursivamente(rootDirectory);
        }
    }

    /**
     * Elimina de forma recursiva el directorio que se le pase como parametro.
     * Si se le pasa un fichero regular, lo borra y devuelve el control.
     * @param fileOrDirectory
     */
    private void eliminaRecursivamente(File fileOrDirectory) {
        System.out.println(getClass()+".eliminaRecursivamente("+fileOrDirectory+")");
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                eliminaRecursivamente(child);

        fileOrDirectory.delete();
    }
}
