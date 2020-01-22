package in.co.madhur.chatbubblesdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Locale;

import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import in.co.madhur.chatbubblesdemo.model.UserType;
import in.co.madhur.chatbubblesdemo.widgets.Emoji;
import us.tfg.p2pmessenger.R;

/**
 * Created by madhur on 17/01/15.
 */
public class ChatListAdapter extends BaseAdapter {

    private ArrayDeque<ChatMessage> chatMessages;
    private Context context;
    private int tipo;

    public static final int colores[] = {
            R.color.color_nombre_usuario_grupo_0,
            R.color.color_nombre_usuario_grupo_1,
            R.color.color_nombre_usuario_grupo_2,
            R.color.color_nombre_usuario_grupo_3,
            R.color.color_nombre_usuario_grupo_4,
            R.color.color_nombre_usuario_grupo_5,
            R.color.color_nombre_usuario_grupo_6,
            R.color.color_nombre_usuario_grupo_7,
            R.color.color_nombre_usuario_grupo_8,
            R.color.color_nombre_usuario_grupo_9,
            R.color.color_nombre_usuario_grupo_10,
            R.color.color_nombre_usuario_grupo_11,
            R.color.color_nombre_usuario_grupo_12,
            R.color.color_nombre_usuario_grupo_13,
            R.color.color_nombre_usuario_grupo_14,
            R.color.color_nombre_usuario_grupo__1
    };

    public static int num_colores = 16;

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatListAdapter(ArrayDeque<ChatMessage> chatMessages,
                           Context context, int tipo) {
        this.chatMessages = chatMessages;
        this.context = context;
        this.tipo = tipo;
    }

    @Override
    public int getCount() {
        return chatMessages.size();
    }

    @Override
    public Object getItem(int position) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = null;

        ChatMessage message = (ChatMessage) getItem(position);

        ViewMensajeEntrante holderEntrante;
        ViewMensajeSaliente holderSaliente;
        ViewFecha holderFecha;

        if (message.getUserType() == UserType.GROUP) {
            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.mensaje_entrante_grupo, parent, false);
                holderEntrante = new ViewMensajeEntrante();

                holderEntrante.fila = (RelativeLayout) v.findViewById(R.id.fila_mensaje_entrante_grupo);
                holderEntrante.messageTextView = (TextView) v.findViewById(R.id.message_text);
                holderEntrante.timeTextView = (TextView) v.findViewById(R.id.time_text);
                holderEntrante.authorNameTextView = (TextView) v.findViewById(R.id.chat_company_reply_author);

                v.setTag(holderEntrante);
            } else {
                v = convertView;
                holderEntrante = (ViewMensajeEntrante) v.getTag();

            }

            holderEntrante.messageTextView.setText(Emoji.replaceEmoji(message.getContenido(),
                    holderEntrante.messageTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16)));
            holderEntrante.timeTextView.setText(SIMPLE_DATE_FORMAT.format(message.getFecha()));
            holderEntrante.authorNameTextView.setText(message.getAlias());
            if (convertView != null) {
                holderEntrante.authorNameTextView.setTextColor(convertView.getResources()
                        .getColor(colores[message.getNumeroContacto()]));
                if (message.isImportante()) {
                    holderEntrante.fila.setBackgroundColor(convertView.getResources()
                            .getColor(R.color.fondo_mensaje_importante));
                } else {
                    holderEntrante.fila.setBackgroundColor(0);
                }
            }
        } else if (message.getUserType() == UserType.PRIVATE) {
            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.mensaje_entrante_individual, null, false);
                holderEntrante = new ViewMensajeEntrante();

                holderEntrante.fila = (RelativeLayout) v.findViewById(R.id.fila_mensaje_entrante_privado);
                holderEntrante.messageTextView = (TextView) v.findViewById(R.id.message_text);
                holderEntrante.timeTextView = (TextView) v.findViewById(R.id.time_text);
                holderEntrante.authorNameTextView = null;

                v.setTag(holderEntrante);
            } else {
                v = convertView;
                holderEntrante = (ViewMensajeEntrante) v.getTag();

            }

            holderEntrante.messageTextView.setText(Emoji.replaceEmoji(message.getContenido(),
                    holderEntrante.messageTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16)));
            holderEntrante.timeTextView.setText(SIMPLE_DATE_FORMAT.format(message.getFecha()));
            if (convertView != null && message.isImportante()) {
                holderEntrante.fila.setBackgroundColor(convertView.getResources()
                        .getColor(R.color.fondo_mensaje_importante));
            } else {
                holderEntrante.fila.setBackgroundColor(0);
            }
        } else if (message.getUserType() == UserType.SELF) {

            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.mensaje_saliente, null, false);

                holderSaliente = new ViewMensajeSaliente();

                holderSaliente.fila = (RelativeLayout) v.findViewById(R.id.fila_mensaje_saliente);
                holderSaliente.messageTextView = (TextView) v.findViewById(R.id.message_text);
                holderSaliente.timeTextView = (TextView) v.findViewById(R.id.time_text);
                holderSaliente.messageStatus = (ImageView) v.findViewById(R.id.user_reply_status);

                v.setTag(holderSaliente);

            } else {
                v = convertView;
                holderSaliente = (ViewMensajeSaliente) v.getTag();

            }

            holderSaliente.messageTextView.setText(Emoji.replaceEmoji(message.getContenido(),
                    holderSaliente.messageTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16)));
            //holder2.messageTextView.setText(message.getContenido());
            holderSaliente.timeTextView.setText(SIMPLE_DATE_FORMAT.format(message.getFecha()));

            // como no se ha implementado, el unico estado sera enviado
            //if (message.getMessageStatus() == Status.DELIVERED) {
            //holderSaliente.messageStatus.setImageDrawable(context.getResources()
            //         .getDrawable(R.drawable.ic_double_tick));
            //} else if (message.getMessageStatus() == Status.SENT) {
            holderSaliente.messageStatus.setImageDrawable(context
                    .getResources().getDrawable(R.drawable.ic_single_tick));
            //}
            if (convertView != null && message.isImportante()) {
                holderSaliente.fila.setBackgroundColor(convertView.getResources()
                        .getColor(R.color.fondo_mensaje_importante));
            } else {
                holderSaliente.fila.setBackgroundColor(0);
            }

        } else if (message.getUserType() == UserType.DATE) {
            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.date_layout, parent, false);
                holderFecha = new ViewFecha();

                holderFecha.fechaTextView = (TextView) v.findViewById(R.id.text_fecha);
                v.setTag(holderFecha);
            } else {
                v = convertView;
                holderFecha = (ViewFecha) v.getTag();
            }

            holderFecha.fechaTextView.setText(message.getContenido());
        } else if (message.getUserType() == UserType.CONTROL) {
            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.date_layout, parent, false);
                holderFecha = new ViewFecha();

                holderFecha.fechaTextView = (TextView) v.findViewById(R.id.text_fecha);
                v.setTag(holderFecha);
            } else {
                v = convertView;
                holderFecha = (ViewFecha) v.getTag();
            }
            holderFecha.fechaTextView.setText(message.getContenido());
        }

        return v;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = (ChatMessage) getItem(position);
        return message.getUserType().ordinal();
    }

    interface MensajeCopiable {
        String getContenido();
    }

    class ViewMensajeEntrante implements MensajeCopiable {
        RelativeLayout fila;
        TextView messageTextView;
        TextView timeTextView;
        TextView authorNameTextView;

        @Override
        public String getContenido() {
            return messageTextView.getText().toString();
        }
    }

    class ViewMensajeSaliente implements MensajeCopiable {
        RelativeLayout fila;
        ImageView messageStatus;
        TextView messageTextView;
        TextView timeTextView;

        @Override
        public String getContenido() {
            return messageTextView.getText().toString();
        }
    }

    class ViewFecha implements MensajeCopiable {
        TextView fechaTextView;

        @Override
        public String getContenido() {
            return fechaTextView.getText().toString();
        }
    }
}
