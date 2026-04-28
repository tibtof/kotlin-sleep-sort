import com.github.tibof.sleepSort
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTimedValue

class SleepSortTest {
    @Test
    fun `sleep sort should sort a list correctly`() =
        runTest {
            val unsorted = (1..100_000).map { Random.nextInt(0, Int.MAX_VALUE) }

            val (defaultSortResult, defaultSortDuration) =
                measureTimedValue {
                    unsorted.sorted()
                }

            val (sleepSortResult, sleepSortDuration) =
                measureTimedValue {
                    unsorted.sleepSort()
                }

            assertEquals(defaultSortResult, sleepSortResult)
            println("Default sort duration: $defaultSortDuration")
            println("Sleep sort duration: $sleepSortDuration")
        }
}
