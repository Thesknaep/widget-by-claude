package cl.yerko.clawd;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mascotas diseñadas por el usuario en pixel art, con animaciones generadas automáticamente.
 * Cada mascota tiene su propio tamaño de cuadrícula (16x12, 32x24 o 48x36, siempre 4:3)
 * y ocupa el mismo espacio en el widget: a más pixeles, más chicos y más detalle.
 */
final class Mascota {
    static final int W = 16, H = 12;
    static final int[][] TAMANOS = {{16, 12}, {32, 24}, {48, 36}};
    static final int OJO = 0;
    private static final String ALFABETO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmn";
    private static final int U = 18;

    static final int[] PALETA = {
        0xFF101010, 0xFFD77757, 0xFFF2A65A, 0xFFFFE14D, 0xFFFFFFFF, 0xFFC9D1D9, 0xFF8B949E, 0xFF484F58,
        0xFFE5484D, 0xFFFF8FA3, 0xFFF4C7A1, 0xFFC68642, 0xFF8D5524, 0xFF5BD15B, 0xFF2E8B3E, 0xFF9BF59B,
        0xFF4FC3F7, 0xFF2F6FDB, 0xFF1B2A6B, 0xFF9B6BF2, 0xFFD2B8FF, 0xFF6A3FC0, 0xFF00C2A8, 0xFF000000,
    };
    static final int MAX_COLORES = ALFABETO.length();

    static final class Datos {
        String id;
        String nombre = "Mi mascota";
        int w = W, h = H;
        int[] pal = PALETA.clone();
        byte[] px = vacio(W, H);

        static byte[] vacio(int w, int h) {
            byte[] b = new byte[w * h];
            java.util.Arrays.fill(b, (byte) -1);
            return b;
        }

        boolean vacia() {
            for (byte b : px) if (b >= 0) return false;
            return true;
        }
    }

    private static final Map<String, Bitmap> CACHE = new HashMap<>();

    static List<Datos> lista(Context c) {
        List<Datos> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(ClawdWidget.prefs(c).getString("mascotas", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Datos d = new Datos();
                d.id = o.getString("id");
                d.nombre = o.optString("nombre", "Mi mascota");
                d.w = o.optInt("w", W);
                d.h = o.optInt("h", H);
                d.px = Datos.vacio(d.w, d.h);
                JSONArray p = o.getJSONArray("pal");
                d.pal = new int[p.length()];
                for (int k = 0; k < p.length(); k++) d.pal[k] = (int) p.getLong(k);
                String s = o.getString("px");
                for (int k = 0; k < Math.min(s.length(), d.w * d.h); k++) {
                    char ch = s.charAt(k);
                    d.px[k] = ch == '.' ? -1 : (byte) ALFABETO.indexOf(ch);
                }
                out.add(d);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void guardarLista(Context c, List<Datos> l) {
        JSONArray a = new JSONArray();
        try {
            for (Datos d : l) {
                JSONArray p = new JSONArray();
                for (int v : d.pal) p.put(v & 0xFFFFFFFFL);
                StringBuilder sb = new StringBuilder();
                for (byte b : d.px) sb.append(b < 0 ? '.' : ALFABETO.charAt(b));
                a.put(new JSONObject().put("id", d.id).put("nombre", d.nombre).put("w", d.w).put("h", d.h)
                        .put("pal", p).put("px", sb.toString()));
            }
        } catch (Exception ignored) {
        }
        ClawdWidget.prefs(c).edit().putString("mascotas", a.toString()).apply();
        synchronized (CACHE) { CACHE.clear(); }
    }

    static Datos buscar(Context c, String id) {
        for (Datos d : lista(c)) if (d.id.equals(id)) return d;
        return null;
    }

    static void guardar(Context c, Datos d) {
        List<Datos> l = lista(c);
        if (d.id == null) {
            d.id = Long.toString(System.currentTimeMillis(), 36);
            l.add(d);
        } else {
            boolean hay = false;
            for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(d.id)) { l.set(i, d); hay = true; }
            if (!hay) l.add(d);
        }
        guardarLista(c, l);
    }

    static void borrar(Context c, String id) {
        List<Datos> l = lista(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).id.equals(id)) l.remove(i);
        guardarLista(c, l);
    }

    static Bitmap cuadro(Context c, String id, char pose) {
        String k = id + ":" + pose;
        synchronized (CACHE) {
            Bitmap b = CACHE.get(k);
            if (b != null) return b;
        }
        Datos d = buscar(c, id);
        Bitmap b = d == null ? dibujar(PALETA, Datos.vacio(W, H), W, H, pose) : dibujar(d.pal, d.px, d.w, d.h, pose);
        synchronized (CACHE) { CACHE.put(k, b); }
        return b;
    }

    /**
     * Dibuja un cuadro de animación en un lienzo de 18 x 14 unidades, igual que las demás skins.
     * La mascota siempre ocupa 16 x 12 unidades; cada pixel mide 16 / w unidades.
     */
    static Bitmap dibujar(int[] pal, byte[] px, int w, int h, char pose) {
        Bitmap bmp = Bitmap.createBitmap(18 * U, 14 * U, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint();
        p.setAntiAlias(false);
        float s = 16f / w;

        float tx = 1, ty = 2, sx = 1, sy = 1;
        boolean parpadeo = false, enojo = false;
        switch (pose) {
            case 'B': parpadeo = true; break;
            case 'W': case '1': case '3': tx += 0.5f; break;
            case 'S': case '2': case '4': tx -= 0.5f; break;
            case 'Q': sx = 1.08f; sy = 0.85f; parpadeo = true; break;
            case 'J': ty -= 2; break;
            case 'A': enojo = true; break;
            case 'C': enojo = true; tx += 0.35f; break;
            case 'D': enojo = true; tx -= 0.35f; break;
            default: break;
        }
        float cx = tx + 8f, fondo = ty + h * s;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = px[y * w + x];
                if (i < 0 || i >= pal.length) continue;
                int color = pal[i];
                boolean ojo = i == OJO;
                if (ojo && enojo) color = 0xFFE5484D;
                if (ojo && parpadeo) color = vecino(pal, px, w, h, x, y);
                float ux0 = tx + x * s, ux1 = tx + (x + 1) * s;
                float uy0 = ty + y * s, uy1 = ty + (y + 1) * s;
                float x0 = cx + (ux0 - cx) * sx, x1 = cx + (ux1 - cx) * sx;
                float y0 = fondo - (fondo - uy0) * sy, y1 = fondo - (fondo - uy1) * sy;
                p.setColor(color);
                cv.drawRect(new RectF(x0 * U, y0 * U, x1 * U + 0.5f, y1 * U + 0.5f), p);
                if (ojo && parpadeo) {
                    p.setColor(0xFF101010);
                    float ly = y0 + (y1 - y0) * 0.55f;
                    cv.drawRect(new RectF(x0 * U, ly * U, x1 * U + 0.5f, (ly + (y1 - y0) * 0.3f) * U), p);
                }
            }
        }
        return bmp;
    }

    private static int vecino(int[] pal, byte[] px, int w, int h, int x, int y) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int r = 1; r < w; r++) {
            for (int[] dd : dirs) {
                int nx = x + dd[0] * r, ny = y + dd[1] * r;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int i = px[ny * w + nx];
                if (i > 0 && i < pal.length) return pal[i];
            }
        }
        return pal.length > 1 ? pal[1] : 0xFFD77757;
    }

    /** Plantilla para empezar: un monito redondo naranja (16 x 12). */
    static byte[] plantilla() {
        String[] g = {
            "................",
            "......1111......",
            "....11111111....",
            "...1111111111...",
            "..111111111111..",
            "..110011110011..",
            "..110411110411..",
            "..111111111111..",
            "..11111991111...",
            "...1111111111...",
            "....11....11....",
            "....11....11....",
        };
        byte[] b = Datos.vacio(W, H);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                char ch = g[y].charAt(x);
                if (ch != '.') b[y * W + x] = (byte) ALFABETO.indexOf(ch);
            }
        return b;
    }
}
