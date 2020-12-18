package kotlite.kapt.common

import kotlite.kapt.model.klass.KlassFunction
import kotlite.kapt.model.klass.QualifiedName

data class Pagination(
    val parameterName: String,
)

val pageableQualifiedName = QualifiedName("kotlite.aux.page", "Pageable")
val pageQualifiedName = QualifiedName("kotlite.aux.page", "Page")

fun KlassFunction.paginationParameter(): Pagination? {
    val paginationParameter = parameters.firstOrNull { it.type.klass.name == pageableQualifiedName }
    if ((returnType.klass.name == pageQualifiedName) xor (paginationParameter != null)) {
        throw KotliteException("Function ${name} must have both Pageable parameter and Page return type")
    }
    return paginationParameter?.let { Pagination(it.name) }
}

