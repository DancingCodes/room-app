package love.moonc.room.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.Room
import love.moonc.room.data.model.User

data class MainUiState(
    val loading: Boolean = false,
    val rooms: List<Room> = emptyList(),
    val user: User? = null,
    val message: String? = null,
)

class MainViewModel(
    private val api: RoomApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            runCatching {
                val rooms = api.rooms().requireData().list
                val user = api.me().requireData().user
                rooms to user
            }.onSuccess { (rooms, user) ->
                _uiState.value = MainUiState(rooms = rooms, user = user)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(loading = false, message = error.userMessage())
            }
        }
    }

    fun joinRoom(roomId: Long, onJoined: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            runCatching { api.joinRoom(roomId).requireData() }
                .onSuccess { detail -> onJoined(detail.room.id) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(loading = false, message = error.userMessage())
                }
        }
    }
}
