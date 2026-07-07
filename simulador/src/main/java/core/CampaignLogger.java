package core;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * CampaignLogger — logger persistente da campanha.
 *
 * Gera dois arquivos na subpasta  logs/  (criada automaticamente ao lado do
 * executável / pasta de trabalho):
 *
 *   logs/missions_<timestamp>.csv   — uma linha por missão concluída
 *   logs/summary_<timestamp>.csv    — resumo da sessão inteira
 *
 * Um único timestamp (gerado no construtor) identifica a sessão, mantendo
 * histórico de execuções anteriores intacto.
 *
 * Uso típico:
 *   CampaignLogger logger = new CampaignLogger(campaignName);
 *   logger.logMission(...);   // chamado a cada missão
 *   logger.logSummary(...);   // chamado na tela final
 */
public class CampaignLogger {

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    /** Subpasta de saída, relativa ao diretório de trabalho do programa. */
    private static final String LOGS_DIR = "logs";

    /** Cabeçalho do CSV de missões. */
    private static final String MISSION_HEADER =
        "sessao,timestamp,campanha,missao_num,total_missoes,arquivo_mapa,resultado,descricao\n";

    /** Cabeçalho do CSV de resumo. */
    private static final String SUMMARY_HEADER =
        "sessao,timestamp,campanha,total_missoes,vitorias,derrotas,empates,veredito,";

    // -------------------------------------------------------------------------
    // Estado da instância
    // -------------------------------------------------------------------------

    private final String sessionId;   // timestamp de início da sessão
    private final String campaignName;
    private final String missionFile; // caminho: logs/missions_<sessao>.csv
    private final String summaryFile; // caminho: logs/summary_<sessao>.csv

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------

    /**
     * Cria um logger para a sessão atual.
     * Cria a pasta  logs/  se não existir e escreve os cabeçalhos dos CSVs.
     *
     * @param campaignName nome da campanha (vai para todas as linhas do CSV)
     */
    public CampaignLogger(String campaignName) {
        this.sessionId    = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        this.campaignName = escapeCsv(campaignName);
        this.missionFile  = LOGS_DIR + File.separator + "missions_" + sessionId + ".csv";
        this.summaryFile  = LOGS_DIR + File.separator + "summary_"  + sessionId + ".csv";

        ensureLogsDir();
        initFile(missionFile, MISSION_HEADER);
        // O summary só recebe cabeçalho dinâmico (com colunas por execução) em logSummary()
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Registra o resultado de uma missão concluída.
     * Adiciona uma linha ao CSV de missões da sessão.
     *
     * @param missionNumber número da missão (base-1)
     * @param totalMissions total de missões da campanha
     * @param mapFile       nome do arquivo de mapa
     * @param result        resultado (VICTORY / DEFEAT / DRAW)
     * @param description   descrição opcional (ex.: "2 soldados chegaram")
     */
    public void logMission(int missionNumber, int totalMissions, String mapFile,
                           MissionManager.MissionResult result, String description) {

        String row = sessionId + ","
                + nowTimestamp() + ","
                + campaignName + ","
                + missionNumber + ","
                + totalMissions + ","
                + escapeCsv(mapFile) + ","
                + resultLabel(result) + ","
                + escapeCsv(description == null ? "" : description)
                + "\n";

        appendToFile(missionFile, row);
    }

    /**
     * Registra o resumo final da campanha.
     * Gera o CSV de resumo com uma coluna por execução individual.
     *
     * @param missions MissionManager já finalizado
     */
    public void logSummary(MissionManager missions) {
        // Cabeçalho dinâmico: colunas fixas + execucao_1, execucao_2, …
        StringBuilder header = new StringBuilder(SUMMARY_HEADER);
        for (int i = 1; i <= missions.resultsHistory.size(); i++) {
            header.append("execucao_").append(i).append(",");
        }
        // Remove última vírgula e fecha
        if (missions.resultsHistory.size() > 0) {
            header.setLength(header.length() - 1);
        }
        header.append("\n");

        // Linha de dados
        StringBuilder row = new StringBuilder();
        row.append(sessionId).append(",")
           .append(nowTimestamp()).append(",")
           .append(campaignName).append(",")
           .append(missions.totalMissions()).append(",")
           .append(missions.victories).append(",")
           .append(missions.defeats).append(",")
           .append(missions.draws).append(",")
           .append(escapeCsv(missions.finalVerdict())).append(",");

        for (int i = 0; i < missions.resultsHistory.size(); i++) {
            row.append(resultLabel(missions.resultsHistory.get(i)));
            if (i < missions.resultsHistory.size() - 1) row.append(",");
        }

        row.append("\n");

        initFile(summaryFile, header.toString());
        appendToFile(summaryFile, row.toString());
    }

    // -------------------------------------------------------------------------
    // Utilitários para leitura do histórico (usados pela Tela Totalizadora)
    // -------------------------------------------------------------------------

    /** Caminho absoluto da pasta onde os CSVs de log são gravados. */
    public static String getLogsDir() {
        return LOGS_DIR;
    }

    /**
     * Lista todos os arquivos  logs/missions_*.csv  já gravados (todas as
     * sessões anteriores, inclusive de execuções passadas do programa),
     * ordenados por nome (ordem cronológica, já que o timestamp está no nome).
     */
    public static File[] listMissionCsvFiles() {
        File dir = new File(LOGS_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("missions_") && name.endsWith(".csv"));
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    // -------------------------------------------------------------------------
    // Rótulos CSV
    // -------------------------------------------------------------------------

    /** Rótulo em inglês para uso no CSV (seguro para parsers). */
    public static String resultLabel(MissionManager.MissionResult result) {
        switch (result) {
            case VICTORY: return "VITORIA";
            case DEFEAT: return "DERROTA";
            default: return "EMPATE";
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de I/O
    // -------------------------------------------------------------------------

    /** Garante que a pasta  logs/  existe ao lado do executável. */
    private void ensureLogsDir() {
        File dir = new File(LOGS_DIR);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        System.out.println("Logs serão salvos em: " + dir.getAbsolutePath());
    }

    /** Cria o arquivo e escreve o cabeçalho (não sobrescreve se já existir). */
    private void initFile(String path, String header) {
        try {
            File file = new File(path);

            System.err.println("CSV criado/lido em: " + file.getAbsolutePath());

            if (!file.exists()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(header);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Acrescenta uma linha ao arquivo. */
    private void appendToFile(String path, String content) {
        File file = new File(path);

        System.err.println("Gravando CSV em: " + file.getAbsolutePath());
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Timestamp legível para coluna do CSV. */
    private static String nowTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Escapa um valor para uso em CSV (RFC 4180):
     * se contiver vírgula, aspas ou quebra de linha, envolve em aspas duplas
     * e dobra as aspas internas.
     */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
