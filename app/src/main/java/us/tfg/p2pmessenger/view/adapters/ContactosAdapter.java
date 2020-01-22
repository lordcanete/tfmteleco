package us.tfg.p2pmessenger.view.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Usuario;

import static android.content.ContentValues.TAG;


/**
 * Created by FPiriz on 6/8/17.
 */

public class ContactosAdapter extends ArrayAdapter<Contacto> {
    public static final int CONTACTOS_ACTIVITY = 1;
    public static final int PRINCIPAL_GRUPO_ACTIVITY = 2;
    public static final String FILA_GRUPO = "FILA_GRUPO";
    private final List<Contacto> mContactos;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private int mResource;
    private int tipo;
    private Usuario lider;

    private static int MAX_LENGTH_ALIAS = 13;

    /**
     * Constructor
     *
     * @param context
     * @param resourceId
     * @param contactos
     */
    public ContactosAdapter(Context context, int resourceId, ArrayList<Contacto> contactos) {

        super(context, resourceId);
        Log.d(TAG,getClass()+".<init>(context, res id = "+resourceId+", "+contactos+")");

        mContext = context;
        mContactos = contactos;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resourceId;

        tipo = 0;
        lider = null;
    }

    public void setLider(Usuario lider) {
        this.lider = lider;
    }

    /**
     * Carga una entrada de la lista de contactos
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG,getClass()+".getView(pos = "+position+", convertView, parent)");
        ViewHolder holder;


        // inicializa la clase auxiliar para facilitar el manejo de los campos
        if (convertView == null) {
            convertView = mInflater.inflate(mResource, null);
            holder = new ViewHolder();
            holder.alias = (TextView) convertView.findViewById(R.id.contacto_alias);
            holder.nombreUsuario = (TextView) convertView.findViewById(R.id.contacto_nombre_usuario);
            holder.id = (TextView) convertView.findViewById(R.id.contacto_id);
            holder.idCompleto = (TextView) convertView.findViewById(R.id.contacto_id_completo);
            holder.admin = (TextView) convertView.findViewById(R.id.admin_tag);
            holder.imagen = (ImageView) convertView.findViewById(R.id.contacto_profile_picture);


            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Contacto contacto = getItem(position);
        //establece el valor de los campos
        if (contacto != null) {
            String texto = "";
            int min = 0;
            String alias;

            if (!contacto.getAlias().equals(FILA_GRUPO)) {
                alias = contacto.getAlias();
                min = Math.min(alias.length(), MAX_LENGTH_ALIAS);
                texto = alias.substring(0, min);
                if (min == MAX_LENGTH_ALIAS && alias.length() > MAX_LENGTH_ALIAS) {
                    texto = texto + "...";
                }
                holder.alias.setText(texto);

                Usuario usuario = contacto.getUsuario();
                holder.nombreUsuario.setText(usuario.getNombre());

                holder.id.setText(usuario.getId().toString());

                holder.imagen.setImageResource(R.mipmap.user_icon);
                holder.idCompleto.setText(contacto.getUsuario().getId().toStringFull());

                if (tipo == CONTACTOS_ACTIVITY) {
                    holder.admin.setVisibility(View.INVISIBLE);
                } else if (this.lider != null
                        && contacto.getUsuario().equals(this.lider)
                        && tipo == PRINCIPAL_GRUPO_ACTIVITY) {
                    holder.admin.setVisibility(View.VISIBLE);
                } else
                {
                    holder.admin.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.alias.setText(mContext.getResources().getText(R.string.texto_nuevo_grupo));
                holder.nombreUsuario.setVisibility(View.INVISIBLE);
                holder.id.setVisibility(View.INVISIBLE);
                holder.admin.setVisibility(View.INVISIBLE);
                holder.imagen.setImageResource(R.drawable.create_group);
            }
        }

        return convertView;
    }

    /**
     * Tamanio de la lista de contactos
     *
     * @return
     */
    @Override
    public int getCount() {
        return mContactos.size();
    }

    /**
     * Devuelve el elemento en la posicion indicada como parametro
     *
     * @param position lugar que nos interesa
     * @return
     */
    @Override
    public Contacto getItem(int position) {
        return mContactos.get(position);
    }

    public ContactosAdapter setTipoVista(int tipo) {
        Log.d(TAG,getClass()+".setTipoVista(tipo = "+tipo+")");
        this.tipo = tipo;
        return this;
    }

    /**
     * Interfaz para facilitar la obtencion del id de la conversacion que
     * queremos borrar
     */
    public interface ContactoEliminable {
        String getIdContacto();
    }

    /**
     * Holder para aumentar la eficiencia al no tener que hacer continuamente
     * findViewById.
     */
    public class ViewHolder implements ContactoEliminable{
        TextView alias;
        TextView nombreUsuario;
        TextView id;
        TextView admin;
        ImageView imagen;
        TextView idCompleto;

        @Override
        public String getIdContacto() {
            return idCompleto.getText().toString();
        }
    }



}