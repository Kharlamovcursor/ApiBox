package ru.hdlabs.apibox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.hdlabs.apibox.ui.components.DeskFace
import ru.hdlabs.apibox.ui.components.HdButton
import ru.hdlabs.apibox.ui.components.HdTopBar
import ru.hdlabs.apibox.ui.components.deskSurface
import ru.hdlabs.apibox.ui.components.formWidth
import ru.hdlabs.apibox.ui.components.rememberContainer
import ru.hdlabs.apibox.ui.dashboard.DeskCard
import ru.hdlabs.apibox.ui.dashboard.buildDeskCards
import ru.hdlabs.apibox.ui.dashboard.statusOk
import ru.hdlabs.apibox.ui.theme.HdBlue
import ru.hdlabs.apibox.ui.theme.HdDown
import ru.hdlabs.apibox.ui.theme.HdTextPrimary
import ru.hdlabs.apibox.ui.theme.HdTextSecondary
import ru.hdlabs.apibox.ui.theme.HdUp

@Composable
fun DeskStatusScreen(onBack: () -> Unit, onOpenCatalog: () -> Unit) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val items by container.desk.observe().collectAsStateWithLifecycle(initialValue = emptyList())
    val quotes by container.desk.observeQuotes().collectAsStateWithLifecycle(initialValue = emptyList())
    val customs by container.customApis.observe().collectAsStateWithLifecycle(initialValue = emptyList())
    var busy by rememberSaveable { mutableStateOf(false) }

    val cards = buildDeskCards(items, quotes, customs)
    val okCount = cards.count { it.statusOk() }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        HdTopBar(title = "Статус виджетов", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .deskSurface()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Проверка столика",
                        color = HdTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Text(
                        "Здесь видно, что отвечает на рабочем столе: значение, подпись и ошибка сети. " +
                            "Обновление — как на главном экране, раз в минуту пока приложение открыто.",
                        color = HdTextSecondary,
                        fontSize = 13.sp,
                    )
                    if (cards.isNotEmpty()) {
                        Text(
                            "Сейчас в порядке: $okCount из ${cards.size}",
                            color = if (okCount == cards.size) HdUp else HdDown,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    HdButton(
                        text = if (busy) "Обновляю…" else "Обновить все виджеты",
                        enabled = !busy && cards.isNotEmpty(),
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    container.desk.refreshAll()
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.formWidth(),
                    )
                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = HdBlue)
                            Text("Запросы идут в фоне", color = HdTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (cards.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("На столе пока ничего нет.", color = HdTextSecondary)
                        HdButton("Открыть каталог API", onOpenCatalog, modifier = Modifier.formWidth())
                    }
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    StatusRow(card)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(card: DeskCard) {
    val ok = card.statusOk()
    Column(Modifier.fillMaxWidth().deskSurface()) {
        DeskFace(card = card, compact = true, modifier = Modifier.fillMaxWidth())
        Text(
            if (ok) "Ответ получен" else "Нужна проверка: ${card.footer}",
            color = if (ok) HdUp else HdDown,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
