package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Tela de transição exibida entre missões.
 *
 * Exibe:
 * - Resultado da missão que acabou (vitória / derrota / empate)
 * - Placar acumulado da campanha
 * - Título da próxima missão
 * - Contagem regressiva (5 s) antes de carregar a próxima tela
 *
 * Todo texto é desenhado com quebra de linha automática (via
 * {@link TextUtils}), e o layout empilha cada bloco usando a altura real
 * ocupada pelo anterior — assim nenhuma linha ultrapassa a largura do
 * painel nem se sobrepõe a outra, mesmo com títulos ou mensagens longas.
 *
 * Ao fim da campanha exibe o resultado global e fecha o app após a contagem.
 */
public class MissionTransitionScreen implements Screen {

    // -------------------------------------------------------------------------
    // Dependências
    // -------------------------------------------------------------------------

    private final MainGame              game;
    private final MissionManager        missions;
    private final MissionManager.MissionResult lastResult;
    private final String                missionResultMsg;

    // -------------------------------------------------------------------------
    // Renderização
    // -------------------------------------------------------------------------

    private ShapeRenderer shape;
    private SpriteBatch   batch;
    private BitmapFont    fontBig;
    private BitmapFont    fontMed;
    private BitmapFont    fontSmall;

    // -------------------------------------------------------------------------
    // Contagem regressiva
    // -------------------------------------------------------------------------

    private static final float COUNTDOWN_SECONDS = 5f;
    private float elapsed = 0f;

    // -------------------------------------------------------------------------
    // Cores
    // -------------------------------------------------------------------------

    private static final Color COL_BG       = new Color(0.06f, 0.06f, 0.10f, 1f);
    private static final Color COL_PANEL    = new Color(0.10f, 0.10f, 0.16f, 1f);
    private static final Color COL_VICTORY  = new Color(0.20f, 0.90f, 0.40f, 1f);
    private static final Color COL_DEFEAT   = new Color(0.95f, 0.25f, 0.25f, 1f);
    private static final Color COL_DRAW     = new Color(0.90f, 0.80f, 0.20f, 1f);
    private static final Color COL_NEXT     = new Color(0.40f, 0.80f, 1.00f, 1f);
    private static final Color COL_TEXT     = new Color(0.70f, 0.70f, 0.80f, 1f);
    private static final Color COL_SEP      = new Color(0.35f, 0.35f, 0.45f, 1f);

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------

    /**
     * @param game             referência ao MainGame para trocar de tela
     * @param missions         MissionManager já avançado para o próximo índice
     * @param lastResult       resultado da missão recém-concluída
     * @param missionResultMsg mensagem descritiva do resultado (ex.: "2 soldados chegaram!")
     */
    public MissionTransitionScreen(MainGame game,
                                   MissionManager missions,
                                   MissionManager.MissionResult lastResult,
                                   String missionResultMsg) {
        this.game             = game;
        this.missions         = missions;
        this.lastResult       = lastResult;
        this.missionResultMsg = missionResultMsg;
    }

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------

    @Override
    public void show() {
        shape    = new ShapeRenderer();
        batch    = new SpriteBatch();
        // Fontes ligeiramente reduzidas em relação à versão anterior, para
        // caber com folga junto com a quebra de linha automática.
        fontBig  = new BitmapFont(); fontBig.getData().setScale(3.0f);
        fontMed  = new BitmapFont(); fontMed.getData().setScale(1.8f);
        fontSmall= new BitmapFont(); fontSmall.getData().setScale(1.2f);
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // ---- Contagem regressiva ----
        float remaining = COUNTDOWN_SECONDS - elapsed;
        if (remaining <= 0) {
            goToNextScreen();
            return;
        }

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // Fundo
        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Painel central
        int panelW = (int)(sw * 0.78f);
        int panelH = (int)(sh * 0.82f);
        int panelX = (sw - panelW) / 2;
        int panelY = (sh - panelH) / 2;
        float wrapW = panelW - 60;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PANEL);
        shape.rect(panelX, panelY, panelW, panelH);

        // Barra colorida no topo do painel
        Color barColor = resultColor();
        shape.setColor(barColor);
        shape.rect(panelX, panelY + panelH - 8, panelW, 8);

        // Barra de progresso da contagem (bottom do painel)
        float progress = elapsed / COUNTDOWN_SECONDS;
        shape.setColor(new Color(0.25f, 0.25f, 0.35f, 1f));
        shape.rect(panelX, panelY, panelW, 12);
        shape.setColor(barColor);
        shape.rect(panelX, panelY, panelW * (1f - progress), 12);

        shape.end();

        // ---- Texto ----
        batch.begin();

        int cx   = panelX + 30;
        float y  = panelY + panelH - 30;

        // Missão concluída — número (com quebra de linha, pode ter nome de campanha longo)
        y -= TextUtils.drawWrapped(batch, fontSmall,
            "MISSÃO " + (missions.currentMissionNumber() - 1) + " / " + missions.totalMissions()
                + " — " + missions.getCampaignName(),
            cx, y, wrapW, COL_TEXT);
        y -= 14;

        // Resultado
        y -= TextUtils.drawWrapped(batch, fontBig, resultLabel(), cx, y, wrapW, resultColor());
        y -= 10;

        // Descrição do resultado
        y -= TextUtils.drawWrapped(batch, fontMed, missionResultMsg, cx, y, wrapW, Color.WHITE);
        y -= 16;

        // Separador
        y -= TextUtils.drawWrapped(batch, fontSmall,
            "────────────────────────────────────────────────", cx, y, wrapW, COL_SEP);
        y -= 16;

        // Placar
        y -= TextUtils.drawWrapped(batch, fontMed, "Placar da campanha:", cx, y, wrapW, COL_TEXT);
        y -= 6;

        y -= TextUtils.drawWrapped(batch, fontSmall,
            "V " + missions.victories + "   D " + missions.defeats + "   E " + missions.draws,
            cx, y, wrapW, Color.WHITE);
        y -= 16;

        y -= TextUtils.drawWrapped(batch, fontSmall,
            "────────────────────────────────────────────────", cx, y, wrapW, COL_SEP);
        y -= 16;

        if (!missions.isFinished()) {
            MapConfig nextCfg = missions.currentConfig();
            String nextTitle = nextCfg.missionTitle.isEmpty()
                ? "Missão " + missions.currentMissionNumber()
                : nextCfg.missionTitle;

            y -= TextUtils.drawWrapped(batch, fontMed, "A seguir: " + nextTitle, cx, y, wrapW, COL_NEXT);
        } else {
            y -= TextUtils.drawWrapped(batch, fontMed, missions.finalVerdict(), cx, y, wrapW, resultColor());
        }

        // Contador (fixado próximo à base do painel, não empilhado com o resto)
        int secs = (int) Math.ceil(remaining);
        TextUtils.drawWrapped(batch, fontSmall, "Continuando em " + secs + "s...",
            cx, panelY + 30, wrapW, new Color(0.55f, 0.55f, 0.65f, 1f));

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Color resultColor() {
        switch (lastResult) {
            case VICTORY: return COL_VICTORY;
            case DEFEAT:  return COL_DEFEAT;
            default:      return COL_DRAW;
        }
    }

    private String resultLabel() {
        switch (lastResult) {
            case VICTORY: return "VITÓRIA!";
            case DEFEAT:  return "DERROTA";
            default:      return "EMPATE";
        }
    }

    private void goToNextScreen() {
        if (missions.isFinished()) {
            // Campanha encerrada — mostra tela final de campanha
            game.setScreen(new CampaignEndScreen(game, missions));
        } else {
            // Próxima missão
            game.setScreen(new GameScreen(game, missions));
        }
    }

    // -------------------------------------------------------------------------
    // Ciclo de vida obrigatório
    // -------------------------------------------------------------------------

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
