## BPMO

BPMO is a blood pressure and oxygen monitoring android app.

On the device the app is called **Vitals**. You photograph your blood pressure monitor or pulse oximeter, or import photos you've already taken, and it reads the numbers off the display. Each reading is dated from the photo, and you check it on a Review screen before it's saved. From your saved readings you get 7-day and 30-day averages, a day-by-day history and weekly or monthly trend charts. Everything stays on the phone.

It's built in Kotlin with Jetpack Compose and Material 3: rounded cards, a sage-and-teal palette with a deep forest-green dark mode, and Figtree type.

<p align="center">
  <img src="docs/screenshots/light-tablet.png" alt="Vitals on a tablet in light mode: Today dashboard beside Trends" width="49%">
  <img src="docs/screenshots/dark-tablet.png" alt="Vitals on a tablet in dark mode: Today dashboard beside Trends" width="49%">
</p>

> The screenshots are the app itself, rendered by the screenshot tests with a month of sample readings ending on Tuesday 20 October 2026. The monitor photos are drawn stand-ins.

### Screens

**Today** shows your average blood pressure for the last 7 or 30 days, with its category (see [Categories](#categories)), the systolic and diastolic ranges and a small trend line. Cards for heart rate and SpO₂ have their own trend lines, and your three latest readings sit underneath. Scan reading and Import are the two buttons at the bottom.

**History** lists every reading grouped by day, each with its category. You can filter to blood pressure or SpO₂ only, or search your notes. Tap a reading to see the source photo, how it was dated and anything you corrected. You can edit or delete it from there.

**Trends** shows a week or a month at a time. Blood pressure appears as rounded bars from diastolic up to systolic, coloured by category, with the Stage 1 thresholds marked. Below are the heart-rate and SpO₂ lines, a count of readings per category and a table of averages, minimums and maximums. The download button exports a CSV of all your readings, or a [PDF report](#pdf-report) for any range of dates.

| | Today | History | Trends |
|---|:---:|:---:|:---:|
| **Light** | <img src="docs/screenshots/light-dashboard.png" alt="Today, light" width="250"> | <img src="docs/screenshots/light-history.png" alt="History, light" width="250"> | <img src="docs/screenshots/light-trends.png" alt="Trends, light" width="250"> |
| **Dark** | <img src="docs/screenshots/dark-dashboard.png" alt="Today, dark" width="250"> | <img src="docs/screenshots/dark-history.png" alt="History, dark" width="250"> | <img src="docs/screenshots/dark-trends.png" alt="Trends, dark" width="250"> |

### Adding a reading

**Scan** opens the camera, which always uses a dark screen. Choose BP monitor or Oximeter and fit the display inside the frame; only what's inside it is read. Each value shows up once several recent frames read it the same, with a dash for any not yet read, so you can see which one a reflection is hiding and tilt the phone. Auto-capture takes the photo when every value has held for about a second. Review starts from whichever read more, the photo or the live view, and fills in any value the photo missed from the live view.

**Import** opens the Android Photo Picker for one or more past photos. They appear in a grid with the time each was taken, read from the photo's EXIF data. A photo with no date is flagged, and you set its date during review.

**Review** shows every reading before it's saved. Boxes on the photo mark where each value was read. Any value the app wasn't sure of gets an orange border and a "check" note, and values outside a plausible range block saving. The category updates as you edit. You can change the date and time, and add a note, with quick chips such as "Before meds" or "Left arm". A reading in the top category (hypertensive crisis or severe hypertension) shows advice on what to do before it's saved.

**How the display is read.** Most monitors show seven-segment digits, made of separate bars, which general text recognition often misses or misreads. So Vitals reads them itself, on the phone:

1. Find the display: the biggest bright, solid patch in the photo.
2. Straighten it, correcting for a phone held at an angle, and enlarge it to a fixed size.
3. Even out the lighting, then separate the dark segments from the background. A shadow's edge across the display stays sharp, so the shadowed side isn't judged against the lit side.
4. Split the display into rows of digits, ignoring icons, the clock and level bars.
5. Read each digit from which of its seven segments are dark, and how clearly.

Most fingertip oximeters have a screen that lights its own digits on black, often in an ordinary typeface rather than segments. When a photo gives fewer than two values, Vitals reads it again inverted, so the screen reads like a normal display, and reads each solid digit from its shape: its holes and the sides it's open on, which every typeface shares. A 6 has a hole low down and is open at the top right; a 2 is open at the top left and the bottom right, above a flat foot. Labels, small print and the pulse wave read as no digit, and a decimal point keeps the perfusion index apart.

The numbers then go to SYS, DIA and pulse, top to bottom, or to SpO₂ and the pulse beside it. SYS and DIA share a digit size, so if only one of them was read it's left for you to fill in rather than guessed. A digit that wasn't clear gets the "check" note. If that misses a value or is unsure of one, ML Kit text recognition reads the photo too, and the fuller result is used.

<table>
  <tr>
    <th>Scan (always dark)</th>
    <th>Review, light</th>
    <th>Review, dark</th>
  </tr>
  <tr>
    <td><img src="docs/screenshots/dark-capture.png" alt="Camera screen with the rounded guide frame, before camera access is granted" width="250"></td>
    <td><img src="docs/screenshots/light-review.png" alt="Review screen, light" width="250"></td>
    <td><img src="docs/screenshots/dark-review.png" alt="Review screen, dark" width="250"></td>
  </tr>
</table>

### PDF report

**Trends › download › PDF report…** opens a date range picker, starting on the period on screen, and shares an A4 report you can print or send to your doctor:

- **Blood pressure:** a table of every reading, with date, time, systolic, diastolic, pulse and notes. After it come the blood pressure chart with the Stage 1 lines, the pulse chart, the category breakdown, and a summary of averages, lowest and highest values.
- **Blood oxygen:** starts on a new page, with a table of date, time, oxygen (SpO₂) and notes, then the SpO₂ chart and its summary.

Tables repeat their headings on each page, notes wrap onto up to three lines, and every page is numbered. Charts show a point per day for up to three months, then weekly or 30-day averages, so a long range stays readable. A section with no readings in the range says so.

<p align="center">
  <img src="docs/screenshots/report-1.png" alt="Report page 1: title, date range and the blood pressure table" width="32%">
  <img src="docs/screenshots/report-3.png" alt="Report page 3: blood pressure, pulse and category charts, and the summary" width="32%">
  <img src="docs/screenshots/report-4.png" alt="Report page 4: blood oxygen table, SpO₂ chart and summary" width="32%">
</p>

### Categories

Blood pressure categories follow the guidance chosen in **Settings › Blood pressure**:

| | AHA (US) | NICE (UK), home readings |
|---|---|---|
| Normal | below 120/80 | below 135/85 |
| Elevated | 120–129 systolic, diastolic below 80 | (no such category) |
| Stage 1 | from 130/80 | from 135/85 |
| Stage 2 | from 140/90 | from 150/95 |
| Top category | Hypertensive crisis, above 180/120 | Severe hypertension, from 180/120 |

A reading goes in a category when either its systolic or its diastolic value reaches that category's threshold. The AHA thresholds are its categories for adults. NICE (guideline NG136) sets lower thresholds for averages measured at home than for clinic readings (140/90 for Stage 1 and 160/100 for Stage 2), because blood pressure tends to read lower at home. Vitals records home readings, so it uses the home thresholds.

Until you choose, Vitals uses NICE if the phone's region is the UK and AHA everywhere else. Categories aren't stored with readings, so changing the guidance re-labels every reading, chart and export straight away.

### Privacy and lock

Readings and photos are kept in app-private storage and excluded from cloud backup. In Settings, the fourth tab, you can require your fingerprint, face or device PIN to open the app. The lock asks again after the app has been in the background for a time you choose: immediately, 1 minute or 5 minutes. After three failed biometric attempts it falls back to your PIN, and app content is hidden from screenshots and the recents screen while the app is locked. Settings also lets you choose a light or dark theme, or follow the phone's setting (the default), and turn the category colours off.

| | Reading detail | Settings | Lock |
|---|:---:|:---:|:---:|
| **Light** | <img src="docs/screenshots/light-detail.png" alt="Reading detail, light" width="250"> | <img src="docs/screenshots/light-settings.png" alt="Settings, light" width="250"> | <img src="docs/screenshots/light-lock.png" alt="Lock screen, light" width="250"> |
| **Dark** | <img src="docs/screenshots/dark-detail.png" alt="Reading detail, dark" width="250"> | <img src="docs/screenshots/dark-settings.png" alt="Settings, dark" width="250"> | <img src="docs/screenshots/dark-lock.png" alt="Lock screen, dark" width="250"> |

### Your data

Everything stays on the phone, in storage only Vitals can read:

- **Readings** are in a SQLite database (Room), `vitals.db`.
- **Photos** are JPEGs in the app's private `photos/` folder.
- **Settings** are in a DataStore preferences file.

Android keeps all of this when the app is **updated**, as long as the new APK is signed with the same key and has a higher version code. Every CI build is (see [Signing](#signing)), so installing a newer APK over the old one keeps your readings. The database schema is versioned, and a change that needs one ships with a migration, never a wipe.

Uninstalling the app, clearing its storage or moving to a new phone does remove the data, and cloud backup is off because it's health data. For those cases, **Settings › Your data › Back up readings** saves every reading and photo to a single `.zip` file wherever you choose, such as Downloads or a drive. **Restore from a backup** adds them back, skipping any that are already there, so restoring twice doesn't double up. The file isn't encrypted, so keep it somewhere private.

### Phones, foldables and tablets

On phones, navigation is a bar along the bottom. On wider screens, such as an unfolded foldable, it moves to a rail on the left that also holds the Scan and Import buttons. History then shows the list and the selected reading side by side. On tablets, Trends sits beside the Today screen (see the top of this page).

<p align="center">
  <img src="docs/screenshots/light-foldable.png" alt="Foldable layout: navigation rail, History list and Reading detail side by side" width="70%">
</p>

### Build

You need JDK 17 or newer and the Android SDK (platform 35).

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :core:test           # domain logic: categories, validation, averages, display reading, EXIF dates
./gradlew :core:test -PdisplayPhotos=/path/to/photos   # also checks the display reader against your own photos
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest -Pscreenshots   # re-renders docs/screenshots with Robolectric
```

The display reader is tested against drawn monitor displays, including tilted, small, dim, shadowed, blurred and heavily compressed versions, and drawn oximeter screens in serif, sans-serif and monospaced typefaces. To check it against real photos without adding them to the repository, put them in a folder, upright, named after what they show: `bp-154-100-76.jpg` for SYS, DIA and pulse, or `ox-97-68.jpg` for SpO₂ and pulse (anything after the values is ignored). Photos in `core/src/test/resources/displays/` are checked on every build.

On every push, GitHub Actions (`.github/workflows/android.yml`) runs the tests, builds the APKs and runs lint. Each run's `vitals-debug` artifact has the debug APK, and `vitals-release` has the release APK once signing is set up. On `claude/**` branches, the Screenshots workflow re-renders the screenshots and commits any that changed, along with the exported database schema in `app/schemas`.

### Signing

An update only installs over the old app, and keeps its data, when both are signed with the same key. CI signs every debug and release APK with one key, stored in two repository secrets (Settings › Secrets and variables › Actions):

- `VITALS_KEYSTORE_BASE64`: the PKCS12 keystore, base64-encoded (`base64 -w0 vitals-signing.p12`)
- `VITALS_KEYSTORE_PASSWORD`: its password, which is also the key's (alias `vitals`)

Each CI build's version code is its run number, so a newer build always installs as an update. Keep a copy of the keystore and password somewhere safe: an APK signed with any other key has to replace the app instead of updating it. Without the secrets, for example on a fork or a local build, the default debug key is used.

### Layout

- `core/`: plain Kotlin with no Android dependencies, holding the AHA and NICE category rules, validation, averages, trend periods, the display reader and parser, EXIF date parsing, Review form state and CSV export. It's unit-tested on the JVM.
- `app/`: the Android app.
  - `data/`: Room and DataStore.
  - `extraction/`: reading photos and live camera frames (with ML Kit as the fallback), and EXIF.
  - `security/`: the app lock.
  - `export/`: CSV and the PDF report.
  - `ui/`: one package per screen.
- `design/`: the original design handoff and its HTML mockups.
- `docs/screenshots/`: the images in this README, rendered by `app/src/test/.../screenshots/ScreenshotTest.kt`.

### Credits

Figtree is under the SIL Open Font License, and the icons are Lucide under ISC. The licence texts are in `app/licenses/`.
