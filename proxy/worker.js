// Boom, listener! — Proxy do Last.fm (Cloudflare Worker)
// Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE
//
// O que isto faz: o app pede informações públicas de artistas aqui, e o Worker
// repassa ao Last.fm acrescentando a chave da API. Assim a chave NUNCA fica
// dentro do APK (quem abrisse o APK poderia copiá-la).
//
// A chave e o segredo NÃO estão neste arquivo. Eles vêm de variáveis secretas
// cadastradas no painel da Cloudflare (Workers → este Worker → Settings →
// Variables and Secrets):
//   LASTFM_API_KEY        → "API key" do Last.fm
//   LASTFM_SHARED_SECRET  → "Shared secret" (só será usado no scrobble, no futuro)
//
// Uso pelo app:  GET /lastfm?method=artist.getinfo&artist=Djavan&lang=pt
// Só métodos de leitura pública da lista abaixo são aceitos.

const ALLOWED_METHODS = new Set([
  "artist.getinfo",
  "artist.getsimilar",
  "artist.gettoptags",
  "album.getinfo",
  "track.getinfo",
]);

// Parâmetros que o app pode repassar (o resto é ignorado).
const ALLOWED_PARAMS = ["artist", "album", "track", "lang", "limit", "autocorrect", "mbid"];

// Respostas ficam no cache da Cloudflare por um dia: bio de artista muda pouco
// e isso poupa o limite de consultas do Last.fm.
const CACHE_SECONDS = 24 * 60 * 60;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method !== "GET") return json({ error: "method not allowed" }, 405);
    if (url.pathname === "/" || url.pathname === "/health") return json({ ok: true });
    if (url.pathname !== "/lastfm") return json({ error: "not found" }, 404);
    if (!env.LASTFM_API_KEY) return json({ error: "proxy not configured" }, 500);

    const method = (url.searchParams.get("method") || "").toLowerCase();
    if (!ALLOWED_METHODS.has(method)) return json({ error: "method not allowed" }, 400);

    const upstream = new URL("https://ws.audioscrobbler.com/2.0/");
    upstream.searchParams.set("method", method);
    for (const name of ALLOWED_PARAMS) {
      const value = url.searchParams.get(name);
      if (value) upstream.searchParams.set(name, value.slice(0, 300));
    }
    upstream.searchParams.set("format", "json");

    // A chave do cache é a URL SEM a chave da API (nunca vaza no cache).
    const cacheKey = new Request(upstream.toString(), { method: "GET" });
    const cache = caches.default;
    const cached = await cache.match(cacheKey);
    if (cached) return cached;

    upstream.searchParams.set("api_key", env.LASTFM_API_KEY);
    const response = await fetch(upstream.toString(), {
      headers: { "User-Agent": "BoomListener (https://github.com/rilclesaraiva/BoomListener)" },
    });
    const body = await response.text();
    const result = new Response(body, {
      status: response.status,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": `public, max-age=${CACHE_SECONDS}`,
      },
    });
    if (response.ok) ctx.waitUntil(cache.put(cacheKey, result.clone()));
    return result;
  },
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}
