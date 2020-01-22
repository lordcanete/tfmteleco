package us.tfg.p2pmessenger.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.JsonWriter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import in.co.madhur.chatbubblesdemo.ConversacionActivity;
import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.ApplicationExtended;
import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.view.adapters.ConversacionesAdapter;

/**
 * Aqui se encuentra la lista de todas las conversaciones abiertas
 * por el usuario. Tanto grupos como individuales.
 */

public class ConversacionesActivity extends ActivityBase {

    private FloatingActionButton fab_nuevo_chat;
    private ListView listView;
    private ConversacionesAdapter adapter;
    private List<Conversacion> conversaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass() + ".onCreateEntorno(...)");
        ApplicationExtended.getInstance().setCurrentActivity(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversaciones);

        fab_nuevo_chat = (FloatingActionButton) findViewById(R.id.fab_nuevo_chat);
        fab_nuevo_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ConversacionesActivity.this, ContactosActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart() {
        Log.d(TAG, getClass() + ".onStart()");
        super.onStart();
        conversaciones = controlador.obtenerConversacionesAbiertas();
        listView = (ListView) findViewById(R.id.lista_conversaciones);
        ConversacionesAdapter adapter = new ConversacionesAdapter(getBaseContext(),
                R.layout.fila_conversacion, conversaciones);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Intent intent = new Intent(ConversacionesActivity.this, ConversacionActivity.class);
                TextView _idText = (TextView) view.findViewById(R.id.id_conversacion_completo);
                TextView _aliasText = (TextView) view.findViewById(R.id.nombre_conversacion_completo);
                TextView _tipoText = (TextView) view.findViewById(R.id.tipo_conversacion);
                String contactoJson = "";
                StringWriter strWriter = new StringWriter();
                JsonWriter writer = new JsonWriter(strWriter);
                try {
                    writer.beginObject()
                            .name("id").value(_idText.getText().toString())
                            .name("alias").value(_aliasText.getText().toString())
                            .name("tipo").value(_tipoText.getText().toString())
                            .endObject().close();
                    contactoJson = strWriter.toString();
                } catch (IOException e) {
                    Log.d(TAG, "Error al serializar el contacto en formato JSON", e);
                }

                intent.putExtra("conversacion", contactoJson);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        conversaciones = controlador.obtenerConversacionesAbiertas();
        listView = (ListView) findViewById(R.id.lista_conversaciones);
        adapter = new ConversacionesAdapter(getBaseContext(), R.layout.fila_conversacion, conversaciones);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, getClass() + ".onCreateOptionsMenu(" + menu + ")");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, getClass() + ".onOptionsItemSelected(" + item + ")");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = null;

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.invitar_a_pastry:
                intent = new Intent(this, MostrarCodigoQRActivity.class);
                if (controlador != null && controlador.isIniciado()) {
                    intent.putExtra("texto",controlador.obtenerIpPuerto());
                } else {
                    intent = null;
                }
                break;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.unirse_a_grupo:
                intent = new Intent(this, UnirseAGrupoActivity.class);
                break;
            case R.id.action_nuevo_grupo:
                intent = new Intent(this, NuevoGrupoActivity.class);
                break;
            case R.id.action_msj_importantes:
                intent = new Intent(this, MensajesImportantesActivity.class);
                break;
        }

        if (intent != null)
            startActivity(intent);

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_popup_onlongclick_conversaciones, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ConversacionesAdapter.ConversacionEliminable tag = null;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        View view = info.targetView;

        switch (item.getItemId()) {
            case R.id.eliminar:
                tag = (ConversacionesAdapter.ConversacionEliminable) view.getTag();
                controlador.eliminarConversacion(tag.getIdConversacion());
                Log.d(TAG,"posicion = "+ tag.getPosicion());
                if(tag.getPosicion()<conversaciones.size())
                {
                    conversaciones.remove(tag.getPosicion());
                    Log.d(TAG,"eliminando "+tag.getPosicion());
                }

                if (adapter != null)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
                else
                    Log.d(TAG,"el adaptador = null");
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}