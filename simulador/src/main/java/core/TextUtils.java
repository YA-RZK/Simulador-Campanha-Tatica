package core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

/**
 * TextUtils — utilitário para desenhar texto com quebra de linha automática.
 *
 * Evita que textos longos (nomes de campanha, mensagens de resultado,
 * nomes de arquivos de mapa, etc.) ultrapassem a largura dos painéis das
 * telas, e permite empilhar blocos de texto verticalmente usando a altura
 * real ocupada por cada bloco (que pode variar conforme o texto quebra em
 * mais de uma linha).
 */
public final class TextUtils {

    private TextUtils() {}

    private static final GlyphLayout LAYOUT = new GlyphLayout();

    /**
     * Desenha {@code text} com quebra de linha automática dentro de
     * {@code wrapWidth} pixels, alinhado à esquerda, com o topo do texto
     * posicionado em {@code y}.
     *
     * @return altura (em pixels) realmente ocupada pelo texto desenhado —
     *         útil para posicionar o próximo bloco de texto logo abaixo,
     *         evitando sobreposição quando o texto quebra em várias linhas.
     */
    public static float drawWrapped(SpriteBatch batch, BitmapFont font, String text,
                                     float x, float y, float wrapWidth, Color color) {
        if (text == null) text = "";
        font.setColor(color);
        LAYOUT.setText(font, text, color, Math.max(10f, wrapWidth), Align.left, true);
        font.draw(batch, LAYOUT, x, y);
        return LAYOUT.height;
    }

    /**
     * Mede a altura que {@code text} ocupará ao ser desenhado com quebra de
     * linha em {@code wrapWidth}, sem desenhar nada na tela.
     */
    public static float heightOf(BitmapFont font, String text, float wrapWidth) {
        if (text == null) text = "";
        LAYOUT.setText(font, text, Color.WHITE, Math.max(10f, wrapWidth), Align.left, true);
        return LAYOUT.height;
    }

    /**
     * Corta {@code text} adicionando "..." caso ultrapasse {@code maxWidth}
     * em uma única linha (sem quebrar). Útil para linhas de lista onde
     * quebrar prejudicaria o alinhamento (ex.: uma linha por item).
     */
    public static String truncate(BitmapFont font, String text, float maxWidth) {
        if (text == null) return "";
        LAYOUT.setText(font, text);
        if (LAYOUT.width <= maxWidth) return text;

        String ellipsis = "...";
        int lo = 0, hi = text.length();
        String best = ellipsis;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            String candidate = text.substring(0, mid) + ellipsis;
            LAYOUT.setText(font, candidate);
            if (LAYOUT.width <= maxWidth) {
                best = candidate;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }
}
