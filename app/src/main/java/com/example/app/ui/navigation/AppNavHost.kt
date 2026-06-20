package com.example.odootask.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.odootask.feature.auth.LoginScreen
import com.example.odootask.feature.task_create.CreateTaskScreen
import com.example.odootask.feature.task_detail.TaskDetailScreen
import com.example.odootask.feature.tasks.TasksScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        SessionState.Loading -> LoadingScreen(modifier)
        else -> {
            val startDestination = if (sessionState == SessionState.LoggedIn) {
                Routes.TASKS
            } else {
                Routes.LOGIN
            }
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier,
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onNavigateToTasks = {
                            navController.navigate(Routes.TASKS) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.TASKS) {
                    TasksScreen(
                        onNavigateToTaskDetail = { taskId ->
                            navController.navigate(Routes.taskDetail(taskId))
                        },
                        onNavigateToCreateTask = {
                            navController.navigate(Routes.CREATE_TASK)
                        },
                        onNavigateToLogin = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.TASKS) { inclusive = true }
                            }
                        },
                    )
                }

                composable(
                    route = Routes.TASK_DETAIL,
                    arguments = listOf(navArgument("taskId") { type = NavType.IntType }),
                ) {
                    TaskDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.CREATE_TASK) {
                    CreateTaskScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
