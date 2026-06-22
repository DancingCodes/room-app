package love.moonc.room.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailCodeRequest(
    val email: String,
)

@Serializable
data class RegisterRequest(
    val account: String,
    val email: String,
    @SerialName("email_code")
    val emailCode: String,
    val password: String,
    val nickname: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
)

@Serializable
data class LoginRequest(
    val account: String,
    val password: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    @SerialName("email_code")
    val emailCode: String,
    @SerialName("new_password")
    val newPassword: String,
)

@Serializable
data class UpdateMeRequest(
    val nickname: String,
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
