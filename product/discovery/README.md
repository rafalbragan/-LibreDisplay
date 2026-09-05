# LibreCare Discovery Agent v1

LibreCare Discovery Agent v1 zbiera publiczne sygnały produktowe i tworzy
**deterministyczny** raport TOP 10 dla Product Ownera.

## Co robi

- Pobiera dane ze skonfigurowanych źródeł publicznych (`product/discovery/sources.json`).
- Normalizuje URL/tekst, wykonuje deduplikację i klastrowanie problemów.
- Porównuje klastry z Product Foundation (`requirements`, `decisions`, `observations`, validated capabilities).
- Uruchamia ograniczoną analizę AI (docelowo `gpt-5.4-mini`, max 2 wywołania/run).
- Stosuje deterministyczne reguły governance i ranking 0-100.
- Tworzy raporty w `product/generated/discovery/`.
- Opcjonalnie publikuje maks. TOP 3 kandydatów do Product Inbox (`publish_top3=true`).
- W workflow CI zapisuje wynik jako artifact GitHub Actions (bez automatycznego commita do repozytorium).

## Czego nie robi

- Nie akceptuje wymagań.
- Nie uruchamia implementacji.
- Nie przypisuje coding-agentów.
- Nie merguje PR.
- Nie tworzy rekomendacji dawkowania insuliny.

## Klasyfikacje

Dokładnie jedna z:

- `VALIDATED_CAPABILITY`
- `PRODUCT_PROBLEM`
- `PRODUCT_OPPORTUNITY`
- `TEST_COVERAGE_GAP`
- `SAFETY_GAP`
- `INCONCLUSIVE`

## Privacy / minimalizacja danych

Agent zapisuje tylko:

- canonical URL,
- source family,
- czas pobrania,
- hash treści,
- krótki excerpt/parafrazę,
- znormalizowany problem,
- metadane do dedupe/rankingu.

Agent nie zapisuje pełnych postów/wątków ani nazw użytkowników Reddit.

## Idempotencja

- Stabilne fingerprinty i stabilne `cluster_id`.
- Brak duplikatów obserwacji dla niezmienionego wejścia.
- Marker issue: `<!-- LIBRECARE_DISCOVERY_CLUSTER: DISC-... -->`.
- Marker jest sprawdzany w open+closed issue przed utworzeniem nowego kandydata.

## Reddit

Reddit jest opcjonalny.

Bez credentials (`REDDIT_CLIENT_ID`, `REDDIT_CLIENT_SECRET`) status źródła:

- `REDDIT_DISABLED`

Run trwa dalej (status może być `DEGRADED`).

## Uruchomienie ręczne

```bash
python scripts/product/discovery_cli.py \
  --sources-file product/discovery/sources.json \
  --max-items-per-source 20 \
  --ai-mode copilot \
  --ai-model gpt-5.4-mini
```

Pierwszy realny run:

- `publish_top3=false` (domyślnie),
- najpierw review raportu TOP 10,
- dopiero potem ewentualne publikowanie TOP 3.

## Persistence w repozytorium

Discovery v1 nie wykonuje `git commit/push` w workflow. Jeśli po review artifactu
chcesz utrwalić wynik w repo, zrób osobny commit na branchu i osobny PR z review człowieka.

## Human decision gate

Discovery tworzy tylko kandydatów. Decyzje `ACCEPT/HOLD/REJECT` pozostają po stronie człowieka w istniejącym workflow Product Inbox.

