package my.pack

import org.junit.jupiter.api.Test

class AnotherItemTest {
    @Test
    fun t(){
        val db = DB(TestUtil.ds)

        TestUtil.ds.connection.use {
            it.autoCommit = false


            db.transaction {
                anotherItemRepository.createTable()
            }

            it.createStatement().execute("create table t (i, j)")

            it.commit()

            db.transaction {
                anotherItemRepository.saveAll(listOf(AnotherItem(1, ""), AnotherItem(2, "")))
            }
            db.transaction {
                anotherItemRepository.findAll()
            }
        }
    }
}