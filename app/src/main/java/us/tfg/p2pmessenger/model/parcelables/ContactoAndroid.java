package us.tfg.p2pmessenger.model.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import us.tfg.p2pmessenger.model.Contacto;

/**
 * Clase contacto. Contiene un usuario y el alias
 * que se le asigna a ese usuario. El alias tiene
 * sentido local al usuario que lo guardo.
 */
public class ContactoAndroid extends Contacto implements Parcelable {

    public ContactoAndroid(Contacto base)
    {
        super(base.getAlias(),base.getUsuario());
    }
    // Metodos de la interfaz parcelable de android
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
        dest.writeString(getAlias());
        UsuarioAndroid ua = new UsuarioAndroid(getUsuario());
        ua.writeToParcel(dest, flags);
    }

    /**
     * Creador del objeto a partir de su forma serializada
     */
    public static final Parcelable.Creator<ContactoAndroid> CREATOR
            = new Parcelable.Creator<ContactoAndroid>() {
        /**
         * Deserializa
         * @param in
         * @return
         */
        public ContactoAndroid createFromParcel(Parcel in) {
            return new ContactoAndroid(in);
        }

        /**
         * Deserializa si se trata de un array
         * @param size
         * @return
         */
        public ContactoAndroid[] newArray(int size) {
            return new ContactoAndroid[size];
        }
    };

    /**
     * Constructor a partir de la forma serializada de android
     *
     * @param in
     * @see Parcelable
     */
    public ContactoAndroid(Parcel in) {
        super(in.readString(),new UsuarioAndroid(in));
    }

}
