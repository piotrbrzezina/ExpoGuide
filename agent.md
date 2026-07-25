# Agent Task History

## Zadanie: Dodanie obsługi wcześniej wykonanych zdjęć z galerii (Issue #5)

**Branch:** `copilot/dodanie-obslugi-zdjec-z-galerii`

### Opis
Dodano możliwość wyboru zdjęcia z galerii urządzenia, które przechodzi przez ten sam pipeline: OCR → tłumaczenie → TTS → AI.

### Zmiany

**`AndroidManifest.xml`**
- Dodano uprawnienie `READ_MEDIA_IMAGES` (Android 13+ / API 33+)
- Dodano uprawnienie `READ_EXTERNAL_STORAGE` z `maxSdkVersion="32"` (API 24–32)

**`activity_main.xml`**
- Dodano przycisk `btnGallery` („Wybierz z galerii") poniżej `btnCapture`

**`MainActivity.kt`**
- Dodano trzy nowe launchery: `pickVisualMedia` (PickVisualMedia), `pickImageFallback` (GetContent fallback), `requestStoragePermission`
- Dodano funkcję `openGallery()` z logiką wyboru odpowiedniego launchera w zależności od dostępności Photo Pickera i wersji API
- Dodano funkcję `loadBitmapFromUri(uri)` dekodującą URI na Bitmap (ImageDecoder API 28+ lub BitmapFactory jako fallback)
- Przycisk `btnGallery` włączany/wyłączany razem z `btnCapture` po pobraniu modelu tłumaczenia
