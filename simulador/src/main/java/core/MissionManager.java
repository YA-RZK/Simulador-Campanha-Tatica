package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * MissionManager — gerencia a sequência de mapas da campanha.
 *
 * Mantém uma instância de {@link CampaignLogger} criada junto com ele,
 * garantindo que um único logger (e portanto um único par de CSVs) cobre
 * toda a sessão — sem estado estático.
 */
public class MissionManager {

    // -------------------------------------------------------------------------
    // Estado
    // -------------------------------------------------------------------------

    private final String         campaignName;
    private final List<String>   mapFiles;
    private int                  currentIndex = 0;

    /** Logger persistente da sessão — criado no construtor, válido até o fim. */
    private final CampaignLogger logger;

    /** Placar acumulado. */
    public int victories = 0;
    public int defeats   = 0;
    public int draws     = 0;

    /**
     * Histórico ordenado dos resultados (1 entrada por missão concluída).
     * Usado para exibir os resultados na tela final.
     */
    public final List<MissionResult> resultsHistory = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Enum
    // -------------------------------------------------------------------------

    public enum MissionResult { VICTORY, DEFEAT, DRAW }

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------

    private MissionManager(String campaignName, List<String> mapFiles) {
        this.campaignName = campaignName;
        this.mapFiles     = mapFiles;
        this.logger       = new CampaignLogger(campaignName);
    }

    // -------------------------------------------------------------------------
    // Fábrica
    // -------------------------------------------------------------------------

    /** Carrega campaign.json (local ou interno); cai no padrão se não achar. */
    public static MissionManager load() {
        try {
            FileHandle fh = Gdx.files.local("campaign.json");
            if (!fh.exists()) fh = Gdx.files.internal("campaign.json");
            if (fh.exists()) {
                MissionManager mm = parse(fh.readString("UTF-8"));
                if (mm != null) return mm;
            }
        } catch (Exception e) {
            Gdx.app.error("MissionManager", "Erro ao ler campaign.json: " + e.getMessage());
        }
        Gdx.app.log("MissionManager", "campaign.json não encontrado — usando campanha padrão.");
        return loadDefault();
    }

    private static MissionManager parse(String json) {
        try {
            JsonValue root = new JsonReader().parse(json);
            String name = root.getString("campaignName", "Campanha");
            List<String> files = new ArrayList<>();
            JsonValue mapsV = root.get("maps");
            if (mapsV != null) {
                for (JsonValue mv : mapsV) files.add(mv.asString());
            }
            if (files.isEmpty()) return null;
            return new MissionManager(name, files);
        } catch (Exception e) {
            Gdx.app.error("MissionManager", "Erro ao parsear campaign.json: " + e.getMessage());
            return null;
        }
    }

    /** Campanha padrão embutida — joga o mapa 1 três vezes seguidas. */
    public static MissionManager loadDefault() {
        List<String> files = new ArrayList<>();
        files.add("map_missao_1.json");
        files.add("map_missao_1.json");
        files.add("map_missao_1.json");
        return new MissionManager("Campanha Padrão - 3 Execuções", files);
    }

    // -------------------------------------------------------------------------
    // Navegação
    // -------------------------------------------------------------------------

    /** MapConfig do mapa atual. */
    public MapConfig currentConfig() {
        if (isFinished()) return MapConfig.loadDefault();
        return MapConfig.loadFile(mapFiles.get(currentIndex));
    }

    /** Avança registrando o resultado sem descrição. */
    public void advance(MissionResult result) {
        advance(result, "");
    }

    /**
     * Avança para o próximo mapa, registra o resultado no placar,
     * no histórico e grava a linha no CSV via logger.
     */
    public void advance(MissionResult result, String description) {
        String mapFile   = currentFileName();
        int    missionNo = currentMissionNumber();

        switch (result) {
            case VICTORY: victories++; break;
            case DEFEAT:  defeats++;   break;
            case DRAW:    draws++;     break;
        }

        resultsHistory.add(result);
        logger.logMission(missionNo, totalMissions(), mapFile, result, description);

        currentIndex++;
    }

    /**
     * Grava o resumo final da campanha no CSV de summary.
     * Deve ser chamado apenas uma vez, ao abrir a {@link CampaignEndScreen}.
     */
    public void flushSummary() {
        logger.logSummary(this);
    }

    // -------------------------------------------------------------------------
    // Getters / utilitários
    // -------------------------------------------------------------------------

    public int    currentMissionNumber() { return currentIndex + 1; }
    public int    totalMissions()        { return mapFiles.size(); }
    public String currentFileName()      { return isFinished() ? "(fim)" : mapFiles.get(currentIndex); }
    public boolean isFinished()          { return currentIndex >= mapFiles.size(); }
    public String getCampaignName()      { return campaignName; }

    public String scoreString() {
        return "Vitórias: " + victories + "   Derrotas: " + defeats + "   Empates: " + draws;
    }

    public String finalVerdict() {
        if (victories > defeats) return "CAMPANHA CONCLUÍDA — VITÓRIA GERAL!";
        if (defeats > victories) return "CAMPANHA CONCLUÍDA — DERROTA GERAL.";
        return "CAMPANHA CONCLUÍDA — EMPATE.";
    }
}
