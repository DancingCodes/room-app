package love.moonc.room.data.websocket

import love.moonc.room.core.network.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RoomSocketFactory(
    private val okHttpClient: OkHttpClient,
) {
    fun connect(
        roomId: Long,
        token: String,
        listener: WebSocketListener,
    ): WebSocket {
        val request = Request.Builder()
            .url(roomSocketUrl(roomId))
            .header("Authorization", "Bearer $token")
            .build()
        return okHttpClient.newWebSocket(request, listener)
    }

    private fun roomSocketUrl(roomId: Long): String {
        val base = NetworkConfig.BACKEND_BASE_URL.trimEnd('/')
        val wsBase = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> base
        }
        return "$wsBase/api/v1/ws/rooms/$roomId"
    }
}
