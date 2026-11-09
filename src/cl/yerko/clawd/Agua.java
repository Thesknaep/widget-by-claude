package cl.yerko.clawd;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

final class Agua {
    static final int OK = 0, SED = 1, ENOJADO = 2;
    static final int VASO = 250;
    private static final long MIN = 60_000L;
    // Si la mascota pidió agua y pasa esta hora sin que tomes, se enoja.
    static final long ENOJO_MS = 60 * MIN;
    private static final int DESPIERTA = 8, DUERME = 23;
    // Primer y último recordatorio del día (minutos desde medianoche): 8:30 y 22:00.
    private static final int PRIMERO = 8 * 60 + 30, ULTIMO = 22 * 60;
    static final String HOMBRE = "H", MUJER = "M";

    private static SharedPreferences p(Context c) { return ClawdWidget.prefs(c); }

    static float peso(Context c) { return p(c).getFloat("pesoKg", 0f); }
    static int estatura(Context c) { return p(c).getInt("estaturaCm", 0); }
    static String sexo(Context c) { return p(c).getString("sexo", ""); }
    static int meta(Context c) { return p(c).getInt("metaMl", 0); }

    /**
     * Guía general (no es indicación médica):
     * - 35 ml por kilo en hombres y 31 ml por kilo en mujeres (las referencias de consumo
     *   para mujeres son cerca de un 20 % menores).
     * - La estatura y el sexo dan el peso ideal (fórmula de Devine). Si el peso pasa bastante
     *   de ese ideal, se usa un peso ajustado para que la meta no salga exagerada.
     * - Se limita a un rango razonable y se redondea a 50 ml.
     */
    static int calcularMeta(float kg, int cm, String sexo) {
        boolean mujer = MUJER.equals(sexo);
        double ideal = (mujer ? 45.5 : 50) + 0.91 * (cm - 152.4);
        double usado = kg;
        if (ideal > 30 && kg > ideal * 1.2) usado = ideal + 0.4 * (kg - ideal);
        double ml = usado * (mujer ? 31 : 35);
        ml = Math.max(mujer ? 1500 : 1800, Math.min(mujer ? 3000 : 3700, ml));
        return (int) Math.round(ml / 50) * 50;
    }

    static void guardarPerfil(Context c, float kg, int cm, String sexo) {
        p(c).edit().putFloat("pesoKg", kg).putInt("estaturaCm", cm).putString("sexo", sexo)
                .putInt("metaMl", calcularMeta(kg, cm, sexo)).apply();
    }

    static int vasos(Context c) {
        return Math.max(1, (int) Math.ceil(meta(c) / (double) VASO));
    }

    /** Horas del día (minutos desde medianoche) en que la mascota pide agua, repartidas parejo. */
    static int[] horario(Context c) {
        int n = vasos(c);
        int[] h = new int[n];
        for (int i = 0; i < n; i++)
            h[i] = n == 1 ? PRIMERO : PRIMERO + Math.round((ULTIMO - PRIMERO) * i / (float) (n - 1));
        return h;
    }

    static String hora(int minutos) {
        return String.format(Locale.US, "%d:%02d", minutos / 60, minutos % 60);
    }

    static String horarioTexto(Context c) {
        StringBuilder s = new StringBuilder();
        for (int m : horario(c)) s.append(s.length() == 0 ? "" : " · ").append(hora(m));
        return s.toString();
    }

    private static int minutoActual() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** Cuántos vasos ya te tocaban a esta hora según el horario. */
    static int vasosQueTocan(Context c) {
        int ahora = minutoActual(), n = 0;
        for (int m : horario(c)) if (m <= ahora) n++;
        return n;
    }

    /** ¿Vas atrasado respecto del horario? (lo que deberías llevar tomado a esta hora). */
    static boolean atrasado(Context c) {
        if (meta(c) <= 0) return false;
        int deberia = Math.min(meta(c), vasosQueTocan(c) * VASO);
        return hoy(c) < deberia;
    }

    /** Momento (milisegundos) del próximo recordatorio del horario. */
    static long proximoRecordatorio(Context c) {
        int ahora = minutoActual();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int[] h = horario(c);
        for (int m : h) {
            if (m > ahora) {
                cal.set(Calendar.HOUR_OF_DAY, m / 60);
                cal.set(Calendar.MINUTE, m % 60);
                return cal.getTimeInMillis();
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, h[0] / 60);
        cal.set(Calendar.MINUTE, h[0] % 60);
        return cal.getTimeInMillis();
    }

    static void marcarPedido(Context c) {
        p(c).edit().putLong("pidioAgua", System.currentTimeMillis()).apply();
    }

    /** La mascota pidió agua y todavía no tomas desde entonces (solo cuenta el pedido de hoy). */
    static boolean pedidoPendiente(Context c) {
        long pidio = p(c).getLong("pidioAgua", 0L);
        return pidio >= inicioDelDia() && pidio > p(c).getLong("ultimoAgua", 0L);
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
        if (meta <= 0 || !despierto() || hoy(c) >= meta || !pedidoPendiente(c)) return OK;
        long pidio = p(c).getLong("pidioAgua", 0L);
        return System.currentTimeMillis() - pidio >= ENOJO_MS ? ENOJADO : SED;
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
