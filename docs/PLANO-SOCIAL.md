# Plano do social: sem chat

Decisão do rilclesaraiva (20/10/2026). **O app deixa de ter chat.** A conversa
acontece onde as pessoas já estão: cada perfil pode ter botões para as redes
sociais da pessoa, e a aba Amigos vira **"O que seus amigos estão ouvindo"**.
Tudo continua **100% gratuito** (plano gratuito da Cloudflare, sem cartão, sem
domínio, sem R2).

## Princípios que não mudam

- Gratuito para sempre; doações ajudam, nada depende delas.
- O player não depende do servidor. Se o servidor cair, só o social pausa.
- Sem conta, e-mail ou telefone para entrar: id + segredo gerados no aparelho.
- O servidor guarda o mínimo. Toda mudança nele deve evitar **escrita** (o limite
  mais apertado do plano gratuito: 100 mil linhas escritas por dia).
- GPL v3, sem anúncios, sem rastreadores, nada do Google (sem Firebase).
- Nunca passa áudio nem arquivo de música: só informação (título, artista, capa, link).

## O que sai (0.30.0)

Conversas, grupos por código, mensagens, caixa postal, lembretes, push,
UnifiedPush/ntfy, notificação de mensagem e a permissão de notificações.
As conversas que estavam nos celulares são **apagadas na atualização**, sem
aviso (decisão do rilclesaraiva). No servidor, as tabelas de mensagens, grupos,
lembretes e anéis são apagadas uma vez, e os endereços de push são limpos.

## O que fica

Perfis (nome, @, foto, capa, cor, descrição), "ouvindo agora" e recentes,
estatísticas do perfil (calculadas no aparelho), bloqueio, e o "Compartilhar
com..." do menu de cada música, para quem quiser mandar por fora.

## Amizade

- **Pedido + aceite pelo @.** Abrir o perfil de alguém → "Adicionar amigo".
  Se a outra pessoa já tinha pedido, vira amizade na hora.
- Quem conversava antes vira amigo automaticamente (migração única no servidor).
- Desfazer amizade e bloquear continuam possíveis. Quem bloqueou não aparece
  na busca para o bloqueado e não recebe pedido dele (some em silêncio).
- Uma linha por amizade (`friends`, par ordenado) e uma por pedido pendente.
  Cota: 50 pedidos por dia.
- **Presencial (QR / Bluetooth): fica para depois.** Nesta fase, só pelo @.

## Redes no perfil

Tudo opcional, nada preenchido por padrão. Só aparecem os botões das redes
preenchidas. A pessoa digita **só o @ ou o número**; o app monta o link (nunca
aceita um link colado como está, para evitar golpe: se colar um endereço, o app
tira só o @ dele e monta de novo).

| Rede | Digita | Link montado | Quem vê |
|---|---|---|---|
| Instagram | @usuario | instagram.com/usuario | todos |
| X | @usuario | x.com/usuario | todos |
| TikTok | @usuario | tiktok.com/@usuario | todos |
| Threads | @usuario | threads.com/@usuario | todos |
| Bluesky | @usuario(.bsky.social) | bsky.app/profile/usuario.bsky.social | todos |
| YouTube | @canal | youtube.com/@canal | todos |
| Last.fm | usuario | last.fm/user/usuario | todos |
| SoundCloud | usuario | soundcloud.com/usuario | todos |
| Bandcamp | usuario | usuario.bandcamp.com | todos |
| Spotify | usuario | open.spotify.com/user/usuario | todos |
| Telegram (@) | @usuario | t.me/usuario | todos |
| WhatsApp | número | wa.me/5591999999999 | **só amigos** |
| Telegram (número) | número | t.me/+5591999999999 | **só amigos** |

- **Número:** o app tira espaços, traços e parênteses. Sem código do país, usa o
  **país do celular** (ex.: Brasil → 55), e a pessoa pode trocar o código ao lado
  do campo. `(91) 99999-9999` → `wa.me/5591999999999`.
- Ao preencher um número, aviso claro: **"Seu número fica visível para os seus
  amigos no app."**
- O número só é guardado no servidor se a pessoa ligar **"Mostrar meu número aos
  amigos"** (desligado por padrão). Desligar apaga do servidor. O Hub entrega o
  número **só a amigos confirmados**; nunca vai no perfil público. Motivo: ninguém
  coleta números em massa percorrendo perfis, e adolescentes ficam protegidos.
- **Ícones:** as logos são marcas das empresas. O app usa um ícone neutro com o
  nome da rede até o rilclesaraiva mandar os arquivos oficiais (kits de imprensa
  de cada rede); aí é só trocar os arquivos `ic_network_*.xml` em
  `feature/social/src/main/res/drawable/`.

## "O que seus amigos estão ouvindo" (dentro da aba Perfil)

Desde a 0.31.0 não existe aba Amigos: tudo isto fica na aba **Perfil**.


- Pedidos de amizade recebidos no topo (aceitar / recusar).
- Um cartão por amigo, numa fila que desliza para o lado: a capa e o que está
  ouvindo agora, ou o que ouviu por último e quando. Quem não compartilha
  aparece só com o nome. A capa é o endereço de uma imagem do Deezer que o
  celular de quem ouve acha e manda junto (só `*.dzcdn.net` é aceito).
- Tocar no cartão abre o perfil (estatísticas, redes, recentes).
- **Reações com emoji** ao que o amigo está ouvindo: 🔥 ❤️ 😂 😮 👏 🎧. Sem texto
  livre, nada para moderar. Vão **só ao vivo** (o amigo precisa estar com o app
  conectado); nada é gravado.
- **Recomendar com um toque** (menu de cada música → escolher amigos): o amigo
  recebe um cartão com capa, artista e link na lista "Recomendações". Guardadas
  num anel de 50 por pessoa, por 30 dias (1 escrita por recomendação). Cota: 100
  por dia. O celular guarda as recebidas e até onde já leu.

## Servidor (o que o Hub guarda)

Cadastros (com redes públicas e versão do perfil), imagens, amizades, pedidos,
bloqueios, números para amigos (só com a opção ligada) e o anel de
recomendações. "Ouvindo agora" e reações vivem na conexão (zero escritas);
recentes são gravados uma vez por sessão.

## Capacidade

O limite passa a ser praticamente só o das conexões do "ouvindo agora".
Estimativa: **cerca de 20 a 25 mil pessoas ativas por dia** no plano gratuito.

## Já feito antes (continua valendo)

- Fase 1 (0.28.0): WebSocket com hibernação, porteiro, cotas, "o social está
  descansando", reconexão com espera crescente.
- Fase 2 gratuita (0.29.0): WebP 512/1080, GIF até 3 MB e 15 s, versão da imagem
  pelo hash, perfil com número de versão e cache no aparelho.

## Depois

- Adicionar amigo pessoalmente por QR (câmera só ao ler) e, talvez, Bluetooth;
  o cartão presencial levaria o número direto, sem passar pelo servidor.
- Linha discreta de doação na tela Sobre.
