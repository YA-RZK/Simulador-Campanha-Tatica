package core;

/**
 * Classe Unit — unidade base do simulador tático.
 *
 * Inclui evasão: chance de esquivar de ataques (útil para Players
 * que priorizam sobreviver e chegar ao destino).
 */
public class Unit {

    public int     x, y;
    public int     hp, maxHp, attackDamage;
    public String  name;
    public boolean isPlayer;
    public boolean alive = true;

    /**
     * Chance de desviar de um ataque (0.0 a 1.0).
     * Apenas Players usam isso por padrão; Enemies têm 0.
     */
    public double evasion = 0.0;

    public Unit(int x, int y, boolean isPlayer, int hp, int attackDamage, String name) {
        this.x            = x;
        this.y            = y;
        this.isPlayer     = isPlayer;
        this.hp           = hp;
        this.maxHp        = hp;
        this.attackDamage = attackDamage;
        this.name         = name;
    }

    // -------------------------------------------------------------------------
    // Movimentação
    // -------------------------------------------------------------------------

    /**
     * Tenta mover para (newX, newY).
     * Unidades mortas não se movem. Tiles com obstáculo ou unidade viva bloqueiam.
     */
    public boolean move(int newX, int newY, Grid grid) {
        if (!alive) return false;
        if (!grid.isValid(newX, newY)) return false;

        Tile dest = grid.tiles[newX][newY];
        if (dest.hasObstacle) return false;
        if (dest.unit != null && dest.unit.alive) return false;

        grid.tiles[x][y].unit = null;
        x = newX;
        y = newY;
        grid.tiles[x][y].unit = this;
        return true;
    }

    // -------------------------------------------------------------------------
    // Combate
    // -------------------------------------------------------------------------

    /**
     * Ataca o alvo. Suporta evasão do alvo.
     * Registra o evento no log da tela.
     */
    public void attack(Unit target, GameScreen gameScreen) {
        if (!alive || !target.alive) return;

        // Checar evasão
        if (target.evasion > 0 && Math.random() < target.evasion) {
            gameScreen.log(target.name + " esquivou do ataque de " + name + "!");
            return;
        }

        target.hp -= this.attackDamage;

        if (target.hp <= 0) {
            target.hp    = 0;
            target.alive = false;
            gameScreen.log(name + " eliminou " + target.name + "!");
        } else {
            String msg = String.format("%s -> %s | dano:%d HP:%d/%d",
                name, target.name, attackDamage, target.hp, target.maxHp);
            gameScreen.log(msg);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------

    /** Distância de Chebyshev (diagonais contam como 1). */
    public int distanceTo(Unit other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    public int distanceTo(int tx, int ty) {
        return Math.max(Math.abs(x - tx), Math.abs(y - ty));
    }

    /** Verdadeiro se o alvo está em uma das 8 células adjacentes. */
    public boolean isAdjacentTo(Unit other) {
        return distanceTo(other) == 1;
    }

    public boolean isAlive() {
        return alive && hp > 0;
    }

    @Override
    public String toString() {
        return String.format("%s(%d,%d) HP:%d/%d%s",
            name, x, y, hp, maxHp, alive ? "" : " [MORTO]");
    }
}