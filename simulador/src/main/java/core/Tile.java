package core;

/**
 * Classe Tile — célula individual do grid.
 *
 * Pode ser: vazia, obstáculo, ocupada por unidade, ou destino dos players.
 */
public class Tile {

    public boolean hasObstacle;
    public boolean isDestination;
    public Unit    unit;

    public Tile() {
        this.hasObstacle   = false;
        this.isDestination = false;
        this.unit          = null;
    }

    /**
     * Retorna true se a tile está livre para receber uma unidade viva.
     * Unidades mortas não bloqueiam a passagem.
     * O destino pode ser ocupado (é o objetivo!).
     */
    public boolean isEmpty() {
        return !hasObstacle && (unit == null || !unit.alive);
    }
}