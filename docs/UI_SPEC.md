# SiteGuard UI spec (Swing + FlatLaf)

Status: draft for TL review. Source of screens and status words: `docs/SITEGUARD_BUILD_BRIEF.md` Part 3. Every simulated item shows `SIM`. No Java is written here.

## 1. Principles

- **Status first.** Connection, SOS and storage state sit in fixed strips at the top and bottom of every screen (Nielsen heuristic 1, visibility of system status: [Decision Lab](https://thedecisionlab.com/reference-guide/design/nielsens-heuristics), [Human Standards](https://www.humanstandards.org/interaction-patterns/nielsen-heuristics/)).
- **Exceptions first.** Sort "Needs attention" puts open SOS on top, then OFFLINE, STALE, SENSOR FAULT, open warnings. Normal rows stay grey-neutral, so colour marks only abnormal states (ISA-101 exception-based display, [IMD guide](https://industrialmonitordirect.com/blogs/knowledgebase/industrial-hmi-design-color-contrast-and-alarm-best-practices), secondary source, verify against ISA-101.01).
- **Text plus colour.** Every status has a word and a glyph. Colour never carries meaning alone.

## 2. Colour tokens

Contrast computed by us (sRGB relative luminance, WCAG formula). Text must reach 4.5:1; non-text indicators and borders 3:1 ([WCAG 1.4.3](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html), [WCAG 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)).

Surfaces: light `#FFFFFF` (panel), `#F3F5F7` (page). Dark `#1E2228` (panel), `#15181C` (page).

| Token | Glyph | Light hex | Light on panel | Dark hex | Dark on panel |
|---|---|---|---|---|---|
| ONLINE | ● | `#1B7F3B` | 5.07 | `#5FD38D` | 8.51 |
| STALE | ◐ | `#8A5A00` | 5.93 | `#F2B84B` | 8.92 |
| OFFLINE | ○ | `#4A5260` | 7.87 | `#A3ADBA` | 7.03 |
| SENSOR FAULT | ⚠ | `#7A3E9D` | 6.99 | `#C9A0F0` | 7.43 |
| NO DATA YET | ◌ | `#2F5D8C` | 6.85 | `#8FB3E0` | 7.37 |
| UNASSIGNED | ▢ | `#3F4A56` | 9.03 | `#B8C2CC` | 8.84 |
| SOS HIGH | ■ | `#B3001B` | 7.16 | `#FF6B6B` | 5.76 |
| WARNING | ▲ | `#8F5200` | 6.22 | `#FFB020` | 8.73 |
| ESCALATION L1 (fill) | ▲ | `#C2410C`, white text 5.18 | | `#F97316`, dark text `#1E2228` 5.70 | |
| ESCALATION L2 (fill) | ■ | `#8B0000`, white text 10.01 | | `#FF5C5C`, dark text 5.28 | |

Other tokens (computed): accent `#1F5FBF` with white text 6.09 (light); `#7AA7FF` with dark text `#15181C` 7.46 (dark). Light border `#7A8492` 3.79 on panel. Dark border `#6B7480` 3.37 on panel. Secondary text light `#5A6470` 6.01; dark `#9AA4B1` 6.33. Focus ring uses the accent (6.09 / 7.46).

Glyphs are Unicode in Segoe UI Symbol. Check each glyph renders on the target machine; if not, drop the glyph and keep the word.

## 3. Type and spacing

- Font: Segoe UI via FlatLaf defaults (Windows). Sizes in points at 100% scaling: Caption 11, Body 13, Subhead 14 bold, Title 18 bold, Metric 28 bold (KPI tiles only).
- Spacing scale in px: 4, 8, 16, 24, 32. Panel padding 16. Gap between related controls 8. Gap between sections 24.
- Table rows 24 px (dense) at 1366x768; 28 px at 1920x1080.

## 4. Theme setup (FlatLaf)

- Use `FlatLightLaf` and `FlatDarkLaf`; a user setting chooses one at start. Both must pass the table above.
- Accent: `FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#1F5FBF"))` before setup. The bundled `FlatLightLaf.properties` names `@accentColor` as the variable to change ([FlatLaf properties](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/resources/com/formdev/flatlaf/FlatLightLaf.properties)). Status colours are not taken from the accent.
- Custom keys (`UIManager.put`) only for status tokens, read with `UIManager.getColor` ([FlatLaf customization](https://www.formdev.com/flatlaf/how-to-customize/)).
- Arc, font and key names were not verified on the official page. Check them against the FlatLaf version in use before coding.

## 5. Screens

- **Login:** centred card, 360 px wide. User name, password, Sign in (Enter submits). Error text: "User name or password is incorrect." No hint on which field is wrong.
- **Site overview:** top strip (site, shift, operator, clock). Escalation banner strip under it when an SOS is open (section 6). Left: zone list with counts (200 px). Right: worker table with sort, zone, trade filters and search; columns Worker, Helmet, Zone (operator-selected), Status, Air temp near helmet, Hum %, Updated, Inc. Hide Hum % and Updated below 1280 px. Bottom: open incidents list, then transport/DB/LIVE/SIM counts.
- **Worker detail:** header with status and "Zone (operator-selected): set 08:55 by name". Warn if zone was set before shift start. Chart "Air temp near helmet, last 10 min" with a text legend, no colour-only lines. Incident and assignment history tables. Buttons: Change zone, End assignment.
- **Incidents and detail:** filter bar; table on top (Time, Type, Severity, State, Level, Worker, Helmet, Unacknowledged for). Split pane below: receipt ID, history log (append-only), chart of stored readings 5 min before and after at 1 Hz with temperature, humidity and acceleration in three rows, not overlaid. Resolution combo and note field, then Acknowledge, Add note, Resolve. Resolve stays disabled until acknowledged, with a category and note; the reason is shown as text.
- **People and helmets:** tabs Workers, Helmets, Zones, Operators. Table with Helmet, Status, Firmware, Last seen, Assigned to. Assign, End assignment, Paste list (worker,helmet per line). Register and Retire only for SITE_MANAGER.
- **Shift report:** header filters (site, date, shift), KPI tiles in one row of four, then the detail table. Each figure opens its incident or helmet list on click. Export CSV.
- **Simulation panel:** start, stop, reset; one scenario per simulated helmet. Every simulated row, tile and chart legend shows `SIM`. A global `SIMULATED DATA` badge sits in the top strip while any simulator runs.

Layout at 1366x768: minimum window 1280x720; scroll the table, never the strips. Layout at 1920x1080: same structure, page size 50 rows instead of 25.

## 6. SOS and escalation

- One banner strip under the top bar, `SOS HIGH` token, text: "SOS Worker C, Level 2 East, open 00:42". Buttons Acknowledge and Open.
- Level L1 at 30 s unacknowledged, L2 at 120 s (timers are placeholders, D20). Escalation changes the strip fill (L1 orange, L2 dark red) and the text. The company overview shows L2.
- Pulse: the strip border flashes twice per second for 3 s on new SOS, then stays steady. Sound repeats per W4. A setting disables sound and pulse, never the text.
- Acknowledge does not silence the helmet buzzer. The UI says so.

## 7. Empty, loading, error, disconnected

| State | Shown as | Action |
|---|---|---|
| Empty | Centred text: "No workers assigned yet. Assign a helmet in People and helmets." | Button to open People and helmets |
| Loading | Progress bar in the table header, rows greyed; last values stay visible | None |
| Error (query) | Inline red text in the panel with the message and "Retry" | Retry |
| Storage fault | Top strip, SOS token: "Storage fault. Event not stored. Helmet alarm still running." | Retry; no receipt is sent |
| Disconnected (transport lost) | Bottom strip "Transport: lost since 09:12. Values frozen" and affected cards show STALE or OFFLINE | Reconnect (auto retry, visible countdown) |

Never show an old value as current. A lost link shows `--` after the status changes.

## 8. Keyboard shortcuts

| Key | Action |
|---|---|
| Ctrl+F | Focus search |
| Alt+1 / 2 / 3 / 4 / 5 | Overview / Incidents / People / Shift report / Simulation |
| Up, Down, Enter | Move in tables; Enter opens detail |
| A | Acknowledge selected incident (no confirmation) |
| R | Open resolve dialog for the selected incident |
| Esc | Close dialog or detail pane |
| F5 | Refresh the current screen |

## 9. Confirmations

Confirm before: Resolve (records a category and note), End assignment, Retire worker or helmet, Reset simulation, overwrite of a CSV export. Dialog focus starts on Cancel. The action button names the verb ("End assignment"). Acknowledge needs no dialog.

## 10. Sources

- Nielsen heuristics: [The Decision Lab](https://thedecisionlab.com/reference-guide/design/nielsens-heuristics), [Human Standards](https://www.humanstandards.org/interaction-patterns/nielsen-heuristics/)
- ISA-101 and alarm design: [Industrial Monitor Direct](https://industrialmonitordirect.com/blogs/knowledgebase/industrial-hmi-design-color-contrast-and-alarm-best-practices), [iFactory](https://ifactoryapp.com/blog/hmi-design-best-practices)
- FlatLaf: [How to customize](https://www.formdev.com/flatlaf/how-to-customize/), [FlatLightLaf.properties](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/resources/com/formdev/flatlaf/FlatLightLaf.properties), [FlatDarkLaf.properties](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/resources/com/formdev/flatlaf/FlatDarkLaf.properties), [FlatLaf Javadoc 2.0.2](https://javadoc.io/static/com.formdev/flatlaf/2.0.2/com/formdev/flatlaf/FlatLaf.html)
- WCAG: [1.4.3 Contrast (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html), [1.4.11 Non-text Contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)

## 11. Limits

- ISA-101 sources are vendor blogs. The exact standard text was not read. Colour rules are design choices, not compliance claims.
- FlatLaf method and key names come from older Javadoc and a 2.0 release note; the current version was not checked.
- Contrast values are our calculation, not a tool report. Re-check after palette changes.
- Layouts are not rendered. Verify at 1366x768 and 1920x1080 with real screenshots.
- Glyph support in Segoe UI Symbol is not tested.
