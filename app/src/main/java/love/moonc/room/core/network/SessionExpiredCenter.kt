package love.moonc.room.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

// 将任意接口发现的登录过期汇总为一次全局事件，避免多个页面重复提示和重复退出。
object SessionExpiredCenter {
    // 保证同一登录周期只发出一次过期事件；删除后并发请求可能连续触发多次退出。
    private val expired = AtomicBoolean(false)
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    // 在检测到后端 401 时通知根页面清除 Token 并跳转登录。
    fun notifyExpired() {
        if (expired.compareAndSet(false, true)) {
            _events.tryEmit(Unit)
        }
    }

    // 登录成功或恢复会话后允许下一次真实过期重新通知；删除后后续登录可能不会响应 401。
    fun reset() {
        expired.set(false)
    }
}
