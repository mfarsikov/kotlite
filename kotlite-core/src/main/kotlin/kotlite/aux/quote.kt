package kotlite.aux

fun String.quote() = replace("'", "''").let { "'$it'" }
