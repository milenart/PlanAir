package pl.pw.planair.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.lang.reflect.Type

fun loadMarkersFromJson(context: Context, resourceId: Int): List<Event> {
    val gson = Gson()
    val inputStream = context.resources.openRawResource(resourceId)
    val reader = InputStreamReader(inputStream)

    return try {
        val listType: Type = object : TypeToken<List<Event>>() {}.type
        val jsonDataList: List<Event> = gson.fromJson(reader, listType)
        jsonDataList.map { jsonData ->
            Event(
                id = jsonData.id,
                lat = jsonData.lat,
                lon = jsonData.lon,
                title = jsonData.title,
                category = jsonData.category,
                description = jsonData.description
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    } finally {
        reader.close()
        inputStream.close()
    }
}

