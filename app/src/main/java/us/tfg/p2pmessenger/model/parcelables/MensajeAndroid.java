package us.tfg.p2pmessenger.model.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import us.tfg.p2pmessenger.model.Mensaje;

import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Created by fpiriz on 1/9/17.
 */

public class MensajeAndroid extends Mensaje implements Parcelable {

    public MensajeAndroid(Mensaje base)
    {
        /*
        private int clase;
        private Id destino;
        private Id origen;
        private String contenido;
        private Date fecha;
        private int priority;
        */

        setClase(base.getClase());
        setDestino(base.getDestino());
        setOrigen(base.getOrigen());
        setContenido(base.getContenido());
        setFecha(base.getFecha());
        setPriority(base.getPriority());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Metodo de serializacion de android (para que pase el
     * objeto a traves de los metodos de AIDL)
     * @see Parcelable
     * @param dest
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());

        dest.writeString(getOrigen().toStringFull());
        dest.writeString(getDestino().toStringFull());
        dest.writeString(getContenido());
        dest.writeInt(getClase());
        dest.writeString(df.format(getFecha()));
        dest.writeInt(getPriority());
    }

    /**
     * Creador del objeto a partir de su forma serializada
     */
    public static final Parcelable.Creator<MensajeAndroid> CREATOR
            = new Parcelable.Creator<MensajeAndroid>() {
        /**
         * Deserializa
         * @param in
         * @return
         */
        public MensajeAndroid createFromParcel(Parcel in) {
            return new MensajeAndroid(in);
        }

        /**
         * Deserializa si se trata de un array
         * @param size
         * @return
         */
        public MensajeAndroid[] newArray(int size) {
            return new MensajeAndroid[size];
        }
    };

    // "De-parcel object
    /**
     * Constructor a partir de la forma serializada de android
     * @see Parcelable
     * @param in
     */
    public MensajeAndroid(Parcel in) {
        super();

        /*
          dest.writeString(getOrigen().toStringFull());
        dest.writeString(getDestino().toStringFull());
        dest.writeString(getContenido());
        dest.writeInt(getClase());
        dest.writeString(df.format(getFecha()));
        dest.writeInt(getPriority());
         */

        setOrigen(rice.pastry.Id.build(in.readString()));
        setDestino(rice.pastry.Id.build(in.readString()));
        setContenido(in.readString());
        setClase(in.readInt());
        DateFormat df = new SimpleDateFormat(FORMATO_FECHA,Locale.getDefault());
        Date fecha=null;
        try
        {
            fecha=df.parse(in.readString());
        } catch (ParseException e)
        {
            fecha=new Date();
        }

        setFecha(fecha);
        setPriority(in.readInt());
    }
}
