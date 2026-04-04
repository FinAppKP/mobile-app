package com.example.finapp

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000" // локальный сервер
    private val client = OkHttpClient()

    // -----------------------
    // LOGIN
    // -----------------------
    fun login(email: String, password: String, callback: (String?) -> Unit) {
        val body = FormBody.Builder()
            .add("username", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "")
                val token = json.optString("access_token", null)
                callback(token)
            }
        })
    }

    // -----------------------
    // REGISTER
    // -----------------------
    fun register(email: String, password: String, callback: (Boolean) -> Unit) {
        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/register/")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    // -----------------------
    // GET TRANSACTIONS
    // -----------------------
    fun getTransactions(token: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/transactions/")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("{\"balance\":0.0,\"transactions\":[]}")
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "{\"balance\":0.0,\"transactions\":[]}"
                val array = JSONArray(json)
                var balance = 0.0
                for (i in 0 until array.length()) {
                    val tx = array.getJSONObject(i)
                    val type = tx.optString("type")
                    val amount = tx.optDouble("amount", 0.0)
                    balance += when (type) {
                        "income" -> amount
                        "expense" -> -amount
                        else -> 0.0
                    }
                }
                val result = JSONObject()
                result.put("balance", balance)
                result.put("transactions", array)
                callback(result.toString())
            }
        })
    }

    // -----------------------
    // CREATE TRANSACTION
    // -----------------------
    fun createTransaction(
        token: String,
        type: String,
        amount: Double,
        paymentMethod: String,
        categoryId: Int?,
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject()
        json.put("type", type)
        json.put("amount", amount)
        json.put("payment_method", paymentMethod)
        json.put("timestamp", System.currentTimeMillis().toString())
        categoryId?.let { json.put("category_id", it) }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/transactions/")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    // -----------------------
    // GET CATEGORIES
    // -----------------------
    fun getCategories(token: String, callback: (List<Category>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/categories/")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val list = mutableListOf<Category>()
                val bodyStr = response.body?.string() ?: "[]"
                val array = JSONArray(bodyStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(Category(obj.getInt("id"), obj.getString("name"), obj.getString("type")))
                }
                callback(list)
            }
        })
    }

    fun createCategory(token: String, name: String, type: String, callback: (Category?) -> Unit) {
        val json = JSONObject()
        json.put("name", name)
        json.put("type", type)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/categories/")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val obj = JSONObject(response.body?.string() ?: "{}")
                    val category = Category(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        type = obj.getString("type")
                    )
                    callback(category)
                } else {
                    callback(null)
                }
            }
        })
    }

    data class Category(val id: Int, val name: String, val type: String)
}