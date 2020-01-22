package us.tfg.p2pmessenger.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Iterator;

import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.view.adapters.MensajesImportantesAdapter;

/**
 * Al seleccionar los mensajes importantes, se abre esta
 * actividad. Se va cargando cada bloque partiendo del
 * que esta en cache o el primero que sale en pastry.
 * Cuando se carga un bloque, nos ofrece cargar el siguiente
 * y el anterior mediante unos botones. Cuando no haya
 * mas bloques, el boton se eliminara.
 */
public class MensajesImportantesActivity extends ActivityBase {

    private int cantidadActual;
    private ArrayDeque<ChatMessage> chatMessages;
    private String idConversacion;
    private String aliasConversacion;
    private ListView listView;
    private MensajesImportantesAdapter adapter;
    private int preLast;

    /**
     * id del bloque anterior
     */
    private String idAnterior;
    private boolean cargandoInferiores;
    private boolean cargandoSuperiores;

    /**
     * Id del siguiente bloque
     */
    private String idSiguiente;
    private ProgressBar progressBar;

    private Button botonCargarAnterior;
    private Button botonCargarSiguiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        cantidadActual = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensajes_importantes);
        chatMessages = new ArrayDeque<>();
        aliasConversacion = "";
        idAnterior = null;
        idSiguiente = null;
        cargandoInferiores = false;
        cargandoSuperiores = false;

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);


        botonCargarSiguiente = new Button(this);
        botonCargarSiguiente.setText("Cargar posteriores");

        botonCargarSiguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Starting a new async task
                if (!cargandoSuperiores && idSiguiente != null) {
                    controlador.obtenerMensajesImportantes(idSiguiente, true);
                    cargandoSuperiores = true;
                    if (!cargandoInferiores)
                        progressBar.setVisibility(View.VISIBLE);
                }
            }
        });
        botonCargarAnterior = new Button(this);
        botonCargarAnterior.setText("Cargar anteriores");

        botonCargarAnterior.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Starting a new async task
                if (!cargandoInferiores && idAnterior != null) {
                    controlador.obtenerMensajesImportantes(idAnterior, true);
                    cargandoInferiores = true;
                    if (!cargandoSuperiores)
                        progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    @Override
    protected void onStart() {
        Log.d(TAG, getClass() + ".onStart()");
        super.onStart();
        if (getIntent().hasExtra("idConversacion"))
            idConversacion = getIntent().getExtras().getString("idConversacion");
        else
            idConversacion = "";

        aliasConversacion = controlador.obtenerMensajesImportantes(idConversacion, false);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Mensajes importantes");
            ab.setDisplayHomeAsUpEnabled(true);
        }

        listView = (ListView) findViewById(R.id.lista_mensajes_importantes);

        adapter = new MensajesImportantesAdapter(getBaseContext(),
                R.layout.fila_mensaje_importante, chatMessages, aliasConversacion, idConversacion);

        idSiguiente = null;
        cargandoSuperiores = true;
        idAnterior = null;
        cargandoInferiores = true;

        listView.setAdapter(adapter);


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int pos, long id) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                MensajesImportantesAdapter.MensajeImportanteCopiable tag =
                        (MensajesImportantesAdapter.MensajeImportanteCopiable) view.getTag();

                ClipData clip = ClipData.newPlainText("", tag.getContenido());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getBaseContext(), R.string.texto_mensaje_copiado, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

/*
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                TextView _aliasText = (TextView) view.findViewById(R.id.contacto_alias);
                Intent intent;

                if (_aliasText.getText().toString().equals(
                        getResources().getText(R.string.texto_nuevo_grupo))) {
                    intent = new Intent(MensajesImportantesActivity.this, NuevoGrupoActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    intent = new Intent(MensajesImportantesActivity.this, ConversacionActivity.class);
                    TextView _idText = (TextView) view.findViewById(R.id.contacto_id);

                    if (controlador.iniciarConversacion(_idText.getText().toString())) {
                        String contactoJson = "";
                        StringWriter strWriter = new StringWriter();
                        JsonWriter writer = new JsonWriter(strWriter);
                        try {
                            writer.beginObject()
                                    .name("idConversacion").value(_idText.getText().toString())
                                    .name("alias").value(_aliasText.getText().toString())
                                    .name("tipo").value(String.valueOf(Conversacion.TIPO_INDIVIDUAL))
                                    .endObject().close();
                            contactoJson = strWriter.toString();
                        } catch (IOException e) {
                            Log.d(TAG, "Error al serializar el contacto en formato JSON", e);
                        }

                        intent.putExtra("conversacion", contactoJson);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MensajesImportantesActivity.this,
                                "Error al iniciar conversacion", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        */
    }

    /**
     * Cuando se ha cargado un bloque de mensajes se llama a este metodo
     * desde el controlador, asincronamente
     *
     * @param mensajesLeidos   Lista de los mensajes leidos
     * @param idAnteriorLeido  id del anterior bloque al leido
     * @param idActualLeido    id bloque leido
     * @param idSiguienteLeido id del bloque siguiente al leido
     */
    @Override
    public void resultadoMensajesImportantes(final ArrayDeque<ChatMessage> mensajesLeidos
            , final String idAnteriorLeido, final String idActualLeido, final String idSiguienteLeido) {
        Log.d(TAG, getClass() + ".resultadoMensajesImportantes(<mensajes>, " + idAnteriorLeido + " <- " +
                idActualLeido + " -> " + idSiguienteLeido);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //si no hay ningun mensaje, para la carga en ambos sentidos,
                // muestra los mensajes y actualiza los punteros
                if (idAnteriorLeido == null && idSiguienteLeido == null && chatMessages.size() == 0) {
                    idAnterior = idAnteriorLeido;
                    idSiguiente = idSiguienteLeido;

                    cargandoInferiores = false;
                    cargandoSuperiores = false;
                    chatMessages.addAll(mensajesLeidos);
                    progressBar.setVisibility(View.GONE);
                    listView.addHeaderView(botonCargarSiguiente);
                    listView.addFooterView(botonCargarAnterior);
                } else {
                    if (idActualLeido.equals(idAnterior)) {
                        for (ChatMessage mensaje : mensajesLeidos) {
                            chatMessages.addLast(mensaje);
                        }
                        idAnterior = idAnteriorLeido;
                        cargandoInferiores = false;
                    } else if (idActualLeido.equals(idSiguiente)) {
                        Iterator<ChatMessage> it = mensajesLeidos.descendingIterator();
                        while (it.hasNext()) {
                            chatMessages.addFirst(it.next());
                        }
                        idSiguiente = idSiguienteLeido;
                        cargandoSuperiores = false;
                    }
                }

                // ocultar marca de cargando
                if (!cargandoInferiores && !cargandoSuperiores)
                    progressBar.setVisibility(View.GONE);

                if (idAnterior == null)
                    listView.removeFooterView(botonCargarAnterior);

                if (idSiguiente == null)
                    listView.removeHeaderView(botonCargarSiguiente);


                if (adapter != null)
                    adapter.notifyDataSetChanged();

                //actualizar subtitulo con el numero de mensajes
                android.support.v7.app.ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    ab.setSubtitle(chatMessages.size() + " mensajes");
                    ab.setDisplayHomeAsUpEnabled(true);
                }

            }
        });
    }

    /**
     * Cuando se desea enviar un nuevo mensaje importante
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, getClass() + ".onCreateOptionsMenu(" + menu + ")");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_msj_importantes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nuevo_msj_importante:
                Log.d(TAG, getClass() + "se ha pulsado la opcion de " +
                        "enviar un mensaje importante");
                if (!"".equals(idConversacion)) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    final EditText edittext = new EditText(this);
                    alert.setTitle("Escriba mensaje");
                    alert.setCancelable(true);

                    alert.setView(edittext);

                    alert.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //What ever you want to do with the value
                            //OR
                            String texto = edittext.getText().toString();
                            if (!"".equals(idConversacion))
                                controlador.enviaMensaje(Mensaje.GRUPO_IMPORTANTE,
                                        texto, idConversacion, false);
                        }
                    });

                    alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            // what ever you want to do with No option.
                        }
                    });

                    alert.show();
                }
                break;
            // elimina todos los mensajes y los carga de nuevo.
            case R.id.actualizar_msj_importantes:
                Log.d(TAG, getClass() + " - se hapulsado la opcion de actualizar " +
                        "los mensajes importantes");

                aliasConversacion = controlador.obtenerMensajesImportantes(idConversacion, false);
                android.support.v7.app.ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    ab.setTitle("Mensajes importantes");
                    ab.setSubtitle(null);
                    ab.setDisplayHomeAsUpEnabled(true);
                }

                listView = (ListView) findViewById(R.id.lista_mensajes_importantes);

                chatMessages = new ArrayDeque<>();

                adapter = new MensajesImportantesAdapter(getBaseContext(),
                        R.layout.fila_mensaje_importante, chatMessages, aliasConversacion, idConversacion);

                idSiguiente = null;
                cargandoSuperiores = true;
                idAnterior = null;
                cargandoInferiores = true;

                listView.setAdapter(adapter);
                listView.removeHeaderView(botonCargarSiguiente);
                listView.removeFooterView(botonCargarAnterior);

                progressBar.setVisibility(View.VISIBLE);


                break;
            case android.R.id.home:
                super.onBackPressed();
                Log.d(TAG, "volver");
                break;
            default:
                Log.d(TAG, "opcion desconocida");
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        return true;
    }

    /**
     * Si se envio el mensaje importante correctamente
     */
    @Override
    public void onMensajeImportanteEnviadoCorrecto() {
        Log.d(TAG, getClass() + ".onMensajeEnviadoCorrecto()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), "Enviado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * No se envio el mensaje o hubo un error
     */
    @Override
    public void onMensajeImportanteEnviadoFallido() {
        Log.d(TAG, getClass() + ".onMensajeEnviadoFallido()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), "Error al enviar", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
