package kotlite.annotations

/**
 * if during insert there is a constraint violation - exception will be raised
 */
@Target(AnnotationTarget.FUNCTION)
annotation class OnConflictFail
