package kotlite.aux

import java.sql.Connection
import java.sql.Savepoint

interface DbOperations {
    /**
     * Usage of a connection should be avoided. For extreme needs only.
     */
    val connection: Connection
    fun execute(sql: String)
    fun commit()
    fun rollback()
    fun rollbackTo(savepoint: Savepoint)
    fun savePoint(): Savepoint
}

class DbOperationsImpl(
    override val connection: Connection,
) : DbOperations {

    override fun execute(sql: String) {
        connection.createStatement().use {
            it.execute(sql)
        }
    }

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