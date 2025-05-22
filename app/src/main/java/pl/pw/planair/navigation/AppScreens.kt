package pl.pw.planair.navigation

/**
 * Definiuje trasy (routes) dla wszystkich ekranów w aplikacji.
 * Używa sealed class dla bezpieczeństwa typów i unikalności tras.
 */
sealed class AppScreens(val route: String) {

    // Obiekt reprezentujący trasę do Ekranu Powitalnego
    object IntroScreen : AppScreens("intro_screen")

    // Obiekt reprezentujący trasę do Ekranu Mapy i Listy.
    // Usunięto argument filtra, bo MapViewModel będzie nim zarządzał.
    object MapScreen : AppScreens("map_screen") {
        // Nie ma już FILTER_ARG ani createRoute(filter: String?) tutaj
    }

    // Nowy obiekt dla ekranu filtra
    object FilterScreen : AppScreens("filter_screen") {
        const val INITIAL_CATEGORY_ARG = "initialCategory"

        fun createRoute(initialCategory: String?): String {
            return if (initialCategory == null) {
                FilterScreen.route // "filter_screen"
            } else {
                "${FilterScreen.route}/$initialCategory" // "filter_screen/SPORT"
            }
        }
    }

    // TODO: Dodaj tutaj obiekty dla innych ekranów, jeśli będziesz je tworzyć
    // object EventDetailsScreen : AppScreens...
}