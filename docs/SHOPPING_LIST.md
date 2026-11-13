# Budget hardware shopping list

Updated November 13, 2026. The team budget is **KRW 100,000 total**. The AliExpress selections below were added to the user's cart and reviewed in the live browser. **No order or payment was made.** This replaces the earlier duplicate-board/starter-kit list; Git history retains that plan.

## Exact cart selections

These are the settled cart line prices, not the lower product-page welcome offers. Each row is one cart unit. The buzzer unit is a five-piece pack; only one buzzer is installed. Generic modules are identified by product ID and exact option because a retailer title alone does not establish a manufacturer or board revision.

| Item | Exact model / selected option | Qty | Cart price (KRW) | Product |
| --- | --- | ---: | ---: | --- |
| Controller | Veeket ESP32-S3-WROOM-1 **N16R8**, dual USB-C, 44-pin DevKitC-1-style board; **N16R8 Pin Soldered** | 1 | 9,860 | [1005009330328373](https://ko.aliexpress.com/item/1005009330328373.html) |
| Environmental sensor | **AHT20 + BMP280** combined I2C breakout; **1pcs** | 1 | 2,544 | [1005006825767164](https://ko.aliexpress.com/item/1005006825767164.html) |
| Motion sensor | **LIS3DH Blue** I2C/SPI breakout; not LIS3DSH Purple | 1 | 1,494 | [1005006579261261](https://ko.aliexpress.com/item/1005006579261261.html) |
| Audible alarm | **3.3-5 V, low-level-trigger active buzzer module**, three pins, blue PCB with driver components; **5PCS pack** | 1 pack | 3,860 | [1005009441125116](https://ko.aliexpress.com/item/1005009441125116.html) |
| Breadboard | **Mb102 400 hole / China Mainland** | 1 | 2,700 | [1005013052850324](https://ko.aliexpress.com/item/1005013052850324.html) |
| Jumper wires | **120PCS X1 set 10CM / China Mainland**; male/male, male/female, female/female | 1 set | 3,940 | [1005013052850324](https://ko.aliexpress.com/item/1005013052850324.html) |
| USB programming/power cable | **UGREEN 3A Cable / 2 meter / Type-C to USB-A**, data-capable | 1 | 3,220 | [1005011626234920](https://ko.aliexpress.com/item/1005011626234920.html) |
| Multimeter | **UNI-T UT33D Plus / China Mainland** (UT33D+) | 1 | 16,639 | [1005007542901099](https://ko.aliexpress.com/item/1005007542901099.html) |
| Soldering kit | **JCD QH908S Set4 / China Mainland / 80W 220V EU** | 1 kit | 13,300 | [1005005412941239](https://ko.aliexpress.com/item/1005005412941239.html) |
| Enclosure | **120 x 80 x 40 mm plastic project box**, one piece | 1 | 2,765 | [1005010182782135](https://ko.aliexpress.com/item/1005010182782135.html) |
| Removable mounting | Double-sided hook-and-loop roll, **Black / 20mm 5meters** | 1 roll | 2,920 | [1005007429595389](https://ko.aliexpress.com/item/1005007429595389.html) |
| Portable power | **Baseus Bipow 2 Pro, 22.5 W, 10,000mAh, black_50189**; Korea-shipped listing | 1 | 21,900 | [1005013421628495](https://ko.aliexpress.com/item/1005013421628495.html) |
| **Item subtotal** | | | **85,142** | |
| Shipping | Power-bank seller; other selected lines showed free shipping | | **3,000** | |
| Additional cart discount | As shown, without assuming an unclaimed coupon | | **-27** | |
| **Estimated cart total** | **12 selected project cart units** | | **88,115** | |
| **Remaining budget** | Batteries, insulation, replacement/price contingency | | **11,885** | |

Unrelated pre-existing cart items were preserved and left unselected. Only the twelve project units above enter this total. Stock and promotions can change. The displayed pre-discount item total was KRW 101,486, so the full-price basket would exceed the cap once shipping is added. Recheck the actual checkout total before paying; do not proceed above KRW 100,000. Borrowing/reusing the multimeter, soldering kit, or power bank provides the largest additional savings.

## Delivery evidence

- Most imported components and tools showed **November 18-23** delivery on their product pages.
- ESP32-S3, meter, cable, and strap pages also showed an **as-fast-as-three-days** badge. It is not a confirmed delivery date.
- The Korea-shipped Baseus bank showed **November 15-18**, plus KRW 3,000 shipping.
- A cheaper-looking ESP32 listing was rejected: KRW 1,398 bought **only a case**, and the actual board option showed **December 28-January 12** delivery.
- Fast/free options were compared where displayed. No paid express upgrade or guaranteed delivery was verified. If parts are required before November 18, domestic sourcing may be necessary.

These are estimates observed on November 13, not dispatch or delivery confirmations. The cart has not been paid or submitted for delivery.

## What the redesign removes

| Earlier purchase | Replacement |
| --- | --- |
| KRW 18,000 component starter kit | One small breadboard and mixed jumper set |
| Two external LEDs and their resistors | Controller's onboard addressable RGB LED |
| Separate bench SOS button | Runtime BOOT button after GPIO/startup/access checks |
| Spare set of every board | One controller and one of each sensor; reserve cash rather than duplicating every part |
| Premium high-speed cable | Ordinary data-capable USB cable |
| DFR0032 passive module/proprietary cable | Active-low driven buzzer with ordinary jumpers |
| HAKKO iron and separate accessories | Entry-level 220 V soldering kit, or lab tools |
| Display, GPS, gas sensor, camera, separate radio | No purchase; use Java UI and controller Wi-Fi |

There are **three main boards** (controller and two sensor boards), plus **one small alarm module**. Tools, wiring, mounting, and power support the build. Removing the motion board would lose physical motion sensing; it is not the selected design.

## Spares and small supplies

The alarm pack gives one working module and four spares. **No spare ESP32, AHT20, or LIS3DH is in this basket.** If borrowing a tool or reusing a power bank frees money, the first spare to add is one identical **N16R8 Pin Soldered** controller. Recheck the entire basket after quantity changes; do not multiply one-unit promotional prices.

Both sensor headers are loose and require soldering. Set4 images show an iron, holder, sponge, tips, solder tube, desoldering pump, and pouch. Verify delivered contents. Borrow a flush cutter, tweezers, and electronics flux if needed rather than ordering another large kit. Do not use plumbing solder or acid flux.

Check whether the shipped meter includes its **two AAA batteries**. The manufacturer specifies two R03 cells; a cross-border seller may omit batteries. Keep the remaining budget for local batteries, insulation/tape/heat-shrink, small consumables, and price changes; those local prices are not quoted or included here.

BOOT SOS is for a controlled prototype after the checks in [HARDWARE.md](HARDWARE.md). If enclosure access is poor, add a cheap external normally-open momentary button to a verified non-strapping input. Do not hide the only SOS control to save a tiny button cost. An external button is not currently in the basket.

## Review and arrival checklist

- [x] Exact variants added, one cart unit per listed row.
- [x] All twelve project units selected; unrelated items unselected.
- [x] Displayed estimate including shipping below KRW 100,000.
- [ ] User checks final discounts, total, dates, and shipping details before payment.
- [ ] Verify controller RGB/button pins, USB interface, and board revision; a compatible clone is not an official Espressif board.
- [ ] Solder headers, inspect joints, and power/test one component at a time.
- [ ] Confirm buzzer pin order, onboard driver circuit, 3.3 V operation, and safe active-low startup.
- [ ] Check meter probes/batteries and kit contents; use the selected 220 V EU iron variant.
- [ ] Fit the case, keep SOS accessible and RGB visible, expose the environmental sensor to ambient air away from board heat.
- [ ] Insulate/secure connections and verify local alarms, freshness, and power-bank idle shutoff before mounting.

## Technical references

- [Espressif S3 reference-board guide](https://docs.espressif.com/projects/esp-idf/en/v4.4.3/esp32s3/hw-reference/esp32s3/user-guide-devkitc-1.html): reference layout/controls, not proof of a clone's exact revision.
- [ST LIS3DH](https://www.st.com/en/mems-and-sensors/lis3dh.html): active-production accelerometer; no extra gyro is needed for the baseline.
- [UNI-T UT33+ series](https://meters.uni-trend.com/product/ut33plus-series/): voltage/current, resistance, continuity, and two-AAA specification.

Retailer photos and non-AI specifications identify the selected generic modules. AI-generated listing summaries do not establish authenticity, pin compatibility, certification, or performance. Verify received hardware before wiring.
