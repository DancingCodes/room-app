package love.moonc.room.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.core.network.requireSuccess
import love.moonc.room.core.network.userMessage
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.EmailCodeRequest
import love.moonc.room.data.model.LoginRequest
import love.moonc.room.data.model.RegisterRequest
import love.moonc.room.data.model.ResetPasswordRequest
import love.moonc.room.data.model.User
import love.moonc.room.data.storage.TokenStore

data class AuthUiState(
    val loading: Boolean = false,
    val message: String? = null,
)

class AuthViewModel(
    private val api: RoomApi,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(account: String, password: String, onSuccess: (User) -> Unit) {
        launchAuth {
            val result = api.login(LoginRequest(account.trim(), password)).requireData()
            tokenStore.saveToken(result.token)
            onSuccess(result.user)
        }
    }

    fun sendRegisterCode(email: String) {
        launchAuth(successMessage = "验证码已发送") {
            api.sendRegisterCode(EmailCodeRequest(email.trim())).requireSuccess()
        }
    }

    fun register(
        account: String,
        email: String,
        emailCode: String,
        password: String,
        nickname: String,
        avatarUrl: String,
        onSuccess: (User) -> Unit,
    ) {
        launchAuth {
            val result = api.register(
                RegisterRequest(
                    account = account.trim(),
                    email = email.trim(),
                    emailCode = emailCode.trim(),
                    password = password,
                    nickname = nickname.trim(),
                    avatarUrl = avatarUrl.trim(),
                ),
            ).requireData()
            tokenStore.saveToken(result.token)
            onSuccess(result.user)
        }
    }

    fun sendPasswordResetCode(email: String) {
        launchAuth(successMessage = "验证码已发送") {
            api.sendPasswordResetCode(EmailCodeRequest(email.trim())).requireSuccess()
        }
    }

    fun resetPassword(email: String, emailCode: String, newPassword: String, onSuccess: () -> Unit) {
        launchAuth {
            api.resetPassword(
                ResetPasswordRequest(
                    email = email.trim(),
                    emailCode = emailCode.trim(),
                    newPassword = newPassword,
                ),
            ).requireSuccess()
            onSuccess()
        }
    }

    private fun launchAuth(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            runCatching { block() }
                .onSuccess { _uiState.value = AuthUiState(message = successMessage) }
                .onFailure { error -> _uiState.value = AuthUiState(message = error.userMessage()) }
        }
    }
}
