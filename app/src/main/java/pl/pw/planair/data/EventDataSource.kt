// pl.pw.planair.data/EventDataSource.kt

package pl.pw.planair.data

import android.content.Context
import android.util.Log // Dodaj import dla Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Locale

// Pomocnicza klasa do bezpośredniego mapowania JSON-a
data class EventRawJsonData(
    val id: Int, // Ważne: dodaj id do rawData, jeśli jest w JSONie
    val date: String,
    val location: LocationData, // Bezpośrednio mapujemy na LocationData
    val title: String,
    val start_time: String,
    val description: String,
    val source_link: String? = null,
    val image_url: String? = null,
    val category: String,
    val price: String // "0" (jako String)
)

fun loadMarkersFromJson(context: Context, resourceId: Int): List<Event> {
    val gson = Gson()
    val inputStream = context.resources.openRawResource(resourceId)
    val reader = InputStreamReader(inputStream)

    return try {
        val listType: Type = object : TypeToken<List<EventRawJsonData>>() {}.type
        val rawDataList: List<EventRawJsonData> = gson.fromJson(reader, listType)

        Log.d("EventDataSource", "Załadowano ${rawDataList.size} surowych wydarzeń z JSON.") // Logowanie liczby załadowanych elementów

        rawDataList.map { rawData ->
            // Logowanie surowej kategorii przed parsowaniem
            Log.d("EventDataSource", "Parsowanie wydarzenia: '${rawData.title}', surowa kategoria: '${rawData.category}'")

            val parsedCategory = try {
                // Użyj .trim() aby usunąć białe znaki
                // Użyj .uppercase(Locale.getDefault()) aby normalizować wielkość liter
                when (rawData.category.trim().uppercase(Locale.getDefault())) {
                    "SPORT" -> EventCategory.SPORT
                    "KULTURA I ROZRYWKA" -> EventCategory.KULTURA
                    "EDUKACJA" -> EventCategory.EDUKACJA
                    "SPOTKANIA I INTEGRACJE" -> EventCategory.AKTYWNOSC_SPOLECZNA
                    else -> {
                        Log.w("EventDataSource", "Nieznana lub nieprawidłowa kategoria JSON '${rawData.category}' dla wydarzenia ${rawData.title}. Ustawiam na OTHER.")
                        EventCategory.OTHER
                    }
                }
            } catch (e: Exception) {
                Log.e("EventDataSource", "Błąd parsowania kategorii '${rawData.category}' dla wydarzenia ${rawData.title}: ${e.message}", e)
                EventCategory.OTHER
            }

            val parsedDateMillis = try {
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                dateFormat.parse(rawData.date)?.time
            } catch (e: Exception) {
                Log.e("EventDataSource", "Błąd parsowania daty '${rawData.date}': ${e.message}")
                null // lub domyślna wartość, jeśli chcesz
            }

            val parsedPrice = rawData.price.toDoubleOrNull()


            val event = Event(
                title = rawData.title,
                description = rawData.description,
                date = rawData.date, // Nadal String
                start_time = rawData.start_time,
                source_link = rawData.source_link,
                image_url = rawData.image_url,
                category = parsedCategory, // Używamy sparsowanej kategorii
                price = rawData.price, // Nadal String
                location = rawData.location // Przekazujemy cały zagnieżdżony obiekt lokalizacji
            )
            Log.d("EventDataSource", "Stworzono Event: ${event.title}, Kategoria: ${event.category}, Wspolrzedne: ${event.location?.coordinates}")
            event
        }
    } catch (e: Exception) {
        Log.e("EventDataSource", "Globalny błąd podczas ładowania markerów z JSON: ${e.message}", e)
        emptyList()
    } finally {
        reader.close()
        inputStream.close()
    }
}