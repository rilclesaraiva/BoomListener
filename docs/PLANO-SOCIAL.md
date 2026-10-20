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

### Fase 2: imagens e perfis no R2 (precisa do rilclesaraiva)
- Criar um bucket R2 e ligar ao Worker; domínio próprio (~US$ 10/ano, primeira meta de doação), porque o `r2.dev` tem limite de velocidade.
- Upload direto com endereço temporário, WebP, nome = hash, cache de 1 ano.
- Perfil público como arquivo no R2; perfis com número de versão.
- Migrar as imagens que hoje estão no SQLite (`image_parts`).

### Fase 3: mensagens presas no celular de quem enviou
- Fila de saída local com status (aguardando, entregue, na caixa postal) e ids únicos (os ids gerados no celular já existem desde a 0.28.0).
- Lembretes no Hub ("tenho mensagens para @fulano"), UnifiedPush (convite para instalar o ntfy; sem Firebase), recibos de entrega.
- Anel de mensagens: 100 por pessoa, 200 por grupo, 7 dias.
- Push só "acorde", no máximo 1 a cada 2 min por celular.

### Fase 4: extras
- QR code e Bluetooth para adicionar amigos e recomendar músicas (nunca o arquivo).
- Exportar e importar conversas (opcionalmente com senha).
- Chats fora do backup automático do Android.

## Doações
Linha discreta na tela Sobre (Pix, GitHub Sponsors, Ko-fi), sem pop-up; nenhum recurso exclusivo de quem doa.
