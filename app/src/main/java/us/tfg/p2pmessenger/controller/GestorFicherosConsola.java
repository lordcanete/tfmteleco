package us.tfg.p2pmessenger.controller;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by FPiriz on 27/6/17.
 */
public class GestorFicherosConsola
{
    public GestorFicherosConsola()
    {

    }

    public void guardaEnFichero(String nombreFichero,byte contenido[]) throws IOException
    {
        File f=new File(nombreFichero);
        if(f.exists()&&!f.isDirectory())
            throw new IOException("El fichero "+nombreFichero+" ya existe, elija un nombre nuevo");
        else
        {
            FileOutputStream fos = new FileOutputStream(nombreFichero);
            fos.write(contenido);
            fos.close();
        }
    }

    public byte[] leeDeFichero(String nombreFichero) throws IOException
    {
        File f = new File(nombreFichero);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[(int)f.length()];
        dis.readFully(bytes);
        dis.close();
        return bytes;
    }


    /**
     *
     * @param nombreFichero Fichero en el que se desea escribir
     * @param content Lo que se quiere escribir en el fichero
     * @param alFinal Indica si escribir el contenido al final del fichero o sobreescribir
     *                el contenido
     */
    public void escribirAFichero(String nombreFichero,String content,boolean alFinal) throws IOException
    {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(nombreFichero,alFinal)));
            out.println(content);
            out.close();
    }


    /**
     *
     * @param nombreFichero Fichero en el que se desea escribir
     * @param content Lo que se quiere escribir en el fichero
     * @param alFinal Indica si escribir el contenido al final del fichero o sobreescribir
     *                el contenido
     * @return
     */
    public void escribirAFichero(String nombreFichero,byte[] content,boolean alFinal) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(nombreFichero,alFinal);
        fos.write(content);
        fos.close();
//        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(nombreFichero,alFinal)));
//            out.println(Base64.getEncoder().encodeToString(content));
//            out.close();
    }


    public boolean existeFichero(String nombreFichero)
    {
        boolean existe=false;
        File f = new File(nombreFichero);
        if(f.exists() && !f.isDirectory()) {
            existe=true;
        }
        return existe;
    }

    public void eliminaFichero(String nombre)
    {
        if(existeFichero(nombre))
        {
                Path ruta = Paths.get(nombre);
            try
            {
                Files.delete(ruta);
            } catch (IOException e)
            {
//                e.printStackTrace();
                // no deberia ocurrir nunca
            }
        }
    }

}
