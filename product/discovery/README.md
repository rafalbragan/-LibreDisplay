# LibreCare Discovery Agent v1.5 — Multilingual Evidence Engine

LibreCare Discovery Agent v1.5 zbiera publiczne sygnały produktowe przez
deterministyczne, problemowe zapytania. Priorytetem jest jakość, więc TOP10
może zawierać od 0 do 10 pozycji i nigdy nie jest sztucznie uzupełniany.

## Co robi

- Pobiera dane z jawnie dozwolonych źródeł (`product/discovery/sources.json`) według wersjonowanych zapytań (`query-packs.json`).
- Traktuje polski i angielski jako języki główne; niemiecki, francuski i hiszpański mają mniejsze budżety.
- Normalizuje URL/tekst, wykonuje deduplikację i klastrowanie problemów.
- Porównuje klastry z Product Foundation (`requirements`, `decisions`, `observations`, validated capabilities).
- Uruchamia ograniczoną analizę AI (docelowo `gpt-5.4-mini`, max 2 wywołania/run).
- Stosuje deterministyczne filtry marketingu, technicznych detali i prywatności przed AI.
- Stosuje tiering `WEAK`, `SUPPORTED`, `CORROBORATED`, `STRONG` oraz score caps 49/74/89/100.
- Tworzy raporty w `product/generated/discovery/`.
- `WEAK` trafia wyłącznie na WATCHLIST. TOP10 zawiera tylko jakościowe sygnały co najmniej `SUPPORTED`.
- Opcjonalnie publikuje maks. TOP 3 kandydatów co najmniej `CORROBORATED` (`publish_top3=true`).
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

Agent nie zapisuje pełnych postów/wątków, komentarzy, logów ani nazw użytkowników.
Przed cache/report/observation redaguje e-mail, IP, uchwyty `@`, numery telefonów,
seryjne/długie identyfikatory, tokeny, nagłówki autoryzacji i cookies.

## Role źródeł i jakość dowodów

- `user_community` — bezpośredni sygnał użytkownika.
- `developer_community` — sygnał techniczny, dopuszczony tylko z jawnym wpływem na użytkownika.
- `official_reference` — kontekst/ograniczenie/udokumentowane zachowanie; nigdy samodzielny popyt ani kandydat.

Najnowsze losowe issue i `/new` Reddita zostały zastąpione wyszukiwaniem według
konceptów. `concept_id` stanowi deterministyczny most PL/EN/de/fr/es; podobieństwo
tekstu nadal zapobiega łączeniu różnych problemów.

`WEAK` nie tworzy obserwacji, wpisu proponowanego rejestru ani Product Inbox.
`SUPPORTED` może utworzyć obserwację i propozycję rejestru, ale nie Inbox.
Inbox wymaga `CORROBORATED` lub `STRONG`, zgodności governance i możliwości
rozwiązania po stronie aplikacji.

## Idempotencja

- Stabilne `cluster_id` pochodzą z **stateful** rejestru problemów: `product/discovery/cluster-registry.json`.
- Brak duplikatów obserwacji dla niezmienionego wejścia.
- Marker issue: `<!-- LIBRECARE_DISCOVERY_CLUSTER: DISC-... -->`.
- Marker jest sprawdzany w open+closed issue przed utworzeniem nowego kandydata.

## Cluster registry (stateful identity)

- Discovery ładuje `product/discovery/cluster-registry.json` jako główne źródło tożsamości klastrów.
- Jeśli nowy klaster deterministycznie pasuje do istniejącego wpisu, używany jest istniejący `cluster_id`.
- Jeśli nie ma dopasowania, tworzony jest nowy deterministyczny `cluster_id` i propozycja nowego wpisu.
- Jeśli dopasowanie jest niejednoznaczne (ambiguous), Discovery nie publikuje takiego kandydata do Product Inbox.
- Discovery **nie nadpisuje automatycznie** rejestu w repozytorium w workflow Actions.

## Reddit

Reddit jest opcjonalny.

Bez credentials (`REDDIT_CLIENT_ID`, `REDDIT_CLIENT_SECRET`) status źródła:

- `REDDIT_DISABLED`

Run trwa dalej (status może być `DEGRADED`).

OAuth służy tylko do bounded query search. Token i dane konta nie są zapisywane.

## RSS/Atom i źródła wyłączone

Adapter RSS 2.0/Atom istnieje jako infrastruktura, lecz każde źródło musi mieć
`allowlisted=true` w `sources.json`. Ten release nie włącza niezweryfikowanych feedów.

`mojacukrzyca.org`, Facebook, grupy społecznościowe/prywatne oraz wyniki HTML
Google/Bing nie są automatycznie zbierane. Wymagają osobnego review praw,
regulaminu i prywatności.

## Uruchomienie ręczne

```bash
python scripts/product/discovery_cli.py \
  --sources-file product/discovery/sources.json \
  --query-packs-file product/discovery/query-packs.json \
  --languages pl,en,de,fr,es \
  --primary-languages pl,en \
  --lookback-days 365 \
  --max-items-per-source 30 \
  --ai-mode copilot \
  --ai-model gpt-5.4-mini
```

Pierwszy realny run:

- `publish_top3=false` (domyślnie),
- najpierw review raportu TOP 10,
- dopiero potem ewentualne publikowanie TOP 3.

## Persistence w repozytorium

Discovery v1.5 nie wykonuje `git commit/push` w workflow. Jeśli po review artifactu
chcesz utrwalić wynik w repo, zrób osobny commit na branchu i osobny PR z review człowieka.

W praktyce: run Discovery zapisuje tylko propozycję rejestru do artifactu
`product/generated/discovery/cluster-registry.proposed.json`.
To człowiek decyduje, czy przenieść tę zmianę do `product/discovery/cluster-registry.json`
w oddzielnym, reviewowanym commicie/PR.

## Human decision gate

Discovery tworzy tylko materiał doradczy. Product Owner pozostaje jedynym
autorytetem `ACCEPT/HOLD/REJECT`. AI nie uruchamia implementacji, nie przypisuje
agentów, nie merguje i nie rekomenduje dawek, bolusa, bazy, proporcji insuliny ani zmian terapii.

