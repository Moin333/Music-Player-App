package com.example.musicplayerapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Base64

object SpotifyTokenHelper {
    private const val CLIENT_ID = "d613b03d31c2489c9fd6b0f5303214d8"  // Move to a secure location in production
    private const val CLIENT_SECRET = "3d31831e2e8d42ee852021f23d441cf4"  // Move to a secure location in production
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"

    /**
     * Retrieves Spotify access token using client credentials.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val credentials = "$CLIENT_ID:$CLIENT_SECRET"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
        val requestBody = "grant_type=client_credentials"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(TOKEN_URL)
            .addHeader("Authorization", "Basic $encodedCredentials")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val json = JSONObject(responseBody)
                    json.getString("access_token")
                } else {
                    println("Error: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            println("Network request failed: ${e.localizedMessage}")
            null
        } catch (e: Exception) {
            println("Unexpected error: ${e.localizedMessage}")
            null
        }
    }
}
