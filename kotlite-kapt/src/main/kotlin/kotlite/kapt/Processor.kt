package kotlite.kapt

import kotlite.annotations.SqliteRepository
import kotlite.kapt.common.Logger
import kotlite.kapt.sqlite.generator.DbDescription
import kotlite.kapt.sqlite.generator.generateDb
import kotlite.kapt.sqlite.generator.generateRepository
import kotlite.kapt.mapper.toRepo
import kotlite.kapt.mapper.validationErrors
import kotlite.kapt.model.klass.Klass
import kotlite.kapt.sqlite.repository.Repo
import kotlite.kapt.parser.Parser
import kotlite.kapt.parser.toQualifiedName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

class Processor : AbstractProcessor() {
    var dbQualifiedName: String? = null
    var spring: String? = null
    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()
    override fun getSupportedOptions() = setOf(
        "kotlite.log.level",
        "kotlite.db.qualifiedName",
        "kotlite.spring"
    )

    override fun getSupportedAnnotationTypes() = setOf(
        SqliteRepository::class,
    )
        .mapTo(mutableSetOf()) { it.qualifiedName }

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        Logger.messager = processingEnv.messager
        processingEnv.options["kotlite.log.level"]
            ?.also { Logger.logLevel = Logger.LogLevel.valueOf(it.uppercase()) }

        spring = processingEnv.options["kotlite.spring"]

        dbQualifiedName = processingEnv.options["kotlite.db.qualifiedName"]
        if (dbQualifiedName == null) Logger.error("kotlite.db.qualifiedName is not specified")//TODO advice
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        if (roundEnv.processingOver()) return false

        val parser = Parser(roundEnv, processingEnv)

        generateRepositories(roundEnv.getElementsAnnotatedWith(SqliteRepository::class.java), parser)

        return true
    }

    private fun generateRepositories(
        repositories: MutableSet<out Element>,
        parser: Parser
    ) {
        val repos = mutableListOf<Repo>()
        repositories.forEach { repoElement ->
            val parsedRepo = parser.parse(repoElement)

            Logger.trace("Parsed repository: $parsedRepo")

            val errors = validationErrors(parsedRepo)

            if (errors.isNotEmpty()) {
                errors.forEach { Logger.error(it) }
                return@forEach
            }

            val repo = parsedRepo.toRepo(
                dbQualifiedName?.toQualifiedName() ?: error("configure database fully qualified name in gradle")
            )
            repos += repo
            val repoFile = generateRepository(repo)

            val file = processingEnv.filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                repoFile.packageName,
                repoFile.name,
                *elementsFromRepo(parsedRepo).toTypedArray()
            )

            file.openWriter().use {
                repoFile.writeTo(it)
            }
        }

        repos.groupBy { it.belongsToDb }.forEach { (dbQualifiedName, repos) ->
            val dbFile = generateDb(
                dbDescription = DbDescription(
                    pkg = dbQualifiedName.pkg,
                    name = dbQualifiedName.name,
                    repositories = repos,
                    spring = spring?.toBoolean() ?: false,
                )
            )

            val file = processingEnv.filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                dbFile.packageName,
                dbFile.name,
                *repos.map { it.superKlass.element }.toTypedArray()
            )

            file.openWriter().use {
                dbFile.writeTo(it)
            }
        }
    }

}

private fun elementsFromRepo(klass: Klass): List<Element> {
    return (klass.superclassParameter?.let { elementsFromFromKlass(it.klass) } ?: emptyList()) + klass.element!!
}

private fun elementsFromFromKlass(klass: Klass): List<Element> {
    if (klass.element == null) return emptyList()

    return klass.fields.flatMap { elementsFromFromKlass(it.type.klass) } + klass.element
}
