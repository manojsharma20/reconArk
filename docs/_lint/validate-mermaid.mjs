// Usage: node validate-mermaid.mjs <file-or-dir>...
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';
import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body></body></html>', { pretendToBeVisual: true });
globalThis.window = dom.window;
globalThis.document = dom.window.document;
globalThis.DOMParser = dom.window.DOMParser;
globalThis.Element = dom.window.Element;
const { default: mermaid } = await import('mermaid');
mermaid.initialize({ startOnLoad: false });

function* files(p) {
  const st = statSync(p);
  if (st.isDirectory()) {
    for (const f of readdirSync(p)) if (f !== 'node_modules') yield* files(join(p, f));
  } else if (['.md', '.mmd'].includes(extname(p))) yield p;
}

function* diagrams(file) {
  const text = readFileSync(file, 'utf8');
  if (file.endsWith('.mmd')) { yield { where: file, src: text }; return; }
  const re = /```mermaid\n([\s\S]*?)```/g;
  let m, i = 0;
  while ((m = re.exec(text))) yield { where: `${file} [block ${++i}]`, src: m[1] };
}

let ok = 0, bad = 0;
for (const root of process.argv.slice(2)) {
  for (const f of files(root)) {
    for (const d of diagrams(f)) {
      try { await mermaid.parse(d.src); ok++; }
      catch (e) { bad++; console.error(`FAIL ${d.where}\n  ${String(e.message ?? e).split('\n').slice(0, 3).join('\n  ')}`); }
    }
  }
}
console.log(`mermaid: ${ok} ok, ${bad} failed`);
process.exit(bad ? 1 : 0);
