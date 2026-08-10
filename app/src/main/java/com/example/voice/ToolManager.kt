package com.example.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class ToolManager(private val context: Context) {

    companion object {
        private const val TAG = "ToolManager"
    }

    fun getToolDeclarations(): JSONArray {
        val openWebsiteParams = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("url", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The HTTP or HTTPS website URL to open in the device browser")
                })
            })
            put("required", JSONArray().apply { put("url") })
        }

        val openWebsiteFunction = JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Opens a web page URL in the system browser")
            put("parameters", openWebsiteParams)
        }

        val functionDeclarations = JSONArray().apply {
            put(openWebsiteFunction)
        }

        return JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", functionDeclarations)
            })
        }
    }

    fun handleToolCall(callId: String, name: String, args: JSONObject): JSONObject {
        Log.d(TAG, "Executing tool call: $name with args $args")
        val result = JSONObject()

        return try {
            when (name) {
                "openWebsite" -> {
                    val url = args.optString("url", "")
                    val success = openWebsite(url)
                    result.put("result", if (success) "Successfully opened $url" else "Failed to open $url. Invalid or unsafe URL scheme.")
                }
                else -> {
                    result.put("result", "Unknown tool function: $name")
                }
            }

            JSONObject().apply {
                put("functionResponses", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", callId)
                        put("response", JSONObject().apply {
                            put("output", result)
                        })
                    })
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing tool $name", e)
            result.put("error", e.localizedMessage ?: "Tool execution failed")
            JSONObject().apply {
                put("functionResponses", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", callId)
                        put("response", JSONObject().apply {
                            put("output", result)
                        })
                    })
                })
            }
        }
    }

    private fun openWebsite(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = Uri.parse(url.trim())
        val scheme = uri.scheme?.lowercase() ?: return false

        // Security check: only allow http or https
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "Blocked unsafe URL scheme: $scheme")
            return false
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch intent for URL: $url", e)
            false
        }
    }
}
