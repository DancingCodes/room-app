package love.moonc.room.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
            Text("房间人数", style = MaterialTheme.typography.titleMedium)
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
    onLeft: (String?) -> Unit,
    viewModel: RoomDetailViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val detail = state.detail

    LaunchedEffect(roomId) {
        viewModel.load(roomId)
    }

    LaunchedEffect(state.disconnected) {
        if (state.disconnected) {
            onLeft("连接已断开，已离开房间")
        }
    }

    RoomScaffold(
        title = detail?.room?.name ?: "房间详情",
        actions = {
            IconButton(onClick = { viewModel.leave(roomId) { onLeft(null) } }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "离开")
            }
        },
    ) { modifier ->
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (detail == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (state.loading) "加载中" else "暂无房间信息")
                        ErrorText(state.message)
                    }
                }
                return@Column
            }

            val currentMember = detail.members.firstOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${detail.room.currentMembers} / ${detail.room.maxMembers} 人",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (currentMember != null) {
                            Text("我的麦克风")
                            Switch(
                                checked = currentMember.micStatus == "on",
                                onCheckedChange = { checked ->
                                    viewModel.updateMic(roomId, if (checked) "on" else "off")
                                },
                            )
                        }
                    }
                    Text("成员", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(detail.members, key = { it.userId }) { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(member.nickname, modifier = Modifier.weight(1f))
                                if (member.isOwner) Text("房主")
                                Text(if (member.micStatus == "on") "麦克风开" else "麦克风关")
                            }
                        }
                    }
                }
            }

            Text("消息", style = MaterialTheme.typography.titleMedium)
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
                    Text(
                        text = "${message.senderNickname}: ${message.content}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                Button(
                    onClick = { viewModel.sendMessage(roomId) },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text("发送")
                }
            }
            ErrorText(state.message)
        }
    }
}
