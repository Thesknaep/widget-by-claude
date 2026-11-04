package cl.yerko.clawd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Editor de pixel art para diseñar tu propia mascota. */
public class PixelEditor extends Activity {
    private static final int FONDO = 0xFF1A1716, TARJETA = 0xFF262220, NARANJA = 0xFFD77757,
            CLARO = 0xFFF5EDE8, GRIS = 0xFFB3A69E;
    private static final int LAPIZ = 0, BORRAR = 1, BALDE = 2;
    private static final String SEC_PREVIA = "NNNNBNNWNWNSNSNNQJJJQNNAACAD";

    private float d;
    private Mascota.Datos datos;
    private int color = 1, herramienta = LAPIZ;
    private boolean espejo = false;
    private final ArrayDeque<byte[]> historial = new ArrayDeque<>();
    private Lienzo lienzo;
    private LinearLayout paleta, herramientas;
    private ImageView previa;
    private int cuadro;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(FONDO);
        getWindow().setNavigationBarColor(FONDO);

        String id = getIntent().getStringExtra("id");
        datos = id == null ? null : Mascota.buscar(this, id);
        if (datos == null) {
            datos = new Mascota.Datos();
        }

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(14), px(10), px(14), px(28));

        LinearLayout barra = new LinearLayout(this);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        TextView cerrar = texto("✕", 26, CLARO);
        cerrar.setPadding(px(8), px(8), px(8), px(8));
        cerrar.setOnClickListener(v -> finish());
        barra.addView(cerrar);
        TextView titulo = texto("Diseña tu mascota", 21, CLARO);
        titulo.setGravity(Gravity.CENTER);
        barra.addView(titulo, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView ok = texto("✓", 28, NARANJA);
        ok.setPadding(px(8), px(8), px(8), px(8));
        ok.setOnClickListener(v -> guardar());
        barra.addView(ok);
        col.addView(barra);

        // Vista previa animada
        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(px(12), px(8), px(12), px(8));
        fila.setBackground(fondo(TARJETA, 16));
        previa = new ImageView(this);
        fila.addView(previa, new LinearLayout.LayoutParams(px(90), px(70)));
        TextView ayuda = texto("Así se moverá. Pinta los ojos con 👁 Ojo para que pueda parpadear y enojarse.", 13, GRIS);
        ayuda.setPadding(px(12), 0, 0, 0);
        fila.addView(ayuda, new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(fila, ancho(8));

        // Lienzo
        lienzo = new Lienzo(this);
        lienzo.setBackground(fondo(TARJETA, 12));
        int anchoPx = getResources().getDisplayMetrics().widthPixels - px(28);
        col.addView(lienzo, new LinearLayout.LayoutParams(-1, anchoPx * Mascota.H / Mascota.W));

        // Herramientas
        herramientas = new LinearLayout(this);
        col.addView(herramientas, ancho(10));
        pintarHerramientas();

        // Paleta
        col.addView(texto("Colores", 14, GRIS), ancho(12));
        paleta = new LinearLayout(this);
        paleta.setOrientation(LinearLayout.VERTICAL);
        col.addView(paleta, ancho(6));
        pintarPaleta();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(FONDO);
        sv.setFitsSystemWindows(true);
        sv.addView(col);
        setContentView(sv);
        animar();
    }

    private void animar() {
        previa.setImageBitmap(Mascota.dibujar(datos.pal, datos.px, SEC_PREVIA.charAt(cuadro++ % SEC_PREVIA.length())));
        h.postDelayed(this::animar, 180);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void guardarHistorial() {
        historial.push(datos.px.clone());
        while (historial.size() > 40) historial.removeLast();
    }

    private void pintarHerramientas() {
        herramientas.removeAllViews();
        herramientas.setOrientation(LinearLayout.VERTICAL);
        LinearLayout fila1 = new LinearLayout(this), fila2 = new LinearLayout(this);
        herramientas.addView(fila1, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-1, -2);
        l2.topMargin = px(6);
        herramientas.addView(fila2, l2);

        herramienta(fila1, "✏️", "Lápiz", herramienta == LAPIZ, v -> { herramienta = LAPIZ; pintarHerramientas(); });
        herramienta(fila1, "🧽", "Borrar", herramienta == BORRAR, v -> { herramienta = BORRAR; pintarHerramientas(); });
        herramienta(fila1, "🪣", "Rellenar", herramienta == BALDE, v -> { herramienta = BALDE; pintarHerramientas(); });
        herramienta(fila1, "🪞", "Espejo", espejo, v -> { espejo = !espejo; pintarHerramientas(); });
        herramienta(fila2, "📷", "Foto", false, v -> elegirFoto());
        herramienta(fila2, "↩️", "Deshacer", false, v -> {
            if (!historial.isEmpty()) {
                datos.px = historial.pop();
                lienzo.invalidate();
            }
        });
        herramienta(fila2, "🧩", "Plantilla", false, v -> {
            guardarHistorial();
            datos.px = Mascota.plantilla();
            lienzo.invalidate();
        });
        herramienta(fila2, "🗑️", "Limpiar", false, v -> new AlertDialog.Builder(this)
                .setMessage("¿Borrar todo el dibujo?")
                .setPositiveButton("Borrar", (x, w) -> {
                    guardarHistorial();
                    datos.px = Mascota.Datos.vacio();
                    lienzo.invalidate();
                })
                .setNegativeButton("Cancelar", null).show());
    }

    // ---------- foto a pixel art ----------

    private static final int PEDIR_FOTO = 88;

    private void elegirFoto() {
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
                .addCategory(android.content.Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        try {
            startActivityForResult(android.content.Intent.createChooser(i, "Elige una foto"), PEDIR_FOTO);
        } catch (Exception e) {
            Toast.makeText(this, "No encontré la galería", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PEDIR_FOTO || res != RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri uri = data.getData();
        Toast.makeText(this, "Convirtiendo tu foto en pixel art…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            FotoPixel.Resultado r = null;
            try {
                android.graphics.Bitmap bmp = cargar(uri, 256);
                if (bmp != null) {
                    int w = bmp.getWidth(), h = bmp.getHeight();
                    int[] argb = new int[w * h];
                    bmp.getPixels(argb, 0, w, 0, 0, w, h);
                    r = FotoPixel.convertir(argb, w, h, datos.pal, Mascota.MAX_COLORES);
                }
            } catch (Exception ignored) {
            }
            final FotoPixel.Resultado fr = r;
            runOnUiThread(() -> {
                if (fr == null) {
                    Toast.makeText(this, "No pude abrir esa foto", Toast.LENGTH_LONG).show();
                    return;
                }
                guardarHistorial();
                datos.pal = fr.pal;
                datos.px = fr.px;
                lienzo.invalidate();
                pintarPaleta();
                new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                        .setTitle("📷 ¡Listo!")
                        .setMessage("Tu foto ya es pixel art.\n\n"
                                + "• Pinta los ojos con 👁 Ojo para que parpadee y se enoje.\n"
                                + "• Si quedó algo del fondo, bórralo con 🧽.\n"
                                + "• Funciona mejor con fotos de fondo liso.\n"
                                + "• ↩️ Deshacer vuelve a como estaba.")
                        .setPositiveButton("Entendido", null)
                        .show();
            });
        }).start();
    }

    /** Abre la foto reducida (máx. lado = max px) y corrige la rotación de la cámara. */
    private android.graphics.Bitmap cargar(android.net.Uri uri, int max) throws Exception {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            android.graphics.BitmapFactory.decodeStream(in, null, o);
        }
        int muestra = 1;
        while (Math.max(o.outWidth, o.outHeight) / (muestra * 2) >= max) muestra *= 2;
        android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
        o2.inSampleSize = muestra;
        android.graphics.Bitmap b;
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            b = android.graphics.BitmapFactory.decodeStream(in, null, o2);
        }
        if (b == null) return null;
        int giro = 0;
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            int or = new android.media.ExifInterface(in).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            if (or == android.media.ExifInterface.ORIENTATION_ROTATE_90) giro = 90;
            else if (or == android.media.ExifInterface.ORIENTATION_ROTATE_180) giro = 180;
            else if (or == android.media.ExifInterface.ORIENTATION_ROTATE_270) giro = 270;
        } catch (Exception ignored) {
        }
        float esc = Math.min(1f, max / (float) Math.max(b.getWidth(), b.getHeight()));
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.postScale(esc, esc);
        m.postRotate(giro);
        android.graphics.Bitmap r = android.graphics.Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
        return r.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
    }

    private void herramienta(LinearLayout padre, String icono, String nombre, boolean activa, View.OnClickListener cl) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, px(6), 0, px(6));
        b.setBackground(fondo(activa ? 0xFF4A3F3A : TARJETA, 12));
        b.addView(texto(icono, 18, CLARO));
        TextView t = texto(nombre, 10, activa ? NARANJA : GRIS);
        t.setGravity(Gravity.CENTER);
        b.addView(t);
        b.setOnClickListener(cl);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(px(2), 0, px(2), 0);
        padre.addView(b, lp);
    }

    private void pintarPaleta() {
        paleta.removeAllViews();
        int porFila = 8;
        LinearLayout fila = null;
        int total = datos.pal.length + 1;
        for (int i = 0; i < total; i++) {
            if (i % porFila == 0) {
                fila = new LinearLayout(this);
                paleta.addView(fila, ancho(i == 0 ? 0 : 6));
            }
            TextView sw = new TextView(this);
            sw.setGravity(Gravity.CENTER);
            sw.setTextSize(12);
            GradientDrawable g;
            if (i == datos.pal.length) {
                g = fondo(0xFF3A3431, 10);
                sw.setText("+");
                sw.setTextColor(CLARO);
                sw.setTextSize(20);
                sw.setOnClickListener(v -> colorNuevo());
            } else {
                final int k = i;
                g = fondo(datos.pal[i], 10);
                if (i == Mascota.OJO) {
                    sw.setText("👁");
                }
                if (i == color) g.setStroke(px(3), NARANJA);
                else g.setStroke(px(1), 0xFF4A3F3A);
                sw.setOnClickListener(v -> {
                    color = k;
                    if (herramienta == BORRAR) herramienta = LAPIZ;
                    pintarPaleta();
                    pintarHerramientas();
                });
            }
            sw.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(38), 1f);
            lp.setMargins(px(3), 0, px(3), 0);
            fila.addView(sw, lp);
        }
        int resto = (porFila - total % porFila) % porFila;
        for (int i = 0; i < resto; i++) fila.addView(new View(this), new LinearLayout.LayoutParams(0, px(38), 1f));
    }

    private void colorNuevo() {
        if (datos.pal.length >= Mascota.MAX_COLORES) {
            Toast.makeText(this, "Ya tienes muchos colores", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(20), px(10), px(20), px(4));
        View muestra = new View(this);
        box.addView(muestra, new LinearLayout.LayoutParams(-1, px(56)));
        final float[] hsv = {20f, 0.6f, 0.85f};
        Runnable act = () -> muestra.setBackground(fondo(Color.HSVToColor(hsv), 12));
        String[] nombres = {"Tono", "Intensidad", "Brillo"};
        int[] max = {360, 100, 100};
        int[] ini = {20, 60, 85};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            box.addView(texto(nombres[i], 13, GRIS), ancho(12));
            SeekBar s = new SeekBar(this);
            s.setMax(max[i]);
            s.setProgress(ini[i]);
            s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int v, boolean u) {
                    hsv[k] = k == 0 ? v : v / 100f;
                    act.run();
                }
                public void onStartTrackingTouch(SeekBar sb) { }
                public void onStopTrackingTouch(SeekBar sb) { }
            });
            box.addView(s);
        }
        act.run();
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Nuevo color")
                .setView(box)
                .setPositiveButton("Agregar", (x, w) -> {
                    datos.pal = Arrays.copyOf(datos.pal, datos.pal.length + 1);
                    datos.pal[datos.pal.length - 1] = Color.HSVToColor(hsv);
                    color = datos.pal.length - 1;
                    herramienta = LAPIZ;
                    pintarPaleta();
                    pintarHerramientas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardar() {
        if (datos.vacia()) {
            Toast.makeText(this, "Dibuja tu mascota primero", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText nombre = new EditText(this);
        nombre.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nombre.setText(datos.nombre);
        nombre.setSelectAllOnFocus(true);
        nombre.setTextColor(CLARO);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(px(20), px(8), px(20), 0);
        box.addView(nombre, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("¿Cómo se llama tu mascota?")
                .setView(box)
                .setPositiveButton("Guardar", (x, w) -> {
                    String n = nombre.getText().toString().trim();
                    datos.nombre = n.isEmpty() ? "Mi mascota" : n;
                    Mascota.guardar(this, datos);
                    Skins.elegir(this, "p:" + datos.id);
                    ClawdWidget.pintar(this, null);
                    Toast.makeText(this, datos.nombre + " ya está en tu widget", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- lienzo de dibujo ----------

    private class Lienzo extends View {
        private final Paint p = new Paint();
        private int ultimo = -1;

        Lienzo(Context c) { super(c); }

        private float celda() { return Math.min(getWidth() / (float) Mascota.W, getHeight() / (float) Mascota.H); }

        @Override
        protected void onDraw(Canvas c) {
            float s = celda();
            float ox = (getWidth() - s * Mascota.W) / 2, oy = (getHeight() - s * Mascota.H) / 2;
            for (int y = 0; y < Mascota.H; y++) {
                for (int x = 0; x < Mascota.W; x++) {
                    int i = datos.px[y * Mascota.W + x];
                    p.setColor(i >= 0 && i < datos.pal.length ? datos.pal[i]
                            : ((x + y) % 2 == 0 ? 0xFF2E2926 : 0xFF353029));
                    c.drawRect(ox + x * s, oy + y * s, ox + (x + 1) * s, oy + (y + 1) * s, p);
                }
            }
            p.setColor(0x22FFFFFF);
            for (int x = 0; x <= Mascota.W; x++) c.drawRect(ox + x * s, oy, ox + x * s + 1, oy + s * Mascota.H, p);
            for (int y = 0; y <= Mascota.H; y++) c.drawRect(ox, oy + y * s, ox + s * Mascota.W, oy + y * s + 1, p);
            if (espejo) {
                p.setColor(0x55D77757);
                float mx = ox + s * Mascota.W / 2;
                c.drawRect(mx - 1, oy, mx + 1, oy + s * Mascota.H, p);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float s = celda();
            float ox = (getWidth() - s * Mascota.W) / 2, oy = (getHeight() - s * Mascota.H) / 2;
            int x = (int) ((e.getX() - ox) / s), y = (int) ((e.getY() - oy) / s);
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
                guardarHistorial();
                ultimo = -1;
            }
            if (x >= 0 && y >= 0 && x < Mascota.W && y < Mascota.H) {
                int celdaIdx = y * Mascota.W + x;
                if (herramienta == BALDE) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        rellenar(x, y);
                        if (espejo) rellenar(Mascota.W - 1 - x, y);
                    }
                } else if (celdaIdx != ultimo) {
                    byte v = herramienta == BORRAR ? -1 : (byte) color;
                    datos.px[celdaIdx] = v;
                    if (espejo) datos.px[y * Mascota.W + (Mascota.W - 1 - x)] = v;
                    ultimo = celdaIdx;
                }
                invalidate();
            }
            if (e.getAction() == MotionEvent.ACTION_UP) performClick();
            return true;
        }

        @Override
        public boolean performClick() { return super.performClick(); }

        private void rellenar(int x0, int y0) {
            byte destino = datos.px[y0 * Mascota.W + x0];
            byte nuevo = (byte) color;
            if (destino == nuevo) return;
            ArrayDeque<int[]> cola = new ArrayDeque<>();
            cola.add(new int[]{x0, y0});
            while (!cola.isEmpty()) {
                int[] q = cola.poll();
                int x = q[0], y = q[1];
                if (x < 0 || y < 0 || x >= Mascota.W || y >= Mascota.H) continue;
                int k = y * Mascota.W + x;
                if (datos.px[k] != destino) continue;
                datos.px[k] = nuevo;
                cola.add(new int[]{x + 1, y});
                cola.add(new int[]{x - 1, y});
                cola.add(new int[]{x, y + 1});
                cola.add(new int[]{x, y - 1});
            }
        }
    }

    // ---------- piezas de interfaz ----------

    private int px(int dp) { return Math.round(dp * d); }

    private LinearLayout.LayoutParams ancho(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(top);
        return lp;
    }

    private TextView texto(String s, int sp, int c) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(c);
        return t;
    }

    private GradientDrawable fondo(int c, int radio) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(px(radio));
        return g;
    }
}
