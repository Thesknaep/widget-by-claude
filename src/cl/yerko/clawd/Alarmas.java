package cl.yerko.clawd;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class Alarmas {
    static final String SONAR = "cl.yerko.clawd.ALARMA";
    static final String DETENER = "cl.yerko.clawd.DETENER";
    static final String POSPONER = "cl.yerko.clawd.POSPONER";
    static final int NOTI = 50;
    private static final int BASE = 2000, COD_POSPUESTA = 2999, MAX = 60;
    static final int POSPONER_MIN = 10;
    static final String[] DIAS = {"L", "M", "X", "J", "V", "S", "D"};
    static final int DIARIO = 0b1111111, LUN_VIE = 0b0011111, FINDE = 0b1100000;

    static final class Alarma {
        int id = -1, h = 7, m = 0, dias = 0;
        String etiqueta = "Alarma";
        String tono = Tonos.DEFECTO;
        boolean activa = true, vibrar = true, borrar = false;

        String hora() { return formato12(h, m); }

        String repeticion() {
            if (dias == 0) return "Una vez";
            if (dias == DIARIO) return "Diariamente";
            if (dias == LUN_VIE) return "Lunes a viernes";
            if (dias == FINDE) return "Fin de semana";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 7; i++) if ((dias & (1 << i)) != 0) sb.append(DIAS[i]).append(' ');
            return sb.toString().trim();
        }
    }

    static String formato12(int h, int m) {
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, m, h < 12 ? "a. m." : "p. m.");
    }

    static List<Alarma> lista(Context c) {
        List<Alarma> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(ClawdWidget.prefs(c).getString("alarmasJson", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Alarma al = new Alarma();
                al.id = o.getInt("id");
                al.h = o.getInt("h");
                al.m = o.getInt("m");
                al.dias = o.optInt("dias", 0);
                al.etiqueta = o.optString("etiqueta", "Alarma");
                al.activa = o.optBoolean("activa", true);
                al.tono = o.optString("tono", Tonos.DEFECTO);
                al.vibrar = o.optBoolean("vibrar", true);
                al.borrar = o.optBoolean("borrar", false);
                out.add(al);
            }
        } catch (Exception ignored) {
        }
        Collections.sort(out, (x, y) -> (x.h * 60 + x.m) - (y.h * 60 + y.m));
        return out;
    }

    private static void guardarLista(Context c, List<Alarma> l) {
        JSONArray a = new JSONArray();
        try {
            for (Alarma al : l) {
                a.put(new JSONObject().put("id", al.id).put("h", al.h).put("m", al.m)
                        .put("dias", al.dias).put("etiqueta", al.etiqueta).put("activa", al.activa)
                        .put("tono", al.tono).put("vibrar", al.vibrar).put("borrar", al.borrar));
            }
        } catch (Exception ignored) {
        }
        ClawdWidget.prefs(c).edit().putString("alarmasJson", a.toString()).apply();
        programarTodas(c);
    }

    static Alarma buscar(Context c, int id) {
        for (Alarma a : lista(c)) if (a.id == id) return a;
        return null;
    }

    /** Crea (id = -1) o actualiza una alarma. */
    static boolean guardar(Context c, Alarma nueva) {
        List<Alarma> l = lista(c);
        if (nueva.etiqueta == null || nueva.etiqueta.trim().isEmpty()) nueva.etiqueta = "Alarma";
        nueva.etiqueta = nueva.etiqueta.trim();
        if (nueva.id < 0) {
            if (l.size() >= MAX) return false;
            int id = 0;
            for (Alarma a : l) id = Math.max(id, a.id + 1);
            nueva.id = id;
            l.add(nueva);
        } else {
            for (int i = 0; i < l.size(); i++) if (l.get(i).id == nueva.id) l.set(i, nueva);
            cancelar(c, nueva.id);
        }
        nueva.activa = true;
        guardarLista(c, l);
        return true;
    }

    static void quitar(Context c, int id) {
        List<Alarma> l = lista(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).id == id) l.remove(i);
        cancelar(c, id);
        guardarLista(c, l);
    }

    static void activar(Context c, int id, boolean si) {
        List<Alarma> l = lista(c);
        for (Alarma a : l) if (a.id == id) a.activa = si;
        if (!si) cancelar(c, id);
        guardarLista(c, l);
    }

    static long proxima(Alarma a, long desde) {
        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(desde);
        for (int k = 0; k <= 7; k++) {
            Calendar cal = (Calendar) base.clone();
            cal.add(Calendar.DAY_OF_YEAR, k);
            cal.set(Calendar.HOUR_OF_DAY, a.h);
            cal.set(Calendar.MINUTE, a.m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= desde) continue;
            int idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if (a.dias == 0 || (a.dias & (1 << idx)) != 0) return cal.getTimeInMillis();
        }
        return -1;
    }

    // "Sonará en 23 horas con 58 minutos", "Sonará en 1 hora con 20 minutos", "Sonará en 45 minutos"
    static String faltaLargo(long t) {
        if (t <= 0) return "";
        long min = Math.max(1, (t - System.currentTimeMillis() + 59_999) / 60_000);
        long d = min / 1440, h = (min % 1440) / 60, m = min % 60;
        StringBuilder sb = new StringBuilder("Sonará en ");
        if (d > 0) {
            sb.append(d).append(d == 1 ? " día" : " días");
            if (h > 0) sb.append(" y ").append(h).append(h == 1 ? " hora" : " horas");
            return sb.toString();
        }
        if (h > 0) {
            sb.append(h).append(h == 1 ? " hora" : " horas");
            if (m > 0) sb.append(" con ").append(m).append(m == 1 ? " minuto" : " minutos");
            return sb.toString();
        }
        return sb.append(m).append(m == 1 ? " minuto" : " minutos").toString();
    }

    private static PendingIntent piSonar(Context c, int codigo, Intent extras) {
        Intent in = new Intent(c, ClawdWidget.class).setAction(SONAR);
        if (extras != null) in.putExtras(extras);
        return PendingIntent.getBroadcast(c, codigo, in,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Intent extras(Alarma a) {
        return new Intent().putExtra("id", a.id).putExtra("etiqueta", a.etiqueta)
                .putExtra("hora", a.hora()).putExtra("tono", a.tono).putExtra("vibrar", a.vibrar);
    }

    private static void cancelar(Context c, int id) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(piSonar(c, BASE + id, null));
    }

    private static void poner(Context c, long t, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ver = PendingIntent.getActivity(c, 3000, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        if (am.canScheduleExactAlarms()) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, ver), pi);
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, t, 60_000L, pi);
        }
    }

    static void programarTodas(Context c) {
        long ahora = System.currentTimeMillis();
        for (Alarma a : lista(c)) {
            if (!a.activa) {
                cancelar(c, a.id);
                continue;
            }
            long t = proxima(a, ahora);
            if (t > 0) poner(c, t, piSonar(c, BASE + a.id, extras(a)));
        }
    }

    static void sonar(Context c, Intent in) {
        int id = in.getIntExtra("id", -1);
        if (id >= 0) {
            Alarma a = buscar(c, id);
            if (a != null && a.borrar) quitar(c, id);
            else if (a != null && a.dias == 0) activar(c, id, false);
            else programarTodas(c);
        }
        Intent s = new Intent(c, AlarmaService.class).putExtras(in);
        if (s.getStringExtra("etiqueta") == null) s.putExtra("etiqueta", "Alarma");
        if (s.getStringExtra("hora") == null) s.putExtra("hora", "");
        c.startForegroundService(s);
    }

    static void detener(Context c) {
        c.stopService(new Intent(c, AlarmaService.class));
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTI);
        AlarmaActivity.cerrar();
    }

    static void posponer(Context c, Intent in) {
        detener(c);
        long t = System.currentTimeMillis() + POSPONER_MIN * 60_000L;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        Intent e = new Intent().putExtras(in).putExtra("id", -1)
                .putExtra("hora", formato12(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        poner(c, t, piSonar(c, COD_POSPUESTA, e));
    }
}
