package core;

import java.util.ArrayList;

/**
 * Classe Player — unidade do jogador (time azul).
 *
 * Objetivo: chegar ao tile de destino (grid.destX, grid.destY)
 * sem ser morto pelos inimigos.
 *
 * Estratégia por turno (prioridade):
 * 1. Se estiver no destino → não faz mais nada (vitória individual).
 * 2. Inimigo adjacente → com fleeChance% de chance foge, resto contra-ataca.
 * 3. Inimigo próximo (raio de perigo) → tenta desviar/fugir em direção ao destino.
 * 4. Sem ameaças → avança em direção ao destino.
 * 5. Bloqueado → movimento aleatório.
 *
 * Parâmetros configuráveis via JSON (em cada player):
 *   evasion    : chance 0–1 de esquivar de ataques recebidos   (padrão 0.20)
 *   fleeChance : chance 0–1 de fugir ao invés de lutar quando adjacente (padrão 0.70)
 */
public class Player extends Unit {

    private static final int    DEFAULT_HP          = 15;
    private static final int    DEFAULT_DAMAGE      = 4;
    private static final double DEFAULT_EVASION     = 0.20;
    private static final double DEFAULT_FLEE_CHANCE = 0.70;
    private static final int    DANGER_RADIUS       = 2;

    /**
     * Chance (0.0–1.0) de fugir quando um inimigo está adjacente.
     * 0.0 → sempre luta; 1.0 → sempre foge.
     */
    private final double fleeChance;

    /** True se este player já alcançou o destino. */
    public boolean reachedDestination = false;

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public Player(int gridX, int gridY) {
        this(gridX, gridY, "Jogador", DEFAULT_HP, DEFAULT_DAMAGE,
             DEFAULT_EVASION, DEFAULT_FLEE_CHANCE);
    }

    public Player(int gridX, int gridY, String name) {
        this(gridX, gridY, name, DEFAULT_HP, DEFAULT_DAMAGE,
             DEFAULT_EVASION, DEFAULT_FLEE_CHANCE);
    }

    /** Construtor legado sem fleeChance (mantém compatibilidade). */
    public Player(int gridX, int gridY, String name, int hp, int damage, double evasion) {
        this(gridX, gridY, name, hp, damage, evasion, DEFAULT_FLEE_CHANCE);
    }

    /** Construtor completo. */
    public Player(int gridX, int gridY, String name, int hp, int damage,
                  double evasion, double fleeChance) {
        super(gridX, gridY, true, hp, damage, name);
        this.evasion    = Math.max(0.0, Math.min(1.0, evasion));
        this.fleeChance = Math.max(0.0, Math.min(1.0, fleeChance));
    }

    // -------------------------------------------------------------------------
    // Turno
    // -------------------------------------------------------------------------

    public void takeTurn(Grid grid, ArrayList<Enemy> enemyUnits, GameScreen gs) {
        if (!alive) return;
        if (reachedDestination) return;

        // 1. Já está no destino?
        if (x == grid.destX && y == grid.destY) {
            reachedDestination = true;
            gs.log("★ " + name + " chegou ao destino!");
            return;
        }

        Enemy closest = findClosestLivingEnemy(enemyUnits);
        boolean dangerNear = closest != null && distanceTo(closest) <= DANGER_RADIUS;

        // 2. Inimigo adjacente → fugir ou lutar conforme fleeChance
        if (closest != null && isAdjacentTo(closest)) {
            if (Math.random() < fleeChance) {
                fleeAndAdvance(closest, grid, gs);
            } else {
                attack(closest, gs);
            }
            return;
        }

        // 3. Perigo próximo → desviar em direção ao destino
        if (dangerNear) {
            if (!fleeAndAdvance(closest, grid, gs)) {
                moveRandom(grid);
            }
            return;
        }

        // 4. Caminho livre → avançar direto ao destino
        if (!stepTowardDestination(grid)) {
            moveRandom(grid);
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares de movimento
    // -------------------------------------------------------------------------

    private boolean fleeAndAdvance(Enemy threat, Grid grid, GameScreen gs) {
        int bestScore = Integer.MIN_VALUE;
        int bestDx = 0, bestDy = 0;
        
        int currentDistToDest = distanceTo(grid.destX, grid.destY);
        int currentDistToThreat = distanceTo(threat);
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = x + dx;
                int ny = y + dy;

                if (!grid.isValid(nx, ny)) continue;
                if (!grid.tiles[nx][ny].isEmpty()) continue;

                int distToThreat = Math.max(Math.abs(nx - threat.x), Math.abs(ny - threat.y));
                int distToDest = Math.max(Math.abs(nx - grid.destX), Math.abs(ny - grid.destY));

                // Evita ficar indo e voltando sem progresso
                if (distToDest > currentDistToDest && distToThreat <= currentDistToThreat + 1) {
                    continue;
                }

                int score = 0;

                // Prioriza continuar chegando ao destino
                score += (currentDistToDest - distToDest) * 4;

                // Ainda valoriza se afastar do inimigo
                score += (distToThreat - currentDistToThreat) * 2;

                if (score > bestScore) {
                    bestScore = score;
                    bestDx = dx;
                    bestDy = dy;
                }
            }
        }

        if (bestScore > Integer.MIN_VALUE) {
            return move(x + bestDx, y + bestDy, grid);
        }

        return false;
    }

    private boolean stepTowardDestination(Grid grid) {
        int dx = Integer.signum(grid.destX - x);
        int dy = Integer.signum(grid.destY - y);
        if (dx == 0 && dy == 0) return false;

        if (dx != 0 && dy != 0 && move(x + dx, y + dy, grid)) return true;
        if (dx != 0 && move(x + dx, y, grid))                  return true;
        if (dy != 0 && move(x, y + dy, grid))                  return true;

        if (dx != 0 && move(x + dx, y + 1, grid)) return true;
        if (dx != 0 && move(x + dx, y - 1, grid)) return true;
        if (dy != 0 && move(x + 1, y + dy, grid)) return true;
        if (dy != 0 && move(x - 1, y + dy, grid)) return true;

        return false;
    }
    // Foi alterado para não ficar escolhendo voltar ou ficar parado
    private void moveRandom(Grid grid) {
        int bestScore = Integer.MIN_VALUE;
        int bestDx = 0, bestDy = 0;

        int currentDistToDest = distanceTo(grid.destX, grid.destY);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = x + dx;
                int ny = y + dy;

                if (!grid.isValid(nx, ny)) continue;
                if (!grid.tiles[nx][ny].isEmpty()) continue;

                int distToDest = Math.max(Math.abs(nx - grid.destX), Math.abs(ny - grid.destY));

                // Evita movimento aleatório que joga a unidade para trás
                if (distToDest > currentDistToDest) continue;

                int score = currentDistToDest - distToDest;

                if (score > bestScore) {
                    bestScore = score;
                    bestDx = dx;
                    bestDy = dy;
                    }
                }
            }
        if (bestScore > Integer.MIN_VALUE) {
            move(x + bestDx, y + bestDy, grid);
        }
    }

    private Enemy findClosestLivingEnemy(ArrayList<Enemy> enemies) {
        Enemy closest = null;
        int   minDist = Integer.MAX_VALUE;
        for (Enemy e : enemies) {
            if (!e.alive) continue;
            int d = distanceTo(e);
            if (d < minDist) { minDist = d; closest = e; }
        }
        return closest;
    }
}
