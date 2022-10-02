package kotlite.ksp.sqlite.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import io.github.enjoydambience.kotlinbard.CodeBlockBuilder
import io.github.enjoydambience.kotlinbard.TypeSpecBuilder
import io.github.enjoydambience.kotlinbard.addClass
import io.github.enjoydambience.kotlinbard.addCode
import io.github.enjoydambience.kotlinbard.addFunction
import io.github.enjoydambience.kotlinbard.buildFile
import io.github.enjoydambience.kotlinbard.controlFlow
import io.github.enjoydambience.kotlinbard.`for`
import io.github.enjoydambience.kotlinbard.`if`
import io.github.enjoydambience.kotlinbard.nullable
import io.github.enjoydambience.kotlinbard.`while`
import kotlite.annotations.Generated
import kotlite.ksp.model.klass.Klass
import kotlite.ksp.model.klass.Nullability
import kotlite.ksp.model.klass.Type
import kotlite.ksp.model.klass.isJavaPrimitive
import kotlite.ksp.model.klass.jdbcTypeMappingsForPrimitives
import kotlite.ksp.parser.KotlinType
import kotlite.ksp.sqlite.repository.ObjectConstructor
import kotlite.ksp.sqlite.repository.QueryMethod
import kotlite.ksp.sqlite.repository.QueryMethodType
import kotlite.ksp.sqlite.repository.QueryParameter
import kotlite.ksp.sqlite.repository.Repo

fun generateRepository(repo: Repo): FileSpec {
    return buildFile(repo.superKlass.name.pkg, "${repo.superKlass.name.name}Impl.kt") {
        addClass("${repo.superKlass.name.name}Impl") {
            addAnnotation(Generated::class)
            addModifiers(KModifier.INTERNAL)
            addSuperinterface(ClassName(repo.superKlass.name.pkg, repo.superKlass.name.name))
            primaryConstructor(
                PropertySpec.builder("connection", ClassName("java.sql", "Connection"), KModifier.PRIVATE).build(),
            )

            repo.queryMethods
                .forEach { queryMethod ->
                    when (queryMethod.type) {
                        QueryMethodType.SINGLE -> generateQueryMethod(queryMethod)
                        QueryMethodType.BATCH -> generateBatchQueryMethod(queryMethod)
                    }
                }
        }
    }
}

private fun TypeSpecBuilder.generateQueryMethod(
    queryMethod: QueryMethod,
) {
    addFunction(queryMethod.name) {
        addModifiers(KModifier.OVERRIDE)
        returns(queryMethod.returnType.toTypeName())
        addParameters(
            queryMethod.queryMethodParameters.map { param ->
                ParameterSpec(
                    name = param.name,
                    type = param.type.toTypeName(),
                )
            },
        )

        addCode {
            addStatement("val query = %S", queryMethod.query)

            indent()
            if (queryMethod.orderParameterName != null) {
                addStatement(
                    ".replace(%S, %L.stringify())",
                    "%orderBy",
                    queryMethod.orderParameterName,
                )
            }

            queryMethod.queryParameters.filter { it.isINClause }
                .forEach {
                    when (KotlinType.of(it.kotlinType.typeParameters.single().klass.name)) {
                        KotlinType.STRING -> addStatement(
                            ".replace(%S, %L.joinToString { it.%M() })",
                            "%${it.path}",
                            it.path,
                            MemberName("kotlite.aux", "quote"),
                        )

                        KotlinType.INT, KotlinType.FLOAT, KotlinType.BIG_DECIMAL -> addStatement(
                            ".replace(%S, %L.joinToString())",
                            "%${it.path}",
                            it.path,
                        )

                        KotlinType.UUID, KotlinType.LOCAL_DATE_TIME, KotlinType.LOCAL_DATE, KotlinType.LOCAL_TIME -> addStatement(
                            ".replace(%S, %L.joinToString { it.toString().%M() })",
                            "%${it.path}",
                            it.path,
                            MemberName("kotlite.aux", "quote"),
                        )

                        KotlinType.DATE, KotlinType.TIMESTAMP -> addStatement(
                            ".replace(%S, %L.joinToString { it.time.toString() })",
                            "%${it.path}",
                            it.path,
                        )

                        else -> error("unsupported type for IN clause: ${it.kotlinType.typeParameters.single().klass.name}")
                    }
                }
            unindent()

            controlFlow("return connection.prepareStatement(query).use") {
                generateParametersSetBlock(queryMethod.queryParameters.filter { !it.isINClause }, "")

                if (queryMethod.returnType.klass.name != KotlinType.UNIT.qn) {
                    controlFlow("it.executeQuery().use") {
                        if (queryMethod.returnsCollection || queryMethod.pagination != null) {
                            generateCollectionExtractor(queryMethod)
                        } else {
                            generateSingleElementExtractor(queryMethod)
                        }
                    }
                } else {
                    when {
                        queryMethod.optimisticallyLocked -> {
                            addStatement("val rows = it.executeUpdate()")
                            `if`("rows != 1") {
                                addStatement(
                                    "throw %M()",
                                    MemberName("kotlite.aux.exception", "OptimisticLockFailException"),
                                )
                            }
                        }

                        queryMethod.isStatement -> addStatement("it.execute()")
                        else -> addStatement("it.executeUpdate()")
                    }
                }
            }
        }
    }
}

private fun CodeBlockBuilder.generateCollectionExtractor(queryMethod: QueryMethod) {
    addStatement(
        "val acc = mutableListOf<%T%L>()",
        queryMethod.trueReturnType.klass.toClassName(),
        if (queryMethod.trueReturnType.nullability == Nullability.NULLABLE) "?" else "",
    )
    `while`("it.next()") {
        addStatement("acc +=")
        indent()
        if (queryMethod.returnsScalar) {
            generateScalarExtraction(queryMethod.objectConstructor as ObjectConstructor.Extractor)
        } else {
            generateConstructorCall(queryMethod.objectConstructor!!)
        }
        unindent()
    }
    if (queryMethod.pagination != null) {
        addStatement(
            "Page(%L, acc)",
            queryMethod.pagination.parameterName,
        ) // TODO use correct parameter name
    } else {
        addStatement("acc")
    }
}

private fun CodeBlockBuilder.generateSingleElementExtractor(
    queryMethod: QueryMethod,
) {
    `if`("it.next()") {
        addStatement("val result =")
        indent()
        if (queryMethod.returnsScalar) {
            generateScalarExtraction(queryMethod.objectConstructor as ObjectConstructor.Extractor)
        } else {
            generateConstructorCall(queryMethod.objectConstructor!!)
        }
        unindent()
        if (!queryMethod.returnsCollection) {
            `if`("it.next()") {
                addStatement(
                    "throw %T(%S)",
                    IllegalStateException::class,
                    "Query has returned more than one element",
                )
            }
        }
        addStatement("result")
    } `else` {
        if (queryMethod.returnType.nullability == Nullability.NULLABLE) {
            addStatement("null")
        } else {
            addStatement("throw %T()", NoSuchElementException::class)
        }
    }
}

private fun CodeBlockBuilder.generateScalarExtraction(extractor: ObjectConstructor.Extractor) {
    when {
        extractor.isJson && extractor.isNullable -> addStatement(
            "it.getString(1)?.let { %M.%M(it) }",
            MemberName("kotlinx.serialization.json", "Json"),
            MemberName("kotlinx.serialization", "decodeFromString"),
        )

        extractor.isJson && !extractor.isNullable -> addStatement(
            "%M.%M(it.getString(1))",
            MemberName("kotlinx.serialization.json", "Json"),
            MemberName("kotlinx.serialization", "decodeFromString"),
        )

        extractor.resultSetGetterName == "Object" && extractor.isNullable -> when (extractor.kotlinType) {
            KotlinType.LOCAL_DATE, KotlinType.LOCAL_TIME, KotlinType.LOCAL_DATE_TIME -> addStatement(
                "it.getString(1)?.let { %M.parse(it) }",
                MemberName(extractor.fieldType.pkg, extractor.fieldType.name),
            )

            KotlinType.UUID ->
                addStatement(
                    "it.getString(1)?.let { %M.fromString() }",
                    MemberName(extractor.fieldType.pkg, extractor.fieldType.name),
                )

            else -> error("unexpected type: ${extractor.kotlinType}")
        }

        extractor.resultSetGetterName == "Object" && !extractor.isNullable -> when (extractor.kotlinType) {
            KotlinType.LOCAL_DATE, KotlinType.LOCAL_TIME, KotlinType.LOCAL_DATE_TIME -> addStatement(
                "%M.parse(it.getString(1))",
                MemberName(extractor.fieldType.pkg, extractor.fieldType.name),
            )

            KotlinType.UUID -> addStatement(
                "%M.fromString(it.getString(1))",
                MemberName(extractor.fieldType.pkg, extractor.fieldType.name),
            )

            else -> error("unexpected type: ${extractor.kotlinType}")
        }

        extractor.isEnum && extractor.isNullable -> addStatement(
            "it.getString(1)?.let { %M.valueOf(it) }",
            MemberName(extractor.fieldType.pkg, extractor.fieldType.name),
        )

        extractor.isEnum && !extractor.isNullable -> addStatement(
            "%M.valueOf(it.getString(1))",
            MemberName(extractor.fieldType.pkg, extractor.fieldType.name),
        )

        extractor.isNullable && extractor.isPrimitive -> addStatement(
            "it.get${extractor.resultSetGetterName}(1).takeIf { _ -> !it.wasNull() }",
        )

        else -> addStatement(
            "it.get${extractor.resultSetGetterName}(1)",
        )
    }
}

fun Klass.toClassName() = ClassName(name.pkg, name.name)

fun Type.toTypeName(): TypeName {
    var cn = klass.toClassName()
    if (nullability == Nullability.NULLABLE) cn = cn.nullable

    return if (typeParameters.isNotEmpty()) {
        val paramType = typeParameters.single()
        val paramClassName = paramType.klass.toClassName()
        val paramWithNullability = if (paramType.nullability == Nullability.NULLABLE) {
            paramClassName.nullable
        } else {
            paramClassName
        }
        cn.parameterizedBy(paramWithNullability)
    } else cn
}

private fun CodeBlockBuilder.generateConstructorCall(c: ObjectConstructor, isTop: Boolean = true) {
    when (c) {
        is ObjectConstructor.Constructor -> {
            val fName = c.fieldName?.let { "$it =" } ?: ""
            addStatement("%L %T(", fName, ClassName(c.className.pkg, c.className.name))
            indent()
            c.nestedFields.forEach { generateConstructorCall(it, false) }
            unindent()
            val trailingComma = if (isTop) "" else ","
            addStatement(")$trailingComma")
        }

        is ObjectConstructor.Extractor -> {
            when {
                c.isJson -> addStatement(
                    "%L = %M.%M(it.getString(%S)),",
                    c.fieldName,
                    MemberName("kotlinx.serialization.json", "Json"),
                    MemberName("kotlinx.serialization", "decodeFromString"),
                    c.columnName,
                )

                c.resultSetGetterName == "Object" -> when (c.kotlinType) { // TODO nullable
                    KotlinType.LOCAL_DATE, KotlinType.LOCAL_DATE_TIME, KotlinType.LOCAL_TIME ->
                        if (c.isNullable) {
                            addStatement(
                                "%L = it.getString(%S)?.let { %M.parse(it) },",
                                c.fieldName,
                                c.columnName,
                                MemberName(c.fieldType.pkg, c.fieldType.name),
                            )
                        } else {
                            addStatement(
                                "%L = %M.parse(it.getString(%S)),",
                                c.fieldName,
                                MemberName(c.fieldType.pkg, c.fieldType.name),
                                c.columnName,
                            )
                        }

                    KotlinType.UUID ->
                        if (c.isNullable) {
                            addStatement(
                                "%L = it.getString(%S)?.let { %M.fromString(it) },",
                                c.fieldName,
                                c.columnName,
                                MemberName(c.fieldType.pkg, c.fieldType.name),
                            )
                        } else {
                            addStatement(
                                "%L = %M.fromString(it.getString(%S)),",
                                c.fieldName,
                                MemberName(c.fieldType.pkg, c.fieldType.name),
                                c.columnName,
                            )
                        }

                    else -> error("unexpected kotlin type: ${c.kotlinType}")
                }

                c.isEnum -> if (c.isNullable) {
                    addStatement(
                        "%L = it.getString(%S)?.let { %M.valueOf(it) },",
                        c.fieldName,
                        c.columnName,
                        MemberName(c.fieldType.pkg, c.fieldType.name),
                    )
                } else {
                    addStatement(
                        "%L = %M.valueOf(it.getString(%S)),",
                        c.fieldName,
                        MemberName(c.fieldType.pkg, c.fieldType.name),
                        c.columnName,
                    )
                }

                c.isPrimitive && c.isNullable -> addStatement(
                    "%L = it.get${c.resultSetGetterName}(%S).takeIf { _ -> !it.wasNull() },",
                    c.fieldName,
                    c.columnName,
                )

                else -> {
                    addStatement(
                        "%L = it.get${c.resultSetGetterName}(%S),",
                        c.fieldName,
                        c.columnName,
                    )
                }
            }
        }
    }
}

private fun TypeSpecBuilder.generateBatchQueryMethod(queryMethod: QueryMethod) {
    addFunction(queryMethod.name) {
        addModifiers(KModifier.OVERRIDE)

        val queryMethodParameter = queryMethod.queryMethodParameters.single()
        addParameter(
            ParameterSpec(queryMethodParameter.name, queryMethodParameter.type.toTypeName()),
        )

        addCode {
            addStatement("val query = %S", queryMethod.query)
            controlFlow("connection.prepareStatement(query).use") {
                `for`("item in ${queryMethodParameter.name}") {
                    generateParametersSetBlock(queryMethod.queryParameters, "item.")

                    addStatement("it.addBatch()")
                }
                if (queryMethod.optimisticallyLocked) {
                    addStatement("val rows = it.executeBatch()")
                    `if`("rows.sum() != ${queryMethodParameter.name}.size") {
                        addStatement(
                            "throw %M()",
                            MemberName("kotlite.aux.exception", "OptimisticLockFailException"),
                        )
                    }
                } else {
                    addStatement("it.executeBatch()")
                }
            }
        }
    }
}

private fun CodeBlockBuilder.generateParametersSetBlock(
    queryParameters: List<QueryParameter>,
    itemPrefix: String,
) {
    for (param in queryParameters) {
        when {
            param.isJson -> addStatement(
                """it.setString(%L, %M.%M($itemPrefix%L))""",
                param.positionInQuery,
                MemberName("kotlinx.serialization.json", "Json"),
                MemberName("kotlinx.serialization", "encodeToString"),
                param.path,
            )

            param.isEnum -> addStatement(
                "it.setString(%L, $itemPrefix%L%L.name)",
                param.positionInQuery,
                param.path,
                if (param.kotlinType.nullability == Nullability.NULLABLE) "?" else "",
            )

            param.kotlinType.klass.isJavaPrimitive() && param.kotlinType.nullability == Nullability.NULLABLE ->
                `if`("$itemPrefix%L == null", param.path) {
                    addStatement(
                        "it.setNull(%L, %M.%L)",
                        param.positionInQuery,
                        MemberName("java.sql", "Types"),
                        jdbcTypeMappingsForPrimitives[param.SqliteType]
                            ?: error("no java.sql.Types mapping for ${param.SqliteType}"),
                    )
                } `else` {
                    addStatement(
                        "it.set${param.setterName}(%L, $itemPrefix%L)",
                        param.positionInQuery,
                        param.path,
                    )
                }

            else -> addStatement(
                "it.set${param.setterName}(%L, $itemPrefix%L)",
                param.positionInQuery,
                param.path,
            )
        }
    }
}

fun TypeSpec.Builder.primaryConstructor(vararg properties: PropertySpec): TypeSpec.Builder {
    val propertySpecs = properties.map { p -> p.toBuilder().initializer(p.name).build() }
    val parameters = propertySpecs.map { ParameterSpec.builder(it.name, it.type).build() }
    val constructor = FunSpec.constructorBuilder()
        .addParameters(parameters)
        .build()

    return this
        .primaryConstructor(constructor)
        .addProperties(propertySpecs)
}
