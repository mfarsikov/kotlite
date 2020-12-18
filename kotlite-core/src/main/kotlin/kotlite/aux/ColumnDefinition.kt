package kotlite.aux

data class ColumnDefinition(
    val name: String,
    val nullable: Boolean,
    val type: SqliteType,
    val isId: Boolean,
    val isVersion: Boolean = false,
){
    override fun toString() = "$name ${type}${if (!nullable) " not null" else "" }"
}