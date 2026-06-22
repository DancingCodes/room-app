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
import love.moonc.room.data.model.User
import love.moonc.room.ui.auth.LoginScreen
import love.moonc.room.ui.auth.RegisterScreen
import love.moonc.room.ui.auth.ResetPasswordScreen
import love.moonc.room.ui.main.MainTab
import love.moonc.room.ui.main.MainScreen
import love.moonc.room.ui.profile.EditProfileScreen
import love.moonc.room.ui.room.CreateRoomScreen
import love.moonc.room.ui.room.RoomDetailScreen

@Composable
fun RoomApp(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = roomViewModel(appContainer),
) {
    val appState by appViewModel.uiState.collectAsState()

    LaunchedEffect(appState.targetRoute) {
        val target = appState.targetRoute ?: return@LaunchedEffect
        navController.navigate(target) {
            popUpTo(0)
            launchSingleTop = true
        }
        appViewModel.clearNavigationTarget()
    }

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
                onRegisterClick = { navController.navigate(Routes.Register) },
                onResetPasswordClick = { navController.navigate(Routes.ResetPassword) },
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onRegisterSuccess = { user -> appViewModel.setUser(user) },
            )
        }
        composable(Routes.ResetPassword) {
            ResetPasswordScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onResetSuccess = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Login) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.Main) {
            MainScreen(
                appContainer = appContainer,
                onCreateRoom = { navController.navigate(Routes.CreateRoom) },
                onRoomClick = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                onEditProfile = { navController.navigate(Routes.EditProfile) },
                onLogout = { appViewModel.logout() },
            )
        }
        composable(Routes.Home) {
            MainScreen(
                appContainer = appContainer,
                onCreateRoom = { navController.navigate(Routes.CreateRoom) },
                onRoomClick = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                onEditProfile = { navController.navigate(Routes.EditProfile) },
                onLogout = { appViewModel.logout() },
            )
        }
        composable(Routes.Me) {
            MainScreen(
                appContainer = appContainer,
                initialTab = MainTab.Me,
                onCreateRoom = { navController.navigate(Routes.CreateRoom) },
                onRoomClick = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                onEditProfile = { navController.navigate(Routes.EditProfile) },
                onLogout = { appViewModel.logout() },
            )
        }
        composable(Routes.EditProfile) {
            EditProfileScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onSaved = { user: User -> appViewModel.setUser(user) },
            )
        }
        composable(Routes.CreateRoom) {
            CreateRoomScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onCreated = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
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
                onLeft = { navController.navigate(Routes.Home) { popUpTo(0) } },
            )
        }
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
