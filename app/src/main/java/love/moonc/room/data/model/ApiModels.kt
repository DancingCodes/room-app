package love.moonc.room.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
)

@Serializable
data class AuthResult(
    val token: String,
    val user: User,
)

@Serializable
data class UserPayload(
    val user: User,
)

@Serializable
data class AvatarPayload(
    @SerialName("avatar_url")
    val avatarUrl: String,
)

@Serializable
data class User(
    val id: Long,
    val account: String,
    val email: String,
    val nickname: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("current_room_id")
    val currentRoomId: Long? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class Room(
    val id: Long,
    val name: String,
    @SerialName("owner_id")
    val ownerId: Long,
    @SerialName("current_members")
    val currentMembers: Int,
    @SerialName("max_members")
    val maxMembers: Int,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class RoomMember(
    @SerialName("user_id")
    val userId: Long,
    val nickname: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("is_owner")
    val isOwner: Boolean,
    @SerialName("mic_status")
    val micStatus: String,
    @SerialName("joined_at")
    val joinedAt: String,
)

@Serializable
data class RoomDetail(
    val room: Room,
    val members: List<RoomMember>,
)

@Serializable
data class RoomListPayload(
    val list: List<Room>,
    val total: Int,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
)

@Serializable
data class Message(
    val id: Long,
    @SerialName("room_id")
    val roomId: Long,
    @SerialName("sender_id")
    val senderId: Long,
    @SerialName("sender_nickname")
    val senderNickname: String,
    @SerialName("sender_avatar_url")
    val senderAvatarUrl: String,
    val type: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class MessagePayload(
    val message: Message,
)

@Serializable
data class MessageListPayload(
    val list: List<Message>,
    val limit: Int,
)

@Serializable
data class MemberPayload(
    val member: RoomMember,
)
