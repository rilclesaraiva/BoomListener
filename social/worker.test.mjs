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

test("mensagem particular chega, confirma e some do servidor", async () => {
  const call = server();
  const ana = (await call("POST", "/register", { name: "Ana", handle: "ana" })).data;
  const beto = (await call("POST", "/register", { name: "Beto", handle: "beto" })).data;
  const sent = await call("POST", "/messages", { conv: "dm:" + beto.id, body: { type: "text", text: "oi" } }, ana);
  assert.equal(sent.status, 200);
  const inbox = (await call("GET", "/inbox", null, beto)).data.messages;
  assert.equal(inbox.length, 1);
  assert.equal(inbox[0].conv, "dm:" + ana.id);
  assert.equal(inbox[0].sender.handle, "ana");
  await call("POST", "/inbox/ack", { ids: [inbox[0].id] }, beto);
  assert.equal((await call("GET", "/inbox", null, beto)).data.messages.length, 0);
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
