# Offline basemap

Everything here ships inside the app; nothing is downloaded at run time.

| File | What | License |
|---|---|---|
| `maplibre-gl.js/.css` 5.24.0 | map renderer | BSD-3-Clause |
| `pmtiles.js` 4.5.0 | reads one-file tile archives | BSD-3-Clause |
| `basemaps.js` (@protomaps/basemaps 5.7.2) | map style | BSD-3-Clause |
| `fonts/`, `sprites/` | glyphs (Noto Sans, OFL) and icons from protomaps/basemaps-assets | see that repo |
| `berlin/p0000.pmtiles`, `p0001.pmtiles` … | Berlin extract cut into 1 MB pieces, created by the CI build, **not in git** (one big file cannot be read in parts on Android: its file server ignores the start of a Range request) | © OpenStreetMap contributors (ODbL), Protomaps build |

If `berlin/p0000.pmtiles` is missing, or a route lies outside Berlin, the app shows the plain line map.
