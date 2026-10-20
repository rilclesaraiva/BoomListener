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

test("app antigo: mensagem vai para a caixa postal (anel) e não duplica", async () => {
  const call = server();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const sent = await call("POST", "/messages", { conv: "dm:" + beto.id, body: { type: "text", text: "oi" } }, ana);
  assert.equal(sent.status, 200);
  const inbox = (await call("GET", "/inbox", null, beto)).data.messages;
  assert.equal(inbox.length, 1);
  assert.equal(inbox[0].conv, "dm:" + ana.id);
  assert.equal(inbox[0].sender.handle, "ana");
  // O anel não apaga (apagar também conta como escrita); o app ignora repetidas pelo id.
  await call("POST", "/inbox/ack", { ids: [inbox[0].id] }, beto);
  const again = (await call("GET", "/inbox", null, beto)).data.messages;
  assert.deepEqual(again.map((m) => m.id), [inbox[0].id]);
});

test("cartão de música aceita só informação, nada de arquivo", async () => {
  const call = server();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const card = { type: "music", kind: "track", title: "Açaí", artist: "Djavan", coverUrl: "http://inseguro", audio: "AAAA" };
  await call("POST", "/messages", { conv: "dm:" + beto.id, body: card }, ana);
  const body = (await call("GET", "/inbox", null, beto)).data.messages[0].body;
  assert.equal(body.title, "Açaí");
  assert.equal(body.coverUrl, null);
  assert.equal(body.audio, undefined);
});

test("ouvindo agora só para quem conversa e com compartilhamento ligado", async () => {
  const call = server();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  await call("PATCH", "/me", { sharing: true }, ana);
  await call("PUT", "/me/now", { track: { title: "Oceano", artist: "Djavan" } }, ana);
  await call("POST", "/messages", { conv: "dm:" + ana.id, body: { type: "text", text: "e aí" } }, beto);
  assert.equal((await call("GET", "/users/by-handle/ana", null, beto)).data.now.title, "Oceano");
  assert.equal((await call("GET", "/users/by-handle/ana", null, caio)).data.now, undefined);
  assert.equal((await call("GET", "/activity", null, beto)).data.people.length, 1);
  await call("PATCH", "/me", { sharing: false }, ana);
  assert.equal((await call("GET", "/activity", null, beto)).data.people.length, 0);
});

test("grupo por código e bloqueio", async () => {
  const call = server();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  const group = (await call("POST", "/groups", { name: "Rolê" }, ana)).data;
  assert.equal(group.code.length, 8);
  await call("POST", "/groups/join", { code: group.code.toLowerCase() }, beto);
  await call("POST", "/groups/join", { code: group.code }, caio);
  await call("POST", "/blocks", { id: ana.id }, caio);
  await call("POST", "/messages", { conv: "g:" + group.id, body: { type: "text", text: "bora" } }, ana);
  assert.equal((await call("GET", "/inbox", null, beto)).data.messages.length, 1);
  assert.equal((await call("GET", "/inbox", null, caio)).data.messages.length, 0);
  assert.equal((await call("GET", "/groups/" + group.id, null, beto)).data.members.length, 3);
  const outsider = (await call("POST", "/register", { name: "Davi", handle: "davi" })).data;
  assert.equal((await call("POST", "/messages", { conv: "g:" + group.id, body: { type: "text", text: "x" } }, outsider)).status, 403);
});

test("apagar perfil apaga tudo", async () => {
  const call = server();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  await call("DELETE", "/me", null, ana);
  assert.equal((await call("GET", "/me", null, ana)).status, 401);
  assert.equal((await call("GET", "/handles/ana")).data.available, true);
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

// ---- Fase 1: WebSocket com hibernação, porteiro e cotas ----

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

test("ao conectar chega o que estava esperando, e mensagem nova chega na hora", async () => {
  const { call, connect, say } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  await call("POST", "/messages", { conv: "dm:" + beto.id, body: { type: "text", text: "antes" } }, ana);

  const betoWs = await connect(beto);
  assert.equal(betoWs.events("hello")[0].me.handle, "beto");
  assert.equal(betoWs.events("inbox")[0].messages[0].body.text, "antes");

  const anaWs = await connect(ana);
  const cid = "a".repeat(32);
  await say(anaWs, { t: "send", cid, conv: "dm:" + beto.id, body: { type: "text", text: "agora" } });
  assert.deepEqual(
    { status: anaWs.events("sent")[0].status, id: anaWs.events("sent")[0].id, cid: anaWs.events("sent")[0].cid },
    { status: 200, id: cid, cid },
  );
  const live = betoWs.events("inbox").at(-1).messages[0];
  assert.equal(live.body.text, "agora");
  assert.equal(live.id, cid);
  assert.equal(live.sender.handle, "ana");

  // Reenviar a mesma mensagem (resposta perdida) não duplica.
  await say(anaWs, { t: "send", cid, conv: "dm:" + beto.id, body: { type: "text", text: "agora" } });
  assert.equal((await call("GET", "/inbox", null, beto)).data.messages.length, 2);

  // App antigo (sem recibo): a mensagem ao vivo também fica no anel, por segurança.
  await say(betoWs, { t: "sync" });
  assert.deepEqual(betoWs.events("inbox").at(-1).messages.map((m) => m.body.text), ["antes", "agora"]);
});

test("ouvindo agora vive na conexão e os recentes são gravados ao fechar", async () => {
  const { hub, call, connect, say } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  await call("PATCH", "/me", { sharing: true }, ana);
  await call("POST", "/messages", { conv: "dm:" + ana.id, body: { type: "text", text: "oi" } }, beto);

  const betoWs = await connect(beto);
  const anaWs = await connect(ana);
  await say(anaWs, { t: "now", track: { title: "Oceano", artist: "Djavan" } });
  assert.equal(betoWs.events("person").at(-1).profile.now.title, "Oceano");
  // Mesma música de novo: ninguém é avisado.
  const before = betoWs.sent.length;
  await say(anaWs, { t: "now", track: { title: "Oceano", artist: "Djavan" } });
  assert.equal(betoWs.sent.length, before);

  await say(anaWs, { t: "listened", track: { title: "Flor de Lis", artist: "Djavan" } });
  assert.equal((await call("GET", "/users/" + ana.id, null, beto)).data.recent[0].title, "Flor de Lis");
  // Nada foi gravado ainda (está só na conexão).
  assert.equal((await call("GET", "/users/" + ana.id, null, beto)).data.now.title, "Oceano");

  await hub.webSocketClose(anaWs, 1000);
  assert.equal(betoWs.events("person").at(-1).profile.now, null);
  const after = (await call("GET", "/users/" + ana.id, null, beto)).data;
  assert.equal(after.now, null);
  assert.equal(after.recent[0].title, "Flor de Lis");

  // Quem não compartilha não manda "ouvindo agora".
  const betoNow = betoWs.sent.length;
  await say(betoWs, { t: "now", track: { title: "Segredo" } });
  assert.equal(betoWs.sent.length, betoNow);
});

test("porteiro recusa sem acordar o Hub", () => {
  const req = (method, path, headers = {}) => mod.gate(new Request("https://x" + path, { method, headers }), new URL("https://x" + path));
  const auth = { authorization: `Bearer ${"a".repeat(32)}.${"b".repeat(64)}` };
  assert.equal(req("GET", "/v1/nada", auth).status, 404);
  assert.equal(req("POST", "/v1/inbox", auth).status, 404); // método errado
  assert.equal(req("GET", "/v1/inbox").status, 401);
  assert.equal(req("GET", "/v1/ws", auth).status, 426);
  assert.equal(req("GET", "/v1/ws", { ...auth, upgrade: "websocket" }), null);
  assert.equal(req("POST", "/v1/messages", { ...auth, "content-length": "999999" }).status, 413);
  assert.equal(req("PUT", "/v1/me/image/avatar", { ...auth, "content-length": "3000000" }), null);
  assert.equal(req("GET", "/v1/handles/ana"), null);
  assert.equal(mod.nextUtcMidnight(Date.UTC(2026, 9, 20, 23, 59)), Date.UTC(2026, 9, 21));
});

test("cotas: mensagens por dia, grupo cheio e cadastros por IP", async () => {
  const { hub, call } = liveServer();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const realNow = Date.now;
  try {
    // 300 mensagens no dia (em minutos diferentes, para não cair no limite por minuto).
    let fake = Math.floor(realNow() / 86_400_000) * 86_400_000 + 1000;
    Date.now = () => fake;
    for (let i = 0; i < 300; i++) {
      fake += 2_000;
      const r = await call("POST", "/messages", { conv: "dm:" + beto.id, body: { type: "text", text: "m" + i } }, ana);
      assert.equal(r.status, 200, "mensagem " + i);
    }
    fake += 2_000;
    const blocked = await call("POST", "/messages", { conv: "dm:" + beto.id, body: { type: "text", text: "demais" } }, ana);
    assert.equal(blocked.data.error, "daily_limit");
  } finally {
    Date.now = realNow;
  }

  // 5 cadastros por hora por IP (todos vêm do mesmo "IP" no teste): ana e beto + 3.
  for (let i = 0; i < 3; i++) assert.equal((await call("POST", "/register", { name: "R", handle: "reg" + i })).status, 200);
  assert.equal((await call("POST", "/register", { name: "X", handle: "xxxx" })).status, 429);

  // Grupo de no máximo 30.
  const group = (await call("POST", "/groups", { name: "Lotado" }, ana)).data;
  for (let i = 0; i < 29; i++) {
    hub.counters.clear(); // libera o limite de cadastros por IP
    const user = (await call("POST", "/register", { name: "U" + i, handle: "user" + i })).data;
    assert.equal((await call("POST", "/groups/join", { code: group.code }, user)).status, 200);
  }
  hub.counters.clear();
  const late = (await call("POST", "/register", { name: "Late", handle: "late" })).data;
  assert.equal((await call("POST", "/groups/join", { code: group.code }, late)).data.error, "group_full");
});

// ---- Fase 3: mensagens presas no celular de quem enviou ----

function withPushes(fn) {
  return async () => {
    const pushes = [];
    const realFetch = globalThis.fetch;
    globalThis.fetch = async (url, init) => {
      pushes.push({ url, body: init?.body });
      return new Response("", { status: 200 });
    };
    try {
      await fn(pushes);
    } finally {
      globalThis.fetch = realFetch;
    }
  };
}

const PUSH_ANA = "https://ntfy.sh/upAnaTopico123";
const PUSH_BETO = "https://ntfy.sh/upBetoTopico456";

async function twoFriends(live) {
  const { call, connect, say } = live;
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  return { ana, beto, call, connect, say };
}

test("destinatário offline: só um lembrete, push, cutucão e entrega ao vivo", withPushes(async (pushes) => {
  const live = liveServer();
  const { hub } = live;
  const { ana, beto, call, connect, say } = await twoFriends(live);
  const anaWs = await connect(ana, "?v=2");
  await say(anaWs, { t: "hello", v: 2, since: 0, groups: {}, push: PUSH_ANA });
  const betoWs = await connect(beto, "?v=2");
  await say(betoWs, { t: "hello", v: 2, since: 0, groups: {}, push: PUSH_BETO });
  await hub.webSocketClose(betoWs, 1000);

  const cid1 = "1".repeat(32);
  const cid2 = "2".repeat(32);
  for (const cid of [cid1, cid2]) {
    await say(anaWs, { t: "send", v: 2, cid, at: Date.now() - 1000, conv: "dm:" + beto.id, body: { type: "text", text: cid.slice(0, 1) } });
  }
  assert.deepEqual(anaWs.events("sent").map((e) => e.how), ["waiting", "waiting"]);
  // Nada da mensagem foi gravado: só um lembrete (uma linha) com a Ana.
  assert.equal(hub.all("SELECT COUNT(*) AS n FROM ring")[0].n, 0);
  assert.deepEqual(JSON.parse(hub.first("SELECT senders FROM reminders WHERE to_user = ?", beto.id).senders), [ana.id]);
  // Beto foi acordado uma vez só (limite de 1 push a cada 2 min).
  assert.deepEqual(pushes.map((p) => p.url), [PUSH_BETO]);
  assert.equal(pushes[0].body, "1"); // o push não leva a mensagem

  // Beto conecta: o lembrete some e a Ana é cutucada pela conexão.
  const betoAgain = await connect(beto, "?v=2");
  assert.equal(betoAgain.events("inbox").length, 0); // app novo não recebe o "despejo" antigo
  await say(betoAgain, { t: "hello", v: 2, since: 0, groups: {} });
  assert.equal(hub.first("SELECT senders FROM reminders WHERE to_user = ?", beto.id), null);
  assert.deepEqual(anaWs.events("poke").map((e) => e.to), [beto.id]);

  // Ana reenvia a fila: vai ao vivo para o Beto, sem gravar.
  for (const cid of [cid1, cid2]) {
    await say(anaWs, { t: "send", v: 2, cid, conv: "dm:" + beto.id, body: { type: "text", text: cid.slice(0, 1) } });
  }
  assert.deepEqual(anaWs.events("sent").slice(2).map((e) => e.how), ["live", "live"]);
  const got = betoAgain.events("inbox").flatMap((e) => e.messages).filter((m) => m.live);
  assert.deepEqual(got.map((m) => m.id), [cid1, cid2]);
  assert.equal(hub.all("SELECT COUNT(*) AS n FROM ring")[0].n, 0);

  // Recibo "entregue" volta para a Ana pela conexão.
  await say(betoAgain, { t: "delivered", items: got.map((m) => ({ id: m.id, to: m.sender.id })) });
  assert.deepEqual(anaWs.events("delivered")[0].ids, [cid1, cid2]);
}));

test("rede de segurança: sem push, 24 h ou destinatário que sumiu vão para o anel", withPushes(async () => {
  const live = liveServer();
  const { hub } = live;
  const { ana, beto, connect, say } = await twoFriends(live);
  // Ana sem push: não dá para acordá-la depois, então vai direto para a caixa postal.
  const anaWs = await connect(ana, "?v=2");
  await say(anaWs, { t: "hello", v: 2, since: 0, groups: {} });
  await say(anaWs, { t: "send", v: 2, cid: "a".repeat(32), conv: "dm:" + beto.id, body: { type: "text", text: "oi" } });
  assert.equal(anaWs.events("sent")[0].how, "mailbox");

  // Com push, mas 24 h sem entregar: o app manda com mailbox.
  await say(anaWs, { t: "push", url: PUSH_ANA });
  await say(anaWs, { t: "send", v: 2, cid: "b".repeat(32), mailbox: true, conv: "dm:" + beto.id, body: { type: "text", text: "e aí" } });
  assert.equal(anaWs.events("sent")[1].how, "mailbox");

  // Beto lê o anel e guarda até onde leu; depois só vem o que é novo.
  const betoWs = await connect(beto, "?v=2");
  await say(betoWs, { t: "hello", v: 2, since: 0, groups: {} });
  const first = betoWs.events("inbox").at(-1).messages;
  assert.deepEqual(first.map((m) => m.body.text), ["oi", "e aí"]);
  const since = Math.max(...first.map((m) => m.seq));
  await say(betoWs, { t: "hello", v: 2, since, groups: {} });
  assert.equal(betoWs.events("inbox").at(-1).messages.length, 0);

  // Anel circular: só as 100 mais novas.
  for (let i = 0; i < 105; i++) {
    hub.ringPut("ring", "user_id", beto.id, 100, { id: i.toString(16).padStart(32, "0"), conv: "dm:" + ana.id, sent: Date.now(), body: { type: "text", text: "n" + i }, sender: { id: ana.id } });
  }
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM ring WHERE user_id = ?", beto.id).n, 100);
  await say(betoWs, { t: "hello", v: 2, since: 0, groups: {} });
  const ring = betoWs.events("inbox").at(-1).messages;
  assert.equal(ring.at(-1).body.text, "n104");
  assert.ok(!ring.some((m) => m.body.text === "n0"));
}));

test("cutucado e o destinatário sumiu de novo: vai para o anel", withPushes(async () => {
  const live = liveServer();
  const { hub } = live;
  const { ana, beto, connect, say } = await twoFriends(live);
  const anaWs = await connect(ana, "?v=2");
  await say(anaWs, { t: "hello", v: 2, since: 0, groups: {}, push: PUSH_ANA });
  await say(anaWs, { t: "send", v: 2, cid: "c".repeat(32), conv: "dm:" + beto.id, body: { type: "text", text: "1" } });
  const betoWs = await connect(beto, "?v=2");
  await say(betoWs, { t: "hello", v: 2, since: 0, groups: {} });
  await hub.webSocketClose(betoWs, 1000);
  await say(anaWs, { t: "send", v: 2, cid: "c".repeat(32), conv: "dm:" + beto.id, body: { type: "text", text: "1" } });
  assert.equal(anaWs.events("sent").at(-1).how, "mailbox");
}));

test("grupo: uma escrita por mensagem, ao vivo para quem está online e cursor por grupo", withPushes(async (pushes) => {
  const live = liveServer();
  const { hub } = live;
  const { ana, beto, call, connect, say } = await twoFriends(live);
  const caio = (await call("POST", "/register", { name: "Caio", handle: "caio" })).data;
  const group = (await call("POST", "/groups", { name: "Rolê" }, ana)).data;
  await call("POST", "/groups/join", { code: group.code }, beto);
  await call("POST", "/groups/join", { code: group.code }, caio);
  const betoWs = await connect(beto, "?v=2");
  await say(betoWs, { t: "hello", v: 2, since: 0, groups: {} });
  const caioWs = await connect(caio, "?v=2");
  await say(caioWs, { t: "hello", v: 2, since: 0, groups: {}, push: "https://ntfy.sh/upCaio789" });
  await hub.webSocketClose(caioWs, 1000);

  const anaWs = await connect(ana, "?v=2");
  await say(anaWs, { t: "send", v: 2, cid: "d".repeat(32), conv: "g:" + group.id, body: { type: "text", text: "bora" } });
  assert.equal(anaWs.events("sent")[0].how, "group");
  assert.equal(hub.first("SELECT COUNT(*) AS n FROM group_ring").n, 1); // uma escrita, não uma por membro
  assert.equal(betoWs.events("inbox").at(-1).messages[0].body.text, "bora");
  assert.deepEqual(pushes.map((p) => p.url), ["https://ntfy.sh/upCaio789"]); // Caio offline foi acordado

  const caioAgain = await connect(caio, "?v=2");
  await say(caioAgain, { t: "hello", v: 2, since: 0, groups: {} });
  const got = caioAgain.events("inbox").at(-1).messages;
  assert.equal(got[0].body.text, "bora");
  assert.equal(got[0].group, group.id);
  await say(caioAgain, { t: "hello", v: 2, since: 0, groups: { [group.id]: got[0].seq } });
  assert.equal(caioAgain.events("inbox").at(-1).messages.length, 0);
}));

test("endereço de push: só https e gravado só quando muda", withPushes(async () => {
  const live = liveServer();
  const { hub } = live;
  const { ana, connect, say } = await twoFriends(live);
  const ws = await connect(ana, "?v=2");
  await say(ws, { t: "push", url: "http://inseguro.com/x" });
  assert.equal(hub.first("SELECT push_url FROM users WHERE id = ?", ana.id).push_url, "");
  await say(ws, { t: "push", url: PUSH_ANA });
  assert.equal(hub.first("SELECT push_url FROM users WHERE id = ?", ana.id).push_url, PUSH_ANA);
  await say(ws, { t: "push", url: "" });
  assert.equal(hub.first("SELECT push_url FROM users WHERE id = ?", ana.id).push_url, "");
}));

test("fase 2: WebP aceito, versão pelo conteúdo e perfil com número de versão", async () => {
  const { hub, call, connect, say } = liveServer();
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
  // A mesma imagem de novo: mesma versão de imagem (o cache do app continua valendo).
  assert.equal((await put(webp)).avatar, first.avatar);
  const image = await hub.fetch(new Request(`https://x/v1/users/${ana.id}/image/avatar`, {
    headers: { authorization: `Bearer ${beto.id}.${beto.token}` },
  }));
  assert.equal(image.headers.get("content-type"), "image/webp");

  // Editar o perfil sobe a versão; ligar o compartilhamento (não é perfil) não.
  const renamed = (await call("PATCH", "/me", { name: "Ana Clara" }, ana)).data;
  assert.equal(renamed.v, 3);
  assert.equal((await call("PATCH", "/me", { sharing: true }, ana)).data.v, 3);

  // Ao conectar, o app recebe as versões de quem conhece.
  await call("POST", "/messages", { conv: "dm:" + ana.id, body: { type: "text", text: "oi" } }, beto);
  const ws = await connect(beto, "?v=2");
  await say(ws, { t: "hello", v: 2, since: 0, groups: {} });
  assert.deepEqual(ws.events("versions")[0].users, { [ana.id]: 3 });
});
