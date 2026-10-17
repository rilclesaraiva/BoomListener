# Proxy do Last.fm (Cloudflare Worker)

Guarda a chave da API do Last.fm fora do APK. O app chama
`https://boomlistener-lastfm.<sua-conta>.workers.dev/lastfm?method=artist.getinfo&artist=…`
e o Worker repassa ao Last.fm com a chave.

## Publicar (pelo painel, uma vez)

1. Cloudflare → **Workers & Pages** → **Create** → **Create Worker**.
2. Nome: **`boomlistener-lastfm`** → **Deploy**.
3. **Edit code** → apague o conteúdo e cole o `worker.js` deste diretório → **Deploy**.
4. **Settings → Variables and Secrets → Add**, tipo **Secret**:
   - `LASTFM_API_KEY` = a "API key" do Last.fm
   - `LASTFM_SHARED_SECRET` = o "Shared secret" (para o scrobble no futuro)
5. Teste abrindo `https://boomlistener-lastfm.<sua-conta>.workers.dev/health` → deve mostrar `{"ok":true}`.

O endereço final vai em `data/lastfm` (constante `PROXY_URL`).
