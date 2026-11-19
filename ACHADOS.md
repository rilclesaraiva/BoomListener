# ACHADOS.md

Achados das revisões de 19/11/2026 (revisor de contexto limpo, só leitura, **sem compilar nem rodar nada**). Tudo aqui é **hipótese ou pendência**, não fato provado, salvo onde está escrito. Fora do CLAUDE.md de propósito: o mantenedor quer revisar e provar cada item em outro momento. Ao provar, anotar o resultado ao lado do item (provado / refutado / corrigido, com a data e o commit) em vez de apagar.

## A. Bugs prováveis (hipóteses)

1. **Contagem dupla ao salvar tags da música que toca** (`PlaybackService.kt` ~147-160, `Media3PlaybackController.kt`). O controller faz `addMediaItem` + `removeMediaItem`, o que deve disparar `onMediaItemTransition` e reiniciar o `playCounter.start(...)`; com mais 30 s a mesma música contaria de novo (mais ouvidas, histórico, resumo diário). O mantenedor nunca viu. *Como provar:* teste com um `Player` falso conferindo o `reason` da transição; ou, no aparelho, salvar tags tocando e ver se a contagem sobe 2 vezes.
2. **Toque duplo lento em `navigateOnce`/`popBackStackOnce`** (`SafeNavigation.kt`): trava de 180 ms pode deixar abrir a mesma tela duas vezes se o toque duplo for mais lento. O mantenedor testou e não reproduziu. *Como provar:* teste de instrumentação ou tentar toques de 200-400 ms.
3. **Tema padrão mudou** (`UserPreferences.kt:19,67`): Escuro e cor dinâmica desligada para quem nunca mexeu nos Ajustes. O CHANGELOG diz "o que você escolheu fica", verdade só para quem escolheu. O mantenedor disse que tanto faz.
4. **`recreatedMeta`** (`DailyStatsRepository.kt` ~117-122): se o `meta.json` sumir/corromper com meses já gravados mas antes da importação do histórico antigo terminar, vira `migrated=true` e o histórico antigo nunca é importado (perda silenciosa no lugar de duplicação). Caso raro. *Como provar:* teste com `meta.json` apagado e `legacy.json` ainda por importar.
5. **Importação parcial** (`DailyStatsRepository`): se gravar só parte dos meses e o app fechar antes do resto, a importação pode repetir na próxima abertura. Aceito como raro.
6. **`file://` e o cache** (`ExternalPlayerActivity.kt` `pointsToOwnPrivateData`): bloqueia tudo dentro de `dataDir`, inclusive `cacheDir`/`external-audio`. Hoje essas cópias não voltam por Intent, então nada quebra. `getExternalFilesDir` não é coberto. *Como provar:* conferir se o app grava algo em `external-files`; teste do caminho com `..` e symlink.
7. **Gravação do resumo diário sem teto** (`scheduleWrite`): se falhar para sempre, tenta a cada 3 s. No teste `migracaoComGravacaoQueFalhouNaoMarcaMigradoNoDisco` a corrotina de fundo segue depois do `tearDown` e o `flush` pode recriar a pasta temporária.
8. **`LocalizedDate.kt`**: `getBestDateTimePattern` (ICU) lido por `DateTimeFormatter.ofPattern`; testado só mentalmente em pt, es e en. Outros idiomas sem garantia. *Como provar:* testar no aparelho com outros idiomas.
9. **Download de imagem incompleto** (`HttpDeezerRepository`): o arquivo é gravado direto no cache, então um download cortado pode ficar guardado. O aviso do registro de diagnóstico que detectava isso foi removido. Nunca apareceu como problema.
10. **Memória das imagens fixadas** (`BoomImageMemory`/`PinImages`): 4 grupos com orçamento de 32 MB cada (teto teórico de 128 MB em aparelho fraco; o uso real deve ser de poucos MB). *Como provar:* medir o heap no aparelho com a biblioteca grande.
11. **Desempenho da barra de rolagem** (`BoomFastScroller`): melhorado (cache das alturas), mas ainda O(n) por quadro enquanto rola. *Como provar:* medir em biblioteca com milhares de músicas.
12. **`CalendarCard`** (`ProfilePanels.kt`): `dayAt` quebraria com `calendar.weeks` vazio; hoje o cartão só aparece com `longestStreak > 0`.
13. **Duas janelas de busca** (`BoomSearchField`): campo focado na janela principal com uma folha aberta por cima é comparado entre janelas diferentes. Considerado aceitável.

## B. Privacidade e documentação

14. **Resumo diário não é apagável pelo app.** "Limpar histórico" não apaga `stats/daily` (título, artista, álbum, gênero por dia, vai no backup). O aviso foi acrescentado ao diálogo e à `privacy.md`; falta decidir se haverá uma opção de apagar.
15. **`ExternalPlayerActivity` aceita `file://`** (`AndroidManifest.xml:52-63`). Decidido: bloquear só os caminhos privados do app (feito). Confirmar no futuro se `file://` é mesmo necessário.
16. **Dois lugares escrevem o prefixo `boomlistener_wrapped_`** (`WrappedShare.kt`, `ShareCardExport.kt`), sem constante compartilhada.
17. **Comentário velho** em `ProfilePanels.kt` (~361): KDoc de `WrappedReadyCard` ainda cita as "builds de teste com registro de diagnóstico".
18. **Warnings possíveis** (sem `allWarningsAsErrors`): `host`/`context`/`Switch` em `SystemSettingsScreen.kt`; `LocalDensity`/`LaunchedEffect`/`view` em `BoomListenerApp.kt`. Não verificado.

## C. Testes que faltam

Ver o item "Pendente para outro momento" em *Contexto atual* do CLAUDE.md (`HttpDeezerRepository`, `keepExternalCopy`, `WrappedNotification`). Também sem teste: `LocalizedDate`, `pointsToOwnPrivateData`, o prefixo do share, `dismissWrappedCard` e `insights` do `ProfileViewModel`.

## D. Já corrigido (não precisa provar de novo)

Foco de áudio e fone no Wrapped, `flush` do resumo diário, cancelamento nos 3 ViewModels (`runCatchingSuspend`), `WrappedShare` apagando o banner, datas do calendário, aviso no "Limpar histórico", `privacy.md` (Deezer no Perfil, limpeza de cópias, "Clear history"), toque fora da busca (raiz e folhas), remoção do registro de diagnóstico e do tester do Wrapped.
