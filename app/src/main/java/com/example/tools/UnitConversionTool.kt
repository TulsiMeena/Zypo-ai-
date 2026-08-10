package com.example.tools

import org.json.JSONArray
import org.json.JSONObject

class UnitConversionTool : Tool {
    override val name: String = "convertUnit"
    override val description: String = "Converts physical quantities between units (length, weight, temperature, speed, time, area, volume)."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("value", JSONObject().apply {
                    put("type", "NUMBER")
                    put("description", "Numerical value to convert")
                })
                put("fromUnit", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Source unit (e.g. 'km', 'miles', 'celsius', 'fahrenheit', 'kg', 'lbs', 'meters', 'feet')")
                })
                put("toUnit", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Target unit (e.g. 'miles', 'km', 'fahrenheit', 'celsius', 'lbs', 'kg', 'feet', 'meters')")
                })
            })
            put("required", JSONArray().apply {
                put("value")
                put("fromUnit")
                put("toUnit")
            })
        }
    }

    override suspend fun execute(args: JSONObject): JSONObject {
        val value = args.optDouble("value", Double.NaN)
        val fromUnit = args.optString("fromUnit", "").trim().lowercase()
        val toUnit = args.optString("toUnit", "").trim().lowercase()

        if (value.isNaN()) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Invalid or missing 'value'.")
            }
        }

        return try {
            val converted = performConversion(value, fromUnit, toUnit)
            JSONObject().apply {
                put("success", true)
                put("inputValue", value)
                put("fromUnit", fromUnit)
                put("toUnit", toUnit)
                put("convertedValue", converted)
                put("resultText", "$value $fromUnit = $converted $toUnit")
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", e.message ?: "Conversion failed")
            }
        }
    }

    private fun performConversion(valIn: Double, from: String, to: String): Double {
        // Temperature handling
        if (isTemp(from) && isTemp(to)) {
            val celsius = when (from) {
                "celsius", "c" -> valIn
                "fahrenheit", "f" -> (valIn - 32.0) * 5.0 / 9.0
                "kelvin", "k" -> valIn - 273.15
                else -> throw IllegalArgumentException("Unknown temp unit: $from")
            }
            return when (to) {
                "celsius", "c" -> celsius
                "fahrenheit", "f" -> (celsius * 9.0 / 5.0) + 32.0
                "kelvin", "k" -> celsius + 273.15
                else -> throw IllegalArgumentException("Unknown temp unit: $to")
            }
        }

        // Length (base: meter)
        val lengthBaseMap = mapOf(
            "meter" to 1.0, "meters" to 1.0, "m" to 1.0,
            "kilometer" to 1000.0, "kilometers" to 1000.0, "km" to 1000.0,
            "centimeter" to 0.01, "centimeters" to 0.01, "cm" to 0.01,
            "millimeter" to 0.001, "millimeters" to 0.001, "mm" to 0.001,
            "mile" to 1609.344, "miles" to 1609.344,
            "yard" to 0.9144, "yards" to 0.9144, "yd" to 0.9144,
            "foot" to 0.3048, "feet" to 0.3048, "ft" to 0.3048,
            "inch" to 0.0254, "inches" to 0.0254, "in" to 0.0254
        )

        // Weight (base: kg)
        val weightBaseMap = mapOf(
            "kilogram" to 1.0, "kilograms" to 1.0, "kg" to 1.0,
            "gram" to 0.001, "grams" to 0.001, "g" to 0.001,
            "milligram" to 0.000001, "mg" to 0.000001,
            "pound" to 0.45359237, "pounds" to 0.45359237, "lb" to 0.45359237, "lbs" to 0.45359237,
            "ounce" to 0.028349523125, "ounces" to 0.028349523125, "oz" to 0.028349523125
        )

        // Speed (base: m/s)
        val speedBaseMap = mapOf(
            "m/s" to 1.0, "mps" to 1.0,
            "km/h" to 0.277777778, "kph" to 0.277777778,
            "mph" to 0.44704, "miles/h" to 0.44704
        )

        // Time (base: second)
        val timeBaseMap = mapOf(
            "second" to 1.0, "seconds" to 1.0, "sec" to 1.0, "s" to 1.0,
            "minute" to 60.0, "minutes" to 60.0, "min" to 60.0,
            "hour" to 3600.0, "hours" to 3600.0, "hr" to 3600.0,
            "day" to 86400.0, "days" to 86400.0
        )

        val maps = listOf(lengthBaseMap, weightBaseMap, speedBaseMap, timeBaseMap)
        for (map in maps) {
            if (map.containsKey(from) && map.containsKey(to)) {
                val baseValue = valIn * map[from]!!
                return baseValue / map[to]!!
            }
        }

        throw IllegalArgumentException("Unsupported unit conversion from '$from' to '$to'")
    }

    private fun isTemp(unit: String): Boolean {
        return unit in listOf("celsius", "c", "fahrenheit", "f", "kelvin", "k")
    }
}
