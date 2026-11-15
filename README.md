# Ani Note

اپلیکیشن اندروید فارسی برای مدیریت یادداشت‌ها، یادآورها و گردش حساب.

برای دریافت APK بخش Actions > Build Android APK > Artifacts را ببینید.

## Preview upgrades

`feature/calendar-widgets-icon` retains `com.anishop.aninote.preview`; main's app is a separate installation. A new application ID cannot read another app's private data. Export a backup from the old app and restore it in Preview when changing IDs. Never uninstall the only copy of your data.

CI assigns an increasing versionCode (run number plus attempt), displays the installed version in Settings, runs unit tests/lint and an emulator upgrade test before publishing a public GitHub prerelease. The in-app button checks the latest 100 releases for this Preview channel, downloads through the user's browser and uses Android's normal installer. It requires user confirmation; it does not silently install or bypass signature checks. Network failures are reported separately from “no update”. Release publishing needs repository Actions write permission.

Preview uses a persistent **debug/test** signing identity from the Actions cache, optionally restored via the `PREVIEW_KEYSTORE_BASE64` repository secret (the same keystore, alias `androiddebugkey`, test password `android`). Cache is not a permanent backup. The workflow refuses a new signing identity after the first Preview release. Back up the keystore securely and set that secret for durable continuity. Never commit/upload private keys as artifacts. This test identity is unsuitable for production; production requires a separately protected release key and a planned transition.

Earlier CI debug APKs used a newly generated key on each runner. Their private keys may no longer exist. An APK certificate/public fingerprint cannot reconstruct its private key. Without the original compatible signing key, installation over those APKs cannot be guaranteed or repaired in app code. Do not uninstall to solve this: export and verify a backup first. Future builds using this persistent identity can update each other. The installer's signature check is authoritative.

Data uses `data` SharedPreferences and Android Keystore AES-GCM, with in-place plaintext migration. Neither preferences nor the key alias is reset during update. Failed reads now block editing instead of returning empty collections. Plaintext JSON is checked before encryption. There is no WebView/service-worker cache; Compose state is loaded per process. Package replacement refreshes widgets and schedules alarms. No data/cache clear is used as an upgrade workaround.

The emulator test creates real encrypted notes and legacy plaintext accounts, installs a higher-version APK using `adb install -r`, then verifies records, settings and migration. This tests the new upgrade path, not every historically distributed APK or the user's device. Testing an exact old APK requires that APK and its original signing key. Phone acceptance: export a backup, note the installed package/version, install the compatible new APK without uninstalling, check records/settings/widgets/reminders and secure entry after relaunch.

## Exact decimal account amounts

Account entries now use BigDecimal throughout input, persistence, editing, sums, widgets and PDF reports. Dot (`.`) and Arabic decimal separator (`٫`) mean a decimal point; comma (`,`) and Arabic thousands separator (`٬`) mean grouping. For example `16500.5` and `۱۶٬۵۰۰٫۵` both represent 16500.5 toman. Input supports up to 19 whole-number digits and 18 fractional digits without floating-point rounding. Existing integer records retain their exact value. Previously saved values whose point was removed cannot be inferred or automatically corrected.

New amounts are stored as exact decimal strings in the existing encrypted account collection. Backup format 2 preserves these strings; restore accepts formats 1 and 2. Older app versions reject format 2 rather than silently truncating fractional data. CI checks exact arithmetic, edit formatting, old integer records, encrypted save/reload, backup/restore and an in-place upgrade from the published main-v205901 APK before exposing the new installable artifact.

## Independent recovery release

The original main signing key was not recoverable. The authorized recovery release lives on `release/recovery-decimals` and uses `com.anishop.aninote.recovery`, preserving the existing installation. See [migration instructions](docs/RECOVERY-MIGRATION.md). The new channel only accepts `recovery-v` release tags and matching recovery APK assets.

Required repository Secrets: `ANI_RECOVERY_KEYSTORE_BASE64` (PKCS12, alias `ani-note-recovery`) and `ANI_RECOVERY_STORE_PASSWORD`. The same protected key is restored independently in two CI jobs; certificate fingerprints are pinned and compared before publication. Neither job generates keys or stores them in Actions cache. Missing Secrets or a changed certificate stops publication. A Windows-user DPAPI protected local backup is kept outside the repository. Never add private keys, password files or that backup to commits or artifacts.


## Main release channel

`main` now continues the independent release with the fixed applicationId `com.anishop.aninote.recovery` and the existing pinned recovery signing certificate. It uses the same `ANI_RECOVERY_KEYSTORE_BASE64` / `ANI_RECOVERY_STORE_PASSWORD` Secrets and `recovery-v` update channel. No signing key is regenerated and no migration to a different installation ID is performed for subsequent updates.

Account amount entry displays automatic three-digit grouping without modifying the raw decimal value, with caret offsets mapped around the display separators. A dot remains the decimal separator. Deleting a single entry now requires an explicit confirmation that identifies its subject and amount; cancellation leaves the entry unchanged. CI covers grouping/caret mapping, UI cancellation/confirmation, save/edit of grouped decimals and upgrading from the published independent APK 306202 without uninstalling it.
