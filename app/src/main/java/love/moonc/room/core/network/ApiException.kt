package love.moonc.room.core.network

class ApiException(
    val code: Int,
    override val message: String,
) : RuntimeException(message)

fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "网络错误"
