package love.moonc.room.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import love.moonc.room.data.model.Room
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.RoomSpacing

enum class MainTab {
    Home,
    Me,
}

@Composable
fun MainScreen(
    appContainer: AppContainer,
    initialTab: MainTab = MainTab.Home,
    onCreateRoom: () -> Unit,
    onRoomClick: (Long) -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = roomViewModel(appContainer),
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.Home,
                    onClick = { tab = MainTab.Home },
                    label = { Text("首页") },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.Me,
                    onClick = { tab = MainTab.Me },
                    label = { Text("我的") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                )
            }
        },
    ) { padding ->
        if (tab == MainTab.Home) {
            HomeContent(
                modifier = Modifier.padding(padding),
                state = state,
                onCreateRoom = onCreateRoom,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMoreRooms,
                onJoinRoom = { roomId -> viewModel.joinRoom(roomId, onRoomClick) },
            )
        } else {
            MeContent(
                modifier = Modifier.padding(padding),
                state = state,
                onEditProfile = onEditProfile,
                onLogout = onLogout,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    modifier: Modifier,
    state: MainUiState,
    onCreateRoom: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onJoinRoom: (Long) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RoomSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onCreateRoom) {
                    Icon(Icons.Filled.Add, contentDescription = "创建")
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
            ) {
                if (state.rooms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.loading) "正在加载房间" else "暂无房间",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                } else {
                    items(state.rooms, key = { it.id }) { room ->
                        RoomItem(
                            room = room,
                            joining = state.joiningRoomId == room.id,
                            onJoinRoom = onJoinRoom,
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Button(
                                onClick = onLoadMore,
                                enabled = !state.loadingMore,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (state.loadingMore) "加载中" else "加载更多")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomItem(
    room: Room,
    joining: Boolean,
    onJoinRoom: (Long) -> Unit,
) {
    val isFull = room.currentMembers >= room.maxMembers
    val canJoin = !isFull && !joining
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canJoin) { onJoinRoom(room.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(RoomSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(RoomSpacing.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${room.currentMembers} / ${room.maxMembers} 人",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = when {
                    joining -> "进入中"
                    isFull -> "满员"
                    else -> "进入"
                },
                color = when {
                    isFull -> MaterialTheme.colorScheme.error
                    joining -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun MeContent(
    modifier: Modifier,
    state: MainUiState,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val user = state.user
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RoomSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RoomSpacing.ItemGap),
    ) {
        if (user == null) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
                contentAlignment = Alignment.Center,
            ) {
                Text("正在加载用户资料")
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditProfile() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RoomSpacing.ProfileCardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RoomSpacing.CompactGap),
                ) {
                    ProfileAvatar(avatarUrl = user.avatarUrl)
                    Text(user.nickname, style = MaterialTheme.typography.titleLarge)
                    Text(user.email)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
    }
}

@Composable
private fun ProfileAvatar(avatarUrl: String?) {
    Surface(
        modifier = Modifier
            .size(RoomSpacing.AvatarSize)
            .clip(CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = RoomSpacing.CompactGap / 4,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(RoomSpacing.ButtonLoadingSize),
                                strokeWidth = RoomSpacing.CompactGap / 4,
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "头像")
                        }
                    },
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = "头像")
            }
        }
    }
}
