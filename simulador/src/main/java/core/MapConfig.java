package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe MapConfig — carrega a configuração do mapa e das unidades
 * a partir de um arquivo JSON externo.
 *
 * Estrutura esperada do JSON:
 * <pre>
 * {
 *   "missionTitle": "Missão 1 — Infiltração",
 *   "roundLimit":   30,
 *   "grid":        { "width": 12, "height": 12 },
 *   "destination": { "x": 10, "y": 10 },
 *   "spawn":       { "x": 1,  "y": 1  },
 *   "players": [
 *     {
 *       "x":1, "y":1, "name":"Soldado 1",
 *       "hp":20, "damage":5,
 *       "evasion":0.25,
 *       "fleeChance":0.60
 *     }
 *   ],
 *   "enemies": [
 *     {
 *       "x":6, "y":5, "name":"Guarda 1",
 *       "hp":12, "damage":4,
 *       "detectionRadius":4,
 *       "aggressiveness":0.5
 *     }
 *   ],
 *   "obstacles": [
 *     { "x":3, "y":3 }
 *   ]
 * }
 * </pre>
 *
 * Campos dos players:
 *   evasion    : chance (0–1) de esquivar de ataques recebidos   (padrão 0.20)
 *   fleeChance : chance (0–1) de fugir ao invés de lutar quando adjacente ao inimigo (padrão 0.70)
 *
 * Campos dos inimigos:
 *   detectionRadius : raio de detecção de players (padrão 3)
 *   aggressiveness  : chance (0–1) de perseguir fora do raio (padrão 0.0)
 */
public class MapConfig {

    // =========================================================================
    // POJOs
    // =========================================================================

    public static class GridCfg {
        public int width  = 10;
        public int height = 10;
    }

    public static class PosCfg {
        public int x = 0;
        public int y = 0;
    }

    public static class PlayerCfg {
        public int    x          = 1;
        public int    y          = 1;
        public String name       = "Jogador";
        public int    hp         = 15;
        public int    damage     = 4;
        public double evasion    = 0.20;
        /** Chance (0–1) de fugir ao invés de lutar quando adjacente ao inimigo. */
        public double fleeChance = 0.70;
    }

    public static class EnemyCfg {
        public int    x               = 7;
        public int    y               = 7;
        public String name            = "Inimigo";
        public int    hp              = 10;
        public int    damage          = 3;
        public int    detectionRadius = 3;
        public double aggressiveness  = 0.0;
    }

    public static class ObstacleCfg {
        public int x = 0;
        public int y = 0;
    }

    // =========================================================================
    // Campos do mapa
    // =========================================================================

    public String missionTitle = "";
    public int    roundLimit   = 0;

    public GridCfg           grid        = new GridCfg();
    public PosCfg            destination = new PosCfg();
    public PosCfg            spawn       = new PosCfg();
    public List<PlayerCfg>   players     = new ArrayList<>();
    public List<EnemyCfg>    enemies     = new ArrayList<>();
    public List<ObstacleCfg> obstacles   = new ArrayList<>();

    // =========================================================================
    // Carregamento
    // =========================================================================

    /** Carrega map_config.json padrão. */
    public static MapConfig load() {
        return loadFile("map_config.json");
    }

    /** Carrega qualquer arquivo JSON de mapa pelo nome. */
    public static MapConfig loadFile(String filename) {
        try {
            FileHandle fh = Gdx.files.local(filename);
            if (!fh.exists()) fh = Gdx.files.internal(filename);
            if (!fh.exists()) {
                Gdx.app.log("MapConfig", filename + " não encontrado. Usando padrão.");
                return loadDefault();
            }
            return parse(fh.readString("UTF-8"));
        } catch (Exception e) {
            Gdx.app.error("MapConfig", "Erro ao carregar " + filename + ": " + e.getMessage());
            return loadDefault();
        }
    }

    // =========================================================================
    // Parser
    // =========================================================================

    private static MapConfig parse(String json) {
        MapConfig cfg = new MapConfig();
        JsonValue root = new JsonReader().parse(json);

        cfg.missionTitle = root.getString("missionTitle", "");
        cfg.roundLimit   = root.getInt("roundLimit", 0);

        JsonValue gridV = root.get("grid");
        if (gridV != null) {
            cfg.grid.width  = gridV.getInt("width",  10);
            cfg.grid.height = gridV.getInt("height", 10);
        }

        JsonValue destV = root.get("destination");
        if (destV != null) {
            cfg.destination.x = destV.getInt("x", 9);
            cfg.destination.y = destV.getInt("y", 9);
        }

        JsonValue spawnV = root.get("spawn");
        if (spawnV != null) {
            cfg.spawn.x = spawnV.getInt("x", 1);
            cfg.spawn.y = spawnV.getInt("y", 1);
        }

        JsonValue playersV = root.get("players");
        if (playersV != null) {
            for (JsonValue pv : playersV) {
                PlayerCfg p = new PlayerCfg();
                p.x          = pv.getInt("x",            1);
                p.y          = pv.getInt("y",             1);
                p.name       = pv.getString("name",       "Jogador");
                p.hp         = pv.getInt("hp",            15);
                p.damage     = pv.getInt("damage",        4);
                p.evasion    = pv.getDouble("evasion",    0.20);
                p.fleeChance = pv.getDouble("fleeChance", 0.70);
                cfg.players.add(p);
            }
        }

        JsonValue enemiesV = root.get("enemies");
        if (enemiesV != null) {
            for (JsonValue ev : enemiesV) {
                EnemyCfg e = new EnemyCfg();
                e.x               = ev.getInt("x",                7);
                e.y               = ev.getInt("y",                7);
                e.name            = ev.getString("name",          "Inimigo");
                e.hp              = ev.getInt("hp",               10);
                e.damage          = ev.getInt("damage",           3);
                e.detectionRadius = ev.getInt("detectionRadius",  3);
                e.aggressiveness  = ev.getDouble("aggressiveness", 0.0);
                cfg.enemies.add(e);
            }
        }

        JsonValue obstV = root.get("obstacles");
        if (obstV != null) {
            for (JsonValue ov : obstV) {
                ObstacleCfg o = new ObstacleCfg();
                o.x = ov.getInt("x", 0);
                o.y = ov.getInt("y", 0);
                cfg.obstacles.add(o);
            }
        }

        return cfg;
    }

    // =========================================================================
    // Fallback padrão
    // =========================================================================

    public static MapConfig loadDefault() {
        MapConfig cfg = new MapConfig();
        cfg.missionTitle = "Missão Padrão";
        cfg.roundLimit   = 40;
        cfg.grid.width   = 10;
        cfg.grid.height  = 10;

        cfg.destination.x = 8; cfg.destination.y = 8;
        cfg.spawn.x       = 1; cfg.spawn.y       = 1;

        PlayerCfg p1 = new PlayerCfg(); p1.x=1; p1.y=1; p1.name="Soldado 1";
        PlayerCfg p2 = new PlayerCfg(); p2.x=2; p2.y=1; p2.name="Soldado 2";
        cfg.players.add(p1); cfg.players.add(p2);

        EnemyCfg e1 = new EnemyCfg(); e1.x=7; e1.y=7; e1.name="Inimigo 1";
        EnemyCfg e2 = new EnemyCfg(); e2.x=8; e2.y=8; e2.name="Inimigo 2";
        cfg.enemies.add(e1); cfg.enemies.add(e2);

        ObstacleCfg o1 = new ObstacleCfg(); o1.x=4; o1.y=4;
        ObstacleCfg o2 = new ObstacleCfg(); o2.x=4; o2.y=5;
        ObstacleCfg o3 = new ObstacleCfg(); o3.x=5; o3.y=4;
        cfg.obstacles.add(o1); cfg.obstacles.add(o2); cfg.obstacles.add(o3);

        return cfg;
    }
}
