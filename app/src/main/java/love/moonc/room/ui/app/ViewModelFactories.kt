package love.moonc.room.ui.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.auth.AuthViewModel
import love.moonc.room.ui.main.MainViewModel
import love.moonc.room.ui.profile.ProfileViewModel
import love.moonc.room.ui.room.CreateRoomViewModel
import love.moonc.room.ui.room.RoomDetailViewModel

@Composable
inline fun <reified VM : ViewModel> roomViewModel(appContainer: AppContainer): VM {
    return viewModel(factory = RoomViewModelFactory(appContainer))
}

class RoomViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            AppViewModel::class.java -> AppViewModel(appContainer.roomApi, appContainer.tokenStore)
            AuthViewModel::class.java -> AuthViewModel(appContainer.roomApi, appContainer.tokenStore)
            MainViewModel::class.java -> MainViewModel(appContainer.roomApi)
            ProfileViewModel::class.java -> ProfileViewModel(appContainer.roomApi)
            CreateRoomViewModel::class.java -> CreateRoomViewModel(appContainer.roomApi)
            RoomDetailViewModel::class.java -> RoomDetailViewModel(
                appContainer.roomApi,
                appContainer.tokenStore,
                appContainer.roomSocketFactory,
            )
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
