## BPMO

BPMO is a blood pressure and oxygen monitoring android app.

The app is called **Vitals** on the device. It's built in Kotlin with Jetpack Compose and Material 3 from the design handoff in [`design/`](design/README.md).

### What it does

- Photograph a blood pressure monitor or pulse oximeter, or import past photos with the Android Photo Picker. Values are read on the device with ML Kit text recognition.
- Every reading is confirmed on an editable Review screen before it's saved. Low-confidence values are flagged, out-of-range values block saving, and the reading is dated from the photo's EXIF timestamp.
- The Today screen shows 7-day and 30-day averages. History groups readings by day. Trends has weekly and monthly charts, AHA category counts and a summary table, with CSV and PDF export.
- An optional biometric lock (fingerprint, face or device PIN), and a setting to turn category colours off.
- Adaptive layouts: a bottom bar on phones, a navigation rail with list-detail History on foldables, and Dashboard beside Trends on tablets.

### Build

You need JDK 17 or newer and the Android SDK (platform 35).

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :core:test           # domain logic: categories, validation, averages, OCR parsing, EXIF dates
./gradlew :app:testDebugUnitTest
```

GitHub Actions (`.github/workflows/android.yml`) runs the tests, builds the debug APK and runs lint on every push.

### Layout

- `core/`: plain Kotlin with no Android dependencies, holding the AHA category rules, validation, averages, trend periods, the display parser, EXIF date parsing, Review form state and CSV export. It's unit-tested on the JVM.
- `app/`: the Android app.
  - `data/`: Room and DataStore.
  - `extraction/`: ML Kit and EXIF.
  - `security/`: the app lock.
  - `export/`: CSV and PDF.
  - `ui/`: one package per screen.

### Credits

Barlow and Barlow Condensed are under the SIL Open Font License, and the icons are Lucide under ISC. The licence texts are in `app/licenses/`.
