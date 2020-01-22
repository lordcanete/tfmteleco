package in.co.madhur.chatbubblesdemo.model;

/**
 * Created by madhur on 17/01/15.
 */
public class ChatMessage {

    private String contenido;
    private String alias;
    private UserType userType;
    private Status messageStatus;
    private int numeroContacto;
    private long fecha;
    private boolean importante;

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }


    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public void setMessageStatus(Status messageStatus) {
        this.messageStatus = messageStatus;
    }

    public String getContenido() {

        return contenido;
    }

    public Status getMessageStatus() {
        return messageStatus;
    }

    public int getNumeroContacto() {
        return numeroContacto;
    }

    public void setNumeroContacto(int numeroContacto) {
        this.numeroContacto = numeroContacto;
    }

    public void setUserType(UserType tipo){
        this.userType=tipo;
    }

    public UserType getUserType() {
        return userType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isImportante() {
        return importante;
    }

    public void setImportante(boolean importante) {
        this.importante = importante;
    }
}
