# Hardware shopping list

Prepared on November 12, 2026. Nothing in this document means an order has been placed. Quantities below support one physical helmet plus replacement parts for wiring or soldering mistakes. Ismoiljon coordinates the order; Jakhongir checks the board variants and pin labels before assembly.

## Electronics and spare quantities

The model or retailer option below is the selection to order. For generic boards, the retailer product and option identify the selection more precisely than a generic chip name. Save the selected option and the board's actual markings when it arrives; do not assume it is an official manufacturer development kit.

| Item and exact selection | Used in prototype | Spare | Buy total | Ordering details |
| --- | ---: | ---: | ---: | --- |
| ESP32 USB-C DevikitC V, 38-pin, HiGenesis listing | 1 | 1 | 2 | [Coupang product 8756951445](https://www.coupang.com/vp/products/8756951445?itemId=25459906348&vendorItemId=92529471727). Choose the same board twice, with soldered headers. This listing says C-to-C is unsupported; use USB-A-to-C. |
| AHT20 + BMP280 combined I2C breakout, soldered UP option (`납땜(위)`) | 1 | 1 | 2 | [Coupang product 9057829063](https://www.coupang.com/vp/products/9057829063?itemId=26590482728&vendorItemId=93563768858). Temperature/humidity come from the AHT20; the extra pressure sensor does not add a project requirement. |
| LIS3DH breakout, `01 LIS3DH Blue`, `02 1PCS` | 1 | 1 | 2 | [Coupang product 9681200821](https://www.coupang.com/vp/products/9681200821?itemId=28949599557&vendorItemId=95880245475). Loose headers require soldering. Do not select LIS3DSH Purple. |
| DFRobot Gravity Digital Buzzer, **DFR0032**, with Gravity digital cable | 1 | 1 optional | 1 required; 2 preferred | [Manufacturer product](https://www.dfrobot.com/product-84.html), [wiring documentation](https://wiki.dfrobot.com/dfr0032/). Manufacturer describes 3.3 V controller compatibility. This is a passive buzzer: firmware must generate a tone/PWM. Purchase the module and cable, not a bare buzzer. A local retailer and KRW price remain to be confirmed. |
| SPEEDY Slim Dual 10,000mAh power bank, **BND-2PCPPB100A**, black | 1 | 0 | 1 | [Coupang product 7869821820](https://www.coupang.com/vp/products/7869821820?itemId=22087730176&vendorItemId=89097924928). Use its 5 V USB output to power the development board; check idle shutoff during bench testing. |
| USB-A-to-USB-C data cable, **S73AR**, 50 cm, silver, right-angle, 10 Gbps listing | 1 | 0 | 1 | [Coupang product 9593626832](https://www.coupang.com/vp/products/9593626832?itemId=28638783388&vendorItemId=95887869253). A known-good existing A-to-C data cable can replace this purchase. The power-bank cable is not confirmed to carry data. |
| Male/male, male/female, female/female 2.54 mm Dupont jumper set | As needed | Remaining wires | 1 mixed set | [Coupang product 9314432696](https://www.coupang.com/vp/products/9314432696?itemId=27602203617&vendorItemId=94565475623). Check that the option includes all three types rather than only one type. |
| Breadboard/component starter kit, Bridge listing | 1 set | Remaining components | 1 | [Coupang product 9305785170](https://www.coupang.com/vp/products/9305785170). Check contents below before ordering separate packs. Kit contents and resistor values are not fully verified. |

Do not buy a second helmet, power bank, multimeter, or complete starter kit as replacement stock.

### Small parts: exact specifications

These common parts do not require a particular brand. The electrical value, package, and quantity are the purchase specification. Buy only what is missing from the kit or lab stock.

| Part | Working quantity | Purchase quantity/specification |
| --- | ---: | --- |
| Green indicator LED | 1 | 5 pieces, 5 mm through-hole, standard indicator LED |
| Red indicator LED | 1 | 5 pieces, 5 mm through-hole, standard indicator LED |
| LED series resistor | 2 | 10 pieces, **330 ohm, 1/4 W, axial through-hole**, one per LED |
| SOS button | 1 | 5 pieces, **6 x 6 mm, normally open, momentary tactile push button**, through-hole; confirm it fits the breadboard or perfboard |
| Final assembly perfboard | 1 | 2 pieces, **2.54 mm pitch, approximately 5 x 7 cm**; fit the enclosure before soldering |
| Enclosure and mounting | 1 set | Insulating plastic enclosure sized after a board fit check; removable hook-and-loop straps, cable ties, and heat-shrink insulation |

A generic enclosure cannot be given a reliable exact size until the chosen boards and interconnects are laid out. Do not order a case solely from the ESP32 dimensions. Do not drill the helmet shell or disturb its suspension.

## Shared tools

| Tool | Exact model or purchase specification | Quantity | Buy or borrow |
| --- | --- | ---: | --- |
| Digital multimeter | **UNI-T UT33D+**, including test leads and two AAA batteries | 1 | Borrow first; otherwise buy this exact model. It provides DC voltage, DC current, resistance, and audible continuity. [Official specifications](https://meters.uni-trend.com/product/ut33plus-series/). [Coupang search](https://www.coupang.com/np/search?q=UNI-T+UT33D%2B) is a search link, not a verified offer. |
| Temperature-controlled soldering iron | **HAKKO FX-600, 220 V version**, T18-B tip | 1 | Borrow from lab. If buying, [Coupang product 211942729](https://www.coupang.com/vp/products/211942729) is a candidate; confirm 220 V and seller authenticity. FX-600 also exists in incompatible 100/120 V versions. [Manufacturer](https://www.hakko.com/english/products/hakko_fx600.html). |
| Iron holder and tip cleaner | **HAKKO 633-01** holder or a lab holder suitable for the actual iron | 1 | Borrow with the iron. Do not assume the iron listing includes a stand. |
| Solder and flux | 0.6-0.8 mm rosin-core electronics solder; electronics-grade flux | 1 small spool/set | Borrow or share. Do not use plumbing solder or acid flux. |
| Desoldering braid | 2 mm copper desoldering wick | 1 roll | Borrow or buy for header solder bridges. |
| Cutter/stripper | Flush cutter and wire stripper suitable for 22-28 AWG wire | 1 each | Borrow or share; no premium model required for this build. |
| Bench power supply | Adjustable **5 V output with current limiting**, supplied and supervised by the lab | 1 shared | Borrow only; buying a bench supply is outside the baseline shopping list. |

The UT33D+ is the meter selection, not a claim that every listing using that name is accurate. One indexed Coupang listing described it as USB-rechargeable, conflicting with the manufacturer's two-AAA specification; do not rely on that description or its displayed price. Confirm the exact model, probes, batteries, seller, shipping, and final KRW total before ordering.

### First power-up and meter use

1. Keep the working spare set disconnected and label it. Do not use it to repeat an unexplained failure.
2. Unplug USB and the battery before changing wires or soldering. Check the actual module's VCC, GND, SDA, and SCL labels.
3. With power disconnected, inspect for solder bridges and check for an unexpected persistent short between supply and ground. Some capacitors can cause a brief continuity indication; investigate rather than assuming a beep proves a fault.
4. Put the black meter lead in COM and the red lead in the voltage/resistance socket. Use DC voltage mode, 20 V range on the UT33D+, for 3.3 V and 5 V checks. Connect the probes across the two points being measured.
5. Use the ESP32 board's USB input for its 5 V supply. Never connect 5 V to its 3.3 V pin or GPIO. Power the selected sensor breakouts at 3.3 V only after confirming their documentation; keep I2C pull-ups at 3.3 V.
6. Test the ESP32 alone, then one sensor, then the other sensor, then indicators and the buzzer. Stop and disconnect power if a component heats unexpectedly.
7. Measure resistance and continuity only with circuit power off. For current measurements, power off first and wire the meter in series using the correct fused input and range. Never put a meter in current mode directly across a supply. Return the red lead to the voltage/resistance socket afterward. Follow the meter manual's current limits.
8. If a board fails, find the wiring, supply, or soldering fault before connecting its spare. Record the cause and keep the damaged board separate.

These instructions cover the low-voltage prototype. They do not require measuring wall outlets or opening a power bank.

## Price evidence and budget

The figures below are snapshots from the earlier live browser checks on November 12, 2026, not current checkout quotations. Generic kit contents, final shipping, and new tool prices remain unresolved. Prices are per unit unless stated otherwise.

| Selection | Earlier Coupang price | Total with the planned quantity |
| --- | ---: | ---: |
| ESP32, 2 | KRW 8,400 | KRW 16,800 |
| AHT20 + BMP280, 2 | KRW 2,450 | KRW 4,900 |
| LIS3DH Blue, 2 | KRW 9,800 | KRW 19,600 |
| Starter kit, 1 | KRW 18,000 | KRW 18,000 |
| Jumper set, 1 | KRW 3,500 | KRW 3,500 |
| S73AR data cable, 1 | KRW 8,460 | KRW 8,460 |
| BND-2PCPPB100A power bank, 1 | KRW 8,990 | KRW 8,990 |
| **Priced electronics subtotal** | | **KRW 80,250** |

Add the selected DFR0032 buzzer(s), multimeter if not borrowed, shipping, missing small parts, and mounting materials. Allow KRW 5,000 for mounting as an estimate only. The AHT20 listing previously showed KRW 3,000 shipping; confirm the charge for two units at checkout. There is no verified complete order total yet. Borrow soldering equipment to avoid adding an expensive tool purchase to the project.

The three main spare boards add **KRW 20,650** to the one-of-each Coupang cost: ESP32 KRW 8,400 + AHT20/BMP280 KRW 2,450 + LIS3DH KRW 9,800. The optional spare buzzer is extra.

The earlier KRW 1,800 generic buzzer candidate is excluded from this subtotal because its 3.3 V control and driver details were not confirmed. It must not be treated as a verified substitute for DFR0032. Consequently, the earlier chat budget is not a complete quote for this revised list.

### Overseas alternatives already inspected

| Part | Listing and option | Earlier displayed price | Conditions |
| --- | --- | ---: | --- |
| LIS3DH | [AliExpress 1005006579261261](https://ko.aliexpress.com/item/1005006579261261.html), **LIS3DH Blue** | KRW 1,498 | Free shipping and November 18-23 shown; page limited this price to one unit per person. Search results also showed a new-member discount. Do not multiply this price by two or assume eligibility. |
| AHT20 + BMP280 | [AliExpress 1005006825767164](https://ko.aliexpress.com/item/1005006825767164.html), **1pcs** | KRW 2,319 | Free shipping and November 18-23 shown; verify the delivered board's header/soldering requirements. |
| ESP32 CH340, 30-pin USB-C with jumpers | [Temu 607488443296880](https://www.temu.com/kr/g-607488443296880.html) | KRW 8,034 in search | Product details were blocked by a security CAPTCHA. This differs from the selected 38-pin board; it is not approved as an interchangeable replacement. Delivery and exact option remain unverified. |

Do not count a first-order promotion several times across a basket. Compare the actual quantities, eligible discounts, delivery, and shipping before selecting a supplier. Keep the main and spare ESP32 boards the same variant to avoid rewiring during a demonstration.

## Order and arrival checklist

- [ ] Team checks existing lab stock and tools before paying.
- [ ] ESP32 main and spare are the same 38-pin board with soldered headers.
- [ ] AHT20 option is soldered; LIS3DH option is Blue, not LIS3DSH Purple.
- [ ] Buzzer is DFR0032 with its cable; firmware owner records the PWM/tone requirement.
- [ ] Kit contents cover LEDs, 330 ohm resistors, and the SOS button, or the missing packs are added.
- [ ] Multimeter is UT33D+ with probes and batteries; soldering iron, if purchased, is 220 V.
- [ ] Record final KRW total and delivery estimates for every seller; no order is implied by this document.
- [ ] Photograph received boards, record markings, check pin labels, and bench-test one component at a time.
- [ ] Label tested working boards and keep the spare set disconnected.
