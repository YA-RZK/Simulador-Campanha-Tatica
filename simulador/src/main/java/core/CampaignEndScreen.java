package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

/**
 * Tela final exibida após a conclusão de todas as missões da campanha.
 *
 * Mostra o placar completo, o veredicto geral e a lista de resultados de
 * cada execução. Todo texto usa quebra de linha automática (via
 * {@link TextUtils}) para nunca ultrapassar a largura do painel, e a lista
 * de execuções é rolável e recortada para nunca vazar para fora da tela,
 * não importa quantas missões a campanha tenha tido.
 *
 * Atalhos:
 *   UP / DOWN      - rolar a lista de execuções
 *   T              - abrir a Tela Totalizadora (histórico via CSV)
 *   ESC            - fechar o programa
 */
public class CampaignEndScreen implements Screen {

    private final MainGame       game;
    private final MissionManager missions;

    private ShapeRenderer shape;
    private SpriteBatch   batch;
    private BitmapFont    fontBig;
    private BitmapFont    fontMed;
    private BitmapFont    fontSmall;

    private static final Color COL_BG      = new Color(0.04f, 0.04f, 0.08f, 1f);
    private static final Color COL_PANEL   = new Color(0.08f, 0.08f, 0.14f, 1f);
    private static final Color COL_VICTORY = new Color(0.20f, 0.90f, 0.40f, 1f);
    private static final Color COL_DEFEAT  = new Color(0.95f, 0.25f, 0.25f, 1f);
    private static final Color COL_DRAW    = new Color(0.90f, 0.80f, 0.20f, 1f);
    private static final Color COL_TEXT    = new Color(0.80f, 0.80f, 1.00f, 1f);
    private static final Color COL_HINT    = new Color(0.45f, 0.45f, 0.55f, 1f);

    private float scrollOffset = 0f;
    private static final float LINE_H_SMALL = 30f;

    public CampaignEndScreen(MainGame game, MissionManager missions) {
        this.game     = game;
        this.missions = missions;
    }

    @Override
    public void show() {
        shape     = new ShapeRenderer();
        batch     = new SpriteBatch();
        // Fontes reduzidas em relação à versão anterior para caber melhor no painel.
        fontBig   = new BitmapFont(); fontBig.getData().setScale(2.8f);
        fontMed   = new BitmapFont(); fontMed.getData().setScale(1.6f);
        fontSmall = new BitmapFont(); fontSmall.getData().setScale(1.15f);

        // Grava o resumo final da campanha no CSV de summary
        missions.flushSummary();
    }

    @Override
    public void render(float delta) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        int panelW = (int) (sw * 0.78f);
        int panelH = (int) (sh * 0.85f);
        int panelX = (sw - panelW) / 2;
        int panelY = (sh - panelH) / 2;
        float wrapW = panelW - 80;

        handleInput(panelH);

        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PANEL);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.setColor(verdictColor());
        shape.rect(panelX, panelY + panelH - 10, panelW, 10);
        shape.end();

        int cx = panelX + 40;

        // ---- Cabeçalho: nome da campanha, veredito e placar ----
        batch.begin();
        float y = panelY + panelH - 40;

        y -= TextUtils.drawWrapped(batch, fontSmall, missions.getCampaignName().toUpperCase(), cx, y, wrapW, COL_TEXT);
        y -= 14;

        y -= TextUtils.drawWrapped(batch, fontBig, missions.finalVerdict(), cx, y, wrapW, verdictColor());
        y -= 20;

        y -= TextUtils.drawWrapped(batch, fontMed, "Resultado final:", cx, y, wrapW, COL_TEXT);
        y -= 6;

        y -= TextUtils.drawWrapped(batch, fontMed, "Vitórias:  " + missions.victories, cx, y, wrapW, COL_VICTORY);
        y -= 4;
        y -= TextUtils.drawWrapped(batch, fontMed, "Derrotas:  " + missions.defeats, cx, y, wrapW, COL_DEFEAT);
        y -= 4;
        y -= TextUtils.drawWrapped(batch, fontMed, "Empates:   " + missions.draws, cx, y, wrapW, COL_DRAW);
        y -= 20;

        y -= TextUtils.drawWrapped(batch, fontMed,
            "Resultados das " + missions.resultsHistory.size() + " execuções:", cx, y, wrapW, COL_TEXT);
        y -= 8;
        batch.end();

        // ---- Lista de execuções: rolável e recortada para nunca sair da tela ----
        float listTop    = y;
        float footerH    = 56f;
        float listBottom = panelY + footerH;
        float listAreaH  = Math.max(0, listTop - listBottom);

        HdpiUtils.glScissor((int) panelX, (int) listBottom, (int) panelW, (int) listAreaH);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);

        batch.begin();
        float rowY = listTop + scrollOffset;
        for (int i = 0; i < missions.resultsHistory.size(); i++) {
            float drawY = rowY - i * LINE_H_SMALL;
            if (drawY < listBottom - LINE_H_SMALL || drawY > listTop + LINE_H_SMALL) continue;

            MissionManager.MissionResult r = missions.resultsHistory.get(i);
            String label = (i + 1) + ") " + labelFor(r);
            fontSmall.setColor(colorFor(r));
            fontSmall.draw(batch, label, cx, drawY);
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ---- Rodapé / instruções ----
        batch.begin();
        TextUtils.drawWrapped(batch, fontSmall,
            "UP/DOWN: rolar execuções   |   T: Tela Totalizadora (histórico)   |   ESC: sair",
            cx, panelY + 34, wrapW, COL_HINT);
        batch.end();
    }

    private void handleInput(int panelH) {
        float listAreaH = Math.max(0, panelH * 0.35f); // aproximação segura para o clamp de rolagem
        float maxScroll = Math.max(0, missions.resultsHistory.size() * LINE_H_SMALL - listAreaH);

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            scrollOffset = Math.min(maxScroll, scrollOffset + LINE_H_SMALL);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            scrollOffset = Math.max(0, scrollOffset - LINE_H_SMALL);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.setScreen(new CampaignTotalsScreen(game));
            return;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private Color verdictColor() {
        if (missions.victories > missions.defeats) return COL_VICTORY;
        if (missions.defeats > missions.victories) return COL_DEFEAT;
        return COL_DRAW;
    }

    /** Cor correspondente ao resultado de uma execução individual. */
    private Color colorFor(MissionManager.MissionResult r) {
        switch (r) {
            case VICTORY: return COL_VICTORY;
            case DEFEAT:  return COL_DEFEAT;
            default:      return COL_DRAW;
        }
    }

    /** Rótulo em texto do resultado de uma execução individual. */
    private String labelFor(MissionManager.MissionResult r) {
        switch (r) {
            case VICTORY: return "VITÓRIA";
            case DEFEAT:  return "DERROTA";
            default:      return "EMPATE";
        }
    }

    @Override public void resize(int w, int h) {
        shape.getProjectionMatrix().setToOrtho2D(0,0,w,h);
        batch.getProjectionMatrix().setToOrtho2D(0,0,w,h);
    }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        shape.dispose();
        batch.dispose();
        fontBig.dispose();
        fontMed.dispose();
        fontSmall.dispose();
    }
}
