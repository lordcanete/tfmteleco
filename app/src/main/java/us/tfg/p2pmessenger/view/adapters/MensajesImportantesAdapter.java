package us.tfg.p2pmessenger.view.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import in.co.madhur.chatbubblesdemo.AndroidUtilities;
import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import in.co.madhur.chatbubblesdemo.widgets.Emoji;
import us.tfg.p2pmessenger.R;

import static in.co.madhur.chatbubblesdemo.ChatListAdapter.SIMPLE_DATE_FORMAT;

/**
 * Clase para formar la vista de cada entrada de la lista
 * de los mensajes importantes
 */
public class MensajesImportantesAdapter extends ArrayAdapter<ChatMessage> {

    private final String idConversacion;
    private ArrayDeque<ChatMessage> chatMessages;
    private Context context;
    private String aliasConversacion;
    public static final SimpleDateFormat FORMATO_FECHA_MENSAJES_IMPORTANTES =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public MensajesImportantesAdapter(Context context, int resourceId, ArrayDeque<ChatMessage> chatMessages, String aliasConversacion, String idConversacion) {
        super(context, resourceId);
        this.chatMessages = chatMessages;
        this.context = context;
        this.idConversacion = idConversacion;
        this.aliasConversacion = aliasConversacion;
    }

    @Override
    public int getCount() {
        return chatMessages.size();
    }

    @Override
    public ChatMessage getItem(int position) {
        ChatMessage ret = null;
        Iterator<ChatMessage> it = null;
        int i = 0;

        if (position > (chatMessages.size() / 2)) {
            it = chatMessages.descendingIterator();
            i = chatMessages.size() - 1;
            while (it.hasNext() && i != position) {
                i--;
                it.next();
            }
            ret = it.next();
        } else {
            it = chatMessages.iterator();
            i = 0;
            while (it.hasNext() && i != position) {
                i++;
                it.next();
            }
            ret = it.next();
        }
        return ret;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = null;

        ChatMessage message = getItem(position);

        if (message == null)
            return convertView;

        ViewMensajeImportante holder;


        if (convertView == null) {
            v = LayoutInflater.from(context).inflate(R.layout.fila_mensaje_importante, parent, false);

            holder = new ViewMensajeImportante();

            holder.messageTextView = (TextView) v.findViewById(R.id.message_text);
            holder.timeTextView = (TextView) v.findViewById(R.id.texto_hora);
            holder.dateTextView = (TextView) v.findViewById(R.id.texto_fecha);
            holder.origenTextView = (TextView) v.findViewById(R.id.origen);
            holder.destinoTextView = (TextView) v.findViewById(R.id.destino);

            v.setTag(holder);

        } else {
            v = convertView;
            holder = (ViewMensajeImportante) v.getTag();

        }

        holder.timeTextView.setText(SIMPLE_DATE_FORMAT.format(new Date(message.getFecha())));
        holder.dateTextView.setText(FORMATO_FECHA_MENSAJES_IMPORTANTES.format(new Date(message.getFecha())));

        holder.messageTextView.setText(Emoji.replaceEmoji(message.getContenido(), holder.messageTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16)));
        holder.origenTextView.setText(message.getAlias());
        holder.destinoTextView.setText(this.aliasConversacion);


        return v;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message != null)
            return message.getUserType().ordinal();
        else
            return 0;
    }


    // TODO al pulsar un boton del menu abrir un popup con texto para enviar
    // TODO mensaje importante a la conversacion en la que estamos. No hacer si
    // TODO id == null


    public interface MensajeImportanteCopiable {
        String getContenido();
    }

    class ViewMensajeImportante implements MensajeImportanteCopiable {
        TextView origenTextView;
        TextView destinoTextView;
        TextView messageTextView;
        TextView timeTextView;
        TextView dateTextView;


        @Override
        public String getContenido() {
            return messageTextView.getText().toString();
        }
    }
}
