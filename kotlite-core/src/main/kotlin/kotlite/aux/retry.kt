package kotlite.aux

import kotlin.reflect.KClass
import kotlite.aux.exception.OptimisticLockFailException

fun <R> retry(times: Int = 3, expect: KClass<out Throwable> = OptimisticLockFailException::class, block: () -> R): R {
    if (times < 1) throw IllegalArgumentException("'times' should be greater than 0")

    var counter = 0
    var lastCaughtEx: Exception

    do {
        try {
            return block()
        } catch (ex: Exception) {
            if (expect.isInstance(ex)) {
                lastCaughtEx = ex
            } else {
                throw ex
            }
        }
        counter++
    } while (counter < times)

    throw lastCaughtEx
}
