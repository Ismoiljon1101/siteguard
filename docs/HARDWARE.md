# Budget hardware and assembly plan

The team has one helmet and a **KRW 100,000 total budget**. The team lead reports ordering the November 14 component selection. The last reviewed checkout was **KRW 40,142 with free shipping**; payment and delivery have not been independently verified. Exact options, quantities, spares, and owned/borrowed tools are in [SHOPPING_LIST.md](SHOPPING_LIST.md).

## Minimal physical build

| Part | Installed quantity | Function |
| --- | ---: | --- |
| ESP32-S3-WROOM-1 N16R8, dual USB-C, 44-pin DevKitC-1-style board, soldered headers | 1 | Sampling, USB, Wi-Fi/MQTT, onboard RGB, and independent local alarm control |
| AHT20 + BMP280 combined breakout | 1 | AHT20 ambient temperature/humidity; pressure is available but unnecessary for baseline |
| LIS3DH Blue breakout | 1 | Acceleration and controlled sudden-motion demonstration |
| 3.3-5 V active-low buzzer module with driver | 1 | Local audible alarm; four remaining pieces of its pack are spares |
| PBS-33B momentary button, red, 20 cm leads | 1 | Accessible external SOS input on a verified non-strapping GPIO |
| Existing USB data cable; borrowed/reused power bank for portable work | 1 each when needed | Wired integration, then portable Wi-Fi demonstration |
| Small breadboard and jumpers; 5x7 cm soldering board for final interconnects | As needed | Bench prototyping, then secure connections |
| Insulation, insulated backing, removable mounting, and strain relief | Fit to actual parts | Separate conductors and secure the assembly; these supplies remain to be checked |

There are three main electronic boards, one buzzer module, and one external button in the installed build. Keep one spare ESP32 and one spare LIS3DH disconnected. No spare environmental sensor is included. The user owns an iron, solder, USB cables/charger, and an apparent resistor assortment; a friend will lend a multimeter. A new meter, iron, power bank, enclosure, or mounting roll was not included in the final eight purchase lines.

No separate display, radio, external indicator LEDs, LED resistor kit, GPS, gas sensor, camera, or full starter kit is needed.

## Features retained

Java still provides live readings, environmental rules, SOS/motion incidents, acknowledgement/resolution/notes, SQLite history, charts, CSV, simulation/replay, freshness, sensor faults, and serial/MQTT integration. Firmware samples sensors and triggers color/sound alarms without Java or network. All features remain planned until tested.

Later tilt/posture-change or sustained-stillness rules can use the existing acceleration vector without a gyro. They need calibration and tests; stillness does not establish unconsciousness, and neither rule is a certified fall detector. Defer these optional rules until the baseline works. Do not substitute accelerometer die temperature for ambient temperature/humidity.

## Board-specific controls and power

- This is **ESP32-S3**, not the earlier 38-pin ESP32-WROOM-32 board. Select the S3 target, flash/PSRAM settings, and actual USB interface. Do not reuse the old pin map.
- Photos show an addressable onboard RGB LED. Use it for normal/warning/fault states after verifying its GPIO against the received board revision. A power LED alone is not a controllable alarm.
- Use the external **PBS-33B** switch for baseline SOS. Verify that it is normally open and momentary, select a suitable non-strapping GPIO with an appropriate pull-up, and debounce presses. Record the actual pin and wiring in A00/A08. No exact pin is frozen here. Test startup and reset with the button both released and held.
- Keep BOOT for programming unless the team deliberately validates an additional runtime input. Holding its GPIO0 low during reset can enter the bootloader. RESET is not SOS; the external SOS control must remain accessible after mounting.
- The buzzer is **active**, with a driver and low-level trigger. Set a safe inactive startup state, then switch its input for alarms. Do not carry over the DFR0032 PWM assumption. Confirm pin order/circuitry; start at 3.3 V supply/logic, with no 5 V pull-up reaching a GPIO.
- Supply the development board through its documented USB input. GPIO/sensor signals remain 3.3 V. Check power paths before combining external and USB supplies.
- Start with laptop USB and an existing data-capable cable. For portable demonstration, borrow/reuse a suitable power bank and carry it separately with cable strain relief rather than putting a heavy battery on the helmet. Test runtime and idle shutoff.

## Assembly order

1. Count received parts and separate spares. Record board markings, USB interface, RGB GPIO, libraries/toolchain settings, and a pin table before wiring.
2. Solder the loose sensor headers with power disconnected. Inspect for bridges; use the borrowed meter for supply/short checks.
3. Upload a minimal S3 program with no external components attached; verify serial output and controllable RGB.
4. Check the external SOS switch with continuity while disconnected. Wire it to the verified input and test debounce, startup, press/hold/release, and local event generation.
5. Read AHT20 over I2C at 3.3 V. Report faults; keep it exposed to ambient air away from controller heat.
6. Add LIS3DH on the shared I2C bus; confirm addresses, pull-up voltage, units, range, and measured sampling rate. Motion sampling is faster than dashboard telemetry.
7. Add the driven active buzzer. Test inactive startup and local SOS/warnings/reset without Java.
8. Complete USB incident integration, then MQTT. The purchasing change does not alter protocol units, schema, or source identity.
9. Move validated connections from the bench breadboard to suitable soldered interconnects. Inspect and repeat electrical/function checks before mounting.
10. Fit insulated backing and a suitable removable enclosure/mount. Check clearance, ventilation, visible RGB, accessible SOS, insulation, and strain relief. Do not claim weatherproofing. Do not drill/cut the helmet or disturb its suspension.

A breadboard is for bench work. Secure final interconnects so they cannot detach in the controlled unworn demo. Spare boards and buzzers stay disconnected. Investigate any wiring/supply/soldering fault before replacing a failed part.

## Tools and demonstration limits

Follow the borrowed meter's actual manual. Use COM and the voltage/resistance socket with an appropriate DC-voltage range for supply checks. Resistance/continuity checks require power off. Current measurement uses the correct fused input/range and a series connection; never connect a current-mode meter across a supply. Return the lead to the voltage/resistance socket afterward.

Use the owned iron and electronics solder after checking their condition and ratings. Borrow cutters, tweezers, flux, or a lab current-limited supply when needed. Inspect the reported 1/4 W, 1% assortment before assuming resistor values or using it in a circuit. No additional resistor purchase is currently required.

Use low-voltage circuits and an unworn helmet for movement tests. Do not strike a person's helmet. Ambient readings do not establish safe working conditions; acceleration cannot establish falls, injury, helmet-wearing compliance, or worker condition. Record actual test conditions and limits.

## References

- [Espressif S3 reference-board guide](https://documentation.espressif.com/esp-dev-kits/en/latest/esp32s3/esp32-s3-devkitc-1/user_guide_v1.1.html)
- [AHT20 guide](https://learn.adafruit.com/adafruit-aht20)
- [ST LIS3DH](https://www.st.com/en/mems-and-sensors/lis3dh.html)

Follow documentation for the received board. Reference guides do not certify a clone's pinout or protection.
