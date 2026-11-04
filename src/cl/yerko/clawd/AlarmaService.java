package cl.yerko.clawd;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import java.io.File;

/** Hace sonar la alarma con el canal de ALARMA (suena aunque el teléfono esté en silencio). */
public class AlarmaService extends Service {
    private static final String CANAL = "claude_alarma_v2";
    private static final long MAX_SONANDO = 10 * 60_000L;

    private MediaPlayer mp;
    private Vibrator vib;
    private int volumenAntes = -1;
    private final Handler h = new Handler(Looper.getMainLooper());

    static MediaPlayer reproducir(Context c, String tonoId, boolean alarma) {
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(alarma ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        Object[] intentos = {Tonos.fuente(c, tonoId),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), Tonos.archivo(c, 7)};
        for (Object fuente : intentos) {
            MediaPlayer p = new MediaPlayer();
            try {
                p.setAudioAttributes(aa);
                if (fuente instanceof File) p.setDataSource(((File) fuente).getAbsolutePath());
                else p.setDataSource(c, (Uri) fuente);
                p.setLooping(true);
                p.prepare();
                p.start();
                return p;
            } catch (Exception e) {
                p.release();
            }
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent in, int flags, int startId) {
        if (in == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String etiqueta = in.getStringExtra("etiqueta");
        String hora = in.getStringExtra("hora");
        String tono = in.getStringExtra("tono");
        boolean vibrar = in.getBooleanExtra("vibrar", true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CANAL, "Alarma sonando", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);

        Intent pantalla = new Intent(this, AlarmaActivity.class).putExtras(in)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int fl = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent full = PendingIntent.getActivity(this, 3001, pantalla, fl);
        Intent det = new Intent(this, ClawdWidget.class).setAction(Alarmas.DETENER);
        Intent pos = new Intent(this, ClawdWidget.class).setAction(Alarmas.POSPONER).putExtras(in);

        Notification n = new Notification.Builder(this, CANAL)
                .setSmallIcon(R.drawable.ic_stat)
                .setColor(0xFFD77757)
                .setContentTitle("⏰ " + hora + " · " + etiqueta)
                .setContentText(Frases.saludo(this) + ". ¡Es hora!")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Detener",
                        PendingIntent.getBroadcast(this, 3002, det, fl)).build())
                .addAction(new Notification.Action.Builder(null, "Posponer " + Alarmas.POSPONER_MIN + " min",
                        PendingIntent.getBroadcast(this, 3003, pos, fl)).build())
                .build();
        startForeground(Alarmas.NOTI, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);

        detenerSonido();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        try {
            volumenAntes = am.getStreamVolume(AudioManager.STREAM_ALARM);
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        } catch (Exception ignored) {
            volumenAntes = -1;
        }
        mp = reproducir(this, tono, true);

        if (vibrar) {
            vib = ((VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 900, 500, 900, 500}, 0),
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        }

        try {
            startActivity(pantalla);
        } catch (Exception ignored) {
        }
        h.removeCallbacksAndMessages(null);
        h.postDelayed(this::stopSelf, MAX_SONANDO);
        return START_NOT_STICKY;
    }

    private void detenerSonido() {
        if (mp != null) {
            try { mp.stop(); } catch (Exception ignored) { }
            mp.release();
            mp = null;
        }
        if (vib != null) {
            vib.cancel();
            vib = null;
        }
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        detenerSonido();
        if (volumenAntes >= 0) {
            try {
                ((AudioManager) getSystemService(AUDIO_SERVICE))
                        .setStreamVolume(AudioManager.STREAM_ALARM, volumenAntes, 0);
            } catch (Exception ignored) {
            }
        }
        AlarmaActivity.cerrar();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
