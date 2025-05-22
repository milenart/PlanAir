package pl.pw.planair.navigation

/**
 * Definiuje trasy (routes) dla wszystkich ekranów w aplikacji.
 * Używa sealed class dla bezpieczeństwa typów i unikalności tras.
 */
sealed class AppScreens(val route: String) {

    // NOWY OBIEKT: Reprezentuje trasę do Twojego własnego ekranu powitalnego (tego z logo i opóźnieniem)
    object SplashScreen : AppScreens("splash_screen") // <--- DODANY EKRAN

    // Obiekt reprezentujący trasę do Ekranu Powitalnego (tego z wyborem kategorii)
    object IntroScreen : AppScreens("intro_screen")

    // Obiekt reprezentujący trasę do Ekranu Mapy i Listy.
    object MapScreen : AppScreens("map_screen")
    // Nie ma już FILTER_ARG ani createRoute(filter: String?) tutaj, bo to było w MapScreen, a teraz jest w FilterScreen

    // Nowy obiekt dla ekranu filtra
    object FilterScreen : AppScreens("filter_screen") {
        const val INITIAL_CATEGORY_ARG = "initialCategory"

        fun createRoute(initialCategory: String?): String {
            return if (initialCategory == null) {
                // Jeśli nie ma initialCategory, nawigujemy do samej ścieżki filter_screen
                // W NavHost argument będzie wtedy null (dzięki nullable = true, defaultValue = null)
                FilterScreen.route
            } else {
                // Jeśli jest initialCategory, dodajemy ją do ścieżki
                "${FilterScreen.route}/$initialCategory"
            }
        }
    }

    // TODO: Dodaj tutaj obiekty dla innych ekranów, jeśli będziesz je tworzyć
    // object EventDetailsScreen : AppScreens...
}