package love.moonc.room.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import love.moonc.room.core.network.requireData
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.User
import love.moonc.room.data.storage.TokenStore

data class AppUiState(
    val isChecking: Boolean = true,
    val user: User? = null,
    val targetRoute: String? = null,
)

class AppViewModel(
    private val api: RoomApi,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    init {
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch {
            val token = tokenStore.token.firstOrNull()
            if (token.isNullOrBlank()) {
                _uiState.value = AppUiState(isChecking = false, targetRoute = Routes.Login)
                return@launch
            }

            runCatching { api.me().requireData().user }
                .onSuccess { user -> setUser(user) }
                .onFailure {
                    tokenStore.clearToken()
                    _uiState.value = AppUiState(isChecking = false, targetRoute = Routes.Login)
                }
        }
    }

    fun setUser(user: User) {
        _uiState.update {
            it.copy(
                isChecking = false,
                user = user,
                targetRoute = routeForUser(user),
            )
        }
    }

    fun clearNavigationTarget() {
        _uiState.update { it.copy(targetRoute = null) }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStore.clearToken()
            _uiState.value = AppUiState(isChecking = false, targetRoute = Routes.Login)
        }
    }

    private fun routeForUser(user: User): String {
        val roomId = user.currentRoomId
        return if (roomId == null) Routes.Home else Routes.roomDetail(roomId)
    }
}
