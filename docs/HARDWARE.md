# Hardware and assembly plan

The team has one helmet. Check exact board variants and available tools before ordering. This list supports the baseline design; it is not a record of purchased parts. See the [shopping list](SHOPPING_LIST.md) for exact product selections, retailer links, spare quantities, tools, price evidence, and ordering checks. Only one physical helmet will be built; spare boards are replacement stock.

| Item | Quantity | Selection notes |
| --- | --- | --- |
| ESP32 development board | 2 total | One installed, one spare; matching 38-pin USB-C boards with soldered headers |
| AHT20 + BMP280 breakout | 2 total | One installed, one spare; soldered-header option; use the AHT20 for temperature/humidity |
| LIS3DH breakout | 2 total | One installed, one spare; select LIS3DH Blue, not LIS3DSH Purple; solder loose headers before use |
| DFRobot Gravity buzzer, DFR0032 | 1 + 1 optional spare | Use the module's power and signal connections and PWM/tone control |
| LEDs | 2 | Normal and warning indicators |
| 330 ohm resistors | 2 | One series resistor per LED |
| Momentary button | 1 | Manual SOS with debounce and an input pull-up |
| Breadboard and jumper wires | 1 set | Male/male and male/female connections for the actual modules |
| Matching USB data cable | 1 | Must support data, not only charging |
| USB power bank with 5 V output | 1 | May be borrowed; test stable operation and idle shutoff behavior |
| Enclosure, removable straps, and cable ties | 1 set | Insulation, strain relief, and mounting without drilling the helmet |

A laptop, a local Wi-Fi network, and basic assembly tools are also needed. Obtain or borrow one UNI-T UT33D+ multimeter with probes. Borrow a soldering iron, stand, solder, cutter, and current-limited bench supply from the lab where available. The [shopping list](SHOPPING_LIST.md#shared-tools) specifies tool models and consumables. A breadboard is for bench testing; choose a secure final interconnect method before mounting the prototype. Do not purchase additional gas sensors, GPS, displays, or cloud hardware for the baseline.

## Assembly order

1. Identify the exact ESP32 and USB interface. Upload a minimal program and confirm serial communication.
2. Test power and ground, then connect one sensor at a time. Confirm pin labels from the actual module documentation.
3. Read AHT20 values and report a missing-sensor fault rather than a fabricated measurement.
4. Test LIS3DH sampling and record the configured range, rate, units, and measured sample interval.
5. Add LEDs, the driven buzzer module, and the SOS button. DFR0032 is a passive buzzer module and needs PWM/tone generation, not just a constant output level. Verify debounce and alarms with no Java connection.
6. Record a wiring diagram and GPIO table, then complete serial integration before MQTT.
7. Test USB power-bank operation. Enclose and secure the electronics, checking cable strain and access to the button.

Do not finalize a GPIO diagram until the exact board and modules are confirmed. Firmware must use the same pin map documented in the repository. Check shared I2C wiring and pull-up voltage against the selected breakouts.

## Demonstration checks

Use low-voltage power and keep student circuits away from mains electricity. Do not drill, cut, or compromise the helmet shell or suspension. Perform movement tests with the helmet unworn; never strike a person's helmet to demonstrate an alarm.

Temperature and humidity are local ambient readings, not body temperature or a certified heat-risk assessment. A low-range accelerometer can show a controlled movement event but does not establish injury severity. Record the limits of the chosen test and sensor range.

## Manufacturer references

- [ESP32-DevKitC](https://docs.espressif.com/projects/esp-dev-kits/en/latest/esp32/esp32-devkitc/user_guide.html)
- [AHT20 guide](https://learn.adafruit.com/adafruit-aht20)
- [LIS3DH guide](https://learn.adafruit.com/adafruit-lis3dh-triple-axis-accelerometer-breakout)
- [DFRobot DFR0032](https://www.dfrobot.com/product-84.html)
- [UNI-T UT33+ series](https://meters.uni-trend.com/product/ut33plus-series/)

Follow the documentation for the module actually purchased. These references do not guarantee that every third-party breakout has the same regulator, pins, or voltage protection.
