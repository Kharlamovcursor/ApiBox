package ru.hdlabs.apibox.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "desk_items",
    indices = [Index(value = ["kind", "ref"], unique = true)],
)
data class DeskItemEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val ref: String,
    val sortOrder: Int,
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val deskItemId: String,
    val valueText: String,
    val deltaText: String,
    val up: Boolean?,
    val footer: String,
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val name: String,
    val sortOrder: Int,
    val lastTestOk: Boolean?,
    val lastTestMessage: String,
    val lastTestAt: Long?,
)

@Entity(tableName = "custom_apis")
data class CustomApiEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val method: String,
    val jsonPath: String,
    val sortOrder: Int,
    val onDesk: Boolean,
)

@Dao
interface DeskDao {
    @Query("SELECT * FROM desk_items ORDER BY sortOrder, ref")
    fun observe(): Flow<List<DeskItemEntity>>

    @Query("SELECT * FROM desk_items ORDER BY sortOrder, ref")
    suspend fun snapshot(): List<DeskItemEntity>

    @Query("SELECT COUNT(*) FROM desk_items")
    suspend fun count(): Int

    @Query("SELECT * FROM desk_items WHERE kind = :kind AND ref = :ref LIMIT 1")
    suspend fun find(kind: String, ref: String): DeskItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: DeskItemEntity)

    @Query("DELETE FROM desk_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM desk_items WHERE kind = :kind AND ref = :ref")
    suspend fun deleteRef(kind: String, ref: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM desk_items")
    suspend fun maxOrder(): Int

    @Update
    suspend fun updateAll(items: List<DeskItemEntity>)
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes")
    fun observe(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE deskItemId = :id")
    suspend fun get(id: String): QuoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: QuoteEntity)

    @Query("DELETE FROM quotes WHERE deskItemId = :id")
    suspend fun delete(id: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, name")
    fun observe(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM accounts")
    suspend fun snapshot(): List<AccountEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM accounts")
    suspend fun maxOrder(): Int

    @Update
    suspend fun updateAll(items: List<AccountEntity>)
}

@Dao
interface CustomApiDao {
    @Query("SELECT * FROM custom_apis ORDER BY sortOrder, name")
    fun observe(): Flow<List<CustomApiEntity>>

    @Query("SELECT * FROM custom_apis WHERE id = :id")
    suspend fun get(id: String): CustomApiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CustomApiEntity)

    @Query("DELETE FROM custom_apis WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM custom_apis")
    suspend fun snapshot(): List<CustomApiEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM custom_apis")
    suspend fun maxOrder(): Int

    @Update
    suspend fun updateAll(items: List<CustomApiEntity>)
}
