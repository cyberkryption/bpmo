# Handoff: Vitals — blood pressure, pulse & SpO₂ tracker (Android)

## Overview
An Android app (Kotlin + Jetpack Compose, Material 3 adaptive) for tracking blood pressure, heart rate and oxygen saturation. The user photographs a BP monitor or pulse oximeter display, or imports past photos (multi-select). Values are extracted on-device, the reading is dated from the photo's EXIF timestamp, and every reading is confirmed on an editable Review screen with an optional note before it's saved. The app shows 7-day and 30-day averages as tiles, plus weekly/monthly charts.

## About the design files
The files here are **design references made in HTML**. They show the intended look and behaviour; they are not production code. Rebuild them in Jetpack Compose with Material 3, mapping the tokens below onto a custom `ColorScheme`, `Typography` and `Shapes`. Open `Vitals App.dc.html` in a browser to see every screen in light, dark, foldable and tablet layouts. Each `*Screen.dc.html` maps 1:1 to a `@Composable` screen of the same name.

## Fidelity
**High-fidelity.** Colours, type, spacing, shapes and copy are final. Match them closely, using Compose/M3 components restyled to these tokens.

## Visual language ("Industry" design system)
- Square corners everywhere (`RoundedCornerShape(0.dp)` for all M3 shape slots). The only rounding is the device bezel in the mock.
- Cards are **transparent, outlined** (1dp `divider` stroke, no fill, no elevation) with "+" **registration marks** at each corner: an 11×11dp cross centred on the corner, offset −6dp outward, 1dp stroke in `text @55%`. Draw them with `Modifier.drawWithContent`. They also go on the primary button and the photo frame.
- The primary action is the only solid object: an `accent` fill with `bg`-coloured text, plus corner marks.
- Icons: Lucide at stroke 1.5 (bundle as vector drawables), 24dp in app bars, 20–22dp in nav/rows, 16–18dp in tile labels.
- Headings use Barlow Condensed SemiBold; body uses Barlow. Numbers use tabular figures (`fontFeatureSettings = "tnum"`).

## Design tokens

### Colour — light
| Token | Hex |
|---|---|
| bg (background/surface) | #F2F2F3 |
| surface (input fill) | #E9E9EA |
| text (onBackground) | #1D1F20 |
| accent (primary) | #5980A6 |
| accent-100 (primaryContainer, selected tint) | #EEF6FF |
| accent-600 (pressed primary) | #597EA3 |
| accent-700 (labels/kickers in accent) | #416180 |
| accent-800 (onPrimaryContainer) | #2C455D |
| divider | text @16% alpha |
| muted text | text @55–65% alpha |
| neutral-500 | #98989B |

### Colour — dark
| Token | Hex |
|---|---|
| bg | #121416 |
| surface | #1B1E21 |
| text | #E4E6E8 |
| accent | #8FB3D8 |
| accent-100 | #1D2D3D |
| accent-600 | #B5D9FD |
| accent-700 | #A9CDF1 |
| accent-800 | #D6EBFF |
| divider | text @18% alpha |

### Status / category colours (OKLCH — convert to sRGB at build time)
All categories share lightness and chroma and differ only in hue. The base colour is used for dots, bars and borders. The ink colour is used for text on a tinted fill. Tag fill = base @18% over bg.

| Category | Light base | Light ink | Dark base | Dark ink |
|---|---|---|---|---|
| normal | oklch(.64 .13 150) | oklch(.40 .09 150) | oklch(.74 .12 150) | oklch(.86 .08 150) |
| elevated | oklch(.64 .13 95) | oklch(.40 .09 95) | oklch(.74 .12 95) | oklch(.86 .08 95) |
| stage1 | oklch(.64 .13 60) | oklch(.40 .09 60) | oklch(.74 .12 60) | oklch(.86 .08 60) |
| stage2 | oklch(.64 .13 30) | oklch(.40 .09 30) | oklch(.74 .12 30) | oklch(.86 .08 30) |
| crisis | oklch(.52 .17 20) | oklch(.36 .12 20) | oklch(.66 .17 20) | oklch(.84 .10 20) |

stage1 is also used as the "warning / low confidence" colour.

### Type (sp)
| Role | Font | Size / line | Notes |
|---|---|---|---|
| Hero value (dashboard BP) | Barlow Condensed 600 | 64 / 1.0 | tnum, −0.02em |
| Detail value | Barlow Condensed 600 | 72 / 1.0 | |
| Tile value | Barlow Condensed 600 | 44 / 1.0 | |
| Screen title (top-level) | Barlow Condensed 600 | 30 / 1.1 | |
| Screen title (sub-screen app bar) | Barlow Condensed 600 | 22 | |
| Section heading | Barlow Condensed 600 | 20 | |
| Row value | Barlow Condensed 600 | 22 / 1.1 | |
| Input value | Barlow Condensed 600 | 28 | |
| Button | Barlow Condensed 600 | 16–17 | |
| Body | Barlow 400 | 14–15 / 1.5 | |
| Meta / caption | Barlow 400 | 12 | text @62% |
| Kicker | Barlow 400 | 11, +0.1em, UPPERCASE | accent-700 |
| Tag | Barlow 400 | 10–11, +0.06em, UPPERCASE | |

### Spacing, shape, elevation
- Screen horizontal padding is 20dp. Section gap is 22dp. Tile grid gap is 20dp. Tile padding is 14–16dp.
- Touch targets are at least 48dp (icon buttons). Chips are 36dp tall, segmented buttons 40–44dp, primary buttons 52–56dp.
- Corner radius is 0 everywhere.
- Shadows appear only on FABs (`shadow-md`: 0 3 10, ink @16%). Cards and tiles never have shadows.

## Adaptive layout
Use `currentWindowAdaptiveInfo()` / `NavigationSuiteScaffold`.
- **Compact (<600dp):** bottom `NavigationBar` with Today, History and Trends, 80dp tall. The selected indicator is a 64×32dp square tint in accent-100 with accent-800 icon/label. The Dashboard has two FABs at the bottom right: a square 56dp Import button (outlined, bg fill, shadow) and an extended "Scan reading" primary button.
- **Medium (600–840dp, e.g. an unfolded foldable):** an 88dp `NavigationRail` with the Scan button at the top. History and Reading detail use `ListDetailPaneScaffold`: the list pane is 360dp and the detail pane fills the rest. The selected row gets an accent-100 fill and a 3dp accent inset bar on the left. The detail pane hides its back arrow.
- **Expanded (>840dp, tablet):** rail, then Dashboard (flex) and Trends (520dp) side by side. Dashboard FABs are hidden because the rail holds Scan.
- Tile grids use adaptive columns with a minimum width of 156dp (`LazyVerticalGrid(GridCells.Adaptive(156.dp))`, or FlowRow). The BP hero tile always spans the full row.

## Screens

### 01 DashboardScreen
- **Header:** kicker date ("Tue 20 Oct") above the title "Vitals", with an overflow menu on the right.
- **Range toggle:** a segmented control with "7 days" and "30 days". The selected segment has an accent fill with bg text.
- **BP hero tile (full width):** kicker "Blood pressure · 7-day avg" and a category tag. Below that, the average value "126/79" with "mmHg", a 64dp sparkline (solid systolic line, dashed 4/3 diastolic line, dashed threshold guides at 120 and 80), and a meta row "N readings · SYS min–max · DIA min–max".
- **Heart rate tile:** average in bpm, sparkline, and "Range min–max".
- **SpO₂ tile:** average in %, sparkline, and "Lowest n%".
- **Latest:** the 3 most recent readings. Each row has a 48dp photo thumbnail, the value, a unit and pulse line, a time and note line (ellipsised), and a category tag. "See all" goes to History.
- Averages are the arithmetic mean over the selected window, rounded. BP category is computed from the averaged systolic/diastolic values.

### 02 HistoryScreen
- Title "History" with a search action. Filter chips: All, Blood pressure, SpO₂ (selected chip has an accent fill).
- Readings are grouped by day under sticky kicker headers ("Today · Tue 20 Oct"). Each row has a 40dp outlined type icon (activity icon for BP, droplet for SpO₂), the value and unit, the time and note, and a category tag. Tapping a row opens Reading detail.

### 03 TrendsScreen
- Week/Month segmented control, then a period stepper (‹ "14 – 20 Oct" ›). The next arrow is disabled on the current period.
- **BP chart:** floating bars, one per reading/day, running from diastolic up to systolic. Bars are coloured by category. The y-axis runs 60–150 with labels at 150, 125, 100 and 75. Dashed stage1 lines mark 130 and 80. Bars have a 14dp gap in week view and 3dp in month view.
- **HR chart and SpO₂ chart:** line charts in adaptive tiles. The SpO₂ chart shows a dashed 95% threshold.
- **BP categories:** a 14dp stacked bar showing reading counts per category, with a legend.
- **Summary table:** Avg, Min and Max for Systolic, Diastolic, Pulse and SpO₂.
- Export (download icon) produces CSV/PDF. This is out of scope for the visuals.

### 04 ReadingDetailScreen
- App bar with back, edit (pencil) and delete (trash). Delete asks for confirmation in a dialog.
- Category tag (full label, e.g. "Stage 1 hypertension") above the large value. Below that, a three-cell strip for SYS, DIA and PUL with hairline dividers.
- Then the note, the framed source photo, and a metadata list: Taken (with seconds), Date source (EXIF DateTimeOriginal / Capture time / Manual), Device, and Edited.

### 05 CaptureScreen (always dark)
- Full-bleed CameraX preview on #0E1012. Top bar has close and flash. A segmented control switches between "BP monitor" and "Oximeter", which changes the guide frame to 3:4 or 16:9.
- The guide frame is a 1.5dp #9EC3EA border with 22dp registration marks.
- A live detection chip (green dot) shows the parsed values once the display is found, e.g. "Display found · 127 / 80 · 71".
- Bottom bar: gallery button (opens Import), a 76dp square shutter (2dp white border around a 60dp #9EC3EA fill), and an "Auto" label for auto-capture when values stay stable for about 1s.
- After capture, go to Review.

### 06 ImportScreen
- Use the Android Photo Picker (`PickMultipleVisualMedia`), or the in-app grid shown here. Photos are grouped as "This week" and "Earlier" in an adaptive grid (minimum 100dp, 4dp gaps). Each tile shows the EXIF time along its bottom edge. Photos without EXIF show "No date" in stage1 ink.
- Selection order is shown as a numbered 24dp square badge, and the selected tile gets a 2dp accent border.
- The footer note says how many photos have no date, and the primary button reads "Review N".
- Images are processed sequentially, one Review screen each.

### 07 ReviewScreen (always shown before save)
- App bar with back, the title "Review reading", and a counter "1 of 3". Below it, a 3dp segmented progress bar.
- The photo appears in a framed 4:3 figure with bounding boxes over the detected regions, labelled SYS, DIA and PUL. A low-confidence region uses a dashed stage1 border and label.
- A type toggle switches between "Blood pressure" and "Oximeter". Oximeter shows two fields: SpO₂ % and Pulse.
- Three numeric fields (56dp tall, value at 28sp) with captions showing unit and confidence. A field below 0.80 confidence gets a stage1 border, a tinted fill, and a "· check" caption. An inline warning banner says: "Diastolic was hard to read. Compare with the photo and correct if needed."
- A live category tag recomputes as values are edited.
- Timestamp row: "Tue 20 Oct 2026, 08:14", with the source "From photo EXIF · DateTimeOriginal" and a "Change" action (date+time picker). If there's no EXIF date, the row starts empty, is flagged, and Save stays disabled until a date is set.
- Note: a multiline field (placeholder "How were you feeling? Arm, posture, meds…") plus quick chips (Before meds, After meds, Left arm, After exercise, Stressed). The chips toggle text into the note.
- Footer actions: "Skip" (outlined) and "Save & next" (primary). On the last item the primary button reads "Save".

### 08 LockScreen (biometric login)
- Shown on cold start and after the re-lock timeout. Use `androidx.biometric` `BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`; the prompt launches automatically over this screen.
- Layout: centred column on bg. A 64dp framed logo tile (activity icon, accent, corner marks), the title "Vitals locked" (Barlow Condensed 600, 36sp), the body "Your readings and photos are private. Unlock to continue." (15sp, 65% text, max width 280dp), a 112dp square sensor button (1.5dp accent border, 56dp fingerprint icon at stroke 1.25), and a status line.
- States: idle ("Touch the sensor" / "Fingerprint or face", accent ring), scanning ("Scanning…", a 2dp accent scan line across the button), failed ("Not recognised" / "2 attempts left, then PIN", stage2 ring and ink). Tapping the sensor button re-launches the prompt.
- Bottom: outlined 52dp button "Use device PIN / pattern", then the caption "Re-locks after 1 minute in background".
- Rules: cancel keeps the app locked. Three failures fall back to the device credential. Set `FLAG_SECURE` on the window while locked and for recents thumbnails. If no biometric or credential is enrolled, skip the lock and offer a link to system Settings.
- Setting: Settings → Security → "Require biometric to open" (default off, offered once after the first saved reading), plus a re-lock timeout of Immediately, 1 minute or 5 minutes.

## Validation
- Systolic must be 60–260, diastolic 30–160 and less than systolic, pulse 30–220, SpO₂ 70–100.
- Values outside these ranges get an error border and block Save.

## Category rules (AHA, adult)
- normal: systolic <120 and diastolic <80
- elevated: systolic 120–129 and diastolic <80
- stage1: systolic 130–139 or diastolic 80–89
- stage2: systolic ≥140 or diastolic ≥90
- crisis: systolic >180 or diastolic >120. Also show a non-dismissable advisory dialog.
- SpO₂: 95 or higher is normal, 90–94 uses the stage1 colour ("Low"), below 90 uses stage2 ("Very low").
- Category colours can be turned off in Settings. When off, tags render neutral (neutral-500 dot, text ink).

## Data and state
- **Room `Reading`:** id, type (BP | OXIMETER), systolic?, diastolic?, pulse, spo2?, takenAt (Instant), takenAtSource (EXIF | CAPTURE | MANUAL), note?, photoUri, confidence (JSON map), createdAt, editedFields.
- **Timestamps:** read with `ExifInterface`, using TAG_DATETIME_ORIGINAL and then TAG_DATETIME, plus TAG_OFFSET_TIME_ORIGINAL when present. Otherwise use the device zone.
- **Photos:** copy into app-private storage and keep a content URI.
- **ViewModels:** DashboardVM (range, averages via Flow), HistoryVM (filter, grouped list), TrendsVM (range, period offset), ReviewVM (queue of pending images, current index, editable fields, confidence, note).
- **Extraction:** ML Kit Text Recognition v2 (on-device), then a parser that orders digit blocks by vertical position (BP: SYS, DIA, PUL top to bottom; oximeter: SpO₂ and PR, by label or size).

## Sample data in the mocks
Week of Wed 14 – Tue 20 Oct 2026: SYS 130 124 127 121 134 119 127, DIA 82 78 79 76 84 74 80, HR 74 69 71 66 75 66 71, SpO₂ 97 96 98 97 96 98 97. That gives a 7-day average of 126/79 (Elevated), HR 70 and SpO₂ 97.

## Files
- `Vitals App.dc.html`: the overview canvas with every screen, theme and size class. Tweaks: range (week/month), categoryColors.
- `DashboardScreen.dc.html`, `HistoryScreen.dc.html`, `TrendsScreen.dc.html`, `ReadingDetailScreen.dc.html`, `CaptureScreen.dc.html`, `ImportScreen.dc.html`, `ReviewScreen.dc.html`: one per Compose screen.
- `LockScreen.dc.html`: biometric login (Tweaks: status, timeout).
- `NavBar.dc.html`: NavigationBar and NavigationRail variants.
- `styles.css`: the source design-system tokens.

## Assets
- Icons: Lucide (ISC licence), stroke 1.5. The set used: camera, image, activity, heart, droplet, more-vertical, download, chevron-left/right, arrow-left, x, zap, check, alert-triangle, clock, pencil, trash-2, search, house, list, chart-line.
- Fonts: Barlow and Barlow Condensed (Google Fonts, OFL). Bundle them as `res/font`.
- Striped boxes in the mocks are placeholders for user photos and the camera feed. No other imagery is used.
