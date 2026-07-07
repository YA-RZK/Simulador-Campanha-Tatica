# POO-2 — Simulador de Campanha Tática
<img width="220" height="320" alt="IF" src="https://github.com/user-attachments/assets/b3a2614e-bac5-43d1-bd4d-89f1f8fc8bf5" />

Este projeto é um Simulador de combate tático por turnos, desenvolvido em Java com LibGDX, no contexto da disciplina de Programação Orientada a Objetos. Um esquadrão de soldados precisa atravessar um grid até um ponto de destino, desviando ou enfrentando inimigos controlados por IA, ao longo de uma campanha com várias missões em sequência.

Cada execução gera logs em CSV, que alimentam uma Tela Totalizadora com o histórico agregado de todas as sessões já jogadas.

---

### Visão Geral

Cada campanha é composta por várias missões executadas em sequência.
Durante uma missão:

- Os **Soldados (Player)** tentam alcançar o objetivo do mapa.
- Os **Inimigos (Enemy)** patrulham, detectam e atacam automaticamente.
- Todo o combate é resolvido por IA, sem necessidade de intervenção do jogador.

Uma missão termina quando ocorre uma das seguintes condições:

- Vitória
  - algum soldado alcança o destino;
  - ou todos os inimigos são eliminados.
- Derrota
  - todos os soldados são eliminados.
- Empate
  - o limite máximo de turnos é atingido.

Após o encerramento da missão:

- o resultado é salvo em CSV;
- a próxima missão é carregada automaticamente;
- ao final da campanha é exibido um resumo geral da execução.

---

### Funcionalidades

- Menu principal
- Carregamento dinâmico de mapas via arquivos JSON
- Inteligência Artificial para soldados e inimigos
- Combate automático
- Painel de combate em tempo real
- Interface responsiva
- Transição automática entre missões
- Tela final da campanha
- Registro de todas as missões em CSV
- Tela Totalizadora com histórico de todas as campanhas executadas

---

### Tecnologias Utilizadas

- Java 17
- LibGDX 1.13.1
- LWJGL3
- Maven

---

### Estrutura do Projeto

<img width="279" height="782" alt="Estrutura do Projeto" src="https://github.com/user-attachments/assets/59e8233c-6240-4ea0-86bc-44f1de70387d" />

---

# Como Executar

## Pré-requisitos

- Java JDK 17 ou superior
- Maven

## Executando

```bash
cd simulador
mvn clean compile exec:java
```
