package com.example.tools

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ToolRegistry(context: Context) {

    companion object {
        private const val TAG = "ToolRegistry"
    }

    private val toolsMap = mutableMapOf<String, Tool>()

    private val _executionLogs = MutableStateFlow<List<ToolExecutionLog>>(emptyList())
    val executionLogs: StateFlow<List<ToolExecutionLog>> = _executionLogs.asStateFlow()

    private val _activeSources = MutableStateFlow<List<SearchResultCard>>(emptyList())
    val activeSources: StateFlow<List<SearchResultCard>> = _activeSources.asStateFlow()

    private val _currentToolStatus = MutableStateFlow<String>("IDLE")
    val currentToolStatus: StateFlow<String> = _currentToolStatus.asStateFlow()

    init {
        registerTool(WebSearchTool())
        registerTool(CalculatorTool())
        registerTool(DateTimeTool())
        registerTool(UnitConversionTool())
        registerTool(OpenWebsiteTool(context))
    }

    fun registerTool(tool: Tool) {
        toolsMap[tool.name] = tool
        Log.d(TAG, "Registered tool: ${tool.name}")
    }

    fun getToolDeclarations(): JSONArray {
        val declarations = JSONArray()
        toolsMap.values.forEach { tool ->
            val funcDecl = JSONObject().apply {
                put("name", tool.name)
                put("description", tool.description)
                put("parameters", tool.getParametersSchema())
            }
            declarations.put(funcDecl)
        }

        return JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", declarations)
            })
        }
    }

    suspend fun handleToolCall(callId: String, name: String, args: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Handling tool call '$name' with args: $args")
        _currentToolStatus.value = "Executing $name..."

        val tool = toolsMap[name]
        val resultObj: JSONObject

        if (tool == null) {
            resultObj = JSONObject().apply {
                put("success", false)
                put("error", "Unknown tool function: $name")
            }
        } else {
            resultObj = try {
                tool.execute(args)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing $name", e)
                JSONObject().apply {
                    put("success", false)
                    put("error", "Tool execution error: ${e.localizedMessage}")
                }
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val isSuccess = resultObj.optBoolean("success", false)

        // Capture search results if webSearch executed
        if (name == "webSearch" && isSuccess) {
            val resultsArray = resultObj.optJSONArray("results") ?: JSONArray()
            val cards = mutableListOf<SearchResultCard>()
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                cards.add(
                    SearchResultCard(
                        title = item.optString("title", "Source"),
                        url = item.optString("url", ""),
                        domain = item.optString("domain", "web"),
                        snippet = item.optString("snippet", "")
                    )
                )
            }
            _activeSources.value = cards
        }

        // Add log entry
        val logEntry = ToolExecutionLog(
            toolName = name,
            arguments = args.toString(),
            startTimeMs = startTime,
            executionTimeMs = duration,
            isSuccess = isSuccess,
            resultSummary = resultObj.toString().take(200)
        )
        addLog(logEntry)
        _currentToolStatus.value = "Completed $name (${duration}ms)"

        JSONObject().apply {
            put("functionResponses", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", callId)
                    put("response", JSONObject().apply {
                        put("output", resultObj)
                    })
                })
            })
        }
    }

    private fun addLog(log: ToolExecutionLog) {
        val current = _executionLogs.value.toMutableList()
        current.add(0, log) // newest first
        if (current.size > 20) current.removeAt(current.size - 1)
        _executionLogs.value = current
    }

    fun clearSources() {
        _activeSources.value = emptyList()
    }
}
