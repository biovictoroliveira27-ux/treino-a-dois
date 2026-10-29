# Gera fosforilacao-oxidativa.svg (cadeia transportadora de elétrons + ATP sintase).
# PNG: node render.mjs (Playwright) ou qualquer conversor SVG→PNG.
import math, random, os
random.seed(4)
W, H = 1600, 900
MT, MB = 340, 460            # membrana interna (topo/base)
out = []
A = out.append
F = "font-family='Inter, DejaVu Sans, Arial, sans-serif'"

def text(x, y, s, size=16, fill='#e8eef8', weight=600, anchor='middle', extra=''):
    A(f"<text x='{x}' y='{y}' {F} font-size='{size}' font-weight='{weight}' fill='{fill}' text-anchor='{anchor}' {extra}>{s}</text>")

def tag(x, y, s, col, size=15, weight=700):
    w = len(s) * size * 0.56 + 20
    A(f"<rect x='{x-w/2:.1f}' y='{y-size*0.95:.1f}' width='{w:.1f}' height='{size*1.5:.1f}' rx='8' fill='#0a1222' fill-opacity='.85' stroke='{col}' stroke-width='1.3'/>")
    text(x, y, s, size, col, weight)

def hplus(x, y, r=13, op=1):
    A(f"<g opacity='{op}'><circle cx='{x}' cy='{y}' r='{r}' fill='url(#gH)'/>"
      f"<text x='{x}' y='{y+4.5}' {F} font-size='{r*0.95:.1f}' font-weight='800' fill='#3b1d00' text-anchor='middle'>H⁺</text></g>")

early = []
def proton_arrow(x, y1, y2, col='#ffb347'):
    early.append(f"<line x1='{x}' y1='{y1}' x2='{x}' y2='{y2}' stroke='{col}' stroke-width='4' stroke-linecap='round' marker-end='url(#arH)'/>")

A(f"<svg xmlns='http://www.w3.org/2000/svg' width='{W}' height='{H}' viewBox='0 0 {W} {H}'>")
A("""<defs>
<linearGradient id='gIMS' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='#2a1430'/><stop offset='1' stop-color='#3a1a2e'/></linearGradient>
<linearGradient id='gMat' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='#10293d'/><stop offset='1' stop-color='#0a1a2b'/></linearGradient>
<radialGradient id='gH' cx='.35' cy='.35'><stop offset='0' stop-color='#fff3c4'/><stop offset='.5' stop-color='#ffc04d'/><stop offset='1' stop-color='#e08a00'/></radialGradient>
<marker id='arH' viewBox='0 0 10 10' refX='5' refY='5' markerWidth='4.5' markerHeight='4.5' orient='auto-start-reverse'><path d='M0,0 L10,5 L0,10 z' fill='#ffb347'/></marker>
<marker id='arE' viewBox='0 0 10 10' refX='5' refY='5' markerWidth='5' markerHeight='5' orient='auto-start-reverse'><path d='M0,0 L10,5 L0,10 z' fill='#ffe14d'/></marker>
<marker id='arW' viewBox='0 0 10 10' refX='5' refY='5' markerWidth='5' markerHeight='5' orient='auto-start-reverse'><path d='M0,0 L10,5 L0,10 z' fill='#e8eef8'/></marker>
<filter id='sh' x='-20%' y='-20%' width='140%' height='140%'><feDropShadow dx='0' dy='4' stdDeviation='5' flood-color='#000' flood-opacity='.45'/></filter>
""")
def grad(id, c1, c2):
    A(f"<radialGradient id='{id}' cx='.35' cy='.3' r='.9'><stop offset='0' stop-color='{c1}'/><stop offset='1' stop-color='{c2}'/></radialGradient>")
grad('g1', '#8ec2ff', '#2f6fc9'); grad('g2', '#8fe3b8', '#23905f'); grad('g3', '#c9a8ff', '#6e43c4')
grad('g4', '#ffd08a', '#d27d12'); grad('g5', '#ff9fb0', '#c73b5a'); grad('gc', '#ff8a8a', '#c42a2a')
grad('gQ', '#fff0a8', '#e0a800'); grad('gF1a', '#ffb6c3', '#d94d6c'); grad('gF1b', '#ffd6de', '#e77a93')
A("</defs>")

# fundos
A(f"<rect width='{W}' height='{MT}' fill='url(#gIMS)'/>")
A(f"<rect y='{MB}' width='{W}' height='{H-MB}' fill='url(#gMat)'/>")
A(f"<rect y='{MT}' width='{W}' height='{MB-MT}' fill='#3b2a1c'/>")
# bicamada lipídica
for x in range(-4, W + 10, 14):
    for (yh, d) in ((MT + 8, 1), (MB - 8, -1)):
        A(f"<path d='M{x-2.5},{yh} q -2,{d*22} 1,{d*44} M{x+2.5},{yh} q 2,{d*22} -1,{d*44}' stroke='#c79a55' stroke-opacity='.55' stroke-width='1.6' fill='none'/>")
        A(f"<circle cx='{x}' cy='{yh}' r='6.3' fill='#f3e3b3'/>")

# gradiente de prótons (evitando textos e rótulos)
KEEP_OUT = [(0, 0, 1600, 150), (0, 95, 360, 160), (90, 220, 370, 350), (430, 260, 580, 330), (620, 150, 830, 300),
            (820, 210, 1110, 340), (1230, 140, 1500, 330)]
n = 0
while n < 46:
    x, y = random.uniform(30, 1570), random.uniform(150, 325)
    if any(a <= x <= c and b <= y <= d for a, b, c, d in KEEP_OUT): continue
    hplus(round(x), round(y), 10, 0.55); n += 1
EARLY_AT = len(out)
for (x, y) in [(60, 600), (185, 690), (600, 560), (870, 770), (1170, 800), (1530, 560), (40, 760), (1560, 680)]:
    hplus(round(x), round(y), 10, 0.4)

# títulos de regiões
text(800, 46, 'Fosforilação oxidativa', 36, '#ffffff', 800)
text(800, 78, 'Cadeia transportadora de elétrons e ATP sintase na membrana mitocondrial interna', 18, '#cbd5e1', 500)
text(24, 116, 'ESPAÇO INTERMEMBRANAS', 17, '#f9a8d4', 800, 'start')
text(24, 138, 'alta [H⁺] · lado positivo (P)', 14, '#f9a8d4', 500, 'start')
text(24, 862, 'MATRIZ MITOCONDRIAL', 17, '#7dd3fc', 800, 'start')
text(24, 884, 'baixa [H⁺] · lado negativo (N)', 14, '#7dd3fc', 500, 'start')
A(f"<rect x='1488' y='{MT+38}' width='106' height='44' rx='8' fill='#0a1222' fill-opacity='.8'/>")
text(1541, MT + 56, 'Membrana', 13, '#f3e3b3', 700); text(1541, MT + 73, 'interna', 13, '#f3e3b3', 700)

# ---------------- Complexo I ----------------
A("<g filter='url(#sh)'>")
A(f"<rect x='100' y='{MT-14}' width='250' height='{MB-MT+28}' rx='34' fill='url(#g1)'/>")
A(f"<path d='M250,{MB-10} C245,560 255,640 270,690 C285,730 345,735 360,700 C375,660 345,560 340,{MB-10} Z' fill='url(#g1)'/>")
A("</g>")
for i, (x, y) in enumerate([(318, 690), (312, 640), (305, 595), (300, 548), (300, 502), (305, 460)]):
    A(f"<rect x='{x-6}' y='{y-6}' width='12' height='12' rx='2' fill='#b45309' stroke='#fde68a' stroke-width='1.3' transform='rotate(45 {x} {y})'/>")
A("<circle cx='318' cy='712' r='9' fill='#fde047' stroke='#854d0e' stroke-width='1.5'/>")
text(225, 395, 'Complexo I', 22, '#ffffff', 800); text(225, 419, 'NADH desidrogenase', 14, '#e0f2fe', 600)
tag(205, 600, 'Fe–S', '#fbbf24', 13); tag(372, 735, 'FMN', '#fde047', 13)
# NADH
text(230, 790, 'NADH', 20, '#ffffff', 800); text(230, 814, '→ NAD⁺ + H⁺', 16, '#cbd5e1', 600)
A("<path d='M262,778 C285,770 300,745 309,722' stroke='#e8eef8' stroke-width='2.2' fill='none' marker-end='url(#arW)'/>")
for x in (130, 175, 220, 265):
    proton_arrow(x, MB + 34, MT - 28)
hplus(197, 270, 16); text(197, 236, '4 H⁺', 17, '#ffd28a', 800)

# ---------------- Complexo II ----------------
A("<g filter='url(#sh)'>")
A(f"<path d='M425,{MT+40} C425,{MT+20} 540,{MT+20} 540,{MT+40} L545,640 C545,690 420,690 420,640 Z' fill='url(#g2)'/>")
A("</g>")
A("<circle cx='482' cy='620' r='9' fill='#fde047' stroke='#854d0e' stroke-width='1.5'/>")
for (x, y) in [(486, 565), (490, 515), (492, 468)]:
    A(f"<rect x='{x-5}' y='{y-5}' width='10' height='10' rx='2' fill='#b45309' stroke='#fde68a' stroke-width='1.2' transform='rotate(45 {x} {y})'/>")
text(482, 744, 'Complexo II', 20, '#ffffff', 800); text(482, 766, 'Succinato desidrogenase', 14, '#d1fae5', 600)
text(482, 806, 'Succinato → Fumarato', 16, '#ffffff', 700); text(482, 828, 'FAD → FADH₂', 15, '#bbf7d0', 600)
tag(540, 612, 'FAD', '#fde047', 13)
text(482, 856, 'não bombeia H⁺', 13, '#94a3b8', 600, extra="font-style='italic'")

# ---------------- Ubiquinona ----------------
for (x, y, lab) in [(385, 405, 'Q'), (620, 392, 'QH₂')]:
    A(f"<ellipse cx='{x}' cy='{y}' rx='24' ry='17' fill='url(#gQ)' stroke='#7c5800' stroke-width='1.5'/>")
    text(x, y + 6, lab, 15, '#3b2a00', 800)
A("<path d='M410,402 C470,388 540,380 594,390' stroke='#ffe14d' stroke-width='2.5' stroke-dasharray='6 5' fill='none' marker-end='url(#arE)'/>")
tag(500, 290, 'Ubiquinona (Q / QH₂)', '#fde047', 15)
A(f"<line x1='500' y1='298' x2='500' y2='{MT+42}' stroke='#fde047' stroke-width='1.3'/>")

# ---------------- Complexo III ----------------
A("<g filter='url(#sh)'>")
A(f"<path d='M650,{MT-48} C650,{MT-70} 720,{MT-75} 722,{MT-50} C724,{MT-75} 795,{MT-70} 795,{MT-48} L800,560 C800,600 645,600 645,560 Z' fill='url(#g3)'/>")
A("</g>")
A(f"<line x1='722' y1='{MT-50}' x2='722' y2='585' stroke='#4c2a99' stroke-width='2' stroke-dasharray='4 4'/>")
for (x, y, c, l) in [(690, 430, '#ef4444', 'b'), (760, 425, '#ef4444', 'b'), (705, 305, '#f97316', 'c₁'), (752, 320, '#b45309', 'FeS')]:
    A(f"<circle cx='{x}' cy='{y}' r='9' fill='{c}' stroke='#fff' stroke-width='1.3'/>")
text(722, 520, 'Complexo III', 21, '#ffffff', 800); text(722, 543, 'Citocromo bc₁', 14, '#ede9fe', 600)
text(722, 562, '(ciclo Q)', 13, '#ddd6fe', 600)
for x in (672, 700, 744, 772):
    proton_arrow(x, MB + 20, MT - 96)
hplus(722, 205, 16); text(722, 172, '4 H⁺', 17, '#ffd28a', 800)

# ---------------- Citocromo c ----------------
A("<g filter='url(#sh)'><circle cx='880' cy='292' r='30' fill='url(#gc)'/></g>")
A("<rect x='870' y='282' width='20' height='20' rx='3' fill='#7f1d1d' stroke='#fecaca' stroke-width='1.4'/><circle cx='880' cy='292' r='3.5' fill='#fca5a5'/>")
tag(880, 238, 'Citocromo c', '#fca5a5', 15)

# ---------------- Complexo IV ----------------
A("<g filter='url(#sh)'>")
A(f"<path d='M955,{MT-62} C955,{MT-82} 1095,{MT-82} 1095,{MT-62} L1105,590 C1105,630 950,630 950,590 Z' fill='url(#g4)'/>")
A("</g>")
for (x, y, c, l) in [(985, 300, '#0ea5e9', 'CuA'), (1015, 415, '#ef4444', 'a'), (1060, 425, '#ef4444', 'a₃'), (1065, 470, '#0ea5e9', 'CuB')]:
    A(f"<circle cx='{x}' cy='{y}' r='9' fill='{c}' stroke='#fff' stroke-width='1.3'/>")
    text(x + (24 if l != 'a' else -20), y + 5, l, 12, '#3b1d00', 800)
text(1022, 530, 'Complexo IV', 20, '#3b1d00', 800); text(1022, 551, 'Citocromo c oxidase', 13, '#4a2a05', 700)
text(1030, 700, '½ O₂ + 2 H⁺ → H₂O', 19, '#ffffff', 800)
A("<path d='M1060,610 C1060,640 1048,660 1040,678' stroke='#e8eef8' stroke-width='2.2' fill='none' marker-start='url(#arW)'/>")
text(1030, 725, 'aceptor final de elétrons', 13, '#94a3b8', 600, extra="font-style='italic'")
for x in (990, 1062):
    proton_arrow(x, MB + 34, MT - 108)
hplus(1026, 205, 16); text(1026, 172, '2 H⁺', 17, '#ffd28a', 800)

# ---------------- caminho dos elétrons ----------------
def epath(d):
    A(f"<path d='{d}' stroke='#ffe14d' stroke-width='3' stroke-dasharray='7 5' fill='none' marker-end='url(#arE)'/>")
epath("M318,700 C312,600 300,520 352,420 L362,412")             # FMN → Fe-S → Q
epath("M486,610 C492,540 495,470 420,416")                        # FAD → Fe-S → Q
epath("M644,395 C664,398 680,410 690,420")                        # QH2 → III
epath("M752,318 C790,300 820,292 846,290")                        # c1 → cyt c
epath("M910,294 C935,296 955,298 973,300")                        # cyt c → CuA
epath("M990,312 C1000,350 1008,380 1012,402")                     # CuA → a
epath("M1026,413 C1034,407 1042,409 1049,417")                    # a → a3/CuB
epath("M1074,478 C1097,515 1098,575 1068,607")                    # a3/CuB → O2
for (x, y) in [(330, 560), (493, 520), (815, 296), (1091, 586)]:
    A(f"<circle cx='{x}' cy='{y}' r='11' fill='#ffe14d'/>")
    text(x, y + 4.5, 'e⁻', 12, '#3b2a00', 800)

# ---------------- ATP sintase ----------------
cx = 1345
A("<g filter='url(#sh)'>")
# estator periférico (b + OSCP)
A(f"<path d='M1440,{MB-10} L1452,700 C1452,745 1410,770 1370,768' stroke='#f0abbb' stroke-width='14' fill='none' stroke-linecap='round'/>")
# subunidade a
A(f"<rect x='1415' y='{MT-6}' width='56' height='{MB-MT+12}' rx='20' fill='url(#g5)'/>")
# anel c
A(f"<rect x='1265' y='{MT-4}' width='150' height='{MB-MT+8}' rx='26' fill='#b8324f'/>")
for i in range(8):
    x = 1278 + i * 17.5
    A(f"<rect x='{x}' y='{MT+2}' width='13' height='{MB-MT-4}' rx='6' fill='url(#g5)'/>")
# haste central γ/ε
A(f"<rect x='{cx-11}' y='{MB}' width='22' height='130' rx='9' fill='#f472b6'/>")
# F1: α3β3
for k in range(6):
    a = k * math.pi / 3 + math.pi / 6
    x, y = cx + 62 * math.cos(a), 680 + 46 * math.sin(a)
    A(f"<circle cx='{x:.1f}' cy='{y:.1f}' r='40' fill='url(#{'gF1a' if k % 2 else 'gF1b'})'/>")
A(f"<ellipse cx='{cx}' cy='768' rx='24' ry='14' fill='#f0abbb'/>")
A("</g>")
text(cx - 128, 690, 'F₁', 26, '#fda4af', 800)
text(cx - 62, 640, 'β', 15, '#5a0d22', 800); text(cx + 62, 640, 'α', 15, '#5a0d22', 800)
text(cx + 2, MT + 63, 'F₀', 24, '#ffe4ea', 800)
# rotação
A(f"<path d='M1278,{MB+26} A72,16 0 0 0 1412,{MB+26}' stroke='#ffffff' stroke-width='2.4' fill='none' marker-end='url(#arW)'/>")
text(1240, MB + 52, 'rotação', 13, '#e8eef8', 700)
# fluxo de H+ a favor do gradiente
A(f"<path d='M1443,{MT-118} L1443,{MB+40}' stroke='#ffb347' stroke-width='6' stroke-linecap='round' marker-end='url(#arH)'/>")
hplus(1443, MT - 136, 16); text(1443, MT - 166, '≈ 3 H⁺', 17, '#ffd28a', 800)
tag(cx - 30, 168, 'ATP sintase', '#fda4af', 19, 800)
text(cx - 30, 196, '(Complexo V)', 14, '#fecdd3', 600)
# síntese de ATP
text(1500, 790, 'ADP + Pᵢ', 20, '#ffffff', 800)
A("<path d='M1490,800 C1470,830 1440,840 1410,836' stroke='#e8eef8' stroke-width='2.2' fill='none' marker-end='url(#arW)'/>")
A("<g filter='url(#sh)'><rect x='1300' y='815' width='96' height='40' rx='20' fill='#fde047'/></g>")
text(1348, 842, 'ATP', 22, '#3b2a00', 800)

# ---------------- legenda ----------------
A("<rect x='600' y='850' width='640' height='38' rx='12' fill='#0a1222' fill-opacity='.85'/>")
A("<line x1='620' y1='869' x2='660' y2='869' stroke='#ffe14d' stroke-width='3' stroke-dasharray='7 5'/>")
text(668, 874, 'fluxo de elétrons', 14, '#fde68a', 600, 'start')
A("<line x1='812' y1='869' x2='848' y2='869' stroke='#ffb347' stroke-width='4' marker-end='url(#arH)'/>")
text(860, 874, 'bombeamento de H⁺', 14, '#ffd28a', 600, 'start')
text(1010, 874, '~2,5 ATP/NADH · ~1,5 ATP/FADH₂', 14, '#e2e8f0', 700, 'start')
text(800, 112, 'Força próton-motriz (ΔpH + Δψ): 10 H⁺ bombeados por NADH oxidado', 15, '#fbcfe8', 600)
out[EARLY_AT:EARLY_AT] = early
A("</svg>")

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fosforilacao-oxidativa.svg')
open(p, 'w').write('\n'.join(out))
print(p)
