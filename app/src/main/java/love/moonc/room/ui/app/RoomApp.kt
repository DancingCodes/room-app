package love.moonc.room.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun RoomApp(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = roomViewModel(appContainer),
    homeViewModel: HomeViewModel = roomViewModel(appContainer),
) {
    val appState by appViewModel.uiState.collectAsState()

    fun navigateMainTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.Home) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateHomeTab() {
        navigateMainTab(Routes.Home)
    }

    fun navigateMeTab() {
        navigateMainTab(Routes.Me)
    }


    LaunchedEffect(appState.user?.id) {
        val userId = appState.user?.id
        if (userId == null) {
            homeViewModel.clear()
        } else {
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
            composable(Routes.Home) {
                MainScreen(
                    selectedTab = MainTab.Home,
                    user = appState.user,
                    homeViewModel = homeViewModel,
                    onHomeTab = ::navigateHomeTab,
                    onMeTab = ::navigateMeTab,
                    onCreateRoom = { navController.navigate(Routes.CreateRoom) },
                    onRoomClick = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                    onEditProfile = { navController.navigate(Routes.EditProfile) },
                    onLogout = { appViewModel.logout() },
                )
            }
            composable(Routes.Me) {
                MainScreen(
                    selectedTab = MainTab.Me,
                    user = appState.user,
                    homeViewModel = homeViewModel,
                    onHomeTab = ::navigateHomeTab,
                    onMeTab = ::navigateMeTab,
                    onCreateRoom = { navController.navigate(Routes.CreateRoom) },
                    onRoomClick = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                    onEditProfile = { navController.navigate(Routes.EditProfile) },
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
                        navController.navigate(Routes.Home) { popUpTo(0) }
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
