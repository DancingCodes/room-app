package love.moonc.room.core.network

object NetworkConfig {
    // 后端 HTTP 地址，WebSocket 地址也由它派生；删除后网络客户端无法确定服务端位置。
    const val BACKEND_BASE_URL = "https://room.moonc.love/"
    // 声网应用标识，用于创建 RTC 引擎；删除后无法加入语音频道。
    const val AGORA_APP_ID = "cca0b8f4bd794b95b0479667be7c9cff"
}
