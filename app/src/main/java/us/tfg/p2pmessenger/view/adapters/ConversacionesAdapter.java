package us.tfg.p2pmessenger.view.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Conversacion;

import static us.tfg.p2pmessenger.ActivityBase.TAG;

/**
 * Clase para formar la vista de cada entrada de la lista
 * de las conversaciones abiertas
 */

public class ConversacionesAdapter extends ArrayAdapter<Conversacion> {
    private final List<Conversacion> conversaciones;
    public ArrayList<Integer> selectedIds = new ArrayList<Integer>();
    private final Context mContext;
    private final LayoutInflater mInflater;
    private int mResource;

    private static int MAX_LENGTH_ALIAS = 17;
    private static int MAX_LENGTH_ULTIMO_MSJ = 30;

    /**
     * Constructor
     * @param context
     * @param resourceId
     * @param conversaciones
     */
    public ConversacionesAdapter(Context context, int resourceId, List<Conversacion> conversaciones) {
        super(context, resourceId);

        mContext = context;
        this.conversaciones = conversaciones;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resourceId;
    }

    /**
     * Carga una entrada de la lista de conversaciones
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(mResource, null);
            holder = new ViewHolder();
            holder.alias = (TextView) convertView.findViewById(R.id.conversacion_alias);
            holder.ultimo_msj = (TextView) convertView.findViewById(R.id.conversacion_ultimo_msj);
            holder.fecha = (TextView) convertView.findViewById(R.id.conversacion_fecha);
            holder.profile_pic = (ImageView) convertView.findViewById(R.id.contacto_profile_picture);
            holder.pendiente = (ImageView) convertView.findViewById(R.id.marca_no_leido);

            holder.tipo = (TextView) convertView.findViewById(R.id.tipo_conversacion);
            holder.idCompleto = (TextView) convertView.findViewById(R.id.id_conversacion_completo);
            holder.nombreCompleto = (TextView) convertView.findViewById(R.id.nombre_conversacion_completo);

            holder.posicion = position;

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final Conversacion conversacion = getItem(position);
        if (conversacion != null) {
            String texto = "";
            int min = 0;

            String alias;
            alias = conversacion.getAlias();

            // guardamos el nombre completo de la conversacion
            holder.nombreCompleto.setText(alias);
            min = Math.min(alias.length(), MAX_LENGTH_ALIAS);
            // limitamos la longitud
            texto = alias.substring(0, min);
            if (min == MAX_LENGTH_ALIAS && alias.length() > MAX_LENGTH_ALIAS) {
                texto = texto + "...";
            }
            holder.alias.setText(texto);


            String ultimo_mensaje = conversacion.getMensaje();
            if(ultimo_mensaje!=null) {
                min = Math.min(ultimo_mensaje.length(), MAX_LENGTH_ULTIMO_MSJ);
                texto = ultimo_mensaje.substring(0, min);
                if (min == MAX_LENGTH_ULTIMO_MSJ &&
                        ultimo_mensaje.length() > MAX_LENGTH_ULTIMO_MSJ) {
                    texto = texto + "...";
                }
            }

            // inicializa los campos
            holder.ultimo_msj.setText(texto);

            holder.fecha.setText(convierteFecha(conversacion.getFecha()));

            holder.idCompleto.setText(conversacion.getId().toStringFull());
            holder.tipo.setText(String.valueOf(conversacion.getTipo()));

            // se pone la imagen del tipo de conversacion
            if (conversacion.getTipo() == Conversacion.TIPO_INDIVIDUAL)
                holder.profile_pic.setImageResource(R.mipmap.user_icon);
            else if(conversacion.getTipo() == Conversacion.TIPO_GRUPO)
                holder.profile_pic.setImageResource(R.mipmap.group_icon);
            else
            {
                Log.d(TAG,"La conversacion no pertenece " +
                        "a ningun tipo conocido: tipo = "+conversacion.getTipo());
            }

            // si tiene mensajes pendientes
            if(conversacion.isPendiente())
                holder.pendiente.setVisibility(View.VISIBLE);
            else
                holder.pendiente.setVisibility(View.INVISIBLE);
        }
        // si esta seleccionado o no
        convertView.setBackgroundColor(selectedIds.contains(position)
                ? mContext.getResources().getColor(R.color.aluminum)
                : mContext.getResources().getColor(R.color.white));

        return convertView;
    }

    /**
     * Devuelve el texto que se incluira en el campo fecha de cada conversacion
     *
     * @param fecha momento del ultimo mensaje de esa conversacion
     * @return el texto que debera ser incluido
     */
    public String convierteFecha(Date fecha) {
        String resultado = "";

        Date hoy = new Date();
        SimpleDateFormat formato = new SimpleDateFormat("dd-mm-yyyy", Locale.getDefault());
        SimpleDateFormat formatoHora = new SimpleDateFormat(mContext.getString(R.string.formato_hora), Locale.getDefault());

        Calendar calendario = Calendar.getInstance();
        calendario.setTime(hoy);
        calendario.add(Calendar.DAY_OF_MONTH, -1);


        try {
            hoy = formato.parse(formato.format(hoy));
        } catch (ParseException e) {
            Log.w("P2PMessenger", "Excepcion al formatear la fecha");
        }

        if (fecha.after(hoy)) {
            resultado = formatoHora.format(fecha);
        } else if (fecha.before(calendario.getTime())) {
            resultado = formato.format(fecha);
        } else {
            resultado = mContext.getString(R.string.texto_ayer);
        }

        return resultado;
    }

    public static String secondsToString(int seconds) {
        int horas = (int) Math.ceil(seconds / (60 * 60));
        seconds -= horas * 60 * 60;
        int minutos = (int) Math.ceil(seconds / 60);
        seconds -= minutos * 60;

        String duracion = "";
        if (horas > 0)
            duracion += horas + "h ";
        if (minutos > 0 || horas > 0)
            duracion += minutos + "m ";
        duracion += seconds + "s";

        return duracion;
    }

    /**
     * Tamanio de la lista de conversaciones
     *
     * @return
     */
    @Override
    public int getCount() {
        return conversaciones.size();
    }

    /**
     * Devuelve el elemento en la posicion indicada como parametro
     *
     * @param position lugar que nos interesa
     * @return
     */
    @Override
    public Conversacion getItem(int position) {
        return conversaciones.get(position);
    }

    /**
     * Interfaz para facilitar la obtencion del id de la conversacion que
     * queremos borrar
     */
    public interface ConversacionEliminable {
        String getIdConversacion();
        int getPosicion();
    }

    /**
     * Holder para aumentar la eficiencia al no tener que hacer continuamente
     * findViewById.
     */
    public class ViewHolder  implements ConversacionEliminable{
        TextView alias;
        TextView ultimo_msj;
        TextView fecha;
        ImageView profile_pic;
        ImageView pendiente;
        TextView tipo;
        TextView idCompleto;
        TextView nombreCompleto;
        int posicion;

        @Override
        public String getIdConversacion() {
            return idCompleto.getText().toString();
        }

        @Override
        public int getPosicion() {
            return posicion;
        }
    }
}
