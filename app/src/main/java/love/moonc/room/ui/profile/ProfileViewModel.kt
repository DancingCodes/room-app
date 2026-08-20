package love.moonc.room.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.file.toImagePart
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.UpdateMeRequest
import love.moonc.room.data.model.User
import love.moonc.room.ui.message.MessageCenter

data class ProfileUiState(
    val loading: Boolean = false,
)

class ProfileViewModel(
    private val api: RoomApi,
    private val messageCenter: MessageCenter,
) : ViewModel() {
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching {
                val avatarUrl = avatarUri?.let { uri ->
                    api.uploadImage(uri.toImagePart(context)).requireData().url
                } ?: currentAvatarUrl
                api.updateMe(UpdateMeRequest(nickname.trim(), avatarUrl)).requireData().user
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
