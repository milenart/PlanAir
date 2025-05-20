package pl.pw.planair.data

// Upewnij sie ze masz import do Twojego enuma EventCategory
import pl.pw.planair.data.EventCategory


// Definiuje możliwe kryteria filtrowania wydarzeń
sealed class FilterCriterion {
    data object All : FilterCriterion() // Pokaż wszystkie wydarzenia
    data class Category(val category: EventCategory) : FilterCriterion() // Filtruj według kategorii
    data object Favorites : FilterCriterion() // Pokaż tylko ulubione
}