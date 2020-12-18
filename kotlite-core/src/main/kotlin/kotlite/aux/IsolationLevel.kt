package kotlite.aux

import java.sql.Connection

enum class IsolationLevel(
    val javaSqlValue:Int
) {
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE)
}