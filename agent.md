# Expo HAS CHANGED

Read the exact versioned docs at https://docs.expo.dev/versions/v57.0.0/ before writing any code.

# Podstawowe założenia aplikacji
Aplikacja ExpoGuide to mobilny przewodnik muzealny. Pozwala na zrobienie zdjęcia tabliczce informacyjnej (np. w j. angielskim), rozpoznanie znajdującego się na niej tekstu (OCR), przetłumaczenie go na język polski i odczytanie za pomocą syntezatora mowy (TTS). Aplikacja umożliwia także wygenerowanie dodatkowych ciekawostek o eksponacie przy pomocy AI (Google Gemini).

# Podstawowe techniczne dane
- **Platforma:** Android
- **Język programowania:** Kotlin
- **Rozpoznawanie obrazu i tekstu (OCR):** Google ML Kit
- **Tłumaczenie offline:** Google ML Kit (EN do PL)
- **Synteza mowy:** Android TextToSpeech (TTS)
- **AI:** Google Generative AI SDK (model gemini-1.5-flash)

**Zasada pracy:**
Za każdym razem gdy kończymy jakieś zadanie, należy dodać opis tego zadania do agent.md.
