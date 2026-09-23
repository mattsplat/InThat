# Price tag test images

Real-world price tags from 14 countries for testing the price scanner. Each entry lists the price(s) a person would read and the numbers that are *not* the price (the traps).

Images are from [Wikimedia Commons](https://commons.wikimedia.org/wiki/Category:Price_tags_by_country), resized to 1600 px on the long edge, a realistic camera frame size.

> **Licensing:** these images are **not** covered by the repo's MIT license. Each keeps its own license, listed below. CC BY and CC BY-SA require the attribution in this file to stay with the images. If you modify one, CC BY-SA requires the modified image to use the same license.

## Images

| File | Country | Currency | Expected price(s) | Traps (not the price) |
|---|---|---|---|---|
| [`uk-muji-multicurrency.jpg`](uk-muji-multicurrency.jpg) | UK | EUR / GBP / SEK | €14.95, £9.95, 159 kr (one tag, three currencies) | EAN `4550182132021`, codes `U9SC290` `CA59077`, size `L` |
| [`australia-eink-unit-price.jpg`](australia-eink-unit-price.jpg) | Australia | AUD | $11 ea | Was `$16`, date `03/05/16`, unit price `$0.25/100 Sheets`, `24pk`, codes `580263` `11395`. No decimals on the price |
| [`canada-pharmacy-shelf.jpg`](canada-pharmacy-shelf.jpg) | Canada | CAD | 11.99, 8.99 | UPCs `05650012315` `05780084875`, `062017`, DIN `02150336`, `500 mg`, `100` tablets on the boxes |
| [`france-carrefour-weighed.jpg`](france-carrefour-weighed.jpg) | France | EUR | 2,79€ | Per-kg price `8,90€` ("Prix/kg"), weight, dot dates `10.12.11` `21.12.11`, barcode, `69003`, `28%`. Label is rotated 90° |
| [`germany-butcher-window.jpg`](germany-butcher-window.jpg) | Germany | EUR | 1,15 · 0,59 · 0,99 · 0,90 (per 100 g), 3,98 (per kg) | Comma decimals; `100g`/`kg` units sit away from the number; five signs in one frame |
| [`sweden-eink-shelf.jpg`](sweden-eink-shelf.jpg) | Sweden | SEK | 16.95 kr, 15.95 kr (LCD, cents raised) | Comparison price `458.11` kr/kg ("Jämförpris"), EANs, `37 g`/`45 g`, blurry background tags |
| [`czech-penny-discount.jpg`](czech-penny-discount.jpg) | Czech Republic | CZK | 33.90 Kč, 10.90 Kč | Crossed-out `43.90` / `19.90`, unit prices `1 kg = 110.58` / `1 kg = 85.39` / `1 l = 10.90`, `-23%` `-45%`, dot dates `25.07.2024`, `397 g`, `1,5%` |
| [`ukraine-superscript-kopecks.jpg`](ukraine-superscript-kopecks.jpg) | Ukraine | UAH | 6.57, 5.18, 4.95 грн (superscript kopecks, no dot) | EANs under each price, bottle labels |
| [`russia-superscript-kopecks.jpg`](russia-superscript-kopecks.jpg) | Russia | RUB | 549.99, 61.49, 77.99 ₽/кг (superscript kopecks, no dot) | Box print `18.4`, size/count table `0`–`6` / `18`–`40`, `1кг`. Every price here is per kg |
| [`brazil-butcher.jpg`](brazil-butcher.jpg) | Brazil | BRL | 14.98, 9.49, 15.48 (×3), 18.98 (×2), 3.99, 5.99 (×2) | Tags tilted at many angles; no `R$` shown; ten prices in one frame |
| [`south-korea-costco.jpg`](south-korea-costco.jpg) | South Korea | KRW | 18,990원 | Item number `597861`, unit price `₩1,266/100G`, `1.5KG`, thermostat `3.8`, `3개` |
| [`japan-shelf-labels.jpg`](japan-shelf-labels.jpg) | Japan | JPY | 1180円, 1480円, 598円 | Huge `0.03`/`003` on packaging (bigger than the prices), `1000`, `#1000`, `12個`, `2個パック`, JAN codes `4970502023…`, `3BL 015` |
| [`japan-before-tax.jpg`](japan-before-tax.jpg) | Japan | JPY | 248円 (本体価格, before tax) | Before-tax price; tax-included price is not shown |
| [`taiwan-eink-sale.jpg`](taiwan-eink-sale.jpg) | Taiwan | TWD | 99元 (促銷價, sale) | Crossed-out original `112`, `180g`, `62768`, `ISO567-1-01@1@4` |
| [`taiwan-barcode-tag.jpg`](taiwan-barcode-tag.jpg) | Taiwan | TWD | NT$899 | `608154`, `1211`, `2072469`, model `WD8907-4G`, `MP3`, `4G` |
| [`taiwan-member-price.jpg`](taiwan-member-price.jpg) | Taiwan | TWD | $570 (售價, regular), $540 (會員價, member) | Handwritten digits; two valid prices |
| [`turkey-cheese-shop.jpg`](turkey-cheese-shop.jpg) | Turkey | TRY | 8.00, 7.50, 9.50, 8.50 TL (superscript kuruş) | Market signs at angles behind glass, reflections |

## What this set covers

- **Decimal styles:** dot (`11.99`), comma (`2,79`), none (`18,990`, `248`), and raised cents with no dot at all (Ukraine, Russia, Sweden, Turkey)
- **Currency markers:** before (`$`, `NT$`), after (`€`, `Kč`, `円`, `원`, `元`, `грн`, `TL`), a separate unit line (`₽/кг`), or nothing (Brazil)
- **Several prices on one tag:** sale vs. was (Czech, Taiwan e-ink, Australia), regular vs. member (Taiwan), three currencies (Muji)
- **Unit and comparison prices:** per kg, per 100 g, per 100 sheets (France, Czech, Korea, Sweden, Australia)
- **Numbers larger than the price:** packaging (Japan `0.03`), shelf-wide scenes with many tags (Brazil, Germany)
- **Hard OCR:** e-ink/LCD segment fonts, handwriting, rotated labels, reflections through glass

## Sources and attribution

| File | Author | License | Source |
|---|---|---|---|
| `uk-muji-multicurrency.jpg` | Tbatb | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Tag of a Muji clothing product in the UK 03.jpg](https://commons.wikimedia.org/wiki/File:Tag_of_a_Muji_clothing_product_in_the_UK_03.jpg) |
| `australia-eink-unit-price.jpg` | Maksym Kozlenko | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [2021-06-28 E-ink price tag in Sydney supermarket.jpg](https://commons.wikimedia.org/wiki/File:2021-06-28_E-ink_price_tag_in_Sydney_supermarket.jpg) |
| `canada-pharmacy-shelf.jpg` | Daniel Case | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Bayer Aspirin and store-brand generic on Canadian drugstore shelf.jpg](https://commons.wikimedia.org/wiki/File:Bayer_Aspirin_and_store-brand_generic_on_Canadian_drugstore_shelf.jpg) |
| `france-carrefour-weighed.jpg` | Alexmar983 | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Emmental de Savoie Carrefour.jpg](https://commons.wikimedia.org/wiki/File:Emmental_de_Savoie_Carrefour.jpg) |
| `germany-butcher-window.jpg` | Monstourz | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [DEU Fleischerei Preisschilder Bregenwurst ua MSZ100108.jpg](https://commons.wikimedia.org/wiki/File:DEU_Fleischerei_Preisschilder_Bregenwurst_ua_MSZ100108.jpg) |
| `sweden-eink-shelf.jpg` | Franklin Heijnen | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [Jars of Chili, with electronic shelf tags.jpg](https://commons.wikimedia.org/wiki/File:Jars_of_Chili,_with_electronic_shelf_tags.jpg) |
| `czech-penny-discount.jpg` | ŠJů, the photographed label by Penny | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0) | [Akční mléko Penny.jpg](https://commons.wikimedia.org/wiki/File:Ak%C4%8Dn%C3%AD_ml%C3%A9ko_Penny.jpg) |
| `ukraine-superscript-kopecks.jpg` | Maksym Kozlenko | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Classic Pepsi bottles in supermarket in Kyiv.JPG](https://commons.wikimedia.org/wiki/File:Classic_Pepsi_bottles_in_supermarket_in_Kyiv.JPG) |
| `russia-superscript-kopecks.jpg` | Stolbovsky | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Cucurbita moschata bell group.jpg](https://commons.wikimedia.org/wiki/File:Cucurbita_moschata_bell_group.jpg) |
| `brazil-butcher.jpg` | Wilfredor | [CC0](http://creativecommons.org/publicdomain/zero/1.0/deed.en) | [Boucherie in São Paulo City.jpg](https://commons.wikimedia.org/wiki/File:Boucherie_in_S%C3%A3o_Paulo_City.jpg) |
| `south-korea-costco.jpg` | Pppjim | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Yangcheon costco 20181105 120006.jpg](https://commons.wikimedia.org/wiki/File:Yangcheon_costco_20181105_120006.jpg) |
| `japan-shelf-labels.jpg` | kanesue | Public domain | [Condoms 3 type.JPG](https://commons.wikimedia.org/wiki/File:Condoms_3_type.JPG) |
| `japan-before-tax.jpg` | nesnad | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0) | [Jack o lantern in Japan - October 2016.jpg](https://commons.wikimedia.org/wiki/File:Jack_o_lantern_in_Japan_-_October_2016.jpg) |
| `taiwan-eink-sale.jpg` | Saimmx | [CC0](http://creativecommons.org/publicdomain/zero/1.0/deed.en) | [20251203 201443 Interesting price tag - Nutella.jpg](https://commons.wikimedia.org/wiki/File:20251203_201443_Interesting_price_tag_-_Nutella.jpg) |
| `taiwan-barcode-tag.jpg` | Solomon203 | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [E-Life Mall WD8907-4G tag.jpg](https://commons.wikimedia.org/wiki/File:E-Life_Mall_WD8907-4G_tag.jpg) |
| `taiwan-member-price.jpg` | Solomon203 | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Animate Taipei Ximen price tag 20130615.jpg](https://commons.wikimedia.org/wiki/File:Animate_Taipei_Ximen_price_tag_20130615.jpg) |
| `turkey-cheese-shop.jpg` | Christopher from Shanghai, China | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Blocks of Soft Cheese (3922499333).jpg](https://commons.wikimedia.org/wiki/File:Blocks_of_Soft_Cheese_(3922499333).jpg) |
