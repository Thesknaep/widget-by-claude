package cl.yerko.clawd;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlarmaActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private ImageView pet;
    private int cuadro;
    private String cuadros;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        float d = getResources().getDisplayMetrics().density;
        String etiqueta = getIntent().getStringExtra("etiqueta");
        String hora = getIntent().getStringExtra("hora");
        if (etiqueta == null) etiqueta = "Alarma";
        if (hora == null) hora = "";
        final String et = etiqueta, ho = hora;

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setBackgroundColor(0xFF1A1716);
        int p = Math.round(28 * d);
        col.setPadding(p, p, p, p);

        String sec = "marciano".equals(Skins.actual(this)) ? "1234NNWQJJQN" : "WNQJJQ";
        cuadros = sec;
        pet = new ImageView(this);
        Skins.poner(this, pet, 'W');
        col.addView(pet, new LinearLayout.LayoutParams(Math.round(160 * d), Math.round(124 * d)));

        TextView t = new TextView(this);
        t.setText(ho);
        t.setTextSize(64);
        t.setTextColor(0xFFF5EDE8);
        t.setGravity(Gravity.CENTER);
        col.addView(t);

        TextView e = new TextView(this);
        e.setText(et);
        e.setTextSize(26);
        e.setTextColor(0xFFD77757);
        e.setGravity(Gravity.CENTER);
        col.addView(e);

        TextView f = new TextView(this);
        f.setText(Frases.saludo(this) + ". " + Frases.delDia(this, "PERSONA", 7));
        f.setTextSize(16);
        f.setTextColor(0xFFB3A69E);
        f.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lf = new LinearLayout.LayoutParams(-1, -2);
        lf.topMargin = Math.round(12 * d);
        col.addView(f, lf);

        Button detener = boton("Detener", 0xFFD77757, d);
        detener.setOnClickListener(v -> {
            Alarmas.detener(this);
            finish();
        });
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(-1, Math.round(64 * d));
        l1.topMargin = Math.round(40 * d);
        col.addView(detener, l1);

        Button posponer = boton("Posponer " + Alarmas.POSPONER_MIN + " min", 0xFF3A3431, d);
        posponer.setOnClickListener(v -> {
            Alarmas.posponer(this, getIntent());
            finish();
        });
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-1, Math.round(56 * d));
        l2.topMargin = Math.round(12 * d);
        col.addView(posponer, l2);

        setContentView(col);
        animar();
    }

    private void animar() {
        Skins.poner(this, pet, cuadros.charAt(cuadro++ % cuadros.length()));
        h.postDelayed(this::animar, 180);
    }

    static AlarmaActivity actual;

    static void cerrar() {
        if (actual != null) actual.finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        actual = this;
    }

    @Override
    protected void onDestroy() {
        if (actual == this) actual = null;
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private Button boton(String s, int color, float d) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(18 * d);
        b.setBackground(g);
        return b;
    }
}
