package cl.yerko.clawd;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class ClawdWidget extends AppWidgetProvider {
    static final String TOCAR = "cl.yerko.clawd.TOCAR";
    static final String DIARIO = "cl.yerko.clawd.DIARIO";
    static final String AGUA = "cl.yerko.clawd.AGUA";
    static final String PEDIR_AGUA = "cl.yerko.clawd.PEDIR_AGUA";
    static final String REVISAR = "cl.yerko.clawd.REVISAR";
    static final String ITEM = "cl.yerko.clawd.ITEM";
    private static final String CANAL = "claude_diario";
    // Android no deja cambiar el sonido de un canal ya creado: el canal con el sonido del vaso es uno nuevo.
    private static final String CANAL_AGUA = "claude_agua_vaso";
    private static final String CANAL_AGUA_VIEJO = "claude_agua";
    private static final Random RND = new Random();

    private static final String[] INSISTE = {
        "%n, ¿tomaste agua? Toca la gota cuando tomes",
        "¡Agua, %n! Llevas %s sin tomar",
        "Un vaso de agua ahora mismo, porfa",
        "Tu cerebro programa mejor hidratado. ¡Toma agua!",
        "Hey bro, la gota no se va a tomar sola",
        "Vas en %p. ¡Falta agua!",
        "Me voy a enojar si no tomas agua...",
    };

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        pintar(ctx, null);
        programar(ctx);
    }

    @Override
    public void onEnabled(Context ctx) {
        programar(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (TOCAR.equals(a)) {
            AppWidgetManager m = AppWidgetManager.getInstance(ctx);
            int[] ids = ids(ctx);
            if (ids.length == 0) return;
            int total = prefs(ctx).getInt("total", 1);
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget);
            rv.setDisplayedChild(R.id.pet, Agua.estado(ctx) == Agua.OK
                    ? Skins.inicioSalto(Skins.secuencia(Skins.actual(ctx))) : 0);
            rv.setDisplayedChild(R.id.texts, RND.nextInt(total));
            rv.setDisplayedChild(R.id.items, RND.nextInt(Math.max(1, total)));
            m.partiallyUpdateAppWidget(ids, rv);
        } else if (ITEM.equals(a)) {
            // Tocaste el ítem que tiene la mascota en la mano.
            pintar(ctx, Items.tocar(ctx, intent.getIntExtra("item", 3)));
        } else if (Alarmas.SONAR.equals(a)) {
            Alarmas.sonar(ctx, intent);
        } else if (Alarmas.DETENER.equals(a)) {
            Alarmas.detener(ctx);
        } else if (Alarmas.POSPONER.equals(a)) {
            Alarmas.posponer(ctx, intent);
        } else if (Actualizador.ESTADO.equals(a)) {
            Actualizador.estado(ctx, intent);
        } else if (AGUA.equals(a)) {
            tomarAgua(ctx);
        } else if (PEDIR_AGUA.equals(a)) {
            // Hora del horario: si vas atrasado, la mascota te pide agua (con el sonido del vaso).
            if (Agua.despierto() && Agua.atrasado(ctx)) {
                Agua.marcarPedido(ctx);
                prefs(ctx).edit().putInt("ultimoEstado", Agua.estado(ctx)).apply();
                avisarAgua(ctx);
                pintar(ctx, null);
            }
            programarAgua(ctx);
        } else if (REVISAR.equals(a)) {
            Actualizador.revisarEnFondo(ctx);
            int antes = prefs(ctx).getInt("ultimoEstado", Agua.OK);
            int ahora = Agua.estado(ctx);
            prefs(ctx).edit().putInt("ultimoEstado", ahora).apply();
            if (ahora != antes || ahora != Agua.OK) pintar(ctx, null);
        } else if (DIARIO.equals(a)) {
            Actualizador.revisarEnFondo(ctx);
            notificar(ctx, Frases.delDia(ctx, "PERSONA", 7));
            pintar(ctx, null);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)
                || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(a)) {
            programar(ctx);
        }
    }

    static void tomarAgua(Context ctx) {
        Agua.sumar(ctx, Agua.VASO);
        prefs(ctx).edit().putInt("ultimoEstado", Agua.OK).apply();
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(2);
        String msg = "¡Bien " + Frases.trato(ctx) + "! Gracias por tomar agua";
        pintar(ctx, msg);
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("claude", Context.MODE_PRIVATE);
    }

    static int[] ids(Context ctx) {
        int[] ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(new ComponentName(ctx, ClawdWidget.class));
        return ids == null ? new int[0] : ids;
    }

    private static PendingIntent difundir(Context ctx, String accion, int codigo) {
        Intent i = new Intent(ctx, ClawdWidget.class).setAction(accion);
        return PendingIntent.getBroadcast(ctx, codigo, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static RemoteViews burbuja(Context ctx, String pkg, int k, String texto, String cat, String[] partes) {
        RemoteViews t = new RemoteViews(pkg, R.layout.item_text);
        t.setTextViewText(R.id.txt, texto);
        Intent v = new Intent(ctx, VinetaActivity.class)
                .putExtra(VinetaActivity.CAT, cat)
                .putExtra(VinetaActivity.PARTES, partes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        t.setOnClickPendingIntent(R.id.txt, PendingIntent.getActivity(ctx, 100 + k, v,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return t;
    }

    static void pintar(Context ctx, String primero) {
        int[] ids = ids(ctx);
        if (ids.length == 0) return;
        String pkg = ctx.getPackageName();
        RemoteViews rv = new RemoteViews(pkg, R.layout.widget);
        int estado = primero != null ? Agua.OK : Agua.estado(ctx);

        String skin = Skins.actual(ctx);
        String secuencia = estado == Agua.ENOJADO ? Skins.ENOJO
                : estado == Agua.SED ? Skins.SED : Skins.secuencia(skin);
        rv.removeAllViews(R.id.pet);
        for (char f : secuencia.toCharArray()) {
            RemoteViews fr = new RemoteViews(pkg, R.layout.item_frame);
            Skins.poner(ctx, fr, R.id.frame, skin, f);
            rv.addView(R.id.pet, fr);
        }

        rv.removeAllViews(R.id.texts);
        int total = 0;
        if (estado == Agua.SED) {
            for (String s : INSISTE) {
                String t = s.replace("%s", Agua.tiempoSinTomar(ctx)).replace("%p", Agua.progreso(ctx))
                        .replace("%n", Frases.trato(ctx));
                rv.addView(R.id.texts, burbuja(ctx, pkg, total++, t, "AGUA", new String[]{t}));
            }
        } else if (estado == Agua.OK) {
            if (primero != null) {
                rv.addView(R.id.texts, burbuja(ctx, pkg, total++, primero, "AGUA", new String[]{primero}));
            }
            ArrayList<Frases.Consejo> frases = Frases.lista(ctx);
            for (Frases.Consejo c : frases) {
                rv.addView(R.id.texts, burbuja(ctx, pkg, total++, c.texto(), c.cat, c.partes));
            }
        }
        rv.setViewVisibility(R.id.texts, estado == Agua.ENOJADO ? View.GONE : View.VISIBLE);
        prefs(ctx).edit().putInt("total", Math.max(1, total)).apply();

        rv.removeAllViews(R.id.items);
        if (estado == Agua.OK) {
            int prev = -1;
            for (int k = 0; k < Math.max(1, total); k++) {
                int it;
                do { it = RND.nextInt(Items.IDS.length); } while (it == prev);
                prev = it;
                RemoteViews fr = new RemoteViews(pkg, R.layout.item_frame);
                fr.setImageViewResource(R.id.frame, Items.drawable(ctx, it));
                Intent ti = new Intent(ctx, ClawdWidget.class).setAction(ITEM).putExtra("item", it);
                fr.setOnClickPendingIntent(R.id.frame, PendingIntent.getBroadcast(ctx, 5000 + k, ti,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
                rv.addView(R.id.items, fr);
            }
        }
        rv.setViewVisibility(R.id.items, estado == Agua.OK ? View.VISIBLE : View.GONE);
        rv.setViewVisibility(R.id.drop, estado == Agua.OK ? View.GONE : View.VISIBLE);

        rv.setTextViewText(R.id.label, "Claude");

        rv.setOnClickPendingIntent(R.id.root, difundir(ctx, TOCAR, 1));
        PendingIntent agua = difundir(ctx, AGUA, 3);
        rv.setOnClickPendingIntent(R.id.drop, agua);
        AppWidgetManager.getInstance(ctx).updateAppWidget(ids, rv);
    }

    static void programar(Context ctx) {
        Alarmas.programarTodas(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, difundir(ctx, DIARIO, 2));
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, difundir(ctx, REVISAR, 4));
        programarAgua(ctx);
    }

    /** Programa el próximo recordatorio de agua del horario (despierta al teléfono si está en reposo). */
    static void programarAgua(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = difundir(ctx, PEDIR_AGUA, 6);
        if (Agua.meta(ctx) <= 0) {
            am.cancel(pi);
            return;
        }
        long t = Agua.proximoRecordatorio(ctx);
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }

    private static boolean puedeNotificar(Context ctx) {
        return Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    static void avisarAgua(Context ctx) {
        if (!puedeNotificar(ctx)) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.deleteNotificationChannel(CANAL_AGUA_VIEJO);
        NotificationChannel canal = new NotificationChannel(CANAL_AGUA, "Recordatorio de agua",
                NotificationManager.IMPORTANCE_HIGH);
        canal.setSound(android.net.Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.agua_vaso),
                new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
        nm.createNotificationChannel(canal);
        Notification.Action tome = new Notification.Action.Builder(
                Icon.createWithResource(ctx, R.drawable.agua), "Ya tomé (+250 ml)",
                difundir(ctx, AGUA, 5)).build();
        Notification n = new Notification.Builder(ctx, CANAL_AGUA)
                .setSmallIcon(R.drawable.ic_stat)
                .setColor(0xFF4FA3F7)
                .setContentTitle("💧 ¡" + Frases.trato(ctx) + ", es hora de tomar agua!")
                .setContentText("Te toca el vaso " + Math.min(Agua.vasos(ctx), Agua.vasosQueTocan(ctx)) + " de "
                        + Agua.vasos(ctx) + ". Vas en " + Agua.progreso(ctx))
                .addAction(tome)
                .setAutoCancel(true)
                .build();
        nm.notify(2, n);
    }

    static void notificar(Context ctx, String frase) {
        if (!puedeNotificar(ctx)) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CANAL, "Frase del día",
                NotificationManager.IMPORTANCE_DEFAULT));
        String texto = frase
                + "\n\nIdea de juego: " + Frases.delDia(ctx, "IDEA", 3)
                + "\n\nTip Roblox: " + Frases.delDia(ctx, "ROBLOX", 5)
                + "\n\nTip Blender: " + Frases.delDia(ctx, "BLENDER", 11)
                + "\n\nProgramación: " + Frases.delDia(ctx, "PROG", 13);
        Notification n = new Notification.Builder(ctx, CANAL)
                .setSmallIcon(R.drawable.ic_stat)
                .setColor(0xFFD77757)
                .setContentTitle(Frases.saludo(ctx))
                .setContentText(frase)
                .setStyle(new Notification.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .build();
        nm.notify(1, n);
    }
}
