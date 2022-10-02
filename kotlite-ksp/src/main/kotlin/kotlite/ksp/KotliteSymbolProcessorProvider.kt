package kotlite.ksp

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import kotlite.ksp.model.klass.QualifiedName

class KotliteSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): KotliteSymbolProcessor {
        val s = environment.options["kotlite.db.qualifiedName"]
            ?: error("""Cannot find option 'kotlite.db.qualifiedName', please configure KSP in build.gradle: ksp { arg("kotlite.db.qualifiedName", "my.pack.DB")}""")
        val parts = s.split(".")
        val dbQualifiedName = QualifiedName(pkg = parts.dropLast(1).joinToString("."), name = parts.last())
        val options = KotliteOptions(
            dbQualifiedName,

        )
        return KotliteSymbolProcessor(environment.codeGenerator, environment.logger, options)
    }
}
