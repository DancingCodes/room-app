package love.moonc.room

import android.app.Application
import love.moonc.room.di.AppContainer

class RoomApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
