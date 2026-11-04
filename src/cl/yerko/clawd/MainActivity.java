package cl.yerko.clawd;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int FONDO = 0xFF1A1716, TARJETA = 0xFF262220, NARANJA = 0xFFD77757,
            AZUL = 0xFF4FA3F7, CLARO = 0xFFF5EDE8, GRIS = 0xFFB3A69E;

    private float d;
    private EditText peso, estatura;
    private TextView meta, progreso;
    private ProgressBar barra;
    private LinearLayout vineta, formulario;
    private TextView resumenMeta, resumenDatos;
    private Button cancelar;
    private LinearLayout listaAlarmas;
    private LinearLayout filaSkins;
    private TextView hola;

    /** Pide el nombre de quien usa la app. La primera vez es obligatorio. */
    private void pedirNombre(boolean primeraVez) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(20), px(8), px(20), 0);
        if (primeraVez) {
            box.addView(texto("Soy Claude, tu mascota. Te voy a motivar, recordar tomar agua y despertarte. "
                    + "¿Cómo quieres que te llame?", 15, GRIS));
        }
        EditText campo = new EditText(this);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        campo.setHint("Tu nombre");
        campo.setText(Frases.nombre(this));
        campo.setSelectAllOnFocus(true);
        campo.setTextColor(CLARO);
        campo.setHintTextColor(0xFF6E625B);
        box.addView(campo, ancho(10));
        android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(primeraVez ? "¡Hola! 👋" : "Tu nombre")
                .setView(box)
                .setPositiveButton("Listo", null)
                .setCancelable(!primeraVez)
                .create();
        if (!primeraVez) dlg.setButton(android.app.AlertDialog.BUTTON_NEGATIVE, "Cancelar",
                (android.content.DialogInterface.OnClickListener) null);
        dlg.setOnShowListener(x -> dlg.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = campo.getText().toString().trim();
            if (n.isEmpty()) {
                campo.setError("Escribe tu nombre");
                return;
            }
            if (n.length() > 20) n = n.substring(0, 20);
            Frases.guardarNombre(this, n);
            hola.setText(Frases.saludo(this));
            ClawdWidget.pintar(this, null);
            dlg.dismiss();
        }));
        dlg.show();
    }
    private ImageView petArriba;

    private void pintarSkins() {
        filaSkins.removeAllViews();
        String actual = Skins.actual(this);
        java.util.List<String[]> todas = new java.util.ArrayList<>();
        for (int i = 0; i < Skins.IDS.length; i++) todas.add(new String[]{Skins.IDS[i], Skins.NOMBRES[i]});
        for (Mascota.Datos m : Mascota.lista(this)) todas.add(new String[]{"p:" + m.id, m.nombre});
        for (String[] sk : todas) {
            String id = sk[0], nombre = sk[1];
            boolean propia = Skins.propia(id);
            boolean sel = id.equals(actual);
            LinearLayout c = tarjetaSkin(sel);
            ImageView img = new ImageView(this);
            Skins.poner(this, img, id, "marciano".equals(id) ? '3' : 'N');
            c.addView(img, new LinearLayout.LayoutParams(px(84), px(66)));
            TextView n = texto((propia ? "✎ " : "") + nombre, 12, sel ? NARANJA : CLARO);
            n.setGravity(Gravity.CENTER);
            n.setMaxLines(2);
            c.addView(n, new LinearLayout.LayoutParams(px(96), -2));
            c.setOnClickListener(v -> {
                Skins.elegir(this, id);
                ClawdWidget.pintar(this, null);
                if (petArriba != null) Skins.poner(this, petArriba, 'W');
                pintarSkins();
                Toast.makeText(this, "Skin: " + nombre, Toast.LENGTH_SHORT).show();
            });
            if (propia) {
                c.setOnLongClickListener(v -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle(nombre)
                            .setItems(new String[]{"✏️ Editar", "🗑️ Eliminar"}, (dlg, w) -> {
                                if (w == 0) {
                                    startActivity(new android.content.Intent(this, PixelEditor.class)
                                            .putExtra("id", id.substring(2)));
                                } else {
                                    Mascota.borrar(this, id.substring(2));
                                    ClawdWidget.pintar(this, null);
                                    if (petArriba != null) Skins.poner(this, petArriba, 'W');
                                    pintarSkins();
                                }
                            }).show();
                    return true;
                });
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = px(8);
            filaSkins.addView(c, lp);
        }

        // "+ Crear": diseña tu propia mascota en pixel art.
        LinearLayout crear = tarjetaSkin(false);
        TextView mas = texto("+", 40, NARANJA);
        mas.setGravity(Gravity.CENTER);
        crear.addView(mas, new LinearLayout.LayoutParams(px(84), px(66)));
        TextView t = texto("Crear la mía", 12, NARANJA);
        t.setGravity(Gravity.CENTER);
        crear.addView(t, new LinearLayout.LayoutParams(px(96), -2));
        crear.setOnClickListener(v -> startActivity(new android.content.Intent(this, PixelEditor.class)));
        filaSkins.addView(crear, new LinearLayout.LayoutParams(-2, -2));
    }

    private LinearLayout tarjetaSkin(boolean sel) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(px(10), px(10), px(10), px(10));
        GradientDrawable g = fondo(0xFF2E2926, 16);
        if (sel) g.setStroke(px(2), NARANJA);
        c.setBackground(g);
        return c;
    }

    private static final String[] RAPIDAS = {"Despertar", "Tarea", "Estudiar", "Crear mi juego",
            "Entrenar", "Tomar remedio", "Dormir"};

    private void nuevaAlarma() {
        startActivity(new android.content.Intent(this, AlarmaEditor.class));
    }

    private void pintarAlarmas() {
        listaAlarmas.removeAllViews();
        java.util.List<Alarmas.Alarma> l = Alarmas.lista(this);
        if (l.isEmpty()) {
            listaAlarmas.addView(texto("Aún no tienes alarmas", 14, 0xFF6E625B));
            return;
        }
        for (Alarmas.Alarma a : l) {
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(px(16), px(12), px(10), px(12));
            fila.setBackground(fondo(0xFF2E2926, 16));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            int color = a.activa ? CLARO : 0xFF6E625B;
            String hm = a.hora();
            int esp = hm.indexOf(' ');
            LinearLayout linea = new LinearLayout(this);
            linea.setGravity(Gravity.BOTTOM);
            linea.addView(texto(hm.substring(0, esp), 32, color));
            TextView sufijo = texto(" " + hm.substring(esp + 1), 15, color);
            sufijo.setPadding(0, 0, 0, px(5));
            linea.addView(sufijo);
            info.addView(linea);
            info.addView(texto(a.etiqueta + " · " + a.repeticion(), 14, a.activa ? NARANJA : 0xFF6E625B));
            if (a.activa) info.addView(texto(Alarmas.faltaLargo(Alarmas.proxima(a, System.currentTimeMillis())), 12, GRIS));
            fila.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

            android.widget.Switch sw = new android.widget.Switch(this);
            sw.setChecked(a.activa);
            sw.setOnCheckedChangeListener((b, on) -> {
                Alarmas.activar(this, a.id, on);
                if (on) {
                    Toast.makeText(this, Alarmas.faltaLargo(Alarmas.proxima(a, System.currentTimeMillis())),
                            Toast.LENGTH_SHORT).show();
                }
                pintarAlarmas();
            });
            fila.addView(sw);
            fila.setOnClickListener(v -> startActivity(new android.content.Intent(this, AlarmaEditor.class)
                    .putExtra("id", a.id)));
            listaAlarmas.addView(fila, ancho(8));
        }
    }

    private void editar(boolean si) {
        boolean hayPerfil = Agua.meta(this) > 0;
        boolean mostrarForm = si || !hayPerfil;
        formulario.setVisibility(mostrarForm ? View.VISIBLE : View.GONE);
        vineta.setVisibility(mostrarForm ? View.GONE : View.VISIBLE);
        cancelar.setVisibility(hayPerfil ? View.VISIBLE : View.GONE);
        if (mostrarForm && hayPerfil) {
            peso.setText(limpio(Agua.peso(this)));
            estatura.setText(String.valueOf(Agua.estatura(this)));
            peso.requestFocus();
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(FONDO);
        getWindow().setNavigationBarColor(FONDO);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(px(20), px(28), px(20), px(28));

        ImageView pet = new ImageView(this);
        Skins.poner(this, pet, 'W');
        petArriba = pet;
        col.addView(pet, new LinearLayout.LayoutParams(px(110), px(86)));

        hola = texto(Frases.saludo(this), 30, CLARO);
        hola.setGravity(Gravity.CENTER);
        hola.setOnClickListener(v -> pedirNombre(false));
        col.addView(hola, ancho(8));
        TextView cambiar = texto("Toca tu nombre para cambiarlo ✎", 12, NARANJA);
        cambiar.setGravity(Gravity.CENTER);
        cambiar.setOnClickListener(v -> pedirNombre(false));
        col.addView(cambiar, ancho(0));

        TextView ayuda = texto("Para agregarme: mantén presionada la pantalla de inicio, toca Widgets y busca \"Widget by Claude\".\n\n"
                + "Todos los días a las 9:00 te mando una frase, una idea de juego y tips de Roblox, Blender y programación.", 15, GRIS);
        ayuda.setGravity(Gravity.CENTER);
        col.addView(ayuda, ancho(10));

        // Skins de la mascota.
        LinearLayout skins = tarjeta(col, "🎨 Skin de tu mascota");
        skins.addView(texto("Toca una para cambiar al monito del widget. Mantén presionada tu mascota para editarla.", 13, GRIS), ancho(2));
        android.widget.HorizontalScrollView hsk = new android.widget.HorizontalScrollView(this);
        hsk.setHorizontalScrollBarEnabled(false);
        filaSkins = new LinearLayout(this);
        hsk.addView(filaSkins);
        skins.addView(hsk, ancho(10));
        pintarSkins();

        // Viñeta guardada: vaso de agua + datos. Al tocarla se abre el formulario para modificar.
        vineta = new LinearLayout(this);
        vineta.setOrientation(LinearLayout.VERTICAL);
        vineta.setPadding(px(16), px(16), px(16), px(16));
        vineta.setBackground(fondo(TARJETA, 22));
        LinearLayout cabecera = new LinearLayout(this);
        cabecera.setOrientation(LinearLayout.HORIZONTAL);
        cabecera.setGravity(Gravity.CENTER_VERTICAL);
        cabecera.setBackground(fondo(0xFF2E2926, 16));
        cabecera.setPadding(px(12), px(12), px(12), px(12));
        ImageView icono = new ImageView(this);
        icono.setImageResource(R.drawable.vaso);
        cabecera.addView(icono, new LinearLayout.LayoutParams(px(46), px(56)));
        LinearLayout datos = new LinearLayout(this);
        datos.setOrientation(LinearLayout.VERTICAL);
        datos.setPadding(px(14), 0, 0, 0);
        datos.addView(texto("Tu agua del día", 13, GRIS));
        resumenMeta = texto("", 22, CLARO);
        datos.addView(resumenMeta);
        resumenDatos = texto("", 14, CLARO);
        datos.addView(resumenDatos);
        meta = texto("", 12, GRIS);
        datos.addView(meta, ancho(4));
        TextView editar = texto("Toca para modificar ✎", 12, NARANJA);
        datos.addView(editar, ancho(4));
        cabecera.addView(datos, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cabecera.setOnClickListener(v -> editar(true));
        vineta.addView(cabecera);

        // Hoy: progreso, vasos y deshacer, dentro de la misma viñeta.
        LinearLayout hoy = vineta;
        hoy.addView(texto("Hoy", 15, GRIS), ancho(16));
        progreso = texto("", 22, CLARO);
        hoy.addView(progreso, ancho(2));
        barra = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        barra.setMax(1000);
        barra.setProgressTintList(android.content.res.ColorStateList.valueOf(AZUL));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(14));
        lb.topMargin = px(6);
        hoy.addView(barra, lb);

        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        Button vaso = boton("+ Vaso 250 ml", AZUL);
        vaso.setOnClickListener(v -> sumar(250));
        Button botella = boton("+ Botella 500 ml", AZUL);
        botella.setOnClickListener(v -> sumar(500));
        LinearLayout.LayoutParams mitad = new LinearLayout.LayoutParams(0, px(48), 1f);
        mitad.setMargins(0, 0, px(6), 0);
        fila.addView(vaso, mitad);
        LinearLayout.LayoutParams mitad2 = new LinearLayout.LayoutParams(0, px(48), 1f);
        mitad2.setMargins(px(6), 0, 0, 0);
        fila.addView(botella, mitad2);
        hoy.addView(fila, ancho(12));

        Button deshacer = boton("Deshacer último vaso", 0xFF3A3431);
        deshacer.setOnClickListener(v -> sumar(-250));
        hoy.addView(deshacer, ancho(8));

        Button reiniciar = boton("Reiniciar el contador de hoy", 0xFF3A3431);
        reiniciar.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setMessage("¿Dejar el agua de hoy en 0?")
                .setPositiveButton("Sí, reiniciar", (dlg, w) -> {
                    Agua.sumar(this, -Agua.hoy(this));
                    ClawdWidget.pintar(this, null);
                    refrescar();
                })
                .setNegativeButton("Cancelar", null)
                .show());
        hoy.addView(reiniciar, ancho(8));

        col.addView(vineta, ancho(20));

        formulario = tarjeta(col, "Tu peso y estatura");
        LinearLayout perfil = formulario;
        perfil.addView(texto("Peso (kg)", 14, GRIS), ancho(6));
        peso = campo("Ej: 60", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        perfil.addView(peso, ancho(4));
        perfil.addView(texto("Estatura (cm)", 14, GRIS), ancho(10));
        estatura = campo("Ej: 170", InputType.TYPE_CLASS_NUMBER);
        perfil.addView(estatura, ancho(4));
        Button guardar = boton("Guardar", NARANJA);
        guardar.setOnClickListener(v -> guardar());
        perfil.addView(guardar, ancho(14));
        cancelar = boton("Cancelar", 0xFF3A3431);
        cancelar.setOnClickListener(v -> editar(false));
        perfil.addView(cancelar, ancho(8));

        // Alarmas: despertar, tareas, lo que sea.
        LinearLayout alarmas = tarjeta(col, "⏰ Alarmas");
        alarmas.addView(texto("Para despertarte, tareas o lo que quieras. Suenan fuerte aunque el teléfono esté en silencio.", 13, GRIS), ancho(4));
        listaAlarmas = new LinearLayout(this);
        listaAlarmas.setOrientation(LinearLayout.VERTICAL);
        alarmas.addView(listaAlarmas, ancho(8));
        Button agregar = boton("+ Nueva alarma", NARANJA);
        agregar.setOnClickListener(v -> nuevaAlarma());
        alarmas.addView(agregar, ancho(12));

        TextView nota = texto("Si pasan 1 h 30 min sin que tomes agua (entre 8:00 y 23:00), Claude se pone insistente. "
                + "A las 2 h 30 min se enoja y deja de hablarte hasta que tomes. "
                + "Cuando aparezca la gota sobre su cabeza, tócala para registrar un vaso.", 13, GRIS);
        col.addView(nota, ancho(16));

        TextView version = texto("Widget by Claude · versión " + Actualizador.versionActual(this) + "   ·   Buscar actualizaciones ⟳", 13, NARANJA);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, px(10), 0, px(10));
        version.setOnClickListener(v -> Actualizador.revisarAhora(this, true));
        col.addView(version, ancho(16));

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(FONDO);
        sv.setFitsSystemWindows(true);
        sv.addView(col);
        setContentView(sv);

        refrescar();
        editar(false);
        pintarAlarmas();

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        ClawdWidget.programar(this);
        Actualizador.revisarAhora(this, false);
        if (Frases.nombre(this).isEmpty()) pedirNombre(true);
    }

    private void guardar() {
        float kg;
        int cm;
        try {
            kg = Float.parseFloat(peso.getText().toString().replace(',', '.'));
            cm = Integer.parseInt(estatura.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Escribe tu peso y tu estatura", Toast.LENGTH_SHORT).show();
            return;
        }
        if (kg < 20 || kg > 250 || cm < 100 || cm > 230) {
            Toast.makeText(this, "Revisa los números: peso en kg y estatura en cm", Toast.LENGTH_SHORT).show();
            return;
        }
        Agua.guardarPerfil(this, kg, cm);
        ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(peso.getWindowToken(), 0);
        refrescar();
        editar(false);
        Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
        ClawdWidget.pintar(this, null);
    }

    private void sumar(int ml) {
        if (Agua.meta(this) <= 0) {
            Toast.makeText(this, "Primero calcula tu meta con tu peso y estatura", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ml > 0) {
            Agua.sumar(this, ml - Agua.VASO);
            ClawdWidget.tomarAgua(this);
        } else {
            Agua.sumar(this, ml);
            ClawdWidget.pintar(this, null);
        }
        refrescar();
    }

    private void refrescar() {
        int m = Agua.meta(this);
        if (m > 0) {
            resumenMeta.setText(Agua.litros(m) + " L al día");
            resumenDatos.setText(limpio(Agua.peso(this)) + " kg · " + Agua.estatura(this) + " cm · unos "
                    + Math.round(m / 250f) + " vasos");
            meta.setText("Cálculo: 35 ml por cada kilo de peso");
            meta.setVisibility(View.VISIBLE);
            progreso.setText("💧 " + Agua.progreso(this));
            barra.setProgress(Math.min(1000, Agua.hoy(this) * 1000 / m));
        } else {
            meta.setVisibility(View.GONE);
            progreso.setText("💧 Calcula tu meta primero");
            barra.setProgress(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (meta != null) refrescar();
        if (listaAlarmas != null) pintarAlarmas();
        if (filaSkins != null) pintarSkins();
        if (petArriba != null) Skins.poner(this, petArriba, 'W');
    }

    private String limpio(float f) {
        return f == Math.round(f) ? String.valueOf(Math.round(f)) : String.valueOf(f);
    }

    private int px(int dp) { return Math.round(dp * d); }

    private LinearLayout.LayoutParams ancho(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(top);
        return lp;
    }

    private TextView texto(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.1f);
        return t;
    }

    private GradientDrawable fondo(int color, int radio) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(radio));
        return g;
    }

    private LinearLayout tarjeta(LinearLayout padre, String titulo) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(px(16), px(16), px(16), px(16));
        t.setBackground(fondo(TARJETA, 16));
        t.addView(texto(titulo, 18, CLARO));
        padre.addView(t, ancho(20));
        return t;
    }

    private EditText campo(String pista, int tipo) {
        EditText e = new EditText(this);
        e.setHint(pista);
        e.setInputType(tipo);
        e.setTextColor(CLARO);
        e.setHintTextColor(0xFF6E625B);
        e.setTextSize(17);
        e.setPadding(px(12), px(10), px(12), px(10));
        e.setBackground(fondo(0xFF342E2B, 10));
        return e;
    }

    private Button boton(String s, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setBackground(fondo(color, 12));
        return b;
    }
}
