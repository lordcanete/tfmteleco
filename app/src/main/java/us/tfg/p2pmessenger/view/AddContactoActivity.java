package us.tfg.p2pmessenger.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.dx.dxloadingbutton.lib.LoadingButton;

import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;
import us.tfg.p2pmessenger.model.Usuario;

import static us.tfg.p2pmessenger.view.SignupActivity.MIN_LENGTH_USERNAME;

/**
 * Actividad para anadir un nuevo contacto a la lista de contactos guardados.
 * Primero se ingresa el nombre de usuario, luego se comprueba. Una vez que
 * esta comprobado, se le asigna un alias al contacto y se guarda en la
 * base de datos
 */
public class AddContactoActivity extends ActivityBase {

    private TextView _textAlias;
    private TextView _textUsername;
    private LoadingButton _botonBuscar;
    private LoadingButton _botonAdd;

    private boolean comprobando;
    private boolean comprobado;

    private Usuario usuario;

    /**
     * Detectara cuando se cambia el texto de alguno de los campos de texto
     */
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!comprobando) {
                _botonBuscar.reset();
                comprobado = false;
            }

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacto);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Nuevo contacto");
            ab.setDisplayHomeAsUpEnabled(true);
        }

        _textUsername = (TextView) findViewById(R.id.input_username);

        _textUsername.addTextChangedListener(textWatcher);

        _textAlias = (TextView) findViewById(R.id.input_alias);

        _botonBuscar = (LoadingButton) findViewById(R.id.btn_buscar_id);
        _botonAdd = (LoadingButton) findViewById(R.id.btn_add_contact);

        _botonBuscar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_botonBuscar.onClick(" + v + ")");
                // si el nombre de usuario es valido, se inicia la comprobacion
                if (!comprobando) {
                    String name = _textUsername.getText().toString();
                    if (name.isEmpty() || name.length() < MIN_LENGTH_USERNAME) {
                        _textUsername.setError("Al menos 3 caracteres");
                    } else {
                        _textUsername.setError(null);
                        _botonBuscar.reset();
                        _botonBuscar.startLoading();
                        controlador.compruebaNombre(name);
                        comprobando = true;
                    }
                }
            }
        });

        _botonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_botonAdd.onClick(" + v + ")");
                // si se ha buscado lo anade, si no, lo busca
                if (!comprobando && !comprobado) {
                    _botonBuscar.reset();
                    _botonBuscar.startLoading();
                    comprobando = true;
                } else if (!comprobando) {
                    String name = usuario.getNombre();
                    String alias = _textAlias.getText().toString();

                    if (name.isEmpty() || name.length() < MIN_LENGTH_USERNAME) {
                        _textAlias.setError("Al menos " + MIN_LENGTH_USERNAME + " caracteres");
                    } else if (alias.isEmpty() || alias.length() < MIN_LENGTH_USERNAME) {
                        _textAlias.setError("Al menos " + MIN_LENGTH_USERNAME + " caracteres");
                    } else {
                        _textAlias.setError(null);
                        controlador.nuevoContacto(usuario, alias);
                        AddContactoActivity.super.onBackPressed();
                    }
                }
            }
        });
    }

    /**
     * Metodo de callback que avisa con el resultado de la busqueda
     * de un usuario en pastry
     * @param usuarioO
     * @param disponible
     */
    @Override
    protected void resultadoNombreUsuario(final Usuario usuarioO,final int disponible) {
        Log.d(TAG,getClass()+".resultadoNombreUsuario("+usuarioO+", disp = "+disponible+")");
        // para que se ejecute en el hilo principal y no lance
        // una excepcion
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,getClass()+".resultadoNombreUsuario("+usuarioO+", disp = "+disponible+")");

                if (disponible==0 && usuarioO != null) {
                    usuario = usuarioO;
                    comprobado = true;
                    _botonBuscar.loadingSuccessful();
                    _textUsername.setError(null);
                } else {
                    _botonBuscar.loadingFailed();
                    _textUsername.setError("No existe");
                }
                comprobando = false;
            }
        });
    }

}
