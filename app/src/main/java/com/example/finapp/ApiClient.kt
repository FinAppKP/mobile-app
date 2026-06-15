package com.example.finapp

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000" // локальный сервер
    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) onUnauthorized?.invoke()
            response
        }
        .build()

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
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }
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
                if (!response.isSuccessful) {
                    callback("{\"balance\":0.0,\"transactions\":[]}")
                    return
                }
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
        timestamp: String = System.currentTimeMillis().toString(),
        description: String? = null,
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject()
        json.put("type", type)
        json.put("amount", amount)
        json.put("payment_method", paymentMethod)
        json.put("timestamp", timestamp)
        categoryId?.let { json.put("category_id", it) }
        description?.let { json.put("description", it) }

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
                if (!response.isSuccessful) {
                    callback(emptyList())
                    return
                }
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

    fun updateCategory(token: String, id: Int, name: String, type: String, callback: (Boolean) -> Unit) {
        val json = JSONObject()
        json.put("name", name)
        json.put("type", type)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/categories/$id")
            .addHeader("Authorization", "Bearer $token")
            .put(body)
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

    fun deleteTransaction(token: String, id: Int, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/transactions/$id")
            .addHeader("Authorization", "Bearer $token")
            .delete()
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

    fun deleteCategory(token: String, id: Int, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/categories/$id")
            .addHeader("Authorization", "Bearer $token")
            .delete()
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

    data class Category(val id: Int, val name: String, val type: String)

    fun getWallets(token: String, callback: (List<Wallet>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/wallets/")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(emptyList())
                    return
                }
                val wallets = mutableListOf<Wallet>()
                val bodyStr = response.body?.string() ?: "[]"
                val array = JSONArray(bodyStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    wallets.add(
                        Wallet(
                            id = obj.getInt("id"),
                            key = obj.getString("key"),
                            title = obj.getString("title"),
                            iconKey = obj.optString("icon_key", "default"),
                            isSystem = obj.optBoolean("is_system", false)
                        )
                    )
                }
                callback(wallets)
            }
        })
    }

    fun createWallet(token: String, title: String, iconKey: String, callback: (Wallet?) -> Unit) {
        val json = JSONObject()
        json.put("title", title)
        json.put("icon_key", iconKey)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/wallets/")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(parseWallet(JSONObject(response.body?.string() ?: "{}")))
                } else {
                    callback(null)
                }
            }
        })
    }

    fun updateWallet(token: String, key: String, title: String?, iconKey: String?, callback: (Wallet?) -> Unit) {
        val json = JSONObject()
        title?.let { json.put("title", it) }
        iconKey?.let { json.put("icon_key", it) }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/wallets/$key")
            .addHeader("Authorization", "Bearer $token")
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(parseWallet(JSONObject(response.body?.string() ?: "{}")))
                } else {
                    callback(null)
                }
            }
        })
    }

    fun deleteWallet(token: String, key: String, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/wallets/$key")
            .addHeader("Authorization", "Bearer $token")
            .delete()
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

    private fun parseWallet(obj: JSONObject): Wallet {
        return Wallet(
            id = obj.getInt("id"),
            key = obj.getString("key"),
            title = obj.getString("title"),
            iconKey = obj.optString("icon_key", "default"),
            isSystem = obj.optBoolean("is_system", false)
        )
    }

    data class Wallet(
        val id: Int,
        val key: String,
        val title: String,
        val iconKey: String,
        val isSystem: Boolean
    )

    fun getGoals(token: String, callback: (List<Goal>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/goals/")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(emptyList())
                    return
                }
                val goals = mutableListOf<Goal>()
                val bodyStr = response.body?.string() ?: "[]"
                val array = JSONArray(bodyStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    goals.add(
                        Goal(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            targetAmount = obj.getDouble("target_amount"),
                            currentAmount = obj.optDouble("current_amount", 0.0),
                            deadline = obj.optString("deadline", "")
                        )
                    )
                }
                callback(goals)
            }
        })
    }

    fun createGoal(token: String, title: String, targetAmount: Double, deadline: String, callback: (Goal?) -> Unit) {
        val json = JSONObject()
        json.put("title", title)
        json.put("target_amount", targetAmount)
        json.put("deadline", deadline)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/goals/")
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
                    callback(
                        Goal(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            targetAmount = obj.getDouble("target_amount"),
                            currentAmount = obj.optDouble("current_amount", 0.0),
                            deadline = obj.optString("deadline", "")
                        )
                    )
                } else {
                    callback(null)
                }
            }
        })
    }

    fun depositGoal(
        token: String,
        goalId: Int,
        amount: Double,
        timestamp: String = System.currentTimeMillis().toString(),
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject()
        json.put("amount", amount)
        json.put("timestamp", timestamp)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/goals/$goalId/deposit")
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

    fun deleteGoal(token: String, goalId: Int, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/goals/$goalId")
            .addHeader("Authorization", "Bearer $token")
            .delete()
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

    data class Goal(
        val id: Int,
        val title: String,
        val targetAmount: Double,
        val currentAmount: Double,
        val deadline: String
    )
}
