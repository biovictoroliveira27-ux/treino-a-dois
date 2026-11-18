# Gera syntax-takes.html a partir de syntax-takes.src.html, embutindo fontes (fontes/) e a logo fatiada (logo/).
# Uso: python3 montar.py [syntax-takes|matriculas-takes]
import base64, json, re, pathlib, sys
nome = sys.argv[1] if len(sys.argv) > 1 else 'syntax-takes'
aqui = pathlib.Path(__file__).parent
b64 = lambda p: base64.b64encode(p.read_bytes()).decode()
css = []
for f in sorted((aqui / 'fontes').glob('*.woff2')):
    fam, _, peso, estilo = re.match(r'(\w+)-latin-(\w+-)?(\d+)-(\w+)', f.stem).groups()[0:4] if False else (None,)*4
    m = re.match(r'(\w+)-latin-(\d+)-(\w+)', f.stem); fam, peso, estilo = m.group(1).capitalize(), m.group(2), m.group(3)
    css.append(f"@font-face{{font-family:'{fam}';font-weight:{peso};font-style:{estilo};src:url(data:font/woff2;base64,{b64(f)}) format('woff2');}}")
meta = json.loads((aqui / 'logo' / 'logo.json').read_text())
for p in meta['parts']: p['data'] = 'data:image/png;base64,' + b64(aqui / 'logo' / p['src'])
src = (aqui / f'{nome}.src.html').read_text()
src = src.replace('/*@FONTES@*/', '\n'.join(css)).replace('/*@LOGO@*/', 'const LOGO = ' + json.dumps(meta) + ';')
(aqui / f'{nome}.html').write_text(src)
print(f'ok: {nome}.html', round(len(src) / 1024), 'KB')
