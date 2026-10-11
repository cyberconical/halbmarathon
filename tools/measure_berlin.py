#!/usr/bin/env python3
"""Misst, wie groß eine vereinfachte Offline-Straßendatei für Berlin würde.

Eingabe: GeoJSON-Sequenzen aus `osmium export` (Straßen/Wege und Flächen).
Ausgabe: Tabelle mit Größen je Detailstufe (Markdown) und Beispieldateien.

Es werden keine persönlichen Daten verwendet, nur öffentliche OpenStreetMap-Daten
(© OpenStreetMap contributors, ODbL).
"""
import json, math, sys, zlib, os

LAT0 = 52.5
KX = 111320.0 * math.cos(math.radians(LAT0))   # Meter pro Grad Länge
KY = 110574.0                                   # Meter pro Grad Breite
Q = 1e5                                         # Raster 1e-5 Grad, etwa 1 m

LEVELS = {
    "1 Hauptstraßen": {"motorway", "trunk", "primary", "secondary", "tertiary",
                       "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link"},
    "2 + Wohnstraßen": {"residential", "unclassified", "living_street"},
    "3 + Wege für Läufer": {"footway", "path", "track", "cycleway", "pedestrian", "steps", "bridleway"},
}


def rdp(pts, eps):
    """Douglas-Peucker, iterativ. pts: Liste von (x, y) in Metern."""
    n = len(pts)
    if n < 3:
        return list(range(n))
    keep = [False] * n
    keep[0] = keep[-1] = True
    stack = [(0, n - 1)]
    while stack:
        a, b = stack.pop()
        ax, ay = pts[a]
        bx, by = pts[b]
        dx, dy = bx - ax, by - ay
        L2 = dx * dx + dy * dy
        best, idx = -1.0, -1
        for i in range(a + 1, b):
            px, py = pts[i]
            if L2 == 0:
                d = math.hypot(px - ax, py - ay)
            else:
                t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L2))
                d = math.hypot(px - (ax + t * dx), py - (ay + t * dy))
            if d > best:
                best, idx = d, i
        if best > eps:
            keep[idx] = True
            stack.append((a, idx))
            stack.append((idx, b))
    return [i for i, k in enumerate(keep) if k]


def simplify(coords, eps):
    pts = [(c[0] * KX, c[1] * KY) for c in coords]
    return [coords[i] for i in rdp(pts, eps)]


def varint_len(n):
    n = int(n)
    l = 1
    while n >= 0x80:
        n >>= 7
        l += 1
    return l


def zigzag(n):
    return (n << 1) ^ (n >> 63) if n < 0 else n << 1


def encode_lines(lines):
    """Binärgröße: je Linie Punktzahl + Startpunkt + Deltas (zigzag-varint, 1e-5 Grad)."""
    out = bytearray()

    def put(n):
        n = int(n)
        while n >= 0x80:
            out.append((n & 0x7F) | 0x80)
            n >>= 7
        out.append(n)

    px = py = 0
    for line in lines:
        put(len(line))
        for x, y in line:
            ix, iy = round(x * Q), round(y * Q)
            put(zigzag(ix - px))
            put(zigzag(iy - py))
            px, py = ix, iy
    return bytes(out)


def iter_features(path):
    with open(path, encoding="utf-8") as f:
        for raw in f:
            raw = raw.strip().lstrip("\x1e")
            if raw:
                try:
                    yield json.loads(raw)
                except ValueError:
                    continue


def lines_of(geom):
    t = geom.get("type")
    c = geom.get("coordinates")
    if t == "LineString":
        yield c
    elif t == "MultiLineString":
        yield from c
    elif t == "Polygon":
        yield from c
    elif t == "MultiPolygon":
        for p in c:
            yield from p


def main(hw_path, area_path, outdir, eps):
    os.makedirs(outdir, exist_ok=True)
    by_level = {k: [] for k in LEVELS}
    names = {k: set() for k in LEVELS}
    seen_classes = {}
    for ft in iter_features(hw_path):
        pr = ft.get("properties") or {}
        hw = pr.get("highway")
        seen_classes[hw] = seen_classes.get(hw, 0) + 1
        for lvl, classes in LEVELS.items():
            if hw in classes:
                for ln in lines_of(ft["geometry"]):
                    by_level[lvl].append(ln)
                if pr.get("name"):
                    names[lvl].add(pr["name"])
    water, parks = [], []
    for ft in iter_features(area_path):
        pr = ft.get("properties") or {}
        is_water = pr.get("natural") == "water" or pr.get("waterway") in ("river", "canal") or pr.get("landuse") == "reservoir"
        is_park = pr.get("leisure") in ("park", "nature_reserve") or pr.get("landuse") == "forest" or pr.get("natural") == "wood"
        for ln in lines_of(ft["geometry"]):
            if is_water:
                water.append(ln)
            elif is_park:
                parks.append(ln)

    rows = []
    cumulative = []
    nm_cum = set()
    for lvl in LEVELS:
        cumulative += by_level[lvl]
        nm_cum |= names[lvl]
        raw_pts = sum(len(l) for l in cumulative)
        simp = [simplify(l, eps) for l in cumulative]
        simp = [l for l in simp if len(l) >= 2]
        pts = sum(len(l) for l in simp)
        enc = encode_lines(simp)
        nm_bytes = sum(len(n.encode("utf-8")) + 1 for n in nm_cum)
        rows.append((lvl, len(simp), raw_pts, pts, len(enc), len(zlib.compress(enc, 9)), len(nm_cum), nm_bytes))
        if lvl.startswith("3"):
            open(os.path.join(outdir, "berlin-streets-v3.bin"), "wb").write(enc)
            open(os.path.join(outdir, "berlin-names-v3.txt"), "w", encoding="utf-8").write("\n".join(sorted(nm_cum)))

    extra = []
    for label, group in (("Wasser", water), ("Parks und Wald", parks)):
        simp = [simplify(l, eps * 2) for l in group]
        simp = [l for l in simp if len(l) >= 2]
        enc = encode_lines(simp)
        extra.append((label, len(simp), sum(len(l) for l in simp), len(enc), len(zlib.compress(enc, 9))))

    kb = lambda n: f"{n/1024:,.0f} KB"
    md = [f"## Berlin: Größe einer Offline-Straßendatei (Vereinfachung {eps} m, Raster 1 m)", "",
          "| Stufe (kumulativ) | Linien | Punkte vorher | Punkte nachher | Binär | Gezippt | Straßennamen | Namen (Text) |",
          "|---|---:|---:|---:|---:|---:|---:|---:|"]
    for r in rows:
        md.append(f"| {r[0]} | {r[1]:,} | {r[2]:,} | {r[3]:,} | {kb(r[4])} | {kb(r[5])} | {r[6]:,} | {kb(r[7])} |")
    md += ["", "| Fläche (eigene Datei, Vereinfachung " + str(eps * 2) + " m) | Linien | Punkte | Binär | Gezippt |", "|---|---:|---:|---:|---:|"]
    for e in extra:
        md.append(f"| {e[0]} | {e[1]:,} | {e[2]:,} | {kb(e[3])} | {kb(e[4])} |")
    top = sorted(seen_classes.items(), key=lambda kv: -kv[1])[:12]
    md += ["", "Wege je highway-Klasse (Top 12): " + ", ".join(f"{k}={v:,}" for k, v in top)]
    text = "\n".join(md)
    print(text)
    open(os.path.join(outdir, "summary.md"), "w", encoding="utf-8").write(text + "\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3], float(sys.argv[4]) if len(sys.argv) > 4 else 4.0)
