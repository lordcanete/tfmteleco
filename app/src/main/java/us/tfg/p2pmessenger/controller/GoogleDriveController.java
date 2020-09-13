package us.tfg.p2pmessenger.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDriveController {
    private static final String APPLICATION_NAME = "P2P Messenger";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private final NetHttpTransport HTTP_TRANSPORT;
    private Drive service;

    public GoogleDriveController() throws IOException, GeneralSecurityException{
        // Build a new authorized API client service.
        this.HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.service = new Drive.Builder(this.HTTP_TRANSPORT, this.JSON_FACTORY, getCredentials(this.HTTP_TRANSPORT))
            .setApplicationName(this.APPLICATION_NAME)
            .build();
    }

    public Drive getService(){
        return this.service;
    }

    public void setService(Drive driveService){
        this.service = driveService;
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveController.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public List<File> listarArchivos(Drive driveService) throws IOException {       

        FileList result = driveService.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        return files;        
    }

    public File crearArchivo(String folderId, String fileName, String fileLocalPath, String fileFormat, Drive driveService) throws IOException{        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(fileLocalPath);
        FileContent mediaContent = new FileContent(fileFormat, filePath);
        File file = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id, parents, name, webViewLink")
            .execute();        
        return file;
    }

    public File crearDirectorio(String folderName, String folderMimeType, Drive driveService) throws IOException{
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType(folderMimeType);

        File file = driveService.files().create(fileMetadata)
            .setFields("id, name")
            .execute();
        return file;
    }
    
    public List<File> buscarArchivosDirectorios(String filtro, Drive driveService) throws IOException{
        FileList result = driveService.files().list()
            .setQ(filtro)
            .setFields("files(id, name)")
            .execute();
        List<File> files = result.getFiles();
        return files;
    }

    public String obtenerEnlaceCompartirArchivo(String idFile, Drive driveService) throws IOException{
        File archivo = driveService.files().get(idFile).setFields("id,name,webViewLink").execute();
        String enlace = archivo.getWebViewLink();
        return enlace;
    }


    
}
