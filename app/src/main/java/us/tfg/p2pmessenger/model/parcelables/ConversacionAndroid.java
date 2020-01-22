package us.tfg.p2pmessenger.model.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import us.tfg.p2pmessenger.model.Conversacion;

/**
 * Created by fpiriz on 1/9/17.
 */

public class ConversacionAndroid extends Conversacion implements Parcelable {

    public ConversacionAndroid(Conversacion base)
    {
        super(base.getId(),base.getFecha(),base.getAlias(),base.getTipo());
    }
    // interfaz parcelable de android
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
        /*

          super(rice.pastry.Id.build(in.readString()),new Date(in.readLong()),
                in.readString(),in.readInt());
        setMensaje(in.readString());

        */

        dest.writeString(getId().toStringFull());
        dest.writeLong(getFecha().getTime());
        dest.writeString(getAlias());
        dest.writeInt(getTipo());
        dest.writeString(getMensaje());

    }

    /**
     * Creador del objeto a partir de su forma serializada
     */
    public static final Parcelable.Creator<ConversacionAndroid> CREATOR
            = new Parcelable.Creator<ConversacionAndroid>() {
        /**
         * Deserializa
         * @param in
         * @return
         */
        public ConversacionAndroid createFromParcel(Parcel in) {
            return new ConversacionAndroid(in);
        }

        /**
         * Deserializa si se trata de un array
         * @param size
         * @return
         */
        public ConversacionAndroid[] newArray(int size) {
            return new ConversacionAndroid[size];
        }
    };

    // "De-parcel object

    /**
     * Constructor a partir de la forma serializada de android
     *
     * @param in
     * @see Parcelable
     */
    public ConversacionAndroid(Parcel in) {
        super(rice.pastry.Id.build(in.readString()), new Date(in.readLong()),
                in.readString(), in.readInt());
        setMensaje(in.readString());
    }
}
