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
        enableEdgeToEdge()
        setContent {
            RoomappTheme {
                RoomApp(appContainer = (application as RoomApplication).appContainer)
            }
        }
    }
}
