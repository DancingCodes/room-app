package love.moonc.room.core.network

import love.moonc.room.data.model.ApiResponse

fun <T> ApiResponse<T>.requireData(): T {
    requireSuccessCode()
    return data ?: throw ApiException(message)
}

fun ApiResponse<Unit>.requireSuccess() {
    requireSuccessCode()
}

private fun ApiResponse<*>.requireSuccessCode() {
    if (code == 401) {
        SessionExpiredCenter.notifyExpired()
    }
    if (code != 200) {
        throw ApiException(message)
    }
}
