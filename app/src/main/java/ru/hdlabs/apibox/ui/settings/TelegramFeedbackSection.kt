package ru.hdlabs.apibox.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hdlabs.apibox.feedback.TelegramFeedback
import ru.hdlabs.apibox.ui.components.HdButton
import ru.hdlabs.apibox.ui.theme.HdTextPrimary
import ru.hdlabs.apibox.ui.theme.HdTextSecondary

@Composable
fun TelegramFeedbackSection() {
    val context = LocalContext.current
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    Text("Обратная связь", color = HdTextPrimary, fontSize = 16.sp)
    Text(
        "Откроется Telegram. Напишите оценку или комментарий — сообщение увидит только разработчик.",
        color = HdTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
    HdButton(text = "Написать разработчику", onClick = {
        TelegramFeedback.open(context).onFailure {
            dialogMessage = if (!TelegramFeedback.isConfigured()) {
                "Telegram не настроен в этой сборке."
            } else {
                it.message ?: "Не удалось открыть Telegram. Установите приложение Telegram."
            }
        }
    })

    dialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Обратная связь") },
            text = { Text(message, color = HdTextSecondary) },
            confirmButton = {
                HdButton(text = "Понятно", onClick = { dialogMessage = null })
            },
        )
    }
}
