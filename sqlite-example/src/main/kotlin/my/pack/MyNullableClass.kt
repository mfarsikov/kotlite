package my.pack

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlite.annotations.Id
import kotlite.annotations.Query
import kotlite.annotations.SqliteRepository
import kotlite.annotations.Where
import kotlite.aux.Repository

// @Table("") todo uncomment and fix error if there is no schema
data class MyNullableClass(
    @Id
    val id: String,
    val name: String?,
    val myNestedClass: MyNullableNestedClass,
    val version: Int?,
    val bool: Boolean?,
    val date: Date?,
    val timestamp: Timestamp?,
    val uuid: UUID?,
    val time: LocalTime?,
    val localDate: LocalDate?,
    val localDateTime: LocalDateTime?,
    val list: List<String>,
    val enum: Mode?,
)

data class MyNullableNestedClass(
    val proc: String?,
    val myNestedNestedClass: MyNullableNestedNestedClass,
)

data class MyNullableNestedNestedClass(
    val capacity: String?,
    val longivity: String?,
)

data class NullableProjectionOfMyClass(val id: String?, val date: Date?, val list: List<String>, val capacity: String?)

@SqliteRepository(belongsToDb = "my.pack.NullableDb")
interface MyNullableClassRepository : Repository<MyNullableClass> {

    fun saveAll(items: List<MyNullableClass>)
    fun save(item: MyNullableClass)
    fun deleteAll()
    fun findAll(): List<MyNullableClass>
    fun findById(id: String): MyNullableClass?
    fun findByName(name: String?): MyNullableClass?
    fun findByDate(date: Date): List<MyNullableClass>
    fun findByIdOrThrow(id: String): MyNullableClass
    fun findBySpecProc(proc: String): List<MyNullableClass>
    fun findSingleBySpecProc(proc: String?): MyNullableClass
    fun findByIdAndVersion(id: String, version: Int?): MyNullableClass?
    fun findByTimestamp(timestamp: Timestamp?): List<MyNullableClass>
    fun findByUUID(uuid: UUID?): MyNullableClass?
    fun findByTime(time: LocalTime?): List<MyNullableClass>
    fun findByLocalDate(localDate: LocalDate?): List<MyNullableClass>
    fun findByLocalDateTime(localDateTime: LocalDateTime?): List<MyNullableClass>
    fun findByMode(enum: Mode): List<MyNullableClass>

    fun findByIdIn(id: List<String>): List<MyNullableClass>

    fun delete(id: String, date: Date)
    fun deleteByDate(date: Date)

    @Where("capacity IS :capacity and date IS :date and :v IS version")
    fun findByCapacityAndVersion(capacity: String?, v: Int?, date: Date?): List<MyNullableClass>

    fun selectProjection(proc: String?): NullableProjectionOfMyClass?

    @Query("select id, capacity, date, list from my_nullable_class where proc IS :proc")
    fun selectProjectionCustomQuery(proc: String?): NullableProjectionOfMyClass?

    @Where("proc IS :proc")
    fun selectProjectionWhere(proc: String?): NullableProjectionOfMyClass?

    @Where("id in (:id)")
    fun selectProjectionWhere(id: List<String>): List<NullableProjectionOfMyClass>

    @Query("select date from my_nullable_class where id = :id")
    fun selectDate(id: String): Date?

    @Query("select date from my_nullable_class where proc IS :proc")
    fun selectDates(proc: String?): List<Date?>

    @Query("update my_nullable_class set date = :date where id = :id")
    fun update(id: String, date: Date?)

    @Query("select id, date, list, capacity from my_nullable_class where date in (:date)")
    fun customSelectWhereDatesIn(date: List<Date>): List<NullableProjectionOfMyClass>

    @Query("select enum from my_nullable_class where id = :id")
    fun selectEnumWhereId(id: String): Mode?

    fun selectEnumWhereEnum(enum: Mode?): List<MyNullableClass>

    @Query("select enum from my_nullable_class")
    fun findAllEnums(): List<Mode?>
}
