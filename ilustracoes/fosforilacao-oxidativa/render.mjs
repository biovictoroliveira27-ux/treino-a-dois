// Converte o SVG em PNG 3200×2000 com o Chromium do Playwright.
import { createRequire } from 'node:module';
import { execSync } from 'node:child_process';
import path from 'node:path'; import fs from 'node:fs'; import { fileURLToPath } from 'node:url';
const here = path.dirname(fileURLToPath(import.meta.url)), require = createRequire(import.meta.url);
let pw; try { pw = require('playwright'); } catch { pw = require(path.join(execSync('npm root -g').toString().trim(), 'playwright')); }
const b = await pw.chromium.launch(fs.existsSync('/opt/pw-browsers/chromium') ? { executablePath: '/opt/pw-browsers/chromium' } : {});
const p = await b.newPage({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 2 });
await p.goto('file://' + path.join(here, 'fosforilacao-oxidativa.svg'));
await p.screenshot({ path: path.join(here, 'fosforilacao-oxidativa.png') });
await b.close();
