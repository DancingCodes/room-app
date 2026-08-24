package love.moonc.room.ui.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import love.moonc.room.data.model.User
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.AvatarPicker
import love.moonc.room.ui.components.FormColumn
import love.moonc.room.ui.components.FormTextField
import love.moonc.room.ui.components.PrimaryButton

@Composable
fun EditProfileScreen(
    appContainer: AppContainer,
    user: User?,
    onSaved: (User) -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var nickname by remember { mutableStateOf(user?.nickname.orEmpty()) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    BackHandler(onBack = onBack)

    LaunchedEffect(user?.id) {
        nickname = user?.nickname.orEmpty()
        avatarUri = null
    }

    FormColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AvatarPicker(
            avatarUri = avatarUri,
            avatarUrl = user?.avatarUrl,
            uploading = state.loading,
            onAvatarSelected = { uri -> avatarUri = uri },
        )
        FormTextField(
            value = nickname,
            onValueChange = { nickname = it.take(ProfileViewModel.MAX_NICKNAME_LENGTH) },
            label = "昵称",
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "保存",
            loading = state.loading,
            enabled = user != null,
            onClick = {
                user?.let {
                    viewModel.save(context, nickname, it.avatarUrl, avatarUri, onSaved)
                }
            },
        )
    }
}
