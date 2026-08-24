package love.moonc.room.ui.app

// 连接更新检查、登录状态和页面导航，是整个 Compose 界面的入口；删除后应用没有可展示的页面结构。

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collect
import love.moonc.room.core.network.SessionExpiredCenter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import love.moonc.room.di.AppContainer
import love.moonc.room.ui.auth.LoginScreen
import love.moonc.room.ui.main.HomeViewModel
import love.moonc.room.ui.main.MainScreen
import love.moonc.room.ui.main.MainTab
import love.moonc.room.ui.message.CenterMessageHost
import love.moonc.room.ui.profile.EditProfileScreen
import love.moonc.room.ui.room.CreateRoomScreen
import love.moonc.room.ui.room.RoomDetailScreen
import love.moonc.room.ui.update.ForceUpdateScreen
import love.moonc.room.ui.update.UpdateViewModel
import love.moonc.room.ui.update.launchPackageInstaller

@Composable
fun RoomApp(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    updateViewModel: UpdateViewModel = roomViewModel(appContainer),
) {
    val updateState by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (updateState.checking) {
        SplashScreen()
        return
    }

    if (updateState.update != null || updateState.error != null) {
        ForceUpdateScreen(
            state = updateState,
            onRetryCheck = updateViewModel::checkForUpdate,
            onDownload = updateViewModel::download,
            onInstall = { apkFile -> launchPackageInstaller(context, apkFile) },
        )
        return
    }

    RoomContent(
        appContainer = appContainer,
        navController = navController,
    )
}

@Composable
private fun RoomContent(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = roomViewModel(appContainer),
    homeViewModel: HomeViewModel = roomViewModel(appContainer),
) {
    val appState by appViewModel.uiState.collectAsState()
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.Home.name) }
    val selectedTab = MainTab.valueOf(selectedTabName)


    LaunchedEffect(appState.user?.id) {
        val userId = appState.user?.id
        if (userId == null) {
            homeViewModel.clear()
        } else {
            SessionExpiredCenter.reset()
            homeViewModel.loadForUser(userId)
        }
    }

    LaunchedEffect(appState.targetRoute) {
        val target = appState.targetRoute ?: return@LaunchedEffect
        navController.navigate(target) {
            popUpTo(0)
            launchSingleTop = true
        }
        appViewModel.clearNavigationTarget()
    }

    LaunchedEffect(SessionExpiredCenter) {
        SessionExpiredCenter.events.collect {
            appContainer.messageCenter.show("登录已过期，请重新登录")
            appViewModel.logout()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
        ) {
            composable(Routes.Splash) {
                SplashScreen()
            }
            composable(Routes.Login) {
                LoginScreen(
                    appContainer = appContainer,
                    onLoginSuccess = { user -> appViewModel.setUser(user) },
                )
            }
            composable(Routes.Main) {
                MainScreen(
                    selectedTab = selectedTab,
                    user = appState.user,
                    homeViewModel = homeViewModel,
                    onHomeTab = { selectedTabName = MainTab.Home.name },
                    onMeTab = { selectedTabName = MainTab.Me.name },
                    onCreateRoom = { navController.navigate(Routes.CreateRoom) },
                    onRoomClick = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                    onEditProfile = {
                        selectedTabName = MainTab.Me.name
                        navController.navigate(Routes.EditProfile)
                    },
                    onLogout = { appViewModel.logout() },
                )
            }
            composable(Routes.EditProfile) {
                EditProfileScreen(
                    appContainer = appContainer,
                    user = appState.user,
                    onSaved = { user ->
                        appViewModel.updateUser(user)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.CreateRoom) {
                CreateRoomScreen(
                    appContainer = appContainer,
                    onCreated = { roomId ->
                        navController.navigate(Routes.roomDetail(roomId)) {
                            popUpTo(Routes.CreateRoom) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Routes.RoomDetail,
                arguments = listOf(navArgument("roomId") { type = NavType.LongType }),
            ) { entry ->
                val roomId = entry.arguments?.getLong("roomId") ?: return@composable
                RoomDetailScreen(
                    appContainer = appContainer,
                    roomId = roomId,
                    onLeft = { message ->
                        appContainer.messageCenter.show(message)
                        homeViewModel.refresh()
                        selectedTabName = MainTab.Home.name
                        navController.navigate(Routes.Main) { popUpTo(0) }
                    },
                    onUnauthorized = {
                        appContainer.messageCenter.show("登录已过期，请重新登录")
                        appViewModel.logout()
                    },
                    currentUserId = appState.user?.id,
                )
            }
        }
        CenterMessageHost(messageCenter = appContainer.messageCenter)
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
