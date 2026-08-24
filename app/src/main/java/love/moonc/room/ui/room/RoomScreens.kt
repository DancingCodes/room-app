package love.moonc.room.ui.room

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import love.moonc.room.data.model.RoomMember
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.FormColumn
import love.moonc.room.ui.components.PrimaryButton
import love.moonc.room.ui.components.RoomSpacing
import love.moonc.room.ui.components.ScreenColumn

@Composable
fun CreateRoomScreen(
    appContainer: AppContainer,
    onCreated: (Long) -> Unit,
    viewModel: CreateRoomViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()

    FormColumn(modifier = Modifier.fillMaxSize()) {
        Text("房间人数", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap)) {
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
        Spacer(Modifier.weight(1f))
        PrimaryButton("创建", state.loading, { viewModel.create(onCreated) })
    }
}

@Composable
fun RoomDetailScreen(
    appContainer: AppContainer,
    roomId: Long,
    onLeft: (String?) -> Unit,
    onUnauthorized: () -> Unit,
    currentUserId: Long? = null,
    viewModel: RoomDetailViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    val detail = state.detail
    val context = LocalContext.current
    val requestMicrophonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.updateMic(roomId, "on")
        } else {
            appContainer.messageCenter.show("未授予录音权限")
        }
    }
    val leaveThresholdPx = with(LocalDensity.current) { RoomSpacing.SwipeLeaveThreshold.toPx() }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(roomId, currentUserId) {
        currentUserId?.let { userId -> viewModel.load(roomId, userId) }
    }

    LaunchedEffect(state.disconnected) {
        if (state.disconnected) {
            onLeft("连接已断开，已离开房间")
        }
    }

    LaunchedEffect(state.unauthorized) {
        if (state.unauthorized) {
            onUnauthorized()
        }
    }

    BackHandler(enabled = detail != null && !showLeaveConfirm) {
        showLeaveConfirm = true
    }

    BackHandler(enabled = showLeaveConfirm) {
        if (!state.leaving) {
            showLeaveConfirm = false
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!state.leaving) {
                    showLeaveConfirm = false
                }
            },
            title = { Text("离开房间？") },
            text = { Text("确定要离开当前房间吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leave(roomId) {
                            showLeaveConfirm = false
                            onLeft(null)
                        }
                    },
                    enabled = !state.leaving,
                ) {
                    Text(if (state.leaving) "离开中" else "离开")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLeaveConfirm = false },
                    enabled = !state.leaving,
                ) {
                    Text("取消")
                }
            },
        )
    }

    ScreenColumn(
        modifier = Modifier.pointerInput(roomId, detail != null, showLeaveConfirm) {
            var dragDistance = 0f
            detectHorizontalDragGestures(
                onDragEnd = { dragDistance = 0f },
                onDragCancel = { dragDistance = 0f },
            ) { _, dragAmount ->
                if (detail != null && !showLeaveConfirm) {
                    dragDistance += dragAmount
                    if (dragDistance <= -leaveThresholdPx) {
                        dragDistance = 0f
                        showLeaveConfirm = true
                    }
                }
            }
        },
        verticalArrangement = Arrangement.spacedBy(RoomSpacing.ItemGap),
    ) {
        if (detail == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.loading) "加载中" else "暂无房间信息")
                }
            }
            return@ScreenColumn
        }

        Text(
            text = detail.room.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
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
            if (state.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("暂无消息", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    Text(
                        text = "${message.senderNickname}: ${message.content}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::updateInput,
                label = { Text("输入消息") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            FilledIconButton(
                onClick = { viewModel.sendMessage(roomId) },
                enabled = state.input.isNotBlank(),
                modifier = Modifier.size(RoomSpacing.FieldMinHeight),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
        }

        MemberAvatarRow(
            members = detail.members,
            maxMembers = detail.room.maxMembers,
            currentUserId = currentUserId,
            onToggleMic = { member ->
                if (member.micStatus == "on") {
                    viewModel.updateMic(roomId, "off")
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.updateMic(roomId, "on")
                } else {
                    requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
    }
}

@Composable
private fun MemberAvatarRow(
    members: List<RoomMember>,
    maxMembers: Int,
    currentUserId: Long?,
    onToggleMic: (RoomMember) -> Unit,
) {
    val slotCount = maxOf(maxMembers, members.size)
    val slots = List(slotCount) { index -> members.getOrNull(index) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RoomSpacing.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(slots.size, key = { it }) { index ->
            val member = slots[index]
            MemberAvatarItem(
                member = member,
                canToggleMic = member?.userId == currentUserId,
                onToggleMic = onToggleMic,
            )
        }
    }
}

@Composable
private fun MemberAvatarItem(
    member: RoomMember?,
    canToggleMic: Boolean,
    onToggleMic: (RoomMember) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(RoomSpacing.MemberAvatarSize + RoomSpacing.ItemGap)
            .clickable(enabled = canToggleMic && member != null) {
                member?.let(onToggleMic)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
    ) {
        Box(modifier = Modifier.size(RoomSpacing.MemberAvatarSize)) {
            AvatarSlot(member = member)
            if (member != null) {
                MicStatusBadge(
                    micOn = member.micStatus == "on",
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
        Text(
            text = when {
                member == null -> "空位"
                member.isOwner -> "${member.nickname} · 房主"
                else -> member.nickname
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (member == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AvatarSlot(member: RoomMember?) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape),
        shape = CircleShape,
        color = if (member == null) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (member == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        tonalElevation = RoomSpacing.CompactGap / 4,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (member?.avatarUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = "头像")
            }
        }
    }
}

@Composable
private fun MicStatusBadge(
    micOn: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(RoomSpacing.MicStatusBadgeSize),
        shape = CircleShape,
        color = if (micOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (micOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = RoomSpacing.CompactGap / 2,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = if (micOn) "麦克风开" else "麦克风关",
                modifier = Modifier.size(RoomSpacing.MicStatusIconSize),
            )
        }
    }
}
