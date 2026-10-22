package ru.hdlabs.apibox

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.hdlabs.apibox.data.AccountRepository
import ru.hdlabs.apibox.data.CustomApiRepository
import ru.hdlabs.apibox.data.DeskRepository
import ru.hdlabs.apibox.data.NetClient
import ru.hdlabs.apibox.data.SecretStore
import ru.hdlabs.apibox.data.SettingsRepository
import ru.hdlabs.apibox.data.local.AppDatabase

class ApiBoxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        runBlocking(Dispatchers.IO) {
            container.desk.ensureSeed()
        }
    }
}

class AppContainer(app: Application) {
    private val database = Room.databaseBuilder(app, AppDatabase::class.java, "apibox.db").build()
    val secrets = SecretStore(app)
    val settings = SettingsRepository(app)
    val net = NetClient()
    val desk = DeskRepository(
        deskDao = database.deskDao(),
        quoteDao = database.quoteDao(),
        customDao = database.customApiDao(),
        secrets = secrets,
        net = net,
    )
    val accounts = AccountRepository(database.accountDao(), secrets, net)
    val customApis = CustomApiRepository(database.customApiDao(), secrets, desk)
}
