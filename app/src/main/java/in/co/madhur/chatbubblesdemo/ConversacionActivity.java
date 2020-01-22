package in.co.madhur.chatbubblesdemo;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import in.co.madhur.chatbubblesdemo.model.UserType;
import in.co.madhur.chatbubblesdemo.widgets.Emoji;
import in.co.madhur.chatbubblesdemo.widgets.EmojiView;
import in.co.madhur.chatbubblesdemo.widgets.SizeNotifierRelativeLayout;
import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.ApplicationExtended;
import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.view.MensajesImportantesActivity;
import us.tfg.p2pmessenger.view.MostrarCodigoQRActivity;
import us.tfg.p2pmessenger.view.PrincipalGrupoActivity;

/**
 * Tomado del proyecto que se describe
 * @see <a href="http://www.madhur.co.in/blog/2015/03/06/emoji-bubbles-android.html">aqui</a>
 * Para la implementacion de los mensajes. Se ha editado esta clase y el
 * adaptador para que funcione segun a los requerimientos del controlador
 * y la funcionalidad deseada. Creados tambien nuevos tipos de mensajes,
 * como el bocadillo de fecha y los mensajes de entrada y salida de un
 * usuario del grupo. Los emojis estan implementados con una biblioteca
 * nativa (JNI)
 */
public class ConversacionActivity extends ActivityBase
        implements SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate,
        NotificationCenter.NotificationCenterDelegate {

    //cantidad de mensajes que carga cada vez
    private static final int NUMERO_MENSAJES_A_CARGAR = 40;

    // mensajes actualmente cargados
    private int mensajesCargados;

    //total de mensajes mostrados
    private int totalMensajes;

    // si quedan mensajes por mostrar de esta conversacion
    private boolean finMensajes;

    // formato de la decha
    public static final SimpleDateFormat FORMATO_FECHA_COMPROBACION = new SimpleDateFormat("dd' de 'MMMM' del 'yyyy", Locale.getDefault());

    // tipo de conversacion actual (grupo o individual)
    private int tipo;
    private String id;
    private String alias;
    private String extras;

    private int preLast;

    private ListView chatListView;
    private EditText chatEditText1;
    private ArrayDeque<ChatMessage> chatMessages;
    private ImageView enterChatView1, emojiButton;
    private ChatListAdapter listAdapter;
    private EmojiView emojiView;
    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;
    private boolean showingEmoji;
    private int keyboardHeight;
    private boolean keyboardVisible;
    private WindowManager.LayoutParams windowLayoutParams;


    private EditText.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on key press

                EditText editText = (EditText) v;

                if (v == chatEditText1) {
                    sendMessage(editText.getText().toString(), UserType.SELF, false);
                }

                chatEditText1.setText("");

                return true;
            }
            return false;

        }
    };

    private ImageView.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (v == enterChatView1) {
                sendMessage(chatEditText1.getText().toString(), UserType.SELF, false);
            }

            chatEditText1.setText("");

        }
    };

    private final TextWatcher watcher1 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if (chatEditText1.getText().toString().equals("")) {

            } else {
                enterChatView1.setImageResource(R.drawable.ic_chat_send);

            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() == 0) {
                enterChatView1.setImageResource(R.drawable.ic_chat_send);
            } else {
                enterChatView1.setImageResource(R.drawable.ic_chat_send_active);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        controlador = ApplicationExtended.getInstance().setCurrentActivity(this);
        setContentView(R.layout.activity_conversacion);

        AndroidUtilities.statusBarHeight = getStatusBarHeight();

        id = "";
        tipo = 0;
        alias = "";

        extras = getIntent().getExtras().getString("conversacion");
        try {
            JSONObject reader = new JSONObject(extras);
            alias = reader.getString("alias");
            id = reader.getString("id");
            tipo = Integer.parseInt(reader.getString("tipo"));
        } catch (JSONException e) {
            Log.d(TAG, "Error JSON", e);
            Toast.makeText(getBaseContext(),
                    "Error al abir la conversacion", Toast.LENGTH_SHORT).show();
        }


        if (!controlador.isIniciado()) {
            Toast.makeText(getBaseContext(),
                    "Error al abir la conversacion", Toast.LENGTH_SHORT).show();
            return;
        }

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(alias);
            //ab.setSubtitle((contactos.size()-1)+" contactos");
            ab.setDisplayHomeAsUpEnabled(true);
        }

        ArrayDeque<ChatMessage> mensajes = controlador.obtieneMensajes(id, 0, NUMERO_MENSAJES_A_CARGAR, tipo);
        mensajesCargados = mensajes.size();
        chatMessages = new ArrayDeque<>();


        ChatMessage previous = null;
        for(ChatMessage message: mensajes)
        {
            if (chatMessages.size() > 0)
                previous = chatMessages.getLast();

            if (previous != null) {
                if (previous.getUserType() != UserType.DATE) {
                    String fecha_prev = FORMATO_FECHA_COMPROBACION.format(new Date(previous.getFecha()));
                    String fecha_actual = FORMATO_FECHA_COMPROBACION.format(new Date(message.getFecha()));
                    if (!fecha_prev.equals(fecha_actual)) {
                        ChatMessage mensajeFecha = new ChatMessage();
                        mensajeFecha.setContenido(fecha_actual);
                        mensajeFecha.setUserType(UserType.DATE);
                        chatMessages.addLast(mensajeFecha);
                    }
                }
            } else {
                ChatMessage mensajeFecha = new ChatMessage();
                String fecha_actual = FORMATO_FECHA_COMPROBACION.format(message.getFecha());
                mensajeFecha.setContenido(fecha_actual);
                mensajeFecha.setUserType(UserType.DATE);
                chatMessages.addLast(mensajeFecha);
            }

            chatMessages.addLast(message);
            previous = message;
        }


        chatListView = (ListView) findViewById(R.id.chat_list_view);
        /*
        chatListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                int duration = Toast.LENGTH_SHORT;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ChatListAdapter.MensajeCopiable tag = (ChatListAdapter.MensajeCopiable) arg1.getTag();

                ClipData clip = ClipData.newPlainText("", tag.getContenido());
                clipboard.setPrimaryClip(clip);

                Toast toast = Toast.makeText(getBaseContext(), R.string.texto_mensaje_copiado, duration);
                toast.show();
                return true;
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(ConversacionActivity.this, arg1);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.menu_popup_onlongclick_msj_grupo, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        Toast.makeText(ConversacionActivity.this, "You Clicked : " +
                                item.getTitle(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

                popup.show();//showing popup menu
                return true;
            }
        });
        */

        registerForContextMenu(chatListView);

        chatEditText1 = (EditText) findViewById(R.id.chat_edit_text1);
        enterChatView1 = (ImageView) findViewById(R.id.enter_chat1);

        // Hide the emoji on click of edit text
        chatEditText1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (showingEmoji)
                    hideEmojiPopup();
            }
        });


        emojiButton = (ImageView) findViewById(R.id.emojiButton);

        emojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmojiPopup(!showingEmoji);
            }
        });

        listAdapter = new ChatListAdapter(chatMessages, this, tipo);

        chatListView.setAdapter(listAdapter);

        chatEditText1.setOnKeyListener(keyListener);

        enterChatView1.setOnClickListener(clickListener);

        chatEditText1.addTextChangedListener(watcher1);

        sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) findViewById(R.id.chat_layout);
        sizeNotifierRelativeLayout.delegate = this;

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        finMensajes = false;

        chatListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last fila_conversacion so it will scroll into view...
                chatListView.setSelection(listAdapter.getCount() - 1);
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();


        chatListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && listIsAtTop()) {
                    Log.d(TAG, ConversacionActivity.this.getClass() + ".onScroll() - list at top ");

                    if (!finMensajes) {
                        ArrayDeque<ChatMessage> cargados = controlador.obtieneMensajes(id, mensajesCargados,
                                mensajesCargados + ConversacionActivity.NUMERO_MENSAJES_A_CARGAR, tipo);
                        mensajesCargados += cargados.size();
                        finMensajes = cargados.size() == 0;
                        if (!finMensajes) {
                            Iterator<ChatMessage> it = cargados.descendingIterator();
                            ChatMessage primero = chatMessages.getFirst();
                            chatMessages.remove(primero);
                            primero = chatMessages.getFirst();
                            while (it.hasNext()) {
                                ChatMessage insertar = it.next();

                                if (primero.getUserType() != UserType.DATE) {
                                    String fecha_prev = FORMATO_FECHA_COMPROBACION.format(new Date(insertar.getFecha()));
                                    String fecha_actual = FORMATO_FECHA_COMPROBACION.format(new Date(primero.getFecha()));
                                    if (!fecha_prev.equals(fecha_actual)) {
                                        ChatMessage mensajeFecha = new ChatMessage();
                                        mensajeFecha.setContenido(fecha_actual);
                                        mensajeFecha.setUserType(UserType.DATE);
                                        chatMessages.addFirst(mensajeFecha);
                                    }

                                    chatMessages.addFirst(insertar);
                                } else {
                                    chatMessages.addFirst(insertar);
                                }

                                primero = insertar;
                            }
                            String fecha = FORMATO_FECHA_COMPROBACION.format(new Date(primero.getFecha()));
                            ChatMessage mensajeFecha = new ChatMessage();
                            mensajeFecha.setContenido(fecha);
                            mensajeFecha.setUserType(UserType.DATE);
                            chatMessages.addFirst(mensajeFecha);

                            if (listAdapter != null)
                                listAdapter.notifyDataSetChanged();
                        }
                    }
                } else {
                    switch (chatListView.getId()) {
                        case R.id.chat_list_view:

                            // Make your calculation stuff here. You have all your
                            // needed info from the parameters of this function.

                            // Sample calculation to determine if the last
                            // item is fully visible.
                            final int lastItem = firstVisibleItem + visibleItemCount;

                            if (lastItem == totalItemCount) {
                                if (preLast != lastItem) {
                                    //to avoid multiple calls for last item
                                    Log.d(TAG, ConversacionActivity.this.getClass() + ".onScroll() - list at bottom ");
                                    preLast = lastItem;
                                }
                            }
                    }
                }
            }
        });

    }


    private boolean listIsAtTop() {
        if (chatListView.getChildCount() == 0) return true;
        return chatListView.getChildAt(0).getTop() == 0;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        switch (tipo) {
            case Conversacion.TIPO_GRUPO:
                inflater.inflate(R.menu.menu_popup_onlongclick_msj_grupo, menu);
                break;
            case Conversacion.TIPO_INDIVIDUAL:
                inflater.inflate(R.menu.menu_popup_onlongclick_msj_individual, menu);
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, getClass() + ".onCreateOptionsMenu(" + menu + ")");
        // Inflate the menu; this adds items to the action bar if it is present.
        switch (tipo) {
            case Conversacion.TIPO_GRUPO:
                getMenuInflater().inflate(R.menu.menu_conversacion_grupo, menu);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.ir_a_mensajes_importantes:
                Log.d(TAG, "Ver los mensajes importantes");
                intent = new Intent(this, MensajesImportantesActivity.class);
                intent.putExtra("idConversacion", getIdConversacion());
                break;
            case R.id.ver_pagina_principal_grupo:
                Log.d(TAG, "Ver la pagian principal");
                intent = new Intent(this, PrincipalGrupoActivity.class);
                intent.putExtra("idConversacion", getIdConversacion());
                break;
            case R.id.invitar_a_grupo:
                Log.d(TAG, "Opcion invitar");
                intent = new Intent(this, MostrarCodigoQRActivity.class);
                String codigo = controlador.obtenerCodigoInvitacion(getIdConversacion());
                String miId = controlador.getMyId().toStringFull();
                intent.putExtra("texto", miId + "-" + codigo);
                startActivity(intent);
                break;
            case R.id.salir_del_grupo:
                Log.d(TAG, "Opcion salir del grupo");
                // TODO salir del grupo
                controlador.abandonaGrupo(getIdConversacion());
                finish();
                break;
            default:
                Log.d(TAG, "Opcion desconocida");
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        if (intent != null)
            startActivity(intent);
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ChatListAdapter.MensajeCopiable tag = null;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        View view = info.targetView;

        switch (item.getItemId()) {
            case R.id.copiar:
                tag = (ChatListAdapter.MensajeCopiable) view.getTag();
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", tag.getContenido());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getBaseContext(), R.string.texto_mensaje_copiado,
                        Toast.LENGTH_SHORT).show();
                return true;
            case R.id.importante:
                tag = (ChatListAdapter.MensajeCopiable) view.getTag();
                sendMessage(tag.getContenido(), UserType.SELF, true);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void sendMessage(final String messageText, final UserType userType, boolean importante) {
        Log.d(TAG, getClass() + ".sendMessage(" + messageText + ", " + userType + ", importante " + importante + ")");
        if (messageText.trim().length() == 0)
            return;

        chatListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last fila_conversacion so it will scroll into view...
                chatListView.setSelection(listAdapter.getCount() - 1);
            }
        });


        ChatMessage message = new ChatMessage();
        message.setContenido(messageText);
        message.setFecha(new Date().getTime());
        message.setUserType(UserType.SELF);
        message.setImportante(importante);


        ChatMessage previous = null;
        if (chatMessages.size() > 0)
            previous = chatMessages.getLast();

        if (previous != null) {
            if (previous.getUserType() != UserType.DATE) {
                String fecha_prev = FORMATO_FECHA_COMPROBACION.format(new Date(previous.getFecha()));
                String fecha_actual = FORMATO_FECHA_COMPROBACION.format(new Date(message.getFecha()));
                if (!fecha_prev.equals(fecha_actual)) {
                    ChatMessage mensajeFecha = new ChatMessage();
                    mensajeFecha.setContenido(fecha_actual);
                    mensajeFecha.setUserType(UserType.DATE);
                    chatMessages.addLast(mensajeFecha);
                }
            }
        } else {
            ChatMessage mensajeFecha = new ChatMessage();
            String fecha_actual = FORMATO_FECHA_COMPROBACION.format(new Date());
            mensajeFecha.setContenido(fecha_actual);
            mensajeFecha.setUserType(UserType.DATE);
            chatMessages.addLast(mensajeFecha);
        }

        chatMessages.addLast(message);

        if (listAdapter != null)
            listAdapter.notifyDataSetChanged();

        boolean individual = tipo == Conversacion.TIPO_INDIVIDUAL;
        int clase = 0;
        // Cambiar por app.enviaMensaje()

        if (importante) {
            if (individual)
                clase = Mensaje.INDIVIDUAL_IMPORTANTE;
            else
                clase = Mensaje.GRUPO_IMPORTANTE;
        } else {
            if (individual)
                clase = Mensaje.INDIVIDUAL_NORMAL;
            else
                clase = Mensaje.GRUPO_NORMAL;
        }

        controlador.enviaMensaje(clase, messageText, id, individual);

    }

    public void onReceivedMessage(final ChatMessage message) {
        Log.d(TAG, getClass() + ".onReceivedMessage(" + message + ")");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatMessage previous = chatMessages.peekLast();
                String fecha_prev = "";
                if (previous != null) {
                    fecha_prev = FORMATO_FECHA_COMPROBACION.format(new Date(previous.getFecha()));
                }
                String fecha_actual = FORMATO_FECHA_COMPROBACION.format(new Date(message.getFecha()));

                if (!fecha_actual.equals(fecha_prev)) {
                    ChatMessage mensajeFecha = new ChatMessage();
                    mensajeFecha.setContenido(fecha_actual);
                    mensajeFecha.setUserType(UserType.DATE);
                    chatMessages.addLast(mensajeFecha);
                }

                chatMessages.addLast(message);
                chatListView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Select the last fila_conversacion so it will scroll into view...
                        chatListView.setSelection(listAdapter.getCount() - 1);
                    }
                });
                if (listAdapter != null)
                    listAdapter.notifyDataSetChanged();
            }
        });
    }


    @Override
    public void onMensajeImportanteEnviadoCorrecto() {
        Log.d(TAG, getClass() + ".onMensajeEnviadoCorrecto()");
        Toast.makeText(getBaseContext(), "Enviado mensaje importante", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMensajeImportanteEnviadoFallido() {
        Log.d(TAG, getClass() + ".onMensajeEnviadoFallido()");
        Toast.makeText(getBaseContext(), "Error al enviar mensaje importante", Toast.LENGTH_SHORT).show();
    }

    private Activity getActivity() {
        return this;
    }


    /**
     * Show or hide the emoji popup
     *
     * @param show
     */
    private void showEmojiPopup(boolean show) {
        showingEmoji = show;

        if (show) {
            if (emojiView == null) {
                if (getActivity() == null) {
                    return;
                }
                emojiView = new EmojiView(getActivity());

                emojiView.setListener(new EmojiView.Listener() {
                    public void onBackspace() {
                        chatEditText1.dispatchKeyEvent(new KeyEvent(0, 67));
                    }

                    public void onEmojiSelected(String symbol) {
                        int i = chatEditText1.getSelectionEnd();
                        if (i < 0) {
                            i = 0;
                        }
                        try {
                            CharSequence localCharSequence = Emoji.replaceEmoji(symbol, chatEditText1.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20));
                            chatEditText1.setText(chatEditText1.getText().insert(i, localCharSequence));
                            int j = i + localCharSequence.length();
                            chatEditText1.setSelection(j, j);
                        } catch (Exception e) {
                            Log.e(Constants.TAG, "Error showing emoji");
                        }
                    }
                });


                windowLayoutParams = new WindowManager.LayoutParams();
                windowLayoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
                if (Build.VERSION.SDK_INT >= 21) {
                    windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
                } else {
                    windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
                    windowLayoutParams.token = getActivity().getWindow().getDecorView().getWindowToken();
                }
                windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }

            final int currentHeight;

            if (keyboardHeight <= 0)
                keyboardHeight = ApplicationExtended.getInstance().getSharedPreferences("emoji", 0).getInt("kbd_height", AndroidUtilities.dp(200));

            currentHeight = keyboardHeight;

            WindowManager wm = (WindowManager) ApplicationExtended.getInstance().getSystemService(Activity.WINDOW_SERVICE);

            windowLayoutParams.height = currentHeight;
            windowLayoutParams.width = AndroidUtilities.displaySize.x;

            try {
                if (emojiView.getParent() != null) {
                    wm.removeViewImmediate(emojiView);
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, e.getMessage());
            }

            try {
                wm.addView(emojiView, windowLayoutParams);
            } catch (Exception e) {
                Log.e(Constants.TAG, e.getMessage());
                return;
            }

            if (!keyboardVisible) {
                if (sizeNotifierRelativeLayout != null) {
                    sizeNotifierRelativeLayout.setPadding(0, 0, 0, currentHeight);
                }

                return;
            }

        } else {
            removeEmojiWindow();
            if (sizeNotifierRelativeLayout != null) {
                sizeNotifierRelativeLayout.post(new Runnable() {
                    public void run() {
                        if (sizeNotifierRelativeLayout != null) {
                            sizeNotifierRelativeLayout.setPadding(0, 0, 0, 0);
                        }
                    }
                });
            }
        }


    }


    /**
     * Remove emoji window
     */
    private void removeEmojiWindow() {
        if (emojiView == null) {
            return;
        }
        try {
            if (emojiView.getParent() != null) {
                WindowManager wm = (WindowManager) ApplicationExtended.getInstance().getSystemService(Context.WINDOW_SERVICE);
                wm.removeViewImmediate(emojiView);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.getMessage());
        }
    }


    /**
     * Hides the emoji popup
     */
    public void hideEmojiPopup() {
        if (showingEmoji) {
            showEmojiPopup(false);
        }
    }

    /**
     * Check if the emoji popup is showing
     *
     * @return
     */
    public boolean isEmojiPopupShowing() {
        return showingEmoji;
    }


    /**
     * Updates emoji views when they are complete loading
     *
     * @param id
     * @param args
     */
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.emojiDidLoaded) {
            if (emojiView != null) {
                emojiView.invalidateViews();
            }

            if (chatListView != null) {
                chatListView.invalidateViews();
            }
        }
    }

    @Override
    public void onSizeChanged(int height) {

        Rect localRect = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);

        WindowManager wm = (WindowManager) ApplicationExtended.getInstance().getSystemService(Activity.WINDOW_SERVICE);
        if (wm == null || wm.getDefaultDisplay() == null) {
            return;
        }


        if (height > AndroidUtilities.dp(50) && keyboardVisible) {
            keyboardHeight = height;
            ApplicationExtended.getInstance().getSharedPreferences("emoji", 0).edit().putInt("kbd_height", keyboardHeight).commit();
        }


        if (showingEmoji) {
            int newHeight = 0;

            newHeight = keyboardHeight;

            if (windowLayoutParams.width != AndroidUtilities.displaySize.x || windowLayoutParams.height != newHeight) {
                windowLayoutParams.width = AndroidUtilities.displaySize.x;
                windowLayoutParams.height = newHeight;

                wm.updateViewLayout(emojiView, windowLayoutParams);
                if (!keyboardVisible) {
                    sizeNotifierRelativeLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            if (sizeNotifierRelativeLayout != null) {
                                sizeNotifierRelativeLayout.setPadding(0, 0, 0, windowLayoutParams.height);
                                sizeNotifierRelativeLayout.requestLayout();
                            }
                        }
                    });
                }
            }
        }


        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;
        if (keyboardVisible && sizeNotifierRelativeLayout.getPaddingBottom() > 0) {
            showEmojiPopup(false);
        } else if (!keyboardVisible && keyboardVisible != oldValue && showingEmoji) {
            showEmojiPopup(false);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
    }

    /**
     * Get the system status bar height
     *
     * @return
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();

        hideEmojiPopup();
    }

    public String getIdConversacion() {
        Log.d(TAG,getClass()+".getIdConversacion()");
        return id;
    }
}
