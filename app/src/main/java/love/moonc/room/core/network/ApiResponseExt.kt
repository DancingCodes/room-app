package love.moonc.room.core.network

import love.moonc.room.data.model.ApiResponse

fun <T> ApiResponse<T>.requireData(): T {
    if (code != 200) {
        throw ApiException(code, message)
    }
    return data ?: throw ApiException(code, message)
}

fun ApiResponse<Unit>.requireSuccess() {
    if (code != 200) {
        throw ApiException(code, message)
    }
}
