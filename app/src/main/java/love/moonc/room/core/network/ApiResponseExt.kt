package love.moonc.room.core.network

import love.moonc.room.data.model.ApiResponse

// 校验业务成功并返回 data；删除后每个接口调用处都要重复判断 code 和 data。
fun <T> ApiResponse<T>.requireData(): T {
    requireSuccessCode()
    return data ?: throw ApiException(message)
}

// 校验没有 data 的成功响应；删除后离开房间等操作无法统一处理业务失败。
fun ApiResponse<Unit>.requireSuccess() {
    requireSuccessCode()
}

// 统一识别登录过期并抛出业务异常；删除后各页面会漏掉 401 的退出登录处理。
private fun ApiResponse<*>.requireSuccessCode() {
    if (code == 401) {
        SessionExpiredCenter.notifyExpired()
    }
    if (code != 200) {
        throw ApiException(message)
    }
}
