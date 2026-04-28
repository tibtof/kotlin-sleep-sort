package com.github.tibof

import kotlin.time.measureTimedValue

suspend fun main() {
    val unsorted = listOf(5, 1, 3, 2, 1, 2, 3, 4)
    val (sorted, duration) = measureTimedValue { unsorted.sleepSort() }
    println("It took $duration to sort $sorted")
}
