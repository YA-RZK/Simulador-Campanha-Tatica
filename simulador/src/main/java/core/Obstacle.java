package core;

/**
 * Classe Obstacle — obstáculo estático no grid.
 * Bloqueia movimento. Pode ser configurado via map_config.json.
 */
public class Obstacle {

    public int x;
    public int y;

    public Obstacle(int gridX, int gridY, Grid grid) {
        this.x = gridX;
        this.y = gridY;
        if (grid.isValid(gridX, gridY)) {
            grid.tiles[gridX][gridY].hasObstacle = true;
        }
    }
}