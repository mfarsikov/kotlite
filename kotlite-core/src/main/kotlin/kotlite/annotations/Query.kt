package kotlite.annotations


@Target(AnnotationTarget.FUNCTION)
annotation class Query(
    val value: String,
)