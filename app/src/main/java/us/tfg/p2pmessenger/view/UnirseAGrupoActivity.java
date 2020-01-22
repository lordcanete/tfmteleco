package us.tfg.p2pmessenger.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dx.dxloadingbutton.lib.LoadingButton;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.samples.vision.barcodereader.BarcodeCaptureActivity;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;

/**
 * Actividad con un cuadro de texto y un boton que
 * lleva al lector de codigos QR. Si no se desea
 * utilizar el lector (o no se puede) se permite
 * la opcion de introucir la informacion de manera
 * manual.
 */
public class UnirseAGrupoActivity extends ActivityBase {

    private TextView _textCodigoGrupo;
    private LoadingButton _botonUnir;
    private LoadingButton _botonAbrirQR;
    private BarcodeDetector detector;
    private String codigo;
    private static final int RC_BARCODE_CAPTURE = 9001;

    /**
     * Notifica un cambio en algun campo de texto de
     * entrada
     */
    private final TextWatcher watcher1 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            _botonAbrirQR.reset();
            _botonUnir.reset();
        }
    };

    /**
     * Al abrir el lector de codigo QR se inicia la actividad
     * {@link BarcodeCaptureActivity} y al termino nos avisa
     * con el metodo {@link this#onActivityResult(int, int, Intent)}
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unirse_a_grupo);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Unirse a grupo");
            ab.setDisplayHomeAsUpEnabled(true);
        }

        _textCodigoGrupo = (TextView) findViewById(R.id.texto_codigo_unirse);
        _textCodigoGrupo.addTextChangedListener(watcher1);

        _botonUnir = (LoadingButton) findViewById(R.id.boton_unirse);
        _botonAbrirQR = (LoadingButton) findViewById(R.id.boton_abrir_lector_qr);

        // lanza la actividad que lee el codigo QR
        _botonAbrirQR.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_botonAbrir.onClick(" + v + ")");
                Intent intent = new Intent(UnirseAGrupoActivity.this, BarcodeCaptureActivity.class);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                _botonAbrirQR.startLoading();

            }
        });


        _botonUnir.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_botonUnir.onClick(" + v + ")");
                _botonUnir.startLoading();
                codigo=_textCodigoGrupo.getText().toString();
                if(!"".equals(codigo)) {
                    String valores[]=codigo.split("-");
                    if(valores.length==2)
                        controlador.enviarPeticionUnirAGrupo(valores[0],valores[1]);
                    else
                        onJoinGroupFailed();
                }

            }
        });
        // hace uso de la biblioteca de android para la lectura
        // de codigos de barra y QR
        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();
        if (!detector.isOperational()) {
            Toast.makeText(this,"Error al leer el código QR",Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Cuando se ha terminado de leer el codigo, la actividad encargada
     * devuelve el resultado del intent aqui. Si la lectura es erronea
     * nos avisa con un mensaje emergente
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    _textCodigoGrupo.setText(barcode.displayValue);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    _botonAbrirQR.loadingSuccessful();
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                    Toast.makeText(this,"Error al leer el código QR",Toast.LENGTH_SHORT).show();
                    _botonAbrirQR.loadingFailed();
                }
            } else {
                _botonAbrirQR.loadingFailed();
                Toast.makeText(this,"Error al leer el código QR",Toast.LENGTH_SHORT).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Metodo llamado asincronamente cuando se realiza correctamente
     * la insercion del usuario en el grupo y este de nuevo
     * a pastry
     */
    @Override
    public void onJoinGroupSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,getClass()+".onCreateGroupSuccess()");
                _botonUnir.loadingSuccessful();
                UnirseAGrupoActivity.super.onBackPressed();
            }
        });
    }

    /**
     * Si la operacion fallo, nos trae devuelta aqui
     */
    @Override
    public void onJoinGroupFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,getClass()+".onCreateGroupFailed()");
                _botonUnir.loadingFailed();
            }
        });
    }


}
