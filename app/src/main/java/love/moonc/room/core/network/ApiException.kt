package love.moonc.room.core.network

// 表示后端返回的业务失败；保留独立异常类型，调用方才能与本地异常使用同一错误提示流程。
class ApiException(message: String) : RuntimeException(message)

// 将没有有效消息的网络异常统一为用户可理解的提示；删除后页面需要各自处理空错误信息。
fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "网络错误"
