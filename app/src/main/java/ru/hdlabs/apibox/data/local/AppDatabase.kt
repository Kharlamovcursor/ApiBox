package ru.hdlabs.apibox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeskItemEntity::class,
        QuoteEntity::class,
        AccountEntity::class,
        CustomApiEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deskDao(): DeskDao
    abstract fun quoteDao(): QuoteDao
    abstract fun accountDao(): AccountDao
    abstract fun customApiDao(): CustomApiDao
}
