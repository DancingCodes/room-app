package love.moonc.room.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailCodeRequest(
    val email: String,
)

@Serializable
data class EmailLoginRequest(
    val email: String,
    @SerialName("email_code")
    val emailCode: String,
)

@Serializable
data class UpdateMeRequest(
    val nickname: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
)

@Serializable
data class CreateRoomRequest(
    @SerialName("max_members")
    val maxMembers: Int,
)

@Serializable
data class UpdateMicRequest(
    @SerialName("mic_status")
    val micStatus: String,
)

@Serializable
data class CreateMessageRequest(
    val content: String,
)