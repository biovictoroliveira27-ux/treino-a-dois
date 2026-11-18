// Gera os takes em MP4 a partir de syntax-takes.html (quadro a quadro, 30 fps, 1080x1920).
// Antes: python3 montar.py (gera syntax-takes.html a partir do .src.html)
// Outro arquivo: --src matriculas-takes (saída em takes-matriculas/)
// Opções: --query fundo=roxo (parâmetros da página)  --alpha (.mov com transparência)  --out arquivo
// Uso: node render.mjs                     -> takes/<n>-<nome>.mp4 (um arquivo por cena) + syntax-takes.mp4 (todos em sequência)
//      node render.mjs --stills 1,4.2 dir  -> apenas quadros de teste (PNG)
import { createRequire } from 'node:module';
import { spawn, execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
let pw;
try { pw = require('playwright'); } catch { pw = require(path.join(execSync('npm root -g').toString().trim(), 'playwright')); }
const { chromium } = pw;
const FPS = 30;
const args = process.argv.slice(2);
let nome = 'syntax-takes';
const si = args.indexOf('--src');
if (si >= 0) { nome = args[si + 1].replace(/\.html$/, ''); args.splice(si, 2); }
let query = '', alpha = false, saida = null;
const qi = args.indexOf('--query'); if (qi >= 0) { query = '&' + args[qi + 1]; args.splice(qi, 2); }
const ai = args.indexOf('--alpha'); if (ai >= 0) { alpha = true; args.splice(ai, 1); }
const oi = args.indexOf('--out'); if (oi >= 0) { saida = args[oi + 1]; args.splice(oi, 2); }
const pasta = nome === 'syntax-takes' ? 'takes' : 'takes-' + nome.replace(/-takes$/, '');

const browser = await chromium.launch(fs.existsSync('/opt/pw-browsers/chromium') ? { executablePath: '/opt/pw-browsers/chromium' } : {});
const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
await page.goto('file://' + path.resolve(here, nome + '.html') + '?render=1' + query);
await page.evaluate(() => PRONTO_TUDO);
const DUR = await page.evaluate(() => DUR);
const CENAS = await page.evaluate(() => CENAS.map(c => ({ nome: c.nome, a: c.a, d: c.d })));
const frame = (t, type = 'image/jpeg') => page.evaluate(([t, type]) => { render(t); return document.getElementById('c').toDataURL(type, 0.95).split(',')[1]; }, [t, type]);

async function video(out, a, d) {
  // --alpha: quadros PNG -> .mov ProRes 4444 com transparência (CapCut/Premiere aceitam como sobreposição)
  const enc = alpha ? ['-c:v', 'prores_ks', '-profile:v', '4444', '-pix_fmt', 'yuva444p10le', '-vendor', 'apl0']
                    : ['-c:v', 'libx264', '-preset', 'slow', '-crf', '17', '-pix_fmt', 'yuv420p', '-movflags', '+faststart'];
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', alpha ? 'png' : 'mjpeg', '-i', '-',
    ...enc, '-r', String(FPS), out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const n = Math.round(FPS * d);
  for (let i = 0; i < n; i++) {
    const buf = Buffer.from(await frame(a + Math.min(i / FPS, d - 1e-3), alpha ? 'image/png' : 'image/jpeg'), 'base64');
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await new Promise((res, rej) => ff.on('close', c => c ? rej(new Error('ffmpeg ' + c)) : res()));
  console.log('ok:', out);
}

if (args[0] === '--stills') {
  const dir = args[2] || here;
  fs.mkdirSync(dir, { recursive: true });
  for (const t of args[1].split(',').map(Number)) fs.writeFileSync(path.join(dir, `still_${t.toFixed(2)}.png`), Buffer.from(await frame(t, 'image/png'), 'base64'));
} else {
  if (saida) { await video(path.resolve(here, saida), 0, DUR); await browser.close(); process.exit(0); }
  fs.mkdirSync(path.join(here, pasta), { recursive: true });
  for (const c of CENAS) await video(path.join(here, pasta, c.nome + '.mp4'), c.a, c.d);
  await video(path.join(here, nome + '.mp4'), 0, DUR);
}
await browser.close();
