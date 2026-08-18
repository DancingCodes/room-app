package love.moonc.room.ui.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.core.file.toAvatarPart
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
import love.moonc.room.ui.message.MessageCenter

data class AuthUiState(
    val loading: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val avatarUrl: String = "",
)

class AuthViewModel(
    private val api: RoomApi,
    private val tokenStore: TokenStore,
    private val messageCenter: MessageCenter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(account: String, password: String, onSuccess: (User) -> Unit) {
        val trimmedAccount = account.trim()
        if (trimmedAccount.isBlank() || password.isBlank()) {
            messageCenter.show("请填写账号和密码")
            return
        }

        launchAuth {
            val result = api.login(LoginRequest(trimmedAccount, password)).requireData()
            tokenStore.saveToken(result.token)
            onSuccess(result.user)
        }
    }

    fun sendRegisterCode(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            messageCenter.show("请填写邮箱")
            return
        }

        launchAuth(successMessage = "验证码已发送") {
            api.sendRegisterCode(EmailCodeRequest(trimmedEmail)).requireSuccess()
        }
    }

    fun uploadRegisterAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(uploadingAvatar = true)
            runCatching { api.uploadRegisterAvatar(uri.toAvatarPart(context)).requireData().avatarUrl }
                .onSuccess { avatarUrl ->
                    _uiState.value = _uiState.value.copy(
                        uploadingAvatar = false,
                        avatarUrl = avatarUrl,
                    )
                    messageCenter.show("头像已上传")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(uploadingAvatar = false)
                    messageCenter.show(error.userMessage())
                }
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
        val trimmedAccount = account.trim()
        val trimmedEmail = email.trim()
        val trimmedEmailCode = emailCode.trim()
        val trimmedNickname = nickname.trim()
        val trimmedAvatarUrl = avatarUrl.trim()
        if (trimmedAccount.isBlank() || trimmedEmail.isBlank() || trimmedEmailCode.isBlank() ||
            password.isBlank() || trimmedNickname.isBlank() || trimmedAvatarUrl.isBlank()
        ) {
            messageCenter.show("请填写完整注册信息")
            return
        }

        launchAuth {
            val result = api.register(
                RegisterRequest(
                    account = trimmedAccount,
                    email = trimmedEmail,
                    emailCode = trimmedEmailCode,
                    password = password,
                    nickname = trimmedNickname,
                    avatarUrl = trimmedAvatarUrl,
                ),
            ).requireData()
            tokenStore.saveToken(result.token)
            onSuccess(result.user)
        }
    }

    fun sendPasswordResetCode(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            messageCenter.show("请填写邮箱")
            return
        }

        launchAuth(successMessage = "验证码已发送") {
            api.sendPasswordResetCode(EmailCodeRequest(trimmedEmail)).requireSuccess()
        }
    }

    fun resetPassword(email: String, emailCode: String, newPassword: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedEmailCode = emailCode.trim()
        if (trimmedEmail.isBlank() || trimmedEmailCode.isBlank() || newPassword.isBlank()) {
            messageCenter.show("请填写完整重置信息")
            return
        }

        launchAuth {
            api.resetPassword(
                ResetPasswordRequest(
                    email = trimmedEmail,
                    emailCode = trimmedEmailCode,
                    newPassword = newPassword,
                ),
            ).requireSuccess()
            onSuccess()
        }
    }

    private fun launchAuth(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching { block() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(loading = false)
                    messageCenter.show(successMessage)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(loading = false)
                    messageCenter.show(error.userMessage())
                }
        }
    }
}