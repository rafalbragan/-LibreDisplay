# LibreCare Performance Plan

## Status

Not implemented yet. This is a forward-looking structure for future macrobenchmark work.

## Goals

- measure cold start
- measure first screen render
- measure chart interaction latency
- measure navigation latency
- detect regressions in memory and frame timing

## Proposed module layout

- `benchmark/` or a dedicated Gradle module for macrobenchmarks
- separate benchmark build variant
- benchmark-only test data with deterministic scenarios

## Suggested benchmark categories

1. app start to first stable frame
2. open Home screen from the launch state
3. switch between Home and History
4. open full-screen history from chart tap
5. open Settings and a nested settings screen

## Guardrails

- no random data without a fixed seed
- no production code changes just for benchmark setup
- benchmark fixtures must not leak secrets
- keep benchmark assertions stable across devices

## Next step

Create the benchmark module only after the UI and navigation test infrastructure is stable.

