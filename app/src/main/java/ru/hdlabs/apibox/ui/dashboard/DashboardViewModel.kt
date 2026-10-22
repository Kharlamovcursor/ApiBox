package ru.hdlabs.apibox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.hdlabs.apibox.AppContainer
import ru.hdlabs.apibox.data.local.CustomApiEntity
import ru.hdlabs.apibox.data.local.DeskItemEntity
import ru.hdlabs.apibox.data.local.QuoteEntity
import ru.hdlabs.apibox.domain.KIND_CUSTOM
import ru.hdlabs.apibox.domain.KIND_MOEX
import ru.hdlabs.apibox.domain.MoexCatalog
import ru.hdlabs.apibox.domain.swapped

data class DeskCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val value: String,
    val delta: String,
    val up: Boolean?,
    val footer: String,
)

data class DashboardUi(
    val cards: List<DeskCard> = emptyList(),
    val tiles: Boolean = true,
    val refreshing: Boolean = false,
)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    private val dragOrder = MutableStateFlow<List<String>?>(null)
    private val refreshing = MutableStateFlow(false)
    private var refreshJob: Job? = null

    private val cards = combine(
        container.desk.observe(),
        container.desk.observeQuotes(),
        container.customApis.observe(),
    ) { items, quotes, customs ->
        val quoteById = quotes.associateBy { it.deskItemId }
        val customById = customs.associateBy { it.id }
        items.mapNotNull { item -> item.toCard(quoteById[item.id], customById) }
    }

    val ui: StateFlow<DashboardUi> = combine(
        cards,
        container.settings.settings,
        dragOrder,
        refreshing,
    ) { list, settings, order, busy ->
        DashboardUi(
            cards = arrange(list, order),
            tiles = settings.tiles,
            refreshing = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUi())

    fun refreshNow() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshing.value = true
            try {
                container.desk.refreshAll()
            } finally {
                refreshing.value = false
            }
        }
    }

    fun onDragStart() {
        dragOrder.value = ui.value.cards.map { it.id }
    }

    fun onMove(from: Int, to: Int) {
        val current = dragOrder.value ?: return
        dragOrder.value = current.swapped(from, to)
    }

    fun onDragEnd() {
        val ids = dragOrder.value ?: return
        viewModelScope.launch {
            container.desk.persistOrder(ids)
            dragOrder.value = null
        }
    }

    fun setTiles(value: Boolean) {
        if (dragOrder.value != null) return
        viewModelScope.launch { container.settings.setTiles(value) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(container) as T
            }
    }
}

private fun arrange(cards: List<DeskCard>, order: List<String>?): List<DeskCard> {
    if (order == null) return cards
    val byId = cards.associateBy { it.id }
    val kept = order.mapNotNull { byId[it] }
    val rest = cards.filter { it.id !in order }
    return kept + rest
}

private fun DeskItemEntity.toCard(
    quote: QuoteEntity?,
    customs: Map<String, CustomApiEntity>,
): DeskCard? {
    val title: String
    val subtitle: String
    when (kind) {
        KIND_MOEX -> {
            val index = MoexCatalog.find(ref)
            title = ref
            subtitle = index?.title ?: ref
        }
        KIND_CUSTOM -> {
            val api = customs[ref] ?: return null
            title = api.name
            subtitle = "Свой API"
        }
        else -> return null
    }
    return DeskCard(
        id = id,
        title = title,
        subtitle = subtitle,
        value = quote?.valueText ?: "…",
        delta = quote?.deltaText.orEmpty(),
        up = quote?.up,
        footer = quote?.footer ?: "загрузка",
    )
}
