package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CampaignTotalsScreen — "Tela Totalizadora".
 *
 * Lê todos os arquivos {@code logs/missions_*.csv} já gravados (de qualquer
 * execução anterior do programa) e mostra, para cada arquivo de mapa, o
 * total de execuções e o placar agregado (vitórias / derrotas / empates).
 *
 * Controles:
 *   ↑ / ↓         — rolar a lista linha a linha
 *   PAGE UP/DOWN  — rolar a lista por página
 *   HOME / END    — ir para o topo / fim da lista
 *   R             — recarregar os CSVs do disco
 *   ESC / BACKSPACE — voltar ao menu principal
 */
public class CampaignTotalsScreen implements Screen {

    private final MainGame game;

    private ShapeRenderer shape;
    private SpriteBatch   batch;
    private BitmapFont    fontTitle;
    private BitmapFont    fontHeader;
    private BitmapFont    fontRow;
    private BitmapFont    fontHint;

    private static final Color COL_BG       = new Color(0.04f, 0.04f, 0.08f, 1f);
    private static final Color COL_PANEL    = new Color(0.08f, 0.08f, 0.14f, 1f);
    private static final Color COL_TITLE    = new Color(0.85f, 0.85f, 1.00f, 1f);
    private static final Color COL_HEADER   = new Color(0.55f, 0.85f, 1.00f, 1f);
    private static final Color COL_ROW      = new Color(0.90f, 0.90f, 0.95f, 1f);
    private static final Color COL_VICTORY  = new Color(0.20f, 0.90f, 0.40f, 1f);
    private static final Color COL_DEFEAT   = new Color(0.95f, 0.25f, 0.25f, 1f);
    private static final Color COL_DRAW     = new Color(0.90f, 0.80f, 0.20f, 1f);
    private static final Color COL_HINT     = new Color(0.45f, 0.45f, 0.55f, 1f);
    private static final Color COL_EMPTY    = new Color(0.55f, 0.55f, 0.65f, 1f);

    /** Estatísticas agregadas de um arquivo de mapa. */
    private static final class MapStat {
        int total, vitorias, derrotas, empates;
    }

    private final Map<String, MapStat> statsByMap = new TreeMap<>();
    private int    sessionsRead   = 0;
    private int    totalExecucoes = 0;
    private int    totalVitorias  = 0;
    private int    totalDerrotas  = 0;
    private int    totalEmpates   = 0;
    private String loadError      = null;

    private float scrollOffset  = 0f;
    private static final float LINE_H = 34f;

    public CampaignTotalsScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        shape      = new ShapeRenderer();
        batch      = new SpriteBatch();
        fontTitle  = new BitmapFont(); fontTitle.getData().setScale(2.4f);
        fontHeader = new BitmapFont(); fontHeader.getData().setScale(1.3f);
        fontRow    = new BitmapFont(); fontRow.getData().setScale(1.05f);
        fontHint   = new BitmapFont(); fontHint.getData().setScale(1.0f);

        loadData();
    }

    // -------------------------------------------------------------------------
    // Leitura e agregação dos CSVs
    // -------------------------------------------------------------------------

    private void loadData() {
        statsByMap.clear();
        sessionsRead   = 0;
        totalExecucoes = 0;
        totalVitorias  = 0;
        totalDerrotas  = 0;
        totalEmpates   = 0;
        loadError      = null;
        scrollOffset   = 0f;

        File[] files = CampaignLogger.listMissionCsvFiles();
        if (files.length == 0) {
            loadError = "Nenhum arquivo de log encontrado em:\n" + CampaignLogger.getLogsDir()
                + "\n\nJogue ao menos uma campanha para gerar dados.";
            return;
        }

        for (File f : files) {
            try {
                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                List<List<String>> records = parseCsv(content);
                if (records.isEmpty()) continue;

                sessionsRead++;

                // Pula o cabeçalho (primeira linha)
                for (int i = 1; i < records.size(); i++) {
                    List<String> row = records.get(i);
                    // sessao,timestamp,campanha,missao_num,total_missoes,arquivo_mapa,resultado,descricao
                    if (row.size() < 7) continue;

                    String mapFile   = row.get(5);
                    String resultado = row.get(6);
                    if (mapFile.isEmpty()) continue;

                    MapStat stat = statsByMap.computeIfAbsent(mapFile, k -> new MapStat());
                    stat.total++;
                    totalExecucoes++;

                    switch (resultado) {
                        case "VITORIA": stat.vitorias++; totalVitorias++; break;
                        case "DERROTA": stat.derrotas++; totalDerrotas++; break;
                        case "EMPATE":  stat.empates++;  totalEmpates++;  break;
                        default: /* linha inesperada — ignora */ break;
                    }
                }
            } catch (IOException e) {
                // Um arquivo corrompido/ilegível não deve esconder os dados já
                // carregados dos demais arquivos — apenas avisa no console.
                Gdx.app.error("CampaignTotalsScreen", "Erro ao ler " + f.getName() + ": " + e.getMessage());
            }
        }

        if (statsByMap.isEmpty()) {
            loadError = "Os arquivos de log foram encontrados, mas nenhuma missão\nconcluída foi registrada ainda.";
        }
    }

    /**
     * Parser simples de CSV (RFC 4180): respeita campos entre aspas,
     * vírgulas e quebras de linha dentro de campos, e aspas duplicadas
     * como escape de aspas literais.
     */
    private static List<List<String>> parseCsv(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int n = content.length();

        int i = 0;
        while (i < n) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                    i++;
                } else if (c == '\r') {
                    i++;
                } else if (c == '\n') {
                    current.add(field.toString());
                    field.setLength(0);
                    records.add(current);
                    current = new ArrayList<>();
                    i++;
                } else {
                    field.append(c);
                    i++;
                }
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(float delta) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        int panelW = (int) (sw * 0.82f);
        int panelH = (int) (sh * 0.82f);
        int panelX = (sw - panelW) / 2;
        int panelY = (sh - panelH) / 2;

        float wrapW = panelW - 60;

        handleInput(panelH);

        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PANEL);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.setColor(COL_HEADER);
        shape.rect(panelX, panelY + panelH - 8, panelW, 8);
        shape.end();

        int cx = panelX + 30;

        // ---- Cabeçalho (título + resumo geral) ----
        batch.begin();
        float y = panelY + panelH - 40;
        y -= TextUtils.drawWrapped(batch, fontTitle, "TELA TOTALIZADORA — HISTÓRICO DE EXECUÇÕES", cx, y, wrapW, COL_TITLE);
        y -= 16;

        if (loadError != null) {
            TextUtils.drawWrapped(batch, fontHeader, loadError, cx, y, wrapW, COL_EMPTY);
            batch.end();
            drawFooter(panelX, panelY, panelW);
            return;
        }

        String resumo = sessionsRead + " sessão(ões) lida(s)   |   " + totalExecucoes + " execuções registradas";
        y -= TextUtils.drawWrapped(batch, fontHeader, resumo, cx, y, wrapW, COL_HEADER);
        y -= 6;

        String placarGeral = "Vitórias: " + totalVitorias + "   Derrotas: " + totalDerrotas + "   Empates: " + totalEmpates;
        y -= TextUtils.drawWrapped(batch, fontHeader, placarGeral, cx, y, wrapW, COL_ROW);
        y -= 14;

        y -= TextUtils.drawWrapped(batch, fontHeader, "Resultado por mapa:", cx, y, wrapW, COL_HEADER);
        y -= 8;
        batch.end();

        // ---- Lista rolável, recortada (não vaza para fora do painel) ----
        float listTop    = y;
        float footerH    = 46f;
        float listBottom = panelY + footerH;
        float listAreaH  = Math.max(0, listTop - listBottom);

        HdpiUtils.glScissor((int) panelX, (int) listBottom, (int) panelW, (int) listAreaH);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);

        batch.begin();
        float rowY = listTop + scrollOffset;
        int idx = 0;
        for (Map.Entry<String, MapStat> entry : statsByMap.entrySet()) {
            float drawY = rowY - idx * LINE_H;
            // só desenha o necessário (o scissor já garante o recorte visual)
            if (drawY < listBottom - LINE_H || drawY > listTop + LINE_H) {
                idx++;
                continue;
            }

            MapStat st = entry.getValue();
            String mapLabel = TextUtils.truncate(fontRow, entry.getKey(), panelW * 0.42f);

            fontRow.setColor(COL_ROW);
            fontRow.draw(batch, mapLabel, cx, drawY);

            float statsX = cx + panelW * 0.44f;
            fontRow.setColor(COL_HEADER);
            fontRow.draw(batch, st.total + " exec.", statsX, drawY);

            fontRow.setColor(COL_VICTORY);
            fontRow.draw(batch, "V:" + st.vitorias, statsX + 110, drawY);
            fontRow.setColor(COL_DEFEAT);
            fontRow.draw(batch, "D:" + st.derrotas, statsX + 190, drawY);
            fontRow.setColor(COL_DRAW);
            fontRow.draw(batch, "E:" + st.empates, statsX + 270, drawY);

            idx++;
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        drawFooter(panelX, panelY, panelW);
    }

    private void drawFooter(int panelX, int panelY, int panelW) {
        batch.begin();
        TextUtils.drawWrapped(batch, fontHint,
            "↑/↓ ou PgUp/PgDn: rolar   |   R: recarregar   |   ESC: voltar ao menu",
            panelX + 30, panelY + 30, panelW - 60, COL_HINT);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void handleInput(int panelH) {
        float listAreaH = Math.max(0, panelH * 0.60f); // aproximação segura para clamp de página
        float maxScroll = Math.max(0, statsByMap.size() * LINE_H - listAreaH);

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            scrollOffset = Math.min(maxScroll, scrollOffset + LINE_H);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            scrollOffset = Math.max(0, scrollOffset - LINE_H);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            scrollOffset = Math.min(maxScroll, scrollOffset + listAreaH);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            scrollOffset = Math.max(0, scrollOffset - listAreaH);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            scrollOffset = maxScroll;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            scrollOffset = 0;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            loadData();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            game.setScreen(new MainMenuScreen(game));
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    // -------------------------------------------------------------------------
    // Ciclo de vida obrigatório
    // -------------------------------------------------------------------------

    @Override
    public void resize(int width, int height) {
        shape.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        shape.dispose();
        batch.dispose();
        fontTitle.dispose();
        fontHeader.dispose();
        fontRow.dispose();
        fontHint.dispose();
    }
}
