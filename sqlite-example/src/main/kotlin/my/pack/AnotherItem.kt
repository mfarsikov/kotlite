package my.pack

import kotlite.annotations.Id
import kotlite.annotations.SqliteRepository
import kotlite.annotations.Statement
import kotlite.aux.Repository

data class AnotherItem(
    @Id

    val id: Int,
    val name: String
)

@SqliteRepository
interface AnotherItemRepository: Repository<AnotherItem>{
    fun saveAll(items: List<AnotherItem>)
    @Statement("""
        create table another_item (id integer primary key asc, name text not null)
    """)
    fun createTable()
    fun findAll():List<AnotherItem>
}