package love.moonc.room.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.ErrorText
import love.moonc.room.ui.components.FormColumn
import love.moonc.room.ui.components.PrimaryButton
import love.moonc.room.ui.components.RoomScaffold

@Composable
fun CreateRoomScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateRoomViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()

    RoomScaffold(title = "创建房间", onBack = onBack) { modifier ->
        FormColumn(modifier = modifier) {
            Text("房间人数")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedMaxMembers == 2,
                    onClick = { viewModel.selectMaxMembers(2) },
                    label = { Text("2人房") },
                )
                FilterChip(
                    selected = state.selectedMaxMembers == 8,
                    onClick = { viewModel.selectMaxMembers(8) },
                    label = { Text("8人房") },
                )
            }
            PrimaryButton("创建", state.loading, { viewModel.create(onCreated) })
            ErrorText(state.message)
        }
    }
}

@Composable
fun RoomDetailScreen(
    appContainer: AppContainer,
    roomId: Long,
    onLeft: () -> Unit,
    viewModel: RoomDetailViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val detail = state.detail

    LaunchedEffect(roomId) {
        viewModel.load(roomId)
    }

    LaunchedEffect(state.disconnected) {
        if (state.disconnected) {
            onLeft()
        }
    }

    RoomScaffold(
        title = detail?.room?.name ?: "房间详情",
        actions = {
            Button(onClick = { viewModel.leave(roomId, onLeft) }) {
                Text("离开")
            }
        },
    ) { modifier ->
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (detail == null) {
                Text(if (state.loading) "加载中" else "暂无房间信息")
                ErrorText(state.message)
                return@Column
            }

            Text("${detail.room.currentMembers} / ${detail.room.maxMembers} 人")
            Text("成员")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(detail.members, key = { it.userId }) { member ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(member.nickname, modifier = Modifier.weight(1f))
                            if (member.isOwner) Text("房主")
                            Text(if (member.micStatus == "on") "麦克风开" else "麦克风关")
                        }
                    }
                }
            }

            val currentMember = detail.members.firstOrNull()
            if (currentMember != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("我的麦克风")
                    Switch(
                        checked = currentMember.micStatus == "on",
                        onCheckedChange = { checked ->
                            viewModel.updateMic(roomId, if (checked) "on" else "off")
                        },
                    )
                }
            }

            Text("消息")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.hasOlderMessages) {
                    item {
                        Button(
                            onClick = { viewModel.loadOlderMessages(roomId) },
                            enabled = !state.loadingOlder,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.loadingOlder) "加载中" else "加载更早消息")
                        }
                    }
                }
                items(state.messages, key = { it.id }) { message ->
                    Text("${message.senderNickname}: ${message.content}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::updateInput,
                    label = { Text("输入消息") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(onClick = { viewModel.sendMessage(roomId) }) {
                    Text("发送")
                }
            }
            ErrorText(state.message)
        }
    }
}
