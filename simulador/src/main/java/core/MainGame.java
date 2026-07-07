package core;

import com.badlogic.gdx.Game;

/**
 * Classe MainGame — ponto de entrada da aplicação libGDX.
 *
 * Abre o {@link MainMenuScreen}, de onde o jogador escolhe entre iniciar
 * uma nova campanha (que carrega o {@link MissionManager} a partir do
 * campaign.json) ou abrir a {@link CampaignTotalsScreen} — a Tela
 * Totalizadora, que lê o histórico de execuções salvo em  logs/*.csv .
 */
public class MainGame extends Game {

    @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }
}
