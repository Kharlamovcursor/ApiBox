package ru.hdlabs.apibox.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import ru.hdlabs.apibox.ui.components.DeskFace
import ru.hdlabs.apibox.ui.components.HdTopBar
import ru.hdlabs.apibox.ui.components.deskSurface
import ru.hdlabs.apibox.ui.components.rememberContainer
import ru.hdlabs.apibox.ui.reorder.ReorderDragHandle
import ru.hdlabs.apibox.ui.reorder.ReorderPointer
import ru.hdlabs.apibox.ui.reorder.hdReorderItem
import ru.hdlabs.apibox.ui.reorder.rememberHdReorderState
import ru.hdlabs.apibox.ui.theme.HdBlue
import ru.hdlabs.apibox.ui.theme.HdTextSecondary
import androidx.compose.material3.Text

@Composable
fun DashboardScreen(onSettings: () -> Unit) {
    val container = rememberContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshNow()
            while (true) {
                delay(60_000)
                viewModel.refreshNow()
            }
        }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        HdTopBar(title = "ApiBox") {
            IconButton(onClick = { viewModel.setTiles(!ui.tiles) }) {
                Icon(
                    imageVector = if (ui.tiles) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (ui.tiles) "Показать списком" else "Показать плитками",
                    tint = HdTextSecondary,
                )
            }
            IconButton(onClick = viewModel::refreshNow, enabled = !ui.refreshing) {
                if (ui.refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = HdBlue,
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Обновить", tint = HdTextSecondary)
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = HdTextSecondary)
            }
        }
        if (ui.cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Включите индекс или добавьте свой API в настройках",
                    color = HdTextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (ui.tiles) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val columns = if (maxWidth > 700.dp) 3 else 2
                DeskGrid(
                    cards = ui.cards,
                    columns = columns,
                    tiles = true,
                    onDragStart = viewModel::onDragStart,
                    onMove = viewModel::onMove,
                    onDragEnd = viewModel::onDragEnd,
                )
            }
        } else {
            DeskGrid(
                cards = ui.cards,
                columns = 1,
                tiles = false,
                onDragStart = viewModel::onDragStart,
                onMove = viewModel::onMove,
                onDragEnd = viewModel::onDragEnd,
            )
        }
    }
}

@Composable
private fun DeskGrid(
    cards: List<DeskCard>,
    columns: Int,
    tiles: Boolean,
    onDragStart: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
) {
    val state = rememberHdReorderState()
    val gap = with(LocalDensity.current) { if (tiles) 12.dp.toPx() else 10.dp.toPx() }
    SideEffect {
        if (!state.isDragging) {
            state.columns = columns
            state.gap = gap
        }
    }
    val padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    if (tiles) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                ReorderPointer(
                    state = state,
                    index = index,
                    itemCount = cards.size,
                    onDragStart = onDragStart,
                    onMove = onMove,
                    onDragEnd = onDragEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .hdReorderItem(state, index)
                        .deskSurface(),
                ) {
                    DeskFace(card = card, compact = false, modifier = Modifier.fillMaxSize())
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .hdReorderItem(state, index)
                        .deskSurface(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeskFace(card = card, compact = true, modifier = Modifier.weight(1f).fillMaxHeight())
                    ReorderDragHandle(
                        state = state,
                        index = index,
                        itemCount = cards.size,
                        onDragStart = onDragStart,
                        onMove = onMove,
                        onDragEnd = onDragEnd,
                    )
                }
            }
        }
    }
}
