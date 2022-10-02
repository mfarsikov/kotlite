package kotlite.annotations

/**
 * SQL query as is. Method returns something. could be used placeholders for parameters like ':param1'
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Query(
    val value: String,
)
