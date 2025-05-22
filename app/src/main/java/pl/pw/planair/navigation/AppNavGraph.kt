package pl.pw.planair.navigation

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Dodaj ten import
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pl.pw.planair.data.EventCategory
import pl.pw.planair.data.FilterState
import pl.pw.planair.ui.filter.FilterViewModel
import pl.pw.planair.ui.map.viewmodel.MapViewModel
import pl.pw.planair.ui.screens.FilterScreen
import pl.pw.planair.ui.screens.IntroScreen
import pl.pw.planair.ui.screens.MainScreenWithMapAndList
import pl.pw.planair.ui.screens.SplashScreen
/**
 * Komponent Composable konfigurujący NavHost i definiujący graf nawigacji aplikacji.
 * Przyjmuje kontroler nawigacji i konfiguruje wszystkie ekrany.
 */
@Composable
fun AppNavHost(
    navController: NavHostController
) {
    // --- Zadeklaruj MapViewModel tylko raz, na najwyższym poziomie NavGraph ---
    val mapViewModel: MapViewModel = viewModel()

    // KLUCZOWA ZMIANA: Zbieramy filterState z MapViewModel jako State<FilterState>
    // To zapewnia, że jego wartość jest bezpiecznie obserwowana w kompozycji.
    val mapFilterState by mapViewModel.filterState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppScreens.SplashScreen.route
    ) {
        composable(route = AppScreens.SplashScreen.route) {
            SplashScreen(navController = navController) // Wywołaj swój SplashScreen Composable
        }
        composable(AppScreens.IntroScreen.route) {
            IntroScreen(
                onNavigateToMap = { filterCategoryString ->
                    val initialCategory = filterCategoryString?.let {
                        try {
                            EventCategory.valueOf(it)
                        } catch (e: IllegalArgumentException) {
                            Log.e("AppNavGraph", "Nieznana kategoria z IntroScreen: $it")
                            EventCategory.OTHER
                        }
                    }

                    // Używamy zebranej wartości mapFilterState, która jest już bezpieczna w kompozycji
                    mapViewModel.applyFilter(
                        mapFilterState.copy(category = initialCategory)
                    )
                    navController.navigate(AppScreens.MapScreen.route)
                }
            )
        }

        composable(AppScreens.MapScreen.route) {
            MainScreenWithMapAndList(
                mapViewModel = mapViewModel,
                onNavigateToFilter = { initialCategoryString ->
                    navController.navigate(AppScreens.FilterScreen.createRoute(initialCategoryString))
                }
            )
        }

        composable(
            route = "${AppScreens.FilterScreen.route}/{${AppScreens.FilterScreen.INITIAL_CATEGORY_ARG}}",
            arguments = listOf(
                navArgument(AppScreens.FilterScreen.INITIAL_CATEGORY_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val applicationContext = LocalContext.current.applicationContext as Application

            // Inicjalizuj FilterViewModel za pomocą ViewModelProvider.Factory
            // Używamy tutaj zebranego mapFilterState jako stanu początkowego
            val filterViewModel: FilterViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(FilterViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return FilterViewModel(
                                application = applicationContext,
                                initialFilterStateFromNav = mapFilterState // Używamy zebranego stanu
                            ) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                }
            )

            val initialCategoryFromRoute = backStackEntry.arguments?.getString(AppScreens.FilterScreen.INITIAL_CATEGORY_ARG)

            LaunchedEffect(initialCategoryFromRoute) {
                initialCategoryFromRoute?.let { categoryString ->
                    try {
                        val category = EventCategory.valueOf(categoryString)
                        if (filterViewModel.filterState.value.category != category) {
                            filterViewModel.updateCategory(category)
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.e("AppNavGraph", "Nieznana kategoria z argumentu trasy do FilterScreen: $categoryString")
                    }
                }
            }

            FilterScreen(
                filterViewModel = filterViewModel,
                onApplyFilters = { filterState ->
                    mapViewModel.applyFilter(filterState)
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}