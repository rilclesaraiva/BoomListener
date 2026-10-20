# Servidor social (Cloudflare Worker)

Guarda os perfis (@nome, foto, redes públicas), as amizades (pedido + aceite)
e o "ouvindo agora" de quem escolheu compartilhar com os amigos. Entrega
recomendações de música e reações com emoji. Não existe chat (a conversa
acontece nas redes de cada perfil) e nunca passa áudio.

Com o app aberto, o celular fica ligado por WebSocket com hibernação
(`/v1/ws`): o Hub dorme entre as mensagens e não gasta duração. Números de
telefone só ficam aqui se a pessoa ligar "Mostrar meu número aos amigos", e só
vão para amigos confirmados. O plano está em `docs/PLANO-SOCIAL.md`.

## Alerta de uso (recomendado)

Cloudflare → **Notifications → Add → Workers / Durable Objects usage**: avise
quando passar de 70% de qualquer limite diário.

## Publicar (uma vez)

Igual ao Worker do Last.fm, importando do GitHub:

1. Cloudflare → **Workers & Pages → Create → Import a repository** → `rilclesaraiva/BoomListener`.
2. **Nome do projeto:** `boomlistener-social` (igual ao `wrangler.toml`).
3. **Comando de implantação:** `npx wrangler deploy`. Builds de preview: desligado.
4. **Configurações avançadas → Caminho:** `/social`.
5. **Implantar.** Não precisa de nenhuma chave.
6. Teste: `https://boomlistener-social.<sua-conta>.workers.dev/health` → `{"ok":true}`.

Depois disso, toda mudança em `social/` é publicada sozinha.

## Testar sem o app

```
node --test social/worker.test.mjs
```
