package kotlite.annotations

/**
 * text from this annotation inserted to ORDER BY clause as is
 */
@Target(AnnotationTarget.FUNCTION)
annotation class OrderBy(
    val value: String
)
