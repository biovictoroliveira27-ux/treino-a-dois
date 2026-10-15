// Gera o vídeo MP4 a partir de hiv-cd4.html (quadro a quadro, 30 fps).
// Uso: node render.mjs [saida.mp4]            -> vídeo completo
//      node render.mjs --stills 1,4.2,10 dir  -> apenas quadros de teste (PNG)
import { createRequire } from 'node:module';
import { spawn, execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';

const here = path.dirname(fileURLToPath(import.meta.url));
// playwright local ou instalado globalmente (npm i -g playwright)
const require = createRequire(import.meta.url);
let pw;
try { pw = require('playwright'); } catch { pw = require(path.join(execSync('npm root -g').toString().trim(), 'playwright')); }
const { chromium } = pw;
const FPS = 30, DUR = 20;
const args = process.argv.slice(2);

const browser = await chromium.launch(fs.existsSync('/opt/pw-browsers/chromium') ? { executablePath: '/opt/pw-browsers/chromium' } : {});
const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
await page.goto('file://' + path.join(here, 'hiv-cd4.html') + '?render=1');
await page.evaluate(() => document.fonts.ready);
const frame = (t, type = 'image/jpeg') => page.evaluate(([t, type]) => { render(t); return document.getElementById('c').toDataURL(type, 0.95).split(',')[1]; }, [t, type]);

if (args[0] === '--stills') {
  const dir = args[2] || here;
  fs.mkdirSync(dir, { recursive: true });
  for (const t of args[1].split(',').map(Number)) fs.writeFileSync(path.join(dir, `still_${t.toFixed(2)}.png`), Buffer.from(await frame(t, 'image/png'), 'base64'));
} else {
  const out = args[0] || path.join(here, 'hiv-cd4.mp4');
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  for (let i = 0; i < FPS * DUR; i++) {
    const buf = Buffer.from(await frame(i / FPS), 'base64');
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await new Promise((res, rej) => ff.on('close', c => c ? rej(new Error('ffmpeg ' + c)) : res()));
  console.log('ok:', out);
}
await browser.close();
