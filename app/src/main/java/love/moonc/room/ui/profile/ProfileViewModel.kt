package love.moonc.room.ui.profile

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

data class ProfileUiState(
    val loading: Boolean = false,
    val user: User? = null,
    val message: String? = null,
)

class ProfileViewModel(
    private val api: RoomApi,
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
                .onFailure { error -> _uiState.value = ProfileUiState(message = error.userMessage()) }
        }
    }

    fun save(nickname: String, onSaved: (User) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            runCatching { api.updateMe(UpdateMeRequest(nickname.trim())).requireData().user }
                .onSuccess { user ->
                    _uiState.value = ProfileUiState(user = user)
                    onSaved(user)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(loading = false, message = error.userMessage())
                }
        }
    }
}
