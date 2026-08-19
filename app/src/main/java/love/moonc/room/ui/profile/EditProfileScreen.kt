package love.moonc.room.ui.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
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
import love.moonc.room.ui.components.CenteredFormColumn
import love.moonc.room.ui.components.FormTextField
import love.moonc.room.ui.components.PrimaryButton

@Composable
fun EditProfileScreen(
    appContainer: AppContainer,
    onSaved: (User) -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var nickname by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    BackHandler(onBack = onBack)

    LaunchedEffect(state.user?.id) {
        state.user?.let { nickname = it.nickname }
    }

    CenteredFormColumn(Modifier.fillMaxSize()) {
        AvatarPicker(
            avatarUri = avatarUri,
            avatarUrl = state.user?.avatarUrl,
            uploading = state.loading,
            onAvatarSelected = { uri -> avatarUri = uri },
        )
        FormTextField(nickname, { nickname = it }, "昵称")
        PrimaryButton(
            text = "保存",
            loading = state.loading,
            onClick = { viewModel.save(context, nickname, avatarUri, onSaved) },
        )
    }
}