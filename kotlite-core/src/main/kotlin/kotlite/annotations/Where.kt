package kotlite.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Where(
    val value: String,
)