# LibreCare Testing Quick Start

## Cel

Ten katalog zawiera całą dokumentację i infrastrukturę potrzebną do automatycznego testowania LibreCare w Codespaces, GitHub Actions i Firebase Test Lab.

## Szybki start w Codespaces

1. Otwórz repozytorium w GitHub Codespaces.
2. Poczekaj, aż kontener wykona `postCreateCommand`.
3. Uruchom w terminalu:

```bash
bash ./scripts/verify-environment.sh
bash ./scripts/test-fast.sh
```

## Testy lokalne

- `bash ./scripts/test-fast.sh` — szybki zestaw PR: środowisko, unit testy, lint, debug build.
- `bash ./scripts/test-all.sh` — pełny zestaw lokalny: clean, unit testy, lint, debug build, androidTest APK.

## GitHub Actions

- `android-ci.yml` — szybki CI dla push / pull request.
- `firebase-test-lab.yml` — budowanie APK i opcjonalne uruchomienie testów na urządzeniach Firebase.

## Artefakty

Po udanym buildzie szukaj plików w:

- `app/build/outputs/apk/debug/`
- `app/build/outputs/apk/release/`
- `app/build/outputs/bundle/release/`
- `release-artifacts/`

Dla Google Play jako plik uploadu zawsze używaj release AAB.

## Co jest objęte

- unit testy JVM
- testy instrumentacyjne Android
- testy migracji Room
- testy UI Compose
- testy nawigacji
- testy backup / restore
- testy screenshot / golden
- testy dostępności

## Co nie jest zmieniane

Ta infrastruktura nie zmienia wyglądu ani zachowania aplikacji produkcyjnej.

