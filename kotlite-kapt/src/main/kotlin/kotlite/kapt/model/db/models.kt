package kotlite.kapt.model.db

import kotlite.aux.ColumnDefinition
import kotlite.kapt.model.klass.Klass
import kotlite.kapt.model.klass.Type
import kotlite.kapt.sqlite.repository.ObjectConstructor

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
