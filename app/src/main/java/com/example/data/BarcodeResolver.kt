package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BarcodeResolver {
    private const val TAG = "BarcodeResolver"
    
    // Offline database for quick, offline-first resolution of common Indonesian products
    private val localDatabase = mapOf(
        "8993053121407" to Product(
            barcode = "8993053121407",
            nama = "Tisu Paseo Smart 250s",
            kategori = "Kebersihan",
            harga = 14500.0,
            stok = 50,
            foto = "https://images.unsplash.com/photo-1616627561950-9f746e330187?w=500&auto=format&fit=crop&q=60"
        ),
        "8992761100114" to Product(
            barcode = "8992761100114",
            nama = "AQUA Air Mineral 600ml",
            kategori = "Minuman",
            harga = 4000.0,
            stok = 80,
            foto = "https://images.unsplash.com/photo-1608885898957-a599fb1b4600?w=500&auto=format&fit=crop&q=60"
        ),
        "8999999002235" to Product(
            barcode = "8999999002235",
            nama = "Indomie Goreng Spesial",
            kategori = "Makanan",
            harga = 3500.0,
            stok = 150,
            foto = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60"
        ),
        "8999999035622" to Product(
            barcode = "8999999035622",
            nama = "Chitato Sapi Panggang",
            kategori = "Snack",
            harga = 11500.0,
            stok = 45,
            foto = "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?w=500&auto=format&fit=crop&q=60"
        ),
        "8991389221147" to Product(
            barcode = "8991389221147",
            nama = "Kopi Kenangan Mantan Bottle",
            kategori = "Minuman",
            harga = 9500.0,
            stok = 30,
            foto = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500&auto=format&fit=crop&q=60"
        ),
        "8991001111147" to Product(
            barcode = "8991001111147",
            nama = "Teh Botol Sosro 450ml",
            kategori = "Minuman",
            harga = 5000.0,
            stok = 60,
            foto = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500&auto=format&fit=crop&q=60"
        ),
        "8992689001147" to Product(
            barcode = "8992689001147",
            nama = "Pocari Sweat 500ml",
            kategori = "Minuman",
            harga = 7500.0,
            stok = 40,
            foto = "https://images.unsplash.com/photo-1608885898957-a599fb1b4600?w=500&auto=format&fit=crop&q=60"
        ),
        "8992696404320" to Product(
            barcode = "8992696404320",
            nama = "Beng-Beng Chocolate Wafer",
            kategori = "Snack",
            harga = 2500.0,
            stok = 100,
            foto = "https://images.unsplash.com/photo-1558961309-dbdf71791f54?w=500&auto=format&fit=crop&q=60"
        ),
        "8992001101234" to Product(
            barcode = "8992001101234",
            nama = "Teh Pucuk Harum 350ml",
            kategori = "Minuman",
            harga = 3500.0,
            stok = 120,
            foto = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500&auto=format&fit=crop&q=60"
        )
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun resolveOffline(barcode: String): Product? {
        val trimmed = barcode.trim()
        return localDatabase[trimmed] ?: localDatabase[trimmed.replace(" ", "")]
    }

    suspend fun resolveOnline(barcode: String): Product? {
        val trimmed = barcode.trim().replace(" ", "")
        
        // 1. Try local offline map first
        val local = resolveOffline(trimmed)
        if (local != null) return local

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured, returning generic guess.")
            return generateGenericGuess(trimmed)
        }

        try {
            val prompt = """
                Determine the product details for barcode: $trimmed.
                This is for an Indonesian retail POS store application.
                If you recognize this specific barcode (e.g. 8993053121407 is Tisu Paseo Smart, or other common ones), return its real details.
                If you do not recognize it specifically, use the EAN-13 prefixes (899 represents Indonesian manufacturers) and patterns to guess a highly realistic product name, category, and price in IDR (Rupiah).
                Respond STRICTLY with a JSON object containing:
                {
                  "nama": "Product Name in Indonesian (e.g., Tisu Paseo Smart 250s)",
                  "kategori": "One of these categories: Makanan, Minuman, Snack, Pakaian, Jasa, or Lain-lain",
                  "harga": 15000.0,
                  "stok": 50,
                  "foto": "A direct image URL from Unsplash that matches the product type perfectly (e.g., a photo of tissues, food, drinks, etc.)"
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API request failed: ${response.code} ${response.message}")
                    return generateGenericGuess(trimmed)
                }

                val bodyString = response.body?.string() ?: return generateGenericGuess(trimmed)
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsedProduct = JSONObject(textResponse.trim())
                return Product(
                    barcode = trimmed,
                    nama = parsedProduct.getString("nama"),
                    kategori = parsedProduct.getString("kategori"),
                    harga = parsedProduct.getDouble("harga"),
                    stok = parsedProduct.getInt("stok"),
                    foto = parsedProduct.optString("foto", null)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving barcode via Gemini: ${e.localizedMessage}", e)
            return generateGenericGuess(trimmed)
        }
    }

    private fun generateGenericGuess(barcode: String): Product {
        val isIndonesian = barcode.startsWith("899")
        val nama = if (isIndonesian) "Produk Baru (Indonesia) - $barcode" else "Produk Impor Baru - $barcode"
        return Product(
            barcode = barcode,
            nama = nama,
            kategori = "Lain-lain",
            harga = 15000.0,
            stok = 50,
            foto = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=60"
        )
    }
}
