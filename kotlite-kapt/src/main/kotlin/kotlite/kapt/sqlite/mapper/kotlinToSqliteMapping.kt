package kotlite.kapt.mapper

import kotlite.aux.SqliteType
import kotlite.kapt.parser.KotlinType

val kotlinTypeToSqliteTypeMapping = mapOf(
    KotlinType.BIG_DECIMAL to SqliteType.NUMERIC,
    KotlinType.BOOLEAN to SqliteType.BOOLEAN,
    KotlinType.BYTE_ARRAY to SqliteType.BYTEA,
    KotlinType.DATE to SqliteType.DATE,
    KotlinType.DOUBLE to SqliteType.DOUBLE,
    KotlinType.FLOAT to SqliteType.REAL,
    KotlinType.INT to SqliteType.INTEGER,
    KotlinType.LIST to SqliteType.JSONB,
    KotlinType.LONG to SqliteType.BIGINT,
    KotlinType.LOCAL_DATE to SqliteType.DATE,
    KotlinType.LOCAL_DATE_TIME to SqliteType.TIMESTAMP,
    KotlinType.LOCAL_TIME to SqliteType.TIME,
    KotlinType.MAP to SqliteType.JSONB,
    KotlinType.STRING to SqliteType.TEXT,
    KotlinType.TIME to SqliteType.TIME,
    KotlinType.TIMESTAMP to SqliteType.TIMESTAMP_WITH_TIMEZONE,
    KotlinType.UUID to SqliteType.UUID,
)
