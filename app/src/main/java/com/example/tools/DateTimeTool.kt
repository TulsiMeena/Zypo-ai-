package com.example.tools

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateTimeTool : Tool {
    override val name: String = "getCurrentDateTime"
    override val description: String = "Gets the current real date, time, day of week, and time zone from the system."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject())
        }
    }

    override suspend fun execute(args: JSONObject): JSONObject {
        return try {
            val now = Date()
            val timeZone = TimeZone.getDefault()

            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).apply { this.timeZone = timeZone }
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US).apply { this.timeZone = timeZone }
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { this.timeZone = timeZone }

            JSONObject().apply {
                put("success", true)
                put("date", dateFormat.format(now))
                put("time", timeFormat.format(now))
                put("timezone", timeZone.displayName)
                put("timeZoneId", timeZone.id)
                put("isoTimestamp", isoFormat.format(now))
                put("formatted", "${dateFormat.format(now)} ${timeFormat.format(now)} (${timeZone.id})")
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", "Failed to retrieve date/time: ${e.localizedMessage}")
            }
        }
    }
}
