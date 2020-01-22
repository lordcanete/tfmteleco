package us.tfg.p2pmessenger.model.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import us.tfg.p2pmessenger.controller.ManejadorClaves;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Usuario;

import static us.tfg.p2pmessenger.controller.Controlador.FORMATO_FECHA;

/**
 * Contiene los usuarios que pertenecen al grupo,
 * quien lo creo (el lider), el nombre, la fecha en
 * la que se creo y el certificado del grupo. Tambien
 * tiene la clave simetrica del grupo pero encriptada
 * con el certificado. Para que cuando alguien se
 * conecte desencripte con la clave privada y
 * comience a recibir los mensajes del grupo.
 */
public class GrupoAndroid extends Grupo implements Parcelable {

    public GrupoAndroid(Grupo base) {
        /*
        private ArrayList<Usuario> componentes;
        private Id idGrupo;
        private String nombre;
        private Id lider;
        private Date fechaCreacion;
        private String enlaceAServicio;
        private int version;
        private Key certificado;
        private String claveSimetricaCifrada;
        private Id bloqueMensajesImportantes;
        private transient SecretKey claveSimetrica;
        private transient Key clavePrivada;
        private transient KeyStore.PrivateKeyEntry entrada;

        public static final short TYPE=316;


         */

        setComponentes(base.getComponentes());
        setIdGrupo(base.getIdGrupo());
        setNombre(base.getNombre());
        setIdLider(base.getIdLider());
        setFechaCreacion(base.getFechaCreacion());
        setEnlaceAServicio(base.getEnlaceAServicio());
        setVersion(base.getVersion());
        setCertificado(base.getCertificado());
        setClaveSimetricaCifrada(base.getClaveSimetricaCifrada());
        setBloqueMensajesImportantes(base.getBloqueMensajesImportantes());

    }
    // interfaz Parcelable android

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

        dest.writeString(getIdLider().toStringFull());

        dest.writeInt(getVersion());

        dest.writeInt(getComponentes().size());
        for (Usuario u : getComponentes()) {
            UsuarioAndroid usuarioAndroid = new UsuarioAndroid(u);
            usuarioAndroid.writeToParcel(dest, flags);
        }

        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());

        dest.writeString(df.format(getFechaCreacion()));
        dest.writeString(getEnlaceAServicio());
        dest.writeString(getCertificado().getAlgorithm());
        byte[] key = getCertificado().getEncoded();

        dest.writeInt(key.length);
        dest.writeByteArray(key);

        if (getClaveSimetricaCifrada() != null)
            dest.writeString(getClaveSimetricaCifrada());
        else if (getClaveSimetrica() != null) {
            try {
                setClaveSimetricaCifrada(ManejadorClaves.encriptaClaveSimetrica
                        (this.getClaveSimetrica(), getCertificado()));
                dest.writeString(getClaveSimetricaCifrada());
            } catch (Exception e) {
                dest.writeString("error al encriptar la clave");
            }
        } else
            dest.writeString("no hay clave simetrica");

        dest.writeString(getBloqueMensajesImportantes().toStringFull());
    }

    /**
     * Creador del objeto a partir de su forma serializada
     */
    public static final Parcelable.Creator<GrupoAndroid> CREATOR
            = new Parcelable.Creator<GrupoAndroid>() {
        /**
         * Deserializa
         * @param in
         * @return
         */
        public GrupoAndroid createFromParcel(Parcel in) {
            return new GrupoAndroid(in);
        }

        /**
         * Deserializa si se trata de un array
         * @param size
         * @return
         */
        public GrupoAndroid[] newArray(int size) {
            return new GrupoAndroid[size];
        }
    };

    // "De-parcel object

    /**
     * Constructor a partir de la forma serializada de android
     *
     * @param in
     * @see Parcelable
     */
    public GrupoAndroid(Parcel in) {
        super();
        // construir grupo a partir de buffer

        setIdGrupo(rice.pastry.Id.build(in.readString()));
        setNombre(in.readString());

        setIdLider(rice.pastry.Id.build(in.readString()));

        setVersion(in.readInt());

        int numComponentes = in.readInt();
        ArrayList<Usuario> componentes = new ArrayList<>(numComponentes);
        for (int i = 0; i < numComponentes; i++) {
            componentes.add(new UsuarioAndroid(in));
        }
        setComponentes(componentes);

        DateFormat df = new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault());

        try {
            setFechaCreacion(df.parse(in.readString()));
        } catch (ParseException e) {
            setFechaCreacion(new Date());
        }
        setEnlaceAServicio(in.readString());
        String tipoCert = in.readString();

        byte[] key = new byte[in.readInt()];
        in.readByteArray(key);

        try {
            setCertificado(ManejadorClaves.leeClavePublica(key, tipoCert));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            setCertificado(null);
        }

        setClaveSimetricaCifrada(in.readString());
        setBloqueMensajesImportantes(rice.pastry.Id.build(in.readString()));
    }
}


