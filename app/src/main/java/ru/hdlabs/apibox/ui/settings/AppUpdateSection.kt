package ru.hdlabs.apibox.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.hdlabs.apibox.BuildConfig
import ru.hdlabs.apibox.ui.components.HdButton
import ru.hdlabs.apibox.ui.theme.HdTextPrimary
import ru.hdlabs.apibox.ui.theme.HdTextSecondary
import ru.hdlabs.apibox.update.GitHubReleases
import ru.hdlabs.apibox.update.InstallHelper
import ru.hdlabs.apibox.update.UpdateInfo
import java.io.File

@Composable
fun AppUpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<UpdateInfo?>(null) }

    Text("Приложение", color = HdTextSecondary, fontSize = 13.sp)
    Text(
        "Версия ${BuildConfig.VERSION_NAME}",
        color = HdTextPrimary,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        "Кнопка смотрит последний релиз на GitHub и скачивает APK. Ключи на телефоне остаются.",
        color = HdTextSecondary,
        fontSize = 13.sp,
    )
    HdButton(
        text = if (busy) "Проверяю…" else "Проверить обновления",
        enabled = !busy,
        onClick = {
            scope.launch {
                busy = true
                try {
                    val info = withContext(Dispatchers.IO) {
                        GitHubReleases.checkLatest(
                            BuildConfig.GITHUB_RELEASES_OWNER,
                            BuildConfig.GITHUB_RELEASES_REPO,
                            BuildConfig.VERSION_CODE,
                        )
                    }
                    if (info == null) {
                        message = "Установлена последняя версия"
                    } else {
                        pending = info
                    }
                } catch (error: Exception) {
                    message = error.message ?: "Не удалось проверить обновления"
                } finally {
                    busy = false
                }
            }
        },
    )

    pending?.let { info ->
        val notes = info.changelog.take(400)
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = { Text("Доступно обновление") },
            text = {
                Text(
                    buildString {
                        append("Версия ${info.versionName} уже на GitHub. Скачать и установить?")
                        if (notes.isNotBlank()) {
                            append("\n\n")
                            append(notes)
                        }
                    },
                    color = HdTextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    val target = File(context.cacheDir, info.apkFileName)
                                    GitHubReleases.downloadApk(info.apkUrl, target)
                                    target
                                }
                                pending = null
                                val started = InstallHelper.promptInstall(context, file)
                                if (!started) {
                                    message = "Разрешите установку из ApiBox и нажмите кнопку ещё раз."
                                }
                            } catch (error: Exception) {
                                message = error.message ?: "Не удалось скачать обновление"
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) { Text(if (busy) "Скачиваю…" else "Скачать") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pending = null }) { Text("Не скачивать") }
            },
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Обновление") },
            text = { Text(text, color = HdTextSecondary) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("Понятно") }
            },
        )
    }
}
