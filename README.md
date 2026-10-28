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
