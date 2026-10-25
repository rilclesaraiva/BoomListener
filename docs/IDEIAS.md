# Ideias para o futuro

Ideias combinadas com o rilclesaraiva que **ainda não são para fazer**. Ficam aqui para não se perderem entre conversas. Só implemente quando ele pedir.

## Personagem que reage à música (no lugar da capa)

Ideia de 25/10/2026, ainda para amadurecer.

- **O que é:** um personagem animado (estilo chibi/anime, por exemplo) que ocupa o espaço da capa no player e reage ao som que está tocando.
- **Formato:** arquivo **Rive (`.riv`)**. Nada de 3D de verdade (modelos, motor 3D): pesado demais para o ganho.
- **"3D falso" vale:** com a arte certa dá para enganar o olho. Camadas em profundidades diferentes que se movem em velocidades diferentes (paralaxe), sombras e luz pintadas, rotação simulada trocando o desenho por ângulos (como sprites de jogo) e deformação de malha do próprio Rive. Tudo isso é 2D por dentro, então continua leve.
- **Um só para o app todo:** não muda por música, álbum ou artista.
- **Substitui a capa** no player quando estiver ligado.
- **Como reage:** o app mede o som no próprio player (um "tap" de áudio no ExoPlayer, tipo `TeeAudioProcessor`) e passa números para o personagem. **Nada** de API `Visualizer` (exige permissão de microfone, `RECORD_AUDIO`, que o app não pede).
- **Entradas do personagem (contrato público):** volume, graves, médios, agudos, batida e tocando/pausado. As entradas da state machine do Rive têm esses nomes fixos, documentados para quem quiser criar o próprio personagem.
- **Importar:** a pessoa escolhe o arquivo pelo seletor do sistema (SAF, sem permissão). O arquivo fica guardado no aparelho (pasta de dados da pessoa, que precisa entrar em `USER_DIRS` do backup).
- **Ordem sugerida:** primeiro um visualizador nosso, ao vivo, para validar a leitura do som; depois o import do `.riv` com o contrato de entradas.
- **Antes de começar:** confirmar na documentação oficial a versão atual da biblioteca do Rive para Android e o que ela exige (minSdk, tamanho no APK).
