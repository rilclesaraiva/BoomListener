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
- Mudanças no banco Room **sempre** com migração escrita e testada. Nunca use `fallbackToDestructiveMigration`, porque isso apagaria a biblioteca, os artistas seguidos e as estatísticas do usuário.

## Regra fixa: entregas com a versão no nome

Todo APK entregue ao usuário se chama `boomlistener_v{versão}.apk` (ex.: `boomlistener_v1.0.0.apk`), nunca `app-release.apk`.

## Player

- A reprodução mora em `core/player`: `PlaybackService` (Media3 `MediaSessionService` + ExoPlayer) toca de verdade; as telas falam só com a interface `PlaybackController` (`PlayerConnection.controller(context)`).
- Notificação, tela de bloqueio, fone Bluetooth e Android Auto vêm de graça do `MediaSession`; não crie notificação de mídia à mão.
- Desde o Media3 1.11, o `MediaSession` exige ser usado na thread principal. O controller já posta tudo no `Looper` principal; mantenha assim.
- O player em tela cheia **não é uma tela da navegação**: é um painel (`PlayerSheet`) por cima do app inteiro, controlado por `PlayerSheetState` (0 = fechado, 1 = aberto). O mini player abre (toque ou puxar para cima) e encerra a música (puxar para baixo). O "voltar" do sistema fecha o painel com voltar preditivo.
- **Aleatório é nosso, não do ExoPlayer.** O serviço usa `ShuffleOrder.UnshuffledShuffleOrder`, então a fila do player é sempre a ordem real. `ShuffleState` (classe pura, testada) embaralha com Fisher-Yates, guarda a ordem original e restaura ao desligar. `shuffleModeEnabled` só guarda se o aleatório está ligado. Toda nova forma de tocar aleatório passa por `PlaybackController.playShuffled` ou `toggleShuffle`.
- **"Mais ouvidas":** quem decide se uma reprodução conta é `PlayCountTracker` (puro, testado) dentro do `PlaybackService`: conta se o usuário **escolheu** a música (lista ou fila, marcada por `ChosenSongSignal` no controller) ou se ela tocou **na ordem normal**; **não** conta se entrou sozinha pelo aleatório; exige **30 s ouvidos de verdade** (80% em músicas menores que 30 s); uma vez por reprodução. O serviço avisa por `PlaybackHooks.onPlayCounted`, que o `BoomApplication` liga às estatísticas (`data/wellbeing`).
- **Lembrar onde parou:** o `PlaybackService` guarda a fila, a música e o ponto (`PlaybackSnapshotStore`, arquivo em `filesDir/player`) a cada mudança e a cada ~5 s tocando, e restaura pausado ao abrir. Fila vazia (música encerrada pelo mini player) apaga o arquivo: o app volta limpo. O aleatório não é restaurado (a fila volta na ordem em que estava).
- **Histórico:** regra diferente das "mais ouvidas": entra **toda** música ouvida 30 s (inclusive as do aleatório), uma vez por reprodução (`PlayCountTracker.checkListened` → `PlaybackHooks.onListened` → `HistoryData`).
- **Abas:** Início (músicas) · Lançamentos · Biblioteca (gêneros, artistas, playlists, álbuns). Ajustes e Histórico não são abas: ficam no canto superior direito de toda aba, sempre com `BoomTopActions`. Tocar na aba já aberta sobe a lista ao topo (`LocalTabReselect` + `ScrollToTopOnReselect` em toda lista de aba); numa tela de dentro da aba, volta ao começo dela.
- **Barra de abas só nas abas:** qualquer tela de dentro (artista, álbum, playlist, gênero, histórico, editor, ajustes) fica sem a barra de baixo. Ajustes (e Bem-estar) e o editor de tags também escondem o mini player. Só existe **um** editor de tags na pilha (`navigateToTagEditor` troca o anterior).
- **Biblioteca:** toda leitura de músicas passa por `LibraryRepository.getTracks()`, que já aplica o filtro de Ajustes → Biblioteca (`LibraryFilter`: pastas escolhidas e duração mínima). Ela se atualiza sozinha (observador do MediaStore → `version`); telas que listam músicas recarregam quando `version` muda. Nada de pedir permissão de pasta (SAF): as pastas vêm do próprio MediaStore de áudio.
- **Blocos com capa e texto embaixo:** o toque usa `shapes.medium` com folga embaixo (e o texto com 4 dp de recuo), senão o canto arredondado corta as letras.
- **Menu das músicas:** segurar a linha abre o mesmo menu ⋮ (`Modifier.clickableWithMenu` + `openState` do `BoomMoreMenu`). Toda música (inclusive a que toca, na fila) tem ⋮. "Apagar do aparelho" usa `LibraryRepository.delete` (Android 11+: `createDeleteRequest`, o sistema confirma; 10: `RecoverableSecurityException`; 8–9: permissão de gravar) e tira a música da fila (`PlaybackController.removeUri`).
- **Listas longas:** toda lista de músicas ou grupos leva `BoomFastScroller` (core/designsystem) por cima, alinhado à direita, com o rótulo `boomIndexLabel(título)`; em listas por data o rótulo é a seção ("Hoje"). Puxador de reordenar fica **ao lado da capa** (à esquerda), e "remover" fica no menu ⋮, nunca como botão solto.
- **Dispositivos:** `AudioOutputs` (core/player) lista as saídas conectadas pelo `AudioManager` (sem permissão) e a escolha vai para o player por `setPreferredAudioDevice` dentro do `PlaybackService`. "Mais dispositivos" abre o seletor do Android 14+ (`MediaRouter2.showSystemOutputSwitcher`). Nada de Cast nem de permissão de Bluetooth.
- **Barra de progresso do player:** desenhada à mão em `BoomSeekBar` (onda grossa na parte tocada, grudada na bolinha redonda; folga só do lado da trilha reta, que tem um ponto no fim); a onda corre com `withFrameNanos`, sem `tween`.
- **Início:** a busca fica ao lado do título "Músicas" (`BoomSearchField`), não no topo. "Ver todos" em Álbuns e Artistas leva à aba Biblioteca já na pílula certa (`CollectionsRequest`). Topo com Aleatório + Tocar, "Adicionadas recentemente" (grade de 3 linhas numerada), Álbuns e Artistas (`buildHomeSections`, puro e testado), depois a lista de músicas. Na busca, o topo some.
- **Aviso de atualização:** `UpdatePromptHost` (feature/settings, colocado no :app) consulta o GitHub no máximo a cada 6 h e mostra as notas da Release em tela cheia. "Agora não" adia 1 dia; o sino adia 1 semana / 1 mês ou pula a versão (`UpdatePromptState` em SharedPreferences, `data/updates`). Só aparece quando existe Release publicada.
- **Lançamentos:** discografia dos artistas seguidos (`DeezerRepository.artistReleases`), em "Mais recentes" ou "Por artista", com filtro Álbuns/EPs/Singles e etiqueta "Novo" (30 dias) e janela de tempo (último mês, 6 meses — padrão — ou 1 ano); a discografia inteira fica no perfil do artista. Lógica pura em `ReleasesLogic.kt`, testada.
- Troca de barra do topo, letra, fila e telas: **sempre animadas**. Toda tela/painel novo precisa de transição e de "voltar" que feche só ele (com `PredictiveBackHandler` quando for painel).
- Todo movimento usa `boomSpatialSpec` / `boomFastSpatialSpec` / `boomEffectsSpec` / `boomScreenSpec` (`core/designsystem/theme/Motion.kt`), que vêm do motionScheme. Nada de `tween(300)` à mão.
- **Navegação entre telas** (no `app`): a tela nova entra deslizando por cima da anterior (que recua e escurece) e sai deslizando de volta, com `boomScreenSpec` (sem quique, para não abrir fresta na borda). O voltar preditivo usa as mesmas animações (`predictivePopEnterTransition`/`predictivePopExitTransition` do `NavHost`); nunca deixe o padrão do Android (cartão que encolhe e some).
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

## Amigos (social)

- **Nunca passa áudio.** Ouvir junto por transmissão foi descartado (seria transmitir obra protegida sem licença). Só texto e cartões de música (título, artista, álbum, capa e link), e o "ouvindo agora".
- Servidor: `social/worker.js` (Cloudflare Worker `boomlistener-social`, um Durable Object `Hub` com SQLite, importado do GitHub com caminho `/social`). Testes do servidor: `node --test social/worker.test.mjs` (rodam aqui na sessão).
- **Sem login e sem nada do Google** (decisão do rilclesaraiva): o perfil é um @ + nome; o app recebe id + segredo e guarda em `filesDir/social/account.json`. Sem Firebase: mensagens chegam com o app aberto (consulta a cada 30 s; 15 s na aba Amigos; 4 s no chat).
- Conversas ficam **só no aparelho** (`filesDir/social/chats`); o servidor apaga cada mensagem quando o app confirma que recebeu.
- "Ouvindo agora" e recentes: **desligado por padrão**, só para quem conversa com a pessoa ou divide grupo. Desligar apaga do servidor.
- Amigo é por @ (abrir o perfil e mandar mensagem). Código de convite só para grupos.
- **Seu perfil** (`social/me`, atalho com a sua inicial no canto superior direito de toda aba via `LocalProfileShortcut`): números do mês, mais ouvidas, tempo por dia, horários de pico, seus artistas, histórico e quem você segue. Tudo calculado no aparelho (`ProfileStats.kt`, testado); funciona até sem perfil em Amigos.
- **Foto, capa e descrição do perfil:** a imagem vem do Photo Picker (sem permissão), é recortada e reduzida no aparelho (`prepareProfileImage`: foto 320×320, capa 1200×450, JPEG até 380 KB) e vai para o servidor (`PUT /v1/me/image/{avatar|banner}`, tabela `images`). O perfil traz só a versão (`avatar`, `banner`); o app baixa uma vez e guarda em `filesDir/social/images`. Descrição até 160 letras (`bio`). GIF animado até 1,4 MB vai como está (servidor aceita até 1,5 MB) e roda pelo `ImageDecoder` (Android 9+). Vídeo → GIF ainda não existe. Quem bloqueou não deixa ver a foto.
- Módulos: `data/social` (cliente e armazenamento) e `feature/social` (aba Amigos, chat, perfil). A folha `RecommendSheet` é usada pelo `:app` nos menus da biblioteca e do player.

## Proxy do Last.fm

- `proxy/worker.js` (Cloudflare Worker `boomlistener-lastfm`, publicado pelo rilclesaraiva pelo painel). Só aceita métodos públicos de leitura e acrescenta a chave. Endereço no app: `PROXY_URL` em `data/lastfm/ProxyLastfmRepository.kt`.
- A chave e o segredo do Last.fm ficam **só** nos Secrets do Worker (`LASTFM_API_KEY`, `LASTFM_SHARED_SECRET`). Nunca escreva os valores em arquivo, commit, log ou mensagem.
- Esta sessão não tem acesso à Cloudflare: qualquer mudança no `worker.js` precisa ser colada de novo no painel pelo usuário.

## Licença

GPL-3.0, igual ao BusBus. Cabeçalho de licença no topo de cada arquivo-fonte.
