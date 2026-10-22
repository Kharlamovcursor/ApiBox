package ru.hdlabs.apibox.ui.reorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.hdlabs.apibox.ui.theme.HdTextSecondary

private const val SettleMs = 240

private suspend fun slide(
    x: Animatable<Float, AnimationVector1D>,
    y: Animatable<Float, AnimationVector1D>,
) {
    val spec = tween<Float>(SettleMs, easing = FastOutSlowInEasing)
    coroutineScope {
        launch { x.animateTo(0f, spec) }
        launch { y.animateTo(0f, spec) }
    }
}

@Stable
class HdReorderState {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set
    var dragOffsetX by mutableFloatStateOf(0f)
        private set
    var dragOffsetY by mutableFloatStateOf(0f)
        private set
    var columns by mutableIntStateOf(1)
    var cellWidth by mutableIntStateOf(0)
    var cellHeight by mutableIntStateOf(0)
    var gap by mutableFloatStateOf(0f)

    val isDragging: Boolean get() = draggingIndex != null

    fun startDrag(index: Int) {
        draggingIndex = index
        dragOffsetX = 0f
        dragOffsetY = 0f
    }

    fun dragBy(dx: Float, dy: Float, count: Int, onMove: (Int, Int) -> Unit) {
        if (draggingIndex == null) return
        dragOffsetX += dx
        dragOffsetY += dy
        val stepX = (cellWidth + gap).coerceAtLeast(1f)
        val stepY = (cellHeight + gap).coerceAtLeast(1f)
        val cols = columns.coerceAtLeast(1)
        if (cellWidth <= 0 || cellHeight <= 0) return
        while (true) {
            val index = draggingIndex ?: return
            val col = index % cols
            when {
                cols > 1 && col < cols - 1 && index + 1 < count && dragOffsetX > stepX / 2f -> {
                    onMove(index, index + 1)
                    draggingIndex = index + 1
                    dragOffsetX -= stepX
                }
                cols > 1 && col > 0 && dragOffsetX < -stepX / 2f -> {
                    onMove(index, index - 1)
                    draggingIndex = index - 1
                    dragOffsetX += stepX
                }
                index + cols < count && dragOffsetY > stepY / 2f -> {
                    onMove(index, index + cols)
                    draggingIndex = index + cols
                    dragOffsetY -= stepY
                }
                index >= cols && dragOffsetY < -stepY / 2f -> {
                    onMove(index, index - cols)
                    draggingIndex = index - cols
                    dragOffsetY += stepY
                }
                else -> break
            }
        }
    }

    fun endDrag() {
        draggingIndex = null
        dragOffsetX = 0f
        dragOffsetY = 0f
    }
}

@Composable
fun rememberHdReorderState(): HdReorderState = remember { HdReorderState() }

fun Modifier.hdReorderItem(
    state: HdReorderState,
    index: Int,
): Modifier = composed {
    val dragging = state.draggingIndex == index
    var lastIndex by remember { mutableIntStateOf(index) }
    val settleX = remember { Animatable(0f) }
    val settleY = remember { Animatable(0f) }
    LaunchedEffect(index, state.draggingIndex) {
        val previous = lastIndex
        lastIndex = index
        if (previous != index && state.isDragging && state.draggingIndex != index) {
            val cols = state.columns.coerceAtLeast(1)
            val stepX = state.cellWidth + state.gap
            val stepY = state.cellHeight + state.gap
            settleX.snapTo(((previous % cols) - (index % cols)) * stepX)
            settleY.snapTo(((previous / cols) - (index / cols)) * stepY)
            slide(settleX, settleY)
        } else if (!state.isDragging && (settleX.value != 0f || settleY.value != 0f)) {
            slide(settleX, settleY)
        }
    }
    val followX by animateFloatAsState(
        targetValue = if (dragging) state.dragOffsetX else 0f,
        animationSpec = tween(if (dragging) 0 else SettleMs, easing = FastOutSlowInEasing),
        label = "reorderX",
    )
    val followY by animateFloatAsState(
        targetValue = if (dragging) state.dragOffsetY else 0f,
        animationSpec = tween(if (dragging) 0 else SettleMs, easing = FastOutSlowInEasing),
        label = "reorderY",
    )
    val settleShiftX = if (dragging) 0f else settleX.value
    val settleShiftY = if (dragging) 0f else settleY.value
    val scale by animateFloatAsState(
        targetValue = if (dragging) 1.04f else 1f,
        animationSpec = tween(SettleMs, easing = FastOutSlowInEasing),
        label = "reorderScale",
    )
    this
        .zIndex(if (dragging) 2f else 0f)
        .onSizeChanged {
            if (it.width > 0) state.cellWidth = it.width
            if (it.height > 0) state.cellHeight = it.height
        }
        .graphicsLayer {
            translationX = followX + settleShiftX
            translationY = followY + settleShiftY
            scaleX = scale
            scaleY = scale
            shadowElevation = if (dragging) 12f else 0f
        }
}

@Composable
fun ReorderDragHandle(
    state: HdReorderState,
    index: Int,
    itemCount: Int,
    onDragStart: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReorderPointer(
        state = state,
        index = index,
        itemCount = itemCount,
        onDragStart = onDragStart,
        onMove = onMove,
        onDragEnd = onDragEnd,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 4.dp),
    ) {
        Text("⋮⋮", color = HdTextSecondary, fontSize = 28.sp)
    }
}

@Composable
fun ReorderPointer(
    state: HdReorderState,
    index: Int,
    itemCount: Int,
    onDragStart: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val latestIndex by rememberUpdatedState(index)
    val latestCount by rememberUpdatedState(itemCount)
    val latestStart by rememberUpdatedState(onDragStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestEnd by rememberUpdatedState(onDragEnd)
    var dragging by remember { mutableStateOf(false) }
    val blockParentScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!dragging && !state.isDragging) return Offset.Zero
                return available
            }
        }
    }
    Box(
        modifier
            .nestedScroll(blockParentScroll)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        state.startDrag(latestIndex)
                        latestStart()
                    },
                    onDragEnd = {
                        dragging = false
                        state.endDrag()
                        latestEnd()
                    },
                    onDragCancel = {
                        dragging = false
                        state.endDrag()
                        latestEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.dragBy(dragAmount.x, dragAmount.y, latestCount, latestMove)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
