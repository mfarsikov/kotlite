package my.pack

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlite.annotations.Column
import kotlite.annotations.First
import kotlite.annotations.Id
import kotlite.annotations.Limit
import kotlite.annotations.OnConflictFail
import kotlite.annotations.OrderBy
import kotlite.annotations.Query
import kotlite.annotations.SqliteRepository
import kotlite.annotations.Statement
import kotlite.annotations.Where
import kotlite.aux.Repository
import kotlite.aux.SqliteType
import kotlite.aux.page.Page
import kotlite.aux.page.Pageable
import kotlite.aux.sort.Order

data class MyClass(
    @Id
    val id: String,
    val name: String?,
    val myNestedClass: MyNestedClass,
    val version: Int,
    val bool: Boolean,
    val date: Date,
    val timestamp: Timestamp,
    val uuid: UUID,
    val time: LocalTime,
    val localDate: LocalDate,
    val localDateTime: LocalDateTime,
    val list: List<String>,
    val enum: Mode,
    val nullableInt: Int?,
)

enum class Mode { ON, OFF }

data class MyNestedClass(
    val proc: String,
    val myNestedNestedClass: MyNestedNestedClass,
)

data class MyNestedNestedClass(
    @Column(name = "cap_city")
    val capacity: String,
    @Column(type = SqliteType.TEXT)
    val longivity: String,
)

data class ProjectionOfMyClass(val id: String, val date: Date, val list: List<String>)

@SqliteRepository
interface MyClassRepository : Repository<MyClass> {

    fun findAll(): List<MyClass>
    fun deleteAll()
    fun delete(item: MyClass)
    fun saveAll(items: List<MyClass>)
    fun save(item: MyClass)
    fun count(): Int
    fun exists(id: String): Boolean

    fun findAll(order: Order): List<MyClass>

    @OnConflictFail
    fun saveOrFail(item: MyClass)

    fun findById(id: String): MyClass?

    @First
    fun findFirstByName(name: String?): MyClass?

    @Limit(3)
    fun findByDate(date: Date): List<MyClass>

    fun findByDate(date: Date, @Limit limit: Int): List<MyClass>

    fun findSingleById(id: String): MyClass
    fun findBySpecProc(proc: String): List<MyClass>
    fun findSingleBySpecProc(proc: String): MyClass
    fun findByIdAndVersion(id: String, version: Int): MyClass?
    fun findByTimestamp(timestamp: Timestamp): List<MyClass>
    fun findByUUID(uuid: UUID): MyClass?
    fun findByTime(time: LocalTime): List<MyClass>
    fun findByLocalDate(localDate: LocalDate): List<MyClass>
    fun findByLocalDateTime(localDateTime: LocalDateTime): List<MyClass>
    fun findByMode(enum: Mode): List<MyClass>

    fun findByIdIn(id: List<String>): List<MyClass>

    fun delete(id: String, date: Date)
    fun deleteByDate(date: Date)

    @Where("cap_city = :capacity and date <= :date and :v <= version")
    fun findByCapacityAndVersion(capacity: String, v: Int, date: Date): List<MyClass>

    fun selectProjection(proc: String): ProjectionOfMyClass?

    @Query("select id, date, list from my_class where proc = :proc")
    fun selectProjectionCustomQuery(proc: String): ProjectionOfMyClass?

    @Where("proc = :proc")
    fun selectProjectionWhere(proc: String): ProjectionOfMyClass?

    fun selectProjectionWhere(id: List<String>): List<ProjectionOfMyClass>

    @Where("id in (:ids)")
    fun findProjectionWhere(ids: List<String>): List<ProjectionOfMyClass>

    @Query("select date from my_class where id = :id")
    fun selectDate(id: String): Date?

    @Query("select date from my_class where proc = :proc")
    fun selectDates(proc: String): List<Date>

    @Query("update my_class set date = :date where id = :id")
    fun update(id: String, date: Date)

    @Query("select id, date, list from my_class where date in (:date)")
    fun customSelectWhereDatesIn(date: List<Date>): List<ProjectionOfMyClass>

    @Query("select enum from my_class")
    fun findAllEnums(): List<Mode>

    fun findByNamePaged(name: String, pageable: Pageable): Page<MyClass>

    @Where("name = :name")
    fun findByNamePagedWhere(name: String, pageable: Pageable): Page<MyClass>

    @Query("select id, date, list from my_class where name = :name")
    fun findByNamePagedCustom(name: String, pageable: Pageable): Page<ProjectionOfMyClass>

    @OrderBy("name ASC NULLS FIRST, id DESC")
    fun findAllOrdered(): List<MyClass>

    @Statement("SELECT set_config('log_statement', 'all', true)")
    fun turnOnLogsOnServerForCurrentTransaction()
}
