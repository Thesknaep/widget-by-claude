package cl.yerko.clawd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class AlarmaEditor extends Activity {
    private static final int FONDO = 0xFF1A1716, TARJETA = 0xFF262220, NARANJA = 0xFFD77757,
            CLARO = 0xFFF5EDE8, GRIS = 0xFFB3A69E, LINEA = 0xFF3A3431;
    private static final String[] ETIQUETAS = {"Despertar", "Tarea", "Estudiar", "Clases", "Crear mi juego",
            "Entrenar", "Tomar remedio", "Dormir"};

    private float d;
    private Alarmas.Alarma a;
    private NumberPicker hora, minuto, ampm;
    private TextView sonara, vTono, vPropio, vRepetir, vEtiqueta;
    private MediaPlayer prueba;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(FONDO);
        getWindow().setNavigationBarColor(FONDO);

        int id = getIntent().getIntExtra("id", -1);
        a = id >= 0 ? Alarmas.buscar(this, id) : null;
        boolean nueva = a == null;
        if (nueva) {
            a = new Alarmas.Alarma();
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, 1);
            a.h = c.get(Calendar.HOUR_OF_DAY);
            a.m = c.get(Calendar.MINUTE);
        }

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(16), px(12), px(16), px(28));

        // Barra superior: ✕  Agregar alarma  ✓
        LinearLayout barra = new LinearLayout(this);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        TextView cerrar = texto("✕", 26, CLARO);
        cerrar.setPadding(px(8), px(8), px(8), px(8));
        cerrar.setOnClickListener(v -> finish());
        barra.addView(cerrar);
        TextView titulo = texto(nueva ? "Agregar alarma" : "Editar alarma", 22, CLARO);
        titulo.setGravity(Gravity.CENTER);
        barra.addView(titulo, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView ok = texto("✓", 28, NARANJA);
        ok.setPadding(px(8), px(8), px(8), px(8));
        ok.setOnClickListener(v -> guardar());
        barra.addView(ok);
        col.addView(barra);

        sonara = texto("", 15, NARANJA);
        sonara.setGravity(Gravity.CENTER);
        col.addView(sonara, ancho(2));

        // Ruedas: hora | minuto | a. m./p. m.
        LinearLayout ruedas = new LinearLayout(this);
        ruedas.setGravity(Gravity.CENTER);
        hora = rueda(1, 12, null);
        String[] mins = new String[60];
        for (int i = 0; i < 60; i++) mins[i] = String.format(java.util.Locale.US, "%02d", i);
        minuto = rueda(0, 59, mins);
        ampm = rueda(0, 1, new String[]{"a. m.", "p. m."});
        ampm.setWrapSelectorWheel(false);
        ruedas.addView(hora, new LinearLayout.LayoutParams(px(90), px(220)));
        TextView dos = texto(":", 34, CLARO);
        dos.setPadding(px(4), 0, px(4), 0);
        ruedas.addView(dos);
        ruedas.addView(minuto, new LinearLayout.LayoutParams(px(90), px(220)));
        LinearLayout.LayoutParams lap = new LinearLayout.LayoutParams(px(96), px(220));
        lap.leftMargin = px(14);
        ruedas.addView(ampm, lap);
        col.addView(ruedas, ancho(10));

        hora.setValue(a.h % 12 == 0 ? 12 : a.h % 12);
        minuto.setValue(a.m);
        ampm.setValue(a.h < 12 ? 0 : 1);
        NumberPicker.OnValueChangeListener cambio = (p, viejo, nuevo) -> {
            leerRuedas();
            actualizar();
        };
        hora.setOnValueChangedListener(cambio);
        minuto.setOnValueChangedListener(cambio);
        ampm.setOnValueChangedListener(cambio);

        // Opciones
        LinearLayout opciones = new LinearLayout(this);
        opciones.setOrientation(LinearLayout.VERTICAL);
        opciones.setBackground(fondo(TARJETA, 20));
        opciones.setPadding(px(4), px(4), px(4), px(4));
        vTono = fila(opciones, "Tono", v -> elegirTono());
        separador(opciones);
        vPropio = fila(opciones, "Tono personalizado", v -> elegirArchivo());
        separador(opciones);
        vRepetir = fila(opciones, "Repetir", v -> elegirRepetir());
        separador(opciones);
        interruptor(opciones, "Vibrar al sonar", a.vibrar, on -> a.vibrar = on);
        separador(opciones);
        interruptor(opciones, "Eliminar después de sonar", a.borrar, on -> a.borrar = on);
        separador(opciones);
        vEtiqueta = fila(opciones, "Etiqueta", v -> elegirEtiqueta());
        col.addView(opciones, ancho(14));

        if (!nueva) {
            TextView borrar = texto("Eliminar alarma", 17, 0xFFE5484D);
            borrar.setGravity(Gravity.CENTER);
            borrar.setPadding(0, px(14), 0, px(14));
            borrar.setBackground(fondo(TARJETA, 20));
            borrar.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setMessage("¿Eliminar esta alarma?")
                    .setPositiveButton("Eliminar", (x, w) -> {
                        Alarmas.quitar(this, a.id);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null).show());
            col.addView(borrar, ancho(14));
        }

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(FONDO);
        sv.setFitsSystemWindows(true);
        sv.addView(col);
        setContentView(sv);
        actualizar();
    }

    private void leerRuedas() {
        int h12 = hora.getValue() % 12;
        a.h = h12 + (ampm.getValue() == 1 ? 12 : 0);
        a.m = minuto.getValue();
    }

    private void actualizar() {
        sonara.setText(Alarmas.faltaLargo(Alarmas.proxima(a, System.currentTimeMillis())));
        boolean propio = a.tono != null && a.tono.startsWith("file:");
        vTono.setText((propio ? "Personalizado" : Tonos.nombre(this, a.tono)) + "  ›");
        vPropio.setText((propio ? "🎵 " + Tonos.nombre(this, a.tono) : "Elegir audio") + "  ›");
        vRepetir.setText(a.repeticion() + "  ›");
        vEtiqueta.setText(a.etiqueta + "  ›");
    }

    private void guardar() {
        leerRuedas();
        if (Alarmas.guardar(this, a)) {
            Toast.makeText(this, Alarmas.faltaLargo(Alarmas.proxima(a, System.currentTimeMillis())),
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Tienes demasiadas alarmas", Toast.LENGTH_SHORT).show();
        }
    }

    private void elegirTono() {
        List<Tonos.Tono> lista = Tonos.todos(this);
        String[] nombres = new String[lista.size()];
        int sel = 0;
        for (int i = 0; i < lista.size(); i++) {
            nombres[i] = lista.get(i).nombre;
            if (lista.get(i).id.equals(a.tono)) sel = i;
        }
        final int[] elegido = {sel};
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Tono (toca para escuchar)")
                .setSingleChoiceItems(nombres, sel, (dlg, i) -> {
                    elegido[0] = i;
                    pararPrueba();
                    prueba = AlarmaService.reproducir(this, lista.get(i).id, true);
                })
                .setPositiveButton("Elegir", (dlg, w) -> {
                    a.tono = lista.get(elegido[0]).id;
                    actualizar();
                })
                .setNegativeButton("Cancelar", null)
                .setOnDismissListener(x -> pararPrueba())
                .show();
    }

    private static final int PEDIR_AUDIO = 77;

    /** Abre el selector de archivos del teléfono para elegir cualquier canción o audio. */
    private void elegirArchivo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("audio/*");
        try {
            startActivityForResult(i, PEDIR_AUDIO);
        } catch (Exception e) {
            Toast.makeText(this, "No encontré un selector de archivos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PEDIR_AUDIO || res != RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri uri = data.getData();
        Toast.makeText(this, "Agregando tu tono…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String id = Tonos.importar(this, uri);
            runOnUiThread(() -> {
                if (id == null) {
                    Toast.makeText(this, "No pude usar ese audio (máximo 30 MB)", Toast.LENGTH_LONG).show();
                    return;
                }
                a.tono = id;
                actualizar();
                pararPrueba();
                prueba = AlarmaService.reproducir(this, id, true);
                Toast.makeText(this, "🎵 Tono: " + Tonos.nombre(this, id), Toast.LENGTH_LONG).show();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::pararPrueba, 4000);
            });
        }).start();
    }

    private void pararPrueba() {
        if (prueba != null) {
            try { prueba.stop(); } catch (Exception ignored) { }
            prueba.release();
            prueba = null;
        }
    }

    private void elegirRepetir() {
        String[] ops = {"Una vez", "Diariamente", "Lunes a viernes", "Fin de semana", "Personalizado"};
        int sel = a.dias == 0 ? 0 : a.dias == Alarmas.DIARIO ? 1 : a.dias == Alarmas.LUN_VIE ? 2
                : a.dias == Alarmas.FINDE ? 3 : 4;
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Repetir")
                .setSingleChoiceItems(ops, sel, (dlg, i) -> {
                    dlg.dismiss();
                    if (i == 4) {
                        personalizado();
                        return;
                    }
                    a.dias = i == 0 ? 0 : i == 1 ? Alarmas.DIARIO : i == 2 ? Alarmas.LUN_VIE : Alarmas.FINDE;
                    actualizar();
                })
                .show();
    }

    private void personalizado() {
        String[] nombres = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        boolean[] sel = new boolean[7];
        for (int i = 0; i < 7; i++) sel[i] = (a.dias & (1 << i)) != 0;
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Elige los días")
                .setMultiChoiceItems(nombres, sel, (dlg, i, on) -> sel[i] = on)
                .setPositiveButton("Listo", (dlg, w) -> {
                    int mask = 0;
                    for (int i = 0; i < 7; i++) if (sel[i]) mask |= 1 << i;
                    a.dias = mask;
                    actualizar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void elegirEtiqueta() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(20), px(8), px(20), px(4));
        EditText campo = new EditText(this);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setText("Alarma".equals(a.etiqueta) ? "" : a.etiqueta);
        campo.setHint("Ej: Tarea de matemáticas");
        campo.setTextColor(CLARO);
        campo.setHintTextColor(0xFF6E625B);
        box.addView(campo);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        for (String e : ETIQUETAS) {
            TextView chip = texto(e, 14, CLARO);
            chip.setPadding(px(12), px(7), px(12), px(7));
            chip.setBackground(fondo(0xFF4A3F3A, 16));
            chip.setOnClickListener(v -> {
                campo.setText(e);
                campo.setSelection(e.length());
            });
            LinearLayout.LayoutParams lc = new LinearLayout.LayoutParams(-2, -2);
            lc.rightMargin = px(6);
            chips.addView(chip, lc);
        }
        hs.addView(chips);
        box.addView(hs, ancho(10));
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Etiqueta")
                .setView(box)
                .setPositiveButton("Listo", (dlg, w) -> {
                    String t = campo.getText().toString().trim();
                    a.etiqueta = t.isEmpty() ? "Alarma" : t;
                    actualizar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onPause() {
        pararPrueba();
        super.onPause();
    }

    // ---------- piezas de interfaz ----------

    private NumberPicker rueda(int min, int max, String[] textos) {
        NumberPicker p = new NumberPicker(this);
        p.setMinValue(min);
        p.setMaxValue(max);
        if (textos != null) p.setDisplayedValues(textos);
        p.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        p.setTextColor(CLARO);
        p.setTextSize(30 * d);
        p.setSelectionDividerHeight(Math.round(2 * d));
        p.setWrapSelectorWheel(true);
        return p;
    }

    private TextView fila(LinearLayout padre, String nombre, View.OnClickListener click) {
        LinearLayout f = new LinearLayout(this);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setPadding(px(14), px(16), px(14), px(16));
        f.addView(texto(nombre, 17, CLARO), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView valor = texto("", 15, GRIS);
        valor.setGravity(Gravity.END);
        valor.setMaxWidth(px(200));
        f.addView(valor);
        f.setOnClickListener(click);
        padre.addView(f);
        return valor;
    }

    interface AlCambiar { void cambio(boolean on); }

    private void interruptor(LinearLayout padre, String nombre, boolean valor, AlCambiar cb) {
        LinearLayout f = new LinearLayout(this);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setPadding(px(14), px(10), px(10), px(10));
        f.addView(texto(nombre, 17, CLARO), new LinearLayout.LayoutParams(0, -2, 1f));
        Switch s = new Switch(this);
        s.setChecked(valor);
        s.setOnCheckedChangeListener((x, on) -> cb.cambio(on));
        f.addView(s);
        f.setOnClickListener(v -> s.toggle());
        padre.addView(f);
    }

    private void separador(LinearLayout padre) {
        View l = new View(this);
        l.setBackgroundColor(LINEA);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Math.max(1, px(1) / 2));
        lp.setMargins(px(14), 0, px(14), 0);
        padre.addView(l, lp);
    }

    private int px(int dp) { return Math.round(dp * d); }

    private LinearLayout.LayoutParams ancho(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(top);
        return lp;
    }

    private TextView texto(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private GradientDrawable fondo(int color, int radio) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(radio));
        return g;
    }
}
