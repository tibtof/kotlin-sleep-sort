# kotlin-sleep-sort

[![Build](https://github.com/tibtof/kotlin-sleep-sort/actions/workflows/build.yml/badge.svg)](https://github.com/tibtof/kotlin-sleep-sort/actions/workflows/build.yml)

Support code for [Sleep, Sort, Repeat](https://medium.com/@tibtof/sleep-sort-repeat-cb11fb27a8c3) — an article on the sleep sort algorithm and how to test coroutines.

## What it does

`List<Int>.sleepSort()` is a `suspend` extension that "sorts" a list of non-negative integers by launching one coroutine per element, delaying each by a duration proportional to the value, and collecting the results in arrival order through a `Channel`.

```kotlin
val sorted = listOf(5, 1, 3, 2, 4).sleepSort()
// → [1, 2, 3, 4, 5]
```

Each element waits `value.seconds` before being emitted, so on a real dispatcher the wall-clock cost grows with the largest element. The interesting part is what coroutines and `runTest` let you do about that — see the article.

## Build & run

Requires JDK 21+ (the toolchain will fetch one via [foojay](https://github.com/gradle/foojay-toolchains) if needed).

```bash
./gradlew build       # compile + test
./gradlew run         # not configured by default — use the test instead
./gradlew test        # run the test suite
```

The test in `src/test/kotlin/SleepSortTest.kt` sorts 100,000 random ints and finishes instantly because `runTest` advances virtual time — `delay` doesn't actually wait.

## Project layout

```
src/main/kotlin/
  SleepSort.kt    # the suspend extension
  Main.kt         # demo entry point
src/test/kotlin/
  SleepSortTest.kt
gradle/
  libs.versions.toml   # centralized dependency catalog
```

## Code style

[ktlint](https://pinterest.github.io/ktlint/) runs as part of `./gradlew check`. To auto-fix violations:

```bash
./gradlew ktlintFormat
```

Optional [pre-commit](https://pre-commit.com/) hook (runs `ktlintCheck` before each commit):

```bash
pre-commit install
```

## Caveats

- Only non-negative integers — negative values would throw on `delay`.
- Not a sort to use in production. It's a coroutine demo, not an algorithm recommendation.
