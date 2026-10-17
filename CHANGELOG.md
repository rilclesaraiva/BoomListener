# Changelog

Todas as mudanças do **Boom, listener!** ficam registradas aqui, por versão, tanto para quem acompanha o projeto quanto para o Claude continuar de onde parou em sessões futuras.

Formato inspirado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/). Versionamento [SemVer](https://semver.org/lang/pt-BR/): `MAJOR.MINOR.PATCH`.

## [0.4.0] — 2026-10-17

### ✨ Novidades
- **O app toca música!** Toque numa música da Biblioteca e ela começa a tocar, com a lista da tela virando a fila (se você estiver buscando, a fila é só o resultado da busca).
- **Mini player** acima da barra de navegação, com capa, música, artista, anterior, tocar/pausar e próxima. Tocar nele abre a tela cheia.
- **Tela "Tocando agora"**: capa grande, barra de progresso ondulada (fica reta quando pausa) que dá para arrastar, tempo decorrido e total, aleatório, anterior, tocar/pausar, próxima e repetir (tudo, uma, desligado).
- **Continua tocando com o app fechado**, com controles na notificação, na tela de bloqueio e nos botões do fone. Pausa sozinho quando o fone é desconectado ou quando chega uma ligação.
- **Ajustes reorganizados** em seções (Geral e Sobre), com ícones coloridos e título grande.
- **Aparência**: escolha entre seguir o sistema, claro, escuro ou AMOLED (preto puro), e ligue ou desligue as cores do papel de parede.
- **Sobre, Privacidade e Licenças** nos Ajustes.

### 🐛 Correções
- **O APK de teste agora atualiza por cima**, sem precisar desinstalar a cada versão.

## [0.3.0] — 2026-10-17

### ✨ Novidades
- **Lista de músicas**: a Biblioteca agora mostra todas as músicas do aparelho, em ordem alfabética, com capa, artista e álbum. Toques de celular, alarmes e gravações ficam de fora.
- **Busca**: a lupa no topo da Biblioteca procura por título, artista ou álbum, sem se importar com acentos ou maiúsculas ("joao" encontra "João").
- **Menu de cada música** (os três pontinhos): ir para o artista, editar tags e ver a duração.
- **Bem-estar mudou de lugar**: saiu da barra de baixo e agora fica em **Ajustes → Bem-estar**.

### 🎨 Ajustes visuais
- Todas as telas ganharam barra no topo com título, e as telas internas (artista, editor de tags, tocando agora, bem-estar) ganharam a seta de voltar.

## [0.2.0] — 2026-10-17

### ✨ Novidades
- **Acesso às suas músicas**: a aba Biblioteca agora explica por que o app precisa de acesso a música e áudio e pede a permissão. Se ela for negada duas vezes, aparece um botão que leva direto às configurações do Android.

## [0.1.0] — 2026-10-17

Primeira versão: o esqueleto do app, ainda sem tocar música.

### ✨ Novidades
- Estrutura do app com as abas **Biblioteca**, **Lançamentos**, **Bem-estar** e **Ajustes**.
- Telas provisórias de **Artista**, **Editor de tags** e **Tocando agora**, abertas a partir da Biblioteca.
- Visual Material 3 Expressive, com as cores do seu papel de parede no Android 12 ou mais novo.
