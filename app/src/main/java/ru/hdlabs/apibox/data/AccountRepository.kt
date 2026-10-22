package ru.hdlabs.apibox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.hdlabs.apibox.data.local.AccountDao
import ru.hdlabs.apibox.data.local.AccountEntity
import ru.hdlabs.apibox.domain.PLATFORM_OZON
import ru.hdlabs.apibox.domain.PLATFORM_WB
import ru.hdlabs.apibox.domain.PLATFORM_YANDEX
import java.util.UUID

data class AccountDraft(
    val id: String?,
    val platform: String,
    val name: String,
    val clientId: String,
    val apiKey: String,
    val token: String,
)

data class AccountSecrets(
    val clientId: String,
    val apiKey: String,
    val token: String,
)

data class CheckedAccount(
    val id: String,
    val message: String,
)

class AccountRepository(
    private val accountDao: AccountDao,
    private val secrets: SecretStore,
    private val net: NetClient,
) {
    fun observe(): Flow<List<AccountEntity>> = accountDao.observe()

    suspend fun get(id: String): AccountEntity? = accountDao.get(id)

    fun secretsOf(id: String): AccountSecrets = AccountSecrets(
        clientId = secrets.get(SecretKeys.client(id)),
        apiKey = secrets.get(SecretKeys.key(id)),
        token = secrets.get(SecretKeys.token(id)),
    )

    suspend fun save(draft: AccountDraft): String {
        val existing = draft.id?.let { accountDao.get(it) }
        val id = existing?.id ?: UUID.randomUUID().toString()
        accountDao.upsert(
            AccountEntity(
                id = id,
                platform = draft.platform,
                name = draft.name.trim(),
                sortOrder = existing?.sortOrder ?: (accountDao.maxOrder() + 1),
                lastTestOk = existing?.lastTestOk,
                lastTestMessage = existing?.lastTestMessage ?: "",
                lastTestAt = existing?.lastTestAt,
            ),
        )
        when (draft.platform) {
            PLATFORM_OZON -> {
                secrets.put(SecretKeys.client(id), draft.clientId.trim())
                secrets.put(SecretKeys.key(id), draft.apiKey.trim())
            }
            PLATFORM_WB -> secrets.put(SecretKeys.token(id), draft.token.trim())
            PLATFORM_YANDEX -> secrets.put(SecretKeys.key(id), draft.apiKey.trim())
        }
        return id
    }

    suspend fun test(draft: AccountDraft): CheckedAccount = withContext(Dispatchers.IO) {
        val id = save(draft)
        val result = when (draft.platform) {
            PLATFORM_OZON -> net.request(
                url = "https://api-seller.ozon.ru/v1/roles",
                method = "POST",
                headers = mapOf(
                    "Client-Id" to draft.clientId.trim(),
                    "Api-Key" to draft.apiKey.trim(),
                    "Content-Type" to "application/json",
                ),
                body = "{}",
            )
            PLATFORM_WB -> net.request(
                url = "https://common-api.wildberries.ru/ping",
                method = "GET",
                headers = mapOf("Authorization" to draft.token.trim()),
                body = null,
            )
            PLATFORM_YANDEX -> net.request(
                url = "https://api.partner.market.yandex.ru/v2/campaigns",
                method = "GET",
                headers = mapOf("Api-Key" to draft.apiKey.trim()),
                body = null,
            )
            else -> HttpResult(0, "", "Неизвестная площадка")
        }
        val text = connectionText(result)
        val stored = accountDao.get(id)
        if (stored != null) {
            accountDao.upsert(
                stored.copy(
                    lastTestOk = result.code in 200..299,
                    lastTestMessage = text,
                    lastTestAt = System.currentTimeMillis(),
                ),
            )
        }
        CheckedAccount(id, text)
    }

    suspend fun delete(id: String) {
        accountDao.delete(id)
        secrets.remove(SecretKeys.client(id))
        secrets.remove(SecretKeys.key(id))
        secrets.remove(SecretKeys.token(id))
    }

    suspend fun persistOrder(ids: List<String>) {
        val byId = accountDao.snapshot().associateBy { it.id }
        val updated = ids.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }
        if (updated.isNotEmpty()) accountDao.updateAll(updated)
    }
}
