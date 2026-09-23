# Home Price brand

A house holding the `$ € ¥` symbols: your prices, brought home.

## Files

| File | What it is |
|---|---|
| `home-price-logo.png` | Original logo design (wordmark and icon variants). The reference for everything below |
| `icon-512.png` | **Google Play store icon.** 512×512, 32-bit PNG, square and full-bleed (Play rounds the corners) |
| `icon.svg` | Square colour master, for web, press and other stores |
| `icon-monochrome.svg` | Single-colour master: the house with the symbols cut out |
| `make_icon.py` | Generator for all of the above plus the Android adaptive icon |
| `fonts/NotoSerif-Bold-symbols.ttf` | Noto Serif Bold, trimmed to `$ € ¥`, used for the symbol outlines |
| `fonts/OFL.txt` | The font's SIL Open Font License, which must stay with the font file |

The Android launcher icon lives in the app itself:

- `app/src/main/res/drawable/ic_launcher_foreground.xml`: the house and symbols
- `app/src/main/res/drawable/ic_launcher_background.xml`: the dark background
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`: themed-icon version (Android 13+)

## Colours

| Use | Hex |
|---|---|
| Background | `#201E1D` |
| House | `#F3F2F2` |
| `$` | `#0081A7` |
| `€` | `#CC0066` |
| `¥` | `#E2B100` |

## Regenerating

All icon files are generated. Don't edit them by hand; change `make_icon.py` and rerun it:

```sh
pip install fonttools skia-pathops   # once; ImageMagick (`magick`) is also needed for the PNG
python docs/brand/make_icon.py
```

The house and symbol positions in the script are measured in pixels on the dark 240 px tile in `home-price-logo.png`, then mapped onto Android's 108 dp adaptive-icon canvas. That tile corresponds to the 72 dp area a launcher shows. The house stays inside the 66 dp safe-zone circle (its furthest corner is 30.3 dp from the centre), so no launcher mask shape crops it.

## Not done yet

- **Feature graphic:** Google Play also requires a 1024×500 banner for the store listing.
- **Wordmark:** the logo's "Home Price" text is only in `home-price-logo.png`. There is no vector version and the font isn't known.
