# Budget hardware and assembly plan

The team has one helmet and a **KRW 100,000 total budget**. The November 13 redesign uses three main boards and one small alarm module. Exact cart variants, tools, prices, and delivery evidence are in [SHOPPING_LIST.md](SHOPPING_LIST.md). Items are in a cart, not purchased. This replaces the duplicate-board/starter-kit plan.

## Minimal physical build

| Part | Installed quantity | Function |
| --- | ---: | --- |
| ESP32-S3-WROOM-1 N16R8, dual USB-C, 44-pin DevKitC-1-style board, soldered headers | 1 | Sampling, USB, Wi-Fi/MQTT, onboard RGB, runtime BOOT-button SOS |
| AHT20 + BMP280 combined breakout | 1 | AHT20 ambient temperature/humidity; pressure is available but unnecessary for baseline |
| LIS3DH Blue breakout | 1 | Acceleration and controlled sudden-motion demonstration |
| 3.3-5 V active-low buzzer module with driver | 1 | Local audible alarm; four remaining pieces of its pack are spares |
| USB data cable and power bank | 1 each | Wired integration, then portable Wi-Fi demonstration |
| Small breadboard, jumpers, plastic case, hook-and-loop | As listed | Bench interconnect, insulation, removable mounting, strain relief |

No separate display, radio, external indicator LEDs, LED resistor kit, GPS, gas sensor, camera, or full starter kit is needed. The basket includes a meter and soldering kit because borrowing was not confirmed. Borrowing either reduces cost without removing a feature.

## Features retained

Java still provides live readings, environmental rules, SOS/motion incidents, acknowledgement/resolution/notes, SQLite history, charts, CSV, simulation/replay, freshness, sensor faults, and serial/MQTT integration. Firmware samples sensors and triggers color/sound alarms without Java or network. All features remain planned until tested.

Later tilt/posture-change or sustained-stillness rules can use the existing acceleration vector without a gyro. They need calibration and tests; stillness does not establish unconsciousness, and neither rule is a certified fall detector. Defer these optional rules until the baseline works. Do not substitute accelerometer die temperature for ambient temperature/humidity.

## Board-specific controls and power

- This is **ESP32-S3**, not the earlier 38-pin ESP32-WROOM-32 board. Select the S3 target, flash/PSRAM settings, and actual USB interface. Do not reuse the old pin map.
- Photos show an addressable onboard RGB LED. Use it for normal/warning/fault states after verifying its GPIO against the received board revision. A power LED alone is not a controllable alarm.
- Verify BOOT's GPIO0 connection, then debounce runtime SOS. Holding BOOT during power-up/reset can enter the bootloader; release it before restart/flashing. RESET is not SOS. Test startup, reconnect, press/hold/release, and access through the enclosure. Add an external button if access fails.
- The new buzzer is **active**, with a driver and low-level trigger. Set a safe inactive startup state, then switch its input for alarms. Do not carry over the DFR0032 PWM assumption. Confirm pin order/circuitry; start at 3.3 V supply/logic, with no 5 V pull-up reaching a GPIO.
- Supply the development board through its documented USB input. GPIO/sensor signals remain 3.3 V. Check power paths before combining external and USB supplies.
- Start with laptop USB. For portable demonstration, carry the power bank separately with cable strain relief rather than putting a heavy battery on the helmet. Test runtime and idle shutoff.

## Assembly order

1. Record board markings, USB interface, RGB/BOOT GPIOs, library/toolchain settings, and a pin table before wiring.
2. Solder the loose sensor headers with power disconnected. Inspect for bridges; use the meter for supply/short checks.
3. Upload a minimal S3 program; test serial, RGB, and BOOT SOS without Java.
4. Read AHT20 over I2C at 3.3 V. Report faults; keep it exposed to ambient air away from controller heat.
5. Add LIS3DH on the shared I2C bus; confirm addresses, pull-up voltage, units, range, and measured sampling rate. Motion sampling is faster than dashboard telemetry.
6. Add the driven active buzzer. Test inactive startup and local SOS/warnings/reset without Java.
7. Complete USB incident integration, then MQTT. The cost redesign does not change protocol units, schema, or source identity.
8. Bench-fit the 120 x 80 x 40 mm case. Check clearance, ventilation, visible RGB, accessible SOS, insulation, and strain relief. Do not claim weatherproofing. Mount removably without drilling/cutting the helmet or disturbing suspension.

A breadboard is for bench work. Secure final interconnects so they cannot detach in the controlled unworn demo. Spare buzzers stay disconnected. Investigate any wiring/supply/soldering fault before replacing a failed part; the basket contains no duplicate controller/sensor set.

## Tools and demonstration limits

Use the UT33D+ voltage/resistance socket and COM for DC voltage checks; its 20 V range covers 3.3 V and 5 V. Resistance/continuity checks require power off. Current measurement uses the correct fused input/range and a series connection; never connect a current-mode meter across a supply. Return the lead to the voltage/resistance socket afterward and follow the manual.

Use the selected **80W 220V EU / QH908S Set4** iron variant, not 110 V. Check its delivered holder/solder/accessories. Borrow cutters, tweezers, flux, or a lab current-limited supply when needed; no premium station or owned bench supply is required.

Use low-voltage circuits and an unworn helmet for movement tests. Do not strike a person's helmet. Ambient readings do not establish safe working conditions; acceleration cannot establish falls, injury, helmet-wearing compliance, or worker condition. Record actual test conditions and limits.

## References

- [Espressif S3 reference-board guide](https://docs.espressif.com/projects/esp-idf/en/v4.4.3/esp32s3/hw-reference/esp32s3/user-guide-devkitc-1.html)
- [AHT20 guide](https://learn.adafruit.com/adafruit-aht20)
- [ST LIS3DH](https://www.st.com/en/mems-and-sensors/lis3dh.html)
- [UNI-T UT33+ specifications/manual resources](https://meters.uni-trend.com/product/ut33plus-series/)

Follow documentation for the received board. Reference guides do not certify a clone's pinout or protection.
