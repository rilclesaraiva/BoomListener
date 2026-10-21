// Testes do servidor social sem a Cloudflare: troca o Durable Object por um
// SQLite em memória do Node. Rodar com: node --test social/worker.test.mjs
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";

const source = readFileSync(new URL("./worker.js", import.meta.url), "utf8").replace(
  'import { DurableObject } from "cloudflare:workers";',
  "class DurableObject { constructor(ctx, env) { this.ctx = ctx; this.env = env; } }",
);
const mod = await import("data:text/javascript;base64," + Buffer.from(source).toString("base64"));

function fakeSql() {
  const db = new DatabaseSync(":memory:");
  return {
    exec(query, ...args) {
      const trimmed = query.trim();
      if (args.length === 0 && /;\s*\S/.test(trimmed)) {
        db.exec(trimmed);
        return { toArray: () => [] };
      }
      const statement = db.prepare(trimmed);
      if (/^\s*(SELECT|WITH)/i.test(trimmed)) return { toArray: () => statement.all(...args) };
      statement.run(...args);
      return { toArray: () => [] };
    },
  };
}

function server() {
  const hub = new mod.Hub({ storage: { sql: fakeSql() } }, {});
  return async (method, path, body, auth) => {
    const headers = { "content-type": "application/json" };
    if (auth) headers.authorization = `Bearer ${auth.id}.${auth.token}`;
    const response = await hub.fetch(
      new Request("https://x/v1" + path, { method, headers, body: body ? JSON.stringify(body) : undefined }),
    );
    return { status: response.status, data: await response.json() };
  };
}

test("cadastro, @ ocupado e segredo", async () => {
  const call = server();
  const ana = await call("POST", "/register", { name: "Ana", handle: "Ana_Clara" });
  assert.equal(ana.status, 200);
  assert.equal(ana.data.handle, "ana_clara");
  assert.equal((await call("POST", "/register", { name: "Outra", handle: "ana_clara" })).status, 409);
  assert.equal((await call("GET", "/handles/ana_clara")).data.available, false);
  assert.equal((await call("GET", "/handles/a")).data.valid, false);
  assert.equal((await call("GET", "/me", null, { id: ana.data.id, token: "0".repeat(64) })).status, 401);
  assert.equal((await call("GET", "/me", null, ana.data)).data.name, "Ana");
});

test("descrição, foto e capa do perfil", async () => {
  const hub = new mod.Hub({ storage: { sql: fakeSql() } }, {});
  const send = (method, path, auth, init = {}) =>
    hub.fetch(
      new Request("https://x/v1" + path, {
        method,
        headers: { authorization: `Bearer ${auth.id}.${auth.token}`, ...(init.headers ?? {}) },
        body: init.body,
      }),
    );
  const register = async (handle) =>
    (await (await hub.fetch(new Request("https://x/v1/register", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ name: handle, handle }),
    }))).json());
  const ana = await register("ana");
  const beto = await register("beto");

  // Descrição: curta e limpa.
  const patched = await (await send("PATCH", "/me", ana, {
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ bio: "  Ouço de tudo.\n\n\nMenos sertanejo " + "x".repeat(300) }),
  })).json();
  assert.ok(patched.bio.startsWith("Ouço de tudo.\nMenos sertanejo"));
  assert.equal(patched.bio.length, 160);

  // Cor do perfil: só "#RRGGBB" (ou vazio para tirar).
  const colored = await (await send("PATCH", "/me", ana, {
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ color: "#ff5500" }),
  })).json();
  assert.equal(colored.color, "#FF5500");
  assert.equal(colored.bio, patched.bio);
  assert.equal((await send("PATCH", "/me", ana, {
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ color: "red" }),
  })).status, 400);

  // Só JPEG.
  assert.equal((await send("PUT", "/me/image/avatar", ana, { body: new Uint8Array([1, 2, 3]) })).status, 400);
  const jpeg = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 9, 9, 9]);
  const withPhoto = await (await send("PUT", "/me/image/avatar", ana, { body: jpeg })).json();
  assert.ok(withPhoto.avatar > 0);
  assert.equal(withPhoto.banner, 0);

  // Outra pessoa vê a versão no perfil e baixa a foto.
  const seen = await (await send("GET", "/users/" + ana.id, beto)).json();
  assert.equal(seen.avatar, withPhoto.avatar);
  assert.equal(seen.bio, patched.bio);
  assert.equal(seen.color, "#FF5500");
  const image = await send("GET", `/users/${ana.id}/image/avatar`, beto);
  assert.equal(image.status, 200);
  assert.equal(image.headers.get("content-type"), "image/jpeg");
  assert.deepEqual(new Uint8Array(await image.arrayBuffer()), jpeg);
  assert.equal((await send("GET", `/users/${ana.id}/image/banner`, beto)).status, 404);

  // Bloqueado não vê a foto.
  await send("POST", "/blocks", ana, {
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ id: beto.id }),
  });
  assert.equal((await send("GET", `/users/${ana.id}/image/avatar`, beto)).status, 404);

  // GIF animado na capa também vale.
  const gif = new Uint8Array([...new TextEncoder().encode("GIF89a"), 1, 0, 1, 0]);
  const withGif = await (await send("PUT", "/me/image/banner", ana, { body: gif })).json();
  assert.ok(withGif.banner > 0);
  // GIF grande (3 MB) vai em pedaços e volta inteiro.
  const big = new Uint8Array(3_000_000);
  big.set(new TextEncoder().encode("GIF89a"));
  for (let i = 6; i < big.length; i++) big[i] = i % 251;
  assert.equal((await send("PUT", "/me/image/avatar", ana, { body: big })).status, 200);
  await send("DELETE", "/blocks/" + beto.id, ana);
  const back = new Uint8Array(await (await send("GET", `/users/${ana.id}/image/avatar`, beto)).arrayBuffer());
  assert.equal(back.length, big.length);
  assert.deepEqual(back.slice(2_500_000, 2_500_010), big.slice(2_500_000, 2_500_010));
  assert.equal((await send("PUT", "/me/image/banner", ana, { body: new Uint8Array(3_100_000).fill(0x47) })).status, 400);

  // Tirar a foto.
  const removed = await (await send("DELETE", "/me/image/avatar", ana)).json();
  assert.equal(removed.avatar, 0);
});

class FakeSocket {
  constructor() {
    this.sent = [];
    this.state = null;
    this.closed = null;
  }
  send(data) {
    if (this.closed) throw new Error("closed");
    this.sent.push(typeof data === "string" && data.startsWith("{") ? JSON.parse(data) : data);
  }
  serializeAttachment(value) {
    this.state = structuredClone(value);
  }
  deserializeAttachment() {
    return structuredClone(this.state);
  }
  close(code) {
    this.closed = code ?? 1000;
  }
  events(type) {
    return this.sent.filter((e) => e.t === type);
  }
}

function liveServer() {
  const tagged = [];
  const ctx = {
    storage: { sql: fakeSql() },
    acceptWebSocket(ws, tags) {
      tagged.push({ ws, tags });
    },
    getWebSockets(tag) {
      return tagged.filter((t) => t.tags.includes(tag) && !t.ws.closed).map((t) => t.ws);
    },
  };
  const hub = new mod.Hub(ctx, {});
  const call = async (method, path, body, auth) => {
    const headers = { "content-type": "application/json" };
    if (auth) headers.authorization = `Bearer ${auth.id}.${auth.token}`;
    const response = await hub.fetch(
      new Request("https://x/v1" + path, { method, headers, body: body ? JSON.stringify(body) : undefined }),
    );
    return { status: response.status, data: await response.json() };
  };
  // Conecta como [auth]: devolve o lado do servidor (o que o Hub manda chega em .sent).
  const connect = async (auth, query = "") => {
    let server;
    globalThis.WebSocketPair = class {
      constructor() {
        server = new FakeSocket();
        this[0] = new FakeSocket();
        this[1] = server;
      }
    };
    const RealResponse = globalThis.Response;
    globalThis.Response = class extends RealResponse {
      constructor(body, init) {
        super(body, init?.status === 101 ? { ...init, status: 200 } : init);
        if (init?.status === 101) this.upgraded = true;
      }
    };
    try {
      const response = await hub.fetch(
        new Request("https://x/v1/ws" + query, {
          headers: { authorization: `Bearer ${auth.id}.${auth.token}`, upgrade: "websocket" },
        }),
      );
      assert.ok(response.upgraded);
    } finally {
      globalThis.Response = RealResponse;
    }
    return server;
  };
  const say = (ws, event) => hub.webSocketMessage(ws, JSON.stringify(event));
  return { hub, call, connect, say };
}

test("porteiro recusa sem acordar o Hub", () => {
  const req = (method, path, headers = {}) => mod.gate(new Request("https://x" + path, { method, headers }), new URL("https://x" + path));
  const auth = { authorization: `Bearer ${"a".repeat(32)}.${"b".repeat(64)}` };
  assert.equal(req("GET", "/v1/nada", auth).status, 404);
  assert.equal(req("POST", "/v1/activity", auth).status, 404); // método errado
  assert.equal(req("GET", "/v1/activity").status, 401);
  assert.equal(req("POST", "/v1/messages", auth).status, 404); // o chat saiu
  assert.equal(req("GET", "/v1/ws", auth).status, 426);
  assert.equal(req("GET", "/v1/ws", { ...auth, upgrade: "websocket" }), null);
  assert.equal(req("POST", "/v1/recommendations", { ...auth, "content-length": "999999" }).status, 413);
  assert.equal(req("PUT", "/v1/me/image/avatar", { ...auth, "content-length": "3000000" }), null);
  assert.equal(req("GET", "/v1/handles/ana"), null);
  assert.equal(mod.nextUtcMidnight(Date.UTC(2026, 9, 20, 23, 59)), Date.UTC(2026, 9, 21));
});


async function befriend(call, x, y) {
  assert.equal((await call("POST", "/friends/" + y.id, null, x)).data.relation, "sent");
  assert.equal((await call("POST", "/friends/" + x.id, null, y)).data.relation, "friend");
}

test("amizade: pedido, aceite, desfazer e bloqueio", async () => {
  const { call } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;

  assert.equal((await call("POST", "/friends/" + beto.id, null, ana)).data.relation, "sent");
  assert.equal((await call("GET", "/users/" + ana.id, null, beto)).data.relation, "received");
  const lists = (await call("GET", "/friends", null, beto)).data;
  assert.deepEqual(lists.incoming.map((u) => u.handle), ["ana"]);
  assert.equal((await call("POST", "/friends/" + ana.id, null, beto)).data.relation, "friend");
  assert.deepEqual((await call("GET", "/friends", null, ana)).data.friends.map((p) => p.handle), ["beto"]);
  assert.equal((await call("GET", "/friends", null, beto)).data.incoming.length, 0);

  await call("DELETE", "/friends/" + ana.id, null, beto);
  assert.equal((await call("GET", "/users/" + beto.id, null, ana)).data.relation, "none");

  // Bloqueado: o pedido some em silêncio e a busca não encontra quem bloqueou.
  await call("POST", "/blocks", { id: ana.id }, caio);
  assert.equal((await call("POST", "/friends/" + caio.id, null, ana)).data.relation, "sent");
  assert.equal((await call("GET", "/friends", null, caio)).data.incoming.length, 0);
  assert.equal((await call("GET", "/users/by-handle/caio", null, ana)).status, 404);
});

test("redes públicas para todos; número só para amigos e só com a opção ligada", async () => {
  const { call } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  await befriend(call, ana, beto);

  // Só o @ (sem link colado) e só redes conhecidas.
  assert.equal((await call("PATCH", "/me", { links: { instagram: "https://evil.com/x" } }, ana)).status, 400);
  assert.equal((await call("PATCH", "/me", { links: { orkut: "ana" } }, ana)).status, 400);
  const saved = (await call("PATCH", "/me", { links: { instagram: "@ana.clara", lastfm: "anaclara", x: "" } }, ana)).data;
  assert.deepEqual(saved.links, { instagram: "ana.clara", lastfm: "anaclara" });

  // Número sem a opção ligada: não é guardado.
  const off = (await call("PATCH", "/me", { numbers: { whatsapp: "5591999999999" } }, ana)).data;
  assert.equal(off.shareNumbers, false);
  assert.deepEqual((await call("GET", "/users/" + ana.id, null, beto)).data.numbers, {});
  assert.equal((await call("PATCH", "/me", { shareNumbers: true, numbers: { whatsapp: "(91) 9999" } }, ana)).status, 400);
  const on = (await call("PATCH", "/me", { shareNumbers: true, numbers: { whatsapp: "5591999999999" } }, ana)).data;
  assert.equal(on.shareNumbers, true);

  // Amigo vê o número; quem não é amigo vê só as redes públicas.
  assert.deepEqual((await call("GET", "/users/" + ana.id, null, beto)).data.numbers, { whatsapp: "5591999999999" });
  const stranger = (await call("GET", "/users/by-handle/ana", null, caio)).data;
  assert.deepEqual(stranger.numbers, {});
  assert.equal(stranger.links.instagram, "ana.clara");

  // Desligar apaga do servidor.
  await call("PATCH", "/me", { shareNumbers: false }, ana);
  assert.deepEqual((await call("GET", "/users/" + ana.id, null, beto)).data.numbers, {});
  assert.equal((await call("GET", "/me", null, ana)).data.shareNumbers, false);
});

test("ouvindo agora só para amigos, vive na conexão e os recentes são gravados ao fechar", async () => {
  const { hub, call, connect, say } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  await call("PATCH", "/me", { sharing: true }, ana);
  await befriend(call, ana, beto);

  const betoWs = await connect(beto);
  assert.deepEqual(betoWs.events("activity")[0].people.map((p) => p.handle), ["ana"]);
  const anaWs = await connect(ana);
  await say(anaWs, { t: "now", track: { title: "Oceano", artist: "Djavan" } });
  assert.equal(betoWs.events("person").at(-1).profile.now.title, "Oceano");
  assert.equal((await call("GET", "/users/" + ana.id, null, caio)).data.now, undefined); // não é amigo

  await say(anaWs, { t: "listened", track: { title: "Flor de Lis", artist: "Djavan" } });
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM recent_log").n, 0); // ainda só na conexão
  await hub.webSocketClose(anaWs, 1000);
  assert.equal(betoWs.events("person").at(-1).profile.now, null);
  const after = (await call("GET", "/users/" + ana.id, null, beto)).data;
  assert.equal(after.recent[0].title, "Flor de Lis");
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM recent_log").n, 1);
});

test("reação com emoji: só entre amigos, só ao vivo, sem gravar", async () => {
  const { hub, call, connect, say } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  await befriend(call, ana, beto);
  const anaWs = await connect(ana);
  const betoWs = await connect(beto);
  const caioWs = await connect(caio);
  await say(betoWs, { t: "react", to: ana.id, emoji: "🔥", title: "Oceano" });
  assert.deepEqual(
    anaWs.events("reaction").map((r) => [r.from.handle, r.emoji, r.title]),
    [["beto", "🔥", "Oceano"]],
  );
  await say(betoWs, { t: "react", to: ana.id, emoji: "texto livre" }); // só os emojis da lista
  await say(caioWs, { t: "react", to: ana.id, emoji: "❤️" }); // não é amigo
  assert.equal(anaWs.events("reaction").length, 1);
  const tables = hub.all("SELECT name FROM sqlite_master WHERE type = 'table'").map((t) => t.name);
  assert.ok(!tables.includes("reactions"));
});

test("recomendação: só para amigos, uma escrita, ao vivo e pelo anel depois", async () => {
  const { hub, call, connect, say } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  await befriend(call, ana, beto);
  const betoWs = await connect(beto);
  const anaWs = await connect(ana);
  const card = { kind: "track", title: "Açaí", artist: "Djavan", coverUrl: "http://inseguro", audio: "AAAA" };
  await say(anaWs, { t: "recommend", cid: "a".repeat(32), to: [beto.id, caio.id], card });
  const reply = anaWs.events("recommended")[0];
  assert.deepEqual(reply.sent, [beto.id]); // Caio não é amigo
  const live = betoWs.events("rec")[0].items[0];
  assert.equal(live.card.title, "Açaí");
  assert.equal(live.card.coverUrl, null);
  assert.equal(live.card.audio, undefined);
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM recs").n, 1);

  // Reenviar (resposta perdida) não duplica; ao conectar de novo vem pelo anel, só o que falta.
  await say(anaWs, { t: "recommend", cid: "a".repeat(32), to: [beto.id], card });
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM recs").n, 1);
  await say(betoWs, { t: "hello", since: 0 });
  const all = betoWs.events("rec").at(-1).items;
  assert.equal(all.length, 1);
  await say(betoWs, { t: "hello", since: all[0].seq });
  assert.equal(betoWs.events("rec").at(-1).items.length, 0);
  assert.equal((await call("GET", "/recommendations?since=0", null, beto)).data.items[0].sender.handle, "ana");

  // Anel de 50.
  for (let i = 0; i < 55; i++) {
    hub.ringPut("recs", "user_id", beto.id, 50, { id: i.toString(16).padStart(32, "0"), sent: Date.now(), body: card, sender: { id: ana.id } });
  }
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM recs WHERE user_id = ?", beto.id).n, 50);
});

test("migração: quem conversava vira amigo e o chat some do servidor", async () => {
  const sql = fakeSql();
  sql.exec(`
    CREATE TABLE users (id TEXT PRIMARY KEY, handle TEXT UNIQUE NOT NULL, name TEXT NOT NULL, token_hash TEXT NOT NULL, created INTEGER NOT NULL, sharing INTEGER NOT NULL DEFAULT 0);
    CREATE TABLE contacts (a TEXT NOT NULL, b TEXT NOT NULL, PRIMARY KEY (a, b));
    CREATE TABLE inbox (id TEXT PRIMARY KEY, to_user TEXT, conv TEXT, sender TEXT, body TEXT, sent INTEGER);
  `);
  sql.exec("INSERT INTO contacts (a, b) VALUES (?, ?)", "a".repeat(32), "b".repeat(32));
  sql.exec("INSERT INTO contacts (a, b) VALUES (?, ?)", "b".repeat(32), "a".repeat(32));
  sql.exec("INSERT INTO inbox (id, to_user, conv, sender, body, sent) VALUES ('x', 'y', 'dm:z', 'z', '{}', 1)");
  const hub = new mod.Hub({ storage: { sql } }, {});
  assert.deepEqual(hub.all("SELECT a, b FROM friends").map((r) => [r.a, r.b]), [["a".repeat(32), "b".repeat(32)]]);
  const tables = hub.all("SELECT name FROM sqlite_master WHERE type = 'table'").map((t) => t.name);
  for (const gone of ["inbox", "contacts", "ring", "reminders", "groups", "members"]) assert.ok(!tables.includes(gone), gone);
  // Roda uma vez só.
  new mod.Hub({ storage: { sql } }, {});
  assert.equal(hub.all("SELECT COUNT(*) AS n FROM friends")[0].n, 1);
});

test("cadastros por IP", async () => {
  const { call } = liveServer();
  for (let i = 0; i < 5; i++) assert.equal((await call("POST", "/register", { name: "R", handle: "reg" + i })).status, 200);
  assert.equal((await call("POST", "/register", { name: "X", handle: "xxxx" })).status, 429);
});

test("WebP aceito, versão pelo conteúdo e perfil com número de versão", async () => {
  const { hub, call } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const put = async (bytes) =>
    (await hub.fetch(new Request("https://x/v1/me/image/avatar", {
      method: "PUT",
      headers: { authorization: `Bearer ${ana.id}.${ana.token}` },
      body: bytes,
    }))).json();
  const webp = new Uint8Array([...new TextEncoder().encode("RIFF"), 0, 0, 0, 0, ...new TextEncoder().encode("WEBP"), 1, 2, 3]);
  const first = await put(webp);
  assert.ok(first.avatar > 0 && Number.isSafeInteger(first.avatar));
  assert.equal(first.v, 1);
  assert.equal((await put(webp)).avatar, first.avatar);
  const renamed = (await call("PATCH", "/me", { name: "Ana Clara" }, ana)).data;
  assert.equal(renamed.v, 3);
  assert.equal((await call("PATCH", "/me", { sharing: true }, ana)).data.v, 3);
  assert.equal((await call("GET", "/users/" + ana.id, null, beto)).data.v, 3);
});

test("ouvindo agora aceita capa só do Deezer", () => {
  const ok = mod.cleanTrack({ title: "Oceano", artist: "Djavan", coverUrl: "https://cdn-images.dzcdn.net/images/cover/abc/250x250-000000-80-0-0.jpg" });
  assert.equal(ok.coverUrl, "https://cdn-images.dzcdn.net/images/cover/abc/250x250-000000-80-0-0.jpg");
  assert.equal(mod.cleanTrack({ title: "Oceano", coverUrl: "https://example.com/x.jpg" }).coverUrl, undefined);
  assert.equal(mod.cleanTrack({ title: "Oceano", coverUrl: "http://cdn-images.dzcdn.net/x.jpg" }).coverUrl, undefined);
  assert.equal(mod.cleanTrack({ title: "Oceano", coverUrl: "https://dzcdn.net.evil.com/x.jpg" }).coverUrl, undefined);
  assert.equal(mod.cleanTrack({ title: "Oceano" }).coverUrl, undefined);
});
