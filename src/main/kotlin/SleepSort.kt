package com.github.tibof

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Sorts a list of non-negative integers using the sleep sort algorithm,
 * implemented with Kotlin coroutines.
 *
 * For each element a coroutine is launched that delays for a duration
 * proportional to the element's value, then sends the element through a
 * channel. Because larger values resume later, elements arrive in
 * ascending order.
 *
 * Note: only works with non-negative integers; negative delays throw.
 */
suspend fun List<Int>.sleepSort(): List<Int> =
    coroutineScope {
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
