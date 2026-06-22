package love.moonc.room.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.requireSuccess
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.CreateMessageRequest
import love.moonc.room.data.model.CreateRoomRequest
import love.moonc.room.data.model.MemberJoinedEvent
import love.moonc.room.data.model.MemberLeftEvent
import love.moonc.room.data.model.Message
import love.moonc.room.data.model.MessagePayload
import love.moonc.room.data.model.MemberPayload
import love.moonc.room.data.model.OwnerChangedEvent
import love.moonc.room.data.model.RoomDetail
import love.moonc.room.data.model.SocketEvent
import love.moonc.room.data.model.UpdateMicRequest
import love.moonc.room.data.storage.TokenStore
import love.moonc.room.data.websocket.RoomSocketFactory
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

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
    val loadingOlder: Boolean = false,
    val detail: RoomDetail? = null,
    val messages: List<Message> = emptyList(),
    val hasOlderMessages: Boolean = false,
    val input: String = "",
    val message: String? = null,
    val disconnected: Boolean = false,
)

class RoomDetailViewModel(
    private val api: RoomApi,
    private val tokenStore: TokenStore,
    private val socketFactory: RoomSocketFactory,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RoomDetailUiState())
    val uiState: StateFlow<RoomDetailUiState> = _uiState
    private val json = Json { ignoreUnknownKeys = true }
    private var webSocket: WebSocket? = null
    private val messagePageSize = 20

    fun load(roomId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            runCatching {
                val detail = api.roomDetail(roomId).requireData()
                val messages = api.messages(roomId, limit = messagePageSize).requireData().list
                detail to messages
            }.onSuccess { (detail, messages) ->
                _uiState.value = RoomDetailUiState(
                    detail = detail,
                    messages = messages,
                    hasOlderMessages = messages.size >= messagePageSize,
                )
                connectSocket(roomId)
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
                        messages = (_uiState.value.messages + message).distinctBy { it.id }.sortedBy { it.id },
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(message = error.userMessage())
                }
        }
    }

    fun loadOlderMessages(roomId: Long) {
        val state = _uiState.value
        if (state.loading || state.loadingOlder || !state.hasOlderMessages) return
        val beforeId = state.messages.minOfOrNull { it.id } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingOlder = true, message = null)
            runCatching {
                api.messages(roomId, limit = messagePageSize, beforeId = beforeId).requireData().list
            }.onSuccess { olderMessages ->
                _uiState.value = _uiState.value.copy(
                    loadingOlder = false,
                    messages = (olderMessages + _uiState.value.messages).distinctBy { it.id }.sortedBy { it.id },
                    hasOlderMessages = olderMessages.size >= messagePageSize,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(loadingOlder = false, message = error.userMessage())
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

    private fun connectSocket(roomId: Long) {
        if (webSocket != null) return

        viewModelScope.launch {
            val token = tokenStore.token.firstOrNull()
            if (token.isNullOrBlank()) return@launch

            webSocket = socketFactory.connect(
                roomId = roomId,
                token = token,
                listener = object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleSocketMessage(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        handleSocketDisconnected()
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        handleSocketDisconnected()
                    }
                },
            )
        }
    }

    private fun handleSocketMessage(text: String) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val event = json.decodeFromString<SocketEvent>(text)
                when (event.type) {
                    "member.joined" -> {
                        val payload = json.decodeFromJsonElement(MemberJoinedEvent.serializer(), event.data)
                        updateMemberJoined(payload)
                    }
                    "member.left" -> {
                        val payload = json.decodeFromJsonElement(MemberLeftEvent.serializer(), event.data)
                        updateMemberLeft(payload)
                    }
                    "room.owner_changed" -> {
                        val payload = json.decodeFromJsonElement(OwnerChangedEvent.serializer(), event.data)
                        updateOwnerChanged(payload)
                    }
                    "message.created" -> {
                        val payload = json.decodeFromJsonElement(MessagePayload.serializer(), event.data)
                        updateMessageCreated(payload)
                    }
                    "member.mic_updated" -> {
                        val payload = json.decodeFromJsonElement(MemberPayload.serializer(), event.data)
                        updateMemberMic(payload)
                    }
                }
            }
        }
    }

    private fun updateMemberJoined(payload: MemberJoinedEvent) {
        val detail = _uiState.value.detail ?: return
        val members = (detail.members.filterNot { it.userId == payload.member.userId } + payload.member)
            .sortedBy { it.joinedAt }
        _uiState.value = _uiState.value.copy(
            detail = detail.copy(
                room = detail.room.copy(currentMembers = payload.currentMembers),
                members = members,
            ),
        )
    }

    private fun updateMemberLeft(payload: MemberLeftEvent) {
        val detail = _uiState.value.detail ?: return
        _uiState.value = _uiState.value.copy(
            detail = detail.copy(
                room = detail.room.copy(currentMembers = payload.currentMembers),
                members = detail.members.filterNot { it.userId == payload.userId },
            ),
        )
    }

    private fun updateOwnerChanged(payload: OwnerChangedEvent) {
        val detail = _uiState.value.detail ?: return
        _uiState.value = _uiState.value.copy(
            detail = detail.copy(
                room = detail.room.copy(ownerId = payload.ownerId),
                members = payload.members,
            ),
        )
    }

    private fun updateMessageCreated(payload: MessagePayload) {
        _uiState.value = _uiState.value.copy(
            messages = (_uiState.value.messages + payload.message).distinctBy { it.id }.sortedBy { it.id },
        )
    }

    private fun updateMemberMic(payload: MemberPayload) {
        val detail = _uiState.value.detail ?: return
        _uiState.value = _uiState.value.copy(
            detail = detail.copy(
                members = detail.members.map {
                    if (it.userId == payload.member.userId) payload.member else it
                },
            ),
        )
    }

    private fun handleSocketDisconnected() {
        _uiState.value = _uiState.value.copy(
            disconnected = true,
            message = "连接已断开，已离开房间",
        )
    }

    override fun onCleared() {
        webSocket?.close(1000, "view model cleared")
        webSocket = null
        super.onCleared()
    }
}
