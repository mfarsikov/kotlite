# Kotlite

Not an ORM.

Generates inspectable SQL queries before compile time rather than in runtime.

`Kotlite = ORM - bullshit`

## Quick start

#### Gradle
```kotlin
plugins {
    kotlin("kapt") version "1.4.32"
    kotlin("plugin.serialization") // for serializing collections as JSON
}

repositories {
    mavenCentral()
    jcenter() // Despite jcenter announced service decommission, Kotlite depends on KotlinBard which is not moved yet
}

dependencies {
  implementation("com.github.mfarsikov:kotlite-core:0.5.0") // library containing annotations and classes used in compile time

  kapt("com.github.mfarsikov:kotlite-kapt:0.5.0") // Kotlin annotation processor, generates repositories code before compilation
}

kapt {
  arguments {
    arg("kotlite.db.qualifiedName", "my.pack.DB") // default database class name
    arg("kotlite.spring", "false") // marks database class as Spring's component
  }
}
```

#### Create entities and declare repositories
```kotlin
import kotlite.annotations.Id
import kotlite.annotations.SqliteRepository
import kotlite.aux.Repository

data class Person(
  @Id
  val id: UUID,
  val name: String,
  val birthDate: LocalDate,
)

@SqliteRepository
interface PersonRepository : Repository<Person> {
    fun save(person: Person)
    fun findBy(birthDate: LocalDate): List<Person>
}

```
#### Generate the code
`./gradlew kaptKotlin` generates in the folder `build/generated/source/kapt` two classes:
`PersonRepositoryImpl` and `DB`
<details>
<summary>Generated code</summary>


```kotlin

@Generated
internal class PersonRepositoryImpl(
  private val connection: Connection
) : PersonRepository {
  
  public override fun findBy(birthDate: LocalDate): List<Person> {
    val query = """
        |SELECT "birth_date", "id", "name"
        |FROM "person"
        |WHERE "birth_date" = ?
        """.trimMargin()
    return connection.prepareStatement(query).use {
      it.setObject(1, birthDate)
      it.executeQuery().use {
        val acc = mutableListOf<Person>()
        while (it.next()) {
          acc +=
             Person(
              birthDate = it.getObject("birth_date", java.time.LocalDate::class.java),
              id = it.getObject("id", UUID::class.java),
              name = it.getString("name"),
            )
        }
        acc
      }
    }
  }

  public override fun save(person: Person): Unit {
    val query = """
        |INSERT INTO "person"
        |("birth_date", "id", "name")
        |VALUES (?, ?, ?)
        |ON CONFLICT (id) DO 
        |UPDATE SET "birth_date" = EXCLUDED."birth_date", "id" = EXCLUDED."id", "name" = EXCLUDED."name"
        |""".trimMargin()
    return connection.prepareStatement(query).use {
      it.setObject(1, person.birthDate)
      it.setObject(2, person.id)
      it.setString(3, person.name)
      it.executeUpdate()
    }
  }
}
```
</details>

#### Usage
```kotlin
val db = DB(dataSource) // create DB access object

db.transaction {
  // inside the transaction all repositories are accessible through 'this'
  personRepository.save(
    Person(
      id = UUID.random(),
      name = "John Doe",
      birthDate = LocalDate.now(),
    )
  )
}

val bornToday = db.transaction(readOnly = true) {
    personRepository.findBy(birthDate = LocalDate.now())
}

```
## Synopsis

* Maps Kotlin classes to Sqlite tables
* Generates SpringData-like repositories with
    * predefined query methods(`saveAll`, `deleteAll`, `findAll`)
    * custom query methods (like `findByLastName`)
    * methods using native SQL (`@Query("select ...")`)
    * query methods returning projections, scalar types and their lists
* Code and queries are generated during build process, before compilation
* Generated code is properly formatted and human-friendly
* Explicit transaction management (DSL instead of annotations driven)
* Sqlite specific
* Uses native SQL and JDBC
* Uses immutable Kotlin data classes as 'entities'
* Maps nested object's properties into a single table (like JPA `@Embeddable`)
* Serializes Kotlin collections as JSONB type in sqlite
* Generates schema validations

Unlike popular ORM:

* No reflection and runtime magic
* No lazy loading
* No automatic joins, and sub selects (no "N+1" problem)
* No query languages other than SQL
* No vendor agnostic
* No implicit type conversions
* No queries triggered by entity's getters
* No "managed" state
* No caches
* No "object relational impedance mismatch"
* No inheritance resolve strategies
* No transaction managers

## Rationale
The intention was to make database interactions (queries and transactions) explicit.
Generate boiler plate code (like trivial queries, and result mappings). 
Give ability to write complex queries, and map their results automatically.
Use full power of Sqlite.

Avoid accidental complexity

## Documentation
https://mfarsikov.github.io/kotlite/

## Example
See `example` project

`./gradlew sqlite-example:kaptKotlin` generates database classes in `example/build/generated/source/kapt/main`

`./gradlew sqlite-example:test` runs real queries against Sqlite DB
