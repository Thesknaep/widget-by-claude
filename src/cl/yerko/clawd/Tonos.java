package cl.yerko.clawd;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Tonos propios generados en el teléfono (chiptune fuerte) + tonos de alarma del sistema. */
final class Tonos {
    static final String DEFECTO = "app:0";
    private static final int SR = 22050;

    static final String[] NOMBRES = {
        "Claude ¡Arriba!", "Power Up", "Victoria épica", "Moneda mágica",
        "Turbo", "Fanfarria", "Pixel Party", "Beep beep fuerte",
    };

    // {nota MIDI, duración en pasos}; 0 = silencio. Más abajo: ms por paso.
    private static final int[][][] MELODIAS = {
        {{72,1},{76,1},{79,1},{84,2},{79,1},{84,1},{88,4},{0,2},{74,1},{77,1},{81,1},{86,2},{81,1},{86,1},{89,4},{0,2},
         {76,1},{79,1},{84,1},{88,2},{84,1},{88,1},{91,6},{0,4}},
        {{60,1},{64,1},{67,1},{72,1},{65,1},{69,1},{72,1},{77,1},{67,1},{71,1},{74,1},{79,1},{72,1},{76,1},{79,1},{84,1},
         {84,4},{0,4}},
        {{67,1},{67,1},{67,1},{72,6},{0,1},{70,2},{72,2},{74,2},{76,6},{0,2},{74,1},{76,1},{79,8},{0,4}},
        {{84,1},{91,5},{0,2},{84,1},{91,5},{0,2},{86,1},{93,5},{0,2},{88,1},{96,8},{0,6}},
        {{72,1},{84,1},{72,1},{84,1},{72,1},{84,1},{72,1},{84,1},{76,1},{88,1},{76,1},{88,1},{76,1},{88,1},{76,1},{88,1},
         {79,1},{91,1},{79,1},{91,1},{79,1},{91,1},{79,1},{91,1},{84,4},{0,4}},
        {{60,2},{64,2},{67,2},{72,4},{67,2},{72,8},{0,2},{62,2},{65,2},{69,2},{74,4},{69,2},{74,8},{0,2},
         {64,2},{67,2},{72,2},{76,4},{79,4},{84,10},{0,6}},
        {{72,1},{0,1},{79,1},{0,1},{76,1},{0,1},{79,1},{0,1},{74,1},{0,1},{81,1},{0,1},{77,1},{0,1},{81,1},{0,1},
         {76,1},{0,1},{84,1},{0,1},{79,1},{0,1},{84,1},{0,1},{77,1},{79,1},{81,1},{83,1},{84,4},{0,4}},
        {{88,2},{0,1},{88,2},{0,1},{88,2},{0,1},{88,2},{0,6}},
    };
    private static final int[] MS_PASO = {85, 65, 95, 55, 50, 85, 90, 70};
    private static final double[] DUTY = {0.5, 0.25, 0.5, 0.25, 0.5, 0.5, 0.25, 0.5};

    static final class Tono {
        final String id, nombre;
        Tono(String id, String nombre) { this.id = id; this.nombre = nombre; }
    }

    static List<Tono> todos(Context c) {
        List<Tono> out = new ArrayList<>();
        for (int i = 0; i < NOMBRES.length; i++) out.add(new Tono("app:" + i, "🎮 " + NOMBRES[i]));
        try {
            RingtoneManager rm = new RingtoneManager(c);
            rm.setType(RingtoneManager.TYPE_ALARM);
            Cursor cur = rm.getCursor();
            int n = 0;
            while (cur.moveToNext() && n < 40) {
                Uri u = rm.getRingtoneUri(cur.getPosition());
                out.add(new Tono("sys:" + u, "🔔 " + cur.getString(RingtoneManager.TITLE_COLUMN_INDEX)));
                n++;
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    static String nombre(Context c, String id) {
        if (id == null) id = DEFECTO;
        if (id.startsWith("app:")) {
            try { return NOMBRES[Integer.parseInt(id.substring(4))]; } catch (Exception e) { return NOMBRES[0]; }
        }
        try {
            String t = RingtoneManager.getRingtone(c, Uri.parse(id.substring(4))).getTitle(c);
            return t == null ? "Tono del teléfono" : t;
        } catch (Exception e) {
            return "Tono del teléfono";
        }
    }

    /** Devuelve un archivo (tono propio) o una Uri del sistema. */
    static Object fuente(Context c, String id) {
        if (id == null) id = DEFECTO;
        if (id.startsWith("sys:")) return Uri.parse(id.substring(4));
        int i;
        try { i = Integer.parseInt(id.substring(4)); } catch (Exception e) { i = 0; }
        if (i < 0 || i >= MELODIAS.length) i = 0;
        return archivo(c, i);
    }

    static synchronized File archivo(Context c, int i) {
        File f = new File(c.getFilesDir(), "tono_" + i + "_v1.wav");
        if (f.exists() && f.length() > 1000) return f;
        short[] pcm = sintetizar(i);
        try (FileOutputStream o = new FileOutputStream(f)) {
            int bytes = pcm.length * 2;
            byte[] h = new byte[44];
            escribir(h, 0, "RIFF");
            le32(h, 4, 36 + bytes);
            escribir(h, 8, "WAVE");
            escribir(h, 12, "fmt ");
            le32(h, 16, 16);
            le16(h, 20, 1);
            le16(h, 22, 1);
            le32(h, 24, SR);
            le32(h, 28, SR * 2);
            le16(h, 32, 2);
            le16(h, 34, 16);
            escribir(h, 36, "data");
            le32(h, 40, bytes);
            o.write(h);
            byte[] data = new byte[bytes];
            for (int k = 0; k < pcm.length; k++) {
                data[2 * k] = (byte) (pcm[k] & 0xff);
                data[2 * k + 1] = (byte) ((pcm[k] >> 8) & 0xff);
            }
            o.write(data);
        } catch (Exception ignored) {
        }
        return f;
    }

    private static short[] sintetizar(int i) {
        int[][] mel = MELODIAS[i];
        int paso = MS_PASO[i] * SR / 1000;
        int total = 0;
        for (int[] n : mel) total += n[1] * paso;
        short[] out = new short[total];
        int pos = 0;
        double duty = DUTY[i];
        for (int[] n : mel) {
            int len = n[1] * paso;
            if (n[0] > 0) {
                double f = 440.0 * Math.pow(2, (n[0] - 69) / 12.0);
                int ataque = SR * 3 / 1000, suelta = Math.min(len / 3, SR * 12 / 1000);
                for (int k = 0; k < len; k++) {
                    double t = (double) k / SR;
                    double fase1 = (t * f) % 1.0, fase2 = (t * f / 2) % 1.0;
                    double v = 0.72 * (fase1 < duty ? 1 : -1) + 0.28 * (fase2 < 0.5 ? 1 : -1);
                    double env = 1.0;
                    if (k < ataque) env = (double) k / ataque;
                    else if (k > len - suelta) env = (double) (len - k) / suelta;
                    env *= 1.0 - 0.25 * k / len;
                    out[pos + k] = (short) (v * env * 0.88 * 32767);
                }
            }
            pos += len;
        }
        return out;
    }

    private static void escribir(byte[] b, int o, String s) {
        for (int i = 0; i < 4; i++) b[o + i] = (byte) s.charAt(i);
    }

    private static void le32(byte[] b, int o, int v) {
        b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); b[o + 2] = (byte) (v >> 16); b[o + 3] = (byte) (v >> 24);
    }

    private static void le16(byte[] b, int o, int v) {
        b[o] = (byte) v; b[o + 1] = (byte) (v >> 8);
    }
}
