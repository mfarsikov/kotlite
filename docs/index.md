# Kotlite

1. [Standalone repositories](#standalone-repositories)
2. [Dedicated repositories](#dedicated-repositories)
3. [Database object](#database-object)
4. [Transactions](#transactions)

## Setup

TODO

## Standalone repositories

Standalone is a repository without any connected entity. It has no insights on table name and columns structure,
so all the queries should be written manually. The only thing that can be generated - is transforming tuples from result
set to requested entities.

The simplest possible standalone interface is this:

```kotlin
@SqliteRepository
interface PersonRepository 
```

It is useless, but Kotlite can generate implementation for it.
For each interface marked as `@SqliteRepository` task `kaptKotlin` (`./gradlew kaptKotlin`) generates implementations
in  
`build/generated/source/kapt/` folder.

<details>
<summary>Generated code</summary>

```kotlin
@Generated
internal class PersonRepositoryImpl(
  private val connection: Connection
) : PersonRepository 
```

</details>

### Simplest query

Next we can add a function, and since Kotlite does not have enough information to generate a SQL query
each method in standalone repository should have manually written query:

```kotlin
@SqliteRepository
interface PersonRepository {
  @Query("SELECT id, name, birth_date FROM person")
  fun findPeople(): List<Person>
}
```

Method name does not make sense, all information is taken from annotation, input parameter types (or absence of input
parameters)
and the return type.
Kotlite knows that return type is a `List` so it expects multiple results.
Based on list type parameter `Person` it also knows which fields to extract from the `ResultSet`.
By convention field names are converted from `camelCase` to `snake_case`.

<details>
<summary>Generated code</summary>

```kotlin
public override fun findPeople(): List<Person> {
  val query = "SELECT id, name, birth_date FROM person"
  return connection.prepareStatement(query).use {
    it.executeQuery().use {
      val acc = mutableListOf<Person>()
      while (it.next()) {
        acc +=
          Person(
            birthDate = it.getObject("birth_date", LocalDate::class.java),
            id = it.getObject("id", java.util.UUID::class.java),
            name = it.getString("name"),
          )
      }
      acc
    }
  }
}
```

</details>

### Query parameters

Method parameters could be passed to a query:

```kotlin
@Query("SELECT id, name, birth_date FROM person WHERE name = :firstName")
fun selectWhere(firstName: String): List<Person>
```

Note that the parameter name (`firstName`) must match the named placeholder in the query (`:firstName`).
Query parameters are set to the query, and it is safe in terms of SQL injections.

<details>
<summary>Generated code</summary>

```kotlin
  public override fun selectWhere(firstName: String): List<Person> {
  val query = "SELECT id, name, birth_date FROM person WHERE name = ?"
  return connection.prepareStatement(query).use {
    it.setString(1, firstName)
    it.executeQuery().use {
      val acc = mutableListOf<Person>()
      while (it.next()) {
        acc +=
          Person(
            birthDate = it.getObject("birth_date", LocalDate::class.java),
            id = it.getObject("id", UUID::class.java),
            name = it.getString("name"),
          )
      }
      acc
    }
  }
}
```

</details>
### Return types
### Single non-nullable value

If method returns a single non-nullable value

```kotlin
@Query("SELECT id, name, birth_date FROM person WHERE id = :id")
fun selectWhere(id: UUID): Person
```

Generated code will throw exceptions in two cases:

* if query does not have any result
* if query has more than one result

The second case can be changed by marking method with `kotlite.annotations.First`,
which means that if there are more than a single result – just return the first one.

<details>
<summary>Generated code</summary>

```kotlin
public override fun selectWhere(id: java.util.UUID): Person {
  val query = """
      |SELECT id, name, birth_date FROM person WHERE id = ?
      |LIMIT 2
      """.trimMargin()
  return connection.prepareStatement(query).use {
    it.setObject(1, id)
    it.executeQuery().use {
      if (it.next()) {
        if (!it.isLast) {
          throw IllegalStateException("Query has returned more than one element")
        }
         Person(
          birthDate = it.getObject("birth_date", LocalDate::class.java),
          id = it.getObject("id", UUID::class.java),
          name = it.getString("name"),
        )
      }
      else {
        throw NoSuchElementException()
      }
    }
  }
}
```

</details>

### nullable value

If return type is nullable:

```kotlin
@Query("SELECT id, name, birth_date FROM person WHERE id = :id")
fun selectWhere(id: UUID): Person?
```

Generated code returns `null` if there is no result
<details>
<summary>Generated code</summary>

```kotlin
public override fun selectWhere(id: java.util.UUID): Person? {
  val query = """
      |SELECT id, name, birth_date FROM person WHERE id = ?
      |LIMIT 2
      """.trimMargin()
  return connection.prepareStatement(query).use {
    it.setObject(1, id)
    it.executeQuery().use {
      if (it.next()) {
        if (!it.isLast) {
          throw IllegalStateException("Query has returned more than one element")
        }
         Person(
          birthDate = it.getObject("birth_date", LocalDate::class.java),
          id = it.getObject("id", UUID::class.java),
          name = it.getString("name"),
        )
      }
      else {
        null
      }
    }
  }
}
```

</details>

### Pagination

```kotlin
@Query("SELECT id, name, birth_date FROM person WHERE name = :name")
fun select(name: String, pagination: Pageable): Page<Person>
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun select(name: String, pagination: Pageable): Page<Person> {
  val query = """
      |SELECT id, name, birth_date FROM person WHERE name = ?
      |LIMIT ? OFFSET ?
      """.trimMargin()
  return connection.prepareStatement(query).use {
    it.setString(1, name)
    it.setInt(2, pagination.pageSize)
    it.setInt(3, pagination.offset)
    it.executeQuery().use {
      val acc = mutableListOf<Person>()
      while (it.next()) {
        acc +=
           Person(
            birthDate = it.getObject("birth_date", LocalDate::class.java),
            id = it.getObject("id", UUID::class.java),
            name = it.getString("name"),
          )
      }
      Page(pagination, acc)
    }
  }
}
```

</details>

### Scalar return type

```kotlin
@Query("SELECT name WHERE id = :id")
fun selectNameWhere(id: UUID): String
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun selectNameWhere(id: java.util.UUID): String {
  val query = """
      |SELECT name WHERE id = ?
      |LIMIT 2
      """.trimMargin()
  return connection.prepareStatement(query).use {
    it.setObject(1, id)
    it.executeQuery().use {
      if (it.next()) {
        if (!it.isLast) {
          throw IllegalStateException("Query has returned more than one element")
        }
        it.getString(1)
      }
      else {
        throw NoSuchElementException()
      }
    }
  }
}
```

</details>

### List of scalar return type

```kotlin
@Query("SELECT id WHERE name = :name")
fun selectIdsWhere(name: String): List<UUID>
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun selectIdsWhere(name: String): List<java.util.UUID> {
  val query = "SELECT id WHERE name = ?"
  return connection.prepareStatement(query).use {
    it.setString(1, name)
    it.executeQuery().use {
      val acc = mutableListOf<java.util.UUID>()
      while (it.next()) {
        acc +=
          it.getObject(1, UUID::class.java)
      }
      acc
    }
  }
}
```

</details>

### `in` operator

```kotlin
@Query("SELECT id, name, birth_date FROM person WHERE name = ANY :names")
fun selectWhere(names: List<String>): List<Person>
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun selectWhere(names: List<String>): List<Person> {
  val query = "SELECT id, name, birth_date FROM person WHERE name = ANY ?"
  return connection.prepareStatement(query).use {
    it.setArray(1, connection.createArrayOf("text", names.toTypedArray()))
    it.executeQuery().use {
      val acc = mutableListOf<Person>()
      while (it.next()) {
        acc +=
           Person(
            birthDate = it.getObject("birth_date", LocalDate::class.java),
            id = it.getObject("id", UUID::class.java),
            name = it.getString("name"),
          )
      }
      acc
    }
  }
}
```

</details>

### null comparisons

If `name` was nullable

```kotlin
@Query("SELECT id, name, birth_date FROM person WHERE name IS :name")
fun selectWhere(name: String?): List<Person>
```

### Updates

Any update or delete must have a `Unit` return type

```kotlin
@Query("UPDATE person SET name = :name WHERE id = :id")
fun update(id: UUID, name: String)
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun update(id: UUID, name: String): Unit {
  val query = "UPDATE person SET name = ? WHERE id = ?"
  return connection.prepareStatement(query).use {
      it.setString(1, name)
      it.setObject(2, id)
      it.executeUpdate()
  }
}
```

</details>

### Statements

Any statement must have a `Unit` return type

```kotlin
@Statement("SELECT set_config('log_statement', 'all', true)")
fun turnOnLogsOnServerForCurrentTransaction()
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun turnOnLogsOnServerForCurrentTransaction(): Unit {
  val query = "SELECT set_config('log_statement', 'all', true)"
  return connection.prepareStatement(query).use {
    it.execute()
  }
}
```

</details>

## Dedicated repositories

Standalone repositories can generate JDBC code but cannot generate SQL queries,
because they do not know enough context, unlike dedicated repositories,
which are attached to specific entity (table).

### Entity

Entity is a Kotlin data class.
It should be declared in the source code, not imported from a library (maybe this will be changed in future).
Entity should have property types listed in [type mappings](#type-mappings).
There is no required annotations to declare an entity, the simplest declaration is:

```kotlin
data class Person(
  val id: UUID,
  val name: String,
  val birthDate: LocalDate,
)
```

This means that it is attached to table `person` (it is a class name converted from `UpperCamelCase` to `snake_case`)
with columns `id`, `name` and `birth_date` (they are field names converted from `camelCase` to `snake_case`).

### Repository declaration

Each dedicated repository interface must be annotated with `@SqliteRepository` and extend `kotlite.aux.Repository`

```kotlin
import kotlite.annotations.SqliteRepository
import kotlite.aux.Repository

interface PersonRepository: Repository<Person>
```

Thus, Kotlite have enough information to generate SQL.
Table name is taken from class name by converting it from `UpperCamelCase` to `snake_case`.
Column names and their types also taken from the entity. Names are converted from `camelCase` to `snake_case`.

Alternatively table and column name can be explicitly specified in an entity in `kotlite.annotations.Table`
and `kotlite.annotations.Column` annotations.

### Query methods

#### Create

To generate `INSERT` query method name should start from from `save` or it can be explicitly marked
with `kotlite.annotations.Save`.
This method can accept either a single entity or a list of entities

```kotlin
@SqliteRepository
interface PersonRepository: Repository<Person>{
    fun save(person: Person)
    fun saveAll(people: List<Person>)
}
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun save(person: Person): Unit {
    val query = """
        |INSERT INTO person
        |("birth_date", "id", "name")
        |VALUES (?, ?, ?)
        """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setObject(1, person.birthDate)
        it.setObject(2, person.id)
        it.setString(3, person.name)
        it.executeUpdate()
    }
}

public override fun saveAll(people: List<Person>): Unit {
    val query = """
        |INSERT INTO person
        |("birth_date", "id", "name")
        |VALUES (?, ?, ?)
        """.trimMargin()
    connection.prepareStatement(query).use {
        for (item in people) {
            it.setObject(1, item.birthDate)
            it.setObject(2, item.id)
            it.setString(3, item.name)
            it.addBatch()
        }
        it.executeBatch()
    }
}
```

</details>

If the entity has a primary key (at least one of its fields marked with `kotlite.annotations.Id` annotation)
`INSERT/UPDATE` is generated

<details>
<summary>Generated code</summary>

```kotlin
public override fun save(person: Person): Unit {
    val query = """
    |INSERT INTO person
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
```

</details>

To prevent this behaviour mark method with `kotlite.annotations.FailOnConflict` annotation.

```kotlin
@FailOnConflict
fun save(person: Person)
```

#### Delete methods

To generate `DELETE` query method name should start with a `delete` or be marked with `kotlite.annotation.Delete`
annotation.
Delete method without parameters removes all the rows from a table

```kotlin
fun delete()
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun delete(): Unit {
    val query = """
    |DELETE 
    |FROM person
    """.trimMargin()
    return connection.prepareStatement(query).use {
        it.executeUpdate()
    }
}
```

</details>

Also, delete method can accept the entity

```kotlin
fun delete(person: Person)
```

The entity is removed by all fields
<details>
<summary>Generated code</summary>

```kotlin
public override fun delete(person: Person): Unit {
    val query = """
        |DELETE 
        |FROM person
        |WHERE "birth_date" = ? AND "id" = ? AND "name" = ?
        """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setObject(1, person.birthDate)
        it.setObject(2, person.id)
        it.setString(3, person.name)
        it.executeUpdate()
    }
}
```

</details>

If entity has a primary key (at least one field marked with `kotlite.annotations.Id` annotation) it is removed by ID

<details>
<summary>Generated code</summary>

```kotlin
public override fun delete(person: Person): Unit {
    val query = """
    |DELETE 
    |FROM person
    |WHERE "id" = ?
    """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setObject(1, person.id)
        it.executeUpdate()
    }
}
```

</details>

Also delete method can have arbitrary parameters, see [Method with parameters](#method-with-parameters)

#### Optimistic lock

If entity has an integer field marked with `kotlite.annotations.Version`
UPDATE and DELETE queries will contain version check

```kotlin
fun save(person: Person)
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun save(person: Person): Unit {
    val query = """
        |INSERT INTO person
        |("birth_date", "id", "name", "version")
        |VALUES (?, ?, ?, ? + 1)
        |ON CONFLICT (id) DO 
        |UPDATE SET "birth_date" = EXCLUDED."birth_date", "id" = EXCLUDED."id", "name" = EXCLUDED."name", "version" = EXCLUDED."version"
        |WHERE person.version = EXCLUDED.version - 1
        """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setObject(1, person.birthDate)
        it.setObject(2, person.id)
        it.setString(3, person.name)
        it.setInt(4, person.version)
        val rows = it.executeUpdate()
        if (rows != 1) {
            throw OptimisticLockFailException()
        }
    }
}
```

</details>

#### Method without parameters

Any method in a repository is considered as a `SELECT` (except those who's name is `save` or `delete`)

```kotlin
fun selectAll(): List<Person>
```

<details>

<summary>Generated code</summary>

```kotlin
public override fun selectAll(): List<Person> {
    val query = """
     |SELECT "birth_date", "id", "name"
     |FROM person
     """.trimMargin()
    return connection.prepareStatement(query).use {
        it.executeQuery().use {
            val acc = mutableListOf<Person>()
            while (it.next()) {
                acc +=
                    Person(
                        birthDate = LocalDate.parse(it.getString("birth_date")),
                        id = UUID.fromString(it.getString("id")),
                        name = it.getString("name"),
                    )
            }
            acc
        }
    }
}
```

</details>

Method name does not make any sense.
For both methods will be generated the same code

```kotlin
fun selectAll(): List<Person>
fun fooBar(): List<Person>
``` 

#### Method with parameters

All method parameters should match entity field names. They are going to be used in a `WHERE` clause
in equality comparisons combined using `AND` logic operator.

```kotlin
fun selectBy(name:String, birthDate: LocalDate): Person
```

<details>
<summary>Generated code</summary>

```kotlin
public override fun selectBy(name: String, birthDate: LocalDate): Person {
    val query = """
    |SELECT "birth_date", "id", "name"
    |FROM person
    |WHERE "name" = ? AND "birth_date" = ?
    |LIMIT 2
    """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setString(1, name)
        it.setObject(2, birthDate)
        it.executeQuery().use {
            if (it.next()) {
                val result =
                    Person(
                        birthDate = java.time.LocalDate.parse(it.getString("birth_date")),
                        id = UUID.fromString(it.getString("id")),
                        name = it.getString("name"),
                    )
                if (it.next()) {
                    throw IllegalStateException("Query has returned more than one element")
                }
                result
            } else {
                throw NoSuchElementException()
            }
        }
    }
}
```

</details>

#### Complex conditionals

If it is not enough to use only `=` combined using `AND`, the whole `WHERE` clause could be placed
in `kotlite.annotations.Where` annotation.
In this case parameters could have names different from entity fields, but they should match placeholders in a query.

```kotlin
@Where("name = :name OR birth_date < :birthDate")
fun selectBy(name: String, birthDate: LocalDate): Person?
```

Its content will be moved to the query almost as is.

<details>

<summary>Generated code</summary>

```kotlin
public override fun selectBy(name: String, birthDate: LocalDate): Person? {
    val query = """
        |SELECT "birth_date", "id", "name"
        |FROM person
        |WHERE name = ? OR birth_date < ?
        |LIMIT 2
        """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setString(1, name)
        it.setObject(2, birthDate)
        it.executeQuery().use {
            if (it.next()) {
                val result =
                    Person(
                        birthDate = java.time.LocalDate.parse(it.getString("birth_date")),
                        id = UUID.fromString(it.getString("id")),
                        name = it.getString("name"),
                    )
                if (it.next()) {
                    throw IllegalStateException("Query has returned more than one element")
                }
                result
            } else {
                null
            }
        }
    }
}
```

</details>

#### @Limit

If method return type is list, it can be annotated with `@Limit`:

```kotlin
@Limit(10)
fun findByName(name: String): List<Person>
```

Also, limit could be a dynamic value:

```kotlin
fun findBy(name: String, @Limit limit: Int): List<Person>
```

<details>

<summary>Generated code</summary>

```kotlin
public override fun findBy(name: String, limit: Int): List<Person> {
    val query = """
    |SELECT "birth_date", "id", "name"
    |FROM person
    |WHERE "name" = ?
    |LIMIT ?
    """.trimMargin()
    return connection.prepareStatement(query).use {
        it.setString(1, name)
        it.setInt(2, limit)
        it.executeQuery().use {
            val acc = mutableListOf<Person>()
            while (it.next()) {
                acc +=
                    Person(
                        birthDate = LocalDate.parse(it.getString("birth_date")),
                        id = UUID.fromString(it.getString("id")),
                        name = it.getString("name"),
                    )
            }
            acc
        }
    }
}
```

</details>

#### Projections

Besides, entities query methods can return projections. For example for entity

```kotlin
data class Person(val id: UUID, val name: String, val birthDate: LocalDate)
```

projection could be any data class having some of Entity's fields:

```kotlin
data class NameAndDate(val name: String, val birthDate: LocalDate)
data class NameAndId(val name: String, val id: UUID)
```

Generated code will query only those required fields

```kotlin
fun selectAll(): List<NameAndId>
fun selectBy(id: UUID): NameAndDate?
```

#### Custom @Query methods

User can define any custom query, which is mapped to any data class. In this case column names in result set should
match
projection class field names (up to camelCase to snake_case conversion)

```kotlin
@Query("""
    SELECT p.first_name, p.last_name, d.age
    FROM person AS p
    JOIN documents AS d ON p.id = d.person_id
    WHERE p.first_name like :namePattern
""")
fun select(namePattern: String): PersonProjection 
```

Also, custom query methods can have scalar ("primitive") or list of scalars as a return type:

```kotlin
@Query("SELECT birth_date FROM person WHERE id = :id")
fun selectBirthDate(id: UUID): LocalDate?
@Query("SELECT birth_date FROM person")
fun selectAllBirthDates(): List<LocalDate>
@Query("SELECT count(*) FROM person")
fun selectPersonNumber(): Int
```

`@Query` annotation cannot be combined with none of: `@Where`, `@Limit`, `@First`.
It should contain the whole query

#### Ordering

TODO

## Database object and transaction DSL

Database object gives access to transactions DSL and contains all the generated repositories.

```kotlin
val db = DB(dataSource)

val johns = db.transaction {
    // the only way to obtain a repository is to start a transaction
    personRepository.selectAllWhere(lastName = "John")
}
```

It's fully qualified name is configured in `build.gradle.kts`:

```kotlin
kapt {
  arguments {
    arg("kotlite.db.qualifiedName", "my.pack.DB") 
  }
}
```

By default, all repositories are assigned to this database object, unless other is specified in
`@SqliteRepository` annotation:

```kotlin
@SqliteRepository(belongsToDb = "my.another.DbObject")
interface MyRepository : Repository<MyEntity>
```

## Transactions

Any repository interactions are done inside a transaction.
This does not introduce any overhead, since even if you do not declare transaction explicitly, it is started implicitly.

### Transaction DSL

Inside transaction lambda all DB's repositories are available through `this`:

```kotlin
val people = db.transaction {
    this.personRepository.findAll()
}
``` 

Of cource `this` can be skipped:

```kotlin
val people = db.transaction {
    personRepository.findAll()
}
``` 

If lambda completed successfully - transaction is committed.
Any exception thrown from the lambda rolls back the transaction.
Also, transaction can be rolled back manually:

```kotlin
db.transaction {
    personRepository.saveAll(people)
    if (somethingGoneWrong) rollback()
}
``` 

It is possible to rollback to certain save point:

```kotlin
db.transaction {
    personRepository.saveAll(people)
    val savePoint = savePoint()
      ...
    if (somethingGoneWrong) rollbackTo(savePoint)
}
``` 

If transaction is read only, it could be specified:

```kotlin
val people = db.transaction(readOnly = true) {
    personRepository.findAll()
}
```

Default isolation level (READ_COMMITTED) can be changed per transaction:

```kotlin
db.transaction(isolationLevel = IsolationLevel.SERIALIZABLE) {
    ...
}
```

### Spring support

DB objects could be marked as Spring components `build.gradle.kts`:

```kotlin
kapt {
  arguments {
    arg("kotlite.spring", "true")
  }
}
```

generated class:

```kotlin
import org.springframework.stereotype.Component

@Generated
@Component
public class DB(
  private val ds: DataSource
) {
...
```

So it could be instantiated and further injected by Spring.

## Database verification

(doesn't work with Sqlite yet)

```kotlin
DB(dataSource).check()
```

Checks all underlying repositories and returns list of errors or empty list if everything is ok.

Checks for absent/extra fields, type/nullability mismatch, key fields/primary keys.

## Type mappings

| Kotlin type               |  Sqlite type                  |
| ------------------------- | ----------------------------- |
| java.math.BigDecimal      |  numeric                      |
| kotlin.Boolean            |  boolean                      |
| kotlin.ByteArray          |  bytea                        |
| java.sql.Date             |  date                         |
| kotlin.Double             |  double precision             |
| kotlin.Float              |  real                         |
| kotlin.Int                |  integer                      |
| kotlin.collections.List   |  jsonb                        |
| kotlin.Long               |  bigint                       |
| java.time.LocalDate       |  date                         |
| java.time.LocalDateTime   |  timestamp without time zone  |
| java.time.LocalTime       |  time without time zone       |
| kotlin.collections.Map    |  jsonb                        |
| kotlin.String             |  text                         |
| java.sql.Time             |  time without time zone       |
| java.sql.Timestamp        |  timestamp with time zone     |
| java.util.UUID            |  uuid                         |
