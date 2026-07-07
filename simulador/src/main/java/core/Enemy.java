package core;

import java.util.ArrayList;

/**
 * Classe Enemy — inimigo controlado pela IA.
 *
 * Comportamento por turno:
 * 1. Jogador adjacente → ataca.
 * 2. Jogador dentro do raio de detecção → persegue.
 * 3. Fora do raio → patrulha aleatória (salvo se aggressiveness decidir perseguir).
 *
 * Parâmetros configuráveis via JSON:
 *   detectionRadius  : distância de detecção do player (padrão 3)
 *   aggressiveness   : chance 0.0–1.0 de perseguir mesmo fora do raio (padrão 0.0)
 */
public class Enemy extends Unit {

    private static final int    DEFAULT_HP             = 10;
    private static final int    DEFAULT_DAMAGE         = 3;
    private static final int    DEFAULT_DETECTION      = 3;
    private static final double DEFAULT_AGGRESSIVENESS = 0.0;

    private final int    detectionRadius;
    private final double aggressiveness;

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public Enemy(int gridX, int gridY) {
        this(gridX, gridY, "Inimigo", DEFAULT_HP, DEFAULT_DAMAGE,
             DEFAULT_DETECTION, DEFAULT_AGGRESSIVENESS);
    }

    public Enemy(int gridX, int gridY, String name) {
        this(gridX, gridY, name, DEFAULT_HP, DEFAULT_DAMAGE,
             DEFAULT_DETECTION, DEFAULT_AGGRESSIVENESS);
    }

    /** Construtor legado sem aggressiveness (mantém compatibilidade). */
    public Enemy(int gridX, int gridY, String name, int hp, int damage, int detectionRadius) {
        this(gridX, gridY, name, hp, damage, detectionRadius, DEFAULT_AGGRESSIVENESS);
    }

    /** Construtor completo. */
    public Enemy(int gridX, int gridY, String name, int hp, int damage,
                 int detectionRadius, double aggressiveness) {
        super(gridX, gridY, false, hp, damage, name);
        this.detectionRadius = detectionRadius;
        this.aggressiveness  = Math.max(0.0, Math.min(1.0, aggressiveness));
        this.evasion         = 0.0;
    }

    // -------------------------------------------------------------------------
    // Turno
    // -------------------------------------------------------------------------

    public void takeTurn(Grid grid, ArrayList<Player> playerUnits, GameScreen gs) {
        if (!alive) return;

        Player target = findClosestActivePlayer(playerUnits);
        if (target == null) { moveRandom(grid); return; }

        boolean inRange   = distanceTo(target) <= detectionRadius;
        boolean willChase = inRange || (Math.random() < aggressiveness);

        if (willChase) {
            if (isAdjacentTo(target)) attack(target, gs);
            else                      stepToward(target, grid);
        } else {
            moveRandom(grid);
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private Player findClosestActivePlayer(ArrayList<Player> players) {
        Player closest = null;
        int    minDist = Integer.MAX_VALUE;
        for (Player p : players) {
            if (!p.alive || p.reachedDestination) continue;
            int d = distanceTo(p);
            if (d < minDist) { minDist = d; closest = p; }
        }
        return closest;
    }

    private void stepToward(Unit target, Grid grid) {
        int dx = Integer.signum(target.x - x);
        int dy = Integer.signum(target.y - y);
        if (move(x + dx, y + dy, grid)) return;
        if (dx != 0 && move(x + dx, y, grid)) return;
        if (dy != 0 && move(x, y + dy, grid)) return;
    }

    private void moveRandom(Grid grid) {
        int dx = (int)(Math.random() * 3) - 1;
        int dy = (int)(Math.random() * 3) - 1;
        move(x + dx, y + dy, grid);
    }
}
