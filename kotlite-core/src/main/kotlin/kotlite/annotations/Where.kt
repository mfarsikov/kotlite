package kotlite.annotations

/**
 * Text from this annotation is inserted to WHERE clause. All placeholders like ':param1' are linked to corresponding
 * method parameters and replaced with question marks
 * example: @Where("field_1 = :param1 and field_2 = :param2")
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Where(
    val value: String,
)
