package core;

/**
 * Classe Grid — o tabuleiro do simulador.
 * Dimensão configurável via arquivo externo (map_config.json).
 */
public class Grid {

    public int     width;
    public int     height;
    public Tile[][] tiles;

    /** Coordenada X do tile de destino dos players. */
    public int destX = -1;
    /** Coordenada Y do tile de destino dos players. */
    public int destY = -1;

    /**
     * Cria o grid com tamanho padrão 10×10.
     */
    public Grid() {
        this(10, 10);
    }

    /**
     * Cria o grid com tamanho personalizado.
     */
    public Grid(int width, int height) {
        this.width  = width;
        this.height = height;
        tiles = new Tile[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                tiles[x][y] = new Tile();
    }

    /** Marca o tile de destino dos players. */
    public void setDestination(int x, int y) {
        if (!isValid(x, y)) return;
        destX = x;
        destY = y;
        tiles[x][y].isDestination = true;
    }

    public boolean isValid(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}