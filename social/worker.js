// Boom, listener! — Servidor social (Cloudflare Worker + Durable Object)
// Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE
//
// O que isto faz: guarda perfis (@nome, foto, redes públicas), amizades
// (pedido + aceite) e o "ouvindo agora" de quem escolheu compartilhar com os
// amigos. Também entrega recomendações de música (cartões com capa, artista e
// link) e reações com emoji. NUNCA passa áudio, e não existe chat: a conversa
// acontece nas redes que cada um põe no perfil (ver docs/PLANO-SOCIAL.md).
//
// Com o app aberto (ou tocando música, para o "ouvindo agora"), o celular
// fica ligado por um WebSocket com hibernação (/v1/ws): o Hub dorme entre uma
// mensagem e outra. O "ouvindo agora" e as reações moram na conexão (zero
// escritas no banco); as músicas recentes são gravadas uma vez, quando a
// conexão fecha. O limite mais apertado do plano gratuito é o de linhas
// escritas, então quase nada aqui grava.
//
// Números de telefone (WhatsApp, Telegram) só ficam aqui se a pessoa ligar
// "Mostrar meu número aos amigos", e só são entregues a amigos confirmados.
//
// Sem login do Google: ao criar o perfil, o app recebe um id e um segredo
// (token) que guarda no aparelho. O servidor guarda só o hash do segredo.

import { DurableObject } from "cloudflare:workers";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/health") return json({ ok: true });
    // Porteiro: pedido inválido é recusado aqui, sem acordar o Hub.
    const refused = gate(request, url);
    if (refused) return refused;
    try {
      const hub = env.HUB.get(env.HUB.idFromName("hub"));
      return await hub.fetch(request);
    } catch (error) {
      // Cota diária do plano gratuito esgotada: o social descansa até a meia-noite UTC.
      const resting = /exceed|limit|quota/i.test(String(error?.message ?? error));
      return json({ error: resting ? "resting" : "unavailable", until: nextUtcMidnight() }, 503);
    }
  },
};

const ID = "[0-9a-f]{32}";
// [métodos, caminho, precisa do segredo]
/** Meia-noite UTC seguinte (quando os contadores do plano gratuito zeram). */
export function nextUtcMidnight(now = Date.now()) {
  const day = 86_400_000;
  return Math.floor(now / day) * day + day;
}

const ROUTES = [
  ["GET", new RegExp("^/v1/handles/[^/]{1,60}$"), false],
  ["POST", /^\/v1\/register$/, false],
  ["GET", /^\/v1\/ws$/, true],
  ["GET PATCH DELETE", /^\/v1\/me$/, true],
  ["PUT DELETE", /^\/v1\/me\/image\/(avatar|banner)$/, true],
  ["GET", new RegExp(`^/v1/users/${ID}(/image/(avatar|banner))?$`), true],
  ["GET", new RegExp("^/v1/users/by-handle/[^/]{1,60}$"), true],
  ["GET", /^\/v1\/activity$/, true],
  ["GET", /^\/v1\/friends$/, true],
  ["POST DELETE", new RegExp(`^/v1/friends/${ID}$`), true],
  ["GET POST", /^\/v1\/recommendations$/, true],
  ["POST", /^\/v1\/blocks$/, true],
  ["DELETE", new RegExp(`^/v1/blocks/${ID}$`), true],
];
const AUTH = /^Bearer [0-9a-f]{32}\.[0-9a-f]{64}$/;
const MAX_JSON_REQUEST = 16_384;

/** Recusa (com a resposta pronta) o que nem precisa chegar ao Hub; null = pode passar. */
export function gate(request, url) {
  const route = ROUTES.find(([methods, pattern]) => pattern.test(url.pathname) && methods.split(" ").includes(request.method));
  if (!route) return json({ error: "not_found" }, 404);
  if (route[2] && !AUTH.test(request.headers.get("authorization") ?? "")) return json({ error: "unauthorized" }, 401);
  if (url.pathname === "/v1/ws" && (request.headers.get("upgrade") ?? "").toLowerCase() !== "websocket") {
    return json({ error: "expected_websocket" }, 426);
  }
  const length = Number(request.headers.get("content-length") ?? 0);
  const limit = /\/me\/image\//.test(url.pathname) ? MAX_GIF_BYTES + 1024 : MAX_JSON_REQUEST;
  if (length > limit) return json({ error: "too_large" }, 413);
  return null;
}

const HANDLE = /^[a-z0-9_.]{3,20}$/;
const USER_ID = /^[0-9a-f]{32}$/;
const RECENT_KEEP = 20;
const MAX_BIO = 160;
const MAX_IMAGE_BYTES = 400_000;
// GIF: o app limita por tamanho, resolução e duração antes de enviar.
const MAX_GIF_BYTES = 3_000_000;
// O SQLite do Durable Object guarda até 2 MB por valor: imagens maiores vão em pedaços.
const IMAGE_PART_BYTES = 1_000_000;
const MAX_SOCKET_MESSAGE = 16_384;
// Cotas por pessoa: um abusador não pode gastar o teto diário de todo mundo.
const IMAGES_PER_DAY = 5;
const FRIEND_REQUESTS_PER_DAY = 50;
const RECOMMENDATIONS_PER_DAY = 100;
const REACTIONS_PER_MINUTE = 30;
const REGISTRATIONS_PER_IP_HOUR = 5;
// Recomendações: anel de 50 por pessoa, guardadas por 30 dias (uma escrita cada, nenhuma exclusão).
const RECS_SIZE = 50;
const RECS_MS = 30 * 86_400_000;
const MAX_RECIPIENTS = 20;
// Reações: só estes emojis (sem texto livre, nada para moderar).
const REACTIONS = ["🔥", "❤️", "😂", "😮", "👏", "🎧"];
// Redes públicas no perfil: o app manda só o @; quem monta o link é o app de quem vê.
export const LINK_NETWORKS = [
  "instagram", "x", "tiktok", "threads", "bluesky", "youtube",
  "lastfm", "soundcloud", "bandcamp", "spotify", "telegram",
];
// Redes que expõem o número: só para amigos confirmados.
export const NUMBER_NETWORKS = ["whatsapp", "telegram"];
function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

function randomHex(bytes) {
  const array = new Uint8Array(bytes);
  crypto.getRandomValues(array);
  return [...array].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Versão de uma imagem: os primeiros 48 bits do SHA-256 (número seguro em JavaScript e no Kotlin). */
async function imageVersion(bytes) {
  const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", bytes));
  let value = 0;
  for (let i = 0; i < 6; i++) value = value * 256 + digest[i];
  return value || 1;
}

async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function cleanName(value) {
  if (typeof value !== "string") return null;
  const name = value.trim().replace(/\s+/g, " ");
  return name.length >= 1 && name.length <= 40 ? name : null;
}

/** Só informações de música, com tamanho limitado. */
/** Descrição do perfil: texto curto, sem quebras de linha sobrando. */
function cleanBio(value) {
  if (typeof value !== "string") return null;
  return value.replace(/\s*\n\s*\n+/g, "\n").trim().slice(0, MAX_BIO);
}

/** Foto ou capa: WebP ou JPEG até 400 KB, ou GIF (animado) até 3 MB. Devolve o tipo, ou null. */
function imageType(bytes) {
  if (bytes.length >= 3 && bytes.length <= MAX_IMAGE_BYTES && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return "image/jpeg";
  }
  if (
    bytes.length >= 12 &&
    bytes.length <= MAX_IMAGE_BYTES &&
    String.fromCharCode(...bytes.slice(0, 4)) === "RIFF" &&
    String.fromCharCode(...bytes.slice(8, 12)) === "WEBP"
  ) {
    return "image/webp";
  }
  const gif = bytes.length >= 6 && String.fromCharCode(...bytes.slice(0, 6));
  if ((gif === "GIF87a" || gif === "GIF89a") && bytes.length <= MAX_GIF_BYTES) return "image/gif";
  return null;
}

function cleanTrack(value) {
  if (!value || typeof value !== "object") return null;
  const str = (v, max) => (typeof v === "string" && v.trim() ? v.trim().slice(0, max) : null);
  const title = str(value.title, 200);
  if (!title) return null;
  return { title, artist: str(value.artist, 200), album: str(value.album, 200) };
}


/** Só informações de música (cartão de recomendação), com tamanho limitado. */
function cleanCard(value) {
  if (!value || typeof value !== "object") return null;
  const str = (v, max) => (typeof v === "string" && v.trim() ? v.trim().slice(0, max) : null);
  const title = str(value.title, 200);
  if (!title) return null;
  const kind = ["track", "album", "artist"].includes(value.kind) ? value.kind : "track";
  const url = (v) => (typeof v === "string" && /^https:\/\//.test(v) ? v.slice(0, 500) : null);
  return {
    kind,
    title,
    artist: str(value.artist, 200),
    album: str(value.album, 200),
    coverUrl: url(value.coverUrl),
    link: url(value.link),
  };
}

/**
 * Redes públicas: { rede: "usuario" }, só as conhecidas e só o @ (letras,
 * números, ponto, traço e sublinhado). null = inválido. Vazio some.
 */
export function cleanLinks(value) {
  if (value === null || value === undefined) return {};
  if (typeof value !== "object" || Array.isArray(value)) return null;
  const links = {};
  for (const [network, raw] of Object.entries(value)) {
    if (!LINK_NETWORKS.includes(network)) return null;
    if (raw === null || raw === "") continue;
    if (typeof raw !== "string") return null;
    const handle = raw.trim().replace(/^@/, "");
    if (!/^[A-Za-z0-9._-]{1,60}$/.test(handle)) return null;
    links[network] = handle;
  }
  return links;
}

/** Números: { whatsapp: "5591999999999" }, só dígitos com código do país (8 a 15). null = inválido. */
export function cleanNumbers(value) {
  if (value === null || value === undefined) return {};
  if (typeof value !== "object" || Array.isArray(value)) return null;
  const numbers = {};
  for (const [network, raw] of Object.entries(value)) {
    if (!NUMBER_NETWORKS.includes(network)) return null;
    if (raw === null || raw === "") continue;
    if (typeof raw !== "string" || !/^[1-9][0-9]{7,14}$/.test(raw)) return null;
    numbers[network] = raw;
  }
  return numbers;
}

function parseJson(text, fallback) {
  try {
    return text ? JSON.parse(text) : fallback;
  } catch {
    return fallback;
  }
}
/** Manda um evento pela conexão; se ela já fechou, deixa para lá. */
function send(ws, event) {
  try {
    ws.send(typeof event === "string" ? event : JSON.stringify(event));
  } catch {
    // Conexão fechada.
  }
}

function safeAttachment(ws) {
  try {
    return ws.deserializeAttachment();
  } catch {
    return null;
  }
}

/** Linha do anel → recomendação no formato que o app entende. */
function recOf(row) {
  return {
    id: row.id,
    at: row.at,
    seq: row.seq,
    card: JSON.parse(row.body),
    sender: row.sid ? { id: row.sid, handle: row.handle, name: row.name } : null,
  };
}

/** Recentes da conexão + os guardados, do mais novo para o mais antigo, sem repetir. */
export function mergeRecent(live, stored) {
  const seen = new Set();
  return [...live, ...stored]
    .sort((a, b) => b.at - a.at)
    .filter((item) => {
      const key = `${item.at}|${item.title}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .slice(0, RECENT_KEEP);
}

export class Hub extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.sql = ctx.storage.sql;
    // Contadores das cotas: só na memória (zeram se o Hub reiniciar; o teto real é o da Cloudflare).
    this.counters = new Map();
    // "ping" do app responde sozinho, sem acordar o Hub.
    if (typeof WebSocketRequestResponsePair === "function") {
      ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair("ping", "pong"));
    }
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY, handle TEXT UNIQUE NOT NULL, name TEXT NOT NULL,
        token_hash TEXT NOT NULL, created INTEGER NOT NULL, sharing INTEGER NOT NULL DEFAULT 0
      );
      CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);
      CREATE TABLE IF NOT EXISTS friends (a TEXT NOT NULL, b TEXT NOT NULL, PRIMARY KEY (a, b));
      CREATE INDEX IF NOT EXISTS friends_b ON friends(b);
      CREATE TABLE IF NOT EXISTS requests (from_user TEXT NOT NULL, to_user TEXT NOT NULL, at INTEGER NOT NULL, PRIMARY KEY (from_user, to_user));
      CREATE INDEX IF NOT EXISTS requests_to ON requests(to_user);
      CREATE TABLE IF NOT EXISTS blocks (user_id TEXT NOT NULL, blocked TEXT NOT NULL, PRIMARY KEY (user_id, blocked));
      CREATE TABLE IF NOT EXISTS recs (
        user_id TEXT NOT NULL, slot INTEGER NOT NULL, id TEXT NOT NULL, sender TEXT NOT NULL,
        body TEXT NOT NULL, at INTEGER NOT NULL, seq INTEGER NOT NULL, PRIMARY KEY (user_id, slot)
      );
      CREATE TABLE IF NOT EXISTS image_parts (
        user_id TEXT NOT NULL, kind TEXT NOT NULL, part INTEGER NOT NULL, data BLOB NOT NULL,
        PRIMARY KEY (user_id, kind, part)
      );
      CREATE TABLE IF NOT EXISTS recent_log (user_id TEXT PRIMARY KEY, items TEXT NOT NULL, updated INTEGER NOT NULL);
      CREATE TABLE IF NOT EXISTS images (
        user_id TEXT NOT NULL, kind TEXT NOT NULL, data BLOB NOT NULL, PRIMARY KEY (user_id, kind)
      );
    `);
    // Colunas que chegaram depois.
    for (const column of [
      "bio TEXT NOT NULL DEFAULT ''",
      "avatar_v INTEGER NOT NULL DEFAULT 0",
      "banner_v INTEGER NOT NULL DEFAULT 0",
      "color TEXT NOT NULL DEFAULT ''",
      "push_url TEXT NOT NULL DEFAULT ''",
      "profile_v INTEGER NOT NULL DEFAULT 0",
      "links TEXT NOT NULL DEFAULT '{}'",
      "numbers TEXT NOT NULL DEFAULT ''",
    ]) {
      try {
        this.sql.exec(`ALTER TABLE users ADD COLUMN ${column}`);
      } catch {
        // Já existe.
      }
    }
    this.migrateToFriends();
  }

  /**
   * Uma vez só: o chat saiu (0.30). Quem conversava vira amigo; mensagens,
   * grupos, lembretes, caixas postais e endereços de push são apagados.
   */
  migrateToFriends() {
    if (this.first("SELECT value FROM meta WHERE key = 'no_chat'")) return;
    try {
      this.sql.exec("INSERT OR IGNORE INTO friends (a, b) SELECT a, b FROM contacts WHERE a < b");
    } catch {
      // Banco novo: nunca teve conversas.
    }
    for (const table of ["inbox", "ring", "group_ring", "reminders", "groups", "members", "contacts", "status", "recent"]) {
      this.sql.exec(`DROP TABLE IF EXISTS ${table}`);
    }
    this.sql.exec("UPDATE users SET push_url = '' WHERE push_url != ''");
    this.sql.exec("INSERT INTO meta (key, value) VALUES ('no_chat', '1')");
  }

  all(query, ...args) {
    return this.sql.exec(query, ...args).toArray();
  }

  first(query, ...args) {
    return this.all(query, ...args)[0] ?? null;
  }

  async authenticate(request) {
    const header = request.headers.get("authorization") ?? "";
    const match = header.match(/^Bearer ([0-9a-f]{32})\.([0-9a-f]{64})$/);
    if (!match) return null;
    const user = this.first("SELECT * FROM users WHERE id = ?", match[1]);
    if (!user) return null;
    return user.token_hash === (await sha256(match[2])) ? user : null;
  }

  /** O que qualquer pessoa vê de um perfil (sem número, sem "ouvindo"). */
  publicUser(user) {
    return {
      id: user.id,
      handle: user.handle,
      name: user.name,
      bio: user.bio ?? "",
      avatar: user.avatar_v ?? 0,
      banner: user.banner_v ?? 0,
      color: user.color ?? "",
      links: parseJson(user.links, {}),
      // Versão do perfil: muda a cada edição; o app só baixa de novo quando muda.
      v: user.profile_v ?? 0,
    };
  }

  /** O seu próprio perfil, com o que só você vê (números guardados e compartilhamento). */
  ownProfile(me) {
    const numbers = parseJson(me.numbers, null);
    return { ...this.publicUser(me), sharing: me.sharing === 1, numbers: numbers ?? {}, shareNumbers: numbers !== null };
  }

  // ---- Amizades ----

  isFriend(x, y) {
    const [a, b] = x < y ? [x, y] : [y, x];
    return !!this.first("SELECT 1 AS x FROM friends WHERE a = ? AND b = ?", a, b);
  }

  friendIds(userId) {
    return this.all(
      "SELECT b AS id FROM friends WHERE a = ? UNION SELECT a AS id FROM friends WHERE b = ?",
      userId,
      userId,
    ).map((row) => row.id);
  }

  /** Relação de [viewer] com [user]: "self", "friend", "sent" (pedido seu), "received" (pedido dele) ou "none". */
  relation(viewerId, userId) {
    if (viewerId === userId) return "self";
    if (this.isFriend(viewerId, userId)) return "friend";
    if (this.first("SELECT 1 AS x FROM requests WHERE from_user = ? AND to_user = ?", viewerId, userId)) return "sent";
    if (this.first("SELECT 1 AS x FROM requests WHERE from_user = ? AND to_user = ?", userId, viewerId)) return "received";
    return "none";
  }

  /** Avisa os aparelhos conectados de [userIds] que a lista de amigos mudou (nada é gravado). */
  friendsChanged(...userIds) {
    for (const id of userIds) for (const ws of this.sockets(id)) send(ws, { t: "friends" });
  }

  blocked(owner, other) {
    return !!this.first("SELECT 1 AS x FROM blocks WHERE user_id = ? AND blocked = ?", owner, other);
  }

  /**
   * O que [viewer] pode ver de [user]: redes públicas para todos; número e
   * "ouvindo agora" só para amigos (e o "ouvindo" só com o compartilhamento ligado).
   */
  profileFor(viewer, user) {
    const blockedMe = this.blocked(user.id, viewer.id);
    const profile = this.publicUser(user);
    if (blockedMe) profile.links = {};
    profile.relation = blockedMe ? "none" : this.relation(viewer.id, user.id);
    const friend = profile.relation === "friend";
    profile.numbers = friend ? parseJson(user.numbers, null) ?? {} : {};
    const canSee = friend && user.sharing === 1;
    if (canSee) {
      // Conectado: o "ouvindo agora" vem da conexão.
      const live = this.liveOf(user.id);
      profile.now = live?.now ?? null;
      profile.updated = live?.at || null;
      profile.recent = mergeRecent(live?.recent ?? [], this.storedRecent(user.id));
    }
    profile.sharing = canSee;
    profile.blockedByMe = this.blocked(viewer.id, user.id);
    return profile;
  }

  /** Cota diária (ou por hora, com [windowMs]) de [kind] para [key]. */
  allow(kind, key, limit, windowMs = 86_400_000) {
    const window = Math.floor(Date.now() / windowMs);
    const id = `${kind}:${key}`;
    const entry = this.counters.get(id);
    if (!entry || entry.window !== window) {
      this.counters.set(id, { window, n: 1 });
      return true;
    }
    if (entry.n >= limit) return false;
    entry.n++;
    return true;
  }

  // ---- Conexões (WebSocket com hibernação) ----

  sockets(userId) {
    return this.ctx.getWebSockets?.(userId) ?? [];
  }

  /** O estado ao vivo mais recente da pessoa (de qualquer aparelho conectado); null se offline. */
  liveOf(userId) {
    let best = null;
    for (const ws of this.sockets(userId)) {
      const state = safeAttachment(ws);
      if (state && !state.closed && (!best || (state.at ?? 0) >= (best.at ?? 0))) best = state;
    }
    return best;
  }

  storedRecent(userId) {
    const row = this.first("SELECT items FROM recent_log WHERE user_id = ?", userId);
    return row ? JSON.parse(row.items) : [];
  }

  /** Grava de uma vez as músicas ouvidas na conexão (uma escrita por sessão). */
  flushRecent(ws) {
    const state = safeAttachment(ws);
    if (!state?.id || !state.recent?.length || state.flushed) return;
    const user = this.first("SELECT sharing FROM users WHERE id = ?", state.id);
    if (user?.sharing === 1) {
      const items = mergeRecent(state.recent, this.storedRecent(state.id));
      this.sql.exec(
        "INSERT INTO recent_log (user_id, items, updated) VALUES (?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET items = excluded.items, updated = excluded.updated",
        state.id,
        JSON.stringify(items),
        Date.now(),
      );
    }
    try {
      ws.serializeAttachment({ ...state, recent: [], flushed: true });
    } catch {
      // Conexão já fechada.
    }
  }

  /** Os seus amigos, com o que cada um está ouvindo (quem não compartilha vem só com o nome). */
  activityFor(me) {
    return this.friendIds(me.id)
      .map((id) => this.first("SELECT * FROM users WHERE id = ?", id))
      .filter(Boolean)
      .map((user) => this.profileFor(me, user));
  }

  /** Avisa os amigos conectados que o "ouvindo agora" de [user] mudou. */
  announce(user) {
    for (const viewerId of this.friendIds(user.id)) {
      const sockets = this.sockets(viewerId);
      if (sockets.length === 0) continue;
      const viewer = this.first("SELECT * FROM users WHERE id = ?", viewerId);
      if (!viewer) continue;
      const profile = this.profileFor(viewer, user);
      for (const ws of sockets) send(ws, { t: "person", profile });
    }
  }

  // ---- Recomendações (anel de 50 por pessoa, 30 dias) ----

  /**
   * Guarda no anel: as primeiras ocupam posições novas; depois, a nova
   * sobrescreve a mais antiga. Uma escrita por recomendação, nenhuma exclusão.
   */
  ringPut(table, keyColumn, key, size, message) {
    if (this.first(`SELECT 1 AS x FROM ${table} WHERE ${keyColumn} = ? AND id = ?`, key, message.id)) return;
    const stats = this.first(`SELECT COUNT(*) AS n, MAX(seq) AS last FROM ${table} WHERE ${keyColumn} = ?`, key);
    const slot =
      stats.n < size
        ? stats.n
        : this.first(`SELECT slot FROM ${table} WHERE ${keyColumn} = ? ORDER BY seq LIMIT 1`, key).slot;
    // seq: ordem de chegada aqui (o celular guarda até onde já leu).
    const seq = Math.max(Date.now(), (stats.last ?? 0) + 1);
    const conv = table === "ring" ? [message.conv] : [];
    this.sql.exec(
      `INSERT OR REPLACE INTO ${table} (${keyColumn}, slot, id, ${table === "ring" ? "conv, " : ""}sender, body, at, seq) VALUES (?, ?, ?, ${table === "ring" ? "?, " : ""}?, ?, ?, ?)`,
      key,
      slot,
      message.id,
      ...conv,
      message.sender.id,
      JSON.stringify(message.body),
      message.sent,
      seq,
    );
  }

  /** Recomendações recebidas por [userId] depois de [since]. */
  recsSince(userId, since) {
    return this.all(
      `SELECT r.id, r.body, r.at, r.seq, u.id AS sid, u.handle, u.name FROM recs r LEFT JOIN users u ON u.id = r.sender
       WHERE r.user_id = ? AND r.seq > ? AND r.seq > ? ORDER BY r.seq`,
      userId,
      Number(since) || 0,
      Date.now() - RECS_MS,
    ).map((row) => recOf(row));
  }

  /**
   * Recomenda [rawCard] a amigos [to]. [cid] é o id gerado no celular:
   * reenviar não duplica. Devolve os ids de quem recebeu.
   */
  recommend(me, to, rawCard, cid) {
    const card = cleanCard(rawCard);
    const ids = Array.isArray(to) ? [...new Set(to.filter((id) => typeof id === "string" && USER_ID.test(id)))].slice(0, MAX_RECIPIENTS) : [];
    if (!card || ids.length === 0) return { status: 400, data: { error: "invalid" } };
    const id = typeof cid === "string" && USER_ID.test(cid) ? cid : randomHex(16);
    const at = Date.now();
    const sender = { id: me.id, handle: me.handle, name: me.name };
    const sent = [];
    for (const toId of ids) {
      if (!this.isFriend(me.id, toId)) continue;
      if (!this.allow("recs", me.id, RECOMMENDATIONS_PER_DAY)) return { status: 429, data: { error: "daily_limit", sent } };
      sent.push(toId);
      if (this.blocked(toId, me.id)) continue; // bloqueado: some em silêncio
      this.ringPut("recs", "user_id", toId, RECS_SIZE, { id, sent: at, body: card, sender });
      const row = this.first("SELECT seq FROM recs WHERE user_id = ? AND id = ?", toId, id);
      for (const ws of this.sockets(toId)) send(ws, { t: "rec", items: [{ id, at, seq: row?.seq ?? 0, card, sender }] });
    }
    return { status: 200, data: { id, sent } };
  }

  async webSocketMessage(ws, raw) {
    if (typeof raw !== "string" || raw.length > MAX_SOCKET_MESSAGE) return;
    if (raw === "ping") return send(ws, "pong");
    let msg;
    try {
      msg = JSON.parse(raw);
    } catch {
      return;
    }
    const state = safeAttachment(ws);
    const me = state?.id ? this.first("SELECT * FROM users WHERE id = ?", state.id) : null;
    if (!me) {
      ws.close(4001, "unauthorized");
      return;
    }
    switch (msg?.t) {
      case "hello":
        // Recomendações que chegaram desde a última vez (o celular guarda até onde já leu).
        return send(ws, { t: "rec", items: this.recsSince(me.id, msg.since), complete: true });
      case "now": {
        // "Ouvindo agora": guardado na conexão, nunca no banco.
        if (me.sharing !== 1) return;
        const track = msg.track === null || msg.track === undefined ? null : cleanTrack(msg.track);
        if (msg.track && !track) return;
        if (JSON.stringify(track) === JSON.stringify(state.now ?? null)) return;
        ws.serializeAttachment({ ...state, now: track, at: Date.now() });
        this.announce(me);
        return;
      }
      case "listened": {
        if (me.sharing !== 1) return;
        const track = cleanTrack(msg.track);
        if (!track) return;
        const at = Number.isFinite(msg.at) && msg.at <= Date.now() ? msg.at : Date.now();
        const recent = [{ at, ...track }, ...(state.recent ?? [])].slice(0, RECENT_KEEP);
        ws.serializeAttachment({ ...state, recent, flushed: false, at: state.at || Date.now() });
        this.announce(me);
        return;
      }
      case "react": {
        // Reação com emoji ao que o amigo está ouvindo: só ao vivo, nada gravado.
        if (typeof msg.to !== "string" || !REACTIONS.includes(msg.emoji)) return;
        if (!this.isFriend(me.id, msg.to) || this.blocked(msg.to, me.id)) return;
        if (!this.allow("reactions", me.id, REACTIONS_PER_MINUTE, 60_000)) return;
        const title = typeof msg.title === "string" ? msg.title.trim().slice(0, 200) || null : null;
        const from = { id: me.id, handle: me.handle, name: me.name };
        for (const socket of this.sockets(msg.to)) send(socket, { t: "reaction", from, emoji: msg.emoji, title, at: Date.now() });
        return;
      }
      case "recommend": {
        const result = this.recommend(me, msg.to, msg.card, msg.cid);
        send(ws, { t: "recommended", cid: msg.cid ?? null, status: result.status, ...result.data });
        return;
      }
      case "activity":
        return send(ws, { t: "activity", people: this.activityFor(me) });
    }
  }

  async webSocketClose(ws, code) {
    this.closeSocket(ws, code);
  }

  async webSocketError(ws) {
    this.closeSocket(ws, 1011);
  }

  /** Fechou: grava os recentes da sessão e avisa os amigos que parou de ouvir. */
  closeSocket(ws, code) {
    const state = safeAttachment(ws);
    this.flushRecent(ws);
    try {
      ws.serializeAttachment({ ...(safeAttachment(ws) ?? {}), closed: true });
    } catch {
      // Já fechada.
    }
    try {
      ws.close(code === 1005 || code === 1006 || !code ? 1000 : code, "bye");
    } catch {
      // Já fechada.
    }
    if (state?.id && state.now) {
      const user = this.first("SELECT * FROM users WHERE id = ?", state.id);
      if (user) this.announce(user);
    }
  }

  /** Guarda a imagem em pedaços de até 1 MB (troca a anterior inteira). */
  saveImage(userId, kind, bytes) {
    this.deleteImage(userId, kind);
    for (let part = 0, offset = 0; offset < bytes.length; part++, offset += IMAGE_PART_BYTES) {
      this.sql.exec(
        "INSERT INTO image_parts (user_id, kind, part, data) VALUES (?, ?, ?, ?)",
        userId,
        kind,
        part,
        bytes.slice(offset, offset + IMAGE_PART_BYTES),
      );
    }
  }

  deleteImage(userId, kind) {
    this.sql.exec("DELETE FROM image_parts WHERE user_id = ? AND kind = ?", userId, kind);
    this.sql.exec("DELETE FROM images WHERE user_id = ? AND kind = ?", userId, kind);
  }

  /** Junta os pedaços (ou lê do formato antigo, de uma linha só). */
  loadImage(userId, kind) {
    const parts = this.all(
      "SELECT data FROM image_parts WHERE user_id = ? AND kind = ? ORDER BY part",
      userId,
      kind,
    ).map((row) => new Uint8Array(row.data));
    if (parts.length === 0) {
      const old = this.first("SELECT data FROM images WHERE user_id = ? AND kind = ?", userId, kind);
      return old ? new Uint8Array(old.data) : null;
    }
    const total = parts.reduce((sum, part) => sum + part.length, 0);
    const joined = new Uint8Array(total);
    let offset = 0;
    for (const part of parts) {
      joined.set(part, offset);
      offset += part.length;
    }
    return joined;
  }

  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/^\/v1/, "");
    const method = request.method;
    const imagePath = path.match(/^\/me\/image\/(avatar|banner)$/);
    const body =
      method === "GET" || method === "DELETE" || imagePath ? null : await request.json().catch(() => null);

    // ---- Sem login ----
    if (method === "GET" && path.startsWith("/handles/")) {
      const handle = decodeURIComponent(path.slice("/handles/".length)).toLowerCase();
      if (!HANDLE.test(handle)) return json({ available: false, valid: false });
      const taken = this.first("SELECT 1 AS x FROM users WHERE handle = ?", handle);
      return json({ available: !taken, valid: true });
    }
    if (method === "POST" && path === "/register") {
      const ip = request.headers.get("cf-connecting-ip") ?? "local";
      if (!this.allow("register", ip, REGISTRATIONS_PER_IP_HOUR, 3_600_000)) return json({ error: "slow_down" }, 429);
      const handle = typeof body?.handle === "string" ? body.handle.trim().toLowerCase() : "";
      const name = cleanName(body?.name);
      if (!HANDLE.test(handle) || !name) return json({ error: "invalid" }, 400);
      if (this.first("SELECT 1 AS x FROM users WHERE handle = ?", handle)) return json({ error: "handle_taken" }, 409);
      const id = randomHex(16);
      const token = randomHex(32);
      this.sql.exec(
        "INSERT INTO users (id, handle, name, token_hash, created) VALUES (?, ?, ?, ?, ?)",
        id,
        handle,
        name,
        await sha256(token),
        Date.now(),
      );
      return json({ id, token, handle, name, sharing: false });
    }

    // ---- Daqui para baixo, só com o segredo do aparelho ----
    const me = await this.authenticate(request);
    if (!me) return json({ error: "unauthorized" }, 401);

    // Conexão ao vivo: o Hub dorme entre as mensagens (hibernação) e não cobra enquanto isso.
    if (path === "/ws" && method === "GET") {
      if ((request.headers.get("upgrade") ?? "").toLowerCase() !== "websocket") {
        return json({ error: "expected_websocket" }, 426);
      }
      const pair = new WebSocketPair();
      const [client, server] = Object.values(pair);
      this.ctx.acceptWebSocket(server, [me.id]);
      server.serializeAttachment({ id: me.id, now: null, at: 0, recent: [] });
      send(server, { t: "hello", me: this.ownProfile(me) });
      send(server, { t: "activity", people: this.activityFor(me) });
      return new Response(null, { status: 101, webSocket: client });
    }

    if (path === "/me" && method === "GET") return json(this.ownProfile(me));
    if (path === "/me" && method === "PATCH") {
      const name = body?.name === undefined ? me.name : cleanName(body.name);
      if (!name) return json({ error: "invalid" }, 400);
      const sharing = body?.sharing === undefined ? me.sharing : body.sharing ? 1 : 0;
      const bio = body?.bio === undefined ? me.bio ?? "" : cleanBio(body.bio);
      if (bio === null) return json({ error: "invalid" }, 400);
      // Cor do perfil: "#RRGGBB", ou "" para nenhuma.
      const color = body?.color === undefined ? me.color ?? "" : body.color;
      if (typeof color !== "string" || (color !== "" && !/^#[0-9a-fA-F]{6}$/.test(color))) {
        return json({ error: "invalid" }, 400);
      }
      const links = body?.links === undefined ? parseJson(me.links, {}) : cleanLinks(body.links);
      if (links === null) return json({ error: "invalid" }, 400);
      // Números: só guardados com "Mostrar meu número aos amigos" ligado; desligar apaga.
      const shareNumbers = body?.shareNumbers === undefined ? me.numbers !== "" : body.shareNumbers === true;
      const numbers = body?.numbers === undefined ? parseJson(me.numbers, {}) : cleanNumbers(body.numbers);
      if (numbers === null) return json({ error: "invalid" }, 400);
      const numbersText = shareNumbers ? JSON.stringify(numbers) : "";
      const linksText = JSON.stringify(links);
      const changed =
        name !== me.name ||
        bio !== (me.bio ?? "") ||
        color.toUpperCase() !== (me.color ?? "") ||
        linksText !== (me.links ?? "{}") ||
        numbersText !== (me.numbers ?? "");
      const profileV = changed ? (me.profile_v ?? 0) + 1 : me.profile_v ?? 0;
      this.sql.exec(
        "UPDATE users SET name = ?, sharing = ?, bio = ?, color = ?, links = ?, numbers = ?, profile_v = ? WHERE id = ?",
        name,
        sharing,
        bio,
        color.toUpperCase(),
        linksText,
        numbersText,
        profileV,
        me.id,
      );
      if (!sharing) {
        // Parou de compartilhar: apaga o que estava guardado.
        this.sql.exec("DELETE FROM recent_log WHERE user_id = ?", me.id);
        for (const ws of this.sockets(me.id)) {
          const state = safeAttachment(ws);
          if (state) ws.serializeAttachment({ ...state, now: null, recent: [], at: Date.now() });
        }
      }
      const updated = { ...me, name, sharing, bio, color: color.toUpperCase(), links: linksText, numbers: numbersText, profile_v: profileV };
      if (changed || sharing !== me.sharing) this.announce(updated);
      return json(this.ownProfile(updated));
    }
    if (path === "/me" && method === "DELETE") {
      const friends = this.friendIds(me.id);
      for (const ws of this.sockets(me.id)) ws.close(4001, "deleted");
      for (const table of ["recent_log", "images", "image_parts", "recs"]) this.sql.exec(`DELETE FROM ${table} WHERE user_id = ?`, me.id);
      this.sql.exec("DELETE FROM friends WHERE a = ? OR b = ?", me.id, me.id);
      this.sql.exec("DELETE FROM requests WHERE from_user = ? OR to_user = ?", me.id, me.id);
      this.sql.exec("DELETE FROM blocks WHERE user_id = ? OR blocked = ?", me.id, me.id);
      this.sql.exec("DELETE FROM users WHERE id = ?", me.id);
      this.friendsChanged(...friends);
      return json({ ok: true });
    }

    // Foto de perfil e capa (banner): o app manda a imagem já reduzida (WebP ou GIF).
    if (imagePath && method === "PUT") {
      const bytes = new Uint8Array(await request.arrayBuffer());
      if (!imageType(bytes)) return json({ error: "invalid" }, 400);
      if (!this.allow("images", me.id, IMAGES_PER_DAY)) return json({ error: "daily_limit" }, 429);
      const kind = imagePath[1];
      // Versão = começo do hash da imagem: a mesma foto tem sempre o mesmo endereço no cache do app.
      const version = await imageVersion(bytes);
      const profileV = (me.profile_v ?? 0) + 1;
      this.saveImage(me.id, kind, bytes);
      this.sql.exec(`UPDATE users SET ${kind}_v = ?, profile_v = ? WHERE id = ?`, version, profileV, me.id);
      return json(this.ownProfile({ ...me, [`${kind}_v`]: version, profile_v: profileV }));
    }
    if (imagePath && method === "DELETE") {
      const kind = imagePath[1];
      const profileV = (me.profile_v ?? 0) + 1;
      this.deleteImage(me.id, kind);
      this.sql.exec(`UPDATE users SET ${kind}_v = 0, profile_v = ? WHERE id = ?`, profileV, me.id);
      return json(this.ownProfile({ ...me, [`${kind}_v`]: 0, profile_v: profileV }));
    }
    const userImage = method === "GET" && path.match(/^\/users\/([0-9a-f]{32})\/image\/(avatar|banner)$/);
    if (userImage) {
      if (this.blocked(userImage[1], me.id)) return json({ error: "not_found" }, 404);
      const data = this.loadImage(userImage[1], userImage[2]);
      if (!data) return json({ error: "not_found" }, 404);
      return new Response(data, {
        headers: { "content-type": imageType(data) ?? "image/jpeg", "cache-control": "private, max-age=31536000" },
      });
    }

    if (method === "GET" && path.startsWith("/users/by-handle/")) {
      const handle = decodeURIComponent(path.slice("/users/by-handle/".length)).toLowerCase();
      const user = this.first("SELECT * FROM users WHERE handle = ?", handle);
      // Quem bloqueou você não aparece na busca.
      if (!user || this.blocked(user.id, me.id)) return json({ error: "not_found" }, 404);
      return json(this.profileFor(me, user));
    }
    if (method === "GET" && path.startsWith("/users/")) {
      const user = this.first("SELECT * FROM users WHERE id = ?", path.slice("/users/".length));
      if (!user) return json({ error: "not_found" }, 404);
      return json(this.profileFor(me, user));
    }

    if (method === "GET" && path === "/activity") return json({ people: this.activityFor(me) });

    // ---- Amizade: pedido + aceite ----
    if (method === "GET" && path === "/friends") {
      const users = (rows) => rows.map((row) => this.first("SELECT * FROM users WHERE id = ?", row.id)).filter(Boolean).map((u) => this.publicUser(u));
      return json({
        friends: this.activityFor(me),
        incoming: users(this.all("SELECT from_user AS id FROM requests WHERE to_user = ? ORDER BY at DESC", me.id)).filter(
          (u) => !this.blocked(me.id, u.id),
        ),
        outgoing: users(this.all("SELECT to_user AS id FROM requests WHERE from_user = ? ORDER BY at DESC", me.id)),
      });
    }
    const friendPath = path.match(/^\/friends\/([0-9a-f]{32})$/);
    if (friendPath) {
      const other = this.first("SELECT * FROM users WHERE id = ?", friendPath[1]);
      if (!other || other.id === me.id) return json({ error: "not_found" }, 404);
      if (method === "DELETE") {
        // Desfazer amizade, cancelar o seu pedido ou recusar o dele.
        const [a, b] = me.id < other.id ? [me.id, other.id] : [other.id, me.id];
        this.sql.exec("DELETE FROM friends WHERE a = ? AND b = ?", a, b);
        this.sql.exec("DELETE FROM requests WHERE (from_user = ? AND to_user = ?) OR (from_user = ? AND to_user = ?)", me.id, other.id, other.id, me.id);
        this.friendsChanged(me.id, other.id);
        return json({ relation: "none" });
      }
      // POST: pedir, ou aceitar se a outra pessoa já tinha pedido.
      const current = this.relation(me.id, other.id);
      if (current === "friend" || current === "sent") return json({ relation: current });
      if (this.blocked(other.id, me.id) || this.blocked(me.id, other.id)) return json({ relation: "sent" }); // bloqueado: some em silêncio
      if (current === "received") {
        const [a, b] = me.id < other.id ? [me.id, other.id] : [other.id, me.id];
        this.sql.exec("DELETE FROM requests WHERE from_user = ? AND to_user = ?", other.id, me.id);
        this.sql.exec("INSERT OR IGNORE INTO friends (a, b) VALUES (?, ?)", a, b);
        this.friendsChanged(me.id, other.id);
        return json({ relation: "friend" });
      }
      if (!this.allow("friend_requests", me.id, FRIEND_REQUESTS_PER_DAY)) return json({ error: "daily_limit" }, 429);
      this.sql.exec("INSERT OR IGNORE INTO requests (from_user, to_user, at) VALUES (?, ?, ?)", me.id, other.id, Date.now());
      this.friendsChanged(other.id);
      return json({ relation: "sent" });
    }

    // ---- Recomendações (reserva HTTP; o normal é pela conexão) ----
    if (path === "/recommendations" && method === "GET") {
      return json({ items: this.recsSince(me.id, url.searchParams.get("since")) });
    }
    if (path === "/recommendations" && method === "POST") {
      const result = this.recommend(me, body?.to, body?.card, body?.cid);
      return json(result.data, result.status);
    }

    if (method === "POST" && path === "/blocks") {
      if (typeof body?.id !== "string" || !USER_ID.test(body.id)) return json({ error: "invalid" }, 400);
      this.sql.exec("INSERT OR IGNORE INTO blocks (user_id, blocked) VALUES (?, ?)", me.id, body.id);
      // Bloquear também desfaz a amizade e os pedidos.
      const [a, b] = me.id < body.id ? [me.id, body.id] : [body.id, me.id];
      this.sql.exec("DELETE FROM friends WHERE a = ? AND b = ?", a, b);
      this.sql.exec("DELETE FROM requests WHERE (from_user = ? AND to_user = ?) OR (from_user = ? AND to_user = ?)", me.id, body.id, body.id, me.id);
      this.friendsChanged(me.id, body.id);
      return json({ ok: true });
    }
    if (method === "DELETE" && path.startsWith("/blocks/")) {
      this.sql.exec("DELETE FROM blocks WHERE user_id = ? AND blocked = ?", me.id, path.slice("/blocks/".length));
      return json({ ok: true });
    }

    return json({ error: "not_found" }, 404);
  }
}
