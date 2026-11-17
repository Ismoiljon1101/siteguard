# Hardware order and budget

Updated November 14, 2026. The team lead reports that the components have been ordered. The table records the final reviewed AliExpress selections; the order receipt, charged amount, shipment status, and delivery dates have not been independently verified. Do not order these components again from this document.

The budget is **KRW 100,000 total**. The last verified checkout total was **KRW 40,142, including free shipping**, leaving **KRW 59,858** against that estimate. This replaces the November 13 twelve-item cart. Tools the team owns or can borrow are excluded.

## Final selections

Prices below are the settled combined-cart prices, not separate product-page welcome offers. Generic modules are identified by product ID and exact option because retailer titles do not establish a manufacturer or board revision.

| Item | Exact model / selected option | Cart quantity | Unit price (KRW) | Line total (KRW) | Product |
| --- | --- | ---: | ---: | ---: | --- |
| Controller | Veeket ESP32-S3-WROOM-1 **N16R8**, dual USB-C, 44-pin DevKitC-1-style board; **N16R8 Pin Soldered** | 2 | 9,860 | 19,720 | [1005009330328373](https://ko.aliexpress.com/item/1005009330328373.html) |
| Environmental sensor | **AHT20 + BMP280** combined I2C breakout; **1pcs** | 1 | 2,519 | 2,519 | [1005006825767164](https://ko.aliexpress.com/item/1005006825767164.html) |
| Motion sensor | **LIS3DH Blue** I2C/SPI breakout; not LIS3DSH Purple | 2 | 2,220 | 4,440 | [1005006579261261](https://ko.aliexpress.com/item/1005006579261261.html) |
| Audible alarm | **3.3-5 V, low-level-trigger active buzzer module**, three pins, blue PCB with driver components; **5PCS / China Mainland** | 1 pack | 3,821 | 3,821 | [1005009441125116](https://ko.aliexpress.com/item/1005009441125116.html) |
| Breadboard | **Mb102 400 hole / China Mainland** | 1 | 2,700 | 2,700 | [1005013052850324](https://ko.aliexpress.com/item/1005013052850324.html) |
| Jumper wires | **120PCS X1 set 10CM / China Mainland**; male/male, male/female, female/female | 1 set | 1,494 | 1,494 | [1005013052850324](https://ko.aliexpress.com/item/1005013052850324.html) |
| External SOS button | **PBS-33B**, momentary/self-reset switch with prewired leads; **red 20cm Line / China Mainland** | 1 | 2,220 | 2,220 | [1005009203401227](https://ko.aliexpress.com/item/1005009203401227.html) |
| Soldering boards | Double-sided prototype PCB; **5x7cm 5pcs** | 1 pack | 3,228 | 3,228 | [1005007977006793](https://ko.aliexpress.com/item/1005007977006793.html) |
| **Last reviewed checkout total** | **Eight selected product lines; free shipping** | | | **40,142** | |

AliExpress applied its one-product new-member discount to the jumper set in this selection. Do not assume the same welcome price applies to every product. The paid receipt, once checked by the team, takes precedence over these observations. Keep receipts, addresses, payment details, customs codes, and private tracking links outside the public repository.

## Installed parts and spares

The helmet uses **one ESP32, one AHT20/BMP280 board, one LIS3DH, one buzzer, and one external SOS button**. The breadboard and jumpers support bench testing; the soldering boards support secure final connections.

- Keep the second ESP32 and second LIS3DH disconnected as spares.
- The buzzer pack provides one installed module and four spares.
- The five prototype boards are assembly supplies; they are not five additional devices.
- The environmental board quantity is one. No spare environmental sensor was retained.

The ESP32 and LIS3DH spares are practical backups for the controller and motion subsystem, not a measured ranking of failure rates. Wrong voltage, short circuits, or solder bridges can damage any board. Find the fault before connecting a replacement.

## Already owned or borrowed

| Item | Availability / remaining check |
| --- | --- |
| Helmet | Owned; verify removable mounting and suspension clearance |
| Soldering iron and solder | Owned; inspect the iron, joints, and solder suitability |
| USB cables and charger | Owned; verify one cable carries data and the supply provides ordinary 5 V USB power |
| Multimeter | A friend agreed to lend one; inspect the actual model, probes, batteries, and manual |
| 600-piece, 1/4 W, 1% assortment | Reported owned; markings suggest resistors, but the package has not been inspected |
| Glue / attachment materials | Some materials are owned; suitability for the helmet shell and insulated mounting is unverified |

No additional resistor kit, soldering kit, meter, or programming cable purchase is currently required. Existing stock still needs checking before assembly.

## Remaining assembly supplies and power

Insulate solder joints and provide a rigid insulated backing with strain relief. Heat-shrink tubing was investigated but not added because the small selected option could not ship to Korea. Source suitable insulation locally or from a verified listing; no price is included here.

A plastic enclosure and mounting roll from the older cart were not part of the final eight selections. Choose a suitable removable mounting arrangement after checking the actual component fit. Do not assume glue is compatible with the helmet, drill/cut its shell, or obstruct the suspension.

Start on laptop USB. A USB power bank was not included in the final selections; borrow or reuse a suitable bank for the portable demo. Carry it separately with cable strain relief and test idle shutoff/runtime. The owned charger supports a tethered bench setup.

No separate display, GPS, gas sensor, camera, radio module, external indicator LEDs, or large starter kit is required for the baseline.

## Delivery status and arrival checks

The order was reported on November 14. Dispatch and arrival have not been verified. Previously observed product-page estimates ranged from **November 16-20** for the sensors/buzzer to **November 19-24** for breadboard, jumpers, and prototype boards. The controller and SOS listing showed an as-fast-as-three-days badge; that is not a delivery guarantee. Use the actual order page for current estimates.

- [x] Final selections, quantities, and combined checkout estimate recorded.
- [x] Team lead reports ordering the components.
- [ ] Check the receipt, final charged amount, quantities, and delivery estimates privately.
- [ ] Count delivered parts; keep the spare ESP32 and LIS3DH separate.
- [ ] Verify the clone's revision, RGB GPIO, USB interface, flash/PSRAM settings, and pin map.
- [ ] Solder loose sensor headers with power disconnected; inspect joints and supply rails.
- [ ] Verify buzzer pin order, onboard driver, 3.3 V operation, and safe inactive startup.
- [ ] Verify the external SOS switch is normally open and momentary using continuity, then use a suitable non-strapping GPIO with debounce.
- [ ] Bring up one component at a time, then check insulation, mounting, and portable power.

Ordering parts does not pass A00/A08 or demonstrate a working feature. Follow [HARDWARE.md](HARDWARE.md) and the stage gates before integration.

## Technical references

- [Espressif S3 reference-board guide](https://documentation.espressif.com/esp-dev-kits/en/latest/esp32s3/esp32-s3-devkitc-1/user_guide_v1.1.html): reference layout and controls, not proof of the clone's exact revision.
- [AHT20 guide](https://learn.adafruit.com/adafruit-aht20): temperature/humidity sensor guidance; verify the combined breakout separately.
- [ST LIS3DH](https://www.st.com/en/mems-and-sensors/lis3dh.html): accelerometer specifications.

Retailer photos and non-AI specifications identify the selected generic modules. Listing summaries do not establish authenticity, pin compatibility, certification, or performance. Verify received hardware before wiring.
