package love.moonc.room.ui.message

// 在非界面代码与页面之间传递短提示；删除后网络失败等反馈无法统一显示给用户。

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest

class MessageCenter {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    fun show(message: String?) {
        if (!message.isNullOrBlank()) {
            _messages.tryEmit(message)
        }
    }
}

@Composable
fun CenterMessageHost(
    messageCenter: MessageCenter,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(messageCenter) {
        messageCenter.messages.collectLatest { nextMessage ->
            message = nextMessage
            delay(1800)
            message = null
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        message?.let { text ->
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
