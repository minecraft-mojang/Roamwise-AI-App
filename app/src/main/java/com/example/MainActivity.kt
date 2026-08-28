package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.ApiKeyManager
import com.example.ui.screens.TripBuilderScreen
import com.example.ui.screens.TripDetailScreen
import com.example.ui.screens.TripListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TripBuilderViewModel
import com.example.ui.viewmodel.TripDetailViewModel
import com.example.ui.viewmodel.TripDetailViewModelFactory
import com.example.ui.viewmodel.TripListViewModel

object AppRoutes {
    const val TRIP_LIST = "trip_list"
    const val TRIP_BUILDER = "trip_builder"
    const val TRIP_DETAIL = "trip_detail/{tripId}"

    fun tripDetail(tripId: Long) = "trip_detail/$tripId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiKeyManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ItineraryAppNavigation()
                }
            }
        }
    }
}

@Composable
fun ItineraryAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Shared Builder ViewModel across flow
    val builderViewModel: TripBuilderViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.TRIP_LIST
    ) {
        composable(AppRoutes.TRIP_LIST) {
            val listViewModel: TripListViewModel = viewModel()
            TripListScreen(
                viewModel = listViewModel,
                onTripClick = { tripId ->
                    navController.navigate(AppRoutes.tripDetail(tripId))
                },
                onPlanNewTripClick = {
                    navController.navigate(AppRoutes.TRIP_BUILDER)
                },
                onSelectPreset = { dest, country, budget, duration, interests ->
                    builderViewModel.selectPresetDestination(dest, country, budget, duration, interests)
                    navController.navigate(AppRoutes.TRIP_BUILDER)
                }
            )
        }

        composable(AppRoutes.TRIP_BUILDER) {
            TripBuilderScreen(
                viewModel = builderViewModel,
                onBack = { navController.popBackStack() },
                onTripGenerated = { tripId ->
                    navController.navigate(AppRoutes.tripDetail(tripId)) {
                        popUpTo(AppRoutes.TRIP_LIST)
                    }
                }
            )
        }

        composable(
            route = AppRoutes.TRIP_DETAIL,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
            val detailViewModel: TripDetailViewModel = viewModel(
                factory = TripDetailViewModelFactory(application, tripId)
            )

            TripDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
