# Instruções para o Claude neste repositório

Este arquivo é lido no início de cada sessão. É como o Claude recupera o contexto do projeto, já que perde a memória entre conversas e esquece detalhes quando o chat fica longo. Leia também:
- `docs/ARQUITETURA.md`: estrutura dos módulos e o porquê de cada um
- `CHANGELOG.md`: o que já foi feito, versão por versão

Em caso de conflito entre uma instrução antiga do chat e este arquivo, **vale este arquivo**. Se uma decisão nova mudar algo aqui, atualize este arquivo no mesmo commit.

## O app

**Nome:** Boom, listener! (trocadilho com *sonic boom* + ouvinte). No ícone da tela inicial: **BoomListener**. Repositório: `rilclesaraiva/BoomListener`. `applicationId`: `com.boomlistener.app`. **O applicationId nunca muda** depois do primeiro APK publicado.

Reprodutor de música **local** para Android, em Material 3, com edição completa de tags, página de artista enriquecida (Deezer + Last.fm) e radar de lançamentos dos artistas seguidos. Gratuito, sem anúncios, sem conta.

**Limites que não mudam:**
- O app **nunca reproduz** conteúdo de serviços externos (YouTube Music, Spotify, Deezer…). Fontes externas servem só para **informação** e para links "abrir em…". Compartilhar manda o link do YouTube Music da própria música no formato `https://music.youtube.com/watch?v=ID&drli=1` (Deezer → song.link → `youtubeMusicShareUrl`, em `data/deezer`); sem resultado confiável, o link de busca.
- **Bandcamp não é usado** (não tem API pública).
- Nada de dados do usuário sai do aparelho, exceto as consultas de metadados e o scrobble no Last.fm quando o próprio usuário ativar.
- **Permissões de mídia: só áudio** (`READ_MEDIA_AUDIO`, e `READ_EXTERNAL_STORAGE` até o Android 12). **Nunca** peça fotos ou vídeos: a política da Play Store só aceita essas permissões quando galeria é a função principal do app, e as capas não precisam delas (vêm de dentro dos arquivos e do MediaStore de áudio). Capa escolhida pelo usuário no editor de tags usa o **Photo Picker**, que não exige permissão.
- **Gravar tags:** no Android 10+ o próprio sistema pergunta arquivo por arquivo (`MediaStore.createWriteRequest` / `RecoverableSecurityException`), sem permissão no manifesto. Só no Android 8 e 9 existe `WRITE_EXTERNAL_STORAGE` (com `maxSdkVersion="28"`). Nenhuma outra permissão de armazenamento.
- A identidade visual é própria: inspiração de layout é permitida, copiar a marca ou o visual de outro app não.

## Regra fixa: Release só com autorização

- **Nunca publique uma Release no GitHub por conta própria.** Todo push gera o APK assinado só como **artefato** da Action (aba Actions → build → Artifacts). É assim que o rilclesaraiva testa.
- Release só quando ele disser explicitamente que pode ("pode lançar", "pode ser release"). Aí dispare: `gh workflow run android.yml -R rilclesaraiva/BoomListener --ref main -f publish_release=true`.
- A primeira Release pública foi a **0.36.2 (beta)**, autorizada em 21/10/2026. A nota dela avisa que celular deitado e tablet ainda têm telas quebradas; o README explica o uso de IA (feito com IA, sob direção, teste e revisão do autor). Mantenha esse tom honesto nas próximas notas.
- Esta sessão **não consegue apagar** Releases nem tags (403). Se uma sair por engano, peça para ele apagar pelo GitHub.

## Regra fixa: modularidade

Siga `docs/ARQUITETURA.md`. Resumo:
- Dependências só nesta direção: `app → feature → data → core`.
- **`feature` nunca importa outra `feature`.** A navegação fica no `app`.
- Implementações em `data` são `internal`; o resto do app só vê a interface `…Repository`.
- Código novo vai para o módulo da responsabilidade dele. Se não houver um módulo adequado, **pergunte antes** de criar um novo ou de colocar o código num lugar "quase certo".
- Dependências novas entram **só** em `gradle/libs.versions.toml`, nunca com versão escrita direto num `build.gradle.kts`.
- Configuração repetida entre módulos vai para os plugins de `build-logic/`, não é copiada.

## Regra fixa: design Material 3 (Expressive)

O visual segue o **Material 3 Expressive** (m3.material.io). Detalhes em `docs/ARQUITETURA.md`, seção "Design".
- **Nenhuma cor, fonte ou forma escrita "na mão" numa tela.** Use sempre `MaterialTheme.colorScheme`, `.typography`, `.shapes` e `.motionScheme`. Valores próprios moram só em `core/designsystem`.
- Cores sempre em par com o "on" correspondente (`primaryContainer` com `onPrimaryContainer`), para garantir contraste.
- Cor dinâmica do papel de parede no Android 12+, com tema próprio de fallback abaixo disso. O player pode tingir o tema com a cor da capa que está tocando.
- Componentes vêm do `androidx.compose.material3`. Componente próprio só quando o M3 não tiver um equivalente, e ele fica em `core/designsystem`.
- Componentes marcados como experimentais (`@ExperimentalMaterial3ExpressiveApi`) podem ser usados, mas **só dentro de `core/designsystem`**, embrulhados num componente nosso. Assim uma mudança de API da Google se corrige em um lugar só.
- App sempre edge-to-edge, com tema claro, escuro e AMOLED (igual ao BusBus).

## Regra fixa: manter o CHANGELOG.md atualizado (em inglês)

O app é internacional: **o CHANGELOG e as Releases são sempre em inglês.**

Toda mudança que vire versão nova (correção, novidade, ajuste visual):
1. Atualize o `CHANGELOG.md` no formato já usado (`✨ New`, `🐛 Fixes`, `🎨 Visual tweaks`), em inglês, escrito para quem usa o app, não para programador.
2. Faça isso **no mesmo momento** do bump de versão, nunca depois.
3. A nota tem que estar pronta antes do push: a Action usa ela como texto da Release.

## Regra fixa: idiomas do app

- **Inglês é o padrão**: todo texto de tela vai em `res/values/strings.xml` em inglês.
- **Português sempre junto**: a mesma chave em `res/values-pt/strings.xml`. Nunca adicione um texto só num dos dois.
- Nenhum texto visível escrito direto no código Kotlin (inclusive `contentDescription`). Nomes próprios (bibliotecas, licenças) podem ficar no código.
- O README é em inglês; o CLAUDE.md, a ARQUITETURA e as mensagens de commit continuam em português.

## Regra fixa: agrupar bugs antes de commitar

Cada push na `main` dispara build e assinatura (Release só com autorização). Para guardar trabalho sem gerar build, faça commit e push numa branch `wip/…` e junte na `main` quando ele liberar. Quando o usuário estiver testando e reportando bugs um a um:
1. Corrija conforme ele reporta, **sem commitar a cada bug**.
2. Só faça commit, push, bump e changelog quando ele sinalizar que terminou a rodada ("pode mandar", "agora sim", pedir a build).
3. Se não estiver claro se ele terminou, **pergunte**.
4. Um bug isolado, reportado fora de uma rodada de testes, segue o fluxo normal: corrigir → commitar → avisar.

## Versionamento

- Fonte única: `APP_VERSION` em `gradle.properties`. Nunca edite `versionCode` ou `versionName` direto.
- `versionCode = MAJOR*10000 + MINOR*100 + PATCH`.
- SemVer: correção sobe PATCH, novidade compatível sobe MINOR, mudança incompatível sobe MAJOR.
- O app não usa Room: os dados do usuário ficam em DataStore e arquivos. Mudança no formato do que já está guardado (artistas seguidos, estatísticas, histórico, playlists, perfil) **sempre** com migração escrita e testada; nunca apague dados do usuário para "começar limpo".

## Regra fixa: entregas com a versão no nome

Todo APK entregue ao usuário se chama `boomlistener_v{versão}.apk` (ex.: `boomlistener_v1.0.0.apk`), nunca `app-release.apk`.

## Player

- A reprodução mora em `core/player`: `PlaybackService` (Media3 `MediaSessionService` + ExoPlayer) toca de verdade; as telas falam só com a interface `PlaybackController` (`PlayerConnection.controller(context)`).
- Notificação, tela de bloqueio, fone Bluetooth e Android Auto vêm de graça do `MediaSession`; não crie notificação de mídia à mão.
- Desde o Media3 1.11, o `MediaSession` exige ser usado na thread principal. O controller já posta tudo no `Looper` principal; mantenha assim.
- O player em tela cheia **não é uma tela da navegação**: é um painel (`PlayerSheet`) por cima do app inteiro, controlado por `PlayerSheetState` (0 = fechado, 1 = aberto). O mini player abre (toque ou puxar para cima) e encerra a música (puxar para baixo). O "voltar" do sistema fecha o painel com voltar preditivo. Saindo do player aberto para outra tela (artista, editor de tags), o `:app` guarda a tela de origem (`reopenPlayerOn`) e reabre o player ao voltar até ela.
- **Aleatório é nosso, não do ExoPlayer.** O serviço usa `ShuffleOrder.UnshuffledShuffleOrder`, então a fila do player é sempre a ordem real. `ShuffleState` (classe pura, testada) embaralha com Fisher-Yates, guarda a ordem original e restaura ao desligar. `shuffleModeEnabled` só guarda se o aleatório está ligado. Toda nova forma de tocar aleatório passa por `PlaybackController.playShuffled` ou `toggleShuffle`.
- **"Mais ouvidas":** quem decide se uma reprodução conta é `PlayCountTracker` (puro, testado) dentro do `PlaybackService`: conta se o usuário **escolheu** a música (lista ou fila, marcada por `ChosenSongSignal` no controller) ou se ela tocou **na ordem normal**; **não** conta se entrou sozinha pelo aleatório; exige **30 s ouvidos de verdade** (80% em músicas menores que 30 s); uma vez por reprodução. O serviço avisa por `PlaybackHooks.onPlayCounted`, que o `BoomApplication` liga às estatísticas (`data/wellbeing`).
- **Lembrar onde parou:** o `PlaybackService` guarda a fila, a música e o ponto (`PlaybackSnapshotStore`, arquivo em `filesDir/player`) a cada mudança e a cada ~5 s tocando, e restaura pausado ao abrir. Fila vazia (música encerrada pelo mini player) apaga o arquivo: o app volta limpo. O aleatório não é restaurado (a fila volta na ordem em que estava).
- **Histórico:** regra diferente das "mais ouvidas": entra **toda** música ouvida 30 s (inclusive as do aleatório), uma vez por reprodução (`PlayCountTracker.checkListened` → `PlaybackHooks.onListened` → `HistoryData`).
- **Navegação sem toque repetido:** toda navegação passa por `navigateOnce` / `popBackStackOnce` (core/common). `navigateOnce` só ignora o toque enquanto a tela que ela mesma acabou de abrir ainda está entrando (não RESUMED); depois de voltar, a tela de baixo aceita toque na hora, sem esperar a animação de saída. `popBackStackOnce` age só com a tela atual RESUMED. Nunca chame `navigate` ou `popBackStack` direto (só a troca de abas no `:app`). O "voltar" do sistema também espera: um `BackHandler` no `:app`, depois do `NavHost`, segura o voltar enquanto a tela atual não está RESUMED (dois voltar seguidos deixavam duas telas sobrepostas).
- **Lançamentos lembram as escolhas:** modo, filtro e janela ficam em `ReleasesChoicesRepository` (data/releases, SharedPreferences) e voltam ao abrir.
- **Abas:** Início (músicas) · Biblioteca (gêneros, artistas, playlists, álbuns) · Perfil (seu perfil, só seu). Lançamentos e Ajustes não são abas: ficam no canto superior direito das abas, nesta ordem (sino de Lançamentos, depois Ajustes), com `BoomTopActions`; no Perfil fica só Ajustes (sem o sino). Lançamentos abre como tela de dentro (com voltar). O histórico completo fica dentro do Perfil ("Histórico" → ver tudo); não há botão de histórico no topo. Não existe aba Amigos. Tocar na aba já aberta sobe a lista ao topo (`LocalTabReselect` + `ScrollToTopOnReselect` em toda lista de aba); numa tela de dentro da aba, volta ao começo dela. A subida é `scrollToTopSmoothly`: pula sem animação para uma tela abaixo do topo e anima esses pixels de uma vez (nunca `animateScrollToItem` com o alvo fora da tela, que anda em etapas e trava).
- **Tela larga (tablet, celular deitado):** `isWideWindow` (`:app`, ≥ 840 dp, ou ≥ 600 dp e mais largo que alto) troca a barra de abas por: barra lateral `WideSidebar` (Lançamentos, Início, Biblioteca, "Nova playlist" com `NewPlaylistDialog`, Favoritos e `FollowedArtistsList` de feature/releases; recolhe para só ícones), as telas com o mini player embaixo no meio, e à direita o `ArtistSidePanel` (feature/artist: foto, seguir, biografia, populares) do artista que está tocando, que recolhe numa seta. `LocalWideLayout` faz o `BoomTopActions` esconder o sino (vai para a barra lateral) e mostrar Ajustes + a sua foto (`LocalProfileButton` → `ProfileAvatarButton`, abre o Perfil). A barra lateral é uma lista só (rola inteira; `followedArtistItems` de feature/releases entra nela) e, recolhida, mantém os mesmos botões em ícones; as margens do sistema ficam por fora da largura dela. Na página de um artista (ou álbum dele) a barra recolhe e o painel some; abrir o artista já aberto não empilha outra página (`openArtist` no `:app`). Na tela larga a barra de cima não muda de cor ao rolar. O player cheio, deitado, põe a capa à esquerda e o resto à direita. Celular em pé continua igual.
- **Favoritos:** `FavoritesRepository` (data/library, `filesDir/favorites/favorites.json`, a favoritada por último primeiro). Coração no player cheio e "Favoritar" nos menus das músicas (`favoriteAction` em cada feature); a lista é a rota `collections/favorites` (`navigateToFavorites`), primeira linha da pílula Playlists e botão da barra lateral.
- **Barra de abas só nas abas:** qualquer tela de dentro (artista, álbum, playlist, gênero, histórico, lançamentos, editor, ajustes) fica sem a barra de baixo. Ajustes (e Bem-estar) e o editor de tags também escondem o mini player. Só existe **um** editor de tags na pilha (`navigateToTagEditor` troca o anterior).
- **Biblioteca:** toda leitura de músicas passa por `LibraryRepository.getTracks()`, que já aplica o filtro de Ajustes → Biblioteca (`LibraryFilter`: pastas escolhidas e duração mínima). Ela se atualiza sozinha (observador do MediaStore → `version`); telas que listam músicas recarregam quando `version` muda. Nada de pedir permissão de pasta (SAF): as pastas vêm do próprio MediaStore de áudio.
- **Blocos com capa e texto embaixo:** o toque usa `shapes.medium` com folga embaixo (e o texto com 4 dp de recuo), senão o canto arredondado corta as letras.
- **Barra de cima:** some ao rolar para baixo e volta ao subir (`enterAlwaysScrollBehavior` no `BoomScreenScaffold`; telas com `largeTitle` continuam encolhendo). Rolar por código não passa pela barra; `ScrollToTopOnReselect` zera `LocalTopBarState.contentOffset` e traz a barra de volta (`heightOffset` → 0) quando a lista chega ao topo, senão ela ficaria escondida ou com a cor de "rolado". Recolhendo, a barra vai ficando transparente (`collapsedFraction`): recolhida, a faixa das notificações mostra a página. Lista com `stickyHeader` (Início) usa `Modifier.padding(top)` em vez de `contentPadding`, senão o cabeçalho preso fica por baixo dessa faixa. Fundo próprio da página inteira (cor do perfil) vai em `pageBackground`. O perfil não tem barra presa na tela: o título com editar e Ajustes é a primeira linha da lista (`ProfileTitleBar`, `topBar = {}`), fica no topo da página e sobe junto ao rolar.
- **Títulos compridos em blocos estreitos** (ex.: adicionadas recentemente): `BoomBouncingText` (vai e volta devagar). É a única exceção ao "nada de tween": letreiro linear de velocidade constante.
- **Menu das músicas:** painel de baixo no estilo do Spotify (`BoomMenuSheet`, core/designsystem): capa e nome no topo (`BoomMenuHeader`) e opções com ícone. Cada opção tem um tipo (`BoomMenuKind`), que dá o ícone e a posição (ordem fixa: compartilhar primeiro, apagar sempre por último; `inMenuOrder`, testado). Mostre só as opções que fazem sentido para o item. Segurar a linha abre o mesmo menu (`Modifier.clickableWithMenu` + `openState` do `BoomMoreMenu`; itens sem botão ⋮ usam `BoomMenuSheet` direto). Toda música (inclusive a que toca, na fila) tem ⋮. "Ir para o álbum" usa o `albumId` (chave da coleção de álbum). "Apagar do aparelho" usa `LibraryRepository.delete` (Android 11+: `createDeleteRequest`, o sistema confirma; 10: `RecoverableSecurityException`; 8–9: permissão de gravar) e tira a música da fila (`PlaybackController.removeUri`). "Detalhes" (`LibraryRepository.details` → `BoomSongDetailsDialog`, core/designsystem) mostra caminho, formato, tamanho, taxa de bits e de amostragem. Puxador de arrastar marca `Modifier.trackPress`; com o dedo nele, segurar não abre o menu (`longPressEnabled`). Segurar um grupo abre o menu dele: álbum (`AlbumMenuSheet`), artista, gênero, playlist e Favoritas (`GroupMenuSheet`, feature/library: tocar, aleatório, playlist, artista); no perfil, mais ouvidas e histórico (`ProfileSongMenuSheet`, com `ProfileSongMenu` do `:app`) e seus artistas (`ProfileArtistMenuSheet`). Todo bloco ou linha que toca algo abre o menu ao segurar.
- **Painéis de baixo:** sempre `BoomModalSheet` (core/designsystem), nunca `ModalBottomSheet` direto. O voltar do M3 fica desligado (`shouldDismissOnBackPress = false`, que encolhia o painel com a animação do sistema) e o nosso `BackHandler` fecha descendo (`sheetState.hide()`). Quem define animação é o app, não o sistema.
- **Compartilhar com o app favorito:** `ShareTarget` (core/common, SharedPreferences) guarda o pacote escolhido em Ajustes → Compartilhar; `LocalFavoriteShare` faz os menus mostrarem "Compartilhar com <app>", que passa por `shareLink` com `setPackage` (cai no seletor se o app sumiu). O manifesto tem `<queries>` para `ACTION_SEND text/plain`.
- **Ajustes de reprodução:** `PlaybackSettings` (core/player). Transição entre músicas (0–12 s, volume por `transitionVolume` a cada 50 ms), fade ao tocar/pausar (`FadingPlayer`, um `ForwardingPlayer` com `VolumeMixer`), velocidade (0,5–2×) e timer para dormir (não é guardado; o "fim da música" usa `pauseAtEndOfMediaItems`). Sem pausa entre faixas (gapless) é nativo do ExoPlayer, não é ajuste.
- **Busca sem pular:** lista com busca (Início, Artistas) ganha um `Spacer(fillParentMaxHeight())` no fim enquanto há texto buscado; sem ele, poucos resultados encurtam a lista e a tela desce a cada letra. Cada letra digitada traz a tela de volta até a busca (`bringToSearch`, perto anima, longe pula).
- **Trocar de música deslizando:** `QueuePager` (feature/nowplaying, um `HorizontalPager` sobre a fila na ordem de tocar) no mini player (capa e nome; botões parados) e na capa do player (cada página carrega a própria capa; reaproveitar a da música atual fazia a anterior piscar): a vizinha já aparece ao puxar, e ao parar numa página troca na hora por `PlaybackController.skipToQueueItem` (não conta como "escolhida", mantém tocando/pausado). Troca por fora (botões, fim da música) move o carrossel junto. Esquerda = próxima, direita = anterior. Só o **dedo** troca de música pelo carrossel (`DragInteraction`), nunca o pager se ajeitando sozinho; o pager **não** usa `key` (com ela, trocar a fila inteira, como no Aleatório, levava o pager até a música antiga e a fazia tocar de novo), o estado de cada página fica no `key()` de dentro. Depois do dedo, espera o player confirmar a troca (até 1,5 s) antes de seguir o player, senão piscava na anterior. Tocar no Aleatório de novo (`playShuffled`) sempre começa por uma música diferente da que tocava.
- **Letra:** a posição de leitura (`LyricsScroll`) fica fora da tela da letra, por música: ir à capa e voltar não perde o ponto.
- **Mix:** no menu do player. `buildMix` (feature/nowplaying, puro e testado): até 5 parecidas (mesmo artista ou gênero) e depois o resto da biblioteca misturado, sem a atual; vai para a fila por `PlaybackController.replaceUpNext` (mantém o que já tocou e a atual, troca o que vem depois).
- **Busca de artistas (Biblioteca → Artistas):** pílula `BoomSearchField`; mostra os do aparelho que batem e, do Deezer (`DeezerRepository.searchArtists`), os que você não tem; tocar abre a página do artista.
- **Listas longas:** a barrinha mede a lista em pixels (linha típica × itens) e o balão mostra o item proporcional à posição da bolinha (no fim do trilho, o último). Com `startIndex`, ela só cobre a lista a partir daquele item e some fora dele (no Início, só a seção "Músicas"). A fila ("A seguir") não tem balão de letra (`label = { null }`), porque não está em ordem alfabética. Toda lista de músicas ou grupos leva `BoomFastScroller` (core/designsystem) por cima, alinhado à direita, com o rótulo `boomIndexLabel(título)`; em listas por data o rótulo é a seção ("Hoje"). Puxador de reordenar fica **ao lado da capa** (à esquerda), e "remover" fica no menu ⋮, nunca como botão solto.
- **Dispositivos:** `AudioOutputs` (core/player) lista as saídas conectadas pelo `AudioManager` (sem permissão) e a escolha vai para o player por `setPreferredAudioDevice` dentro do `PlaybackService`. "Mais dispositivos" abre o seletor do Android 14+ (`MediaRouter2.showSystemOutputSwitcher`). Nada de Cast nem de permissão de Bluetooth. A marca na lista segue a última saída tocada (`chosen`), mesmo a automática: a rota que o sistema informa demora a mudar.
- **Barra de progresso do player:** arrastando a bolinha, a onda continua mostrando o ponto real da música e só a bolinha segue o dedo; um balão (`dragLabel`) e o tempo da esquerda (`onDragPosition`) mostram o alvo. Perto do ponto real (14 dp) a bolinha gruda nele (ímã, com vibração) e soltar ali não faz nada. Arrastando, a trilha fica reta (volta a ondular ao soltar) e o tempo da esquerda continua mostrando o ponto real (o alvo fica só no balão). O gesto é um só (`awaitEachGesture`) e consome tudo desde o toque, inclusive o vertical: senão o painel do player sentia o arrasto e dava trancos. Desenhada à mão em `BoomSeekBar` (onda grossa na parte tocada, grudada na bolinha redonda; arrastando, a bolinha abre um buraco na trilha; folga só do lado da trilha reta, que tem um ponto no fim); a onda corre com `withFrameNanos`, sem `tween`.
- **Início:** a busca fica ao lado do título "Músicas" (`BoomSearchField`), num `stickyHeader` que não sai do lugar enquanto digita (o topo continua montado durante a busca, para a lista não pular). "Ver todos" em Álbuns e Artistas leva à aba Biblioteca já na pílula certa (`CollectionsRequest`). Topo com Aleatório + Tocar, "Adicionadas recentemente" (grade de 3 linhas numerada), Álbuns e Artistas (`buildHomeSections`, puro e testado), depois a lista de músicas.
- **Aviso de atualização:** `UpdatePromptHost` (feature/settings, colocado no :app) consulta o GitHub no máximo a cada 6 h e mostra as notas da Release em tela cheia. "Agora não" adia 1 dia; o sino adia 1 semana / 1 mês ou pula a versão (`UpdatePromptState` em SharedPreferences, `data/updates`). Só aparece quando existe Release publicada.
- **Tags automáticas:** `searchTrack` tenta a busca precisa (artista + título), a livre e, sem nada, as músicas da página do artista no Deezer (`pickArtist` + `/artist/{id}/top`, mais as faixas dos álbuns dele que batem com o álbum ou o nome da música, `albumsForTrack`; comparadas por `rankArtistTracks`, que conta o artista como o seu: em remix o artista principal no Deezer pode ser outro). Entra sempre que a busca não dá resposta segura (abaixo de `AUTO_PICK_CONFIDENCE`), não só quando não acha nada. Título com quase nada em comum (abaixo de `MIN_TITLE_SIMILARITY`, ex.: só "Slowed") nunca vira opção.
- **MusicBrainz (reserva):** `data/musicbrainz` entra só quando o Deezer não acha (ou não responde): nas tags automáticas (gravações, comparadas por `rankCandidates`; capa do Cover Art Archive) e na página do artista (discografia com capas; entre homônimos, ganha quem tem mais álbuns do aparelho). Perfil vindo de lá não deixa seguir (o radar usa o Deezer) e o álbum abre no site. Máximo 1 consulta por segundo e User-Agent do app (regra do MusicBrainz); respostas em cache por 7 dias. Ids viram Long negativo (`musicBrainzKey`), para nunca bater com os do Deezer.
- **Lançamentos:** discografia dos artistas seguidos (`DeezerRepository.artistReleases`), em "Mais recentes" ou "Por artista", com filtro Álbuns/EPs/Singles e etiqueta "Novo" (30 dias) e janela de tempo (último mês, 6 meses — padrão — ou 1 ano); a discografia inteira fica no perfil do artista. Lógica pura em `ReleasesLogic.kt`, testada.
- Troca de barra do topo, letra, fila e telas: **sempre animadas**. Toda tela/painel novo precisa de transição e de "voltar" que feche só ele (com `PredictiveBackHandler` quando for painel).
- Todo movimento usa `boomSpatialSpec` / `boomFastSpatialSpec` / `boomEffectsSpec` / `boomScreenSpec` (`core/designsystem/theme/Motion.kt`), que vêm do motionScheme. Nada de `tween(300)` à mão.
- **Navegação entre telas** (no `app`): a tela nova entra deslizando por cima da anterior (que recua e escurece) e sai deslizando de volta, com `boomScreenSpec` (sem quique, para não abrir fresta na borda). O voltar preditivo usa as mesmas animações (`predictivePopEnterTransition`/`predictivePopExitTransition` do `NavHost`); nunca deixe o padrão do Android (cartão que encolhe e some). A direção (abrir ou voltar) é decidida pelo `:app` olhando a pilha (`isBackNavigation`), não pelo NavHost: no voltar preditivo ele às vezes aplicava as animações de abrir, deixando as duas telas sobrepostas e transparentes.
- Ícones de player (pausa, próxima, aleatório…) são desenhados em `BoomIcons` a partir dos caminhos dos Material Icons. Não adicione o pacote `material-icons-extended` (pesa vários MB).

## Testes

- Toda lógica de `data/*` e todo ViewModel novo vem com teste unitário.
- Comparação de discografia, associação de artistas e regras dos avisos de tela são as partes mais sensíveis e precisam ter teste cobrindo casos reais (nomes com acento, "feat.", artistas homônimos).
- A Action roda os testes antes do build. Teste falhando significa que não sai versão.

## Limitações desta sessão (aprendidas no BusBus)

- **Artefatos da Action não baixam daqui** (blob storage da Azure dá 403). O usuário baixa o APK pela aba Actions do GitHub ou pela Release.

- **Nada do Gradle compila aqui:** o Google Maven, o Maven Central, o Gradle Plugin Portal e o `services.gradle.org` estão fora da lista de rede liberada. A única prova real é a Action do GitHub. Por isso, antes de usar uma versão nova de biblioteca, confirme na documentação oficial (via WebFetch) que ela existe e o que exige (compileSdk, versão do AGP).
- **Quando a Action falhar**, ela mesma posta o resumo do erro como comentário no commit. Leia com `gh api repos/rilclesaraiva/BoomListener/commits/<sha>/comments`.
- **Logs da Action não são legíveis daqui** (`gh run view --log` dá 403). É por isso que o workflow já posta o erro no commit sozinho.
- **Secrets e Releases do GitHub dão 403 para esta sessão** (criar, editar e apagar). Disparar o workflow (`gh workflow run`) funciona. Quem cadastra os Secrets de assinatura é o usuário, pelo painel (Settings → Secrets and variables → Actions).
- **Deletar branches remotas é bloqueado.** Peça ao usuário.
- Na Action: **JDK 21**, e **sem** `android-actions/setup-android` (o runner já tem o SDK, e essa action quebrou o build no BusBus).
- Os Secrets de assinatura estão cadastrados: a Action gera `boomlistener_v{versão}.apk` assinado, como artefato. Sem eles, cairia no APK de debug. Release só por disparo manual (ver regra acima).
- O alias da chave (`boomlistener`) é um Secret, então o GitHub esconde essa palavra nos logs (aparece `***`). É só cosmético.

## Build: AGP 9

- **AGP 9.4 + Gradle 9.6 + Kotlin 2.4**, `compileSdk 37`, `targetSdk 36`, `minSdk 26`, Java 17.
- O AGP 9 tem **Kotlin embutido**: nunca aplique `org.jetbrains.kotlin.android` num módulo (ele só é declarado na raiz para fixar a versão do Kotlin). `kapt` não funciona; use KSP.
- Plugins de convenção usam a DSL nova: `CommonExtension` **sem** parâmetros de tipo, `ApplicationExtension` e `LibraryExtension`. Nada de `BaseExtension` ou `applicationVariants`.
- Todo módulo é uma biblioteca Android (inclusive `core/model`), para não misturar alvos de JVM. `core/model` continua sem nenhum `import android.*`, por regra.

## Segurança

- **Nenhuma chave secreta no repositório:** nem keystore de release, nem chave do Last.fm. Única exceção: `app/debug.keystore` (senha pública `android`), que existe só para o APK de debug atualizar por cima sem desinstalar. A keystore vive só nos Secrets do GitHub; a chave e o segredo do Last.fm só nos Secrets do Cloudflare Worker (`proxy/`).
- A **keystore de release** é deste app (não é a do BusBus): `boomlistener-release.p12`, alias `boomlistener`, SHA-256 `8E:66:90:F2:B7:75:BD:FB:13:0F:F7:4F:28:87:18:CA:BD:8C:2B:8C:3B:09:75:11:FA:0A:D2:AE:8C:58:09:A8`. Gerada em PKCS12 moderno (JDK 21), sem o problema de criptografia legada do BusBus. Se ela for perdida, o app não recebe mais atualização. O usuário precisa guardar uma cópia fora do GitHub.

## Perfil (sem social)

- **O app não tem nada de social** (decisão do rilclesaraiva, 0.33.0): sem amigos, sem "ouvindo agora", sem recomendações, sem reações, sem redes no perfil, sem servidor. Música local e social não combinam aqui. **Não volte a criar nada disso.**
- O perfil é **só da pessoa e só no aparelho**, como deixar o próprio sistema bonito para si mesmo: nome, descrição (até 160 letras), cor, foto e capa. Módulos: `data/profile` (`ProfileRepository` → `LocalProfileRepository`, arquivo `filesDir/profile/profile.json` e as imagens `filesDir/profile/{avatar,banner}`) e `feature/profile` (aba Perfil: `MY_PROFILE_ROUTE` = `profile`).
- Imagens vêm do Photo Picker (sem permissão), recortadas e reduzidas no aparelho (`prepareProfileImage`: foto 512×512, capa 1080×405, WebP); GIF de até 8 MB vai como está, GIF maior ou vídeo de até 15 s vira GIF menor (`animatedProfileGif` + `GifEncoder`, 6 a 15 quadros por segundo; abaixo disso fica travado). A versão da imagem é a hora em que foi trocada (a tela relê quando muda).
- "Editar perfil" (lápis, ou tocar na foto, na capa ou no nome) só grava ao tocar em Salvar e não fecha ao puxar para baixo (`sheetGesturesEnabled = false`; fecha pelo X, voltar ou tocando fora). A cor tinge o perfil (`Modifier.profileBackground`).
- Estatísticas calculadas no aparelho (`ProfileStats.kt`, testado): números do mês, mais ouvidas, tempo por dia, horários de pico, seus artistas e histórico.
- **Migração da 0.32:** na primeira abertura, `LocalProfileRepository` traz nome, descrição, cor, foto e capa de `filesDir/social` e apaga essa pasta inteira (o @, o segredo e o resto do social). O servidor (`boomlistener-social` na Cloudflare) foi apagado pelo rilclesaraiva em 20/10/2026 e o código dele saiu do repositório. O único Worker que resta é o proxy do Last.fm.

## Proxy do Last.fm

- `proxy/worker.js` (Cloudflare Worker `boomlistener-lastfm`, publicado pelo rilclesaraiva pelo painel). Só aceita métodos públicos de leitura e acrescenta a chave. Endereço no app: `PROXY_URL` em `data/lastfm/ProxyLastfmRepository.kt`.
- A chave e o segredo do Last.fm ficam **só** nos Secrets do Worker (`LASTFM_API_KEY`, `LASTFM_SHARED_SECRET`). Nunca escreva os valores em arquivo, commit, log ou mensagem.
- Esta sessão não tem acesso à Cloudflare: qualquer mudança no `worker.js` precisa ser colada de novo no painel pelo usuário.

## Licença

GPL-3.0, igual ao BusBus. Cabeçalho de licença no topo de cada arquivo-fonte.
