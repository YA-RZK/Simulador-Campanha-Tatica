package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * Classe GameScreen — tela principal de jogo.
 *
 * Novidades em relação à versão original:
 * - Recebe um {@link MissionManager} para saber qual mapa carregar.
 * - Conta turnos e encerra a missão quando {@code roundLimit} é atingido (empate).
 * - Ao fim da missão (vitória/derrota/empate) aguarda 2 s e transita para
 *   {@link MissionTransitionScreen} em vez de exigir fechar a janela.
 * - Exibe o número do turno atual e o limite configurado no painel de log.
 */
public class GameScreen implements Screen {

    // -------------------------------------------------------------------------
    // Campos de renderização
    // -------------------------------------------------------------------------

    ShapeRenderer shape;
    SpriteBatch   batch;
    BitmapFont    font;
    BitmapFont    fontLog;
    BitmapFont    fontSmall;

    // -------------------------------------------------------------------------
    // Estado do jogo
    // -------------------------------------------------------------------------

    Grid                grid;
    ArrayList<Player>   playerUnits;
    ArrayList<Enemy>    enemyUnits;
    ArrayList<Obstacle> obstacles;

    /** Controle de Velocidade */
    boolean isPlayerTurn = true;
    float   turnTimer    = 0f;
    float   turnDelay    = 0.15f;

    /** Turno atual (1 turno = fase dos players + fase dos inimigos). */
    int currentRound = 0;

    /** Limite de turnos carregado do JSON (0 = sem limite). */
    int roundLimit = 0;

    /** Mensagem de fim de jogo; null enquanto o jogo ainda corre. */
    String gameOverMessage = null;

    /** Resultado da missão (preenchido junto com gameOverMessage). */
    MissionManager.MissionResult missionResult = null;

    /** Timer pós-game-over antes de ir para a tela de transição. */
    float postGameTimer = 0f;
    static final float POST_GAME_DELAY = 2.0f;

    // -------------------------------------------------------------------------
    // Campanha
    // -------------------------------------------------------------------------

    private final MainGame       game;
    private final MissionManager missions;

    // -------------------------------------------------------------------------
    // Layout (calculado em show())
    // -------------------------------------------------------------------------

    int tileSize;
    int gridPixelW;
    int gridPixelH;
    int screenW;
    int screenH;
    int logX;

    // -------------------------------------------------------------------------
    // Painel de log
    // -------------------------------------------------------------------------

    private static final int        LOG_MAX_LINES = 32;
    private final Deque<String>     logLines      = new ArrayDeque<>();

    // -------------------------------------------------------------------------
    // Cores
    // -------------------------------------------------------------------------

    private static final Color COL_PLAYER_ALIVE = new Color(0.20f, 0.45f, 1.00f, 1);
    private static final Color COL_PLAYER_DEAD  = new Color(0.10f, 0.10f, 0.40f, 1);
    private static final Color COL_ENEMY_ALIVE  = new Color(1.00f, 0.22f, 0.22f, 1);
    private static final Color COL_ENEMY_DEAD   = new Color(0.40f, 0.10f, 0.10f, 1);
    private static final Color COL_OBSTACLE     = new Color(0.50f, 0.50f, 0.50f, 1);
    private static final Color COL_EMPTY        = new Color(0.88f, 0.88f, 0.88f, 1);
    private static final Color COL_DESTINATION  = new Color(0.10f, 0.80f, 0.30f, 1);
    private static final Color COL_DEST_REACHED = new Color(0.05f, 0.50f, 0.20f, 1);
    private static final Color COL_BACKGROUND   = new Color(0.12f, 0.12f, 0.15f, 1);
    private static final Color COL_PANEL_BG     = new Color(0.06f, 0.06f, 0.08f, 1);

    // =========================================================================
    // Construtores
    // =========================================================================

    /**
     * Construtor legado — cria um MissionManager padrão de um único mapa.
     * Mantém compatibilidade com código que instancie GameScreen diretamente.
     */
    public GameScreen() {
        this.game     = null;
        this.missions = MissionManager.loadDefault();
    }

    /**
     * Construtor principal — usado pelo MainGame e pelas telas de transição.
     *
     * @param game     referência ao Game para trocar de tela
     * @param missions gerenciador da campanha, já posicionado na missão atual
     */
    public GameScreen(MainGame game, MissionManager missions) {
        this.game     = game;
        this.missions = missions;
    }

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Override
    public void show() {
        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

        shape    = new ShapeRenderer();
        batch    = new SpriteBatch();
        font     = new BitmapFont(); font.getData().setScale(2.2f);
        fontLog  = new BitmapFont(); fontLog.getData().setScale(0.90f);
        fontSmall= new BitmapFont(); fontSmall.getData().setScale(0.80f);

        setupGame();
    }

    // =========================================================================
    // Configuração via MissionManager / MapConfig
    // =========================================================================

    void setupGame() {
        // Usa o mapa atual da campanha
        MapConfig cfg = missions.currentConfig();

        roundLimit   = cfg.roundLimit;
        currentRound = 0;

        // ---- Grid ----
        grid        = new Grid(cfg.grid.width, cfg.grid.height);
        playerUnits = new ArrayList<>();
        enemyUnits  = new ArrayList<>();
        obstacles   = new ArrayList<>();

        // ---- Layout responsivo ----
        int logPanelW = Math.max(240, screenW / 4);
        int availW    = screenW - logPanelW - 10;
        int availH    = screenH - 20;

        int tileSizeW = availW  / grid.width;
        int tileSizeH = availH  / grid.height;
        tileSize      = Math.max(20, Math.min(tileSizeW, tileSizeH));

        gridPixelW = tileSize * grid.width;
        gridPixelH = tileSize * grid.height;
        logX       = gridPixelW + 10;

        float fontScale = tileSize / 50f;
        font.getData().setScale(fontScale * 2.2f);
        fontSmall.getData().setScale(fontScale * 0.85f);

        // ---- Destino ----
        grid.setDestination(cfg.destination.x, cfg.destination.y);

        // ---- Título e header do log ----
        String title = cfg.missionTitle.isEmpty()
            ? "Missão " + missions.currentMissionNumber() + "/" + missions.totalMissions()
            : cfg.missionTitle + "  (" + missions.currentMissionNumber() + "/" + missions.totalMissions() + ")";
        log("══ " + title + " ══");
        log("Destino: (" + cfg.destination.x + "," + cfg.destination.y + ")");
        if (roundLimit > 0) log("Limite de turnos: " + roundLimit);

        // ---- Players ----
        for (MapConfig.PlayerCfg pc : cfg.players) {
            if (!grid.isValid(pc.x, pc.y)) continue;
            Player p = new Player(pc.x, pc.y, pc.name, pc.hp, pc.damage, pc.evasion, pc.fleeChance);
            grid.tiles[pc.x][pc.y].unit = p;
            playerUnits.add(p);
        }

        // ---- Inimigos ----
        for (MapConfig.EnemyCfg ec : cfg.enemies) {
            if (!grid.isValid(ec.x, ec.y)) continue;
            Enemy e = new Enemy(ec.x, ec.y, ec.name, ec.hp, ec.damage,
                                ec.detectionRadius, ec.aggressiveness);
            grid.tiles[ec.x][ec.y].unit = e;
            enemyUnits.add(e);
        }

        // ---- Obstáculos ----
        for (MapConfig.ObstacleCfg oc : cfg.obstacles) {
            obstacles.add(new Obstacle(oc.x, oc.y, grid));
        }

        log("Mapa: " + grid.width + "x" + grid.height +
            " | " + playerUnits.size() + " soldados | " +
            enemyUnits.size() + " inimigos | " +
            obstacles.size() + " obstáculos");
    }

    // =========================================================================
    // Log
    // =========================================================================

    public void log(String message) {
        logLines.addFirst(message);
        if (logLines.size() > LOG_MAX_LINES) logLines.removeLast();
    }

    // =========================================================================
    // Lógica de turnos
    // =========================================================================

    void playerTurn() {
        for (Player player : playerUnits) {
            player.takeTurn(grid, enemyUnits, this);
        }
        isPlayerTurn = false;
    }

    void enemyTurn() {
        for (Enemy enemy : enemyUnits) {
            enemy.takeTurn(grid, playerUnits, this);
        }
        isPlayerTurn = true;
        currentRound++;  // 1 turno completo = players + inimigos
    }

    /**
     * Verifica condições de fim de missão.
     * Ordem de prioridade: vitória > derrota > empate por limite de turnos.
     *
     * @return mensagem descritiva, ou null se o jogo ainda não terminou
     */
    String checkGameOver() {
        long playersAlive  = playerUnits.stream().filter(p -> p.alive).count();
        long playersAtDest = playerUnits.stream().filter(p -> p.reachedDestination).count();
        boolean anyEnemyAlive = enemyUnits.stream().anyMatch(e -> e.alive);

        if (playersAtDest > 0) {
            missionResult = MissionManager.MissionResult.VICTORY;
            return playersAtDest + " soldado(s) chegaram ao destino! VITÓRIA!";
        }
        if (playersAlive == 0) {
            missionResult = MissionManager.MissionResult.DEFEAT;
            return "Todos os soldados foram eliminados. Derrota!";
        }
        if (!anyEnemyAlive) {
            missionResult = MissionManager.MissionResult.VICTORY;
            return "Todos os inimigos eliminados! Caminho livre — VITÓRIA!";
        }
        if (roundLimit > 0 && currentRound >= roundLimit) {
            missionResult = MissionManager.MissionResult.DRAW;
            return "Limite de " + roundLimit + " turnos atingido. Empate!";
        }
        return null;
    }

    // =========================================================================
    // Renderização
    // =========================================================================

    @Override
    public void render(float delta) {

        // ---- Lógica de turno (só enquanto o jogo corre) ----
        if (gameOverMessage == null) {
            turnTimer += delta;
            if (turnTimer >= turnDelay) {
                turnTimer = 0f;
                if (isPlayerTurn) playerTurn();
                else              enemyTurn();
                gameOverMessage = checkGameOver();
                if (gameOverMessage != null) log("=== " + gameOverMessage + " ===");
            }
        } else {
            // Aguarda um pouco e depois vai para a tela de transição
            postGameTimer += delta;
            if (postGameTimer >= POST_GAME_DELAY) {
                goToTransition();
                return;
            }
        }

        // ---- Limpa tela ----
        Gdx.gl.glClearColor(COL_BACKGROUND.r, COL_BACKGROUND.g, COL_BACKGROUND.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Fundo painel de log
        shape.setColor(COL_PANEL_BG);
        shape.rect(logX - 5, 0, screenW - (logX - 5), screenH);

        // ---- Grid ----
        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                Tile tile = grid.tiles[x][y];

                Color c;
                if (tile.hasObstacle) {
                    c = COL_OBSTACLE;
                } else if (tile.unit != null) {
                    Unit u = tile.unit;
                    if (u.isPlayer) c = u.alive ? COL_PLAYER_ALIVE : COL_PLAYER_DEAD;
                    else            c = u.alive ? COL_ENEMY_ALIVE   : COL_ENEMY_DEAD;
                } else if (tile.isDestination) {
                    c = COL_DESTINATION;
                } else {
                    c = COL_EMPTY;
                }

                shape.setColor(c);
                shape.rect(x * tileSize + 1, y * tileSize + 1, tileSize - 2, tileSize - 2);

                // Marcador interno no destino
                if (tile.isDestination && tile.unit == null) {
                    shape.setColor(COL_DEST_REACHED);
                    shape.rect(x * tileSize + tileSize / 4, y * tileSize + tileSize / 4,
                               tileSize / 2, tileSize / 2);
                }
            }
        }

        // ---- HP bars nos players vivos ----
        for (Player p : playerUnits) {
            if (!p.alive || p.reachedDestination) continue;
            float hpRatio = (float) p.hp / p.maxHp;
            int barW = tileSize - 4;
            int barH = Math.max(3, tileSize / 8);
            int bx   = p.x * tileSize + 2;
            int by   = p.y * tileSize + tileSize - barH - 2;

            shape.setColor(Color.DARK_GRAY);
            shape.rect(bx, by, barW, barH);
            shape.setColor(hpRatio > 0.5f ? Color.GREEN : hpRatio > 0.25f ? Color.YELLOW : Color.RED);
            shape.rect(bx, by, (int)(barW * hpRatio), barH);
        }

        shape.end();

        // ---- Texto ----
        batch.begin();

        int logLineH = Math.max(14, screenH / 38);
        int topY     = screenH - 8;

        // Cabeçalho do painel
        fontLog.setColor(Color.YELLOW);
        fontLog.draw(batch, "═══ COMBATE LOG ═══", logX, topY);

        // Turno e round
        fontLog.setColor(Color.CYAN);
        String turnLabel = isPlayerTurn ? "▶ Fase: Soldados" : "▶ Fase: Inimigos";
        String roundInfo = "  Turno " + currentRound + (roundLimit > 0 ? "/" + roundLimit : "");
        fontLog.draw(batch, turnLabel + roundInfo, logX, topY - logLineH);

        // Contadores
        long alive   = playerUnits.stream().filter(p -> p.alive).count();
        long arrived = playerUnits.stream().filter(p -> p.reachedDestination).count();
        long enemies = enemyUnits.stream().filter(e -> e.alive).count();
        fontLog.setColor(COL_PLAYER_ALIVE);
        fontLog.draw(batch,
            "Soldados: " + alive + "/" + playerUnits.size() + " vivos  ★" + arrived + " chegaram",
            logX, topY - logLineH * 2);
        fontLog.setColor(COL_ENEMY_ALIVE);
        fontLog.draw(batch, "Inimigos: " + enemies + "/" + enemyUnits.size() + " vivos",
                     logX, topY - logLineH * 3);

        // Placar da campanha
        fontLog.setColor(new Color(0.60f, 0.60f, 0.75f, 1f));
        fontLog.draw(batch, missions.scoreString(), logX, topY - logLineH * 4);

        // Separador
        fontLog.setColor(Color.DARK_GRAY);
        fontLog.draw(batch, "─────────────────────", logX, topY - logLineH * 5);

        // Linhas de log
        int lineY = topY - logLineH * 6;
        for (String line : logLines) {
            if (lineY < logLineH) break;
            if (line.startsWith("★"))          fontLog.setColor(Color.GREEN);
            else if (line.startsWith("═══") || line.startsWith("══"))
                                               fontLog.setColor(Color.YELLOW);
            else if (line.startsWith("==="))   fontLog.setColor(Color.YELLOW);
            else if (line.contains("esquivou"))fontLog.setColor(Color.CYAN);
            else if (line.contains("eliminou"))fontLog.setColor(Color.ORANGE);
            else if (line.contains("Empate"))  fontLog.setColor(COL_ENEMY_ALIVE);
            else                               fontLog.setColor(new Color(0.9f, 0.55f, 0.55f, 1));
            fontLog.draw(batch, line, logX, lineY);
            lineY -= logLineH;
        }

        // Legenda
        drawLegend();

        // Mensagem de game over sobreposta ao grid
        if (gameOverMessage != null) {
            boolean victory = (missionResult == MissionManager.MissionResult.VICTORY);
            boolean draw    = (missionResult == MissionManager.MissionResult.DRAW);
            font.setColor(victory ? Color.YELLOW : draw ? Color.ORANGE : Color.RED);
            font.draw(batch, gameOverMessage, 20, gridPixelH / 2f + 30);

            // Barra de progresso do delay pré-transição
            float progress = postGameTimer / POST_GAME_DELAY;
            fontSmall.setColor(Color.WHITE);
            fontSmall.draw(batch, "Próxima tela em breve...", 20, gridPixelH / 2f - 10);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(new Color(0.2f, 0.2f, 0.3f, 0.8f));
            shape.rect(20, gridPixelH / 2f - 30, gridPixelW - 40, 10);
            shape.setColor(victory ? Color.GREEN : draw ? Color.ORANGE : Color.RED);
            shape.rect(20, gridPixelH / 2f - 30, (gridPixelW - 40) * progress, 10);
            shape.end();
        }

        batch.end();
    }

    /** Transita para a tela de transição de missão. */
    private void goToTransition() {
        if (game == null) {
            // Fallback se instanciado sem MainGame (modo legado)
            Gdx.app.exit();
            return;
        }
        missions.advance(missionResult, gameOverMessage);
        game.setScreen(new MissionTransitionScreen(game, missions, missionResult, gameOverMessage));
    }

    /** Desenha legenda colorida. */
    private void drawLegend() {
        int ly = gridPixelH + 4;
        if (ly + 14 > screenH) {
            ly = 5;
            int lx = logX;
            fontSmall.setColor(COL_PLAYER_ALIVE); fontSmall.draw(batch, "■ Soldado", lx,       ly + 14);
            fontSmall.setColor(COL_ENEMY_ALIVE);  fontSmall.draw(batch, "■ Inimigo", lx + 90,  ly + 14);
            fontSmall.setColor(COL_DESTINATION);  fontSmall.draw(batch, "■ Destino", lx + 180, ly + 14);
            fontSmall.setColor(COL_OBSTACLE);     fontSmall.draw(batch, "■ Muro",    lx,        ly);
        } else {
            fontSmall.setColor(COL_PLAYER_ALIVE); fontSmall.draw(batch, "■ Soldado", 2,   ly + 14);
            fontSmall.setColor(COL_ENEMY_ALIVE);  fontSmall.draw(batch, "■ Inimigo", 80,  ly + 14);
            fontSmall.setColor(COL_DESTINATION);  fontSmall.draw(batch, "■ Destino", 160, ly + 14);
            fontSmall.setColor(COL_OBSTACLE);     fontSmall.draw(batch, "■ Muro",    240, ly + 14);
        }
    }

    // =========================================================================
    // Resize
    // =========================================================================

    @Override
    public void resize(int width, int height) {
        screenW = width;
        screenH = height;
        if (grid == null) return;

        int logPanelW = Math.max(240, screenW / 4);
        int availW    = screenW - logPanelW - 10;
        int availH    = screenH - 20;

        int tileSizeW = availW  / grid.width;
        int tileSizeH = availH  / grid.height;
        tileSize      = Math.max(20, Math.min(tileSizeW, tileSizeH));

        gridPixelW = tileSize * grid.width;
        gridPixelH = tileSize * grid.height;
        logX       = gridPixelW + 10;

        shape.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    // =========================================================================
    // Ciclo de vida obrigatório
    // =========================================================================

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        if (batch != null) batch.dispose();
        if (font != null)  font.dispose();
        if (fontLog != null) fontLog.dispose();
        if (fontSmall != null) fontSmall.dispose();
    }
}
