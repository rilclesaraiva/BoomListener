# Arquitetura do Boom, listener!

Documento de referência da estrutura do app. O `CLAUDE.md` resume as regras; aqui fica o porquê de cada peça.

## Visão geral

App Android nativo em **Kotlin + Jetpack Compose (Material 3)**, dividido em **módulos Gradle**. Cada módulo tem uma responsabilidade só, compila separado e só enxerga o que declarou como dependência, então um bug fica "preso" no módulo onde nasceu e uma atualização mexe em poucos lugares.

Princípios herdados do BusBus:
- **Local e privado por padrão:** sem conta, sem anúncio, nada sai do aparelho além das consultas de metadados (Deezer e Last.fm).
- **Fonte única da verdade:** versão, dependências e regras de build em um lugar só.
- **Build e release automáticos:** cada push na `main` gera o APK assinado e a Release do GitHub com a nota do `CHANGELOG.md`.

## Mapa dos módulos

```
app/                      ← ponto de entrada: Activity, navegação, injeção de dependências
build-logic/              ← plugins de convenção do Gradle (config compartilhada)
gradle/libs.versions.toml ← catálogo de versões (única lista de dependências)

core/
  model/          ← classes de dados puras (Track, Album, Artist, Release…). Sem nenhum import android.*
  common/         ← utilidades: Result, dispatchers, log, formatação de tempo
  designsystem/   ← tema M3, cor dinâmica, cor extraída da capa, componentes visuais reutilizáveis
  database/       ← Room: tabelas, DAOs, migrações
  datastore/      ← preferências do usuário (DataStore)
  network/        ← cliente HTTP (OkHttp + Retrofit/kotlinx.serialization), cache e limite de requisições
  player/         ← Media3 (ExoPlayer) + MediaSessionService: fila, notificação, tela de bloqueio, Bluetooth

data/
  library/        ← varre o MediaStore, sincroniza com o Room, expõe faixas, álbuns e artistas
  tags/           ← lê e grava tags nos arquivos (TagLib nativo) + pedido de permissão de escrita
  deezer/         ← API do Deezer: foto do artista, discografia, lançamentos, faixas
  lastfm/         ← API do Last.fm (via proxy): biografia, tags de gênero, similares, scrobble
  releases/       ← artistas seguidos, comparação de discografia, WorkManager diário, notificações
  wellbeing/      ← estatísticas de escuta (mais ouvidas) e, depois, tempo de escuta e avisos de tela
  lyrics/         ← letras: LRCLIB + cache no aparelho + leitor de LRC
  updates/        ← "Verificar atualizações": compara a versão instalada com a última Release do GitHub

feature/
  library/        ← telas da biblioteca: músicas, álbuns, artistas, pastas, busca
  nowplaying/     ← player em tela cheia + mini player
  artist/         ← página do artista: foto, bio, discografia com "tenho / falta"
  tageditor/      ← edição de tags de uma ou várias faixas
  releases/       ← aba de lançamentos e gerenciamento de quem você segue
  settings/       ← ajustes, tema, sobre, licenças, privacidade
  wellbeing/      ← estatísticas de escuta e configuração dos avisos

proxy/            ← Cloudflare Worker que guarda a chave e o segredo do Last.fm (igual ao do BusBus)
```

## Regra de dependências

```
app ──► feature/* ──► data/* ──► core/*
```

- **`feature` nunca depende de outra `feature`.** A navegação entre telas é feita pelo `app`, que conhece todas. Isso permite apagar ou reescrever uma tela sem quebrar as outras.
- **`data` nunca depende de `feature`.** Cada módulo `data` expõe uma interface `…Repository`; a implementação fica `internal`.
- **`core` não depende de `data` nem de `feature`.** `core/model` não depende de nada.
- `feature` pode usar `core/designsystem`, `core/model` e `core/common` diretamente.

Se uma mudança "precisa" quebrar essas setas, o código está no módulo errado.

## Dentro de um módulo `feature`

Padrão MVVM com estado único por tela:

```
feature/artist/
  ArtistScreen.kt       ← Composable: só desenha o estado e repassa eventos
  ArtistViewModel.kt    ← junta os repositórios, expõe StateFlow<ArtistUiState>
  ArtistUiState.kt      ← sealed interface: Loading / Success / Error
  ArtistNavigation.kt   ← rota e função de navegação que o `app` registra
```

A tela nunca chama repositório nem API direto; tudo passa pelo ViewModel.

## Fontes de dados externas

| Fonte | Uso | Chave | Observação |
|---|---|---|---|
| MediaStore | arquivos de música do aparelho | — | fonte da biblioteca |
| Deezer API | foto do artista, discografia, lançamentos | não precisa | ~50 req / 5 s por IP; o `core/network` limita |
| Last.fm API | bio, tags, similares, scrobble | sim | chamado **só via `proxy/`**, a chave nunca vai no APK |
| song.link | links "abrir em…" para outros serviços | não precisa | o app **nunca reproduz** nada de fora |

Tudo que vem de fora é salvo no Room com data de atualização, para o app funcionar offline e não repetir consultas.

## Fluxos principais

- **Biblioteca:** `data/library` escuta mudanças do MediaStore → atualiza o Room → as telas observam via `Flow`.
- **Edição de tags:** `feature/tageditor` → `data/tags` pede permissão de escrita (Android 11+ usa `MediaStore.createWriteRequest`) → grava → pede reescaneamento do arquivo → `LibraryRepository.notifyChanged` (lista e capas recarregam) e `PlaybackController.updateTrackInfo` (fila e notificação sem parar a música).
- **Tags automáticas:** o ViewModel do editor junta `data/deezer` (`searchTrack` + `rankCandidates`, testado com acentos, "feat." e homônimos; depois `trackTags` com faixa, disco, ano, capa) e `data/lastfm` (`trackTags` + `pickGenre`, gênero mais preciso). Resultado claro (≥ 0,8 e 0,1 à frente do segundo) preenche sozinho; senão o usuário escolhe. Nada é gravado sem tocar em Salvar.
- **Página do artista:** primeiro mostra o cache do Room, depois atualiza em segundo plano com Deezer e Last.fm.
- **Radar de lançamentos:** `data/releases` associa os artistas locais a IDs do Deezer; um WorkManager diário compara a discografia com a última checagem e notifica o que é novo.
- **Avisos de tela:** `data/wellbeing` soma o tempo de reprodução vindo do `core/player`; ao passar do limite configurado, mostra um aviso leve (nunca bloqueia).

## Design

Base: **Material 3 Expressive**, a evolução do M3 lançada com o Android 16. Ela traz paletas mais ricas, fontes variáveis, formas que se transformam ("shape morphing") e movimento com física de mola.

**Versão da biblioteca:** `androidx.compose.material3` **1.5.0** (em beta desde outubro de 2026). A 1.4.0 estável ainda não tem a maior parte dos componentes Expressive. Quando a 1.5.0 sair estável, troca-se só a versão no catálogo.

**Tema (`core/designsystem`)**
- **Cor:** 5 cores-chave, cada uma gerando uma paleta de 13 tons. Usamos papéis (`primary`, `secondary`, `tertiary`, `surface…`), nunca hex. Cor dinâmica no Android 12+, com tema próprio como fallback. No player, o esquema é gerado a partir da cor dominante da capa (com Palette ou MaterialKolor).
- **Tipografia:** a escala M3 de 15 estilos (display, headline, title, body, label × large/medium/small). Fonte variável (Roboto Flex ou outra) para usar peso e largura como efeito, por exemplo no título da música que está tocando.
- **Forma:** os 5 tamanhos (extraSmall a extraLarge) e as `MaterialShapes` do Expressive para capas, botões e indicadores.
- **Movimento:** `MaterialTheme.motionScheme` (molas "expressive" ou "standard"), nunca durações escritas à mão.
- **Elevação:** tonal (camadas de cor de superfície), sombra só onde precisa destacar algo.

**Componentes previstos**

| Onde | Componente M3 |
|---|---|
| Navegação principal (≤ 5 abas) | `NavigationBar` no celular, `NavigationRail` em tablet ou paisagem (via adaptive navigation suite) |
| Barra de progresso da música | `Slider` com trilha ondulada / `LinearWavyProgressIndicator` (o mesmo estilo do player do Android 16) |
| Controles do player | `ButtonGroup` com botões que mudam de forma ao tocar |
| Carregando | `LoadingIndicator` (formas que se transformam) |
| Busca na biblioteca | `SearchBar` (slot-based, estável desde 1.5.0-alpha24) |
| Ações na lista ou seleção | `FloatingToolbar` / `FlexibleBottomAppBar` |
| Álbuns e lançamentos em destaque | `Carousel` |
| Listas de faixas | List items Expressive (agrupados, com cantos que se adaptam) |

## Ideias registradas (ainda sem data)

- Biblioteca dividida em **Músicas, Álbuns, Artistas e Gêneros** — ✅ na 0.15.0: abas **Início** (lista de músicas) · **Lançamentos** · **Biblioteca** (botões em pílula — `FilterChip` — com Gêneros, Artistas, Playlists e Álbuns; telas de gênero, álbum e playlist em `feature/library`). Ajustes e Histórico saíram da barra de baixo e ficam no canto superior direito de cada aba (`BoomTopActions`).
- **Playlists do app** — ✅ na 0.15.0: `data/library/PlaylistRepository` (arquivo JSON no aparelho, como as estatísticas). Criar, renomear, apagar, adicionar pelo menu ⋮ (folha `AddToPlaylistSheet`, também usada pelo player via `:app`), tirar e reordenar segurando.
- **Aba Lançamentos** — ✅ na 0.16.0 (sem notificação ainda): lista os lançamentos dos artistas seguidos, do mais novo para o mais antigo ou por artista. Falta o WorkManager diário com notificação.
- **Histórico de escuta** — ✅ na 0.15.0: `data/wellbeing/ListeningHistory` (até 1 ano / 5.000 reproduções) e tela em `feature/wellbeing`, agrupada em Hoje, Ontem, Esta semana, Semana passada e meses.
- **Radar de lançamentos** na aba Lançamentos: para cada artista seguido (`data/releases`), checagem diária no Deezer (WorkManager), lista dos lançamentos recentes e notificação do que é novo.
- **Ajustes ainda por vir** (seguindo o modelo das telas de referência): Gestão de música (pastas, ignorar áudios curtos), Reprodução (pular silêncio, crossfade), Equalizador, Notificações (radar de lançamentos), Integrações (Last.fm, Deezer), Cópia e restauração.
- Cor do player tirada da capa.
- **Embaralhamento próprio do app** — ✅ feito na 0.9.0 (`core/player/ShuffleState`). Regras:
  - **Algoritmo sem repetição:** embaralhamento de Fisher-Yates sobre a fila inteira. Cada música toca **uma vez** por rodada; nenhuma se repete antes de todas terem tocado.
  - **Vale para qualquer origem do aleatório:** botão dentro do player, um futuro "Tocar em ordem aleatória" na Biblioteca, álbum, artista etc. Todos passam pelo mesmo embaralhador em `core/player`.
  - **A ordem embaralhada fica fixa** e é a fila real (o ExoPlayer não embaralha nada): a tela mostra a ordem verdadeira, e reordenar e remover funcionam igual com ou sem aleatório.
  - **A ordem original é guardada** ao ligar o aleatório e restaurada ao desligar, continuando da música que está tocando. Músicas removidas durante o aleatório também saem da ordem original; músicas movidas ficam onde o usuário pôs só na ordem embaralhada.
  - **Com "repetir tudo" + aleatório:** ao fim de uma rodada, embaralha de novo, sem deixar a última música da rodada anterior ser a primeira da nova.
  - A lógica do embaralhador fica numa classe pura (sem Android), com testes: sem repetição, ordem original restaurada, remoção refletida nas duas ordens, virada de rodada.
- **Letras no player** — ✅ feito na 0.10.0 (`data/lyrics`), letra embutida no arquivo ✅ na 0.14.0 (tem prioridade sobre o LRCLIB): tocar na capa do player troca, com animação suave (capa encolhe e esmaece, letra entra), para a letra; tocar de novo volta. Letra **sincronizada** quando houver: a linha atual em destaque (maior e mais forte), as outras esmaecidas, rolagem automática acompanhando a música, e tocar numa linha pula para aquele trecho. Fontes, em ordem: letra embutida no arquivo (tag de letra/LRC) → **LRCLIB** (gratuito, sem chave, tem letras sincronizadas) → "letra não encontrada". Guardar em cache no aparelho. Precisa de um módulo novo `data/lyrics` (perguntar antes de criar, pela regra). Fundo do player tingido pela cor da capa (combina com a ideia já registrada).
- **Perfil do artista** — ✅ feito na 0.11.0 com o Deezer; falta a biografia do Last.fm (precisa da chave + proxy na Cloudflare). Visual próprio em Material 3 Expressive, inspirado nas referências (Spotify e perfil do TikTok), sem copiar nenhum dos dois:
  - Cabeçalho grande com a foto do artista (Deezer) ocupando o topo e esmaecendo para a cor de fundo; nome em destaque por cima; foto em círculo sobreposta na borda do cabeçalho (estilo perfil).
  - Linha de números: músicas que você tem, álbuns que você tem, fãs no Deezer.
  - Botões: **Seguir** (liga o radar de lançamentos), **aleatório** e **tocar** (usam o embaralhador próprio).
  - Abas: **Músicas** (as suas desse artista e as mais tocadas no Deezer, marcando as que você já tem), **Discografia** (álbuns, EPs e singles com "tenho / falta"), **Sobre** (biografia e tags do Last.fm, artistas parecidos).
  - Cabeçalho encolhe ao rolar, virando barra de topo com o nome.
- **Injeção de dependências:** enquanto o Hilt não entra, cada `data` expõe um ponto de acesso provisório (ex.: `LibraryData.repository(context)`) que devolve sempre a mesma instância.

## Stack

Hoje estão em uso: Compose, Material 3, navegação, Media3 e DataStore. O resto entra módulo a módulo, conforme cada funcionalidade for construída.


- Kotlin, Compose, Material 3 (com cor dinâmica)
- Hilt (injeção de dependências)
- Room, DataStore, WorkManager
- Media3 (ExoPlayer + MediaSession)
- OkHttp, Retrofit, kotlinx.serialization
- Coil (imagens)
- TagLib para Android (`io.github.kyant0:taglib`): tags de MP3, FLAC, M4A, OGG, OPUS, WAV… Trabalha com o descritor de arquivo do MediaStore, o único jeito de gravar no Android 10+ (o jaudiotagger precisa do caminho do arquivo, por isso foi trocado).
- Testes: JUnit, Turbine (Flows), MockK

## Build e release

- Versão definida **só** em `gradle.properties` (`APP_VERSION=1.0.0`); o `versionCode` é calculado como `MAJOR*10000 + MINOR*100 + PATCH`, o mesmo esquema do BusBus.
- `.github/workflows/android.yml`: testes → `assembleRelease` assinado com os Secrets → renomeia para `boomlistener_v{versão}.apk` → Release do GitHub com a nota tirada do `CHANGELOG.md`.
- JDK 21 e o SDK que já vem no runner `ubuntu-latest` (sem `setup-android`), lições já aprendidas no BusBus.
