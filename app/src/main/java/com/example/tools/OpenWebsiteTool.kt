package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class OpenWebsiteTool(private val context: Context) : Tool {
    override val name: String = "openWebsite"
    override val description: String = "Opens a verified HTTP or HTTPS web page URL in the device web browser."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("url", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The HTTP or HTTPS website URL to open in the browser")
                })
            })
            put("required", JSONArray().apply { put("url") })
        }
    }

    override suspend fun execute(args: JSONObject): JSONObject {
        val url = args.optString("url", "").trim()
        if (url.isBlank()) {
            return JSONObject().apply {
                put("success", false)
                put("error", "URL cannot be empty.")
            }
        }

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Invalid URL syntax.")
            }
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            Log.w("OpenWebsiteTool", "Blocked dangerous URL scheme: $scheme")
            return JSONObject().apply {
                put("success", false)
                put("error", "Blocked unsafe scheme '$scheme'. Only HTTP and HTTPS URLs are permitted.")
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject().apply {
                put("success", true)
                put("url", url)
                put("message", "Successfully opened browser for $url")
            }
        } catch (e: Exception) {
            Log.e("OpenWebsiteTool", "Failed to launch browser", e)
            JSONObject().apply {
                put("success", false)
                put("url", url)
                put("error", "Failed to open browser: ${e.localizedMessage}")
            }
        }
    }
}
