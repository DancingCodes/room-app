package love.moonc.room.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import love.moonc.room.data.model.User
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.CenteredFormColumn
import love.moonc.room.ui.components.FormTextField
import love.moonc.room.ui.components.PrimaryButton
import love.moonc.room.ui.components.RoomSpacing

@Composable
fun LoginScreen(
    appContainer: AppContainer,
    onLoginSuccess: (User) -> Unit,
    viewModel: AuthViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    CenteredFormColumn(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = -RoomSpacing.LoginLift),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "星闪",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(RoomSpacing.SectionGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormTextField(email, { email = it }, "邮箱", Modifier.weight(1f))
            TextButton(
                onClick = { viewModel.sendLoginCode(email) },
                enabled = !state.sendingLoginCode && state.loginCodeCountdownSeconds == 0,
                modifier = Modifier.heightIn(min = RoomSpacing.FieldMinHeight),
            ) {
                Text(
                    when {
                        state.sendingLoginCode -> "发送中"
                        state.loginCodeCountdownSeconds > 0 -> "${state.loginCodeCountdownSeconds}s"
                        else -> "发送验证码"
                    },
                )
            }
        }
        FormTextField(code, { code = it }, "验证码")
        PrimaryButton(
            text = "登录",
            loading = state.loading,
            onClick = { viewModel.login(email, code, onLoginSuccess) },
            modifier = Modifier.padding(top = RoomSpacing.CompactGap),
        )
    }
}