# CLAUDE.md

Lido no início de cada sessão: é como o Claude recupera o contexto do projeto. Detalhes técnicos de cada parte (player, ajustes, perfil, build) ficam no [`ARCHITECTURE.md`](ARCHITECTURE.md); o histórico, no [`CHANGELOG.md`](CHANGELOG.md). Em conflito entre uma instrução antiga do chat e este arquivo, **vale este arquivo**. Decisão nova que mude algo aqui entra neste arquivo no mesmo commit.

## Projeto
**Boom, listener!** (trocadilho com *sonic boom* + ouvinte; no ícone, **BoomListener**): reprodutor de música **local** para Android, em Material 3 Expressive, com edição de tags, página de artista (Deezer + Last.fm) e radar de lançamentos dos artistas seguidos. Gratuito, sem anúncios, sem conta. Público: **global** (beta pública desde a 0.36.2).

## Mantenedor
- Nome: **rilclesaraiva** · Contato: issues do GitHub (sem e-mail público)
- Apoio: nenhum link de doação · Licença: GPL-3.0 (cabeçalho de licença no topo de cada arquivo-fonte)
- Segredos ficam em: Google Drive (pasta privada do mantenedor)

## Stack
- Stack: Kotlin 2.4 + Jetpack Compose (Material 3 Expressive) + Media3. AGP 9.4, Gradle 9.6, `compileSdk 37`, `targetSdk 36`, `minSdk 26`, Java 17 (o Gradle roda com JDK 21).
- Build: `./gradlew assembleRelease` · Testes: `./gradlew testDebugUnitTest`. **Nada do Gradle compila nesta sessão** (repositórios Maven fora da rede liberada): a única prova é o CI.
- Mapa (módulos Gradle): core → `core/*` · data → `data/*` · features → `feature/*` (no singular, decisão do mantenedor) · entrada → `app`.
- Versão: `APP_VERSION` em `gradle.properties` (`versionCode = MAJOR*10000 + MINOR*100 + PATCH`; nunca editar `versionCode`/`versionName` direto).
- Release: tag `vX.Y.Z` → `.github/workflows/ci.yml` → `boomlistener_vX.Y.Z.apk` na Release do GitHub. Build para vistoria: artefato `build-para-vistoria-…` de cada push.
- Pacote: `com.boomlistener.app`, fixo para sempre · R8 ativo no release, nunca desligar (com `-dontobfuscate`: os nomes ficam legíveis nos erros, decisão do mantenedor; problema só no release → regra `-keep` em `app/proguard-rules.pro`).
- Ícone: `app/src/main/res/mipmap-anydpi*/` e `drawable/ic_launcher_*`; só muda se o mantenedor pedir.

## Arquitetura
Módulos Gradle com dependências só em uma direção: `app → feature → data → core`. Implementações de `data` são `internal` (o resto só vê a interface `…Repository`). Detalhes, mapa dos módulos e comportamentos em [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Regras de estrutura
- Feature não importa outra feature; o compartilhado sobe para core/data. Navegação entre features passa pelo `app`.
- Banco, APIs e arquivos só via data. Core sem regra de negócio.
- Código novo vai para o módulo da responsabilidade dele. Sem módulo adequado, **perguntar antes** de criar um ou de usar um "quase certo".
- Dependência nova só em `gradle/libs.versions.toml` (nunca versão escrita num `build.gradle.kts`), e só justificada. Antes de usar versão nova, conferir na documentação oficial que ela existe e o que exige.
- Configuração repetida entre módulos vai para os plugins de `build-logic/`. AGP 9 tem Kotlin embutido: nunca aplicar `org.jetbrains.kotlin.android` num módulo; `kapt` não funciona (KSP).
- Dados da pessoa ficam em DataStore e arquivos (sem Room). Mudança no formato do que já está guardado **sempre** com migração escrita e testada; nunca apagar dados para "começar limpo". Arquivo novo de dados da pessoa entra no backup (`USER_DIRS` do `BackupEngine`).

## Limites do app (não mudam)
- **Nunca reproduz** conteúdo de serviços externos. Fontes externas são só informação e links "abrir em…". Compartilhar manda o link de **busca** do YouTube Music; o banner manda imagem ou vídeo com **no máximo 30 s** da música, nunca a música inteira. Nada de API do YouTube, da API interna do YouTube Music, Piped/Invidious ou Bandcamp.
- **Sem nada de social** (decisão de 0.33.0): sem amigos, "ouvindo agora", recomendações, reações nem servidor. O perfil é só da pessoa e só no aparelho.
- **Permissões:** só áudio (`READ_MEDIA_AUDIO`; `READ_EXTERNAL_STORAGE` até o 12; `WRITE_EXTERNAL_STORAGE` só no 8 e 9), `POST_NOTIFICATIONS` só para avisos de lançamentos. **Nunca** fotos/vídeos (imagens pelo Photo Picker), `BLUETOOTH_CONNECT`, `RECORD_AUDIO` (nada da API `Visualizer`) ou Cast. No Android 10+ gravar tags é por `MediaStore.createWriteRequest`.
- **Modo offline:** todo `openConnection` novo chama `OfflineMode.requireOnline()` antes; só a verificação de atualizações passa.
- Identidade visual própria: inspiração de layout pode, copiar marca ou visual de outro app não.

## Design (Material 3 Expressive)
- Nenhuma cor, fonte ou forma "na mão" numa tela: `MaterialTheme.colorScheme`, `.typography`, `.shapes`, `.motionScheme`. Valores próprios só em `core/designsystem`. Cores em par com o "on" correspondente.
- Componentes do `androidx.compose.material3`; componente próprio só sem equivalente, em `core/designsystem`. APIs experimentais só dentro de `core/designsystem`, embrulhadas.
- Edge-to-edge, temas claro, escuro e AMOLED, cor dinâmica no Android 12+.
- Todo movimento usa `boomSpatialSpec`/`boomFastSpatialSpec`/`boomEffectsSpec`/`boomScreenSpec` (nada de `tween(300)` à mão). Tela/painel novo tem transição e "voltar" que fecha só ele; tela nova da navegação usa `BoomNavScreen`. Painéis de baixo sempre `BoomModalSheet`.
- Toda opção dos Ajustes mora numa categoria (Aparência, Reprodução, Biblioteca, Compartilhamento, Bem-estar, Sistema). Se nenhuma serve, perguntar.

## Idiomas do app
- Inglês (padrão, `res/values`), português e espanhol: **toda** chave nova nos três. Nenhum texto visível escrito no Kotlin (inclusive `contentDescription`).
- Em português, "remover" e não "tirar".
- README, CHANGELOG e páginas legais em inglês; CLAUDE.md, ARCHITECTURE.md e mensagens de commit em português.

## Fluxo de trabalho
- Uma tarefa por vez, mudanças pequenas. Não refatorar fora do escopo. Na dúvida, perguntar. Nunca inventar API (conferir no código-fonte ou na documentação).
- Toda lógica de `data/*` e todo ViewModel novo vem com teste unitário. Partes sensíveis (discografia, associação de artistas, avisos) com casos reais: acentos, "feat.", homônimos.
- Registrar toda mudança em `[Unreleased]` no CHANGELOG (Added, Changed, Fixed, Removed, Security, Known issues), escrito para quem usa o app.
- **Sem subir versão** fora do release.
- Só dizer que terminou quando o CI daquele commit passar. CI vermelho é corrigido antes de qualquer tarefa nova (o erro vem como comentário no commit: `gh api repos/rilclesaraiva/BoomListener/commits/<sha>/comments`).
- Rodada de testes do mantenedor (bugs relatados um a um): corrigir sem commitar a cada bug e mandar tudo junto quando ele sinalizar que terminou ("pode mandar"); na dúvida, perguntar. Para guardar trabalho sem gerar build, usar branch `wip/…`.
- Preferir remover a acumular: o que não é usado sai. Navegação sempre por `navigateOnce`/`popBackStackOnce`.

## Release (regra absoluta)
- Só com confirmação explícita do mantenedor citando a versão ("confirmo release v1.2.0"), na conversa. "Ok", "beleza", "pode lançar" sem versão ou silêncio não contam.
- Sem confirmação: nada de tag, bump de versão, mover `[Unreleased]` ou Release.
- Toda atualização vai para os artefatos do Actions (build para vistoria) e o Claude avisa com o link da execução. O mantenedor vistoria e só então pede o release.
- Com a confirmação: (0) revisão com contexto limpo (subagente que lê só este arquivo e o diff desde o último release, procurando falhas de segurança, quebra das regras e itens do CHANGELOG que não estão no código), achados vão ao mantenedor; (1) mover `[Unreleased]` para `## [X.Y.Z] - AAAA-MM-DD`, juntando o que veio desde a última Release pública; (2) `APP_VERSION=X.Y.Z`; (3) commit `chore(release): vX.Y.Z`, tag `vX.Y.Z`, push do commit e da tag; (4) o CI confere e publica.
- Entre a vistoria e a tag só mudam versão e CHANGELOG; mudou código, nova vistoria. Notas honestas: avisar o que ainda está quebrado (tela larga, por exemplo).
- Esta sessão não apaga Releases nem tags (403): se sair algo por engano, pedir ao mantenedor.

## Segredos
- Nunca commitar chaves, keystores, senhas ou `.env`. Única exceção: `app/debug.keystore` (senha pública `android`), só para o APK de debug atualizar por cima.
- A keystore de release já existe (`boomlistener-release.p12`, alias `boomlistener`): **nunca gerar outra** (trocar impede atualizar o app). Os 4 Secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) só existem no GitHub, cadastrados pelo mantenedor.
- Chave e segredo do Last.fm só nos Secrets do Cloudflare Worker (`proxy/worker.js`, `LASTFM_API_KEY`, `LASTFM_SHARED_SECRET`): nunca escrever os valores em arquivo, commit, log ou mensagem. Mudança no `worker.js` o mantenedor cola no painel da Cloudflare.

## Projeto público
- Páginas legais em `docs/privacy.md` e `docs/terms.md` (GitHub Pages: `https://rilclesaraiva.github.io/BoomListener/privacy` e `/terms`), linkadas no README e em Ajustes → Sobre. Sem cookies nem rastreadores, então sem página de cookies.
- Coleta nova (SDK, permissão, chamada de rede) → atualizar a política, a data e o texto de Privacidade do app no mesmo commit.
- Nenhuma afirmação sem respaldo: o que README, páginas legais e notas dizem precisa ser verdade no código. Na dúvida, não afirmar.

## Apoio e contato
- README termina com a seção de apoio e contato (hoje só o contato pelas issues).
- No app: nunca citar doação ou pagamento. Só "Quer apoiar o projeto? Entre no repositório do projeto" (Ajustes → Sobre), com link para o repositório.

## Decisões recusadas (não propor de novo sem o mantenedor pedir)
- **Baseline Profile** (28/10/2026).
- **Visualizador osciloscópio XY** (0.52.0, removido na 0.52.2): pesado e ruidoso no Canvas. Se voltar: OpenGL (feixe gaussiano, fósforo, interpolação); ficou a ideia de um tipo próprio "Boom" (onda de choque a cada batida).
- Capas de playlist em 2 (metades) ou 3 ("Y") partes: só o mosaico 2 × 2 (0.51.1).
- Onda saindo do centro no vídeo do banner (0.45.0). Esconder o resto da página para disfarçar a busca (0.43.3).
- FFmpeg para formatos extras (22/10/2026).

## Limitações desta sessão
- Artefatos da Action não baixam daqui (403); assets de Release baixam pela API (`releases/assets/<id>`).
- Logs da Action não são legíveis (`gh run view --log` dá 403): o workflow posta o erro no commit.
- Secrets do GitHub dão 403 (o mantenedor cadastra). Deletar branches remotas é bloqueado.
- A API do GitHub só alcança este repositório; para outras coisas (versões de actions etc.), pesquisar na web.

## Contexto atual
- Foco: polimento do player e da fila; seguir o documento de estrutura do mantenedor (adotado em 29/10/2026).
- Problemas conhecidos: telas em celular deitado e tablet ainda quebradas em alguns pontos. Última Release pública: v0.51.2; `APP_VERSION` está em 0.53.0 (subiu antes da regra nova).
