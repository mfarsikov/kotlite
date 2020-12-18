package my.pack

import kotlite.annotations.Id
import kotlite.annotations.SqliteRepository
import kotlite.annotations.Version
import kotlite.aux.Repository
import java.util.*

data class OptimisticallyLockedItem(
    @Id
    val id: UUID,

    @Version
    val version: Int = 0,
)

@SqliteRepository
interface OptimisticLockRepository : Repository<OptimisticallyLockedItem> {
    fun save(item: OptimisticallyLockedItem)
    fun saveAll(items: List<OptimisticallyLockedItem>)
    fun find(id: UUID): OptimisticallyLockedItem?
    fun deleteAll()
    fun delete(item: OptimisticallyLockedItem)
}