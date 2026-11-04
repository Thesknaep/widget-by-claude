package cl.yerko.clawd;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

final class Agua {
    static final int OK = 0, SED = 1, ENOJADO = 2;
    static final int VASO = 250;
    private static final long MIN = 60_000L;
    static final long SED_MS = 90 * MIN;
    static final long ENOJO_MS = 150 * MIN;
    private static final int DESPIERTA = 8, DUERME = 23;

    private static SharedPreferences p(Context c) { return ClawdWidget.prefs(c); }

    static float peso(Context c) { return p(c).getFloat("pesoKg", 0f); }
    static int estatura(Context c) { return p(c).getInt("estaturaCm", 0); }
    static int meta(Context c) { return p(c).getInt("metaMl", 0); }

    // Guía general: unos 35 ml por kilo de peso al día, redondeado a 50 ml.
    static int calcularMeta(float kg) {
        return Math.round(kg * 35f / 50f) * 50;
    }

    static void guardarPerfil(Context c, float kg, int cm) {
        p(c).edit().putFloat("pesoKg", kg).putInt("estaturaCm", cm)
                .putInt("metaMl", calcularMeta(kg)).apply();
    }

    static int hoy(Context c) {
        if (p(c).getInt("diaAgua", -1) != Frases.dia()) return 0;
        return p(c).getInt("hoyMl", 0);
    }

    static void sumar(Context c, int ml) {
        int total = Math.max(0, hoy(c) + ml);
        SharedPreferences.Editor e = p(c).edit().putInt("hoyMl", total).putInt("diaAgua", Frases.dia());
        if (ml > 0) e.putLong("ultimoAgua", System.currentTimeMillis());
        e.apply();
    }

    static boolean despierto() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= DESPIERTA && h < DUERME;
    }

    private static long inicioDelDia() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, DESPIERTA);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static long sinTomar(Context c) {
        long ultimo = Math.max(p(c).getLong("ultimoAgua", 0L), inicioDelDia());
        return Math.max(0L, System.currentTimeMillis() - ultimo);
    }

    static int estado(Context c) {
        int meta = meta(c);
        if (meta <= 0 || !despierto() || hoy(c) >= meta) return OK;
        long d = sinTomar(c);
        if (d >= ENOJO_MS) return ENOJADO;
        if (d >= SED_MS) return SED;
        return OK;
    }

    static String litros(int ml) {
        return String.format(Locale.US, "%.1f", ml / 1000f).replace('.', ',');
    }

    static String progreso(Context c) {
        return litros(hoy(c)) + " / " + litros(meta(c)) + " L";
    }

    static String tiempoSinTomar(Context c) {
        long m = sinTomar(c) / MIN;
        if (m < 60) return m + " min";
        long h = m / 60, r = m % 60;
        return r == 0 ? h + " h" : h + " h " + r + " min";
    }
}
