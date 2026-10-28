// Signs a resource description with the fixed test keys of digest D22 and D23 (no dependencies).
// Usage: node sign-rd.mjs <key-name> < unsigned.json > signed.json
//        node sign-rd.mjs --did <key-name>      prints the did:jwk of the key
import crypto from 'node:crypto';
import fs from 'node:fs';

const PKCS8_ED25519 = Buffer.from('302e020100300506032b657004220420', 'hex');
const b64u = (buf) => Buffer.from(buf).toString('base64url');

export function keyFor(name) {
  const seed = crypto.createHash('sha256').update(`simpl-catalogue-factory/${name}`, 'ascii').digest();
  const priv = crypto.createPrivateKey({ key: Buffer.concat([PKCS8_ED25519, seed]), format: 'der', type: 'pkcs8' });
  const x = crypto.createPublicKey(priv).export({ format: 'jwk' }).x;
  const did = `did:jwk:${b64u(JSON.stringify({ crv: 'Ed25519', kty: 'OKP', x }))}`;
  return { priv, x, did };
}

// RFC 8785: sorted keys (UTF-16 code units), ECMAScript number and string serialisation.
export function jcs(v) {
  if (v === null || typeof v !== 'object') return JSON.stringify(v);
  if (Array.isArray(v)) return `[${v.map(jcs).join(',')}]`;
  return `{${Object.keys(v).sort().map((k) => `${JSON.stringify(k)}:${jcs(v[k])}`).join(',')}}`;
}

export function sign(vc, name) {
  const { priv, did } = keyFor(name);
  const unsigned = { ...vc };
  delete unsigned.proof;
  unsigned.issuer = unsigned.issuer || did;
  const header = b64u(JSON.stringify({ alg: 'EdDSA', b64: false, crit: ['b64'] }));
  const input = Buffer.concat([Buffer.from(`${header}.`, 'ascii'), Buffer.from(jcs(unsigned), 'utf8')]);
  const sig = crypto.sign(null, input, priv);
  return {
    ...unsigned,
    proof: {
      type: 'JsonWebSignature2020', proofPurpose: 'assertionMethod', created: new Date().toISOString(),
      verificationMethod: `${unsigned.issuer}#0`, jws: `${header}..${b64u(sig)}`,
    },
  };
}

if (process.argv[1] && process.argv[1].endsWith('sign-rd.mjs')) {
  if (process.argv[2] === '--did') {
    process.stdout.write(keyFor(process.argv[3]).did);
  } else if (process.argv[2] === '--x') {
    process.stdout.write(keyFor(process.argv[3]).x);
  } else {
    const vc = JSON.parse(fs.readFileSync(0, 'utf8'));
    process.stdout.write(JSON.stringify(sign(vc, process.argv[2])));
  }
}
