package love.moonc.room.ui.profile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import love.moonc.room.data.model.User
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.AvatarPicker
import love.moonc.room.ui.components.ErrorText
import love.moonc.room.ui.components.FormColumn
import love.moonc.room.ui.components.FormTextField
import love.moonc.room.ui.components.PrimaryButton
import love.moonc.room.ui.components.RoomScaffold

@Composable
fun EditProfileScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onSaved: (User) -> Unit,
    viewModel: ProfileViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var nickname by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(state.user?.id) {
        state.user?.let { nickname = it.nickname }
    }

    RoomScaffold(title = "编辑资料", onBack = onBack) { modifier ->
        FormColumn(modifier = modifier) {
            AvatarPicker(
                avatarUri = avatarUri,
                avatarUrl = state.user?.avatarUrl,
                uploading = state.uploadingAvatar,
                onAvatarSelected = { uri ->
                    avatarUri = uri
                    viewModel.updateAvatar(context, uri, onSaved)
                },
            )
            FormTextField(nickname, { nickname = it }, "昵称")
            PrimaryButton("保存", state.loading, { viewModel.save(nickname, onSaved) })
            ErrorText(state.message)
        }
    }
}
