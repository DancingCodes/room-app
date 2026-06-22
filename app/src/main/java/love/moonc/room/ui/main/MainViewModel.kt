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
    val loadingMore: Boolean = false,
    val rooms: List<Room> = emptyList(),
    val user: User? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
    val total: Int = 0,
    val hasMore: Boolean = false,
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
                val roomPayload = api.rooms(page = 1, pageSize = _uiState.value.pageSize).requireData()
                val user = api.me().requireData().user
                roomPayload to user
            }.onSuccess { (roomPayload, user) ->
                _uiState.value = MainUiState(
                    rooms = roomPayload.list,
                    user = user,
                    page = roomPayload.page,
                    pageSize = roomPayload.pageSize,
                    total = roomPayload.total,
                    hasMore = roomPayload.page * roomPayload.pageSize < roomPayload.total,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = _uiState.value.message ?: error.userMessage(),
                )
            }
        }
    }

    fun loadMoreRooms() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMore = true, message = null)
            runCatching {
                api.rooms(page = state.page + 1, pageSize = state.pageSize).requireData()
            }.onSuccess { roomPayload ->
                val rooms = (_uiState.value.rooms + roomPayload.list).distinctBy { it.id }
                _uiState.value = _uiState.value.copy(
                    loadingMore = false,
                    rooms = rooms,
                    page = roomPayload.page,
                    pageSize = roomPayload.pageSize,
                    total = roomPayload.total,
                    hasMore = roomPayload.page * roomPayload.pageSize < roomPayload.total,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(loadingMore = false, message = error.userMessage())
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

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
}
