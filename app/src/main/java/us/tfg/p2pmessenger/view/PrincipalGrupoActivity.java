package us.tfg.p2pmessenger.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.view.adapters.ContactosAdapter;


/**
 * Clase que nos muestra la pagina principal de un grupo,
 * el id que tiene, su titulo y los participantes, asi
 * como quien es el lider del grupo. La vista de los
 * contactos es la misma que en la actividad para elegir
 * interlocutor de nueva conversacion
 */
public class PrincipalGrupoActivity extends ActivityBase {

    /**
     * Para abrir los mensajes importantes del grupo
     */
    private FloatingActionButton fab;
    private String idGrupo;
    private Grupo grupo;
    private ListView listView;

    /**
     * Para invitar a alguien al grupo
     */
    private Button botonInvitarUsuario;
    private boolean boton_cargado;

    /**
     * Mantiene la vista de la lista
     */
    private ContactosAdapter adapter;

    private ArrayList<Contacto> contactos;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_grupo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PrincipalGrupoActivity.this, MensajesImportantesActivity.class);
                intent.putExtra("idConversacion",idGrupo);
                startActivity(intent);
            }
        });

        boton_cargado=false;
        botonInvitarUsuario = new Button(this);
        botonInvitarUsuario.setText("Invitar a usuario");

        botonInvitarUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Starting a new async task
                Intent intent = new Intent(PrincipalGrupoActivity.this, MostrarCodigoQRActivity.class);
                String codigo = controlador.obtenerCodigoInvitacion(getIdConversacion());
                String idUsuario = controlador.getMyId().toStringFull();
                intent.putExtra("texto", idUsuario+"-"+codigo);
                startActivity(intent);

            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();

        if (controlador != null && controlador.isIniciado() && extras != null) {

            idGrupo = extras.getString("idConversacion");

            if (!"".equals(idGrupo)) {
                grupo = controlador.obtenerGrupo(idGrupo);
                controlador.actualizaGrupo(idGrupo);

                if (grupo != null) {

                    contactos = controlador.obtenerContactosDeGrupo(grupo);
                    if(contactos==null)
                        contactos = new ArrayList<>();

                    Log.d(TAG,"contactos = "+contactos);

                    android.support.v7.app.ActionBar ab = getSupportActionBar();
                    if (ab != null) {
                        ab.setTitle(grupo.getNombre());
                        ab.setSubtitle((contactos.size()-1)+" contactos blabla");
                        ab.setDisplayHomeAsUpEnabled(true);
                    }
                    listView = (ListView) findViewById(R.id.lista_participantes_grupo);

                    adapter = new ContactosAdapter(getBaseContext(), R.layout.fila_contacto, contactos);
                    adapter.setLider(grupo.getLider());
                    adapter.setTipoVista(ContactosAdapter.PRINCIPAL_GRUPO_ACTIVITY);
                    listView.setAdapter(adapter);

                    if(!boton_cargado) {
                        listView.addHeaderView(botonInvitarUsuario);
                        boton_cargado=true;
                    }

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position,
                                                long id) {

                            TextView _aliasText = (TextView) view.findViewById(R.id.contacto_alias);
                            Intent intent;

                            intent = new Intent(PrincipalGrupoActivity.this, ConversacionActivity.class);
                            TextView _idText = (TextView) view.findViewById(R.id.contacto_id);

                            if (controlador.iniciarConversacion(_idText.getText().toString())) {
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
                            } else {
                                Toast.makeText(PrincipalGrupoActivity.this, "Error al iniciar conversacion", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }
    }

    public String getIdConversacion()
    {
        return this.idGrupo;
    }

    public void onGrupoActualizado(final Grupo grupoActualizado)
    {
        Log.d(TAG,getClass()+".onGrupoActualizado("+grupoActualizado+")");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Contacto> contactosActualizados = controlador.
                        obtenerContactosDeGrupo(grupoActualizado);

                adapter.setLider(grupo.getLider());

                for(Contacto c :contactosActualizados)
                {
                    int i = contactos.indexOf(c);
                    if(i!=-1)
                        contactos.add(c);
                }

                //adapter = new ContactosAdapter(PrincipalGrupoActivity.this,)

                if(adapter!=null)
                    adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

            }
        });
    }

    /**
     * Menu contextual
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, getClass() + ".onCreateOptionsMenu(" + menu + ")");
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                Log.d(TAG, getClass()+" - volver");
                break;
            default:
                Log.d(TAG, "opcion desconocida");
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        return true;
    }
}
