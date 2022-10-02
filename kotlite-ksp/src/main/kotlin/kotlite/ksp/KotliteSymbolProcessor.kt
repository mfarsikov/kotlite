package kotlite.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import kotlite.ksp.mapper.toRepo
import kotlite.ksp.mapper.validationErrors
import kotlite.ksp.model.klass.Klass
import kotlite.ksp.model.klass.QualifiedName
import kotlite.ksp.sqlite.generator.DbDescription
import kotlite.ksp.sqlite.generator.generateDb
import kotlite.ksp.sqlite.generator.generateRepository

class KotliteSymbolProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val options: KotliteOptions,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor = KotliteSymbolVisitor(logger)
        val classes = resolver.getSymbolsWithAnnotation("kotlite.annotations.SqliteRepository")
            .toList()
            .map { it.accept(visitor, Unit) as Klass }

        // classes.forEach { logger.info("parsed: $it") }
        visitor.cache[QualifiedName("my.pack", "MyNestedNestedClass")]?.also { logger.info(it.toString()) }

        classes.forEach { cls ->
            validationErrors(cls).takeIf { it.isNotEmpty() }?.forEach { err -> logger.error(err) }
        }
        val repos = classes.map { it.toRepo(options.dbQualifiedName) }

        val repoFiles = repos.map { generateRepository(it) }

        // TODO set dependencies
        repoFiles.forEach { fileSpec ->
            val f = codeGenerator.createNewFile(Dependencies.ALL_FILES, fileSpec.packageName, fileSpec.name)

            val b = StringBuilder().also {
                fileSpec.writeTo(it)
            }
            f.write(b.toString().toByteArray())
        }

        repos.groupBy { it.belongsToDb }.forEach { (dbQualifiedName, repos) ->
            val dbFile = generateDb(
                dbDescription = DbDescription(
                    pkg = dbQualifiedName.pkg,
                    name = dbQualifiedName.name,
                    repositories = repos,
                    spring = false, // TODO
                ),
            )

            val file = codeGenerator.createNewFile(
                Dependencies.ALL_FILES, // TODO
                dbFile.packageName,
                dbFile.name,
            )

            val b = StringBuilder().also {
                dbFile.writeTo(it)
            }
            file.write(b.toString().toByteArray())
        }

        return emptyList()
    }
}
