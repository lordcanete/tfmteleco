package us.tfg.p2pmessenger.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dx.dxloadingbutton.lib.LoadingButton;

import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;

import static us.tfg.p2pmessenger.view.SignupActivity.MIN_LENGTH_USERNAME;

/**
 * Actividad cuando se crea un nuevo gurpo. Solicita
 * un nombre para el grupo. Cuando se aprieta el boton
 * crear, se inicia el proceso de creacion y si algo
 * falla se devuelve un error. En caso contrario
 * se llama asincronamente al metodo que gestiona
 * el resultado correcto
 */
public class NuevoGrupoActivity extends ActivityBase {

    private TextView _textNombreGrupo;
    private LoadingButton _botonCrear;
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            _botonCrear.reset();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_grupo);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if(ab!=null) {
            ab.setTitle("Nuevo grupo");
            ab.setDisplayHomeAsUpEnabled(true);
        }

        _textNombreGrupo = (TextView) findViewById(R.id.input_nombre_grupo);
        _textNombreGrupo.addTextChangedListener(textWatcher);

        _botonCrear = (LoadingButton) findViewById(R.id.btn_crear_grupo);

        // boton para iniciar el proceso de creacion
        _botonCrear.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass()+"_botonAdd.onClick(" + v + ")");
                String name = _textNombreGrupo.getText().toString();

                if (name.isEmpty() || name.length() < MIN_LENGTH_USERNAME) {
                    _textNombreGrupo.setError("Al menos "+MIN_LENGTH_USERNAME+" caracteres");
                } else {
                    _textNombreGrupo.setError(null);
                    _botonCrear.startLoading();
                    controlador.crearGrupo(name);
                }
            }
        });
    }


    /**
     * Llamada asincrona que ocurre cuando se crea correctamente
     */
    @Override
    public void onCreateGroupSuccess() {
        Log.d(TAG,getClass()+".onCreateGroupSuccess()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _botonCrear.loadingSuccessful();
                NuevoGrupoActivity.super.onBackPressed();
            }
        });
    }

    /**
     * Llamada asincrona que ocurre cuando se produce un error
     */
    @Override
    public void onCreateGroupFailed() {
        Log.d(TAG,getClass()+".onCreateGroupFailed()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _botonCrear.loadingFailed();
                Toast.makeText(getBaseContext(), "Error al crear el grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, getClass() + ".onCreateOptionsMenu(" + menu + ")");
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    /**
     * Menu de opciones
     * @param item
     * @return
     */
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
