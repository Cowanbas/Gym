package com.cowanbas.gym.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object Store {
    private const val PREFS = "gym_store"
    private const val KEY_ROUTINES = "routines"
    private const val KEY_HISTORY = "history"
    private const val KEY_TEMPLATES = "templates"
    private const val KEY_ACTIVE_TEMPLATE = "active_template"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun loadRoutines(ctx: Context): Map<String, Routine>? = withContext(Dispatchers.IO) {
        val raw = prefs(ctx).getString(KEY_ROUTINES, null) ?: return@withContext null
        runCatching {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { k -> put(k, Routine.fromJson(obj.getJSONObject(k))) } }
        }.getOrNull()
    }

    suspend fun loadTemplates(ctx: Context): List<WorkoutTemplate>? = withContext(Dispatchers.IO) {
        val raw = prefs(ctx).getString(KEY_TEMPLATES, null) ?: return@withContext null
        runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { WorkoutTemplate.fromJson(arr.getJSONObject(it)) }.ifEmpty { null }
        }.getOrNull()
    }

    suspend fun loadActiveTemplateName(ctx: Context): String = withContext(Dispatchers.IO) {
        prefs(ctx).getString(KEY_ACTIVE_TEMPLATE, "Template A") ?: "Template A"
    }

    suspend fun loadHistory(ctx: Context): Map<String, WorkoutHistory> = withContext(Dispatchers.IO) {
        val raw = prefs(ctx).getString(KEY_HISTORY, null) ?: return@withContext emptyMap()
        runCatching {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { k -> put(k, WorkoutHistory.fromJson(obj.getJSONObject(k))) } }
        }.getOrDefault(emptyMap())
    }

    suspend fun saveRoutines(ctx: Context, routines: Map<String, Routine>) = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        routines.forEach { (k, v) -> obj.put(k, v.toJson()) }
        prefs(ctx).edit { putString(KEY_ROUTINES, obj.toString()) }
    }

    suspend fun saveTemplates(ctx: Context, templates: List<WorkoutTemplate>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        templates.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit { putString(KEY_TEMPLATES, arr.toString()) }
    }

    suspend fun saveActiveTemplateName(ctx: Context, name: String) = withContext(Dispatchers.IO) {
        prefs(ctx).edit { putString(KEY_ACTIVE_TEMPLATE, name) }
    }

    suspend fun saveHistory(ctx: Context, history: Map<String, WorkoutHistory>) = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        history.forEach { (k, v) -> obj.put(k, v.toJson()) }
        prefs(ctx).edit { putString(KEY_HISTORY, obj.toString()) }
    }

    fun defaultRoutines(): Map<String, Routine> = mapOf(
        "monday" to Routine("Empty", emptyList()),
        "tuesday" to Routine("Empty", emptyList()),
        "wednesday" to Routine("Empty", emptyList()),
        "thursday" to Routine("Empty", emptyList()),
        "friday" to Routine("Empty", emptyList()),
        "saturday" to Routine("Empty", emptyList()),
        "sunday" to Routine("Empty", emptyList())
    )

    fun defaultTemplates(): List<WorkoutTemplate> = listOf(
        WorkoutTemplate("Template A", defaultRoutines()),
        WorkoutTemplate("Template B", defaultRoutines())
    )
}