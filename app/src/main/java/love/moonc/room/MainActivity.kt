package love.moonc.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import love.moonc.room.ui.app.RoomApp
import love.moonc.room.ui.theme.RoomappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让页面延伸到系统状态栏和导航栏区域；删除后界面会避开这些区域。
        enableEdgeToEdge()
        // 将 Compose 根页面放入 Activity 窗口；删除后应用没有可显示的界面。
        setContent {
            RoomappTheme {
                // 从 Application 获取同一组全局依赖，供页面和 ViewModel 共享。
                RoomApp(appContainer = (application as RoomApplication).appContainer)
            }
        }
    }
}
