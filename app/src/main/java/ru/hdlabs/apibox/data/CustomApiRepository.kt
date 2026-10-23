package ru.hdlabs.apibox.data

import kotlinx.coroutines.flow.Flow
import ru.hdlabs.apibox.data.local.CustomApiDao
import ru.hdlabs.apibox.data.local.CustomApiEntity
import ru.hdlabs.apibox.domain.CustomApiPreset
import java.util.UUID

data class CustomDraft(
    val id: String?,
    val name: String,
    val url: String,
    val method: String,
    val jsonPath: String,
    val body: String,
    val headers: List<Pair<String, String>>,
    val onDesk: Boolean,
)

class CustomApiRepository(
    private val customDao: CustomApiDao,
    private val secrets: SecretStore,
    private val desk: DeskRepository,
) {
    fun observe(): Flow<List<CustomApiEntity>> = customDao.observe()

    suspend fun get(id: String): CustomApiEntity? = customDao.get(id)

    fun headers(id: String): List<Pair<String, String>> = decodeHeaders(secrets.get(SecretKeys.headers(id)))

    fun body(id: String): String = secrets.get(SecretKeys.body(id))

    suspend fun ensureFromPreset(preset: CustomApiPreset): String {
        if (customDao.get(preset.id) != null) return preset.id
        return save(
            CustomDraft(
                id = preset.id,
                name = preset.name,
                url = preset.url,
                method = "GET",
                jsonPath = preset.jsonPath,
                body = "",
                headers = emptyList(),
                onDesk = false,
            ),
        )
    }

    suspend fun setPresetOnDesk(preset: CustomApiPreset, enabled: Boolean) {
        val id = ensureFromPreset(preset)
        val existing = customDao.get(id) ?: return
        save(
            CustomDraft(
                id = id,
                name = existing.name,
                url = existing.url,
                method = existing.method,
                jsonPath = existing.jsonPath,
                body = body(id),
                headers = headers(id),
                onDesk = enabled,
            ),
        )
    }

    suspend fun save(draft: CustomDraft): String {
        val existing = draft.id?.let { customDao.get(it) }
        val id = existing?.id
            ?: draft.id?.trim()?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString()
        customDao.upsert(
            CustomApiEntity(
                id = id,
                name = draft.name.trim(),
                url = draft.url.trim(),
                method = if (draft.method.equals("POST", ignoreCase = true)) "POST" else "GET",
                jsonPath = draft.jsonPath.trim(),
                sortOrder = existing?.sortOrder ?: (customDao.maxOrder() + 1),
                onDesk = draft.onDesk,
            ),
        )
        secrets.put(SecretKeys.headers(id), encodeHeaders(draft.headers))
        secrets.put(SecretKeys.body(id), draft.body)
        desk.setCustomOnDesk(id, draft.onDesk)
        return id
    }

    suspend fun delete(id: String) {
        customDao.delete(id)
        secrets.remove(SecretKeys.headers(id))
        secrets.remove(SecretKeys.body(id))
        desk.removeCustom(id)
    }

    suspend fun persistOrder(ids: List<String>) {
        val byId = customDao.snapshot().associateBy { it.id }
        val updated = ids.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }
        if (updated.isNotEmpty()) customDao.updateAll(updated)
    }
}
