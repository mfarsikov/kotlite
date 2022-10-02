package kotlite.annotations

/**
 * Statement returns nothing. Only void functions can be marked as statements
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Statement(
    val value: String,
)
