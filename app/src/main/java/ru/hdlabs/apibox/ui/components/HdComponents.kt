package ru.hdlabs.apibox.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hdlabs.apibox.ApiBoxApp
import ru.hdlabs.apibox.AppContainer
import ru.hdlabs.apibox.ui.dashboard.DeskCard
import ru.hdlabs.apibox.ui.theme.HdBlue
import ru.hdlabs.apibox.ui.theme.HdDown
import ru.hdlabs.apibox.ui.theme.HdOrange
import ru.hdlabs.apibox.ui.theme.HdPanel
import ru.hdlabs.apibox.ui.theme.HdTextPrimary
import ru.hdlabs.apibox.ui.theme.HdTextSecondary
import ru.hdlabs.apibox.ui.theme.HdUp

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as ApiBoxApp).container
}

fun Context.clipboardText(): String? {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return null
    val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
    return text?.takeIf { it.isNotEmpty() }
}

fun Modifier.formWidth(): Modifier = widthIn(max = 520.dp).fillMaxWidth()

fun Modifier.deskSurface(): Modifier {
    val shape = RoundedCornerShape(22.dp)
    return clip(shape)
        .background(HdPanel.copy(alpha = 0.9f))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(HdBlue.copy(alpha = 0.55f), HdOrange.copy(alpha = 0.4f))),
            shape = shape,
        )
}

@Composable
fun HdTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = HdTextPrimary)
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            color = HdTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun ToolPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        HdTopBar(title = title, onBack = onBack)
        Box(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .formWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
fun HdField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    secret: Boolean = false,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    val context = LocalContext.current
    var revealed by rememberSaveable(label) { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        visualTransformation = if (secret && !revealed) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else keyboard,
        ),
        trailingIcon = {
            Row {
                if (secret) {
                    IconButton(onClick = { revealed = !revealed }) {
                        Icon(
                            imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (revealed) "Скрыть" else "Показать",
                            tint = HdTextSecondary,
                        )
                    }
                }
                IconButton(onClick = {
                    context.clipboardText()?.let(onValueChange)
                }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Вставить", tint = HdOrange)
                }
            }
        },
        colors = fieldColors(),
    )
}

@Composable
fun HdButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HdBlue, contentColor = Color.Black),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HdTextButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, color = HdDown)
    }
}

@Composable
fun DeskFace(card: DeskCard, compact: Boolean, modifier: Modifier = Modifier) {
    val big = card.value.length <= 14
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Column {
            Text(card.title, color = HdBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.subtitle, color = HdTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = card.value,
            color = HdTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 20.sp else if (big) 28.sp else 16.sp,
            maxLines = if (big) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (card.delta.isNotBlank()) {
            Text(
                text = card.delta,
                color = when (card.up) {
                    true -> HdUp
                    false -> HdDown
                    null -> HdTextSecondary
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(card.footer, color = HdTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HdBlue,
    unfocusedBorderColor = HdTextSecondary.copy(alpha = 0.35f),
    focusedTextColor = HdTextPrimary,
    unfocusedTextColor = HdTextPrimary,
    cursorColor = HdOrange,
    focusedLabelColor = HdBlue,
    unfocusedLabelColor = HdTextSecondary,
    focusedContainerColor = HdPanel.copy(alpha = 0.72f),
    unfocusedContainerColor = HdPanel.copy(alpha = 0.72f),
)
