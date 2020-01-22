package us.tfg.p2pmessenger.model.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import us.tfg.p2pmessenger.controller.ManejadorClaves;
import us.tfg.p2pmessenger.model.Usuario;

import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Created by fpiriz on 1/9/17.
 */

public class UsuarioAndroid extends Usuario implements Parcelable {

    public UsuarioAndroid(Usuario base)
    {
        /*
        *  short tipo = buf.readShort();
        if (rice.pastry.Id.TYPE == tipo)
            this.id = rice.pastry.Id.build(buf);
        else
            throw new IOException("No se puede leer el id");
        this.nombre = buf.readUTF();
        this.version = 0;
        this.vacio = false;
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());
        this.fechaDeRegistro = df.parse(buf.readUTF());
        String tipoCertificado = buf.readUTF();
        String cert = buf.readUTF();
        this.certificado = ManejadorClaves.leeCertificado(cert, tipoCertificado);
        if (buf.readBoolean()) {
            tipo = buf.readShort();
            if (rice.pastry.Id.TYPE == tipo)
                this.bloqueMensajesImportantes = rice.pastry.Id.build(buf);
            else
                throw new IOException("No se puede leer el id del primer bloque de mensajes");
        }*/
        setId(base.getId());
        setNombre(base.getNombre());
        setVersion(base.getVersion());
        setVacio(base.estaVacio());
        setFechaDeRegistro(base.getFechaDeRegistro());
        setCertificado(base.getCertificado());
        setBloqueMensajesImportantes(base.getBloqueMensajesImportantes());

    }
    // interfaz percelable de android
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Metodo de serializacion de android (para que pase el
     * objeto a traves de los metodos de AIDL)
     *
     * @param dest
     * @param flags
     * @see Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId().toStringFull());
        dest.writeString(getNombre());
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());
        dest.writeString(df.format(getFechaDeRegistro()));
        //dest.writeByte((byte) (certificado!=null ? 1 : 0));
        //if(certificado!=null) {
        dest.writeString(getCertificado().getType());
        try {
            dest.writeString(ManejadorClaves.imprimeCertificado(getCertificado()));
        } catch (CertificateEncodingException e) {
            System.out.println("error al serializar la clave");
        }
        //}
        if (getBloqueMensajesImportantes() != null) {
            dest.writeByte((byte) (1));     //if myBoolean == true, byte == 1
            dest.writeString(getBloqueMensajesImportantes().toStringFull());
        } else {
            dest.writeByte((byte) (0));     //if myBoolean == true, byte == 1
        }
    }

    /**
     * Creador del objeto a partir de su forma serializada
     */
    public static final Parcelable.Creator<UsuarioAndroid> CREATOR
            = new Parcelable.Creator<UsuarioAndroid>() {
        /**
         * Deserializa
         * @param in
         * @return
         */
        public UsuarioAndroid createFromParcel(Parcel in) {
            return new UsuarioAndroid(in);
        }

        /**
         * Deserializa si se trata de un array
         * @param size
         * @return
         */
        public UsuarioAndroid[] newArray(int size) {
            return new UsuarioAndroid[size];
        }
    };

    // "De-parcel object

    /**
     * Constructor a partir de la forma serializada de android
     *
     * @param in
     * @see Parcelable
     */
    public UsuarioAndroid(Parcel in) {
        super();

        setId(rice.pastry.Id.build(in.readString()))
                .setNombre(in.readString())
                .setVersion(0)
                .setVacio(false);


        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());
        Date fechaReg=null;
        try {
            fechaReg= df.parse(in.readString());
        } catch (ParseException ignore) {
            fechaReg = null;
        }

        setFechaDeRegistro(fechaReg);

        String tipoCertificado = in.readString();
        String cert = in.readString();
        try {
            setCertificado(ManejadorClaves.leeCertificado(cert, tipoCertificado));
        } catch (IOException | CertificateException e) {
            setCertificado(null);
        }

        boolean existeBloque = in.readByte() != 0;     //myBoolean == true if byte != 0

        if (existeBloque) {
            setBloqueMensajesImportantes(rice.pastry.Id.build(in.readString()));
        }
    }
}
