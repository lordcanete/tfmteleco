
package us.tfg.p2pmessenger;

import org.junit.Test;
import us.tfg.p2pmessenger.controller.GoogleDriveController;

import com.google.api.services.drive.model.File;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;



public class GoogleDriveTest {

    private GoogleDriveController gDriveController;
    private final String nombreDirectorioTest = "P2P-Messenger-test";
    private final String nombreArchivoTest = "test-archivo.txt";
    private final String formatoArchivoTest = "text/plain";
    private final String rutaArchivoTest = "/home/pcano/tfmteleco/P2PMessenger-testContentFolder/test.txt";
    private final String formatoDirectorio = "application/vnd.google-apps.folder";
    private final String formatoArchivo = "application/vnd.google-apps.file";
    
    

    public GoogleDriveTest() throws Exception{
        this.gDriveController = new GoogleDriveController();
        
    }  
    @Test
    public void testListarArchivos() throws Exception {        
        System.out.println("----------------------------------\n"+
                            "-----   TEST testListarArchivos---\n"+
                            "----------------------------------");
        List<File> files = this.gDriveController.listarArchivos();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }
    }
    @Test
    public void testCrearDirectorio() throws Exception {
        System.out.println("----------------------------------\n"+
                            "-----   TEST testCrearDirectorio---\n"+
                            "----------------------------------");
        File directorio = this.gDriveController.crearDirectorio(nombreDirectorioTest, this.formatoDirectorio);
        System.out.println("Created folder with id= "+ directorio.getId());
        System.out.println("                    name= "+ directorio.getName());
    }
    @Test
    public void testBuscarDirectorio() throws Exception {
        System.out.println("----------------------------------\n"+
                            "-----   TEST testBuscarDirectorio---\n"+
                            "----------------------------------");
        String filtro = "name='"+this.nombreDirectorioTest+"' and mimeType = '"+this.formatoDirectorio+"'";
        System.out.println(filtro);
        List<File> files = this.gDriveController.buscarArchivosDirectorios(filtro);
        for (File file : files) {
            System.out.printf("Contenido encontrado: %s (%s)\n",
                file.getName(), file.getId());
        }
    }
    @Test
    public void testBuscarArchivo() throws Exception {
        System.out.println("----------------------------------\n"+
                            "-----   TEST testBuscarArchivo---\n"+
                            "----------------------------------");
        String filtro = "name='"+this.nombreArchivoTest+"'";
        System.out.println(filtro);
        List<File> files = this.gDriveController.buscarArchivosDirectorios(filtro);
        for (File file : files) {
            System.out.printf("Contenido encontrado: %s (%s)\n",
                file.getName(), file.getId());
        }
    }
    @Test
    public void testCrearArchivo() throws Exception {
        System.out.println("----------------------------------\n"+
                            "-----   TEST testCrearArchivo---\n"+
                            "----------------------------------");
        String filtro = "name='"+this.nombreDirectorioTest+"' and mimeType = '"+this.formatoDirectorio+"'";        
        List<File> files = this.gDriveController.buscarArchivosDirectorios(filtro);
        String folderId = files.get(0).getId();        
        File archivoCreado = this.gDriveController.crearArchivoDesdeRuta(folderId, this.nombreArchivoTest, this.rutaArchivoTest, Files.probeContentType(Paths.get(this.rutaArchivoTest)));       
        System.out.println("Archivo creado con    id= "+ archivoCreado.getId());
        System.out.println("                    name= "+ archivoCreado.getName());
        
    }
    @Test
    public void testCompartirArchivo() throws Exception{
        System.out.println("----------------------------------\n"+
                            "-----   TEST testCompartirArchivo---\n"+
                            "----------------------------------");
        String filtro = "name='"+this.nombreArchivoTest+"'";
        System.out.println(filtro);
        List<File> files = this.gDriveController.buscarArchivosDirectorios(filtro);
        String idFile = files.get(0).getId();
        String enlaceCompartirArchivo = this.gDriveController.obtenerEnlaceCompartirArchivo(idFile);
        System.out.printf("Enlace para compartir archivo: %s\n", enlaceCompartirArchivo);        
    }

    
    public void testSuite(){
        try {
            testListarArchivos();
            testBuscarDirectorio();
            testCrearArchivo();
            testBuscarArchivo();
            testCompartirArchivo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}

