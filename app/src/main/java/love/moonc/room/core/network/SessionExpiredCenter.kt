package love.moonc.room.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

object SessionExpiredCenter {
    private val expired = AtomicBoolean(false)
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifyExpired() {
        if (expired.compareAndSet(false, true)) {
            _events.tryEmit(Unit)
        }
    }

    fun reset() {
        expired.set(false)
    }
}