// Gera os takes em MP4 a partir de syntax-takes.html (quadro a quadro, 30 fps, 1080x1920).
// Antes: python3 montar.py (gera syntax-takes.html a partir do .src.html)
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

const browser = await chromium.launch(fs.existsSync('/opt/pw-browsers/chromium') ? { executablePath: '/opt/pw-browsers/chromium' } : {});
const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
await page.goto('file://' + path.resolve(here, 'syntax-takes.html') + '?render=1');
await page.evaluate(() => PRONTO_TUDO);
const DUR = await page.evaluate(() => DUR);
const CENAS = await page.evaluate(() => CENAS.map(c => ({ nome: c.nome, a: c.a, d: c.d })));
const frame = (t, type = 'image/jpeg') => page.evaluate(([t, type]) => { render(t); return document.getElementById('c').toDataURL(type, 0.95).split(',')[1]; }, [t, type]);

async function video(out, a, d) {
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '17', '-pix_fmt', 'yuv420p', '-r', String(FPS), '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const n = Math.round(FPS * d);
  for (let i = 0; i < n; i++) {
    const buf = Buffer.from(await frame(a + Math.min(i / FPS, d - 1e-3)), 'base64');
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
  fs.mkdirSync(path.join(here, 'takes'), { recursive: true });
  for (const c of CENAS) await video(path.join(here, 'takes', c.nome + '.mp4'), c.a, c.d);
  await video(path.join(here, 'syntax-takes.mp4'), 0, DUR);
}
await browser.close();
