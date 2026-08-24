package love.moonc.room.ui.app

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Main = "main"
    const val EditProfile = "profile/edit"
    const val CreateRoom = "rooms/create"
    const val RoomDetail = "rooms/{roomId}"

    fun roomDetail(roomId: Long): String = "rooms/$roomId"
}
