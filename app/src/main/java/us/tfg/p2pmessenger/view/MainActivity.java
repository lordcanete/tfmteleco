package us.tfg.p2pmessenger.view;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.ApplicationExtended;
import us.tfg.p2pmessenger.R;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_APAGADO;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_INICIO_SESION;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_NECESARIA_DIRECION;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_REGISTRO;
import static us.tfg.p2pmessenger.controller.Controlador.MODO_SESION_INICIADA;


public class MainActivity extends ActivityBase {

    public static final int DESCONECTAR = 1;
    public static final int CERRAR_SESION = 2;
    public static final int CREAR_RED_PASTRY = 3;
    private final static int MY_PERMISSIONS_REQUEST = 0;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private boolean pWES;
    private boolean pINT;
    private boolean pNS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, getClass() + ".onCreateEntorno(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);

        permissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!pWES && !pINT && !pNS) {
            if (controlador.isIniciado()) {

                int accion = 0;
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    accion = extras.getInt("accion");
                    switch (accion) {
                        case DESCONECTAR:
                            controlador.apagar();
                            break;
                        case CERRAR_SESION:
                            controlador.cerrarSesion();
                            break;
                        case CREAR_RED_PASTRY:
                            controlador.crearRedPastry();
                            break;
                    }
                }

                ApplicationExtended.getInstance().setCurrentActivity(this);
                Intent intent = null;

                if (controlador.getModo() != -1)
                    setTheme(R.style.LoginTheme);

                setContentView(R.layout.activity_conversaciones);


                switch (controlador.getModo()) {
                    case MODO_APAGADO:
                        if(accion==DESCONECTAR || accion == CERRAR_SESION) {
                            this.finishAffinity();
                            Toast.makeText(getBaseContext(), "Aplicación desconectada",
                                    Toast.LENGTH_LONG).show();
                        }
                        break;
                    case MODO_SESION_INICIADA:
                        intent = new Intent(this, ConversacionesActivity.class);
                        break;
                    case MODO_NECESARIA_DIRECION:
                        intent = new Intent(this, UnirseAPastryActivity.class);
                        break;
                    case MODO_INICIO_SESION:
                        intent = new Intent(this, LoginActivity.class);
                        break;
                    case MODO_REGISTRO:
                        intent = new Intent(this, SignupActivity.class);
                        break;
                    default:
                        intent = null;
                }

                //Intent intent = new Intent(this, ConversacionActivity.class);

                if (intent != null) {
                    startActivity(intent);
                    finish();
                } else if(accion==DESCONECTAR || accion == CERRAR_SESION) {
                    Toast.makeText(getBaseContext(), "Ha ocurrido un error",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                controlador.iniciar();
            }
        } else {
            Toast.makeText(getBaseContext(), "Faltan permisos",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected void onServiceLoaded() {
        Log.d(TAG, getClass() + ".onServiceLoded()");
        ApplicationExtended.getInstance().setCurrentActivity(this);
        Intent intent = null;

        //setContentView(R.layout.activity_conversaciones);


        switch (controlador.getModo()) {
            case MODO_APAGADO:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "El nodo no se ha encendido",
                                Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case MODO_SESION_INICIADA:
                intent = new Intent(this, ConversacionesActivity.class);
                break;
            case MODO_NECESARIA_DIRECION:
                intent = new Intent(this, UnirseAPastryActivity.class);
                break;
            case MODO_INICIO_SESION:
                intent = new Intent(this, LoginActivity.class);
                break;
            case MODO_REGISTRO:
                intent = new Intent(this, SignupActivity.class);
                break;
            default:
                intent = null;
        }

        //Intent intent = new Intent(this, ConversacionActivity.class);

        if (intent != null) {
            startActivity(intent);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Ha ocurrido un error",
                            Toast.LENGTH_LONG).show();
                }
            });

        }
        finish();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, getClass() + ".onPause()");
        super.onPause();
    }


    // Método para pedir los permisos necesarios para la aplicación.
    // Comprueba qué permisos posee ya, y los que no posee los pide.
    private void permissions() {
        Log.d(TAG, "Permissions()");

        int numPermisos = 0;
        String[] permisos;
        pWES = false;
        pINT = false;
        pNS = false;

        //if (ContextCompat.checkSelfPermission(MainActivity.this,
        //        WRITE_EXTERNAL_STORAGE)
        //        != PackageManager.PERMISSION_GRANTED) {
        //    numPermisos++;
        //    pWES = true;
        //}

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            numPermisos++;
            pINT = true;
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            numPermisos++;
            pNS = true;
        }


        if (numPermisos > 0) {
            permisos = new String[numPermisos];
            int i = 0;
            if (pWES) {
                permisos[i] = WRITE_EXTERNAL_STORAGE;
                i++;
            }

            if (pINT) {
                permisos[i] = INTERNET;
                i++;
            }

            if (pNS) {
                permisos[i] = ACCESS_NETWORK_STATE;
                i++;
            }
            ActivityCompat.requestPermissions(MainActivity.this, permisos, MY_PERMISSIONS_REQUEST);
        }
    }

    // Método para el permiso de escritura en el almacenamiento externo.
    private void permissionWriteES() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    WRITE_EXTERNAL_STORAGE)) {

                // Mostrar explicación al usuario ?
            } else {
                // No explicación, pedimos el permiso
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

            }
        } else {
            //crearFicheros();
        }
    }

    @Override
    // Método que recibe el resultado de la petición de permisos.
    // Aquí introducimos el código que se debía ejecutar si necesitabamos
    // permiso para ejecutarlo.
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST:
                for (String permission : permissions) {
                    switch (permission) {
                        case WRITE_EXTERNAL_STORAGE:
                            //crearFicheros();
                            break;
                        case INTERNET:
                            break;
                        case ACCESS_NETWORK_STATE:
                            break;
                    }
                }
                return;
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //crearFicheros();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
        }
    }

}