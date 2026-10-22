package ru.hdlabs.apibox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.hdlabs.apibox.ui.AppNavHost
import ru.hdlabs.apibox.ui.theme.ApiBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApiBoxTheme {
                AppNavHost()
            }
        }
    }
}
