package cl.yerko.clawd;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

/**
 * Ítems que la mascota tiene en la mano. Al tocarlos en el widget pasa algo distinto
 * según el ítem, y se guardan en la mochila (colección + monedas).
 */
final class Items {
    static final String[] IDS = {
        "llave", "espada", "pico", "moneda", "gema", "pocion", "corazon",
        "pizza", "helado", "balon", "control", "libro", "cohete", "estrella",
        "regalo", "trofeo", "flor", "varita", "hamburguesa",
    };
    static final String[] NOMBRES = {
        "Llave", "Espada", "Pico", "Moneda", "Gema", "Poción", "Corazón",
        "Pizza", "Helado", "Balón", "Control", "Libro", "Cohete", "Estrella",
        "Regalo", "Trofeo", "Flor", "Varita", "Hamburguesa",
    };
    private static final Random R = new Random();

    static int drawable(Context c, int i) {
        int id = c.getResources().getIdentifier("item_" + IDS[i], "drawable", c.getPackageName());
        return id != 0 ? id : R_moneda();
    }

    private static int R_moneda() { return cl.yerko.clawd.R.drawable.item_moneda; }

    private static SharedPreferences p(Context c) { return ClawdWidget.prefs(c); }

    static int veces(Context c, int i) { return p(c).getInt("item_" + IDS[i], 0); }
    static int monedas(Context c) { return p(c).getInt("monedas", 0); }
    static int total(Context c) { return p(c).getInt("itemsTocados", 0); }

    static int distintos(Context c) {
        int n = 0;
        for (int i = 0; i < IDS.length; i++) if (veces(c, i) > 0) n++;
        return n;
    }

    private static String al(String... s) { return s[R.nextInt(s.length)]; }

    /** Qué pasa al tocar el ítem. Devuelve el mensaje que dirá la mascota. */
    static String tocar(Context c, int i) {
        if (i < 0 || i >= IDS.length) i = 3;
        String n = Frases.trato(c);
        boolean nuevo = veces(c, i) == 0;
        int monedasGanadas = 0;
        String msg;
        switch (IDS[i]) {
            case "moneda":
                monedasGanadas = 1;
                msg = "🪙 ¡+1 moneda!";
                break;
            case "gema":
                monedasGanadas = 5;
                msg = "💎 ¡Gema rara! +5 monedas";
                break;
            case "corazon":
                msg = al("❤️ Te quiero, " + n, "❤️ Eres lo máximo, " + n, "❤️ Gracias por cuidarme");
                break;
            case "llave":
                msg = "🗝️ Abriste un cofre secreto… Tip Roblox: " + Frases.otroDe(c, "ROBLOX", "").partes[0];
                break;
            case "regalo":
                msg = "🎁 ¡Sorpresa! " + Frases.otroDe(c, "IDEA", "").partes[0];
                break;
            case "pocion":
                msg = al("🧪 ¡Poción de energía! Hoy rindes x2", "🧪 Glup glup… ¡me siento invencible!",
                        "🧪 Poción de concentración activada");
                break;
            case "espada":
                msg = al("⚔️ ¡A derrotar bugs!", "⚔️ Ningún error se nos escapa hoy", "⚔️ ¡En guardia, " + n + "!");
                break;
            case "pico":
                msg = "⛏️ Minando ideas… Programación: " + Frases.otroDe(c, "PROG", "").partes[0];
                break;
            case "pizza":
                msg = al("🍕 ¡Ñam! Pizza nivel dios", "🍕 Una rebanada para mí y otra para ti", "🍕 ¡Pizza party!");
                break;
            case "helado":
                msg = al("🍦 ¡Brain freeze!", "🍦 Helado de frutilla, mi favorito", "🍦 ¿Me das una probadita?");
                break;
            case "hamburguesa":
                msg = al("🍔 ¡Combo agrandado!", "🍔 Ñam ñam ñam", "🍔 Con extra queso, porfa");
                break;
            case "balon":
                msg = al("⚽ ¡GOOOOL!", "⚽ ¡Golazo de chilena!", "⚽ ¿Un partido después de programar?");
                break;
            case "control":
                msg = al("🎮 ¿Una partida? Primero termina tu tarea 😏", "🎮 ¡Combo perfecto!", "🎮 Player 2 se unió");
                break;
            case "libro":
                msg = "📖 Leí algo bueno: " + Frases.otroDe(c, "PERSONA", "").partes[0];
                break;
            case "cohete":
                msg = "🚀 ¡Despegando! " + Frases.otroDe(c, "CONSTANCIA", "").partes[0];
                break;
            case "estrella":
                msg = al("⭐ ¡Eres una estrella, " + n + "!", "⭐ Brillas más que yo", "⭐ +1 de estilo");
                break;
            case "trofeo":
                msg = "🏆 Logro: tocaste " + (total(c) + 1) + " ítems y tienes " + monedas(c) + " monedas";
                break;
            case "flor":
                msg = al("🌸 Para ti, " + n, "🌸 Huele a primavera", "🌸 Una flor por ser tan constante");
                break;
            case "varita":
                msg = "🪄 ¡Magia! Tip Blender: " + Frases.otroDe(c, "BLENDER", "").partes[0];
                break;
            default:
                msg = "✨ ¡Wow!";
        }
        SharedPreferences.Editor e = p(c).edit()
                .putInt("item_" + IDS[i], veces(c, i) + 1)
                .putInt("itemsTocados", total(c) + 1)
                .putInt("monedas", monedas(c) + monedasGanadas);
        e.apply();
        if (monedasGanadas > 0) msg += " · Tienes " + monedas(c) + " 🪙";
        if (nuevo) msg += " · ¡Nuevo en tu mochila! 🎒";
        return msg;
    }
}
