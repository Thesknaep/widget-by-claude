package cl.yerko.clawd;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class Frases {
    static String nombre(Context c) {
        return ClawdWidget.prefs(c).getString("nombre", "").trim();
    }

    static void guardarNombre(Context c, String n) {
        ClawdWidget.prefs(c).edit().putString("nombre", n.trim()).apply();
    }

    /** "Hola Ana", o solo "Hola" si todavía no hay nombre. */
    static String saludo(Context c) {
        String n = nombre(c);
        return n.isEmpty() ? "Hola" : "Hola " + n;
    }

    /** El nombre para frases como "¡Agua, Ana!"; si no hay, usa "bro". */
    static String trato(Context c) {
        String n = nombre(c);
        return n.isEmpty() ? "bro" : n;
    }
    static final String CAT_SALUDO = "SALUDO";
    private static final int EN_WIDGET = 80;
    private static final Random R = new Random();
    private static List<Consejo> cache;

    static final class Consejo {
        final String cat;
        final String[] partes;

        Consejo(String cat, String[] partes) {
            this.cat = cat;
            this.partes = partes;
        }

        String texto() { return prefijo(cat) + partes[0]; }
    }

    static String prefijo(String cat) {
        switch (cat) {
            case "PROG": return "Programación: ";
            case "ROBLOX": return "Tip Roblox: ";
            case "BLENDER": return "Tip Blender: ";
            case "CONSTANCIA": return "Constancia: ";
            default: return "";
        }
    }

    /** Consejos descargados desde GitHub (si existen, reemplazan a los que vienen en la app). */
    static java.io.File archivoRemoto(Context ctx) {
        return new java.io.File(ctx.getFilesDir(), "consejos_remotos.txt");
    }

    static synchronized void olvidarCache() { cache = null; }

    static synchronized List<Consejo> todos(Context ctx) {
        if (cache != null) return cache;
        List<Consejo> out = new ArrayList<>();
        java.io.File remoto = archivoRemoto(ctx);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                remoto.exists() && remoto.length() > 100 ? new java.io.FileInputStream(remoto)
                        : ctx.getResources().openRawResource(R_raw()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int bar = line.indexOf('|');
                if (bar <= 0) continue;
                String[] partes = line.substring(bar + 1).split(" >> ");
                for (int i = 0; i < partes.length; i++) partes[i] = partes[i].trim();
                out.add(new Consejo(line.substring(0, bar).trim(), partes));
            }
        } catch (Exception ignored) {
        }
        if (out.isEmpty()) out.add(new Consejo("PERSONA", new String[]{"Hoy es un buen día para crear algo increíble"}));
        cache = out;
        return out;
    }

    private static int R_raw() { return cl.yerko.clawd.R.raw.consejos; }

    static List<Consejo> deCategoria(Context ctx, String cat) {
        List<Consejo> out = new ArrayList<>();
        for (Consejo c : todos(ctx)) if (c.cat.equals(cat)) out.add(c);
        return out;
    }

    static Consejo otroDe(Context ctx, String cat, String evitar) {
        List<Consejo> l = deCategoria(ctx, cat);
        if (l.isEmpty()) l = todos(ctx);
        Consejo c;
        int intentos = 0;
        do { c = l.get(R.nextInt(l.size())); } while (c.partes[0].equals(evitar) && ++intentos < 10);
        return c;
    }

    static int dia() { return Calendar.getInstance().get(Calendar.DAY_OF_YEAR); }

    static String delDia(Context ctx, String cat, int salto) {
        List<Consejo> l = deCategoria(ctx, cat);
        if (l.isEmpty()) return "";
        return l.get((dia() * salto) % l.size()).partes[0];
    }

    static ArrayList<Consejo> lista(Context ctx) {
        ArrayList<Consejo> mezcla = new ArrayList<>(todos(ctx));
        Collections.shuffle(mezcla, R);
        ArrayList<Consejo> out = new ArrayList<>();
        Consejo hola = new Consejo(CAT_SALUDO, new String[]{saludo(ctx),
                "¿En qué trabajamos hoy? Toca a Claude para que te dé una idea de juego"});
        out.add(hola);
        for (int i = 0; i < Math.min(EN_WIDGET, mezcla.size()); i++) {
            out.add(mezcla.get(i));
            if (i % 10 == 9) out.add(hola);
        }
        return out;
    }
}
