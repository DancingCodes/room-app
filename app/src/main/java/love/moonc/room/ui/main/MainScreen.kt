package love.moonc.room.ui.main

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.moonc.room.di.AppContainer
import love.moonc.room.data.model.Room
import love.moonc.room.ui.app.roomViewModel
import love.moonc.room.ui.components.ErrorText

enum class MainTab {
    Home,
    Me,
}

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text(if (tab == MainTab.Home) "首页" else "我的") },
                actions = {
                    if (tab == MainTab.Home) {
                        TextButton(onClick = onCreateRoom) { Text("创建") }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.Home,
                    onClick = { tab = MainTab.Home },
                    label = { Text("首页") },
                    icon = {},
                )
                NavigationBarItem(
                    selected = tab == MainTab.Me,
                    onClick = { tab = MainTab.Me },
                    label = { Text("我的") },
                    icon = {},
                )
            }
        },
    ) { padding ->
        if (tab == MainTab.Home) {
            HomeContent(
                modifier = Modifier.padding(padding),
                state = state,
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

@Composable
private fun HomeContent(
    modifier: Modifier,
    state: MainUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onJoinRoom: (Long) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh, enabled = !state.loading) { Text("刷新") }
        }
        ErrorText(state.message)
        if (state.rooms.isEmpty()) {
            Text("暂无房间", modifier = Modifier.padding(top = 32.dp))
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(state.rooms, key = { it.id }) { room ->
                RoomItem(room = room, onJoinRoom = onJoinRoom)
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

@Composable
private fun RoomItem(room: Room, onJoinRoom: (Long) -> Unit) {
    val isFull = room.currentMembers >= room.maxMembers
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isFull) { onJoinRoom(room.id) },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleMedium)
            Text("${room.currentMembers} / ${room.maxMembers} 人")
            Text(room.createdAt)
            Text(if (isFull) "满员" else "进入")
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
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (user == null) {
            Text("正在加载用户资料")
        } else {
            Text("账号  ${user.account}")
            Text("邮箱  ${user.email}")
            Text("昵称  ${user.nickname}")
        }
        Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) { Text("编辑资料") }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
        ErrorText(state.message)
    }
}
