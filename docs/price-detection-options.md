# Price detection: options

Status: exploring. Last updated 2026-09-23.

The price scanner reads every number on screen with ML Kit text recognition, but a price tag carries many numbers that are not the price. This doc records what the scanner does today, what we learned from real tags, and the options for improving accuracy.

## Current approach

- **OCR:** ML Kit Latin text recognition via CameraX `MlKitAnalyzer`, running at half the device's maximum OCR rate. Boxes are in preview coordinates.
- **Aim box:** only numbers whose centre is inside the box in the middle of the screen are used (see option 1).
- **Candidate filtering** (`analyzer/PriceAnalyzer.kt`) drops numbers that are clearly not prices:
  - long digit runs (7+ digits: UPC/JAN/SKU codes)
  - dates (`11-26-24`, `11/26/2024`)
  - numbers followed by a unit (`10 OZ`, `2L`, `500 ml`, `37.9¢`, `per`)
  - lone digits unless a currency symbol marks them (`2個入` dropped, `$5` kept)
- **Missed decimal points:** `2 99` is read as 2.99. For `299`, the character boxes are checked for a wide gap or superscript cents before the last two digits.
- **Modes:**
  - **Manual** (default): candidates are highlighted, the user taps the real price, and the frame freezes.
  - **Auto:** every candidate shows its conversion.

The filters only look at the shape of a number. They cannot tell a shelf price from a unit price, a before-tax price from a tax-included one, or a price from a number printed on the product.

## What real tags showed

| Tag | Numbers present | What goes wrong |
|---|---|---|
| US e-ink sale tag (Kroger) | UPC `0001111091046`, `10 OZ`, `37.9¢`/`30.0¢` per ounce, `3.79`, `2/6.00`, `3.00 each`, date `11-26-24` | Filters leave `3.79`, `6.00`, `3.00`. There is still more than one "price": regular, multi-buy, and each. |
| Japanese apples | `2`個入, `1`パック, `498`円 (本体, before tax), `538`円 (税込, tax included) | No `¥` and no decimals. `円` and `税込` are invisible to the Latin OCR model. Which one is "the price" depends on whether you want before or after tax. |
| Japanese sake shelf | ~15 shelf labels (`478`–`928`), JAN codes, `2L`/`2.0` and a large `2` printed on cartons | Carton numbers are *larger* than shelf prices, so "tallest number wins" across the frame picks the wrong ones. |
| German e-ink tag | `2.99`, `1kg = 14.95€` | The tiny decimal dot is easy for OCR to miss. The per-kg price has its unit *before* the number, so the suffix filter misses it. |

A wider set of 17 tags from 14 countries, with expected prices, is in [test-images/](test-images/README.md).

### Rules we considered and set aside

- **Require a currency symbol or two decimals.** This works for US tags but rejects every Japanese price.
- **Tallest number in the frame wins.** Carton graphics beat shelf labels on the sake shelf.
- **Tallest number per ML Kit text block, plus decimal places from the From currency** (`java.util.Currency.defaultFractionDigits`: 0 means integers with 3+ digits, 2 means `x.xx`). This handles all four tags reasonably, but it is fragile, and every new country or layout needs new rules. It remains a candidate for a smarter Auto mode.

## Options

### 1. Aim box (implemented 2026-09-23)

Draw a rectangle in the middle of the preview and only use numbers inside it. Both modes behave as before, limited to the box. Not yet done: in Auto mode, converting only the largest number in the box instead of all of them.

- **Pros:** cheap (an overlay change plus one filter), offline, and it removes neighbouring tags, packaging and most fine print.
- **Cons:** the user has to aim. It does not resolve several prices on one tag (regular vs sale, per-kg).
- **Also useful for option 4:** it produces the crop we would send to a model.

### 2. More rules

- Keep the biggest number per tag.
- Skip numbers after `kg=`, `/100g`, `Grundpreis`.
- Only trust numbers seen in several frames in a row.
- Use the currency's decimal places.
- Use the Japanese OCR model (`text-recognition-japanese`) to read `円` and `税込`.

- **Pros:** offline, incremental, and unit-testable.
- **Cons:** accuracy is capped. Every new country or tag layout needs new rules.

### 3. On-device price-tag detector

Run an object detection model to find the tag region, then run our filters plus "biggest number wins" inside each tag.

- **Pros:** offline and fast once trained, and it removes non-tag numbers without the user aiming.
- **Cons:** it only finds the tag and doesn't pick the price within it. It needs a small, phone-sized model, which in practice means training our own (see "Existing models" below). Expect several days of ML work.

### 4. Vision AI model on a captured frame

On a "Capture" tap, send the frame, or better the aim-box crop, to a vision AI model (e.g. Claude Haiku/Sonnet or Gemini Flash). Ask it to return structured JSON: the shelf price, currency, and flags for unit, multi-buy and tax-included prices.

- **Pros:** by far the most accurate. It reads context the way a person does: `2/6.00`, `1kg = 14.95€`, 本体 vs 税込, sale vs regular. It works for any country without new rules. The most serious open project in this space uses exactly this (see below).
- **Cons:**
  - Needs a network connection and adds 1–3 s of latency.
  - Every scan costs money.
  - Photos leave the device, which needs a privacy note.
  - Needs a small backend to hold the API key, since keys can't ship in the app.
  - Suits a capture button, not live scanning.

### 5. On-device AI (Gemini Nano)

Same idea as option 4, but on the phone.

- **Pros:** no network, no per-scan cost, and private.
- **Cons:** only runs on a limited set of recent devices. Support and image input need checking before committing.

## Existing models

| Model | What it does | Notes |
|---|---|---|
| [openfoodfacts/price-tag-detection](https://huggingface.co/openfoodfacts/price-tag-detection) | Locates price tags (does not read them) | YOLOv11x, 960×960, ONNX. Precision 0.94 / recall 0.90. Trained on Open Prices photos. **AGPLv3**, too heavy for live mobile use. |
| [openfoodfacts/price-tag-extractor](https://huggingface.co/openfoodfacts/price-tag-extractor) | Reads price data from a tag image | Qwen3-VL-8B LoRA fine-tune. Server-only (8B parameters). On par with Gemini 2.5 Flash, below Gemini 3 Flash. |
| [Roboflow: price_tag (nimes)](https://universe.roboflow.com/nimes/price_tag-kvox2) | Locates price tags | 747 images, mAP@50 97%. License per project. |
| [Roboflow: prices (priceDetect)](https://universe.roboflow.com/pricedetect/prices-zrttt) | Separates price, discount price, weight and barcode | The right idea for our problem, but weak: mAP@50 69%, recall 59%. |
| [Roboflow: Shelf Tag Label Assist](https://universe.roboflow.com/shelf-tag-scanning/shelf-tag-label-assist) | Locates shelf tags | YOLOv8m, 127 images. |
| [bebbieyin/Price-Tag-Data-Extraction](https://github.com/bebbieyin/Price-Tag-Data-Extraction) | YOLOv7 finds price and barcode, then OCR reads them | Demo only (42 training images). |

**Key takeaway:** [Open Prices](https://blog.openfoodfacts.org/en/news/open-prices-200000-prices-and-beyond) has thousands of labelled tags. Even so, it uses a detector only to find tags and a vision AI model (Gemini 3 Flash in production) to read them. No off-the-shelf model reads prices on-device.

### License caution

This repo is MIT-licensed. Models trained with Ultralytics YOLO, including the Open Food Facts detector, are AGPLv3 unless you buy an Ultralytics commercial license. Bundling one would require releasing the app under AGPL. If we train our own detector, use a framework without that condition, and check the dataset's license too.

## Recommendation

1. **Done:** the aim box (option 1). Next, measure how much accuracy is left to gain, ideally with an on-device evaluation against [test-images/](test-images/README.md).
2. **If accuracy across countries matters:** add a "Capture" button that sends the aim-box crop to a vision AI model (option 4), and keep live OCR as the offline fallback.
3. **Only if this becomes a core feature:** train a small on-device detector (option 3) on the Open Food Facts dataset, subject to its license.
