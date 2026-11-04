package cl.yerko.clawd;

import android.content.Context;

/** Skins de la mascota. Cada letra de una secuencia es un cuadro de animación. */
final class Skins {
    static final String[] IDS = {"claude", "gatita", "marciano", "gatito", "robotin", "slime"};
    static final String[] NOMBRES = {"Claude", "Mi gatita", "Marcianito piu piu", "Gatito", "Robotín", "Slime galáctico"};

    // N normal, B parpadeo, W saludo, S paso, Q agacharse, J salto, A/C/D enojado, 1-4 disparo.
    private static final String NORMAL =
            "NNNNNNNNBNNNNN" + "SNSNSNNNNBNNNN" + "WNWNWNNNNNBBNN" + "QJJJJQNNNNNNBN" + "SNSNNNNNNNNN";
    private static final String MARCIANO =
            "NNNNNNNNBNNNNN" + "1234NN1234NNNN" + "NNBNNNWNWNNNNN" + "QJJJJQNNNN1234" + "N1234NNNBNNNNN";
    static final String SED = "WNWNWNQJJJQNWNWNBNQJJJQN";
    static final String ENOJO = "AAACADAAAACDCAAAAA";

    static String actual(Context c) {
        String s = ClawdWidget.prefs(c).getString("skin", "claude");
        for (String id : IDS) if (id.equals(s)) return s;
        if (s.startsWith("p:") && Mascota.buscar(c, s.substring(2)) != null) return s;
        return "claude";
    }

    static void elegir(Context c, String id) {
        ClawdWidget.prefs(c).edit().putString("skin", id).apply();
    }

    static String secuencia(String skin) {
        return "marciano".equals(skin) ? MARCIANO : NORMAL;
    }

    static int inicioSalto(String sec) {
        int i = sec.indexOf("QJ");
        return Math.max(0, i);
    }

    static int cuadro(Context c, String skin, char pose) {
        String nombre;
        if ("claude".equals(skin)) {
            switch (pose) {
                case 'B': nombre = "pose_blink"; break;
                case 'W': case '1': case '3': nombre = "pose_wave"; break;
                case 'S': case '2': case '4': nombre = "pose_step"; break;
                case 'Q': nombre = "pose_squash"; break;
                case 'J': nombre = "pose_jump"; break;
                case 'A': nombre = "pose_angry"; break;
                case 'C': nombre = "pose_angry2"; break;
                case 'D': nombre = "pose_angry3"; break;
                default: nombre = "pose_normal";
            }
        } else {
            char p = Character.toLowerCase(pose);
            if ((p >= '1' && p <= '4') && !"marciano".equals(skin)) p = (p == '1' || p == '3') ? 'w' : 's';
            nombre = "s_" + skin + "_" + p;
        }
        int id = c.getResources().getIdentifier(nombre, "drawable", c.getPackageName());
        return id != 0 ? id : R.drawable.pose_normal;
    }

    static int cuadro(Context c, char pose) { return cuadro(c, actual(c), pose); }

    static boolean propia(String skin) { return skin.startsWith("p:"); }

    /** Pone el cuadro en un ImageView (sirve para skins normales y mascotas propias). */
    static void poner(Context c, android.widget.ImageView iv, String skin, char pose) {
        if (propia(skin)) iv.setImageBitmap(Mascota.cuadro(c, skin.substring(2), pose));
        else iv.setImageResource(cuadro(c, skin, pose));
    }

    static void poner(Context c, android.widget.ImageView iv, char pose) { poner(c, iv, actual(c), pose); }

    /** Igual, pero para el widget. */
    static void poner(Context c, android.widget.RemoteViews rv, int vista, String skin, char pose) {
        if (propia(skin)) rv.setImageViewBitmap(vista, Mascota.cuadro(c, skin.substring(2), pose));
        else rv.setImageViewResource(vista, cuadro(c, skin, pose));
    }
}
