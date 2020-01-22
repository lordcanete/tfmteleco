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
 * Cuando se descarga por primera vez la aplicacion
 * esta actividad se llama para ofrecernos unirnos a
 * una red existente o crear la nuestra propia.
 * Al abrir el lector de codigos QR se inicia
 * {@link BarcodeCaptureActivity} y cuando finalice
 * el resultado nos lo muestra en
 * {@link this#onActivityResult(int, int, Intent)}
 *
 * Crear nuestra propia red solo hace que el primer
 * nodo al que se conecta sea el mismo.
 *
 * Permite el ingreso de la direccion a mano por si
 * no se quisiese o no se pudiese abrir el lector de
 * codigos QR
 *
 */
public class UnirseAPastryActivity extends ActivityBase {


    // vista de los objetos de la pantalla
    private TextView _textIPPuerto;
    private LoadingButton _botonUnir;
    private LoadingButton _botonAbrirQR;
    private LoadingButton _botonNuevaRed;

    // variable para lanzar y recoger el resultado de la lectura del codigo QR
    private BarcodeDetector detector;
    private static final int RC_BARCODE_CAPTURE = 9001;

    /**
     * Notifica cuando algun campo de texto cambia
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
     * Inicializa los campos de texto y los enlaces de los botones
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unirse_a_pastry);
        //android.support.v7.app.ActionBar ab = getSupportActionBar();
        //if (ab != null) {
        //    ab.setTitle("Unirse a la red");
        //}

        _textIPPuerto = (TextView) findViewById(R.id.texto_codigo_unirse);
        _textIPPuerto.addTextChangedListener(watcher1);

        _botonUnir = (LoadingButton) findViewById(R.id.boton_unirse);

        _botonUnir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (controlador.nuevaDireccionArranque(_textIPPuerto.getText().toString())) {
                    _botonUnir.loadingSuccessful();
                    finish();
                    Intent intent = new Intent(UnirseAPastryActivity.this,MainActivity.class);
                    startActivity(intent);
                    Toast.makeText(getBaseContext(), "Unido", Toast.LENGTH_SHORT).show();
                } else {
                    _botonUnir.loadingFailed();
                    Toast.makeText(getBaseContext(), "Error al unirse", Toast.LENGTH_LONG).show();
                }
            }
        });
        _botonAbrirQR = (LoadingButton) findViewById(R.id.boton_leer_codigo_qr);
        _botonNuevaRed = (LoadingButton) findViewById(R.id.boton_crear_red);

        _botonAbrirQR.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_botonAbrir.onClick(" + v + ")");
                Intent intent = new Intent(UnirseAPastryActivity.this, BarcodeCaptureActivity.class);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                _botonAbrirQR.startLoading();
            }
        });

        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();
        if (!detector.isOperational()) {
            Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show();
        }

        _botonNuevaRed.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, getClass() + "_botonNuevaRed.onClick(" + v + ")");
                controlador.crearRedPastry();
                finish();
                Intent intent = new Intent(UnirseAPastryActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
    }


    /**
     * Valor devuelto por {@link BarcodeCaptureActivity}
     * si es correcto el formato se termina y se pasa a
     * la activdiad de login.
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
                    _textIPPuerto.setText(barcode.displayValue);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    _botonAbrirQR.loadingSuccessful();
                    if (controlador.nuevaDireccionArranque(barcode.displayValue)) {
                        _botonUnir.loadingSuccessful();
                        finish();
                        Intent intent = new Intent(UnirseAPastryActivity.this,MainActivity.class);
                        startActivity(intent);
                        Toast.makeText(getBaseContext(), "Unido", Toast.LENGTH_SHORT).show();
                        super.onBackPressed();
                    } else {
                        _botonUnir.loadingFailed();
                        Toast.makeText(getBaseContext(), "Error al unirse", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                    Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show();
                    _botonAbrirQR.loadingFailed();
                }
            } else {
                _botonAbrirQR.loadingFailed();
                Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}

