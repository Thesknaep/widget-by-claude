package cl.yerko.clawd;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VinetaActivity extends Activity {
    static final String CAT = "cat";
    static final String PARTES = "partes";

    private String cat;
    private String[] partes;
    private int paso;
    private TextView texto;
    private TextView pista;
    private ImageView pet;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setFinishOnTouchOutside(true);
        float d = getResources().getDisplayMetrics().density;

        cat = getIntent().getStringExtra(CAT);
        partes = getIntent().getStringArrayExtra(PARTES);
        if (cat == null) cat = Frases.CAT_SALUDO;
        if (partes == null || partes.length == 0) partes = new String[]{Frases.saludo(this)};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = (int) (24 * d);
        box.setPadding(p, p, p, p);

        pet = new ImageView(this);
        Skins.poner(this, pet, 'W');
        box.addView(pet, new LinearLayout.LayoutParams((int) (72 * d), (int) (56 * d)));

        texto = new TextView(this);
        texto.setTextColor(0xFF3A2A22);
        texto.setTextSize(19);
        texto.setGravity(Gravity.CENTER);
        texto.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * d);
        box.addView(texto, lp);

        pista = new TextView(this);
        pista.setTextColor(0xFF9A8A80);
        pista.setTextSize(13);
        pista.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = (int) (14 * d);
        box.addView(pista, lp2);

        pet.setOnClickListener(v -> seguir());
        texto.setOnClickListener(v -> seguir());
        setContentView(box);
        mostrar();
    }

    private void mostrar() {
        String t = paso == 0 ? Frases.prefijo(cat) + partes[0] : partes[paso];
        texto.setText(t);
        boolean quedan = paso < partes.length - 1;
        if (Frases.CAT_SALUDO.equals(cat)) {
            pista.setText("Toca a Claude para una idea de juego");
        } else if (quedan) {
            pista.setText("Toca a Claude para seguir (" + (paso + 1) + "/" + partes.length + ")");
        } else if ("IDEA".equals(cat)) {
            pista.setText("Toca a Claude para otra idea · toca afuera para cerrar");
        } else {
            pista.setText("Toca a Claude para otro consejo así · toca afuera para cerrar");
        }
        Skins.poner(this, pet, paso % 2 == 0 ? 'W' : 'J');
    }

    private void seguir() {
        if (paso < partes.length - 1) {
            paso++;
        } else {
            String sig = Frases.CAT_SALUDO.equals(cat) ? "IDEA" : cat;
            Frases.Consejo c = Frases.otroDe(this, sig, partes[0]);
            cat = c.cat;
            partes = c.partes;
            paso = 0;
        }
        mostrar();
    }
}
