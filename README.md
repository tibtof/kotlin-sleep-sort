# kotlin-sleep-sort

[![Build](https://github.com/tibtof/kotlin-sleep-sort/actions/workflows/build.yml/badge.svg)](https://github.com/tibtof/kotlin-sleep-sort/actions/workflows/build.yml)

Support code for [Sleep, Sort, Repeat](https://dev.to/tibtof/sleep-sort-repeat-testing-kotlin-coroutines-with-virtual-time-25pl) — an article on the sleep sort algorithm and how to test coroutines.

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

The test in `src/test/kotlin/SleepSortTest.kt` sorts 100,000 random ints and finishes in about a second because `runTest` advances virtual time — `delay` doesn't actually wait.

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

- Only non-negative integers. `delay` returns immediately for non-positive durations rather than throwing, so negative values race to the channel and produce silently wrong output. A defensive version would start with `require(all { it >= 0 })`.
- Not a sort to use in production. It's a coroutine demo, not an algorithm recommendation.

---

## Going deeper

The rest of this README expands on what the article gestures at — implementation details, edge cases I ran into, and the failure modes of virtual time that didn't fit in a five-minute read.

### How the implementation works

```kotlin
suspend fun List<Int>.sleepSort(): List<Int> = coroutineScope {
    val sorted = Channel<Int>(size)

    launch {
        map { i ->
            launch {
                delay(i.seconds)
                sorted.send(i)
            }
        }.joinAll()
        sorted.close()
    }

    sorted.consumeAsFlow().toList()
}
```

A few details worth knowing:

- **Two layers of `launch`.** The outer `launch` is a single supervisor that fans out one coroutine per element, joins on all of them, then closes the channel. Without it, the function couldn't return until every element had finished sleeping, blocking the consumer until the very end.
- **The channel is buffered to `size`.** Sends never suspend, so each producer wakes up, sends, and exits immediately.
- **The result is built lazily.** `consumeAsFlow().toList()` collects from the channel in parallel with the producers. Elements arrive in delay-order — which is sorted order.
- **`coroutineScope` does the cleanup.** Cancellation propagates correctly, no coroutines leak if the caller times out.

### Notes from poking at the edges

A few findings from running variations of the code:

- **Nanoseconds don't sort.** `delay(i.nanoseconds)` produces incorrect output even under virtual time. The scheduler's clock has finite resolution; below it, distinct wake-up times collide.
- **Milliseconds are a fault line.** `delay(i.milliseconds)` works under `runTest` but not from `main`. Real OS scheduling jitter exceeds a millisecond, so coroutines wake out of order. Virtual time has zero jitter, so the sort holds there only.
- **Seconds work everywhere.** Coarse enough to survive real-world scheduler jitter.
- **Memory ceiling.** Sorting 1,000,000 ints under `runTest` takes about 12 seconds. 10,000,000 runs out of heap. Coroutines are cheap, not free.

The pattern: virtual time is faithful to the model of `delay`, not to the physics of a real CPU. It assumes infinite-precision clocks, zero scheduling jitter, and unlimited memory — and gives you exactly that.

### When virtual time will lie to you

Virtual time is fast because it's a model, not a simulator. The contract: *all the time in your code flows through `delay`*. Where that breaks, tests can pass while production is broken.

Cases to watch for:

- **Wall-clock APIs ignore `runTest`.** `Clock.System.now()`, `Instant.now()`, `System.currentTimeMillis()` all return real time. Quick demo inside a `runTest` block:

```kotlin
  println(Clock.System.now())
  delay(10_000_000)  // ten million milliseconds — almost three hours
  println(Clock.System.now())
```

```
  2026-04-28T10:37:18.758510Z
  2026-04-28T10:37:18.759692Z
```

About a millisecond of real time elapsed while the virtual clock advanced nearly three hours. Two stamps an hour apart in production will be microseconds apart in test. The fix is to inject a `Clock` and substitute the test scheduler's virtual time source in tests.

- **Wall-clock-based timeouts won't fire.** Code comparing `System.currentTimeMillis() - start > TIMEOUT_MS` sees real time stand still while virtual delays advance freely. The bug ships, the test stays green.
- **Blocking ≠ suspending.** `Thread.sleep`, blocking I/O, `Future.get()` — none of these participate in virtual time. They block real threads for real time and can deadlock the test if they're waiting on virtual-time progress.
- **External systems run on real clocks.** Testcontainers, embedded databases, anything over the network. A virtual `delay` between a write and a read will fire instantly in test; the database is still doing real work on real time.
- **Recurring jobs collapse.** `advanceTimeBy(24.hours)` on an hourly job runs 24 iterations back-to-back, not spread out. Fine if your iterations are independent; broken if they aren't.

Mental model: virtual time tests *dependencies between* suspending operations, not their *absolute timing*. Use it for "this should happen after that," not for "this should happen one wall-clock second after that."
