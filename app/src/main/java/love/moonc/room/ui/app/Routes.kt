package love.moonc.room.ui.app

// 集中定义应用路由，避免页面之间散落字符串；删除后导航目标容易写错且无法统一维护。
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val EDIT_PROFILE = "profile/edit"
    const val CREATE_ROOM = "rooms/create"
    const val ROOM_DETAIL = "rooms/{roomId}"

    fun roomDetail(roomId: Long): String = "rooms/$roomId"
}
