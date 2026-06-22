package love.moonc.room.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.requireSuccess
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.CreateMessageRequest
import love.moonc.room.data.model.CreateRoomRequest
import love.moonc.room.data.model.Message
import love.moonc.room.data.model.RoomDetail
import love.moonc.room.data.model.UpdateMicRequest

data class CreateRoomUiState(
    val selectedMaxMembers: Int = 8,
    val loading: Boolean = false,
    val message: String? = null,
)

class CreateRoomViewModel(
    private val api: RoomApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateRoomUiState())
    val uiState: StateFlow<CreateRoomUiState> = _uiState

    fun selectMaxMembers(value: Int) {
        _uiState.value = _uiState.value.copy(selectedMaxMembers = value)
    }

    fun create(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val maxMembers = _uiState.value.selectedMaxMembers
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            runCatching { api.createRoom(CreateRoomRequest(maxMembers)).requireData() }
                .onSuccess { detail -> onCreated(detail.room.id) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(loading = false, message = error.userMessage())
                }
        }
    }
}

data class RoomDetailUiState(
    val loading: Boolean = false,
    val detail: RoomDetail? = null,
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val message: String? = null,
)

class RoomDetailViewModel(
    private val api: RoomApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RoomDetailUiState())
    val uiState: StateFlow<RoomDetailUiState> = _uiState

    fun load(roomId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            runCatching {
                val detail = api.roomDetail(roomId).requireData()
                val messages = api.messages(roomId).requireData().list
                detail to messages
            }.onSuccess { (detail, messages) ->
                _uiState.value = RoomDetailUiState(detail = detail, messages = messages)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(loading = false, message = error.userMessage())
            }
        }
    }

    fun updateInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value)
    }

    fun sendMessage(roomId: Long) {
        val content = _uiState.value.input.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            runCatching { api.createMessage(roomId, CreateMessageRequest(content)).requireData().message }
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        input = "",
                        messages = (_uiState.value.messages + message).distinctBy { it.id },
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(message = error.userMessage())
                }
        }
    }

    fun updateMic(roomId: Long, micStatus: String) {
        viewModelScope.launch {
            runCatching { api.updateMic(roomId, UpdateMicRequest(micStatus)).requireData().member }
                .onSuccess { member ->
                    val detail = _uiState.value.detail ?: return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        detail = detail.copy(
                            members = detail.members.map {
                                if (it.userId == member.userId) member else it
                            },
                        ),
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(message = error.userMessage())
                }
        }
    }

    fun leave(roomId: Long, onLeft: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.leaveRoom(roomId).requireSuccess() }
                .onSuccess { onLeft() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(message = error.userMessage())
                }
        }
    }
}
