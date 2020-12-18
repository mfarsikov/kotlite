package kotlite.kapt.model.klass

import kotlite.aux.SqliteType

fun Klass.isJavaPrimitive() = name in primitives

val primitives = listOf(
    QualifiedName("kotlin","Int" ),
    QualifiedName("kotlin","Long" ),
    QualifiedName("kotlin","Float" ),
    QualifiedName("kotlin","Double" ),
    QualifiedName("kotlin","Boolean" ),
)

val jdbcTypeMappingsForPrimitives = mapOf(
    SqliteType.INTEGER to "INTEGER",
    SqliteType.BIGINT to "BIGINT",
    SqliteType.REAL to "FLOAT",
    SqliteType.DOUBLE to "DOUBLE",
    SqliteType.BOOLEAN to "BOOLEAN",
)