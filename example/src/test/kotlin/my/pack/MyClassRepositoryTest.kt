package my.pack

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class MyClassRepositoryTest {

    companion object {

        @JvmStatic
        @BeforeAll
        fun createTable() {
            TestUtil.runMigrations()
        }

        val item = MyClass(
            id = "13",
            name = "iphone13",
            myNestedClass = MyNestedClass(
                proc = "bionic13",
                myNestedNestedClass = MyNestedNestedClass(
                    capacity = "13wh",
                    longivity = "13h"
                )
            ),
            version = 13,
            bool = true,
            date = Date.valueOf(LocalDate.parse("2010-01-01")),
            timestamp = Timestamp.from(Instant.parse("2010-01-01T00:00:00.000Z")),
            uuid = UUID.fromString("66832deb-1864-42b1-b057-e65c28d39a4e"),
            time = LocalTime.parse("00:00:00"),
            localDate = LocalDate.parse("2010-01-01"),
            localDateTime = LocalDateTime.parse("2010-01-01T00:00:00"),
            list = listOf("a", "b", "c"),
            enum = Mode.OFF,
            nullableInt = null
        )
    }

    val db = DB(TestUtil.ds)

    @AfterEach
    fun cleanup() {
        db.transaction { myClassRepository.deleteAll() }
    }

    @Test
    fun check() {
        assert(db.check().isEmpty())
    }

    @Test
    fun rollback() {

        db.transaction {
            myClassRepository.save(item)
            rollback()
        }

        val phones = db.transaction(readOnly = true) { myClassRepository.findAll() }
        assert(phones.isEmpty()) { "rollback does not work" }
    }

    @Test
    fun `rollback on exception`() {
        try {
            db.transaction {
                myClassRepository.save(item)
                error("")
            }
        } catch (ex: IllegalStateException) {
        }

        val phones = db.transaction(readOnly = true) { myClassRepository.findAll() }
        assert(phones.isEmpty())
    }

    @Test
    fun save() {

        db.transaction {
            myClassRepository.save(item)
        }

        val phones2 = db.transaction(readOnly = true) { myClassRepository.findAll() }

        assert(phones2 == listOf(item))
    }

    @Test
    fun saveAll() {

        val phones = listOf(item, item.copy(id = "14"))

        db.transaction {
            myClassRepository.saveAll(phones)

        }

        val phones2 = db.transaction(readOnly = true) { myClassRepository.findAll() }

        assert(phones2 == phones)
    }

    @Test
    fun update() {

        db.transaction { myClassRepository.save(item) }
        db.transaction { myClassRepository.save(item.copy(name = "iphone2")) }

        val phones = db.transaction(readOnly = true) { myClassRepository.findAll() }

        assert(phones == listOf(item.copy(name = "iphone2")))
    }

    @Test
    fun `query method returns an entity`() {
        db.transaction { myClassRepository.save(item) }

        val found = db.transaction(readOnly = true) { myClassRepository.findById(item.id) }

        assert(found == item)
    }

    @Test()
    fun `single result query method throws if there are more than one result`() {
        db.transaction { myClassRepository.saveAll(listOf(item, item.copy(id = "14"))) }

        expect<IllegalStateException> {
            db.transaction(readOnly = true) { myClassRepository.findSingleBySpecProc("bionic13") }
        }
    }

    @Test
    fun `nullable query method returns null if there is no result`() {

        val found = db.transaction(readOnly = true) { myClassRepository.findById(item.id) }

        assert(found == null)
    }

    @Test
    fun `not null method throws if there is no result`() {
        expect<NoSuchElementException> {
            db.transaction(readOnly = true) { myClassRepository.findSingleById(item.id) }
        }
    }

    @Test
    fun `multiple parameters combined with AND`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by id and version`(id: String, version: Int) =
            db.transaction(readOnly = true) { myClassRepository.findByIdAndVersion(id, version) }

        all(
            { assert(`find by id and version`("13", 13) == item) },
            { assert(`find by id and version`("13", 14) == null) },
        )
    }

    @Test
    fun `@Where annotation works`() {
        db.transaction { myClassRepository.save(item) }

        fun `test @Where`(
            capacity: String,
            v: Int,
            date: String
        ) = db.transaction(readOnly = true) {
            myClassRepository.findByCapacityAndVersion(
                capacity = capacity,
                v = v,
                date = Date.valueOf(LocalDate.parse(date))
            )
        }

        all(
            { assert(`test @Where`("13wh", 13, "2010-01-01") == listOf(item)) },
            { assert(`test @Where`("13wh", 13, "2010-01-02") == listOf(item)) },
            { assert(`test @Where`("13wh", 12, "2010-01-02") == listOf(item)) },
            { assert(`test @Where`("12wh", 12, "2010-01-02") == emptyList<MyClass>()) },
            { assert(`test @Where`("13wh", 12, "2009-01-01") == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `search by timestamp`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by timestamp`(ts: String) =
            db.transaction { this.myClassRepository.findByTimestamp(Timestamp.from(Instant.parse(ts))) }

        all(
            { assert(`find by timestamp`("2010-01-01T00:00:00.000Z") == listOf(item)) },
            { assert(`find by timestamp`("2010-01-01T00:00:00.001Z") == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `search by uuid`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by uuid`(uuid: String) =
            db.transaction { this.myClassRepository.findByUUID(UUID.fromString(uuid)) }

        all(
            { assert(`find by uuid`("66832deb-1864-42b1-b057-e65c28d39a4e") == item) },
            { assert(`find by uuid`("00000000-0000-0000-0000-000000000001") == null) },
        )
    }

    @Test
    fun `search by time`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by time`(time: String) =
            db.transaction { this.myClassRepository.findByTime(LocalTime.parse(time)) }

        all(
            { assert(`find by time`("00:00:00") == listOf(item)) },
            { assert(`find by time`("00:00:01") == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `search by local date`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by local date`(time: String) =
            db.transaction { this.myClassRepository.findByLocalDate(LocalDate.parse(time)) }

        all(
            { assert(`find by local date`("2010-01-01") == listOf(item)) },
            { assert(`find by local date`("2010-01-02") == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `search by local date time`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by local date time`(time: String) =
            db.transaction { this.myClassRepository.findByLocalDateTime(LocalDateTime.parse(time)) }

        all(
            { assert(`find by local date time`("2010-01-01T00:00:00") == listOf(item)) },
            { assert(`find by local date time`("2010-01-02T00:00:00") == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `search by enum`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by enum`(mode: Mode) =
            db.transaction { this.myClassRepository.findByMode(mode) }

        all(
            { assert(`find by enum`(Mode.OFF) == listOf(item)) },
            { assert(`find by enum`(Mode.ON) == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `select projection`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by proc`(proc: String) =
            db.transaction { this.myClassRepository.selectProjection(proc) }

        all(
            { assert(`find by proc`("bionic13") == ProjectionOfMyClass(item.id, item.date, listOf("a", "b", "c"))) },
            { assert(`find by proc`("bionic14") == null) },
        )
    }

    @Test
    fun `select projection in custom query`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by proc`(proc: String) =
            db.transaction { this.myClassRepository.selectProjectionCustomQuery(proc) }

        all(
            { assert(`find by proc`("bionic13") == ProjectionOfMyClass(item.id, item.date, listOf("a", "b", "c"))) },
            { assert(`find by proc`("bionic14") == null) },
        )
    }

    @Test
    fun `select projection in custom where`() {
        db.transaction { myClassRepository.save(item) }

        fun `find by proc`(proc: String) =
            db.transaction { this.myClassRepository.selectProjectionWhere(proc) }

        all(
            { assert(`find by proc`("bionic13") == ProjectionOfMyClass(item.id, item.date, item.list)) },
            { assert(`find by proc`("bionic14") == null) },
        )
    }

    @Test
    fun `select scalar`() {
        db.transaction { myClassRepository.save(item) }

        fun `select date by id`(id: String) =
            db.transaction { myClassRepository.selectDate(id) }

        all(
            { assert(`select date by id`("13") == item.date) },
            { assert(`select date by id`("14") == null) },
        )
    }

    @Test
    fun `select scalars`() {
        db.transaction {
            myClassRepository.save(item)
            myClassRepository.save(item.copy(id = "14"))
        }

        fun `select date by proc`(proc: String) =
            db.transaction { myClassRepository.selectDates(proc) }

        all(
            { assert(`select date by proc`("bionic13") == listOf(item.date, item.date)) },
            { assert(`select date by proc`("bionic14") == emptyList<Date>()) },
        )
    }

    @Test
    fun `custom update`() {
        //GIVEN
        db.transaction { myClassRepository.save(item) }

        //WHEN
        db.transaction { myClassRepository.update(item.id, Date.valueOf("2020-12-31")) }

        //THEN
        val date = db.transaction { myClassRepository.selectDate(item.id) }

        assert(date == Date.valueOf("2020-12-31"))
    }

    @Test
    fun `select IN`() {

        val phones = listOf(item, item.copy(id = "14"))
        db.transaction {
            myClassRepository.saveAll(phones)
        }

        fun `id in`(ids: List<String>) =
            db.transaction { myClassRepository.findByIdIn(ids) }

        all(
            { assert(`id in`(listOf("13", "14")) == phones) },
            { assert(`id in`(listOf("15")) == emptyList<MyClass>()) },
            { assert(`id in`(emptyList()) == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `select IN with @Where`() {
        val phones = listOf(item, item.copy(id = "14"))
        db.transaction {
            myClassRepository.saveAll(phones)
        }

        fun `id in`(ids: List<String>) =
            db.transaction { myClassRepository.selectProjectionWhere(ids) }

        all(
            {
                assert(
                    `id in`(listOf("13", "14")) == listOf(
                        ProjectionOfMyClass(
                            id = item.id,
                            date = item.date,
                            list = item.list
                        ), ProjectionOfMyClass(id = "14", date = item.date, list = item.list)
                    )
                )
            },
            { assert(`id in`(listOf("15")) == emptyList<MyClass>()) },
            { assert(`id in`(emptyList()) == emptyList<MyClass>()) },
        )
    }

    @Test
    fun `select IN with custom query`() {
        val phones = listOf(item)
        db.transaction {
            myClassRepository.saveAll(phones)
        }

        fun `dates in`(dates: List<String>) =
            db.transaction { myClassRepository.customSelectWhereDatesIn(dates.map { Date.valueOf(it) }) }

        all(
            {
                assert(
                    `dates in`(listOf("2010-01-01", "2010-01-02")) == listOf(
                        ProjectionOfMyClass(id = item.id, date = item.date, list = item.list)
                    )
                )
            },
        )
    }

    @Test
    fun `save-read null value`() {
        val noNamePhone = item.copy(name = null)

        db.transaction { myClassRepository.save(noNamePhone) }
        val fromDb = db.transaction { myClassRepository.findById(noNamePhone.id) }

        assert(fromDb == noNamePhone)
    }

    @Test
    fun `where name is null`() {
        val noNamePhone = item.copy(name = null)

        db.transaction { myClassRepository.save(noNamePhone) }
        val fromDb = db.transaction { myClassRepository.findFirstByName(null) }

        assert(fromDb == noNamePhone)
    }

    @Test
    fun `find first does not fail on multiple results`() {
        val noNamePhone1 = item.copy(name = null)
        val noNamePhone2 = item.copy(id = "14", name = null)

        db.transaction { myClassRepository.saveAll(listOf(noNamePhone1, noNamePhone2)) }
        val fromDb = db.transaction { myClassRepository.findFirstByName(null) }

        assert(fromDb != null)
    }

    @Test
    fun `delete by date`() {

        db.transaction { myClassRepository.save(item) }
        db.transaction { myClassRepository.deleteByDate(item.date) }
        val fromDb = db.transaction { myClassRepository.findAll() }

        assert(fromDb.isEmpty())
    }

    @Test
    fun `limit by 3 elemets`() {
        val fourPhones = listOf(
            item,
            item.copy(id = "14"),
            item.copy(id = "15"),
            item.copy(id = "16"),
        )

        db.transaction { myClassRepository.saveAll(fourPhones) }

        val limited = db.transaction { myClassRepository.findByDate(item.date) }
        assert(limited.size == 3)
    }

    @Test
    fun `select list of enums`() {
        val items = listOf(
            item,
            item.copy(id = "14", enum = Mode.ON),
        )

        db.transaction { myClassRepository.saveAll(items) }

        val enums = db.transaction { myClassRepository.findAllEnums() }
        assert(enums.toSet() == setOf(Mode.ON, Mode.OFF))
    }
}
