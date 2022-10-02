package kotlite.annotations

/**
 * Should be placed on interfaces, and for such interface KSP will generate implementation
 */
@Target(AnnotationTarget.CLASS)
annotation class SqliteRepository(
    /**
     * Can be either simple class name or fully qualified class name.
     * In case if it is a simple class name, a package name should be configured in TODO
     */
    val belongsToDb: String = "",
)
