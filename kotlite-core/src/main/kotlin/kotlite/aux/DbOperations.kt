package kotlite.aux

import java.sql.Connection
import java.sql.Savepoint
import java.util.concurrent.CompletableFuture

interface DbOperations {
    val connection: Connection
    fun commit()
    fun rollback()
    fun rollbackTo(savepoint: Savepoint)
    fun savePoint(): Savepoint
}

class DbOperationsImpl(
    override val connection: Connection,
) : DbOperations {

    override fun commit() {
        connection.commit()
    }

    override fun rollback() {
        connection.rollback()
    }

    override fun rollbackTo(savepoint: Savepoint) {
        connection.rollback(savepoint)
    }

    override fun savePoint(): Savepoint {
        return connection.setSavepoint()
    }
}