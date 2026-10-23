package ru.hdlabs.apibox.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.hdlabs.apibox.data.AccountDraft
import ru.hdlabs.apibox.data.AppSettings
import ru.hdlabs.apibox.data.CustomDraft
import ru.hdlabs.apibox.data.local.AccountEntity
import ru.hdlabs.apibox.domain.CustomApiPresets
import ru.hdlabs.apibox.domain.MoexCatalog
import ru.hdlabs.apibox.domain.PLATFORM_OZON
import ru.hdlabs.apibox.domain.PLATFORM_WB
import ru.hdlabs.apibox.domain.PLATFORM_YANDEX
import ru.hdlabs.apibox.domain.KIND_MOEX
import ru.hdlabs.apibox.domain.platformTitle
import ru.hdlabs.apibox.domain.swapped
import ru.hdlabs.apibox.ui.components.HdButton
import ru.hdlabs.apibox.ui.components.HdField
import ru.hdlabs.apibox.ui.components.HdTextButton
import ru.hdlabs.apibox.ui.components.HdTopBar
import ru.hdlabs.apibox.ui.components.ToolPage
import ru.hdlabs.apibox.ui.components.deskSurface
import ru.hdlabs.apibox.ui.components.formWidth
import ru.hdlabs.apibox.ui.components.rememberContainer
import ru.hdlabs.apibox.ui.reorder.ReorderDragHandle
import ru.hdlabs.apibox.ui.reorder.hdReorderItem
import ru.hdlabs.apibox.ui.reorder.rememberHdReorderState
import ru.hdlabs.apibox.ui.theme.HdBlue
import ru.hdlabs.apibox.ui.theme.HdDown
import ru.hdlabs.apibox.ui.theme.HdOrange
import ru.hdlabs.apibox.ui.theme.HdTextPrimary
import ru.hdlabs.apibox.ui.theme.HdTextSecondary
import ru.hdlabs.apibox.ui.theme.HdUp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsHomeScreen(
    onBack: () -> Unit,
    onMoex: () -> Unit,
    onAccounts: () -> Unit,
    onCustom: () -> Unit,
) {
    val container = rememberContainer()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    var speed by remember { mutableStateOf<Float?>(null) }
    ToolPage(title = "Настройки", onBack = onBack) {
        Text("Рабочий стол", color = HdTextSecondary, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Плитки", color = HdTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Выкл. — список с ручкой справа", color = HdTextSecondary, fontSize = 13.sp)
            }
            HdSwitch(checked = settings.tiles, onChecked = { scope.launch { container.settings.setTiles(it) } })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Анимация космоса", color = HdTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Останавливается, когда приложение свёрнуто", color = HdTextSecondary, fontSize = 13.sp)
            }
            HdSwitch(
                checked = settings.animationEnabled,
                onChecked = { scope.launch { container.settings.setAnimationEnabled(it) } },
            )
        }
        if (settings.animationEnabled) {
            Text("Скорость анимации ${speed?.toInt() ?: settings.animationSpeedPercent}%", color = HdTextSecondary)
            Slider(
                value = speed ?: settings.animationSpeedPercent.toFloat(),
                onValueChange = { speed = it },
                onValueChangeFinished = {
                    val chosen = speed ?: return@Slider
                    scope.launch { container.settings.setAnimationSpeed(chosen.toInt()) }
                    speed = null
                },
                valueRange = 40f..160f,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Экономия заряда", color = HdTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Меньше звёзд и вспышек", color = HdTextSecondary, fontSize = 13.sp)
            }
            HdSwitch(checked = settings.powerSave, onChecked = { scope.launch { container.settings.setPowerSave(it) } })
        }
        SectionLink("Индексы Мосбиржи", "IMOEX уже на столе, остальные включаются здесь", onMoex)
        SectionLink("Кабинеты маркетплейсов", "Ozon, Wildberries, Яндекс Маркет. Ключи только на телефоне", onAccounts)
        SectionLink("Свои API", "Курсы ЦБ и BTC, свои HTTPS и путь в JSON", onCustom)
        TelegramFeedbackSection()
        AppUpdateSection()
    }
}

@Composable
private fun SectionLink(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .deskSurface()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, color = HdTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(subtitle, color = HdTextSecondary, fontSize = 13.sp)
    }
}

@Composable
fun MoexScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val items by container.desk.observe().collectAsStateWithLifecycle(initialValue = emptyList())
    val enabled = items.filter { it.kind == KIND_MOEX }.map { it.ref }.toSet()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        HdTopBar(title = "Индексы", onBack = onBack)
        Text(
            "Порядок на рабочем столе меняется удержанием плитки.",
            color = HdTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(MoexCatalog.all, key = { _, index -> index.secId }) { _, index ->
                Row(
                    Modifier.fillMaxWidth().deskSurface().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(index.secId, color = HdBlue, fontWeight = FontWeight.Bold)
                        Text(index.title, color = HdTextPrimary)
                        Text(index.hint, color = HdTextSecondary, fontSize = 12.sp)
                    }
                    HdSwitch(
                        checked = index.secId in enabled,
                        onChecked = { scope.launch { container.desk.setMoexEnabled(index.secId, it) } },
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsScreen(onBack: () -> Unit, onCreate: (String) -> Unit, onOpen: (String) -> Unit) {
    val container = rememberContainer()
    val accounts by container.accounts.observe().collectAsStateWithLifecycle(initialValue = emptyList())
    var pick by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        HdTopBar(title = "Кабинеты", onBack = onBack)
        if (accounts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Кабинетов пока нет", color = HdTextSecondary)
                    HdButton(text = "Добавить кабинет", onClick = { pick = true }, modifier = Modifier.formWidth())
                }
            }
        } else {
            HdButton(
                text = "Добавить кабинет",
                onClick = { pick = true },
                modifier = Modifier.padding(horizontal = 16.dp).formWidth(),
            )
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.TopCenter) {
                DragList(
                    items = accounts,
                    keyOf = { it.id },
                    onOrder = { container.accounts.persistOrder(it) },
                ) { account ->
                    Column(Modifier.fillMaxSize().clickable { onOpen(account.id) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(account.name, color = HdTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(platformTitle(account.platform), color = HdBlue, fontSize = 13.sp)
                        Text(
                            testLine(account),
                            color = when (account.lastTestOk) {
                                true -> HdUp
                                false -> HdDown
                                null -> HdTextSecondary
                            },
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    if (pick) {
        AlertDialog(
            onDismissRequest = { pick = false },
            title = { Text("Площадка") },
            text = {
                Column {
                    listOf(PLATFORM_OZON, PLATFORM_WB, PLATFORM_YANDEX).forEach { platform ->
                        TextButton(onClick = {
                            pick = false
                            onCreate(platform)
                        }) { Text(platformTitle(platform), color = HdBlue) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pick = false }) { Text("Закрыть") } },
        )
    }
}

@Composable
fun AccountEditorScreen(existingId: String?, newPlatform: String, onBack: () -> Unit) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    var accountId by remember { mutableStateOf(existingId) }
    var platform by remember { mutableStateOf(newPlatform) }
    var name by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(existingId == null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(existingId) {
        if (existingId != null && !ready) {
            val loaded = withContext(Dispatchers.IO) {
                val account = container.accounts.get(existingId)
                val secrets = container.accounts.secretsOf(existingId)
                account to secrets
            }
            val account = loaded.first
            if (account != null) {
                platform = account.platform
                name = account.name
                clientId = loaded.second.clientId
                apiKey = loaded.second.apiKey
                token = loaded.second.token
                status = account.lastTestMessage
            }
            ready = true
        }
    }

    val draft = AccountDraft(
        id = accountId,
        platform = platform,
        name = name,
        clientId = clientId,
        apiKey = apiKey,
        token = token,
    )
    val valid = name.isNotBlank() && when (platform) {
        PLATFORM_OZON -> clientId.isNotBlank() && apiKey.isNotBlank()
        PLATFORM_WB -> token.isNotBlank()
        PLATFORM_YANDEX -> apiKey.isNotBlank()
        else -> false
    }

    ToolPage(title = platformTitle(platform).ifBlank { "Кабинет" }, onBack = onBack) {
        Text("Ключи сохраняются на этом телефоне и никуда больше не копируются.", color = HdTextSecondary, fontSize = 13.sp)
        HdField(name, { name = it }, "Название магазина")
        when (platform) {
            PLATFORM_OZON -> {
                HdField(clientId, { clientId = it }, "Client-Id", secret = true)
                HdField(apiKey, { apiKey = it }, "Api-Key", secret = true)
            }
            PLATFORM_WB -> HdField(token, { token = it }, "Токен", secret = true)
            PLATFORM_YANDEX -> HdField(apiKey, { apiKey = it }, "Api-Key", secret = true)
        }
        if (status.isNotBlank()) {
            Text(status, color = if (status == "Связь есть") HdUp else HdDown)
        }
        HdButton(text = if (busy) "Проверяю…" else "Проверить связь", enabled = valid && !busy && ready, onClick = {
            busy = true
            scope.launch {
                val checked = container.accounts.test(draft.copy(id = accountId))
                accountId = checked.id
                status = checked.message
                busy = false
            }
        })
        HdButton(text = "Сохранить", enabled = valid && !busy && ready, onClick = {
            busy = true
            scope.launch {
                accountId = container.accounts.save(draft.copy(id = accountId))
                status = "Сохранено на телефоне"
                busy = false
            }
        })
        if (accountId != null) {
            HdTextButton("Удалить кабинет") { confirmDelete = true }
        }
    }
    if (confirmDelete && accountId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить кабинет?") },
            text = { Text("Ключи будут стёрты с телефона.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = accountId ?: return@TextButton
                    scope.launch {
                        container.accounts.delete(id)
                        onBack()
                    }
                }) { Text("Удалить", color = HdDown) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Оставить") } },
        )
    }
}

@Composable
fun CustomListScreen(onBack: () -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit) {
    val container = rememberContainer()
    val apis by container.customApis.observe().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val customOnly = apis.filter { CustomApiPresets.find(it.id) == null }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        HdTopBar(title = "Свои API", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Стандартные курсы", color = HdTextSecondary, fontSize = 13.sp)
            CustomApiPresets.all.forEach { preset ->
                val stored = apis.find { it.id == preset.id }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .deskSurface()
                        .clickable {
                            scope.launch {
                                val id = container.customApis.ensureFromPreset(preset)
                                onOpen(id)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(preset.name, color = HdTextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(preset.hint, color = HdTextSecondary, fontSize = 12.sp)
                        Text(
                            if (stored?.onDesk == true) "На рабочем столе" else "Нажмите, чтобы настроить",
                            color = if (stored?.onDesk == true) HdUp else HdTextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    HdSwitch(
                        checked = stored?.onDesk == true,
                        onChecked = { enabled ->
                            scope.launch { container.customApis.setPresetOnDesk(preset, enabled) }
                        },
                    )
                }
            }
            Text("Свои запросы", color = HdTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            HdButton("Добавить API", onCreate, modifier = Modifier.formWidth())
            if (customOnly.isEmpty()) {
                Text("Своих запросов пока нет", color = HdTextSecondary, fontSize = 13.sp)
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DragList(
                        items = customOnly,
                        keyOf = { it.id },
                        onOrder = { container.customApis.persistOrder(it) },
                    ) { api ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .clickable { onOpen(api.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(api.name, color = HdTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (api.onDesk) "На рабочем столе" else "Скрыт", color = HdTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private class HeaderField(name: String, value: String) {
    var name by mutableStateOf(name)
    var value by mutableStateOf(value)
}

@Composable
fun CustomEditorScreen(existingId: String?, onBack: () -> Unit) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    var apiId by remember { mutableStateOf(existingId) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var method by remember { mutableStateOf("GET") }
    var jsonPath by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var onDesk by remember { mutableStateOf(true) }
    val headers = remember { mutableStateListOf<HeaderField>() }
    var ready by remember { mutableStateOf(existingId == null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(existingId) {
        if (existingId != null && !ready) {
            val loaded = withContext(Dispatchers.IO) {
                val api = container.customApis.get(existingId) ?: return@withContext null
                Triple(api, container.customApis.headers(existingId), container.customApis.body(existingId))
            }
            if (loaded != null) {
                name = loaded.first.name
                url = loaded.first.url
                method = loaded.first.method
                jsonPath = loaded.first.jsonPath
                onDesk = loaded.first.onDesk
                body = loaded.third
                headers.clear()
                loaded.second.forEach { headers.add(HeaderField(it.first, it.second)) }
            }
            ready = true
        }
    }

    val https = url.trim().startsWith("https://")
    val valid = name.isNotBlank() && https && ready
    fun draft() = CustomDraft(
        id = apiId,
        name = name,
        url = url,
        method = method,
        jsonPath = jsonPath,
        body = body,
        headers = headers.map { it.name to it.value },
        onDesk = onDesk,
    )

    ToolPage(title = if (apiId == null) "Новый API" else "Свой API", onBack = onBack) {
        Text("Только https. Значение для плитки берётся по пути, например data.price или items[0].value.", color = HdTextSecondary, fontSize = 13.sp)
        HdField(name, { name = it }, "Название")
        HdField(url, { url = it }, "Адрес", keyboard = KeyboardType.Uri)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = method == "GET", onClick = { method = "GET" }, label = { Text("GET") })
            FilterChip(selected = method == "POST", onClick = { method = "POST" }, label = { Text("POST") })
        }
        HdField(jsonPath, { jsonPath = it }, "Путь в JSON")
        if (method == "POST") {
            HdField(body, { body = it }, "Тело запроса", singleLine = false)
        }
        Text("Заголовки", color = HdTextPrimary, fontWeight = FontWeight.SemiBold)
        headers.forEachIndexed { index, header ->
            HdField(header.name, { header.name = it }, "Имя")
            HdField(header.value, { header.value = it }, "Значение", secret = true)
            TextButton(onClick = { headers.removeAt(index) }) { Text("Убрать заголовок", color = HdDown) }
        }
        TextButton(onClick = { headers.add(HeaderField("", "")) }) { Text("Ещё заголовок", color = HdOrange) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Показывать на рабочем столе", modifier = Modifier.weight(1f), color = HdTextPrimary)
            HdSwitch(checked = onDesk, onChecked = { onDesk = it })
        }
        if (!https && url.isNotBlank()) Text("Нужен адрес https://", color = HdDown, fontSize = 13.sp)
        if (status.isNotBlank()) Text(status, color = if (status.startsWith("Ошибка") || status == "Нет сети") HdDown else HdUp)
        HdButton(text = if (busy) "Запрашиваю…" else "Проверить", enabled = valid && !busy, onClick = {
            busy = true
            scope.launch {
                val id = container.customApis.save(draft().copy(id = apiId))
                apiId = id
                val message = container.desk.refreshCustomById(id)
                status = message
                busy = false
            }
        })
        HdButton(text = "Сохранить", enabled = valid && !busy, onClick = {
            busy = true
            scope.launch {
                apiId = container.customApis.save(draft().copy(id = apiId))
                status = "Сохранено на телефоне"
                busy = false
            }
        })
        if (apiId != null) HdTextButton("Удалить API") { confirmDelete = true }
    }
    if (confirmDelete && apiId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить этот API?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = apiId ?: return@TextButton
                    scope.launch {
                        container.customApis.delete(id)
                        onBack()
                    }
                }) { Text("Удалить", color = HdDown) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Оставить") } },
        )
    }
}

@Composable
private fun <T> DragList(
    items: List<T>,
    keyOf: (T) -> String,
    onOrder: suspend (List<String>) -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf<List<T>?>(null) }
    val rows = dragging ?: items
    val state = rememberHdReorderState()
    val gap = with(LocalDensity.current) { 10.dp.toPx() }
    SideEffect {
        if (!state.isDragging) {
            state.columns = 1
            state.gap = gap
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(rows, key = { _, item -> keyOf(item) }) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .hdReorderItem(state, index)
                    .deskSurface(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).fillMaxHeight()) { itemContent(item) }
                ReorderDragHandle(
                    state = state,
                    index = index,
                    itemCount = rows.size,
                    onDragStart = { if (dragging == null) dragging = rows },
                    onMove = { from, to -> dragging = (dragging ?: rows).swapped(from, to) },
                    onDragEnd = {
                        val ids = dragging?.map(keyOf) ?: return@ReorderDragHandle
                        scope.launch {
                            onOrder(ids)
                            dragging = null
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HdSwitch(checked: Boolean, onChecked: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChecked,
        colors = SwitchDefaults.colors(
            checkedTrackColor = HdBlue,
            checkedThumbColor = HdOrange,
            uncheckedTrackColor = HdTextSecondary.copy(alpha = 0.3f),
            uncheckedThumbColor = HdTextSecondary,
        ),
    )
}

private fun testLine(account: AccountEntity): String {
    val status = account.lastTestMessage.ifBlank { "Ключ ещё не проверен" }
    val time = account.lastTestAt?.let { epoch ->
        val clock = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        " · $clock"
    }.orEmpty()
    return status + time
}