package kotgres.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Statement(
    val value: String
)
