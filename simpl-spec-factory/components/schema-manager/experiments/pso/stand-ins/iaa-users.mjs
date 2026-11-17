// Environment stand-in (4 Nov 2026) for the IAA Users and Roles API of the governance authority:
// POST /userApi/tier1/v2/users creates the Tier 1 user in the build's Keycloak (realm simpl), with
// the realm roles of the request that the realm knows. Roles the realm does not have (SIMPL_USER)
// are left out. It needs a bearer token and does not check it. Nothing else of IAA is there.
// Env: KC_URL, KC_REALM, KC_ADMIN_USER, KC_ADMIN_PASSWORD (the throwaway admin of the build's compose).
import http from 'node:http';

const { KC_URL = 'http://keycloak:8080', KC_REALM = 'simpl', KC_ADMIN_USER, KC_ADMIN_PASSWORD } = process.env;

async function adminToken() {
  const r = await fetch(`${KC_URL}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'password', client_id: 'admin-cli', username: KC_ADMIN_USER, password: KC_ADMIN_PASSWORD }),
  });
  if (!r.ok) throw new Error(`admin token: ${r.status}`);
  return (await r.json()).access_token;
}

async function createUser(u) {
  const t = await adminToken();
  const h = { authorization: `Bearer ${t}`, 'content-type': 'application/json' };
  const base = `${KC_URL}/admin/realms/${KC_REALM}`;
  const r = await fetch(`${base}/users`, {
    method: 'POST', headers: h,
    body: JSON.stringify({
      username: u.username, email: u.email, firstName: u.firstName, lastName: u.lastName,
      enabled: u.enabled !== false, emailVerified: true,
      credentials: [{ type: 'password', value: u.password, temporary: false }],
    }),
  });
  if (r.status !== 201) return { status: r.status, body: await r.text() };
  const id = r.headers.get('location').split('/').pop();
  const known = await (await fetch(`${base}/roles`, { headers: h })).json();
  const roles = known.filter((k) => (u.roles || []).includes(k.name));
  if (roles.length) {
    await fetch(`${base}/users/${id}/role-mappings/realm`, { method: 'POST', headers: h, body: JSON.stringify(roles) });
  }
  return { status: 201, body: JSON.stringify({ id, username: u.username, roles: roles.map((x) => x.name) }) };
}

http.createServer((req, res) => {
  let data = '';
  req.on('data', (c) => { data += c; });
  req.on('end', async () => {
    const send = (s, b) => { res.writeHead(s, { 'content-type': 'application/json' }); res.end(b); };
    if (req.url === '/health') return send(200, '{"status":"UP"}');
    if (req.method !== 'POST' || req.url.split('?')[0] !== '/userApi/tier1/v2/users') return send(404, '{"status":404}');
    if (!/^Bearer /i.test(req.headers.authorization || '')) return send(401, '{"status":401}');
    try {
      const out = await createUser(JSON.parse(data || '{}'));
      send(out.status, out.body);
    } catch (e) {
      send(502, JSON.stringify({ status: 502, detail: String(e.message) }));
    }
  });
}).listen(8080);
