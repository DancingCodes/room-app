package love.moonc.room.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import love.moonc.room.data.model.User
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
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
    var nickname by remember { mutableStateOf("") }

    LaunchedEffect(state.user?.id) {
        state.user?.let { nickname = it.nickname }
    }

    RoomScaffold(title = "编辑资料", onBack = onBack) { modifier ->
        FormColumn(modifier = modifier) {
            FormTextField(nickname, { nickname = it }, "昵称")
            PrimaryButton("保存", state.loading, { viewModel.save(nickname, onSaved) })
            ErrorText(state.message)
        }
    }
}
