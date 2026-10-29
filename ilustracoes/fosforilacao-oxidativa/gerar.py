# Gera fosforilacao-oxidativa.svg — versão didática (ensino médio):
# cadeia respiratória simplificada + ATP sintase detalhada, com passos numerados.
# PNG: node render.mjs
import math, random, os
random.seed(7)
W, H = 1600, 1000
MT, MB = 370, 470                     # membrana interna (topo / base)
out = []
A = out.append
FONT = "font-family='Inter, DejaVu Sans, Arial, sans-serif'"

def text(x, y, s, size=16, fill='#e8eef8', weight=600, anchor='middle', extra=''):
    A(f"<text x='{x}' y='{y}' {FONT} font-size='{size}' font-weight='{weight}' fill='{fill}' text-anchor='{anchor}' {extra}>{s}</text>")

def lines(x, y, rows, size=16, fill='#e8eef8', weight=600, anchor='middle', lh=1.3):
    for i, r in enumerate(rows):
        text(x, y + i * size * lh, r, size, fill, weight, anchor)

def tag(x, y, rows, col, size=16, weight=700, bg='#0b1424'):
    if isinstance(rows, str): rows = [rows]
    w = max(len(r) for r in rows) * size * 0.56 + 26
    h = len(rows) * size * 1.3 + 12
    A(f"<rect x='{x-w/2:.1f}' y='{y-size-4:.1f}' width='{w:.1f}' height='{h:.1f}' rx='10' fill='{bg}' fill-opacity='.9' stroke='{col}' stroke-width='1.6'/>")
    lines(x, y + 2, rows, size, col, weight)

def leader(x1, y1, x2, y2, col='#e8eef8'):
    A(f"<line x1='{x1}' y1='{y1}' x2='{x2}' y2='{y2}' stroke='{col}' stroke-width='1.6' stroke-opacity='.8'/>")
    A(f"<circle cx='{x2}' cy='{y2}' r='4' fill='{col}'/>")

def hplus(x, y, r=14, op=1):
    A(f"<g opacity='{op}'><circle cx='{x}' cy='{y}' r='{r}' fill='url(#gH)' stroke='#9a5a00' stroke-width='1'/>"
      f"<text x='{x}' y='{y + r*0.36:.1f}' {FONT} font-size='{r*0.98:.1f}' font-weight='800' fill='#3b1d00' text-anchor='middle'>H⁺</text></g>")

def badge(x, y, n):
    A(f"<g filter='url(#sh)'><circle cx='{x}' cy='{y}' r='19' fill='#ff4d6d' stroke='#fff' stroke-width='2.5'/></g>")
    text(x, y + 7, str(n), 20, '#ffffff', 800)

def phosphate(x, y, r=11):
    A(f"<circle cx='{x}' cy='{y}' r='{r}' fill='url(#gP)' stroke='#7c2d12' stroke-width='1.2'/>")
    text(x, y + 4.5, 'P', 12, '#3b1300', 800)

def nucleotide(x, y, nP):
    """Ícone: adenosina (A) + nP fosfatos encadeados."""
    xs = [x + 30 + 25 * i for i in range(nP)]
    A(f"<line x1='{x}' y1='{y}' x2='{xs[-1]}' y2='{y}' stroke='#e2e8f0' stroke-width='3'/>")
    if nP == 3:   # ligação rica em energia
        A(f"<path d='M{xs[1]+6},{y-9} q 6,9 0,18' stroke='#fde047' stroke-width='2.5' fill='none'/>")
    A(f"<circle cx='{x}' cy='{y}' r='17' fill='url(#gA)' stroke='#312e81' stroke-width='1.4'/>")
    text(x, y + 5.5, 'A', 16, '#ffffff', 800)
    for px in xs: phosphate(px, y)

# ---------------- definições ----------------
A(f"<svg xmlns='http://www.w3.org/2000/svg' width='{W}' height='{H}' viewBox='0 0 {W} {H}'>")
A("<defs>")
A("""<linearGradient id='gIMS' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='#26142e'/><stop offset='1' stop-color='#3d1a32'/></linearGradient>
<linearGradient id='gMat' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='#0f2b40'/><stop offset='1' stop-color='#0a1b2c'/></linearGradient>
<radialGradient id='gH' cx='.35' cy='.32'><stop offset='0' stop-color='#fff4c9'/><stop offset='.55' stop-color='#ffc04d'/><stop offset='1' stop-color='#e58e00'/></radialGradient>
<radialGradient id='gP' cx='.35' cy='.32'><stop offset='0' stop-color='#fed7aa'/><stop offset='1' stop-color='#f97316'/></radialGradient>
<radialGradient id='gA' cx='.35' cy='.32'><stop offset='0' stop-color='#a5b4fc'/><stop offset='1' stop-color='#4f46e5'/></radialGradient>
<linearGradient id='gRing' x1='0' y1='0' x2='1' y2='0'><stop offset='0' stop-color='#b45309'/><stop offset='.35' stop-color='#fcd34d'/><stop offset='.6' stop-color='#f59e0b'/><stop offset='1' stop-color='#92400e'/></linearGradient>
<linearGradient id='gRod' x1='0' y1='0' x2='1' y2='0'><stop offset='0' stop-color='#b45309'/><stop offset='.45' stop-color='#fde68a'/><stop offset='1' stop-color='#b45309'/></linearGradient>
<linearGradient id='gStat' x1='0' y1='0' x2='1' y2='0'><stop offset='0' stop-color='#0f766e'/><stop offset='.45' stop-color='#5eead4'/><stop offset='1' stop-color='#0f766e'/></linearGradient>
<marker id='arH' viewBox='0 0 10 10' refX='5' refY='5' markerWidth='4' markerHeight='4' orient='auto-start-reverse'><path d='M0,0 L10,5 L0,10 z' fill='#ffa733'/></marker>
<marker id='arE' viewBox='0 0 10 10' refX='5' refY='5' markerWidth='4.5' markerHeight='4.5' orient='auto-start-reverse'><path d='M0,0 L10,5 L0,10 z' fill='#ffe14d'/></marker>
<marker id='arW' viewBox='0 0 10 10' refX='5' refY='5' markerWidth='5' markerHeight='5' orient='auto-start-reverse'><path d='M0,0 L10,5 L0,10 z' fill='#f1f5f9'/></marker>
<filter id='sh' x='-30%' y='-30%' width='160%' height='160%'><feDropShadow dx='0' dy='5' stdDeviation='6' flood-color='#000' flood-opacity='.5'/></filter>
<filter id='glow' x='-50%' y='-50%' width='200%' height='200%'><feGaussianBlur stdDeviation='6'/></filter>
""")
def rgrad(id, c1, c2):
    A(f"<radialGradient id='{id}' cx='.32' cy='.28' r='.95'><stop offset='0' stop-color='{c1}'/><stop offset='1' stop-color='{c2}'/></radialGradient>")
rgrad('g1', '#93c5fd', '#2563eb'); rgrad('g2', '#86efac', '#16a34a'); rgrad('g3', '#d8b4fe', '#7c3aed')
rgrad('g4', '#fdba74', '#ea580c'); rgrad('gc', '#fca5a5', '#dc2626'); rgrad('gQ', '#fef08a', '#ca8a04')
rgrad('gAlpha', '#fde2e8', '#f08ca4'); rgrad('gBeta', '#fb9fb4', '#d4335a'); rgrad('gSub', '#99f6e4', '#0d9488')
A("</defs>")

# ---------------- fundo ----------------
A(f"<rect width='{W}' height='{H}' fill='#081221'/>")
A(f"<rect y='88' width='{W}' height='{MT-88}' fill='url(#gIMS)'/>")
A(f"<rect y='{MB}' width='{W}' height='{800-MB}' fill='url(#gMat)'/>")
A(f"<rect y='{MT}' width='{W}' height='{MB-MT}' fill='#3a2a1b'/>")
for x in range(-4, W + 10, 15):
    for (yh, d) in ((MT + 9, 1), (MB - 9, -1)):
        A(f"<path d='M{x-2.5},{yh} q -2,{d*20} 1,{d*39} M{x+2.5},{yh} q 2,{d*20} -1,{d*39}' stroke='#c79a55' stroke-opacity='.5' stroke-width='1.6' fill='none'/>")
        A(f"<circle cx='{x}' cy='{yh}' r='6.6' fill='#f3e3b3'/>")

# título
text(800, 44, 'Respiração celular: cadeia respiratória e ATP sintase', 32, '#ffffff', 800)
text(800, 74, 'Tudo acontece na membrana interna da mitocôndria', 17, '#cbd5e1', 500)

# mini mitocôndria (localização)
A("<g transform='translate(24,100)'>")
A("<rect width='176' height='112' rx='14' fill='#0b1424' fill-opacity='.9' stroke='#334155'/>")
A("<ellipse cx='88' cy='50' rx='70' ry='34' fill='#fb923c' stroke='#fed7aa' stroke-width='3'/>")
A("<path d='M30,50 q8,-22 16,0 q8,22 16,0 q8,-22 16,0 q8,22 16,0 q8,-22 16,0 q8,22 16,0 q8,-22 14,0' stroke='#7c2d12' stroke-width='3.5' fill='none'/>")
A("<rect x='92' y='20' width='26' height='26' fill='none' stroke='#ffffff' stroke-width='2.5' stroke-dasharray='4 3'/>")
text(88, 102, 'mitocôndria (zoom)', 13, '#cbd5e1', 600)
A("</g>")

# regiões
text(222, 128, 'ESPAÇO INTERMEMBRANAS', 19, '#f9a8d4', 800, 'start')
text(222, 152, 'muitos H⁺ acumulados', 16, '#fbcfe8', 600, 'start')
text(24, 762, 'MATRIZ MITOCONDRIAL', 19, '#7dd3fc', 800, 'start')
text(24, 786, 'poucos H⁺', 16, '#bae6fd', 600, 'start')
A(f"<rect x='8' y='{MT+24}' width='118' height='54' rx='10' fill='#0b1424' fill-opacity='.88'/>")
lines(67, MT + 46, ['Membrana', 'interna'], 15, '#f3e3b3', 700, lh=1.2)

# H+ no espaço intermembranas (muitos) e na matriz (poucos)
KEEP = [(0, 88, 520, 230), (370, 240, 575, 350), (140, 240, 330, 340), (540, 230, 900, 350), (900, 230, 1010, 350), (1020, 88, 1600, 360)]
pts = []
while len(pts) < 30:
    x, y = random.uniform(30, 1010), random.uniform(170, 345)
    if any(a <= x <= c and b <= y <= d for a, b, c, d in KEEP): continue
    if any(math.hypot(x - p, y - q) < 34 for p, q in pts): continue
    pts.append((x, y))
for x, y in pts: hplus(round(x), round(y), 13, .8)
for x, y in [(70, 560), (470, 720), (720, 650), (1000, 520)]: hplus(x, y, 13, .55)

# ---------------- bombeamento de H+ (atrás das proteínas) ----------------
def pump(x, top=250):
    A(f"<line x1='{x}' y1='{MB+90}' x2='{x}' y2='{top}' stroke='#ffa733' stroke-width='11' stroke-linecap='round' marker-end='url(#arH)' opacity='.95'/>")
pump(222); pump(612); pump(935)
for (x, y) in [(222, 222), (612, 222), (935, 222)]:
    hplus(x - 20, y, 14); hplus(x + 18, y - 8, 14)

# ---------------- complexos ----------------
def complex_shape(d, grad, label, name, lx, ly, light=True):
    A(f"<g filter='url(#sh)'><path d='{d}' fill='url(#{grad})' stroke='#ffffff' stroke-opacity='.35' stroke-width='2'/></g>")
    text(lx, ly, label, 44, '#ffffff', 800, extra="stroke='#0b1424' stroke-opacity='.25' stroke-width='1'")
    lines(lx, ly + 26, name, 14, '#ffffff', 700, lh=1.15)

complex_shape("M140,345 Q140,322 165,322 L290,322 Q308,322 308,345 L308,600 Q308,628 280,628 L208,628 Q182,628 182,600 L182,486 L160,486 Q140,486 140,462 Z",
              'g1', 'I', ['Complexo I'], 228, 430)
complex_shape("M338,452 Q338,432 362,432 L418,432 Q440,432 440,452 L440,588 Q440,612 414,612 L364,612 Q338,612 338,588 Z",
              'g2', 'II', ['Complexo', 'II'], 378, 500)
complex_shape("M532,342 Q532,312 562,312 L660,312 Q690,312 690,342 L690,552 Q690,582 660,582 L562,582 Q532,582 532,552 Z",
              'g3', 'III', ['Complexo III'], 611, 470)
complex_shape("M856,342 Q856,312 886,312 L986,312 Q1014,312 1014,342 L1014,560 Q1014,592 984,592 L886,592 Q856,592 856,560 Z",
              'g4', 'IV', ['Complexo IV'], 948, 470)

# transportadores
A("<g filter='url(#sh)'><circle cx='487' cy='420' r='24' fill='url(#gQ)' stroke='#713f12' stroke-width='2'/></g>")
text(487, 427, 'Q', 20, '#422006', 800)
tag(470, 292, ['Ubiquinona (Q)', 'transportador'], '#fde047', 14)
leader(470, 312, 485, 394, '#fde047')
A("<g filter='url(#sh)'><circle cx='772' cy='326' r='28' fill='url(#gc)' stroke='#7f1d1d' stroke-width='2'/></g>")
text(772, 332, 'c', 20, '#ffffff', 800)
tag(772, 258, ['Citocromo c', 'transportador'], '#fca5a5', 14)

# ---------------- caminho dos elétrons ----------------
def epath(d):
    A(f"<path d='{d}' stroke='#0b1424' stroke-width='9' stroke-linecap='round' fill='none' opacity='.5'/>")
    A(f"<path d='{d}' stroke='#ffe14d' stroke-width='4.5' stroke-dasharray='10 7' stroke-linecap='round' fill='none' marker-end='url(#arE)'/>")
epath("M290,676 L290,470 C290,420 350,396 460,414")                       # NADH → I → Q
epath("M422,676 L422,470 C422,450 440,440 464,434")                       # FADH2 → II → Q
epath("M513,420 C560,420 590,420 605,392 C622,350 700,330 740,327")       # Q → III → cit c
epath("M803,326 C840,326 872,345 872,390 L872,640")                       # cit c → IV → O2
def ebead(x, y):
    A(f"<circle cx='{x}' cy='{y}' r='13' fill='#ffe14d' stroke='#713f12' stroke-width='1.5'/>")
    text(x, y + 5, 'e⁻', 13, '#3b2a00', 800)
for x, y in [(290, 620), (422, 620), (560, 420), (690, 335), (838, 330), (872, 560)]: ebead(x, y)

# reações de entrada e saída
A("<g filter='url(#sh)'><rect x='182' y='680' width='128' height='60' rx='14' fill='#0b1424' stroke='#93c5fd' stroke-width='2'/></g>")
text(246, 707, 'NADH', 22, '#ffffff', 800); text(246, 729, '→ NAD⁺', 15, '#bfdbfe', 600)
A("<g filter='url(#sh)'><rect x='328' y='680' width='122' height='60' rx='14' fill='#0b1424' stroke='#86efac' stroke-width='2'/></g>")
text(389, 707, 'FADH₂', 22, '#ffffff', 800); text(389, 729, '→ FAD', 15, '#bbf7d0', 600)
A("<g filter='url(#sh)'><rect x='820' y='648' width='170' height='74' rx='14' fill='#0b1424' stroke='#7dd3fc' stroke-width='2'/></g>")
text(905, 678, 'O₂ + H⁺', 21, '#ffffff', 800); text(905, 706, '→ H₂O (água)', 17, '#bae6fd', 700)
badge(170, 678, 1); badge(560, 226, 2); badge(1008, 650, 3)

# ---------------- ATP sintase ----------------
cx, rx, ry = 1240, 82, 20
yt, yb = MT + 2, MB - 2
# título
tag(cx, 128, ['ATP sintase'], '#fda4af', 26, 800)
text(cx, 168, 'uma “turbina” que fabrica ATP', 16, '#fecdd3', 600)
badge(cx - 118, 120, 4)

# H+ esperando para entrar
for (x, y) in [(1340, 205), (1378, 238), (1338, 262), (1395, 290), (1352, 315)]: hplus(x, y, 14)
text(1460, 222, 'H⁺ voltam', 15, '#ffd28a', 700, 'start'); text(1460, 241, 'a favor do', 15, '#ffd28a', 700, 'start'); text(1460, 260, 'gradiente', 15, '#ffd28a', 700, 'start')

# estator periférico (b) — atrás da cabeça
A(f"<path d='M1378,{yb} C1410,540 1418,660 1372,722 C1345,758 1300,770 1270,772' stroke='url(#gStat)' stroke-width='18' fill='none' stroke-linecap='round' filter='url(#sh)'/>")

# cabeça F1 — lóbulos de trás
F1Y = 668
lobes = []
for k in range(6):
    ang = math.radians(k * 60 + 30)
    lobes.append((math.cos(ang), cx + 74 * math.sin(ang), F1Y - 14 * math.cos(ang), k % 2))
lobes.sort()
for depth, x, y, beta in lobes[:3]:
    A(f"<ellipse cx='{x:.1f}' cy='{y:.1f}' rx='44' ry='60' fill='url(#{'gBeta' if beta else 'gAlpha'})' stroke='#7f1d3a' stroke-width='1.5' opacity='.9'/>")
# eixo central (gira)
A(f"<rect x='{cx-15}' y='{yb-4}' width='30' height='{F1Y-yb+20}' rx='12' fill='url(#gRod)' stroke='#78350f' stroke-width='1.5'/>")
# anel rotor (F0) em perspectiva
A(f"<path d='M{cx-rx},{yt} L{cx-rx},{yb} A{rx},{ry} 0 0 0 {cx+rx},{yb} L{cx+rx},{yt} Z' fill='url(#gRing)' stroke='#78350f' stroke-width='1.5'/>")
for i in range(1, 10):
    x = cx - rx * math.cos(i * math.pi / 10)          # faixas (subunidades c) na face visível
    A(f"<line x1='{x:.1f}' y1='{yt + ry * math.sin(i*math.pi/10):.1f}' x2='{x:.1f}' y2='{yb + ry * math.sin(i*math.pi/10):.1f}' stroke='#78350f' stroke-width='1.6' stroke-opacity='.7'/>")
A(f"<ellipse cx='{cx}' cy='{yt}' rx='{rx}' ry='{ry}' fill='#fde68a' stroke='#78350f' stroke-width='1.5'/>")
A(f"<ellipse cx='{cx}' cy='{yt}' rx='{rx-30}' ry='{ry-8}' fill='#d97706' opacity='.6'/>")
# subunidade a (canal de H+, parada)
A(f"<g filter='url(#sh)'><rect x='{cx+rx-6}' y='{MT-18}' width='66' height='{MB-MT+36}' rx='24' fill='url(#gSub)' stroke='#134e4a' stroke-width='1.5'/></g>")
# trajeto do H+ pelo canal
A(f"<path d='M1357,330 L1357,384 C1357,414 1316,420 1300,430 C1290,438 1306,460 1310,482 L1310,548' stroke='#ffa733' stroke-width='7' fill='none' stroke-linecap='round' marker-end='url(#arH)'/>")
hplus(1300, 432, 13); hplus(1312, 570, 14)
# lóbulos da frente
for depth, x, y, beta in lobes[3:]:
    A(f"<ellipse cx='{x:.1f}' cy='{y:.1f}' rx='44' ry='60' fill='url(#{'gBeta' if beta else 'gAlpha'})' stroke='#7f1d3a' stroke-width='1.5'/>")
    A(f"<ellipse cx='{x-12:.1f}' cy='{y-24:.1f}' rx='12' ry='18' fill='#ffffff' opacity='.25'/>")
A(f"<ellipse cx='{cx}' cy='{F1Y+70}' rx='30' ry='12' fill='url(#gSub)' stroke='#134e4a' stroke-width='1.5'/>")
# setas de rotação
A(f"<path d='M{cx-rx-8},{yb+30} A{rx+8},{ry+8} 0 0 0 {cx+rx-14},{yb+34}' stroke='#ffffff' stroke-width='3.5' fill='none' marker-end='url(#arW)'/>")
A(f"<path d='M{cx-38},{F1Y-92} A38,10 0 0 0 {cx+30},{F1Y-90}' stroke='#ffffff' stroke-width='3' fill='none' marker-end='url(#arW)'/>")

# ADP + P entram, ATP sai
nucleotide(1046, 594, 2); text(1118, 600, '+', 22, '#ffffff', 800); phosphate(1140, 594, 12)
text(1092, 566, 'ADP + P', 17, '#e2e8f0', 700)
A("<path d='M1150,616 C1165,640 1175,650 1188,658' stroke='#f1f5f9' stroke-width='3' fill='none' marker-end='url(#arW)'/>")
A("<path d='M1180,720 C1160,745 1140,752 1125,754' stroke='#f1f5f9' stroke-width='3' fill='none' marker-end='url(#arW)'/>")
A("<ellipse cx='1080' cy='758' rx='72' ry='26' fill='#fde047' filter='url(#glow)' opacity='.45'/>")
nucleotide(1030, 758, 3)
text(1066, 732, 'ATP', 22, '#fde047', 800)

# legendas da ATP sintase
tag(1500, 336, ['Canal de H⁺'], '#5eead4', 15); leader(1450, 336, 1390, 360, '#5eead4')
tag(1085, 418, ['Rotor', '(gira)'], '#fcd34d', 15); leader(1123, 420, 1165, 420, '#fcd34d')
tag(1505, 520, ['Eixo central', '(gira junto)'], '#fcd34d', 15); leader(1445, 522, 1256, 540, '#fcd34d')
tag(1510, 640, ['Cabeça F₁:', 'monta o ATP'], '#fda4af', 15); leader(1450, 642, 1318, 668, '#fda4af')
tag(1500, 750, ['Estator', '(fica parado)'], '#5eead4', 15); leader(1442, 752, 1385, 712, '#5eead4')

# ---------------- passo a passo ----------------
A("<rect x='0' y='808' width='1600' height='192' fill='#060d19'/>")
CARDS = [
    ('Elétrons entram', '#93c5fd', ['NADH e FADH₂ (vindos da glicólise', 'e do ciclo de Krebs) entregam', 'elétrons para a cadeia.']),
    ('H⁺ são bombeados', '#ffc46b', ['Ao passar pelos complexos I, III e IV,', 'os elétrons liberam energia, usada', 'para bombear H⁺ para fora da matriz.']),
    ('O₂ recebe os elétrons', '#7dd3fc', ['No fim da cadeia, o oxigênio recebe', 'os elétrons e H⁺ e forma água.', 'Sem O₂, a cadeia para!']),
    ('ATP é produzido', '#fda4af', ['Os H⁺ voltam para a matriz pela ATP', 'sintase, que gira como uma turbina e', 'une ADP + P → ATP.']),
]
for i, (title, col, body) in enumerate(CARDS):
    x0 = 16 + i * 396
    A(f"<rect x='{x0}' y='822' width='380' height='164' rx='16' fill='#0f1b2e' stroke='{col}' stroke-opacity='.6' stroke-width='1.6'/>")
    badge(x0 + 34, 856, i + 1)
    text(x0 + 64, 863, title, 20, col, 800, 'start')
    lines(x0 + 22, 900, body, 16, '#e2e8f0', 500, 'start', lh=1.35)
A("</svg>")

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fosforilacao-oxidativa.svg')
open(p, 'w').write('\n'.join(out))
print(p)
