package kotlite.annotations

import kotlite.aux.SqliteType

@Target(AnnotationTarget.FIELD)
annotation class Column(
    val name: String = "",
    val type: SqliteType = SqliteType.NONE,
)