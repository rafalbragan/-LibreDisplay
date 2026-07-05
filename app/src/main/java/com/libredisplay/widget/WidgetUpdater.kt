package com.libredisplay.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.libredisplay.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WidgetUpdater(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun updateWithReading(value: Int, unit: String, trend: String, timestamp: Instant) {
        prefs.edit()
            .putInt(KEY_VALUE, value)
            .putString(KEY_UNIT, unit)
            .putString(KEY_TREND, trend)
            .putString(KEY_LAST_UPDATE, formatTime(timestamp))
            .putString(KEY_ERROR, "")
            .apply()
        pushUpdate()
    }

    fun updateWithError(message: String) {
        prefs.edit()
            .putString(KEY_ERROR, message)
            .apply()
        pushUpdate()
    }

    fun clear() {
        prefs.edit().clear().apply()
        pushUpdate()
    }

    fun buildRemoteViews(): RemoteViews {
        val value = prefs.getInt(KEY_VALUE, -1)
        val unit = prefs.getString(KEY_UNIT, "mg/dL").orEmpty()
        val trend = prefs.getString(KEY_TREND, "?").orEmpty()
        val lastUpdate = prefs.getString(KEY_LAST_UPDATE, "-").orEmpty()
        val error = prefs.getString(KEY_ERROR, "").orEmpty()

        return RemoteViews(context.packageName, R.layout.widget_glucose).apply {
            setTextViewText(R.id.widget_value, if (value >= 0) value.toString() else "--")
            setTextViewText(R.id.widget_unit, unit)
            setTextViewText(R.id.widget_trend, trend)
            setTextViewText(R.id.widget_time, context.getString(R.string.widget_last_update, lastUpdate))
            setTextViewText(R.id.widget_error, error)
        }
    }

    fun pushUpdate() {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, LibreDisplayWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val views = buildRemoteViews()
            manager.updateAppWidget(ids, views)
        }
    }

    private fun formatTime(timestamp: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return formatter.format(timestamp.atZone(ZoneId.systemDefault()))
    }

    companion object {
        private const val PREFS_NAME = "widget_glucose_cache"
        private const val KEY_VALUE = "value"
        private const val KEY_UNIT = "unit"
        private const val KEY_TREND = "trend"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_ERROR = "error"
    }
}

