package love.moonc.room

import android.app.Application
import love.moonc.room.di.AppContainer

class RoomApplication : Application() {
    // 网络、存储和语音等全局依赖只在进程启动时创建一次；放进 Activity 会在界面重建时重复创建。
    lateinit var appContainer: AppContainer
    private set

    override fun onCreate() {
        super.onCreate()
        // 使用 Application Context 创建依赖，避免持有 Activity 导致内存泄漏。
        appContainer = AppContainer(this)
    }
}
