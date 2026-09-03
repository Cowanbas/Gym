package com.cowanbas.gym.data

import androidx.compose.runtime.Stable
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Stable
data class ExerciseSet(
    val id: String = System.nanoTime().toString(),
    val reps: Int = 0,
    val weight: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("reps", reps); put("weight", weight)
    }

    companion object {
        fun fromJson(json: JSONObject) = ExerciseSet(
            id = json.optString("id", System.nanoTime().toString()),
            reps = json.optInt("reps", 0),
            weight = json.optDouble("weight", 0.0)
        )
    }
}

@Stable
data class Exercise(
    val id: String = System.nanoTime().toString(),
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val advancedSets: List<ExerciseSet> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("sets", sets); put("reps", reps); put("weight", weight)
        put("advancedSets", JSONArray().also { arr -> advancedSets.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): Exercise {
            val arr = json.optJSONArray("advancedSets")
            val adv = if (arr != null) List(arr.length()) { ExerciseSet.fromJson(arr.getJSONObject(it)) } else emptyList()
            return Exercise(
                id = json.optString("id", System.nanoTime().toString()),
                name = json.optString("name", ""),
                sets = json.optInt("sets", 0),
                reps = json.optInt("reps", 0),
                weight = json.optDouble("weight", 0.0),
                advancedSets = adv
            )
        }
    }
}

@Stable
data class Routine(
    val title: String = "Workout",
    val exercises: List<Exercise> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("exercises", JSONArray().also { arr -> exercises.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): Routine {
            val arr = json.optJSONArray("exercises")
            val list = if (arr != null) List(arr.length()) { Exercise.fromJson(arr.getJSONObject(it)) } else emptyList()
            return Routine(json.optString("title", "Workout"), list)
        }
    }
}

@Stable
data class WorkoutTemplate(
    val name: String,
    val routines: Map<String, Routine>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        val obj = JSONObject()
        routines.forEach { (k, v) -> obj.put(k, v.toJson()) }
        put("routines", obj)
    }

    companion object {
        fun fromJson(json: JSONObject): WorkoutTemplate {
            val name = json.optString("name", "Template A")
            val routinesObj = json.optJSONObject("routines") ?: JSONObject()
            val map = buildMap {
                routinesObj.keys().forEach { k -> put(k, Routine.fromJson(routinesObj.getJSONObject(k))) }
            }
            return WorkoutTemplate(name, map.ifEmpty { Store.defaultRoutines() })
        }
    }
}

@Stable
data class WorkoutHistory(
    val date: String,
    val routineKey: String = "",
    val routineTitle: String = "Workout",
    val exercises: List<Exercise> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("routineKey", routineKey)
        put("routineTitle", routineTitle)
        put("exercises", JSONArray().also { arr -> exercises.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): WorkoutHistory {
            val arr = json.optJSONArray("exercises")
            val list = if (arr != null) List(arr.length()) { Exercise.fromJson(arr.getJSONObject(it)) } else emptyList()
            return WorkoutHistory(
                date = json.optString("date", ""),
                routineKey = json.optString("routineKey", ""),
                routineTitle = json.optString("routineTitle", "Workout"),
                exercises = list
            )
        }
    }
}

val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val fmt: (LocalDate) -> String = { date -> date.format(DATE_FMT) }

val DAY_KEYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

@Stable
data class WeekDay(val key: String, val name: String, val short: String)

val WEEK_DAYS = listOf(
    WeekDay("monday", "Monday", "Mon"),
    WeekDay("tuesday", "Tuesday", "Tue"),
    WeekDay("wednesday", "Wednesday", "Wed"),
    WeekDay("thursday", "Thursday", "Thu"),
    WeekDay("friday", "Friday", "Fri"),
    WeekDay("saturday", "Saturday", "Sat"),
    WeekDay("sunday", "Sunday", "Sun")
)

fun routineKeyForDate(date: LocalDate): String = DAY_KEYS[date.dayOfWeek.value - 1]