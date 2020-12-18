package my.pack

import kotlite.annotations.SqliteRepository
import kotlite.aux.Repository

data class Blob(
    val byteArray: ByteArray,
)

@SqliteRepository(belongsToDb = "my.pack.BlobDb")
interface BlobRepository : Repository<Blob> {
    fun findAll(): List<Blob>
    fun deleteAll()
    fun saveAll(items: List<Blob>)
    fun save(item: Blob)
}
