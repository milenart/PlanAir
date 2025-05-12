package pl.pw.planair.navigation // Upewnij sie, ze nazwa pakietu jest poprawna

import androidx.compose.runtime.Composable // Potrzebne dla @Composable
import androidx.compose.runtime.LaunchedEffect // Potrzebne do wywolania applyFilter po nawigacji
import androidx.lifecycle.viewmodel.compose.viewModel // Potrzebne do uzyskania ViewModelu w Composable
import androidx.navigation.NavHostController // Potrzebne do kontrolera nawigacji
import androidx.navigation.NavType // Potrzebne do definiowania typu argumentu nawigacji
import androidx.navigation.compose.NavHost // Glowny komponent NavHost
import androidx.navigation.compose.composable // Funkcja do definiowania ekranow w NavHost
import androidx.navigation.navArgument // Funkcja do definiowania argumentow nawigacji

// Import Twoich ekranów Composable
import pl.pw.planair.ui.screen.IntroScreen // Upewnij sie, ze pakiet jest poprawny
import pl.pw.planair.ui.screen.MainScreenWithMapAndList // Upewnij sie, ze pakiet jest poprawny

// Import Twojego ViewModelu
import pl.pw.planair.ui.map.viewmodel.MapViewModel


/**
 * Komponent Composable konfigurujący NavHost i definiujący graf nawigacji aplikacji.
 * Przyjmuje kontroler nawigacji i konfiguruje wszystkie ekrany.
 */
@Composable
fun AppNavHost(
    navController: NavHostController, // Kontroler nawigacji dostarczony przez Activity
    startDestination: String = AppScreens.IntroScreen.route // Pierwszy ekran po uruchomieniu aplikacji
) {
    NavHost(
        navController = navController, // Podlacz kontroler
        startDestination = startDestination // Ustaw ekran startowy
    ) {
        // Definicja ekranu powitalnego
        composable(route = AppScreens.IntroScreen.route) {
            // Wywolaj komponent IntroScreen
            IntroScreen(
                onNavigateToMap = { filter ->
                    // Logika nawigacji po kliknieciu boxa na ekranie powitalnym
                    // Budujemy trase z argumentem filtru i nawigujemy
                    navController.navigate("${AppScreens.MapScreen.route}/${filter ?: "null"}") {
                        // Opcjonalnie dodaj opcje nawigacji, np. zeby nie wracac do intro po przejsciu dalej
                        popUpTo(AppScreens.IntroScreen.route) {
                            inclusive = true // Usun IntroScreen ze stosu wstecznego
                        }
                    }
                }
            )
        }

        // Definicja ekranu mapy i listy
        // Okreslamy trase z argumentem "filter"
        composable(
            route = "${AppScreens.MapScreen.route}/{${AppScreens.MapScreen.FILTER_ARG}}", // Pelna trasa z argumentem
            arguments = listOf( // Definiujemy argumenty oczekiwane przez trase
                navArgument(AppScreens.MapScreen.FILTER_ARG) { // Nazwa argumentu
                    type = NavType.StringType // Typ danych argumentu (tutaj String)
                    nullable = true // Argument moze byc null (dla opcji "Wszystko")
                }
            )
        ) { backStackEntry ->
            // Tutaj uzyskujemy instancje ViewModelu, ktora bedzie "przypisana" do tego ekranu
            val mapViewModel: MapViewModel = viewModel()

            // Pobierz argument filtru z backStackEntry
            val filter = backStackEntry.arguments?.getString(AppScreens.MapScreen.FILTER_ARG)

            // Uzyj LaunchedEffect, zeby wywolac applyFilter TYLKO GDY FILTR SIE ZMIENI
            // To zapewnia, ze filtr zostanie zastosowany po nawigacji i gdy argument filtra sie zmieni
            LaunchedEffect(filter) {
                // Przekaz pobrany filtr (String? z nawigacji) do ViewModelu
                mapViewModel.applyFilter(filter)
            }

            // Wywolaj komponent ekranu mapy i listy, przekazujac mu instancje ViewModelu
            MainScreenWithMapAndList(mapViewModel = mapViewModel)
        }

        // TODO: Dodaj definicje dla innych ekranów tutaj (np. ekranu szczegółów wydarzenia)
    }
}