package love.moonc.room.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SocketEvent(
    val type: String,
    @SerialName("room_id")
    val roomId: Long,
    val data: JsonElement,
)

@Serializable
data class MemberJoinedEvent(
    val member: RoomMember,
    @SerialName("current_members")
    val currentMembers: Int,
)

@Serializable
data class MemberLeftEvent(
    @SerialName("user_id")
    val userId: Long,
    @SerialName("current_members")
    val currentMembers: Int,
)

@Serializable
data class OwnerChangedEvent(
    @SerialName("owner_id")
    val ownerId: Long,
    val members: List<RoomMember>,
)
