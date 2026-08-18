package love.moonc.room.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.requireSuccess
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.EmailCodeRequest
import love.moonc.room.data.model.EmailLoginRequest
import love.moonc.room.data.model.User
import love.moonc.room.data.storage.TokenStore
import love.moonc.room.ui.message.MessageCenter

data class AuthUiState(
    val loading: Boolean = false,
    val sendingLoginCode: Boolean = false,
)

class AuthViewModel(
    private val api: RoomApi,
    private val tokenStore: TokenStore,
    private val messageCenter: MessageCenter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun sendLoginCode(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            messageCenter.show("请填写邮箱")
            return
        }
        if (_uiState.value.sendingLoginCode) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendingLoginCode = true)
            runCatching { api.sendEmailLoginCode(EmailCodeRequest(trimmedEmail)).requireSuccess() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(sendingLoginCode = false)
                    messageCenter.show("验证码已发送")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(sendingLoginCode = false)
                    messageCenter.show(error.userMessage())
                }
        }
    }

    fun login(email: String, emailCode: String, onSuccess: (User) -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedEmailCode = emailCode.trim()
        if (trimmedEmail.isBlank() || trimmedEmailCode.isBlank()) {
            messageCenter.show("请填写邮箱和验证码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { api.emailLogin(EmailLoginRequest(trimmedEmail, trimmedEmailCode)).requireData() }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(loading = false)
                    tokenStore.saveToken(result.token)
                    onSuccess(result.user)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(loading = false)
                    messageCenter.show(error.userMessage())
                }
        }
    }
}