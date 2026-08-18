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
import love.moonc.room.core.file.toAvatarPart
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.UpdateMeRequest
import love.moonc.room.data.model.User
import love.moonc.room.ui.message.MessageCenter

data class ProfileUiState(
    val loading: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val user: User? = null,
)

class ProfileViewModel(
    private val api: RoomApi,
    private val messageCenter: MessageCenter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { api.me().requireData().user }
                .onSuccess { user -> _uiState.value = ProfileUiState(user = user) }
                .onFailure { error ->
                    _uiState.value = ProfileUiState()
                    messageCenter.show(error.userMessage())
                }
        }
    }

    fun save(nickname: String, onSaved: (User) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { api.updateMe(UpdateMeRequest(nickname.trim())).requireData().user }
                .onSuccess { user ->
                    _uiState.value = ProfileUiState(user = user)
                    onSaved(user)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(loading = false)
                    messageCenter.show(error.userMessage())
                }
        }
    }

    fun updateAvatar(context: Context, uri: Uri, onUpdated: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(uploadingAvatar = true)
            runCatching { api.updateMyAvatar(uri.toAvatarPart(context)).requireData().user }
                .onSuccess { user ->
                    _uiState.value = ProfileUiState(user = user)
                    onUpdated()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(uploadingAvatar = false)
                    messageCenter.show(error.userMessage())
                }
        }
    }
}
