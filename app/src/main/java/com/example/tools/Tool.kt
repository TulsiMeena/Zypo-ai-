package com.example.tools

import org.json.JSONObject

data class ToolExecutionLog(
    val toolName: String,
    val arguments: String,
    val startTimeMs: Long,
    val executionTimeMs: Long,
    val isSuccess: Boolean,
    val resultSummary: String
)

data class SearchResultCard(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String
)

interface Tool {
    val name: String
    val description: String
    fun getParametersSchema(): JSONObject
    suspend fun execute(args: JSONObject): JSONObject
}
