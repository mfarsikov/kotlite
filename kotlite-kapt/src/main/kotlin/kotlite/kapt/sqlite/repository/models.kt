package kotlite.kapt.sqlite.repository

import kotlite.aux.SqliteType
import kotlite.kapt.common.Pagination
import kotlite.kapt.model.db.TableMapping
import kotlite.kapt.model.klass.Klass
import kotlite.kapt.model.klass.QualifiedName
import kotlite.kapt.model.klass.Type
import kotlite.kapt.parser.KotlinType

data class Repo(
    val superKlass: Klass,
    val queryMethods: List<QueryMethod>,
    val mappedKlass: TableMapping?,
    val belongsToDb: QualifiedName,
)

data class QueryMethod(
    val name: String,
    val query: String,
    val queryMethodParameters: List<QueryMethodParameter>,
    val queryParameters: List<QueryParameter>,
    val returnType: Type, //TODO remove?
    val trueReturnType: Type,
    val returnsCollection: Boolean,
    val objectConstructor: ObjectConstructor?,
    val returnsScalar: Boolean = false,
    val pagination: Pagination?,
    val type: QueryMethodType = QueryMethodType.SINGLE,
    val orderParameterName: String? = null,
    val optimisticallyLocked: Boolean,
    val isStatement: Boolean = false,
)

enum class QueryMethodType { SINGLE, BATCH }

data class QueryMethodParameter(
    val name: String,
    val type: Type,
)

data class QueryParameter(
    val positionInQuery: Int,
    val kotlinType: Type,
    val setterName: String,
    val path: String,
    val isJson: Boolean,
    val isEnum: Boolean,
    val isINClause: Boolean,
    val SqliteType: SqliteType,
)

sealed class ObjectConstructor {
    data class Constructor(
        val fieldName: String?,
        val className: QualifiedName,
        val nestedFields: List<ObjectConstructor>,
    ) : ObjectConstructor()

    data class Extractor(
        val resultSetGetterName: String, //TODO remove in favor of kotlinType?
        val columnName: String,
        val fieldName: String?,
        val fieldType: QualifiedName, //TODO remove in favor of kotlinType?
        val isJson: Boolean,
        val isEnum: Boolean,
        val isPrimitive: Boolean,
        val isNullable: Boolean,
        val kotlinType: KotlinType?,
    ) : ObjectConstructor()
}
