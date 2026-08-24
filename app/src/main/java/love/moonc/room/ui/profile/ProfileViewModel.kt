package love.moonc.room.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.UpdateMeRequest
import love.moonc.room.data.model.User
import love.moonc.room.ui.message.MessageCenter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class ProfileUiState(
    val loading: Boolean = false,
)

class ProfileViewModel(
    private val api: RoomApi,
    private val messageCenter: MessageCenter,
) : ViewModel() {
    companion object {
        const val MAX_NICKNAME_LENGTH = 8
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun save(
        context: Context,
        nickname: String,
        currentAvatarUrl: String,
        avatarUri: Uri?,
        onSaved: (User) -> Unit,
    ) {
        if (_uiState.value.loading) return
        val trimmedNickname = nickname.trim()
        if (trimmedNickname.length !in 1..MAX_NICKNAME_LENGTH) {
            messageCenter.show("昵称需为1-8个字符")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching {
                val avatarUrl = avatarUri?.let { uri ->
                    api.uploadImage(uri.toImagePart(context)).requireData().url
                } ?: currentAvatarUrl
                api.updateMe(UpdateMeRequest(trimmedNickname, avatarUrl)).requireData().user
            }.onSuccess { user ->
                _uiState.value = ProfileUiState()
                onSaved(user)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(loading = false)
                messageCenter.show(error.userMessage())
            }
        }
    }
}

private fun Uri.toImagePart(context: Context): MultipartBody.Part {
    val mimeType = context.contentResolver.getType(this) ?: "application/octet-stream"
    val bytes = context.contentResolver.openInputStream(this)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("无法读取头像文件")
    return MultipartBody.Part.createFormData("file", "image", bytes.toRequestBody(mimeType.toMediaType()))
}
