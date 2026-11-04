package cl.yerko.clawd;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Busca versiones nuevas y contenido nuevo en GitHub.
 * version.json:  {"versionCode": 19, "versionName": "19.0", "apk": "https://...", "notas": "..."}
 */
final class Actualizador {
    static final String REPO = "Thesknaep/widget-by-claude";
    static final String RAW = "https://raw.githubusercontent.com/" + REPO + "/main/";
    static final String URL_VERSION = RAW + "version.json";
    static final String URL_CONSEJOS = RAW + "contenido/consejos.txt";
    static final String ESTADO = "cl.yerko.clawd.INSTALAR_ESTADO";
    private static final String CANAL = "claude_actualizacion";
    private static final long CADA = 6 * 60 * 60_000L;

    static final class Version {
        int code;
        String name, apk, notas;
    }

    static int versionActual(Context c) {
        try {
            return (int) c.getPackageManager().getPackageInfo(c.getPackageName(), 0).getLongVersionCode();
        } catch (Exception e) {
            return 0;
        }
    }

    private static byte[] bajar(String url, int maxBytes) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url + (url.contains("?") ? "&" : "?") + "t="
                + System.currentTimeMillis() / 60_000).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(30_000);
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("Cache-Control", "no-cache");
        if (con.getResponseCode() != 200) throw new Exception("HTTP " + con.getResponseCode());
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > maxBytes) throw new Exception("muy grande");
            }
            return out.toByteArray();
        } finally {
            con.disconnect();
        }
    }

    static Version consultar() {
        try {
            JSONObject o = new JSONObject(new String(bajar(URL_VERSION, 64_000), "UTF-8"));
            Version v = new Version();
            v.code = o.getInt("versionCode");
            v.name = o.optString("versionName", String.valueOf(v.code));
            v.apk = o.getString("apk");
            v.notas = o.optString("notas", "");
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    /** Descarga los consejos nuevos (si cambiaron) para que todos los vean sin reinstalar. */
    static boolean actualizarConsejos(Context c) {
        try {
            byte[] b = bajar(URL_CONSEJOS, 2_000_000);
            String s = new String(b, "UTF-8");
            if (!s.contains("|")) return false;
            File f = Frases.archivoRemoto(c);
            if (f.exists() && f.length() == b.length) return false;
            try (OutputStream o = new FileOutputStream(f)) { o.write(b); }
            Frases.olvidarCache();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Revisión en segundo plano (como mucho cada 6 horas): consejos + aviso de versión nueva. */
    static void revisarEnFondo(Context ctx) {
        final Context c = ctx.getApplicationContext();
        long ultima = ClawdWidget.prefs(c).getLong("ultimaRevision", 0L);
        if (System.currentTimeMillis() - ultima < CADA) return;
        ClawdWidget.prefs(c).edit().putLong("ultimaRevision", System.currentTimeMillis()).apply();
        new Thread(() -> {
            if (actualizarConsejos(c)) ClawdWidget.pintar(c, null);
            Version v = consultar();
            if (v != null && v.code > versionActual(c)
                    && ClawdWidget.prefs(c).getInt("avisada", 0) < v.code) {
                ClawdWidget.prefs(c).edit().putInt("avisada", v.code).apply();
                notificar(c, v);
            }
        }).start();
    }

    private static void notificar(Context c, Version v) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CANAL, "Actualizaciones",
                NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent abrir = PendingIntent.getActivity(c, 4000,
                new Intent(c, MainActivity.class).putExtra("actualizar", true),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        nm.notify(60, new Notification.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_stat)
                .setColor(0xFFD77757)
                .setContentTitle("🎉 Hay una versión nueva de Widget by Claude")
                .setContentText(v.notas.isEmpty() ? "Toca para actualizar" : v.notas)
                .setStyle(new Notification.BigTextStyle().bigText(
                        (v.notas.isEmpty() ? "" : v.notas + "\n\n") + "Toca para actualizar a la versión " + v.name))
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .build());
    }

    /** Desde la app: revisa ahora y, si hay versión nueva, pregunta si actualizar. */
    static void revisarAhora(Activity a, boolean avisarSiNoHay) {
        Handler h = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            actualizarConsejos(a);
            Version v = consultar();
            h.post(() -> {
                if (a.isFinishing()) return;
                if (v == null) {
                    if (avisarSiNoHay) Toast.makeText(a, "No pude revisar. ¿Tienes internet?", Toast.LENGTH_SHORT).show();
                } else if (v.code > versionActual(a)) {
                    preguntar(a, v);
                } else if (avisarSiNoHay) {
                    Toast.makeText(a, "Ya tienes la última versión ✅", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private static void preguntar(Activity a, Version v) {
        new AlertDialog.Builder(a, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("🎉 ¡Versión nueva " + v.name + "!")
                .setMessage((v.notas.isEmpty() ? "" : v.notas + "\n\n")
                        + "Tus datos (nombre, agua, alarmas y mascotas) se mantienen.")
                .setPositiveButton("Actualizar", (d, w) -> descargarEInstalar(a, v))
                .setNegativeButton("Después", null)
                .show();
    }

    static void descargarEInstalar(Activity a, Version v) {
        if (!a.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(a, "Activa \"Permitir de esta fuente\" y vuelve a tocar Actualizar", Toast.LENGTH_LONG).show();
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + a.getPackageName())));
            return;
        }
        Toast.makeText(a, "Descargando la versión " + v.name + "…", Toast.LENGTH_LONG).show();
        Context c = a.getApplicationContext();
        Handler h = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                byte[] apk = bajar(v.apk, 60_000_000);
                PackageInstaller pi = c.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                sp.setAppPackageName(c.getPackageName());
                int id = pi.createSession(sp);
                try (PackageInstaller.Session s = pi.openSession(id)) {
                    try (OutputStream o = s.openWrite("app.apk", 0, apk.length)) {
                        o.write(apk);
                        s.fsync(o);
                    }
                    Intent estado = new Intent(c, ClawdWidget.class).setAction(ESTADO);
                    PendingIntent cb = PendingIntent.getBroadcast(c, 4001, estado,
                            PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                    s.commit(cb.getIntentSender());
                }
            } catch (Exception e) {
                h.post(() -> Toast.makeText(c, "No se pudo descargar la actualización", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** Respuesta del instalador del sistema. */
    static void estado(Context c, Intent in) {
        int st = in.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmar = in.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmar != null) {
                confirmar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(confirmar);
            }
        } else if (st != PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(c, "No se pudo instalar la actualización", Toast.LENGTH_LONG).show();
        }
    }
}
