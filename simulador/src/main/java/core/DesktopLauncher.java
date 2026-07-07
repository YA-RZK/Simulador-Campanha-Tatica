package core;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Classe DesktopLauncher — inicializa o simulador no ambiente desktop.
 *
 * Configura a janela (título e dimensões) e instancia a aplicação
 * libGDX passando {@link MainGame} como lógica principal.
 */
public class DesktopLauncher {

    /**
     * Método principal: configura e lança a aplicação.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        // Configurações da janela desktop
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Simulador_C");        // título exibido na barra da janela
        config.setWindowedMode(1200, 800);      // resolução inicial em pixels (largura × altura)

        // Inicia a aplicação libGDX com a lógica de jogo definida em MainGame
        new Lwjgl3Application(new MainGame(), config);
    }
}   