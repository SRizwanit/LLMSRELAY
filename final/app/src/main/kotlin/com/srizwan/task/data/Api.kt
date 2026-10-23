package com.srizwan.task.data

import android.content.Context
import com.srizwan.task.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supabase REST API client — common.js-এর sb অবজেক্টের Kotlin রূপ।
 * header: apikey + Authorization: Bearer <key>
 */
class Api(private val context: Context) {
    private val url = BuildConfig.SB_URL
    private val key = BuildConfig.SB_KEY

    suspend fun get(path: String): JSONArray = request("GET", path, null, null)

    suspend fun post(path: String, body: JSONObject): JSONArray =
        request("POST", path, body, "return=representation")

    suspend fun patch(path: String, body: JSONObject): JSONArray =
        request("PATCH", path, body, "return=representation")

    suspend fun patchMin(path: String, body: JSONObject): Unit {
        request("PATCH", path, body, "return=minimal")
    }

    suspend fun upsert(path: String, body: JSONObject): JSONArray =
        request("POST", path, body, "resolution=merge-duplicates,return=representation")

    suspend fun delete(path: String): JSONArray = request("DELETE", path, null, null)

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject?,
        prefer: String?
    ): JSONArray = withContext(Dispatchers.IO) {
        val conn = URL("$url/rest/v1/$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.setRequestProperty("apikey", key)
            conn.setRequestProperty("Authorization", "Bearer $key")
            if (body != null) conn.setRequestProperty("Content-Type", "application/json")
            if (prefer != null) conn.setRequestProperty("Prefer", prefer)
            conn.doInput = true
            if (body != null) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (code !in 200..299) {
                throw ApiException("$method ${path.substringBefore('?')} failed: $code $text")
            }

            if (text.isBlank()) JSONArray() else JSONArray(text)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("ইন্টারনেট সংযোগ নেই বা খুব দুর্বল — আবার চেষ্টা করুন", e)
        } finally {
            conn.disconnect()
        }
    }
}

class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
