package love.moonc.room.ui.main

// 维护首页房间列表、分页和入房操作；删除后首页无法加载或进入房间。

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.Room
import love.moonc.room.ui.message.MessageCenter

data class HomeUiState(
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val joiningRoomId: Long? = null,
    val rooms: List<Room> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 20,
    val total: Int = 0,
    val hasMore: Boolean = false,
)

class HomeViewModel(
    private val api: RoomApi,
    private val messageCenter: MessageCenter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var loadedUserId: Long? = null

    fun loadForUser(userId: Long) {
        if (loadedUserId == userId) return
        refresh(markLoadedUserId = userId)
    }

    fun clear() {
        loadedUserId = null
        _uiState.value = HomeUiState()
    }

    fun refresh() {
        refresh(markLoadedUserId = null)
    }

    private fun refresh(markLoadedUserId: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching {
                api.rooms(page = 1, pageSize = _uiState.value.pageSize).requireData()
            }.onSuccess { roomPayload ->
                if (markLoadedUserId != null) {
                    loadedUserId = markLoadedUserId
                }
                _uiState.value = HomeUiState(
                    rooms = roomPayload.list,
                    page = roomPayload.page,
                    pageSize = roomPayload.pageSize,
                    total = roomPayload.total,
                    hasMore = roomPayload.page * roomPayload.pageSize < roomPayload.total,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(loading = false)
                messageCenter.show(error.userMessage())
            }
        }
    }

    fun loadMoreRooms() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMore = true)
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
                _uiState.value = _uiState.value.copy(loadingMore = false)
                messageCenter.show(error.userMessage())
            }
        }
    }

    fun joinRoom(roomId: Long, onJoined: (Long) -> Unit) {
        if (_uiState.value.joiningRoomId != null) return

        _uiState.value = _uiState.value.copy(joiningRoomId = roomId)
        viewModelScope.launch {
            runCatching { api.joinRoom(roomId).requireData() }
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(joiningRoomId = null)
                    onJoined(detail.room.id)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(joiningRoomId = null)
                    messageCenter.show(error.userMessage())
                }
        }
    }
}
