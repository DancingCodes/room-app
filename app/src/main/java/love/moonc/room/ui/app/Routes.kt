package love.moonc.room.ui.app

// 集中定义应用路由，避免页面之间散落字符串；删除后导航目标容易写错且无法统一维护。

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Main = "main"
    const val EditProfile = "profile/edit"
    const val CreateRoom = "rooms/create"
    const val RoomDetail = "rooms/{roomId}"

    fun roomDetail(roomId: Long): String = "rooms/$roomId"
}
