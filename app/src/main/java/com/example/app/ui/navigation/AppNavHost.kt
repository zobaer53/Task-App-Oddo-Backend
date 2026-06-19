package com.example.odootask.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.odootask.feature.items.ItemsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ITEMS,
        modifier = modifier,
    ) {
        composable(Routes.ITEMS) {
            ItemsScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(Routes.itemDetail(id))
                }
            )
        }

        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            // ItemDetailScreen(id = id, onBack = { navController.popBackStack() })
        }
    }
}
