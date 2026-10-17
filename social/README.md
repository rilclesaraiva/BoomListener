# Servidor social (Cloudflare Worker)

Guarda os perfis (@nome), entrega as mensagens das conversas e grupos e o
"ouvindo agora" de quem escolheu compartilhar. Nunca passa áudio. As mensagens
ficam aqui só até o app de quem recebe confirmar; o histórico mora no celular.

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
