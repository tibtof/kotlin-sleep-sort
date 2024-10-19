import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * A suspended function for sorting a list of positive integers
 * using the sleep sort algorithm.
 *
 * The function leverages coroutines to delay each element
 * by a time proportional to its value.
 *
 * The function is suspended and creates a coroutine scope
 * because we need to launch multiple coroutines from it
 *
 * Note: The algorithm only works with positive integers.
 */
suspend fun List<Int>.sleepSort() = coroutineScope {
    // Create a channel to collect results as they complete.
    // Channels are a natural choice for receiving
    // elements from concurrent operations.
    val sorted = Channel<Int>(size)

    // Launch a new coroutine, that lasts until all
    // the elements are sorted and the channel can be closed.
    launch {
        // Iterate over each element in the list,
        // launching a new coroutine for each.
        map { i ->
            launch {
                // Delay for a time equal to the element in minutes.
                // This simulates the "sleeping" part of sleep sort.
                delay(i.seconds)
                // Once the delay is complete, send the element to the channel.
                sorted.send(i)
            }
        }.joinAll() // Wait for all launched coroutines to finish execution.
        // Close the channel after all elements have been sent,
        // signaling that no more data is coming.
        sorted.close()
    }

    // Consume the elements from the channel as they arrive and collect them into a list.
    // This will be the sorted order based on the delays.
    sorted.consumeAsFlow().toList()
}

suspend fun main() {
    val unsorted = listOf(5, 1, 3, 2, 1, 2, 3, 4)

    val (sorted, duration) = measureTimedValue { unsorted.sleepSort() }

    println("It took $duration to sort $sorted")
}

class SleepSortTest {

    @Test
    fun `sleep short should sort a list correctly`() = runTest {
        val unsorted = (1..100_000).map { Random.nextInt(0, Int.MAX_VALUE) }

        val (defaultSortResult, defaultSortDuration) = measureTimedValue {
            unsorted.sorted()
        }

        val (sleepSortResult, sleepSortDuration) = measureTimedValue {
            unsorted.sleepSort()
        }

        assertEquals(defaultSortResult, sleepSortResult)
        println("Default sort duration: $defaultSortDuration")
        println("Sleep sort duration: $sleepSortDuration")
    }
}
