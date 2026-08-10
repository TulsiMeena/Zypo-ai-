package com.example.tools

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class WebSearchTool : Tool {
    override val name: String = "webSearch"
    override val description: String = "Searches the live web for real-time information, news, current events, facts, or data."

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("query", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The search query string to look up on the web")
                })
                put("maxResults", JSONObject().apply {
                    put("type", "INTEGER")
                    put("description", "Maximum number of search results to retrieve (default: 5)")
                })
            })
            put("required", JSONArray().apply { put("query") })
        }
    }

    override suspend fun execute(args: JSONObject): JSONObject {
        val query = args.optString("query", "").trim()
        val maxResults = args.optInt("maxResults", 5).coerceIn(1, 10)

        if (query.isBlank()) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Search query cannot be empty.")
            }
        }

        return try {
            val searchResults = performSearch(query, maxResults)
            if (searchResults.isEmpty()) {
                JSONObject().apply {
                    put("success", true)
                    put("query", query)
                    put("count", 0)
                    put("message", "No search results found for query: $query")
                    put("results", JSONArray())
                }
            } else {
                val resultsArray = JSONArray()
                val sourcesArray = JSONArray()

                searchResults.forEach { result ->
                    val item = JSONObject().apply {
                        put("title", result.title)
                        put("url", result.url)
                        put("domain", result.domain)
                        put("snippet", result.snippet)
                    }
                    resultsArray.put(item)
                    sourcesArray.put(result.domain)
                }

                JSONObject().apply {
                    put("success", true)
                    put("query", query)
                    put("count", searchResults.size)
                    put("results", resultsArray)
                    put("sources", sourcesArray)
                }
            }
        } catch (e: Exception) {
            Log.e("WebSearchTool", "Search failed", e)
            JSONObject().apply {
                put("success", false)
                put("query", query)
                put("error", "Web search failed: ${e.localizedMessage ?: "Network error"}")
            }
        }
    }

    private fun performSearch(query: String, maxResults: Int): List<SearchResultCard> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .post(FormBody.Builder().add("q", query).build())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()
            return parseDuckDuckGoHtml(html, maxResults)
        }
    }

    private fun parseDuckDuckGoHtml(html: String, maxResults: Int): List<SearchResultCard> {
        val results = mutableListOf<SearchResultCard>()

        // Regex for result blocks in DDG HTML
        val resultPattern = Pattern.compile("<a class=\"result__url\" href=\"([^\"]+)\"[^>]*>\\s*([^<]+)\\s*</a>.*?<a class=\"result__snippet[^\"]*\"[^>]*>(.*?)</a>", Pattern.DOTALL)
        val matcher = resultPattern.matcher(html)

        while (matcher.find() && results.size < maxResults) {
            var rawUrl = matcher.group(1) ?: continue
            var title = matcher.group(2)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "Search Result"
            var snippet = matcher.group(3)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""

            // DDG URLs often redirect via //duckduckgo.com/l/?uddg=...
            if (rawUrl.contains("uddg=")) {
                val extracted = rawUrl.substringAfter("uddg=").substringBefore("&")
                rawUrl = java.net.URLDecoder.decode(extracted, "UTF-8")
            } else if (rawUrl.startsWith("//")) {
                rawUrl = "https:$rawUrl"
            }

            val domain = try {
                URI(rawUrl).host ?: "web"
            } catch (e: Exception) {
                "web"
            }

            if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                results.add(
                    SearchResultCard(
                        title = cleanText(title),
                        url = rawUrl,
                        domain = domain.removePrefix("www."),
                        snippet = cleanText(snippet)
                    )
                )
            }
        }

        // Fallback simple parsing if pattern didn't match
        if (results.isEmpty()) {
            val simpleLinkPattern = Pattern.compile("href=\"//duckduckgo.com/l/\\?uddg=([^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL)
            val simpleMatcher = simpleLinkPattern.matcher(html)
            while (simpleMatcher.find() && results.size < maxResults) {
                val encodedUrl = simpleMatcher.group(1) ?: continue
                val rawTitle = simpleMatcher.group(2)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "Web Source"
                val decodedUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
                val domain = try { URI(decodedUrl).host ?: "web" } catch (e: Exception) { "web" }

                if (decodedUrl.startsWith("http://") || decodedUrl.startsWith("https://")) {
                    results.add(
                        SearchResultCard(
                            title = cleanText(rawTitle),
                            url = decodedUrl,
                            domain = domain.removePrefix("www."),
                            snippet = "Information retrieved from $domain."
                        )
                    )
                }
            }
        }

        return results
    }

    private fun cleanText(input: String): String {
        return input.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
