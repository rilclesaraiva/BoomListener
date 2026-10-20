# Plano do social gratuito

Resumo do plano do rilclesaraiva (20/10/2026) para o social do Boom, listener!
caber no plano gratuito da Cloudflare com ~8 a 10 mil pessoas ativas por dia
(antes: ~500). Princípio: **o servidor é um carteiro, o celular é a casa.**

## Princípios que não mudam

- Gratuito para sempre; doações ajudam, nada depende delas.
- O player não depende do servidor. Se o servidor cair, só o social pausa.
- Sem conta, e-mail ou telefone: id + token gerados no aparelho.
- Chats moram no celular (desinstalar apaga; backup só exportando).
- O servidor guarda o mínimo.
- GPL v3, sem anúncios e sem rastreadores; nenhum SDK de terceiros obrigatório.

## Limites que importam (plano gratuito)

- 100 mil requisições de Worker por dia **por conta** (o proxy do Last.fm divide a mesma cota).
- 100 mil requisições de Durable Object por dia (mensagem recebida por WebSocket conta 20 para 1).
- **100 mil linhas escritas por dia** no SQLite: o limite mais apertado.
- 13 mil GB-s de duração por dia (um Hub acordado o dia todo gasta ~10.800).
- Contadores zeram à meia-noite UTC (21h em Belém).

## Fases

### Fase 1: menos requisições ✅ na 0.28.0
- [x] Polling trocado por WebSocket com hibernação (`/v1/ws`, `SocialLink` no app).
- [x] "Ouvindo agora" na conexão (`serializeAttachment`), zero escritas.
- [x] Recentes gravados uma vez por sessão (`recent_log`, ao fechar a conexão).
- [x] Worker como porteiro (`gate`): rota, método, segredo e tamanho, sem acordar o Hub.
- [x] Cotas por pessoa: 300 mensagens/dia, 5 trocas de imagem/dia, grupos de 30, 20 grupos por pessoa, 5 cadastros/hora por IP (na memória do Hub).
- [x] "O social está descansando, volta às 21h" e reconexão com espera crescente (3 s, depois 1, 2, 4, 8 até 15 min, com sorteio).
- [ ] Alerta de uso a 70% no painel da Cloudflare (o rilclesaraiva configura no painel).

### Fase 2, versão gratuita ✅ na 0.29.0
Decisão do rilclesaraiva: **tudo gratuito**. O R2 exige cartão cadastrado para ativar e o domínio custa ~US$ 10/ano, então ficaram de fora.
- [x] Imagens em WebP: foto 512 px, capa 1080×405 (bem menores que o JPEG; cabem muito mais perfis nos 5 GB).
- [x] GIF limitado por tamanho (3 MB), resolução e duração (15 s); fora disso o app refaz menor.
- [x] Versão da imagem = começo do hash dela: a mesma foto tem sempre o mesmo endereço no cache do app.
- [x] Perfil com número de versão (`profile_v`): ao conectar, o app recebe as versões de quem conhece e só baixa o perfil que mudou (`profileCard`, guardado em `filesDir/social/profiles`).
- [ ] R2 e domínio próprio: só se um dia houver cartão cadastrado (continua grátis) e doação para o domínio.

### Fase 3: mensagens presas no celular de quem enviou ✅ na 0.29.0
- [x] Mensagem nasce "aguardando" no celular (`Delivery.Waiting`); destinatário online → vai ao vivo, sem gravar nada, e volta o recibo "entregue".
- [x] Destinatário offline → só um lembrete no Hub (uma linha por destinatário) + push "acorde" pelo UnifiedPush.
- [x] Destinatário aparece → o Hub apaga o lembrete e cutuca quem enviou (pela conexão ou por push); o celular manda a fila.
- [x] Rede de segurança (anel, 100 por pessoa, 7 dias, sem exclusão): quem enviou não tem push, 24 h sem entregar, ou o destinatário sumiu de novo depois do cutucão.
- [x] Grupos: uma escrita por mensagem no anel do grupo (200), cursor por grupo no celular, ao vivo para quem está online, push para quem não está.
- [x] UnifiedPush (connector 3.0.10, sem Firebase); convite para instalar o ntfy em Amigos; push no máximo 1 a cada 2 min por celular; o push não leva conteúdo.
- [x] Notificação de mensagem nova com o app fora da tela; chats fora do backup do Android.
- [x] Apps antigos (até a 0.28) continuam recebendo pela caixa postal.

### Fase 4: extras
- QR code e Bluetooth para adicionar amigos e recomendar músicas (nunca o arquivo).
- Exportar e importar conversas (opcionalmente com senha).
- ~~Chats fora do backup automático do Android~~ (feito na 0.29.0).

## Doações
Linha discreta na tela Sobre (Pix, GitHub Sponsors, Ko-fi), sem pop-up; nenhum recurso exclusivo de quem doa.
