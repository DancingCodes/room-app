package love.moonc.room.ui.auth

// 处理邮箱验证码、登录和令牌保存；删除后登录页只能展示输入框，无法完成登录。

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val loginCodeCountdownSeconds: Int = 0,
)

class AuthViewModel(
    private val api: RoomApi,
    private val tokenStore: TokenStore,
    private val messageCenter: MessageCenter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    private var loginCodeCountdownJob: Job? = null

    fun sendLoginCode(email: String) {
        val trimmedEmail = email.trim()
        if (!validateEmail(trimmedEmail)) return
        val state = _uiState.value
        if (state.sendingLoginCode || state.loginCodeCountdownSeconds > 0) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendingLoginCode = true)
            runCatching { api.sendEmailLoginCode(EmailCodeRequest(trimmedEmail)).requireSuccess() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(sendingLoginCode = false)
                    messageCenter.show("验证码已发送")
                    startLoginCodeCountdown()
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
        if (!validateEmail(trimmedEmail)) return
        if (trimmedEmailCode.isBlank()) {
            messageCenter.show("请填写验证码")
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

    private fun startLoginCodeCountdown() {
        loginCodeCountdownJob?.cancel()
        loginCodeCountdownJob = viewModelScope.launch {
            for (seconds in 60 downTo 1) {
                _uiState.value = _uiState.value.copy(loginCodeCountdownSeconds = seconds)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(loginCodeCountdownSeconds = 0)
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isBlank()) {
            messageCenter.show("请填写邮箱")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            messageCenter.show("请填写正确的邮箱")
            return false
        }
        return true
    }
}
