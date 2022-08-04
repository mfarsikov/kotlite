package kotlite.ksp.model.db

import kotlite.aux.ColumnDefinition
import kotlite.ksp.model.klass.Klass
import kotlite.ksp.model.klass.Type
import kotlite.ksp.sqlite.repository.ObjectConstructor

data class TableMapping(
    val name: String,
    val klass: Klass,
    val columns: List<ColumnMapping>,
    val objectConstructor: ObjectConstructor?,
)

data class ColumnMapping(
    val path: List<String>,
    val type: Type,
    val column: ColumnDefinition,
)
