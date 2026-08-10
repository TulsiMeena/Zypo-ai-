package com.example.tools

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class CalculatorTool : Tool {
    override val name: String = "calculator"
    override val description: String = "Evaluates mathematical expressions, arithmetic, percentages, powers, square roots, and trigonometric calculations accurately."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("expression", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Mathematical expression to evaluate (e.g. '25 * 48', 'sqrt(144)', '15% of 800')")
                })
            })
            put("required", JSONArray().apply { put("expression") })
        }
    }

    override suspend fun execute(args: JSONObject): JSONObject {
        val rawExpression = args.optString("expression", "").trim()
        if (rawExpression.isBlank()) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Expression cannot be empty.")
            }
        }

        return try {
            val normalized = normalizeExpression(rawExpression)
            val result = ExpressionParser(normalized).parse()

            JSONObject().apply {
                put("success", true)
                put("expression", rawExpression)
                put("result", result)
                put("formattedResult", if (result % 1.0 == 0.0) result.toLong().toString() else result.toString())
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("expression", rawExpression)
                put("error", "Calculation error: ${e.message ?: "Invalid expression"}")
            }
        }
    }

    private fun normalizeExpression(expr: String): String {
        var clean = expr.lowercase()
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "3.141592653589793")
            .replace("pi", "3.141592653589793")

        // Convert "X% of Y" to "(Y * (X / 100))"
        val percentOfRegex = Regex("([0-9.]+)\\s*%\\s*of\\s*([0-9.]+)")
        clean = percentOfRegex.replace(clean) { match ->
            val pct = match.groupValues[1]
            val base = match.groupValues[2]
            "($base * ($pct / 100))"
        }

        // Convert standalone percentage "X%" to "(X / 100)"
        val standalonePercentRegex = Regex("([0-9.]+)\\s*%")
        clean = standalonePercentRegex.replace(clean) { match ->
            val pct = match.groupValues[1]
            "($pct / 100)"
        }

        return clean
    }

    private class ExpressionParser(private val str: String) {
        private var pos = -1
        private var ch = -1

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw RuntimeException("Division by zero")
                        x /= divisor
                    }
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, pos)
                x = parseFactor()
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "abs" -> abs(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected character: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }
}
