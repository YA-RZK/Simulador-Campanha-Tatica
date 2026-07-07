# POO-2 — Simulador de Campanha Tática
<img width="220" height="320" alt="IF" src="https://github.com/user-attachments/assets/b3a2614e-bac5-43d1-bd4d-89f1f8fc8bf5" />

Este projeto é um Simulador de combate tático por turnos, desenvolvido em Java com LibGDX, no contexto do Curso Bacharelado em Ciência da Computação na disciplina de Programação Orientada a Objetos . Um esquadrão de soldados precisa atravessar um grid até um ponto de destino, desviando ou enfrentando inimigos controlados por IA, ao longo de uma campanha com várias missões em sequência.

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
  - Algum soldado alcança o destino;
  - Ou todos os inimigos são eliminados.
- Derrota
  - todos os soldados são eliminados.
- Empate
  - O limite máximo de turnos é atingido.

Após o encerramento da missão:

- O resultado é salvo em CSV;
- A próxima missão é carregada automaticamente;
- Ao final da campanha é exibido um resumo geral da execução.

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

### Diagrama de Classes

O projeto acompanha um diagrama PlantUML:

```
plantuml_export.puml
```

Pode ser aberto utilizando:
- VS Code + extensão PlantUML
- IntelliJ IDEA
- NetBeans
- PlantUML Online

<img width="2250" height="2583" alt="Diagrama de Classe" src="https://github.com/user-attachments/assets/ecf41ef3-af2f-4a4e-855f-39fbbc896ab4" />

# Como Executar

## Pré-requisitos

- Java JDK 17 ou superior
- Maven

## Executando

```bash
cd simulador
mvn clean compile exec:java
```

---

## Fluxo das Telas

### Controles - Main Menu
| Tecla | Ação |
|-------|------|
| ENTER / 1 | Iniciar campanha |
| T / 2 | Tela Totalizadora |
| ESC | Sair |

*Tela de Menu*
<img width="1200" height="830" alt="MainScreen" src="https://github.com/user-attachments/assets/04500760-aad8-4495-a059-0e821b12f2a6" />

### Game Screen

A simulação ocorre automaticamente.

*Tela da Simulação*
<img width="1201" height="799" alt="GameScreen" src="https://github.com/user-attachments/assets/b9a295ab-9ce2-455d-8df2-51c917345d6d" />

*Tela de Resultado da Rodada*
<img width="1196" height="799" alt="GameScreen Resultado" src="https://github.com/user-attachments/assets/b2bf8a44-f328-4e60-b186-89799a5a296e" />

*Tela de Transição*
<img width="1200" height="798" alt="Tela de Transição" src="https://github.com/user-attachments/assets/81bf5d64-9f50-49e8-8065-e52f1931db8a" />

### Fim da Campanha

| Tecla | Ação |
|--------|------|
| ↑ ↓ | Navegar |
| T | Abrir Totalizadora |
| ESC | Sair |

*Tela Final*
<img width="1199" height="800" alt="Tela Final" src="https://github.com/user-attachments/assets/d46d8f42-a28b-4991-b2ac-a1aaca72e47e" />

### Tela totalizadora

A tela de estatísticas lê automaticamente todos os arquivos CSV existentes em:

```
logs/
```

Ela apresenta:

- total de campanhas executadas;
- quantidade de vitórias;
- derrotas;
- empates;
- desempenho agregado por mapa.

| Tecla | Ação |
|--------|------|
| ↑ ↓ | Navegar |
| PgUp / PgDn | Rolagem rápida |
| Home / End | Início/Fim |
| R | Recarregar CSVs |
| ESC | Voltar |

*Tela Totalizadora*
<img width="1197" height="795" alt="Tela de resultados totais" src="https://github.com/user-attachments/assets/bdca2762-ffa4-44fd-9a72-436a4fdecc1e" />

---

# Configuração da Campanha

A campanha é definida pelo arquivo:

```json
{
  "campaignName": "Operação Resistência",
  "maps": [
    "map_missao_1.json",
    "map_missao_2.json",
    "map_missao_3.json"
  ]
}
```
*Imagem do Arquivo*

<img width="609" height="314" alt="Campanha" src="https://github.com/user-attachments/assets/832a7203-84d4-4f44-912d-9bea07d8a935" />

Cada missão possui seu próprio arquivo JSON contendo:

- tamanho do grid;
- posição inicial;
- destino;
- soldados;
- inimigos;
- obstáculos;
- limite de turnos.

_Exemplo:_

<img width="1001" height="523" alt="Campanha1" src="https://github.com/user-attachments/assets/e8011d5c-6ff2-4777-8aeb-b71db6846295" />

Caso algum arquivo não seja encontrado, o simulador utiliza automaticamente um mapa padrão incorporado ao código.

---

# Inteligência Artificial

## Soldados

A cada turno:

1. Permanecem parados caso já estejam no destino.
2. Fogem ou contra-atacam inimigos adjacentes.
3. Desviam de áreas perigosas.
4. Avançam em direção ao objetivo.

## Inimigos

A cada turno:

1. Atacam jogadores adjacentes.
2. Perseguem soldados detectados.
3. Patrulham aleatoriamente quando nenhum alvo é encontrado.

---

# Condições de Vitória

A missão termina quando ocorre uma das seguintes situações:

| Resultado | Condição |
|------------|----------|
| Vitória | Algum soldado chega ao destino |
| Vitória | Todos os inimigos são eliminados |
| Derrota | Todos os soldados morrem |
| Empate | Limite de turnos atingido |

---

# Sistema de Logs

Cada campanha gera automaticamente dois arquivos CSV dentro da pasta:

```
logs/
```

### Missions

```
missions_<timestamp>.csv
```

Contém uma linha para cada missão executada.

### Summary

```
summary_<timestamp>.csv
```

Contém um resumo completo da campanha.
*Exemplo:*
<img width="660" height="193" alt="CSV" src="https://github.com/user-attachments/assets/79c2bba0-6513-41a7-ae73-d4c2d931c4a1" />

---

### Fim
