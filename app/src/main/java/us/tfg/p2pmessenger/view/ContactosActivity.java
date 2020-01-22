package us.tfg.p2pmessenger.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
import android.widget.Toast;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import in.co.madhur.chatbubblesdemo.ConversacionActivity;
import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.view.adapters.ContactosAdapter;

import static us.tfg.p2pmessenger.view.adapters.ContactosAdapter.FILA_GRUPO;

/**
 * Lista de todos los contactos. Al leerse de la base de datos
 * se inserta en la primera posicion un contacto especial
 * que tendra la funcion de llevarno a la actividad de crear
 * un grupo nuevo. En la barra de accion hay un boton para
 * crear un nuevo contacto. Se llega aqui al pulsar el boton
 * flotante de la {@link ConversacionesActivity}
 */
public class ContactosActivity extends ActivityBase {

    private ContactosAdapter adapter;
    private ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    protected void onStart() {
        super.onStart();
        final ArrayList<Contacto> contactos=new ArrayList<>();
        Contacto crea_grupo = new Contacto(FILA_GRUPO,null);
        contactos.add(crea_grupo);
        // obtiene los contactos de la base de datos
        contactos.addAll(controlador.obtenerContactos());
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if(ab!=null) {
            ab.setTitle("Elegir contacto");
            if((contactos.size()-1)==1)
                ab.setSubtitle((contactos.size()-1)+" contacto");
            else
                ab.setSubtitle((contactos.size()-1)+" contactos");
            ab.setDisplayHomeAsUpEnabled(true);
        }
        listView = (ListView) findViewById(R.id.lista_contactos);

        adapter = new ContactosAdapter(getBaseContext(), R.layout.fila_contacto,contactos);
        adapter.setTipoVista(ContactosAdapter.CONTACTOS_ACTIVITY);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                Log.d(TAG,ContactosActivity.this.getClass()+".listView.setOnItemClickListener()");
                // al seleccionar un contacto nos crea una nueva conversacion
                TextView _aliasText= (TextView) view.findViewById(R.id.contacto_alias);
                Intent intent;

                if(_aliasText.getText().toString().equals(
                        getResources().getText(R.string.texto_nuevo_grupo)))
                {
                    // crear un nuevo grupo
                    intent=new Intent(ContactosActivity.this,NuevoGrupoActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    // crear una nueva conversacion
                    intent = new Intent(ContactosActivity.this, ConversacionActivity.class);
                    TextView _idText = (TextView) view.findViewById(R.id.contacto_id_completo);

                    if(controlador.iniciarConversacion(_idText.getText().toString())) {
                        String contactoJson = "";
                        StringWriter strWriter = new StringWriter();
                        JsonWriter writer = new JsonWriter(strWriter);
                        try {
                            writer.beginObject()
                                    .name("id").value(_idText.getText().toString())
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
                    } else
                    {
                        Toast.makeText(ContactosActivity.this,"Error al iniciar conversacion",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        registerForContextMenu(listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG,getClass()+".onCreateOptionsMenu("+menu+")");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_elegir_contactos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent=null;
        switch (item.getItemId()) {
            case R.id.new_contact:
                Log.d(TAG,"se ha pulsado la opcion de añadir contacto");
                intent=new Intent(this,AddContactoActivity.class);
                break;
            default:
                Log.d(TAG,"opcion desconocida");
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        if(intent!=null)
            startActivity(intent);
        return true;
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_popup_onlongclick_contactos, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContactosAdapter.ContactoEliminable tag = null;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        View view = info.targetView;

        switch (item.getItemId()) {
            // elimina un contacto
            case R.id.eliminar:
                tag = (ContactosAdapter.ContactoEliminable) view.getTag();
                controlador.eliminarConversacion(tag.getIdContacto());

                if (adapter != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });

                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}
