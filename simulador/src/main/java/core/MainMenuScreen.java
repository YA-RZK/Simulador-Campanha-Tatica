package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * MainMenuScreen — tela inicial do simulador.
 *
 * Opções:
 *   [1] ou [ENTER] — Iniciar nova campanha
 *   [2] ou [T]     — Tela Totalizadora (histórico de execuções lido dos CSVs)
 *   [ESC]          — Sair
 */
public class MainMenuScreen implements Screen {

    private final MainGame game;

    private ShapeRenderer shape;
    private SpriteBatch   batch;
    private BitmapFont    fontTitle;
    private BitmapFont    fontOption;
    private BitmapFont    fontHint;

    private static final Color COL_BG     = new Color(0.04f, 0.04f, 0.08f, 1f);
    private static final Color COL_PANEL  = new Color(0.08f, 0.08f, 0.14f, 1f);
    private static final Color COL_TITLE  = new Color(0.85f, 0.85f, 1.00f, 1f);
    private static final Color COL_OPTION = new Color(0.55f, 0.85f, 1.00f, 1f);
    private static final Color COL_HINT   = new Color(0.45f, 0.45f, 0.55f, 1f);

    public MainMenuScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        shape      = new ShapeRenderer();
        batch      = new SpriteBatch();
        fontTitle  = new BitmapFont(); fontTitle.getData().setScale(2.8f);
        fontOption = new BitmapFont(); fontOption.getData().setScale(1.7f);
        fontHint   = new BitmapFont(); fontHint.getData().setScale(1.1f);
    }

    @Override
    public void render(float delta) {
        handleInput();

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int panelW = (int) (sw * 0.62f);
        int panelH = (int) (sh * 0.50f);
        int panelX = (sw - panelW) / 2;
        int panelY = (sh - panelH) / 2;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PANEL);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.setColor(COL_OPTION);
        shape.rect(panelX, panelY + panelH - 8, panelW, 8);
        shape.end();

        batch.begin();

        float wrapW = panelW - 60;
        int   cx    = panelX + 30;
        float y     = panelY + panelH - 40;

        y -= TextUtils.drawWrapped(batch, fontTitle, "SIMULADOR DE CAMPANHA", cx, y, wrapW, COL_TITLE);
        y -= 40;

        y -= TextUtils.drawWrapped(batch, fontOption, "[1] Iniciar nova campanha", cx, y, wrapW, COL_OPTION);
        y -= 20;
        y -= TextUtils.drawWrapped(batch, fontOption, "[2] Tela Totalizadora (histórico de execuções)", cx, y, wrapW, COL_OPTION);
        y -= 40;

        TextUtils.drawWrapped(batch, fontHint,
            "Pressione ENTER para iniciar ou T para ver o histórico. ESC para sair.",
            cx, y, wrapW, COL_HINT);

        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startCampaign();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.setScreen(new CampaignTotalsScreen(game));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    private void startCampaign() {
        MissionManager missions = MissionManager.load();
        game.setScreen(new GameScreen(game, missions));
    }

    @Override
    public void resize(int width, int height) {
        shape.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        shape.dispose();
        batch.dispose();
        fontTitle.dispose();
        fontOption.dispose();
        fontHint.dispose();
    }
}
