# Instruções para o Claude neste repositório

Este arquivo é lido no início de cada sessão. É como o Claude recupera o contexto do projeto, já que perde a memória entre conversas e esquece detalhes quando o chat fica longo. Leia também:
- `docs/ARQUITETURA.md`: estrutura dos módulos e o porquê de cada um
- `CHANGELOG.md`: o que já foi feito, versão por versão

Em caso de conflito entre uma instrução antiga do chat e este arquivo, **vale este arquivo**. Se uma decisão nova mudar algo aqui, atualize este arquivo no mesmo commit.

## O app

**Nome:** Boom, listener! (trocadilho com *sonic boom* + ouvinte). No ícone da tela inicial: **BoomListener**. Repositório: `rilclesaraiva/BoomListener`. `applicationId`: `com.boomlistener.app`. **O applicationId nunca muda** depois do primeiro APK publicado.

Reprodutor de música **local** para Android, em Material 3, com edição completa de tags, página de artista enriquecida (Deezer + Last.fm) e radar de lançamentos dos artistas seguidos. Gratuito, sem anúncios, sem conta.

**Limites que não mudam:**
- O app **nunca reproduz** conteúdo de serviços externos (YouTube Music, Spotify, Deezer…). Fontes externas servem só para **informação** e para links "abrir em…".
- **Bandcamp não é usado** (não tem API pública).
- Nada de dados do usuário sai do aparelho, exceto as consultas de metadados e o scrobble no Last.fm quando o próprio usuário ativar.
- **Permissões de mídia: só áudio** (`READ_MEDIA_AUDIO`, e `READ_EXTERNAL_STORAGE` até o Android 12). **Nunca** peça fotos ou vídeos: a política da Play Store só aceita essas permissões quando galeria é a função principal do app, e as capas não precisam delas (vêm de dentro dos arquivos e do MediaStore de áudio). Capa escolhida pelo usuário no editor de tags usa o **Photo Picker**, que não exige permissão.
- A identidade visual é própria: inspiração de layout é permitida, copiar a marca ou o visual de outro app não.

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

## Regra fixa: manter o CHANGELOG.md atualizado

Toda mudança que vire versão nova (correção, novidade, ajuste visual):
1. Atualize o `CHANGELOG.md` no formato já usado (`✨ Novidades`, `🐛 Correções`, `🎨 Ajustes visuais`), escrito para quem usa o app, não para programador.
2. Faça isso **no mesmo momento** do bump de versão, nunca depois.
3. A nota tem que estar pronta antes do push: a Action usa ela como texto da Release.

## Regra fixa: agrupar bugs antes de commitar

Cada push na `main` dispara build, assinatura e Release. Quando o usuário estiver testando e reportando bugs um a um:
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

## Testes

- Toda lógica de `data/*` e todo ViewModel novo vem com teste unitário.
- Comparação de discografia, associação de artistas e regras dos avisos de tela são as partes mais sensíveis e precisam ter teste cobrindo casos reais (nomes com acento, "feat.", artistas homônimos).
- A Action roda os testes antes do build. Teste falhando significa que não sai versão.

## Limitações desta sessão (aprendidas no BusBus)

- **Artefatos da Action não baixam daqui** (blob storage da Azure dá 403). O usuário baixa o APK pela aba Actions do GitHub ou pela Release.

- **Nada do Gradle compila aqui:** o Google Maven, o Maven Central, o Gradle Plugin Portal e o `services.gradle.org` estão fora da lista de rede liberada. A única prova real é a Action do GitHub. Por isso, antes de usar uma versão nova de biblioteca, confirme na documentação oficial (via WebFetch) que ela existe e o que exige (compileSdk, versão do AGP).
- **Quando a Action falhar**, ela mesma posta o resumo do erro como comentário no commit. Leia com `gh api repos/rilclesaraiva/BoomListener/commits/<sha>/comments`.
- **Logs da Action não são legíveis daqui** (`gh run view --log` dá 403). É por isso que o workflow já posta o erro no commit sozinho.
- **Secrets e Releases do GitHub dão 403 para esta sessão.** Quem cadastra os Secrets de assinatura é o usuário, pelo painel (Settings → Secrets and variables → Actions).
- **Deletar branches remotas é bloqueado.** Peça ao usuário.
- Na Action: **JDK 21**, e **sem** `android-actions/setup-android` (o runner já tem o SDK, e essa action quebrou o build no BusBus).
- Sem os Secrets de assinatura, a Action gera um APK de **debug** (`boomlistener_v{versão}-debug.apk`, só como artefato) e não cria Release.

## Build: AGP 9

- **AGP 9.4 + Gradle 9.6 + Kotlin 2.4**, `compileSdk 37`, `targetSdk 36`, `minSdk 26`, Java 17.
- O AGP 9 tem **Kotlin embutido**: nunca aplique `org.jetbrains.kotlin.android` num módulo (ele só é declarado na raiz para fixar a versão do Kotlin). `kapt` não funciona; use KSP.
- Plugins de convenção usam a DSL nova: `CommonExtension` **sem** parâmetros de tipo, `ApplicationExtension` e `LibraryExtension`. Nada de `BaseExtension` ou `applicationVariants`.
- Todo módulo é uma biblioteca Android (inclusive `core/model`), para não misturar alvos de JVM. `core/model` continua sem nenhum `import android.*`, por regra.

## Segurança

- **Nenhuma chave no repositório:** nem keystore, nem chave do Last.fm. A keystore vive só nos Secrets do GitHub; a chave e o segredo do Last.fm só nos Secrets do Cloudflare Worker (`proxy/`).
- A **keystore de assinatura** é deste app (não é a do BusBus). Se ela for perdida, o app não recebe mais atualização. O usuário precisa guardar uma cópia fora do GitHub.

## Licença

GPL-3.0, igual ao BusBus. Cabeçalho de licença no topo de cada arquivo-fonte.
