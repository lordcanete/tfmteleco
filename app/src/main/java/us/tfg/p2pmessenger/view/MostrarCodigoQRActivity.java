package us.tfg.p2pmessenger.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import us.tfg.p2pmessenger.ActivityBase;
import us.tfg.p2pmessenger.R;

/**
 * Actividad para mostrar el codigo QR. Se genera a partir de la
 * informacion pasada en el intent y se muestra un cuadrado con
 * el codigo, el texto del codigo y un boton para copiar al
 * portapapeles el texto. Actividad tomada de internet.
 * @author Alexander Farber
 * @see <a href="https://stackoverflow.com/users/165071/alexander-farber>Author</a>
 * @see <a href="https://stackoverflow.com/questions/28827407/generate-designer-2d-qr-code-in-android/30529519#30529519">Fuente</a>
 */
public class MostrarCodigoQRActivity extends ActivityBase {

    public final static int WHITE = 0xFFFFFFFF;
    public final static int BLACK = 0xFF000000;
    public final static int WIDTH = 400;
    public final static int HEIGHT = 400;
    private String texto;
    private Button _botonCopiar;
    private TextView _textoCodigo;
    private ImageView _imagenQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrar_codigo_qr);
        if (getIntent().hasExtra("texto"))
            texto = getIntent().getExtras().getString("texto");
        else
            texto = "No se ha proporcionado informacion";



        _botonCopiar = (Button) findViewById(R.id.boton_copiar_codigo);

        _botonCopiar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Starting a new async task
                if (!"".equals(_textoCodigo.getText().toString())) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("",
                            _textoCodigo.getText().toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getBaseContext(), R.string.texto_mensaje_copiado, Toast.LENGTH_SHORT).show();
                }
            }
        });

        _textoCodigo = (TextView) findViewById(R.id.texto_codigo_qr);
        _textoCodigo.setText(texto);
        _imagenQR = (ImageView) findViewById(R.id.imagen_codigo_qr);
    }

    @Override
    protected void onStart() {
        super.onStart();
            _textoCodigo.setText(texto);
            if (!"".equals(texto)) {
                try {
                    Bitmap bitmap = encodeAsBitmap(texto);
                    _imagenQR.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    Log.d(TAG, getClass() + ".onStart() - excepcion al crear el codigo QR", e);
                }
            }
    }

    /**
     * Codigo encontrado en
     * Generacion de codigo QR a partir de texto
     */
    private Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, HEIGHT, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    /**
     * Menu de opciones
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
