// Boom, listener! — Servidor social (Cloudflare Worker + Durable Object)
// Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE
//
// O que isto faz: guarda perfis (@nome), entrega mensagens (conversa e grupo)
// e o "ouvindo agora" de quem escolheu compartilhar. NUNCA passa áudio: só
// texto e informações de músicas (título, artista, capa, links).
//
// As mensagens ficam aqui só até o app do destinatário confirmar que recebeu;
// depois são apagadas. O histórico do chat mora no celular de cada um.
//
// Sem login do Google: ao criar o perfil, o app recebe um id e um segredo
// (token) que guarda no aparelho. O servidor guarda só o hash do segredo.
//
// Tudo roda num único Durable Object ("Hub") com SQLite, que cabe na faixa
// gratuita da Cloudflare para o tamanho do app.

import { DurableObject } from "cloudflare:workers";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/health") return json({ ok: true });
    if (!url.pathname.startsWith("/v1/")) return json({ error: "not_found" }, 404);
    const hub = env.HUB.get(env.HUB.idFromName("hub"));
    return hub.fetch(request);
  },
};

const HANDLE = /^[a-z0-9_.]{3,20}$/;
const MAX_TEXT = 2000;
const MAX_BODY_BYTES = 4096;
const RECENT_KEEP = 20;
const MESSAGES_PER_MINUTE = 40;
const MAX_BIO = 160;
const MAX_IMAGE_BYTES = 400_000;
const MAX_GIF_BYTES = 6_000_000;
// O SQLite do Durable Object guarda até 2 MB por valor: imagens maiores vão em pedaços.
const IMAGE_PART_BYTES = 1_000_000;

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

async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Código de grupo fácil de ditar: 8 letras/números sem os que confundem (0/O, 1/I). */
function groupCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const array = new Uint8Array(8);
  crypto.getRandomValues(array);
  return [...array].map((b) => alphabet[b % alphabet.length]).join("");
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

/** Foto ou capa: JPEG até 400 KB, ou GIF (animado) até 6 MB. Devolve o tipo, ou null. */
function imageType(bytes) {
  if (bytes.length >= 3 && bytes.length <= MAX_IMAGE_BYTES && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return "image/jpeg";
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

function cleanBody(value) {
  if (!value || typeof value !== "object") return null;
  if (value.type === "text") {
    if (typeof value.text !== "string") return null;
    const text = value.text.trim();
    if (!text || text.length > MAX_TEXT) return null;
    return { type: "text", text };
  }
  if (value.type === "music") {
    const str = (v, max) => (typeof v === "string" && v.trim() ? v.trim().slice(0, max) : null);
    const title = str(value.title, 200);
    const kind = ["track", "album", "artist"].includes(value.kind) ? value.kind : "track";
    if (!title) return null;
    const url = (v) => (typeof v === "string" && /^https:\/\//.test(v) ? v.slice(0, 500) : null);
    return {
      type: "music",
      kind,
      title,
      artist: str(value.artist, 200),
      album: str(value.album, 200),
      coverUrl: url(value.coverUrl),
      link: url(value.link),
      note: str(value.note, 500),
    };
  }
  return null;
}

export class Hub extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.sql = ctx.storage.sql;
    this.rate = new Map();
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY, handle TEXT UNIQUE NOT NULL, name TEXT NOT NULL,
        token_hash TEXT NOT NULL, created INTEGER NOT NULL, sharing INTEGER NOT NULL DEFAULT 0
      );
      CREATE TABLE IF NOT EXISTS status (user_id TEXT PRIMARY KEY, now TEXT, updated INTEGER NOT NULL);
      CREATE TABLE IF NOT EXISTS recent (user_id TEXT NOT NULL, at INTEGER NOT NULL, track TEXT NOT NULL);
      CREATE INDEX IF NOT EXISTS recent_user ON recent(user_id, at);
      CREATE TABLE IF NOT EXISTS groups (
        id TEXT PRIMARY KEY, name TEXT NOT NULL, code TEXT UNIQUE NOT NULL, owner TEXT NOT NULL, created INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS members (group_id TEXT NOT NULL, user_id TEXT NOT NULL, PRIMARY KEY (group_id, user_id));
      CREATE TABLE IF NOT EXISTS contacts (a TEXT NOT NULL, b TEXT NOT NULL, PRIMARY KEY (a, b));
      CREATE TABLE IF NOT EXISTS blocks (user_id TEXT NOT NULL, blocked TEXT NOT NULL, PRIMARY KEY (user_id, blocked));
      CREATE TABLE IF NOT EXISTS inbox (
        id TEXT PRIMARY KEY, to_user TEXT NOT NULL, conv TEXT NOT NULL, sender TEXT NOT NULL,
        body TEXT NOT NULL, sent INTEGER NOT NULL
      );
      CREATE INDEX IF NOT EXISTS inbox_user ON inbox(to_user, sent);
      CREATE TABLE IF NOT EXISTS image_parts (
        user_id TEXT NOT NULL, kind TEXT NOT NULL, part INTEGER NOT NULL, data BLOB NOT NULL,
        PRIMARY KEY (user_id, kind, part)
      );
      CREATE TABLE IF NOT EXISTS images (
        user_id TEXT NOT NULL, kind TEXT NOT NULL, data BLOB NOT NULL, PRIMARY KEY (user_id, kind)
      );
    `);
    // Colunas que chegaram depois (descrição, versões da foto e da capa).
    for (const column of [
      "bio TEXT NOT NULL DEFAULT ''",
      "avatar_v INTEGER NOT NULL DEFAULT 0",
      "banner_v INTEGER NOT NULL DEFAULT 0",
      "color TEXT NOT NULL DEFAULT ''",
    ]) {
      try {
        this.sql.exec(`ALTER TABLE users ADD COLUMN ${column}`);
      } catch {
        // Já existe.
      }
    }
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

  publicUser(user) {
    return {
      id: user.id,
      handle: user.handle,
      name: user.name,
      bio: user.bio ?? "",
      avatar: user.avatar_v ?? 0,
      banner: user.banner_v ?? 0,
      color: user.color ?? "",
    };
  }

  /** Conversaram em particular ou estão num mesmo grupo. */
  related(a, b) {
    if (a === b) return true;
    if (this.first("SELECT 1 AS x FROM contacts WHERE a = ? AND b = ?", a, b)) return true;
    return !!this.first(
      "SELECT 1 AS x FROM members m1 JOIN members m2 ON m1.group_id = m2.group_id WHERE m1.user_id = ? AND m2.user_id = ?",
      a,
      b,
    );
  }

  blocked(owner, other) {
    return !!this.first("SELECT 1 AS x FROM blocks WHERE user_id = ? AND blocked = ?", owner, other);
  }

  /** O que [viewer] pode ver de [user]: o "ouvindo" só com compartilhamento ligado e sendo próximos. */
  profileFor(viewer, user) {
    const profile = this.publicUser(user);
    const canSee =
      user.sharing === 1 && this.related(viewer.id, user.id) && !this.blocked(user.id, viewer.id);
    if (canSee) {
      const status = this.first("SELECT now, updated FROM status WHERE user_id = ?", user.id);
      profile.now = status?.now ? JSON.parse(status.now) : null;
      profile.updated = status?.updated ?? null;
      profile.recent = this.all(
        "SELECT at, track FROM recent WHERE user_id = ? ORDER BY at DESC LIMIT ?",
        user.id,
        RECENT_KEEP,
      ).map((row) => ({ at: row.at, ...JSON.parse(row.track) }));
    }
    profile.sharing = canSee;
    profile.blockedByMe = this.blocked(viewer.id, user.id);
    return profile;
  }

  allowMessage(userId) {
    const now = Date.now();
    const recent = (this.rate.get(userId) ?? []).filter((t) => now - t < 60_000);
    if (recent.length >= MESSAGES_PER_MINUTE) return false;
    recent.push(now);
    this.rate.set(userId, recent);
    return true;
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

  groupInfo(group) {
    const members = this.all(
      "SELECT u.id, u.handle, u.name FROM members m JOIN users u ON u.id = m.user_id WHERE m.group_id = ? ORDER BY u.name",
      group.id,
    );
    return { id: group.id, name: group.name, code: group.code, owner: group.owner, members };
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

    if (path === "/me" && method === "GET") return json({ ...this.publicUser(me), sharing: me.sharing === 1 });
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
      this.sql.exec(
        "UPDATE users SET name = ?, sharing = ?, bio = ?, color = ? WHERE id = ?",
        name,
        sharing,
        bio,
        color.toUpperCase(),
        me.id,
      );
      if (!sharing) {
        // Parou de compartilhar: apaga o que estava guardado.
        this.sql.exec("DELETE FROM status WHERE user_id = ?", me.id);
        this.sql.exec("DELETE FROM recent WHERE user_id = ?", me.id);
      }
      return json({ ...this.publicUser({ ...me, name, bio, color: color.toUpperCase() }), sharing: sharing === 1 });
    }
    if (path === "/me" && method === "DELETE") {
      for (const table of ["status", "recent", "images", "image_parts"]) this.sql.exec(`DELETE FROM ${table} WHERE user_id = ?`, me.id);
      this.sql.exec("DELETE FROM members WHERE user_id = ?", me.id);
      this.sql.exec("DELETE FROM contacts WHERE a = ? OR b = ?", me.id, me.id);
      this.sql.exec("DELETE FROM blocks WHERE user_id = ? OR blocked = ?", me.id, me.id);
      this.sql.exec("DELETE FROM inbox WHERE to_user = ? OR sender = ?", me.id, me.id);
      this.sql.exec("DELETE FROM users WHERE id = ?", me.id);
      return json({ ok: true });
    }

    // Foto de perfil e capa (banner): o app manda o JPEG já reduzido.
    if (imagePath && method === "PUT") {
      const bytes = new Uint8Array(await request.arrayBuffer());
      if (!imageType(bytes)) return json({ error: "invalid" }, 400);
      const kind = imagePath[1];
      const version = Date.now();
      this.saveImage(me.id, kind, bytes);
      this.sql.exec(`UPDATE users SET ${kind}_v = ? WHERE id = ?`, version, me.id);
      return json({ ...this.publicUser({ ...me, [`${kind}_v`]: version }), sharing: me.sharing === 1 });
    }
    if (imagePath && method === "DELETE") {
      const kind = imagePath[1];
      this.deleteImage(me.id, kind);
      this.sql.exec(`UPDATE users SET ${kind}_v = 0 WHERE id = ?`, me.id);
      return json({ ...this.publicUser({ ...me, [`${kind}_v`]: 0 }), sharing: me.sharing === 1 });
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

    if (path === "/me/now" && method === "PUT") {
      if (me.sharing !== 1) return json({ ok: true, ignored: true });
      const track = body?.track === null ? null : cleanTrack(body?.track);
      this.sql.exec(
        "INSERT INTO status (user_id, now, updated) VALUES (?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET now = excluded.now, updated = excluded.updated",
        me.id,
        track ? JSON.stringify(track) : null,
        Date.now(),
      );
      return json({ ok: true });
    }
    if (path === "/me/recent" && method === "POST") {
      if (me.sharing !== 1) return json({ ok: true, ignored: true });
      const track = cleanTrack(body?.track);
      if (!track) return json({ error: "invalid" }, 400);
      this.sql.exec("INSERT INTO recent (user_id, at, track) VALUES (?, ?, ?)", me.id, Date.now(), JSON.stringify(track));
      this.sql.exec(
        "DELETE FROM recent WHERE user_id = ? AND at NOT IN (SELECT at FROM recent WHERE user_id = ? ORDER BY at DESC LIMIT ?)",
        me.id,
        me.id,
        RECENT_KEEP,
      );
      return json({ ok: true });
    }

    if (method === "GET" && path.startsWith("/users/by-handle/")) {
      const handle = decodeURIComponent(path.slice("/users/by-handle/".length)).toLowerCase();
      const user = this.first("SELECT * FROM users WHERE handle = ?", handle);
      if (!user) return json({ error: "not_found" }, 404);
      return json(this.profileFor(me, user));
    }
    if (method === "GET" && path.startsWith("/users/")) {
      const user = this.first("SELECT * FROM users WHERE id = ?", path.slice("/users/".length));
      if (!user) return json({ error: "not_found" }, 404);
      return json(this.profileFor(me, user));
    }

    if (method === "GET" && path === "/activity") {
      // Todo mundo com quem conversei ou divido grupo, que compartilha o que ouve.
      const ids = this.all(
        `SELECT b AS id FROM contacts WHERE a = ?
         UNION SELECT m2.user_id AS id FROM members m1 JOIN members m2 ON m1.group_id = m2.group_id
         WHERE m1.user_id = ? AND m2.user_id != ?`,
        me.id,
        me.id,
        me.id,
      ).map((row) => row.id);
      const people = ids
        .map((id) => this.first("SELECT * FROM users WHERE id = ?", id))
        .filter((user) => user && user.sharing === 1)
        .map((user) => this.profileFor(me, user))
        .filter((profile) => profile.sharing);
      return json({ people });
    }

    if (method === "POST" && path === "/messages") {
      const message = cleanBody(body?.body);
      const conv = typeof body?.conv === "string" ? body.conv : "";
      if (!message || JSON.stringify(message).length > MAX_BODY_BYTES) return json({ error: "invalid" }, 400);
      if (!this.allowMessage(me.id)) return json({ error: "slow_down" }, 429);
      const id = randomHex(16);
      const sent = Date.now();
      const payload = JSON.stringify(message);
      if (conv.startsWith("dm:")) {
        const to = this.first("SELECT * FROM users WHERE id = ?", conv.slice(3));
        if (!to) return json({ error: "not_found" }, 404);
        if (this.blocked(to.id, me.id)) return json({ id, sent }); // bloqueado: some em silêncio
        this.sql.exec("INSERT OR IGNORE INTO contacts (a, b) VALUES (?, ?)", me.id, to.id);
        this.sql.exec("INSERT OR IGNORE INTO contacts (a, b) VALUES (?, ?)", to.id, me.id);
        this.sql.exec(
          "INSERT INTO inbox (id, to_user, conv, sender, body, sent) VALUES (?, ?, ?, ?, ?, ?)",
          id,
          to.id,
          "dm:" + me.id,
          me.id,
          payload,
          sent,
        );
        return json({ id, sent });
      }
      if (conv.startsWith("g:")) {
        const groupId = conv.slice(2);
        if (!this.first("SELECT 1 AS x FROM members WHERE group_id = ? AND user_id = ?", groupId, me.id)) {
          return json({ error: "not_member" }, 403);
        }
        const others = this.all("SELECT user_id FROM members WHERE group_id = ? AND user_id != ?", groupId, me.id);
        for (const row of others) {
          if (this.blocked(row.user_id, me.id)) continue;
          this.sql.exec(
            "INSERT INTO inbox (id, to_user, conv, sender, body, sent) VALUES (?, ?, ?, ?, ?, ?)",
            id + row.user_id.slice(0, 8),
            row.user_id,
            conv,
            me.id,
            payload,
            sent,
          );
        }
        return json({ id, sent });
      }
      return json({ error: "invalid" }, 400);
    }

    if (method === "GET" && path === "/inbox") {
      const rows = this.all(
        "SELECT i.id, i.conv, i.body, i.sent, u.id AS sid, u.handle, u.name FROM inbox i LEFT JOIN users u ON u.id = i.sender WHERE i.to_user = ? ORDER BY i.sent LIMIT 200",
        me.id,
      );
      return json({
        messages: rows.map((row) => ({
          id: row.id,
          conv: row.conv,
          sent: row.sent,
          body: JSON.parse(row.body),
          sender: row.sid ? { id: row.sid, handle: row.handle, name: row.name } : null,
        })),
      });
    }
    if (method === "POST" && path === "/inbox/ack") {
      const ids = Array.isArray(body?.ids) ? body.ids.filter((v) => typeof v === "string").slice(0, 500) : [];
      for (const id of ids) this.sql.exec("DELETE FROM inbox WHERE id = ? AND to_user = ?", id, me.id);
      return json({ ok: true });
    }

    if (method === "POST" && path === "/groups") {
      const name = cleanName(body?.name);
      if (!name) return json({ error: "invalid" }, 400);
      const id = randomHex(16);
      let code = groupCode();
      while (this.first("SELECT 1 AS x FROM groups WHERE code = ?", code)) code = groupCode();
      this.sql.exec("INSERT INTO groups (id, name, code, owner, created) VALUES (?, ?, ?, ?, ?)", id, name, code, me.id, Date.now());
      this.sql.exec("INSERT INTO members (group_id, user_id) VALUES (?, ?)", id, me.id);
      return json(this.groupInfo(this.first("SELECT * FROM groups WHERE id = ?", id)));
    }
    if (method === "POST" && path === "/groups/join") {
      const code = typeof body?.code === "string" ? body.code.trim().toUpperCase().replace(/[^A-Z0-9]/g, "") : "";
      const group = this.first("SELECT * FROM groups WHERE code = ?", code);
      if (!group) return json({ error: "not_found" }, 404);
      this.sql.exec("INSERT OR IGNORE INTO members (group_id, user_id) VALUES (?, ?)", group.id, me.id);
      return json(this.groupInfo(group));
    }
    const groupMatch = path.match(/^\/groups\/([0-9a-f]{32})(\/leave)?$/);
    if (groupMatch) {
      const group = this.first("SELECT * FROM groups WHERE id = ?", groupMatch[1]);
      const member = group && this.first("SELECT 1 AS x FROM members WHERE group_id = ? AND user_id = ?", group.id, me.id);
      if (!group || !member) return json({ error: "not_found" }, 404);
      if (method === "GET" && !groupMatch[2]) return json(this.groupInfo(group));
      if (method === "POST" && groupMatch[2]) {
        this.sql.exec("DELETE FROM members WHERE group_id = ? AND user_id = ?", group.id, me.id);
        const left = this.first("SELECT COUNT(*) AS n FROM members WHERE group_id = ?", group.id);
        if (!left || left.n === 0) this.sql.exec("DELETE FROM groups WHERE id = ?", group.id);
        return json({ ok: true });
      }
    }

    if (method === "POST" && path === "/blocks") {
      if (typeof body?.id !== "string") return json({ error: "invalid" }, 400);
      this.sql.exec("INSERT OR IGNORE INTO blocks (user_id, blocked) VALUES (?, ?)", me.id, body.id);
      return json({ ok: true });
    }
    if (method === "DELETE" && path.startsWith("/blocks/")) {
      this.sql.exec("DELETE FROM blocks WHERE user_id = ? AND blocked = ?", me.id, path.slice("/blocks/".length));
      return json({ ok: true });
    }

    return json({ error: "not_found" }, 404);
  }
}
