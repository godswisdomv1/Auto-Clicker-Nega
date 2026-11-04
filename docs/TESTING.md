# Manual QA matrix

Automated tests cover logic. This matrix covers what only a person with a phone can judge.
Each row is ticked per release candidate, on the owner's phone unless a device is named.

## Android versions

| Bucket | Device or emulator | Checks |
|---|---|---|
| Android 11 (API 30, minSdk) | Emulator profile, 1.5 GB RAM | Install, onboarding, enable service, tap script runs |
| Android 12 to 13 | Emulator or phone | Splash, per-app language, panel opens over another app |
| Android 14 to 15 | Phone | Edge-to-edge insets, predictive back, screenshot capture |
| Android 16 (API 36, target) | Phone | Everything above, plus Start in X wait |

## Languages

For each of the 11 languages (English, Spanish, French, German, Portuguese, Arabic, Hindi,
Japanese, Korean, Simplified Chinese, Hebrew): open Home, Settings, the editor and the run
gate sheet. Nothing truncated, nothing in the wrong language, numbers and durations localised.

## Layout and display

- Right-to-left: Arabic and Hebrew mirror every screen and the floating panel.
- Large font: system font size at 200 percent, no clipped controls.
- Dark mode: every screen and the panel, contrast legible.
- Landscape: Home, editors, panel placement, orientation-mismatch warning on scripts.

## Consent regions

Run online, offline with consent saved, and offline with no consent in: the EEA, the UK,
Switzerland and one unregulated region. Confirm the run gate follows the rules in the blueprint.

## Checkpoints

- APK-1 (after B12): onboarding, service enable, panel, coordinate script, Detection lab report.
- APK-2 (after B18): the full list above, using test ads.
