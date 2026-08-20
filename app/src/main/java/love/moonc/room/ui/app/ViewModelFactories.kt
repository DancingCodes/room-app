package love.moonc.room.ui.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.auth.AuthViewModel
import love.moonc.room.ui.main.HomeViewModel
import love.moonc.room.ui.profile.ProfileViewModel
import love.moonc.room.ui.room.CreateRoomViewModel
import love.moonc.room.ui.room.RoomDetailViewModel
import love.moonc.room.ui.update.UpdateViewModel

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
            UpdateViewModel::class.java -> UpdateViewModel(appContainer.roomApi, appContainer.apkDownloader)
            AppViewModel::class.java -> AppViewModel(appContainer.roomApi, appContainer.tokenStore)
            AuthViewModel::class.java -> AuthViewModel(appContainer.roomApi, appContainer.tokenStore, appContainer.messageCenter)
            HomeViewModel::class.java -> HomeViewModel(appContainer.roomApi, appContainer.messageCenter)
            ProfileViewModel::class.java -> ProfileViewModel(appContainer.roomApi, appContainer.messageCenter)
            CreateRoomViewModel::class.java -> CreateRoomViewModel(appContainer.roomApi, appContainer.messageCenter)
            RoomDetailViewModel::class.java -> RoomDetailViewModel(
                appContainer.roomApi,
                appContainer.tokenStore,
                appContainer.roomSocketFactory,
                appContainer.messageCenter,
            )
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
