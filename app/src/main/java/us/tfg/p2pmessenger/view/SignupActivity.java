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
import us.tfg.p2pmessenger.model.Usuario;

import static us.tfg.p2pmessenger.controller.Controlador.MODO_INICIO_SESION;


/**
 * Actividad tomada de
 * @see <a href="https://sourcey.com/beautiful-android-login-and-signup-screens-with-material-design/">Material Login</a>
 * y editada a conveniencia. En esta actividad se muestra el dialogo
 * de registro. Se llega aqui desde {@link LoginActivity}
 * Si seleccionamos el enlace de abajo nos lleva a
 * {@link LoginActivity}. Antes de cargar la actividad, es necesario
 * que pastry termine el modo de regisro.
 *
 * Al rellenar los campos se validan localmente y si son correctos se
 * inicia la aplicacion pastry. Dependiendo de la red sera mas o menos
 * rapido el inicio. Cuando se completa el incio, se almacenan las
 * credenciales para no tener que pedirlas la siguiente vez que se
 * inicie. Cuando se inicia correctamente, se pasa a {@link ConversacionesActivity},
 * en caso contrario no cambia de actividad.
 *
 * Para registrar a un usuario, este no debe estar en uso, de lo
 * contrario el formulario no se validara. Cuando se valida correctamente
 * se crea la informacion necesaria para que el usuario se una a la red
 *
 */
public class SignupActivity extends ActivityBase {
    public static final int MIN_LENGTH_USERNAME = 3;
    private static final int MIN_LENGTH_PASSWD = 6;

    /**
     * Nombre de usuario
     */
    private EditText _usernameText;

    /**
     * Boton para comprobar el nombre
     */
    private LoadingButton _checkUsername;

    /**
     * Contrasena
     */
    private EditText _passwordText;

    /**
     * Confirmar contrasena
     */
    private EditText _reEnterPasswordText;

    /**
     * Boton registrar
     */
    private LoadingButton _signupButton;

    /**
     * Enlace para cerrar la ventana de registro
     */
    private TextView _loginLink;

    private boolean comprobando;
    private boolean comprobado;
    private boolean validado;
    private boolean nombreDisponible;
    private ProgressDialog dialog;

    /**
     * Notifica si un campo cambia
     */
    private TextWatcher nombreTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            comprobado=false;
            if(!comprobando&&!validado) {
                _checkUsername.cancelLoading();
                _checkUsername.reset();
                _signupButton.cancelLoading();
                _signupButton.reset();
            }
        }
    };
    /**
     * Notifica si un campo cambia
     */
    private TextWatcher passTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            validado=false;
            _signupButton.cancelLoading();
            _signupButton.reset();
        }
    };

    /**
     * cancelar la notificacion
     */
    private TextWatcher nullTextWatcher = new TextWatcher() {
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
        Log.d(TAG, getClass()+"onCreateEntorno(...)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        _usernameText = (EditText) this.findViewById(R.id.input_username);
        _checkUsername = (LoadingButton) this.findViewById(R.id.comprobar_username);
        _passwordText = (EditText) this.findViewById(R.id.input_password);
        _reEnterPasswordText = (EditText) this.findViewById(R.id.input_reEnterPassword);
        _signupButton = (LoadingButton) this.findViewById(R.id.btn_signup);
        _loginLink = (TextView) this.findViewById(R.id.link_login);

        comprobando = false;
        comprobado = false;

        _usernameText.addTextChangedListener(nombreTextWatcher);

        _checkUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass()+"_checkUsername.onClick(" + v + ")");
                String name = _usernameText.getText().toString();
                comprobado=false;
                comprobando=false;
                if (name.isEmpty() || name.length() < MIN_LENGTH_USERNAME) {
                    _usernameText.setError("Al menos 3 caracteres");
                } else {
                    _checkUsername.startLoading();
                    controlador.compruebaNombre(_usernameText.getText().toString());
                    comprobando = true;
                }
            }
        });

        _passwordText.addTextChangedListener(passTextWatcher);
        _reEnterPasswordText.addTextChangedListener(passTextWatcher);

        _signupButton.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass()+"_signupButton.onClick(" + v + ")");
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass()+"_loginLink.onClick(" + v + ")");
                //controlador.stop();
                // Finish the registration screen and return to the Login activity
                controlador.setModo(MODO_INICIO_SESION);
                dialog = ProgressDialog.show(SignupActivity.this,"Cargando","Cerrando registro",true);
            }
        });


    }

    /**
     * Llamado cuando se comprueba un nombre de usuario
     * @param usuario
     * @param disponibleByte
     */
    @Override
    protected void resultadoNombreUsuario(Usuario usuario, int disponibleByte) {
        Log.d(TAG, getClass()+".resultadoNombreUsuario(" + usuario +
                " , disponible = " + (disponibleByte!=0) + ")");
        comprobando = false;
        if (disponibleByte != 0) {
            _usernameText.post(new Runnable() {
                @Override
                public void run() {
                    _usernameText.setError(null);
                }
            });
            _checkUsername.post(new Runnable() {
                @Override
                public void run() {
                    _checkUsername.loadingSuccessful();
                }
            });
            comprobado = true;
            if(validado)
            {
                _signupButton.post(new Runnable() {
                    @Override
                    public void run() {
                        _signupButton.performClick();
                    }
                });
            }
            this.nombreDisponible = true;
        } else {
            _usernameText.post(new Runnable() {
                @Override
                public void run() {
                    _usernameText.setError("Nombre en uso");
                }
            });
            _checkUsername.post(new Runnable() {
                @Override
                public void run() {
                    _checkUsername.cancelLoading();
                }
            });
            this.nombreDisponible = false;
        }


    }

    /**
     * Cuando se pulsa la tecla back
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, getClass()+".onBackPressed()");
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
    }

    /**
     * Metodo que valida la entrada y lanza el registro si los
     * campos son correctos
     */
    public void signup() {
        Log.d(TAG, getClass()+".Signup");

        _signupButton.startLoading();

        if (!validate()) {
            onSignupFailed();
            _signupButton.loadingFailed();
            return;
        }


        String name = _usernameText.getText().toString();
        String password = _passwordText.getText().toString();

        /*new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onSignupSuccess or onSignupFailed
                        // depending on success
                        onSignupSuccess();
                        // onSignupFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);*/

        _reEnterPasswordText.addTextChangedListener(nullTextWatcher);
        _passwordText.addTextChangedListener(nullTextWatcher);
        _usernameText.addTextChangedListener(nullTextWatcher);
        controlador.registrarUsuario(name, password);

    }

    /**
     * Resultado asincrono del registro
     */
    @Override
    public void onSignupSuccess() {
        Log.d(TAG, getClass()+".onSignupSuccess()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _reEnterPasswordText.addTextChangedListener(passTextWatcher);
                _passwordText.addTextChangedListener(passTextWatcher);
                _usernameText.addTextChangedListener(nombreTextWatcher);

                _signupButton.setEnabled(true);
                setResult(RESULT_OK, null);
                _signupButton.loadingSuccessful();
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * Resultado asincrono del registro
     */
    @Override
    public void onSignupFailed() {
        Log.d(TAG, getClass()+".onSignupFailed()");
        if(comprobado&&validado) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    _reEnterPasswordText.addTextChangedListener(passTextWatcher);
                    _passwordText.addTextChangedListener(passTextWatcher);
                    _usernameText.addTextChangedListener(nombreTextWatcher);
                    Toast.makeText(getBaseContext(), "Registro fallido", Toast.LENGTH_LONG).show();
                    _signupButton.loadingFailed();
                }
            });
        }
        else
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    _reEnterPasswordText.addTextChangedListener(passTextWatcher);
                    _passwordText.addTextChangedListener(passTextWatcher);
                    _usernameText.addTextChangedListener(nombreTextWatcher);
                    Toast.makeText(getBaseContext(), "Comprobando el usuario", Toast.LENGTH_SHORT).show();
                }
            });

    }

    /**
     * Si se pulsa la tecla hacia atras, se debe parar el
     * registro y para que el controlador avise a la vista,
     * se utiliza este metodo
     */
    public void onServiceLoaded()
    {
        Log.d(TAG,getClass()+".onServiceLoaded()");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
        dialog.dismiss();
    }

    /**
     * Valida la entrada e inicia el proceso de registro si
     * es correcta
     * @return
     */
    public boolean validate() {
        Log.d(TAG, getClass()+".validate()");
        boolean valid = true;

        String name = _usernameText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.isEmpty() || name.length() < MIN_LENGTH_USERNAME) {
            _usernameText.setError("Al menos 3 caracteres");
            valid = false;
        } else {
            _usernameText.setError(null);
        }

        if (password.isEmpty() || password.length() < MIN_LENGTH_PASSWD) {
            _passwordText.setError("Al menos 6 catacteres");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < MIN_LENGTH_PASSWD || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError("Las contraseñas no coinciden");
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }

        validado=valid;

        if (comprobado) {
            if (comprobando && !nombreDisponible) {
                valid = false;
                validado=false;
            }
        } else {
            _checkUsername.performClick();
            valid=false;
        }

        return valid;
    }
}