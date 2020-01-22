package us.tfg.p2pmessenger.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dx.dxloadingbutton.lib.LoadingButton;

import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;

import static us.tfg.p2pmessenger.controller.Controlador.MODO_REGISTRO;
import static us.tfg.p2pmessenger.view.SignupActivity.MIN_LENGTH_USERNAME;

/**
 * Actividad tomada de
 * @see <a href="https://sourcey.com/beautiful-android-login-and-signup-screens-with-material-design/">Material Login</a>
 * y editada a conveniencia. En esta actividad se muestra el dialogo
 * de inicio de sesion si no hay ningun usuario cuyas credenciales
 * esten guardadas. Si seleccionamos el enlace de abajo nos lleva a
 * {@link SignupActivity}. Antes de cargar la actividad, es necesario
 * que pastry se inicie en el modo de registro.
 *
 * Al rellenar los campos se validan localmente y si son correctos se
 * inicia la aplicacion pastry. Dependiendo de la red sera mas o menos
 * rapido el inicio. Cuando se completa el incio, se almacenan las
 * credenciales para no tener que pedirlas la siguiente vez que se
 * inicie. Cuando se inicia correctamente, se pasa a {@link ConversacionesActivity}
 *
 */
public class LoginActivity extends ActivityBase {
    private static final int REQUEST_SIGNUP = 0;

    private EditText _userText;
    private EditText _passwordText;
    private LoadingButton _loginButton;
    private TextView _signupLink;
    private ProgressDialog dialog;
    private boolean iniciandoRegistro;
    private boolean iniciandoSesion;

    /**
     * Notificacion si alguno de los campos cambia
     */
    private TextWatcher passTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            _loginButton.reset();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    /**
     * Para dejar de recibir notificaciones cuando cambie un campo
     */
    private TextWatcher voidTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("p2pmessenger", getClass() + ".onCreate(...)");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        _userText = (EditText) this.findViewById(R.id.input_user);
        _passwordText = (EditText) this.findViewById(R.id.input_password);

        _loginButton = (LoadingButton) this.findViewById(R.id.btn_login);
        _signupLink = (TextView) this.findViewById(R.id.link_signup);

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_loginButton.onClick(" + v + ")");
                String name = _userText.getText().toString();
                if (name.isEmpty() || name.length() < MIN_LENGTH_USERNAME) {
                    _userText.setError("Al menos 3 caracteres");
                } else {
                    _loginButton.startLoading();
                    login();
                }
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                dialog = ProgressDialog.show(LoginActivity.this, "Cargando", "Iniciando registro", true);
                controlador.setModo(MODO_REGISTRO);
                iniciandoRegistro = true;

            }
        });
    }

    /**
     * empieza el inicio de sesion. Primero valida la entrada y
     * luego avisa al controlador si la entrada es valida
     */
    public void login() {
        //
        Log.d(TAG, getClass() + ".Login");

        iniciandoSesion = true;

        _userText.addTextChangedListener(voidTextWatcher);
        _passwordText.addTextChangedListener(voidTextWatcher);

        if (!validate()) {
            onLoginFailed();
            return;
        }

        Log.d(TAG, "showed dialog");

        String user = _userText.getText().toString();
        String password = _passwordText.getText().toString();

        controlador.iniciaSesion(user, password);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, getClass() + ".onBackPressed()");
        // Disable going back to the ConversacionActivity
        moveTaskToBack(true);
    }

    /**
     * Se ha iniciado correctamente la sesion
     */
    public void onLoginSuccess() {
        Log.d(TAG, getClass() + ".onLoginSuccess()");
        iniciandoSesion = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setResult(RESULT_OK, null);
                _loginButton.setEnabled(true);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                _loginButton.loadingSuccessful();
                finish();
            }
        });
    }

    /**
     * Error al iniciar la sesion
     */
    public void onLoginFailed() {
        Log.d(TAG, getClass() + ".onLoginFailed()");
        if (iniciandoSesion) {
            iniciandoSesion = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _userText.addTextChangedListener(passTextWatcher);
                    _passwordText.addTextChangedListener(passTextWatcher);
                    _loginButton.setEnabled(true);
                    _loginButton.loadingFailed();
                    Toast.makeText(getBaseContext(),
                            getResources().getString(R.string.msj_login_failed)
                            ,Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Cuando se ha solicitado el registro, se recibe esta llamada para
     * descartar el cuadro de dialogo de espera (para no tener la
     * pantalla en negro) y finalizar la actividad.
     */
    public void onServiceLoaded() {
        Log.d(TAG, getClass() + ".onServiceLoaded()");
        if (iniciandoRegistro) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
                    if (dialog != null)
                        dialog.dismiss();
                }
            });
        }
    }

    /**
     * Valida la entrada
     * @return si es correcta o no
     */
    public boolean validate() {
        Log.d(TAG, getClass() + ".validate()");
        boolean valid = true;

        String email = _userText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty()) {
            _userText.setError(getResources().getString(R.string.nombre_usr_vacio));
            valid = false;
        } else {
            _userText.setError(null);
        }

        if (password.isEmpty()) {
            _passwordText.setError(getResources().getString(R.string.contrasena_vacia));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }
}
