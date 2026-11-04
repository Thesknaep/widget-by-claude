package cl.yerko.clawd;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Convierte una foto (pixeles ARGB) en una mascota pixel art (16x12, 32x24 o 48x36):
 * quita el fondo (opcional), recorta, reduce y pasa a una paleta de pocos colores.
 * No usa clases de Android para poder probarla también en el PC.
 */
final class FotoPixel {

    static final class Resultado {
        int[] pal;
        byte[] px;
    }

    /**
     * @param argb    pixeles de la foto (ancho x alto)
     * @param pal     paleta base (se reutilizan colores parecidos y se agregan los que falten)
     * @param maxPal  tamaño máximo de la paleta
     * @param W       ancho de la cuadrícula de pixel art
     * @param H       alto de la cuadrícula de pixel art
     * @param quitar  true = quitar el fondo; false = usar la foto completa recortada al centro
     */
    static Resultado convertir(int[] argb, int ancho, int alto, int[] pal, int maxPal,
                               int W, int H, boolean quitar) {
        int COLORES = W >= 64 ? 16 : W >= 32 ? 14 : 8;
        boolean[] fondo = quitar ? quitarFondo(argb, ancho, alto) : new boolean[argb.length];

        // Si quitar el fondo dejó casi nada o casi todo, mejor usar la foto completa.
        int objeto = 0;
        for (boolean f : fondo) if (!f) objeto++;
        if (quitar && (objeto < argb.length * 0.04f || objeto > argb.length * 0.97f)) {
            fondo = new boolean[argb.length];
            quitar = false;
        }

        int x0 = ancho, y0 = alto, x1 = -1, y1 = -1;
        if (quitar) {
            // Recortar al objeto (lo que no es fondo).
            for (int y = 0; y < alto; y++)
                for (int x = 0; x < ancho; x++)
                    if (!fondo[y * ancho + x]) {
                        x0 = Math.min(x0, x); y0 = Math.min(y0, y);
                        x1 = Math.max(x1, x); y1 = Math.max(y1, y);
                    }
        } else {
            // Foto completa: recorte centrado con la forma de la cuadrícula (4:3).
            float rel = W / (float) H;
            int cw = ancho, ch = Math.round(ancho / rel);
            if (ch > alto) { ch = alto; cw = Math.round(alto * rel); }
            x0 = (ancho - cw) / 2; y0 = (alto - ch) / 4; x1 = x0 + cw - 1; y1 = y0 + ch - 1;
        }
        if (x1 < 0) { x0 = 0; y0 = 0; x1 = ancho - 1; y1 = alto - 1; Arrays.fill(fondo, false); }
        int cw = x1 - x0 + 1, ch = y1 - y0 + 1;

        // Encajar en la cuadrícula sin deformar, centrado y apoyado en el suelo.
        float escala = Math.min(W / (float) cw, H / (float) ch);
        int gw = Math.max(1, Math.min(W, Math.round(cw * escala)));
        int gh = Math.max(1, Math.min(H, Math.round(ch * escala)));
        int ox = (W - gw) / 2, oy = H - gh;

        int[] celdas = new int[W * H];
        boolean[] llena = new boolean[W * H];
        for (int gy = 0; gy < gh; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                int sx0 = x0 + gx * cw / gw, sx1 = Math.max(sx0 + 1, x0 + (gx + 1) * cw / gw);
                int sy0 = y0 + gy * ch / gh, sy1 = Math.max(sy0 + 1, y0 + (gy + 1) * ch / gh);
                long r = 0, g = 0, b = 0;
                int n = 0, total = 0;
                for (int y = sy0; y < sy1; y++)
                    for (int x = sx0; x < sx1; x++) {
                        total++;
                        int i = y * ancho + x;
                        if (fondo[i]) continue;
                        int c = argb[i];
                        r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255;
                        n++;
                    }
                if (n == 0 || n < total * 0.4f) continue;
                int k = (oy + gy) * W + (ox + gx);
                celdas[k] = color((int) (r / n), (int) (g / n), (int) (b / n));
                llena[k] = true;
            }
        }

        // Colores más vivos, estilo pixel art.
        for (int i = 0; i < celdas.length; i++) if (llena[i]) celdas[i] = avivar(celdas[i]);

        int[] centros = kmedias(celdas, llena, COLORES);

        // Reutilizar colores de la paleta si son parecidos; si no, agregarlos.
        List<Integer> nueva = new ArrayList<>();
        for (int c : pal) nueva.add(c);
        int[] indice = new int[centros.length];
        for (int k = 0; k < centros.length; k++) {
            int mejor = -1;
            double dMejor = Double.MAX_VALUE;
            for (int i = 1; i < nueva.size(); i++) {
                double d = dist(centros[k], nueva.get(i));
                if (d < dMejor) { dMejor = d; mejor = i; }
            }
            if (dMejor > 28 && nueva.size() < maxPal) {
                nueva.add(centros[k]);
                mejor = nueva.size() - 1;
            }
            indice[k] = mejor;
        }

        Resultado res = new Resultado();
        res.pal = new int[nueva.size()];
        for (int i = 0; i < nueva.size(); i++) res.pal[i] = nueva.get(i);
        res.px = new byte[W * H];
        Arrays.fill(res.px, (byte) -1);
        for (int i = 0; i < celdas.length; i++) {
            if (!llena[i]) continue;
            int mejor = 0;
            double dMejor = Double.MAX_VALUE;
            for (int k = 0; k < centros.length; k++) {
                double d = dist(celdas[i], centros[k]);
                if (d < dMejor) { dMejor = d; mejor = k; }
            }
            res.px[i] = (byte) indice[mejor];
        }
        return res;
    }

    /** Marca como fondo lo transparente y lo que se parece al color de los bordes y toca el borde. */
    static boolean[] quitarFondo(int[] argb, int ancho, int alto) {
        boolean[] fondo = new boolean[argb.length];
        boolean hayAlfa = false;
        for (int i = 0; i < argb.length; i++) {
            if (((argb[i] >>> 24) & 255) < 128) { fondo[i] = true; hayAlfa = true; }
        }
        if (hayAlfa) return fondo;

        // Color típico del borde (mediana por canal).
        int nb = 2 * (ancho + alto);
        int[] rs = new int[nb], gs = new int[nb], bs = new int[nb];
        int k = 0;
        for (int x = 0; x < ancho; x++) {
            for (int y : new int[]{0, alto - 1}) {
                int c = argb[y * ancho + x];
                rs[k] = (c >> 16) & 255; gs[k] = (c >> 8) & 255; bs[k] = c & 255; k++;
            }
        }
        for (int y = 0; y < alto; y++) {
            for (int x : new int[]{0, ancho - 1}) {
                int c = argb[y * ancho + x];
                rs[k] = (c >> 16) & 255; gs[k] = (c >> 8) & 255; bs[k] = c & 255; k++;
            }
        }
        Arrays.sort(rs, 0, k); Arrays.sort(gs, 0, k); Arrays.sort(bs, 0, k);
        int bg = color(rs[k / 2], gs[k / 2], bs[k / 2]);

        // Si el borde es muy variado (foto con fondo complicado), se usa el método por grupos de color.
        int parecidos = 0;
        for (int i = 0; i < k; i++) if (dist(color(rs[i], gs[i], bs[i]), bg) < 60) parecidos++;
        if (parecidos < k * 0.55f) return fondoComplejo(argb, ancho, alto);

        ArrayDeque<Integer> cola = new ArrayDeque<>();
        for (int x = 0; x < ancho; x++) { cola.add(x); cola.add((alto - 1) * ancho + x); }
        for (int y = 0; y < alto; y++) { cola.add(y * ancho); cola.add(y * ancho + ancho - 1); }
        while (!cola.isEmpty()) {
            int i = cola.poll();
            if (fondo[i] || dist(argb[i], bg) > 55) continue;
            fondo[i] = true;
            int x = i % ancho, y = i / ancho;
            if (x > 0) cola.add(i - 1);
            if (x < ancho - 1) cola.add(i + 1);
            if (y > 0) cola.add(i - ancho);
            if (y < alto - 1) cola.add(i + ancho);
        }
        // Además, lo que tenga colores muy presentes en los bordes (plantas, rejas…) también es fondo.
        boolean[] porGrupos = fondoComplejo(argb, ancho, alto);
        int quedan = 0, quedanAmbos = 0;
        for (int i = 0; i < fondo.length; i++) {
            if (!fondo[i]) quedan++;
            if (!fondo[i] && !porGrupos[i]) quedanAmbos++;
        }
        if (quedanAmbos > quedan * 0.25f) {
            for (int i = 0; i < fondo.length; i++) fondo[i] = fondo[i] || porGrupos[i];
        }
        return objetoPrincipal(fondo, ancho, alto);
    }

    /**
     * Fondo con varios colores: agrupa los colores de la foto y marca como fondo los grupos
     * que aparecen mucho en los bordes. Luego se queda con el objeto principal.
     */
    static boolean[] fondoComplejo(int[] argb, int ancho, int alto) {
        int n = argb.length;
        boolean[] todos = new boolean[n];
        Arrays.fill(todos, true);
        int k = 7;
        int[] cen = kmedias(argb, todos, k);
        k = cen.length;
        int[] grupo = new int[n];
        for (int i = 0; i < n; i++) {
            int m = 0;
            double dm = Double.MAX_VALUE;
            for (int q = 0; q < k; q++) {
                double d = dist(argb[i], cen[q]);
                if (d < dm) { dm = d; m = q; }
            }
            grupo[i] = m;
        }
        // Cuánto aparece cada grupo de color en las orillas y en el centro de la foto.
        int[] enBorde = new int[k], enCentro = new int[k];
        int borde = 0, centro = 0;
        int mx = Math.max(1, ancho / 12), my = Math.max(1, alto / 12);
        for (int i = 0; i < n; i++) {
            int x = i % ancho, y = i / ancho;
            if (x < mx || y < my || x >= ancho - mx || y >= alto - my) { enBorde[grupo[i]]++; borde++; }
            if (x > ancho * 0.3f && x < ancho * 0.7f && y > alto * 0.25f && y < alto * 0.75f) {
                enCentro[grupo[i]]++; centro++;
            }
        }
        boolean[] esFondo = new boolean[k];
        for (int q = 0; q < k; q++) {
            float b = enBorde[q] / (float) Math.max(1, borde), c = enCentro[q] / (float) Math.max(1, centro);
            // Fondo: colores que dominan las orillas, o que están en las orillas y casi no en el centro.
            esFondo[q] = b > 0.08f && b > c * 0.8f || (b > 0.01f && c < 0.04f);
        }
        boolean[] fondo = new boolean[n];
        for (int i = 0; i < n; i++) fondo[i] = esFondo[grupo[i]];
        return objetoPrincipal(fondo, ancho, alto);
    }

    /** Deja solo el bloque más grande cerca del centro y rellena sus huecos internos. */
    static boolean[] objetoPrincipal(boolean[] fondo, int ancho, int alto) {
        int n = fondo.length;
        int[] etiqueta = new int[n];
        Arrays.fill(etiqueta, -1);
        int mejor = -1;
        double puntajeMejor = -1;
        int et = 0;
        ArrayDeque<Integer> cola = new ArrayDeque<>();
        for (int s = 0; s < n; s++) {
            if (fondo[s] || etiqueta[s] >= 0) continue;
            int tam = 0;
            long sx = 0, sy = 0;
            cola.add(s);
            etiqueta[s] = et;
            while (!cola.isEmpty()) {
                int i = cola.poll();
                tam++;
                int x = i % ancho, y = i / ancho;
                sx += x; sy += y;
                int[] vec = {x > 0 ? i - 1 : -1, x < ancho - 1 ? i + 1 : -1, y > 0 ? i - ancho : -1, y < alto - 1 ? i + ancho : -1};
                for (int v : vec) if (v >= 0 && !fondo[v] && etiqueta[v] < 0) { etiqueta[v] = et; cola.add(v); }
            }
            double dx = sx / (double) tam / ancho - 0.5, dy = sy / (double) tam / alto - 0.5;
            double puntaje = tam * (1.2 - Math.min(1.0, Math.sqrt(dx * dx + dy * dy) * 2));
            if (puntaje > puntajeMejor) { puntajeMejor = puntaje; mejor = et; }
            et++;
        }
        boolean[] res = new boolean[n];
        for (int i = 0; i < n; i++) res[i] = etiqueta[i] != mejor;
        if (mejor < 0) return fondo;

        // Rellenar huecos: el fondo que no se conecta con el borde pasa a ser parte del objeto.
        boolean[] exterior = new boolean[n];
        for (int x = 0; x < ancho; x++) { cola.add(x); cola.add((alto - 1) * ancho + x); }
        for (int y = 0; y < alto; y++) { cola.add(y * ancho); cola.add(y * ancho + ancho - 1); }
        while (!cola.isEmpty()) {
            int i = cola.poll();
            if (exterior[i] || !res[i]) continue;
            exterior[i] = true;
            int x = i % ancho, y = i / ancho;
            if (x > 0) cola.add(i - 1);
            if (x < ancho - 1) cola.add(i + 1);
            if (y > 0) cola.add(i - ancho);
            if (y < alto - 1) cola.add(i + ancho);
        }
        for (int i = 0; i < n; i++) if (res[i] && !exterior[i]) res[i] = false;
        return res;
    }

    private static int[] kmedias(int[] celdas, boolean[] llena, int k) {
        List<Integer> pts = new ArrayList<>();
        for (int i = 0; i < celdas.length; i++) if (llena[i]) pts.add(celdas[i]);
        if (pts.isEmpty()) return new int[]{0xFFD77757};
        // Colores distintos; si son pocos, no hace falta agrupar.
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>();
        for (int c : pts) { set.add(c); if (set.size() > k) break; }
        List<Integer> distintos = new ArrayList<>(set);
        if (distintos.size() <= k) {
            int[] r = new int[distintos.size()];
            for (int i = 0; i < r.length; i++) r[i] = distintos.get(i);
            return r;
        }
        // Inicio: el primero y luego el más lejano a los ya elegidos (k-means++ simple).
        int[] cen = new int[k];
        cen[0] = pts.get(0);
        for (int j = 1; j < k; j++) {
            int lejano = pts.get(0);
            double dLejano = -1;
            for (int c : pts) {
                double dm = Double.MAX_VALUE;
                for (int q = 0; q < j; q++) dm = Math.min(dm, dist(c, cen[q]));
                if (dm > dLejano) { dLejano = dm; lejano = c; }
            }
            cen[j] = lejano;
        }
        for (int it = 0; it < 12; it++) {
            long[] r = new long[k], g = new long[k], b = new long[k];
            int[] n = new int[k];
            for (int c : pts) {
                int m = 0;
                double dm = Double.MAX_VALUE;
                for (int q = 0; q < k; q++) {
                    double d = dist(c, cen[q]);
                    if (d < dm) { dm = d; m = q; }
                }
                r[m] += (c >> 16) & 255; g[m] += (c >> 8) & 255; b[m] += c & 255; n[m]++;
            }
            for (int q = 0; q < k; q++)
                if (n[q] > 0) cen[q] = color((int) (r[q] / n[q]), (int) (g[q] / n[q]), (int) (b[q] / n[q]));
        }
        return cen;
    }

    private static int avivar(int c) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float gris = 0.299f * r + 0.587f * g + 0.114f * b;
        float s = 1.25f;
        r = gris + (r - gris) * s; g = gris + (g - gris) * s; b = gris + (b - gris) * s;
        return color(lim(r * 255), lim(g * 255), lim(b * 255));
    }

    private static int lim(float v) { return Math.max(0, Math.min(255, Math.round(v))); }

    static int color(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }

    static double dist(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255);
        int dg = ((a >> 8) & 255) - ((b >> 8) & 255);
        int db = (a & 255) - (b & 255);
        return Math.sqrt(2 * dr * dr + 4 * dg * dg + 3 * db * db) / 3.0;
    }
}
