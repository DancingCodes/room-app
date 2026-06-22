package love.moonc.room.ui.auth

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import love.moonc.room.di.AppContainer
import love.moonc.room.data.model.User
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.CenteredFormColumn
import love.moonc.room.ui.components.ErrorText
import love.moonc.room.ui.components.AvatarPicker
import love.moonc.room.ui.components.FormColumn
import love.moonc.room.ui.components.FormTextField
import love.moonc.room.ui.components.PrimaryButton
import love.moonc.room.ui.components.RoomScaffold

@Composable
fun LoginScreen(
    appContainer: AppContainer,
    onLoginSuccess: (User) -> Unit,
    onRegisterClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    viewModel: AuthViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    RoomScaffold(title = "Room") { modifier ->
        CenteredFormColumn(modifier.fillMaxSize()) {
            Spacer(Modifier.height(80.dp))
            Text("Room", style = MaterialTheme.typography.headlineMedium)
            Text("轻量房间聊天", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            FormTextField(account, { account = it }, "账号")
            FormTextField(password, { password = it }, "密码", password = true)
            PrimaryButton("登录", state.loading, { viewModel.login(account, password, onLoginSuccess) })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onRegisterClick) { Text("注册账号") }
                Spacer(Modifier.width(16.dp))
                TextButton(onClick = onResetPasswordClick) { Text("忘记密码") }
            }
            ErrorText(state.message)
        }
    }
}

@Composable
fun RegisterScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onRegisterSuccess: (User) -> Unit,
    viewModel: AuthViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    RoomScaffold(title = "注册", onBack = onBack) { modifier ->
        CenteredFormColumn(modifier.fillMaxSize()) {
            AvatarPicker(
                avatarUri = avatarUri,
                avatarUrl = state.avatarUrl,
                uploading = state.uploadingAvatar,
                onAvatarSelected = { uri ->
                    avatarUri = uri
                    viewModel.uploadRegisterAvatar(context, uri)
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormTextField(email, { email = it }, "邮箱", Modifier.weight(1f))
                TextButton(onClick = { viewModel.sendRegisterCode(email) }) { Text("发验证码") }
            }
            FormTextField(code, { code = it }, "验证码")
            FormTextField(account, { account = it }, "账号")
            FormTextField(nickname, { nickname = it }, "昵称")
            FormTextField(password, { password = it }, "密码", password = true)
            PrimaryButton("注册", state.loading, {
                viewModel.register(account, email, code, password, nickname, state.avatarUrl, onRegisterSuccess)
            })
            ErrorText(state.message)
        }
    }
}

@Composable
fun ResetPasswordScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: AuthViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    RoomScaffold(title = "找回密码", onBack = onBack) { modifier ->
        CenteredFormColumn(modifier.fillMaxSize()) {
            Spacer(Modifier.height(80.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormTextField(email, { email = it }, "邮箱", Modifier.weight(1f))
                TextButton(onClick = { viewModel.sendPasswordResetCode(email) }) { Text("发验证码") }
            }
            FormTextField(code, { code = it }, "验证码")
            FormTextField(password, { password = it }, "新密码", password = true)
            PrimaryButton("重置密码", state.loading, {
                viewModel.resetPassword(email, code, password, onResetSuccess)
            })
            ErrorText(state.message)
        }
    }
}
